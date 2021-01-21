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
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import static de.hsesslingen.keim.efs.annotations.Utils.*;
import de.hsesslingen.keim.efs.mobility.requests.MiddlewareRequest;
import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import static javax.lang.model.element.Modifier.*;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.JavaFileObject;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 *
 * @author ben
 */
@SupportedAnnotationTypes({
    "org.springframework.web.bind.annotation.GetMapping",
    "org.springframework.web.bind.annotation.PostMapping",
    "org.springframework.web.bind.annotation.PutMapping",
    "org.springframework.web.bind.annotation.DeleteMapping",
    "org.springframework.web.bind.annotation.PatchMapping",
    "org.springframework.web.bind.annotation.RequestMapping"

})
@SupportedSourceVersion(SourceVersion.RELEASE_14)
public class ApiClientMaker extends AbstractProcessor {

    private Map<String, ApiScope> apis = new HashMap<>();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        // First read and collect the api scopes together with its endpoint scopes...
        for (TypeElement anTypeEl : annotations) {
            for (Element el : roundEnv.getElementsAnnotatedWith(anTypeEl)) {
                if (el == null || !(el instanceof ExecutableElement)) {
                    // Skip TypeElements as we only want the method elements.
                    // We need this check because @RequestMapping is also allowed to be on Types.
                    continue;
                }

                var exEl = (ExecutableElement) el;

                // Get class of this method.
                var parentEl = anTypeEl.getEnclosingElement();

                if (!(parentEl instanceof TypeElement)) {
                    // If this is not a type element we skip this element.
                    // TODO: Log warning.
                    continue;
                }

                var api = getOrCreateApiScope((TypeElement) parentEl);

                var anInstance = el.getAnnotation((Class<? extends Annotation>) anTypeEl.getClass());
                var ep = createEnpointScope(anInstance, exEl);

                api.getEndpoints().add(ep);
            }
        }

        for (var api : apis.values()) {
            try {
                buildApiClient(api);
            } catch (IOException ex) {
                Logger.getLogger(ApiClientMaker.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return false; // Do not prevent calling of other processors.
    }

    /**
     * If there is already an {@link ApiScope} for {@link typeEl} in
     * {@link apis} (recognized by simpleName), the element from {@link apis}
     * will be returned. Otherwise an new {@link ApiScope} will be generated
     * based on {@link typeEl}.
     *
     * @param typeEl
     * @return
     */
    private ApiScope getOrCreateApiScope(TypeElement typeEl) {
        var name = typeEl.getSimpleName().toString();

        if (apis.containsKey(name)) {
            return apis.get(name);
        }

        var api = new ApiScope().setTypeElement(typeEl);

        // If the parent element has a request mapping, we must know the path, if that one is set.
        var mapping = typeEl.getAnnotation(RequestMapping.class);
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
    private EndpointScope createEnpointScope(Annotation an, ExecutableElement javaMethod) {
        RequestMethod method = null;
        String path = null;

        if (an instanceof RequestMapping) {
            var mapping = (RequestMapping) an;
            method = firstOrNull(mapping.method());
            path = firstOrNull(mapping.path());

        } else if (an instanceof GetMapping) {
            method = RequestMethod.GET;
            path = firstOrNull(((GetMapping) an).value());

        } else if (an instanceof PostMapping) {
            method = RequestMethod.POST;
            path = firstOrNull(((PostMapping) an).value());

        } else if (an instanceof PutMapping) {
            method = RequestMethod.PUT;
            path = firstOrNull(((PutMapping) an).value());

        } else if (an instanceof DeleteMapping) {
            method = RequestMethod.DELETE;
            path = firstOrNull(((DeleteMapping) an).value());

        } else if (an instanceof PatchMapping) {
            method = RequestMethod.PATCH;
            path = firstOrNull(((PatchMapping) an).value());
        }

        return new EndpointScope(method, path, javaMethod);
    }
    
    private ParameterScope createParameterScope(VariableElement el) {
        var name = el.getSimpleName().toString();
        var type = el.asType();
        
        
    }

    private Writer getWriter(String sourceFileName, Element... originatingElements) throws IOException {
        Filer filer = processingEnv.getFiler();
        JavaFileObject fileObject = filer.createSourceFile(sourceFileName, originatingElements);
        return fileObject.openWriter();
    }

    private void buildApiClient(ApiScope api) throws IOException {
        var clientClassName = api.getApiClassName() + "RequestBuilder";

        var classBldr = TypeSpec.classBuilder(clientClassName)
                .addModifiers(PUBLIC, FINAL);

        for (var ep : api.getEndpoints()) {
            var methodSpec = createMethodSpec(ep);
            classBldr.addMethod(methodSpec);
        }

        var classTypeSpec = classBldr.build();

        var javaFile = JavaFile.builder(api.getApiPackageName(), classTypeSpec)
                .build();

        var writer = getWriter(api.getApiPackageName() + "." + clientClassName, api.getTypeElement());

        javaFile.writeTo(writer);
    }

    private MethodSpec createMethodSpec(EndpointScope ep) {

        // Create return type as ResponseEntity<T>, where T is the return type of the endpoint.
        var returnType = ParameterizedTypeName.get(
                ClassName.get(MiddlewareRequest.class),
                TypeName.get(ep.getReturnType())
        );

        var m = MethodSpec
                .methodBuilder("build" + toUpperCamelCase(ep.getMethodName()) + "Request")
                .addModifiers(PUBLIC, STATIC)
                .returns(returnType);

        m.addParameter(String[].class, "args")
                .addStatement("$T.out.println($S)", System.class, "Hello, JavaPoet!");

        return m.build();
    }

}
