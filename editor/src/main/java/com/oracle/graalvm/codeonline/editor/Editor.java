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

package com.oracle.graalvm.codeonline.editor;

import com.oracle.graalvm.codeonline.editor.cm.CodeMirror.Doc;
import com.oracle.graalvm.codeonline.editor.cm.CodeMirror.EditorFromTextArea;
import com.oracle.graalvm.codeonline.editor.cm.CodeMirror.Position;
import com.oracle.graalvm.codeonline.editor.cm.CodeMirror.TextMarker;
import com.oracle.graalvm.codeonline.editor.cm.CodeMirror.TextMarkerOptions;
import com.oracle.graalvm.codeonline.editor.cm.CodeMirrorFactory;
import com.oracle.graalvm.codeonline.editor.cm.ShowHint.Hint;
import com.oracle.graalvm.codeonline.editor.cm.ShowHint.Hints;
import com.oracle.graalvm.codeonline.editor.cm.ShowHint.ShowHintOptions;
import com.oracle.graalvm.codeonline.json.CompilationRequest;
import com.oracle.graalvm.codeonline.json.CompilationResult;
import com.oracle.graalvm.codeonline.json.CompilationResultModel;
import com.oracle.graalvm.codeonline.json.CompletionItem;
import com.oracle.graalvm.codeonline.json.CompletionList;
import com.oracle.graalvm.codeonline.json.CompletionListModel;
import com.oracle.graalvm.codeonline.json.Diag;
import com.oracle.graalvm.codeonline.json.DiagModel;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.java.html.lib.Function;
import net.java.html.lib.Objs;
import net.java.html.lib.dom.Element;
import net.java.html.lib.dom.EventListener;
import net.java.html.lib.dom.HTMLElement;
import static net.java.html.lib.dom.Exports.btoa;
import static net.java.html.lib.dom.Exports.document;
import net.java.html.lib.dom.HTMLAnchorElement;
import net.java.html.lib.dom.HTMLButtonElement;
import net.java.html.lib.dom.HTMLTextAreaElement;
import net.java.html.lib.dom.Text;

/**
 * An instance of this class represents an editable code snippet.
 * It wraps a CodeMirror editor and adds error highlighting and auto-complete.
 */
public class Editor {
    private static final String EDITOR_PROPERTY = "codeonline.Editor";

    private final EditorParams params;
    private final ArrayList<TextMarker> markers = new ArrayList<>();
    private String origSource;
    private String imports;
    private boolean requireFull;
    private EditorFromTextArea codeMirror;
    private Doc doc;
    private HTMLElement errorIndicator, warningIndicator, noteIndicator, noDiagIndicator;
    private List<Diag> diags;
    private TaskQueue.Task<String, String> currentCompileTask;

    private Editor(EditorParams params) {
        this.params = params;
    }

    public static Editor getInstance(EditorFromTextArea codeMirror) {
        return (Editor) codeMirror.$get(EDITOR_PROPERTY);
    }

    private void initialize(Element oldElement) {
        origSource = unIndent(oldElement.textContent());
        imports = getImports(oldElement.getAttribute("data-imports"));
        requireFull = getBoolean(oldElement.getAttribute("data-require-full"), false);
        HTMLElement newElement = document.createElement("div");
        newElement.appendChild(createButton("Save", this::save));
        newElement.appendChild(document.createTextNode(" "));
        newElement.appendChild(createButton("Reset", this::reset));
        newElement.appendChild(document.createTextNode(" "));
        errorIndicator = createIndicator(newElement, "ErrorIndicator", () -> getDiags(DiagModel.Kind.ERROR));
        warningIndicator = createIndicator(newElement, "WarningIndicator", () -> getDiags(DiagModel.Kind.WARNING));
        noteIndicator = createIndicator(newElement, "NoteIndicator", () -> getDiags(DiagModel.Kind.NOTE));
        noDiagIndicator = createIndicator(newElement, "NoDiagIndicator", () -> "No errors or warnings");
        newElement.appendChild(document.createElement("br"));
        HTMLTextAreaElement ta = newElement.appendChild(document.createElement("textarea")).$cast(HTMLTextAreaElement.class);
        ta.value.set(origSource);
        oldElement.parentNode().replaceChild(newElement, oldElement);
        codeMirror = CodeMirrorFactory.create(ta);
        doc = codeMirror.getDoc();
        codeMirror.$set(EDITOR_PROPERTY, this);
        on("changes", this::compile);
        on("cursorActivity", this::updateOrCloseHints);
    }

