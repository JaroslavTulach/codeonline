/*
 * Copyright 2021 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.oracle.graalvm.codeonline.json;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import net.java.html.BrwsrCtx;
import net.java.html.json.Model;
import net.java.html.json.Models;
import net.java.html.json.Property;

@Model(className = "CompilationRequest", properties = {
    @Property(name = "source", type = String.class),
    @Property(name = "imports", type = String.class),
    @Property(name = "full", type = boolean.class),
    @Property(name = "name", type = String.class),
    @Property(name = "completionOffset", type = int.class)
})
public class CompilationRequestModel {
    public static CompilationRequest parseCompilationRequest(String json) {
        try {
            return Models.parse(BrwsrCtx.findDefault(CompilationRequest.class), CompilationRequest.class, new ByteArrayInputStream(json.getBytes()));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
