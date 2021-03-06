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

package com.oracle.graalvm.codeonline;

import com.oracle.graalvm.codeonline.files.JavaFileManagerImpl;
import com.oracle.graalvm.codeonline.nbjava.CompilationInfo;
import com.oracle.graalvm.codeonline.nbjava.JavaCompletionItem;
import com.oracle.graalvm.codeonline.nbjava.JavaCompletionQuery;
import com.sun.tools.javac.api.JavacTool;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

/**
 * One instance of this class corresponds to one execution of the compiler.
 * It stores all information for the compiler and the result of the compilation.
 */
public final class Compilation {
    private final JavacTool compiler = JavacTool.create();
    private final ArrayList<Diagnostic> diagnostics = new ArrayList<>();
    private final List<Diagnostic> diagnosticsView = Collections.unmodifiableList(diagnostics);
    private List<? extends JavaCompletionItem> completions;

    private JavaFileManagerImpl files;

    public void setFiles(JavaFileManagerImpl files) {
        this.files = files;
    }

    public boolean compile() {
        System.out.println("Compiling...");
        diagnostics.clear();
        try {
            JavaFileObject f = files.getJavaFileForInput(StandardLocation.SOURCE_PATH, "Main", JavaFileObject.Kind.SOURCE);
            JavaCompiler.CompilationTask task = compiler.getTask(
                    null, // Writer, null ~ System.err
                    files,
                    diagnostics::add,
                    Arrays.asList("-source", "1.8", "-target", "1.8"),
                    null, // Iterable<String> classes to be processed by annotation processing, null ~ no classes
                    Arrays.asList(f)
            );
            boolean success = task.call();
            System.out.println("Result: " + success);

            System.out.println("Diagnostics:");
            for(Diagnostic diag : diagnostics)
                System.out.println(diag);

            System.out.println("Files:");
            files.debugDump();
            return success;
        } catch(Throwable t) {
            t.printStackTrace();
            return false;
        }
    }

    public List<Diagnostic> getDiagnostics() {
        return diagnosticsView;
    }

    public boolean completion(int offset) {
        completions = Collections.emptyList();
        try {
            JavaFileObject f = files.getJavaFileForInput(StandardLocation.SOURCE_PATH, "Main", JavaFileObject.Kind.SOURCE);
            completions = JavaCompletionQuery.query(new CompilationInfo(f, files), JavaCompletionQuery.COMPLETION_QUERY_TYPE, offset);
            return true;
        } catch(Throwable t) {
            t.printStackTrace();
            return false;
        }
    }

    public List<? extends JavaCompletionItem> getCompletions() {
        return completions;
    }
}
