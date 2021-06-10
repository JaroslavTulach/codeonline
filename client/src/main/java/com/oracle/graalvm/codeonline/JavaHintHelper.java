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
import net.java.html.lib.codemirror.CodeMirror.Doc;
import net.java.html.lib.codemirror.CodeMirror.Position;
import net.java.html.lib.codemirror.showhint.CodeMirror.Hints;
import net.java.html.lib.codemirror.showhint.CodeMirror.ShowHintOptions;

/**
 * Implements a CodeMirror hint helper using javac.
 */
public class JavaHintHelper implements Function.A2<Doc, ShowHintOptions, Hints> {
    @Override
    public Hints call(Doc doc, ShowHintOptions opts) {
        Position cursor = doc.getCursor();
        String line = doc.getLine(cursor.line().intValue());
        int col = cursor.ch().intValue();
        int start = col, end = col;
        while(start > 0 && Character.isJavaIdentifierPart(line.charAt(start - 1)))
            start--;
        while(end < line.length() && Character.isJavaIdentifierPart(line.charAt(end)))
            end++;
        // TODO ask javac
        System.out.println("CodeOnline: Hint requested: " + line.substring(start, col) + "|" + line.substring(col, end));
        return null;
    }
}
