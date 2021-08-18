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

package com.oracle.graalvm.codeonline.compiler.common;

import com.oracle.graalvm.codeonline.json.CompilationRequest;
import com.oracle.graalvm.codeonline.json.CompilationRequestModel;
import com.oracle.graalvm.codeonline.json.CompilationResult;
import com.oracle.graalvm.codeonline.json.CompletionList;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

public abstract class Compiler {
    protected abstract CompilationResult compile(JavaFileManager fm, JavaFileObject file) throws Exception;
    protected abstract CompletionList complete(JavaFileManager fm, JavaFileObject file, int offset) throws Exception;

    public final String handleRequest(PlatformServices platformServices, String requestJson) {
        CompilationRequest request = CompilationRequestModel.parseCompilationRequest(requestJson);
        boolean noCompletion = (request.getCompletionOffset() == -1);
        try {
            JavaFileManagerImpl fm = new JavaFileManagerImpl.Builder(platformServices)
                    .addSource(request.getName(), request.getSource())
                    .build();
            JavaFileObject file = fm.getJavaFileForInput(StandardLocation.SOURCE_PATH, "Main", JavaFileObject.Kind.SOURCE);
            if(noCompletion) {
                return compile(fm, file).toString();
            } else {
                return complete(fm, file, request.getCompletionOffset()).toString();
            }
        } catch(Exception ex) {
            ex.printStackTrace();
            if(noCompletion) {
                return new CompilationResult(false).toString();
            } else {
                return new CompletionList(false).toString();
            }
        }
    }
}
