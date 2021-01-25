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
package de.hsesslingen.keim.efs.annotations;

import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 *
 * @author ben
 */
@Data
@Accessors(chain = true)
public class ApiScope {

    private TypeElement typeElement;
    private String path;
    private List<EndpointScope> endpoints;

    public ApiScope() {
        endpoints = new ArrayList<>();
    }

    public String getApiPackageName() {
        return ((PackageElement) typeElement.getEnclosingElement()).getQualifiedName().toString();
    }

    public String getApiClassName() {
        return typeElement.getSimpleName().toString();
    }
    
    public String getApiRequestsClassName() {
        return getApiClassName() + "Requests";
    }
}
