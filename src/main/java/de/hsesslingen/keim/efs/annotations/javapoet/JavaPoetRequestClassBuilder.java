/*
 * The MIT License
 *
 * Copyright 2021 ben.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package de.hsesslingen.keim.efs.annotations.javapoet;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import de.hsesslingen.keim.efs.annotations.ApiScope;
import de.hsesslingen.keim.efs.annotations.EndpointScope;
import de.hsesslingen.keim.efs.annotations.ParameterScope;
import static de.hsesslingen.keim.efs.annotations.javapoet.FieldSpecUtils.*;
import static de.hsesslingen.keim.efs.annotations.javapoet.ParameterSpecUtils.*;
import static de.hsesslingen.keim.efs.annotations.ParameterScope.Kind.PATH_VARIABLE;
import static de.hsesslingen.keim.efs.annotations.Utils.safeConcat;
import static de.hsesslingen.keim.efs.annotations.javapoet.MethodSpecUtils.methodSpec;
import static de.hsesslingen.keim.efs.annotations.javapoet.TypeNameUtils.*;
import de.hsesslingen.keim.restutils.AbstractRequest;
import java.io.IOException;
import javax.annotation.processing.Filer;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PROTECTED;
import static javax.lang.model.element.Modifier.PUBLIC;
import javax.lang.model.type.TypeKind;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author ben
 */
public class JavaPoetRequestClassBuilder {

    private static final ClassName STRING = ClassName.get(String.class);
    private static final Logger logger = getLogger(JavaPoetRequestClassBuilder.class);

    public void buildRequestClasses(ApiScope api, Filer filer) throws IOException {
        api.getEndpoints().stream()
                .forEach(ep -> {
                    var spec = createRequestClass(api, ep);

                    var javaFile = JavaFile.builder(api.getApiRequestClassPackageName(), spec)
                            .build();

                    try {
                        javaFile.writeTo(System.out);
                        javaFile.writeTo(filer);
                    } catch (IOException ex) {
                        logger.error("{}", ex);
                        throw new RuntimeException(ex);
                    }
                });
    }

    private MethodSpec.Builder createConstructorWithBasicParams() {
        return MethodSpec.constructorBuilder()
                .addModifiers(PUBLIC)
                .addParameter(STRING, "baseUrl")
                .addCode("this.baseUrl = baseUrl;\n");
    }

    private MethodSpec.Builder createConstructorWithRequiredParams(EndpointScope ep) {
        var m = createConstructorWithBasicParams();

        ep.getParams().stream()
                .filter(ps -> ps.isRequired())
                // Create assignment statement for contstructor body...
                .peek(ps -> m.addCode("this.$1L = $1L;\n", ps.getVariableName()))
                .map(ps -> paramSpec(ps.getType(), ps.getVariableName())).
                forEachOrdered(m::addParameter);

        return m;
    }

    private MethodSpec.Builder createConstructorWithAllParams(EndpointScope ep) {
        var m = createConstructorWithRequiredParams(ep);

        // Add those params, that are not required. This makes sure, the required ones come as first and the order is the same.
        ep.getParams().stream()
                .filter(ps -> !ps.isRequired())
                // Create assignment statement for contstructor body...
                .peek(ps -> m.addCode("this.$1L = $1L;\n", ps.getVariableName()))
                .map(ps -> paramSpec(ps.getType(), ps.getVariableName())).
                forEachOrdered(m::addParameter);

        return m;
    }

    private MethodSpec createParameterMethod(ClassName returnType, ParameterScope ps) {
        return methodSpec(ps.getVariableName(), PUBLIC)
                .returns(returnType)
                .addParameter(paramSpec(ps.getType(), ps.getVariableName()))
                .addStatement("this." + ps.getVariableName() + " = " + ps.getVariableName())
                .addStatement("return this")
                .build();
    }

    private MethodSpec createGoMethodWithRestTemplate(EndpointScope ep) {
        var m = methodSpec("go", PUBLIC);

        if (ep.getReturnType().getKind() == TypeKind.VOID) {
            m.returns(TypeName.VOID);
        } else {
            m.returns(paramsTypeName(ResponseEntity.class, ep.getReturnType()));
        }

        m.addParameter(paramSpec(RestTemplate.class, "restTemplate"))
                .addStatement("this.restTemplate = restTemplate")
                .addStatement("return this.go()")
                .build();

        return m.build();
    }

    private MethodSpec createGoOverrideMethod(EndpointScope ep) {
        var m = methodSpec("go", PUBLIC)
                .addAnnotation(Override.class);

        if (ep.getReturnType().getKind() == TypeKind.VOID) {
            m.returns(TypeName.VOID);
        } else {
            m.returns(paramsTypeName(ResponseEntity.class, ep.getReturnType()));
        }

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

        // Inject expected type reference...
        sb.append("super.expect(new org.springframework.core.ParameterizedTypeReference<");

        if (ep.getReturnType().getKind() == TypeKind.VOID) {
            sb.append("Void.class");
        } else {
            sb.append(ep.getReturnType().toString());
        }

        sb.append(">() {});\n\n")
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
        t.superclass(paramsTypeName(AbstractRequest.class, ep.getReturnType()));

        // concatenate api and endpoint path to get full pathTemplate for this endpoint.
        var pathTemplate = safeConcat(api.getPath(), ep.getPath());

        // Add common fields...
        t.addField(fieldSpecBldr(STRING, "pathTemplate", PRIVATE, FINAL).initializer("\"" + pathTemplate + "\"").build());
        t.addField(fieldSpec(STRING, "baseUrl", PRIVATE, FINAL));
        t.addField(fieldSpec(RestTemplate.class, "restTemplate", PRIVATE));

        // Add a storage field for each param...
        ep.getParams().stream()
                .map(ps -> fieldSpec(ps.getType(), ps.getVariableName(), PRIVATE))
                .forEach(t::addField);

        // Create constructors...
        t.addMethod(createConstructorWithBasicParams().build());

        if (!ep.getParams().isEmpty()) {
            if (ep.getParams().stream().anyMatch(p -> p.isRequired())) {
                t.addMethod(createConstructorWithRequiredParams(ep).build());
            }

            if (ep.getParams().stream().anyMatch(p -> !p.isRequired())) {
                t.addMethod(createConstructorWithAllParams(ep).build());
            }
        }

        t.addMethod(createGetRestTemplateOverride());

        // Make builder style methods for each param.
        ep.getParams().stream()
                //.filter(ps -> !ps.isRequired()) // Required values are set in constructor.
                .map(ps -> createParameterMethod(className, ps))
                .forEach(t::addMethod);

        // Add essential go method override.
        t.addMethod(createGoOverrideMethod(ep));
        t.addMethod(createGoMethodWithRestTemplate(ep));

        return t.build();
    }

}
