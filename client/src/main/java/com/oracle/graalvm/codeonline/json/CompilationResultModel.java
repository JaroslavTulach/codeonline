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
import java.util.List;
import javax.tools.Diagnostic;
import net.java.html.BrwsrCtx;
import net.java.html.json.Model;
import net.java.html.json.Models;
import net.java.html.json.Property;

@Model(className = "CompilationResult", properties = {
    @Property(name = "success", type = boolean.class),
    @Property(name = "diagnostics", type = Diag.class, array = true)
})
public class CompilationResultModel {
    public static CompilationResult createCompilationResult(boolean success, List<Diagnostic> diagnostics) {
        return new CompilationResult(success, diagnostics.stream().map(DiagModel::createDiag).toArray(Diag[]::new));
    }

    public static CompilationResult parseCompilationResult(String json) {
        try {
            return Models.parse(BrwsrCtx.findDefault(CompilationResult.class), CompilationResult.class, new ByteArrayInputStream(json.getBytes()));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
