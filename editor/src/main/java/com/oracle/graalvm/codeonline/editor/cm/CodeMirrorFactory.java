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

import com.oracle.graalvm.codeonline.editor.cm.CodeMirror.EditorConfiguration;
import com.oracle.graalvm.codeonline.editor.cm.CodeMirror.EditorFromTextArea;
import static com.oracle.graalvm.codeonline.editor.cm.CodeMirror.Exports.fromTextArea;
import static com.oracle.graalvm.codeonline.editor.cm.CodeMirror.Exports.registerHelper;
import net.java.html.lib.Objs;
import net.java.html.lib.dom.HTMLTextAreaElement;

public final class CodeMirrorFactory {
    private static final EditorConfiguration CODEMIRROR_CONF;

    static {
        CodeMirrorModuleProvider.register();
        registerHelper("hint", "clike", JavaHintHelper.create());
        EditorConfiguration conf = new Objs().$cast(EditorConfiguration.class);
        conf.lineNumbers.set(true);
        conf.mode.set("text/x-java");
        conf.extraKeys.set(new Objs().$set("Ctrl-Space", "autocomplete"));
        conf.indentUnit.set(4);
        CODEMIRROR_CONF = conf;
    }

    public static EditorFromTextArea create(HTMLTextAreaElement ta, HintProvider hintProvider) {
        EditorFromTextArea editor = fromTextArea(ta, CODEMIRROR_CONF);
        JavaHintHelper.setHintFunction(editor, hintProvider);
        return editor;
    }
}
