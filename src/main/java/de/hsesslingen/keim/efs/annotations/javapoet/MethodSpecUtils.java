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
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeVariableName;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeVariable;

/**
 *
 * @author ben
 */
public class MethodSpecUtils {

    public static MethodSpec.Builder methodSpec(String name, Modifier... modifiers) {
        return MethodSpec.methodBuilder(name).addModifiers(modifiers);
    }

    public static MethodSpec.Builder methodSpec(String name, Object... properties) {
        var spec = MethodSpec.methodBuilder(name);

        // First non-annotation class is used
        boolean returnTypeSet = false;

        for (var prop : properties) {
            if (prop instanceof Class<?>) {
                var clazz = (Class<?>) prop;

                if (clazz.isAnnotation()) {
                    spec.addAnnotation(clazz);
                } else {
                    if (!returnTypeSet) {
                        spec.returns(ClassName.get(clazz));
                        returnTypeSet = true;
                    } else if (Throwable.class.isAssignableFrom(clazz)) {
                        spec.addException(ClassName.get(clazz));
                    }
                }
            } else if (prop instanceof Modifier) {
                spec.addModifiers((Modifier) prop);
            } else if (prop instanceof ParameterSpec) {
                spec.addParameter((ParameterSpec) prop);
            } else if (prop instanceof TypeVariable) {
                spec.addTypeVariable(TypeVariableName.get((TypeVariable) prop));
            }
        }

        return spec;
    }

}
