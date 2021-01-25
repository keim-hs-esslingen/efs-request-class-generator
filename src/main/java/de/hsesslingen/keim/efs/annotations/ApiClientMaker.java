/*
 * MIT License
 * 
 * Copyright (c) 2021 Hochschule Esslingen
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE. 
 */
package de.hsesslingen.keim.efs.annotations;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import de.hsesslingen.keim.efs.annotations.ParameterScope.Kind;
import static de.hsesslingen.keim.efs.annotations.ParameterScope.Kind.BODY;
import static de.hsesslingen.keim.efs.annotations.ParameterScope.Kind.HEADER_PARAM;
import static de.hsesslingen.keim.efs.annotations.ParameterScope.Kind.PATH_VARIABLE;
import static de.hsesslingen.keim.efs.annotations.ParameterScope.Kind.QUERY_PARAM;
import static de.hsesslingen.keim.efs.annotations.Utils.*;
import de.hsesslingen.keim.restutils.AbstractRequest;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Set;
import static java.util.stream.Collectors.toList;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ExecutableElement;
import static javax.lang.model.element.Modifier.*;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;
import static javax.tools.Diagnostic.Kind.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author ben
 */
@SupportedAnnotationTypes("de.hsesslingen.keim.efs.annotations.GenerateRequestClass")
@SupportedSourceVersion(SourceVersion.RELEASE_14)
public class ApiClientMaker extends AbstractProcessor {

    private static final ClassName STRING = ClassName.get(String.class);

    public void log(Diagnostic.Kind kind, String message) {
        processingEnv.getMessager().printMessage(kind, message);
    }

    public void logNote(String message) {
        processingEnv.getMessager().printMessage(NOTE, message);
    }

    public void logWarn(String message) {
        processingEnv.getMessager().printMessage(WARNING, message);
    }

    public void logError(String message) {
        processingEnv.getMessager().printMessage(ERROR, message);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations.isEmpty()) {
            return false;
        }

        logNote("Started processing of @GeneratedRequestClass annotations.");

        logNote("Started collecting endpoints.");
        // First read and collect the api scopes together with its endpoint scopes...
        var apis = annotations.stream()
                .flatMap(a -> roundEnv.getElementsAnnotatedWith(a).stream())
                .map(el -> (TypeElement) el)
                .map(el -> createApiScope(el))
                .filter(api -> api != null)
                .peek(this::collectEndpointScopes)
                .collect(toList());

        logNote("Started generating request classes.");
        for (var api : apis) {
            try {
                buildApiClient(api);
            } catch (IOException ex) {
                logError("An exception occured:\n");
                logError(ex.getMessage());
            }
        }

