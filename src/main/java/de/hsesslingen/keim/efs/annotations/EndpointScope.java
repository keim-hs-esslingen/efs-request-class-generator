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

import java.util.List;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 *
 * @author ben
 */
@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class EndpointScope {

    private RequestMethod method;
    private String path;
    private ExecutableElement javaMethod;
    private List<ParameterScope> params;

    public String getMethodName() {
        return javaMethod.getSimpleName().toString();
    }

    public TypeMirror getReturnType() {
        return javaMethod.getReturnType();
    }

}
