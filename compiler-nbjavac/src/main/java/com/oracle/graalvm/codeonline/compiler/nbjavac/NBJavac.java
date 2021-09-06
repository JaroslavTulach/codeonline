/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package com.oracle.graalvm.codeonline.compiler.nbjavac;

import com.oracle.graalvm.codeonline.compiler.common.Compiler;
import com.oracle.graalvm.codeonline.compiler.common.WebWorker;
import com.oracle.graalvm.codeonline.json.CompletionList;
import com.oracle.graalvm.codeonline.json.CompletionListModel;
import com.sun.tools.javac.api.JavacTool;
import java.util.Arrays;
import java.util.List;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;

public class NBJavac extends Compiler {
    public static void main(String[] args) {
        WebWorker.main(new NBJavac());
    }

    private final JavacTool compiler = JavacTool.create();

    @Override
    protected boolean compile(JavaFileManager fm, JavaFileObject file, DiagnosticListener<JavaFileObject> diags) throws Exception {
        JavaCompiler.CompilationTask task = compiler.getTask(
                null, // Writer, null ~ System.err
                fm,
                diags,
                Arrays.asList("-source", "1.8", "-target", "1.8"),
                null, // Iterable<String> classes to be processed by annotation processing, null ~ no classes
                Arrays.asList(file)
        );
        return task.call();
    }

    @Override
    protected CompletionList complete(JavaFileManager fm, JavaFileObject file, int offset) throws Exception {
        List<? extends JavaCompletionItem> completions = JavaCompletionQuery.query(new CompilationInfo(file, fm), JavaCompletionQuery.COMPLETION_QUERY_TYPE, offset);
        return CompletionListModel.createCompletionList(true, completions, JavaCompletionItem::toCompletionItem);
    }
}
