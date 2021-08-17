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

package com.oracle.graalvm.codeonline.compiler.nbjavac;

import com.oracle.graalvm.codeonline.compiler.common.JavaFileManagerImpl;
import com.oracle.graalvm.codeonline.compiler.common.PlatformServices;
import com.oracle.graalvm.codeonline.json.CompilationResultModel;
import com.oracle.graalvm.codeonline.json.CompletionListModel;

public class NBJavacMain {
    public static String executeTask(String request, PlatformServices platformServices) {
        // TODO use JSON, detect class name
        if(request.startsWith("/")) {
            String[] split = request.split("/", 3);
            int pos = Integer.parseInt(split[1]);
            String source = split[2];
            Compilation c = new Compilation();
            JavaFileManagerImpl files = new JavaFileManagerImpl.Builder(platformServices)
                    .addSource("Main", source)
                    .build();
            c.setFiles(files);
            boolean success = c.completion(pos);
            return CompletionListModel.createCompletionList(success, c.getCompletions(), JavaCompletionItem::toCompletionItem).toString();
        }
        String source = request;
        Compilation c = new Compilation();
        JavaFileManagerImpl files = new JavaFileManagerImpl.Builder(platformServices)
                .addSource("Main", source)
                .build();
        c.setFiles(files);
        boolean success = c.compile();
        return CompilationResultModel.createCompilationResult(success, c.getDiagnostics()).toString();
    }
}
