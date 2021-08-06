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

import com.oracle.graalvm.codeonline.js.PlatformServices;
import com.oracle.graalvm.codeonline.js.TaskQueue;
import com.oracle.graalvm.codeonline.json.CompilationResult;
import com.oracle.graalvm.codeonline.json.CompilationResultModel;
import com.oracle.graalvm.codeonline.json.CompletionItem;
import com.oracle.graalvm.codeonline.json.CompletionList;
import com.oracle.graalvm.codeonline.json.CompletionListModel;
import com.oracle.graalvm.codeonline.json.Diag;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.tools.Diagnostic;
import net.java.html.lib.Function;
import net.java.html.lib.Objs;
import net.java.html.lib.codemirror.CodeMirror.Doc;
import net.java.html.lib.codemirror.CodeMirror.EditorConfiguration;
import net.java.html.lib.codemirror.CodeMirror.EditorFromTextArea;
import net.java.html.lib.codemirror.CodeMirror.Position;
import net.java.html.lib.codemirror.CodeMirror.TextMarker;
import net.java.html.lib.codemirror.CodeMirror.TextMarkerOptions;
import net.java.html.lib.codemirror.showhint.CodeMirror.Hint;
import net.java.html.lib.codemirror.showhint.CodeMirror.Hints;
import net.java.html.lib.codemirror.showhint.CodeMirror.ShowHintOptions;
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
    private static final String EDITOR_PROPERTY = "codeonline.Editor";
    private static final EditorConfiguration CODEMIRROR_CONF;

    private final PlatformServices platformServices;
    private final ArrayList<TextMarker> markers = new ArrayList<>();
    private EditorFromTextArea codeMirror;
    private Doc doc;
    private TaskQueue.Task<String, String> currentCompileTask;

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

    public static Editor getInstance(EditorFromTextArea codeMirror) {
        return (Editor) codeMirror.$get(EDITOR_PROPERTY);
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
        doc = codeMirror.getDoc();
        codeMirror.$set(EDITOR_PROPERTY, this);
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
            if(!line.isEmpty())
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
        String request = getJavaSource();
        if(currentCompileTask != null && !currentCompileTask.isSent()) {
            currentCompileTask.update(request);
            return;
        }
        currentCompileTask = platformServices.getWorkerQueue().enqueue(request, response -> {
            CompilationResult cr = CompilationResultModel.parseCompilationResult(response);
            for(TextMarker marker : markers) {
                marker.clear();
            }
            for(Diag diag : cr.getDiagnostics()) {
                reportError(diag);
                highlightError(diag);
            }
        });
    }

    private void reportError(Diag diag) {
        // TODO
    }

    private void highlightError(Diag diag) {
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
        options.title.set(diag.getMessage());
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

    private String hintPrefix;
    private int currentHintLine, currentHintTokenStart;
    private List<CompletionItem> hintItems;
    private TaskQueue.Task<String, String> currentCompletionTask;

    private boolean hintActive() {
        return hintPrefix != null;
    }

    private boolean hintRelevant(Position cur0) {
        int line = cur0.line().intValue();
        int col = cur0.ch().intValue();
        hintPrefix = hintPrefix(line, col);
        return hintPrefix != null;
    }

    private String hintPrefix(int newLine, int newCol) {
        if(newLine != currentHintLine)
            return null;
        if(newCol < currentHintTokenStart)
            return null;
        if(newCol == currentHintTokenStart)
            return "";
        String line = doc.getLine(newLine);
        for(int i = currentHintTokenStart; i < newCol; i++) {
            if(!Character.isJavaIdentifierPart(line.charAt(i)))
                return null;
        }
        return line.substring(currentHintTokenStart, newCol);
    }

    private int setHintToken(int line, int col) {
        String s = doc.getLine(line);
        int i = col;
        while(i > 0 && Character.isJavaIdentifierPart(s.charAt(i - 1)))
            i--;
        currentHintLine = line;
        currentHintTokenStart = i;
        return col - i;
    }

    public void hint(Function cb, ShowHintOptions opts) {
        opts.completeSingle.set(false);
        Position cur0 = doc.getCursor();
        if(hintActive() && hintRelevant(cur0)) {
            cb.apply(null, makeHints());
            return;
        }
        long offset = (long) doc.indexFromPos(cur0) - setHintToken(cur0.line().intValue(), cur0.ch().intValue());
        String request = "/" + offset + "/" + doc.getValue();
        if(currentCompletionTask != null && !currentCompletionTask.isSent()) {
            currentCompletionTask.update(request);
            return;
        }
        currentCompletionTask = platformServices.getWorkerQueue().enqueue(request, response -> {
            Position cur1 = doc.getCursor();
            if(hintRelevant(cur1)) {
                CompletionList cl = CompletionListModel.parseCompletionList(response);
                hintItems = cl.getItems();
                cb.apply(null, makeHints());
            }
        });
    }

    private static Hints makeHints(Stream<Hint> list, Position from, Position to) {
        Hints hints = new Objs().$cast(Hints.class);
        ((Objs.Property) hints.list).set(list.toArray());
        ((Objs.Property) hints.from).set(from);
        ((Objs.Property) hints.to).set(to);
        return hints;
    }

    private Hints makeHints() {
        Stream<Hint> list = hintItems.stream().filter(it -> it.getText().startsWith(hintPrefix)).map(Editor::makeHint);
        return makeHints(list, makePosition(currentHintLine, currentHintTokenStart), doc.getCursor());
    }

    private static Hint makeHint(CompletionItem ci) {
        Hint hint = new Objs().$cast(Hint.class);
        hint.text.set(ci.getText());
        hint.displayText.set(ci.getDisplayText());
        hint.className.set(ci.getClassName());
        return hint;
    }
}
