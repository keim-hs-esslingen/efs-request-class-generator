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

import de.hsesslingen.keim.efs.annotations.ParameterScope.Kind;
import static de.hsesslingen.keim.efs.annotations.Utils.*;
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
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;
import static javax.tools.Diagnostic.Kind.*;
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

/**
 *
 * @author ben
 */
@SupportedAnnotationTypes("de.hsesslingen.keim.efs.annotations.GenerateRequestClass")
@SupportedSourceVersion(SourceVersion.RELEASE_14)
public class ApiClientMaker extends AbstractProcessor {

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
                new JavaPoetRequestClassBuilder().buildRequestClasses(api, processingEnv.getFiler());
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

}