    private void on(String eventName, Runnable handler) {
        codeMirror.on(eventName, fnFromRunnable(handler));
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
            if(!line.matches("\\s*")) // .isBlank()
                length = result.length();
        }
        return result.substring(0, length);
    }

    private static String getImports(String attr) {
        if(attr == null)
            return "";
        final String IMPORT = "import ";
        String[] items = attr.split("[,;]");
        StringBuilder result = new StringBuilder(attr.length() + items.length * IMPORT.length() + 2);
        for (String item : items) {
            if(item.matches("\\s*")) // .isBlank()
                continue;
            if(!item.matches("\\s*import\\s.*"))
                result.append(IMPORT);
            result.append(item).append(';');
        }
        result.append('\n');
        return result.toString();
    }

    private boolean getBoolean(String attr, boolean defaultValue) {
        if(attr == null) // not present
            return defaultValue;
        if(Boolean.toString(defaultValue).equalsIgnoreCase(attr)) // present but explicitly default value
            return defaultValue;
        return !defaultValue;
    }

    public static Editor from(Element element, EditorParams params) {
        Editor instance = new Editor(params);
        instance.initialize(element);
        return instance;
    }

    private void compile() {
        String request = new CompilationRequest(getJavaSource(), imports, requireFull, getClassName(), -1).toString();
        if(currentCompileTask != null && !currentCompileTask.isSent()) {
            currentCompileTask.update(request);
            return;
        }
        currentCompileTask = params.compilationQueue.enqueue(request, response -> {
            CompilationResult cr = CompilationResultModel.parseCompilationResult(response);
            for(TextMarker marker : markers) {
                marker.clear();
            }
            int numErrors = 0, numWarnings = 0, numNotes = 0;
            boolean noDiags = true;
            for(Diag diag : diags = cr.getDiagnostics()) {
                switch(diag.getKind()) {
                    case ERROR:
                        numErrors++;
                        break;
                    case WARNING:
                        numWarnings++;
                        break;
                    case NOTE:
                        numNotes++;
                        break;
                }
                noDiags = false;
                highlightError(diag);
            }
            reportCount(errorIndicator, numErrors);
            reportCount(warningIndicator, numWarnings);
            reportCount(noteIndicator, numNotes);
            reportNoDiags(noDiags);
        });
    }

    private void reportCount(HTMLElement counter, int count) {
        if(count != 0) {
            counter.style().display.set("inline-block");
            setText(counter, Integer.toString(count));
        } else {
            counter.style().display.set("none");
        }
    }

    private void reportNoDiags(boolean noDiags) {
        if(noDiags) {
            noDiagIndicator.style().display.set("inline-block");
        } else {
            noDiagIndicator.style().display.set("none");
        }
    }

    private void highlightError(Diag diag) {
        long pos = diag.getPosition();
        if(pos == DiagModel.NOPOS)
            return;
        Position start = codeMirror.getDoc().posFromIndex(diag.getStartPosition());
        Position end = codeMirror.getDoc().posFromIndex(diag.getEndPosition());
        TextMarkerOptions options = new Objs().$cast(TextMarkerOptions.class);
        switch(diag.getKind()) {
            case ERROR:
                options.className.set("codeonline-error");
                break;
            case WARNING:
                options.className.set("codeonline-warning");
                break;
            case NOTE:
                options.className.set("codeonline-note");
                break;
        }
        options.title.set(diag.getMessage());
        TextMarker marker = codeMirror.getDoc().markText(start, end, options);
        markers.add(marker);
    }

    private String getDiags(DiagModel.Kind kind) {
        StringBuilder result = new StringBuilder();
        boolean addSep = false;
        for(Diag diag : diags) {
            if(diag.getKind() != kind)
                continue;
            if(addSep)
                result.append('\n');
            else
                addSep = true;
            long line = diag.getLineNumber(), col = diag.getColumnNumber();
            String msg = diag.getMessage();
            if(line == DiagModel.NOPOS)
                result.append(diag.getMessage());
            else if(col == DiagModel.NOPOS)
                result.append(line).append(": ").append(msg);
            else
                result.append(line).append(":").append(col).append(": ").append(msg);
        }
        return result.toString();
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

    private void reset() {
        codeMirror.setValue(origSource);
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
        button.addEventListener("click", listener(onClick));
        return button;
    }

    private static HTMLElement createIndicator(HTMLElement parent, String cssClass, Supplier<String> getDesc) {
        HTMLElement wrapper = document.createElement("span");
        wrapper.className.set("IndicatorWrapper");
        HTMLElement indicator = document.createElement("span");
        indicator.className.set("Indicator " + cssClass);
        indicator.style().display.set("none");
        indicator.appendChild(document.createTextNode(""));
        wrapper.appendChild(indicator);
        HTMLElement desc = document.createElement("span");
        desc.className.set("IndicatorDesc");
        desc.style().display.set("none");
        desc.appendChild(document.createTextNode(""));
        wrapper.appendChild(desc);
        wrapper.tabIndex.set(0);
        wrapper.addEventListener("click", listener(() -> {
            setText(desc, getDesc.get());
            desc.style().display.set("inline-block");
        }));
        wrapper.addEventListener("blur", listener(() -> desc.style().display.set("none")));
        parent.appendChild(wrapper);
        return indicator;
    }

    private static EventListener listener(Runnable runnable) {
        return EventListener.$as(fnFromRunnable(runnable));
    }

    private static Function.A1 fnFromRunnable(Runnable runnable) {
        return ignored -> {
            runnable.run();
            return null;
        };
    }

    private static void setText(Element element, String text) {
        element.childNodes().$get(0).$cast(Text.class).data.set(text);
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
        int offset = (int) doc.indexFromPos(cur0) - setHintToken(cur0.line().intValue(), cur0.ch().intValue());
        String request = new CompilationRequest(getJavaSource(), imports, requireFull, getClassName(), offset).toString();
        if(currentCompletionTask != null && !currentCompletionTask.isSent()) {
            currentCompletionTask.update(request);
            return;
        }
        currentCompletionTask = params.completionQueue.enqueue(request, response -> {
            Position cur1 = doc.getCursor();
            if(hintRelevant(cur1)) {
                CompletionList cl = CompletionListModel.parseCompletionList(response);
                hintItems = cl.getItems();
                cb.apply(null, makeHints());
            }
        });
    }

    private void updateOrCloseHints() {
        if(hintActive() && hintRelevant(doc.getCursor()))
            com.oracle.graalvm.codeonline.editor.cm.ShowHint.Exports.showHint(codeMirror);
    }

    private static Hints makeHints(Stream<Hint> list, Position from, Position to) {
        Hints hints = new Objs().$cast(Hints.class);
        ((Objs.Property) hints.list).set(list.toArray());
        hints.from.set(from);
        hints.to.set(to);
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
