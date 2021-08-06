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

import net.java.html.lib.Function;
import net.java.html.lib.Objs;
import net.java.html.lib.codemirror.CodeMirror.EditorFromTextArea;
import net.java.html.lib.codemirror.showhint.CodeMirror.ShowHintOptions;

/**
 * Implements a CodeMirror hint helper using javac.
 */
public class JavaHintHelper implements Function.A3/*<EditorFromTextArea, Function, ShowHintOptions, Hints>*/ {
    private JavaHintHelper() {}

    @Override
    public Object call(Object doc, Object cb, Object opts) {
        Editor.getInstance(EditorFromTextArea.$as(doc)).hint(Function.$as(cb), ShowHintOptions.$as(opts));
        return null;
    }

    public static Objs create() {
        Objs result = new Objs(new JavaHintHelper());
        result.$set("async", true);
        return result;
    }
}
