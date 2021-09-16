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

package com.oracle.graalvm.codeonline.editor.cm;

import com.oracle.graalvm.codeonline.editor.cm.CodeMirror.EditorFromTextArea;
import com.oracle.graalvm.codeonline.editor.cm.ShowHint.ShowHintOptions;
import net.java.html.lib.Function;
import net.java.html.lib.Objs;

/**
 * Implements a CodeMirror hint helper using javac.
 */
final class JavaHintHelper implements Function.A3/*<EditorFromTextArea, Function, ShowHintOptions, Hints>*/ {
    private static final String HINT_FUNCTION_PROPERTY = "codeonline:hintFunction";

    private JavaHintHelper() {}

    @Override
    public Object call(Object editor, Object cb, Object opts) {
        Function function = Function.$as(cb);
        getHintFunction(EditorFromTextArea.$as(editor)).getHints(hints -> function.apply(null, hints), ShowHintOptions.$as(opts));
        return null;
    }

    static Objs create() {
        Objs result = new Objs(new JavaHintHelper());
        result.$set("async", true);
        return result;
    }

    static void setHintFunction(EditorFromTextArea editor, HintProvider hintFunction) {
        editor.$set(HINT_FUNCTION_PROPERTY, hintFunction);
    }

    static HintProvider getHintFunction(EditorFromTextArea editor) {
        return (HintProvider) editor.$get(HINT_FUNCTION_PROPERTY);
    }
}
