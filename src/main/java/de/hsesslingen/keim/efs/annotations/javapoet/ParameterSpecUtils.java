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
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import java.lang.reflect.Type;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

/**
 *
 * @author ben
 */
public class ParameterSpecUtils {

    public static ParameterSpec paramSpec(VariableElement el) {
        return ParameterSpec.get(el);
    }

    public static ParameterSpec.Builder paramSpecBldr(Type type, String name, Modifier... modifiers) {
        return ParameterSpec.builder(type, name, modifiers);
    }

    public static ParameterSpec.Builder paramSpecBldr(TypeName type, String name, Modifier... modifiers) {
        return ParameterSpec.builder(type, name, modifiers);
    }

    public static ParameterSpec.Builder paramSpecBldr(Class<?> clazz, String name, Modifier... modifiers) {
        return paramSpecBldr(ClassName.get(clazz), name, modifiers);
    }

    public static ParameterSpec.Builder paramSpecBldr(TypeMirror type, String name, Modifier... modifiers) {
        return paramSpecBldr(TypeName.get(type), name, modifiers);
    }

    public static ParameterSpec paramSpec(Type type, String name, Modifier... modifiers) {
        return paramSpecBldr(type, name, modifiers).build();
    }

    public static ParameterSpec paramSpec(TypeName type, String name, Modifier... modifiers) {
        return paramSpecBldr(type, name, modifiers).build();
    }

    public static ParameterSpec paramSpec(Class<?> clazz, String name, Modifier... modifiers) {
        return paramSpec(ClassName.get(clazz), name, modifiers);
    }

    public static ParameterSpec paramSpec(TypeMirror type, String name, Modifier... modifiers) {
        return paramSpec(TypeName.get(type), name, modifiers);
    }
}