        return false;
    }

    /**
     * Iterates over the executable elements in the type element of this api and
     * collects the endpoint scopes of the suitable methods.
     *
     * @param api
     */
    private void collectEndpointScopes(ApiScope api) {
        api.getTypeElement().getEnclosedElements().stream()
                .filter(el -> el instanceof ExecutableElement)
                .map(el -> ((ExecutableElement) el))
                .map(this::createEnpointScope) // return null if unable to create scope.
                .filter(ep -> ep != null) // filter null values.
                .forEach(api.getEndpoints()::add);
    }

    /**
     * If there is already an {@link ApiScope} for {@link typeEl} in
     * {@link apis} (recognized by simpleName), the element from {@link apis}
     * will be returned. Otherwise an new {@link ApiScope} will be generated
     * based on {@link typeEl}.
     *
     * @param typeElement
     * @return
     */
    private ApiScope createApiScope(TypeElement typeElement) {
        var api = new ApiScope().setTypeElement(typeElement);

        // If the parent element has a request mapping, we must know the path, if that one is set.
        var mapping = typeElement.getAnnotation(RequestMapping.class);
        if (mapping != null) {
            var path = firstOrNull(mapping.path());
            api.setPath(path);
        }

        return api;
    }

    /**
     * Creates an {@link EndpointScope} element from a RequestMapping kind of
     * annotation.
     *
     * @param an
     * @param javaMethod
     * @return
     */
    private EndpointScope createEnpointScope(ExecutableElement javaMethod) {
        RequestMethod method;
        String path;

        Annotation an;

        if ((an = javaMethod.getAnnotation(RequestMapping.class)) != null) {
            var mapping = (RequestMapping) an;
            method = firstOrNull(mapping.method());
            path = firstOrNull(mapping.path());

        } else if ((an = javaMethod.getAnnotation(GetMapping.class)) != null) {
            method = RequestMethod.GET;
            path = firstOrNull(((GetMapping) an).value());

        } else if ((an = javaMethod.getAnnotation(PostMapping.class)) != null) {
            method = RequestMethod.POST;
            path = firstOrNull(((PostMapping) an).value());

        } else if ((an = javaMethod.getAnnotation(PutMapping.class)) != null) {
            method = RequestMethod.PUT;
            path = firstOrNull(((PutMapping) an).value());

        } else if ((an = javaMethod.getAnnotation(DeleteMapping.class)) != null) {
            method = RequestMethod.DELETE;
            path = firstOrNull(((DeleteMapping) an).value());

        } else if ((an = javaMethod.getAnnotation(PatchMapping.class)) != null) {
            method = RequestMethod.PATCH;
            path = firstOrNull(((PatchMapping) an).value());
        } else {
            return null;
        }

        // First create a list of ParameterScopes...
        var params = javaMethod.getParameters().stream()
                .map(this::createParameterScope)
                .collect(toList());

        return new EndpointScope(method, path, javaMethod, params);
    }

    /**
     * Creates a {@link ParameterScope} object from a particular
     * {@link VariableElement}.
     *
     * @param el
     * @return
     */
    private ParameterScope createParameterScope(VariableElement el) {
        var varName = el.getSimpleName().toString();
        var name = varName;
        var type = el.asType();

        ParameterScope.Kind kind = null;
        boolean required = true;
        String defaultValue = null;
        String declaredName = null;

        Annotation an;

        if ((an = el.getAnnotation(PathVariable.class)) != null) {
            kind = Kind.PATH_VARIABLE;
            var pan = (PathVariable) an;
            declaredName = pan.name();
            required = pan.required();

        } else if ((an = el.getAnnotation(RequestParam.class)) != null) {
            kind = Kind.QUERY_PARAM;
            var qan = (RequestParam) an;
            declaredName = qan.name();
            required = qan.required();
            defaultValue = qan.defaultValue();

        } else if ((an = el.getAnnotation(RequestHeader.class)) != null) {
            kind = Kind.HEADER_PARAM;
            var han = (RequestHeader) an;
            declaredName = han.name();
            required = han.required();
            defaultValue = han.defaultValue();

        } else if ((an = el.getAnnotation(RequestBody.class)) != null) {
            kind = Kind.BODY;
            var ban = (RequestBody) an;
            declaredName = "body";
            required = ban.required();

        }

        if (kind == null) {
            // Without kind, no equest parameter.
            return null;
        }

        var nameToUse = declaredName != null ? declaredName : name;

        return new ParameterScope(varName, nameToUse, kind, type, required, defaultValue);
    }

    private void buildApiClient(ApiScope api) throws IOException {
        api.getEndpoints().stream()
                .forEach(ep -> {
                    var spec = createRequestClass(api, ep);

                    var javaFile = JavaFile.builder(api.getApiRequestClassPackageName(), spec)
                            .build();

                    try {
                        javaFile.writeTo(System.out);
                        javaFile.writeTo(processingEnv.getFiler());
                    } catch (IOException ex) {
                        logError(ex.getMessage());
                        throw new RuntimeException(ex);
                    }
                });
    }

    private FieldSpec createParameterField(ParameterScope pv) {
        var f = FieldSpec.builder(TypeName.get(pv.getType()), pv.getVariableName(), PRIVATE);
        return f.build();
    }

    private MethodSpec createConstructor(EndpointScope ep) {
        var m = MethodSpec.constructorBuilder()
                .addModifiers(PUBLIC)
                .addParameter(STRING, "baseUrl");

        var sb = new StringBuilder()
                .append("this.baseUrl = baseUrl;\n\n");

        ep.getParams().stream()
                .filter(ps -> ps.isRequired())
                .peek(ps -> { // Create assignment statement for contstructor body...
                    sb.append("this.")
                            .append(ps.getVariableName())
                            .append(" = ")
                            .append(ps.getVariableName())
                            .append(";\n");
                })
                .map(ps -> ParameterSpec.builder(TypeName.get(ps.getType()), ps.getVariableName()).build()).
                forEach(m::addParameter);

        m.addCode(sb.toString());
        return m.build();
    }

    private MethodSpec createParameterMethod(ClassName returnType, ParameterScope ps) {
        var m = MethodSpec.methodBuilder(ps.getVariableName())
                .addModifiers(PUBLIC)
                .returns(returnType);

        m.addParameter(TypeName.get(ps.getType()), ps.getVariableName());

        m
                .addStatement("this." + ps.getVariableName() + " = " + ps.getVariableName())
                .addStatement("return this");

        return m.build();
    }

    private MethodSpec createGoMethodWithRestTemplate(EndpointScope ep) {
        return MethodSpec.methodBuilder("go")
                .addModifiers(PUBLIC)
                .returns(ParameterizedTypeName.get(
                        ClassName.get(ResponseEntity.class),
                        TypeName.get(ep.getReturnType())
                ))
                .addParameter(ParameterSpec.builder(ClassName.get(RestTemplate.class), "restTemplate").build())
                .addStatement("this.restTemplate = restTemplate")
                .addStatement("return this.go()")
                .build();
    }

    private MethodSpec createGoOverrideMethod(EndpointScope ep) {
        var m = MethodSpec.methodBuilder("go")
                .addModifiers(PUBLIC)
                .addAnnotation(Override.class)
                .returns(ParameterizedTypeName.get(
                        ClassName.get(ResponseEntity.class),
                        TypeName.get(ep.getReturnType())
                ));

        var sb = new StringBuilder()
                .append("var path = pathTemplate\n");

        // Build path template converter statement...
        for (var pv : ep.getParams()) {
            if (pv.getKind() != PATH_VARIABLE) {
                continue;
            }

            // Add replacement statement that replaces the path variable placeholder in the template with the actual value.
            sb
                    .append("\t.replace(\"{")
                    .append(pv.getVariableName())
                    .append("}\", ");

            // If there is a default value for this param. Add it with the ternary operator...
            if (pv.hasDefaultValue()) {
                sb
                        .append(pv.getVariableName())
                        .append(" == null ? \"")
                        .append(pv.getDefaultValue())
                        .append("\" : ");
            }

            sb
                    .append(pv.getVariableName())
                    .append(")\n");
        }

        // Add uri setter with baseUrl and path...
        sb.append("\t;\n\nsuper.uri(baseUrl + path);\n\n");

        // Add rest of params (non-path-variables)...
        for (var ps : ep.getParams()) {
            if (ps.getKind() == PATH_VARIABLE) {
                continue;
            }

            if (!ps.isRequired()) {
                sb
                        .append("if (this.")
                        .append(ps.getVariableName())
                        .append(" != null) {\n\t");
            }

            sb.append("super.");

            switch (ps.getKind()) {
                case BODY:
                    sb
                            .append("body(this.")
                            .append(ps.getVariableName());
                    break;
                case HEADER_PARAM:
                    sb
                            .append("header(\"")
                            .append(ps.getName())
                            .append("\", this.")
                            .append(ps.getVariableName());
                    break;
                case QUERY_PARAM:
                    sb
                            .append("query(\"")
                            .append(ps.getName())
                            .append("\", this.")
                            .append(ps.getVariableName());
            }

            sb.append(");\n");

            if (!ps.isRequired()) {
                sb.append("}\n");
            }

            sb.append("\n\n");
        }

        sb
                // Inject expected type reference...
                .append("super.expect(new org.springframework.core.ParameterizedTypeReference<")
                .append(ep.getReturnType().toString())
                .append(">() {});\n\n")
                // Call go...
                .append("return super.go();");

        m.addCode(sb.toString());
        return m.build();
    }

    private MethodSpec createGetRestTemplateOverride() {
        return MethodSpec.methodBuilder("getRestTemplate")
                .addModifiers(PROTECTED)
                .addAnnotation(Override.class)
                .returns(ClassName.get(RestTemplate.class))
                .addCode("if(this.restTemplate == null){\n"
                        + "\treturn super.getRestTemplate();\n"
                        + "}\n"
                        + "return this.restTemplate;\n"
                )
                .build();
    }

    private TypeSpec createRequestClass(ApiScope api, EndpointScope ep) {
        var className = ClassName.get(api.getApiRequestClassPackageName(), ep.getRequestClassName());
        var t = TypeSpec.classBuilder(className).addModifiers(PUBLIC, FINAL);

        // Create parent type as AbstractRequest<T>, where T is the return type of the endpoint.
        t.superclass(ParameterizedTypeName.get(
                ClassName.get(AbstractRequest.class),
                TypeName.get(ep.getReturnType())
        ));

        // concatenate api and endpoint path to get full pathTemplate for this endpoint.
        var pathTemplate = safeConcat(api.getPath(), ep.getPath());

        // Add common fields...
        t.addField(FieldSpec.builder(STRING, "baseUrl", PRIVATE, FINAL).build());
        t.addField(FieldSpec.builder(STRING, "pathTemplate", PRIVATE, FINAL).initializer("\"" + pathTemplate + "\"").build());
        t.addField(FieldSpec.builder(ClassName.get(RestTemplate.class), "restTemplate", PRIVATE).build());

        // Add a storage field for each param...
        ep.getParams().stream()
                .map(this::createParameterField)
                .forEach(t::addField);

        t.addMethod(createConstructor(ep));
        t.addMethod(createGetRestTemplateOverride());
        // Make builder style methods for each param.
        ep.getParams().stream()
                .filter(ps -> !ps.isRequired()) // Required values are set in constructor.
                .map(ps -> createParameterMethod(className, ps))
                .forEach(t::addMethod);

        // Add essential go method override.
        t.addMethod(createGoOverrideMethod(ep));
        t.addMethod(createGoMethodWithRestTemplate(ep));

        return t.build();
    }

}
