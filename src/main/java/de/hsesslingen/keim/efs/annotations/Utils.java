package de.hsesslingen.keim.efs.annotations;

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
/**
 *
 * @author ben
 */
public class Utils {

    private Utils() {
    }

    public static String toUpperCamelCase(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }

        var result = name.substring(0, 1).toUpperCase();

        if (name.length() == 1) {
            return result;
        }

        return result + name.substring(1);
    }

    public static String toLowerCamelCase(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }

        var result = name.substring(0, 1).toLowerCase();

        if (name.length() == 1) {
            return result;
        }

        return result + name.substring(1);
    }

    public static <T> T firstOrNull(T[] array) {
        if (array == null || array.length == 0) {
            return null;
        }
        return array[0];
    }

}
