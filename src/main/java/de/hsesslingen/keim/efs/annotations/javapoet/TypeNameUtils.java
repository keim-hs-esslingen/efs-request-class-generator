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
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.lang.reflect.Type;
import javax.lang.model.type.TypeMirror;

/**
 *
 * @author ben
 */
public class TypeNameUtils {

    public static TypeName typeName(Type type) {
        return TypeName.get(type);
    }

    public static TypeName typeName(TypeMirror type) {
        return TypeName.get(type);
    }

    public static ClassName typeName(Class<?> type) {
        return ClassName.get(type);
    }

    public static ParameterizedTypeName paramsTypeName(ClassName rawType, TypeName... params) {
        return ParameterizedTypeName.get(
                rawType,
                params
        );
    }

    public static ParameterizedTypeName paramsTypeName(Class<?> rawType, TypeName... params) {
        return paramsTypeName(ClassName.get(rawType), params);
    }

    public static ParameterizedTypeName paramsTypeName(Class<?> rawType, Type... params) {
        return ParameterizedTypeName.get(
                rawType,
                params
        );
    }

    public static ParameterizedTypeName paramsTypeName(Class<?> rawType, TypeMirror param1) {
        return paramsTypeName(ClassName.get(rawType), typeName(param1));
    }

    public static ParameterizedTypeName paramsTypeName(Class<?> rawType, TypeMirror param1, TypeMirror param2) {
        return paramsTypeName(ClassName.get(rawType), typeName(param1), typeName(param2));
    }

    public static ParameterizedTypeName paramsTypeName(Class<?> rawType, TypeMirror param1, TypeMirror param2, TypeMirror param3) {
        return paramsTypeName(ClassName.get(rawType), typeName(param1), typeName(param2), typeName(param3));
    }

}
