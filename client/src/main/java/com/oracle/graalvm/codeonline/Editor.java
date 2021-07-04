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
import com.oracle.graalvm.codeonline.js.PlatformServices;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.tools.Diagnostic;
import net.java.html.lib.Function;
import net.java.html.lib.Objs;
import net.java.html.lib.codemirror.CodeMirror.EditorConfiguration;
import net.java.html.lib.codemirror.CodeMirror.EditorFromTextArea;
import net.java.html.lib.codemirror.CodeMirror.Position;
import net.java.html.lib.codemirror.CodeMirror.TextMarker;
import net.java.html.lib.codemirror.CodeMirror.TextMarkerOptions;
import net.java.html.lib.dom.Element;
import net.java.html.lib.dom.EventListener;
import net.java.html.lib.dom.HTMLElement;
import static net.java.html.lib.dom.Exports.btoa;
import static net.java.html.lib.dom.Exports.document;
import net.java.html.lib.dom.HTMLAnchorElement;
import net.java.html.lib.dom.HTMLButtonElement;
import net.java.html.lib.dom.HTMLTextAreaElement;

/**
 * An instance of this class represents an editable code snippet.
 * It wraps a CodeMirror editor and adds error highlighting and auto-complete.
 */
public class Editor {
    private static final EditorConfiguration CODEMIRROR_CONF;

    private final PlatformServices platformServices;
    private final ArrayList<TextMarker> markers = new ArrayList<>();
    private EditorFromTextArea codeMirror;

    static {
        EditorConfiguration conf = new Objs().$cast(EditorConfiguration.class);
        conf.lineNumbers.set(true);
        conf.mode.set("text/x-java");
        conf.extraKeys.set(Objs.$as(Objs.create(null)).$set("Ctrl-Space", "autocomplete"));
        conf.indentUnit.set(4);
        CODEMIRROR_CONF = conf;
    }

    private Editor(PlatformServices platformServices) {
        this.platformServices = platformServices;
    }

    private void initialize(Element oldElement) {
        HTMLElement newElement = document.createElement("div");
        newElement.appendChild(createButton("Compile", this::compile));
        newElement.appendChild(document.createTextNode(" "));
        newElement.appendChild(createButton("Save", this::save));
        newElement.appendChild(document.createElement("br"));
        HTMLTextAreaElement ta = newElement.appendChild(document.createElement("textarea")).$cast(HTMLTextAreaElement.class);
        ta.value.set(unIndent(oldElement.textContent()));
        oldElement.parentNode().replaceChild(newElement, oldElement);
        codeMirror = net.java.html.lib.codemirror.CodeMirror.Exports.fromTextArea(ta, CODEMIRROR_CONF);
    }

    private static String unIndent(String code) {
        String[] lines = code.split("\n");
        String head = lines[0];
        String indent = "";
        for(int i = 0; i < head.length(); i++) {
            if(!Character.isWhitespace(head.charAt(i))) {
                indent = head.substring(0, i);
                break;
            }
        }
        if(indent.isEmpty())
            return code;
        StringBuilder result = new StringBuilder();
        int length = 0;
        for(String line : lines) {
            if(line.startsWith(indent))
                result.append(line, indent.length(), line.length());
            else
                result.append(line);
            result.append('\n');
            if(!line.isBlank())
                length = result.length();
        }
        return result.substring(0, length);
    }

    public static Editor from(Element element, PlatformServices platformServices) {
        Editor instance = new Editor(platformServices);
        instance.initialize(element);
        return instance;
    }

    private void compile() {
        Compilation c = new Compilation();
        JavaFileManagerImpl files = new JavaFileManagerImpl.Builder(platformServices)
                .addSource(getClassName(), getJavaSource())
                .build();
        c.setFiles(files);
        c.compile();
        List<Diagnostic> diags = c.getDiagnostics();
        for(TextMarker marker : markers) {
            marker.clear();
        }
        for(Diagnostic<?> diag : diags) {
            reportError(diag);
            highlightError(diag);
        }
    }

    private void reportError(Diagnostic<?> diag) {
        // TODO
    }

    private void highlightError(Diagnostic<?> diag) {
        long pos = diag.getPosition();
        if(pos == Diagnostic.NOPOS)
            return;
        long before = pos - diag.getStartPosition();
        long after = diag.getEndPosition() - pos;
        assert before >= 0 && after >= 0;
        long line = diag.getLineNumber() - 1;
        long column = diag.getColumnNumber() - 1;
        long startColumn = column - before;
        long endColumn = column + after;
        if(endColumn - startColumn == 0)
            endColumn++;
        Position start = makePosition(line, startColumn);
        Position end = makePosition(line, endColumn);
        TextMarkerOptions options = new Objs().$cast(TextMarkerOptions.class);
        switch(diag.getKind()) {
            case ERROR:
                options.className.set("codeonline-error");
                break;
            case WARNING:
            case MANDATORY_WARNING:
                options.className.set("codeonline-warning");
                break;
            case NOTE:
            case OTHER:
                options.className.set("codeonline-note");
                break;
        }
        options.title.set(diag.getMessage(Locale.getDefault()));
        TextMarker marker = codeMirror.getDoc().markText(start, end, options);
        markers.add(marker);
    }

    private static Position makePosition(long line, long ch) {
        Position result = new Objs().$cast(Position.class);
        result.line.set(line);
        result.ch.set(ch);
        return result;
    }

    private void save() {
        HTMLAnchorElement a = document.createElement("a").$cast(HTMLAnchorElement.class);
        a.href.set("data:text/x-java;base64," + btoa(getJavaSource()));
        a.setAttribute("download", getClassName() + ".java");
        a.click();
    }

    private String getJavaSource() {
        return codeMirror.getValue();
    }

    private String getClassName() {
        return "Main";
    }

    private static HTMLElement createButton(String text, Runnable onClick) {
        HTMLButtonElement button = document.createElement("button").$cast(HTMLButtonElement.class);
        button.appendChild(document.createTextNode(text));
//        button.onclick.set(ignored -> {
//            onClick.run();
//            return null;
//        });
        button.addEventListener("click", EventListener.$as((Function.A1<Object, Object>)(ignored -> {
            onClick.run();
            return null;
        })));
        return button;
    }
}
