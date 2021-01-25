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

import de.hsesslingen.keim.restutils.AbstractRequest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;

/**
 * Just a template class with no direct use.
 * @author ben
 */
public final class RequestClassTemplate<T, B> extends AbstractRequest<T> {

    private final String baseUrl;
    private final String pathTemplate = "path/template/{placeholder}";

    private String pathVar1;
    private String pathVar2;

    private B bodyObj;
    private Integer queryParam1;
    private Double queryParam2;

    public RequestClassTemplate(
            String baseUrl,
            String pathVar1,
            B bodyObj,
            Double queryParam2
    ) {
        this.baseUrl = baseUrl;

        this.pathVar1 = pathVar1;
        this.bodyObj = bodyObj;
        this.queryParam2 = queryParam2;
    }

    public RequestClassTemplate queryParam1(Integer value) {
        this.queryParam1 = value;
        return this;
    }

    public RequestClassTemplate queryParam2(Double value) {
        this.queryParam2 = value;
        return this;
    }

    public RequestClassTemplate bodyObj(B value) {
        this.bodyObj = value;
        return this;
    }

    public RequestClassTemplate pathVar1(String value) {
        this.pathVar1 = value;
        return this;
    }

    public RequestClassTemplate pathVar2(String value) {
        this.pathVar2 = value;
        return this;
    }

    @Override
    public ResponseEntity<T> go() {        
        var path = pathTemplate
                .replace("{var1Name}", pathVar1)
                
                // for params with deafult values use the ternary operator.
                .replace("{var2Name}", pathVar2 == null ? "pathVar2DV" : pathVar2);

        super.uri(baseUrl + path);

        // wrap non-required params in if block like this. (But only if they do not have default values.
        if (queryParam1 != null) {
            super.query("queryParam1", queryParam1);
        }

        super.query("queryParam2", queryParam2);

        super.body(bodyObj);

        super.expect(new ParameterizedTypeReference<T>() {
        });

        return super.go();
    }

}
