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
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.java.html.lib.Function;
import net.java.html.lib.Objs;
import net.java.html.lib.dom.HTMLTextAreaElement;

/**
 * An instance of this class represents an editable code snippet.
 * It wraps a CodeMirror editor and adds error highlighting and auto-complete.
 */
public final class Editor {
    private final EditorParams params;
    private final ArrayList<TextMarker> markers = new ArrayList<>();
    private EditorFromTextArea codeMirror;
    private Doc doc;
    private TaskQueue.Task<String, String> currentCompileTask;

    private Editor(EditorParams params) {
        this.params = params;
    }

    ////////////////////////////////////////////////////////////////
    // API methods
    ////////////////////////////////////////////////////////////////

    /**
     * Initializes a new CodeOnline Editor in place of a textarea element.
     * The initial content of the editor is set to the value of the textarea.
     * @param textArea the element to be replaced and used for the value
     * @param params additional configuration and callbacks
     * @return a new editor instance
     */
    public static Editor from(HTMLTextAreaElement textArea, EditorParams params) {
        return new Editor(params).initialize(textArea);
    }

    /**
     * Reads the current content from the editor.
     * @return a non-null string
     */
    public String getSourceCode() {
        return codeMirror.getValue();
    }

    /**
     * Updates the content of the editor, displays it, and compiles it.
     * @param value a non-null string
     */
    public void setSourceCode(String value) {
        codeMirror.setValue(value);
    }

    /**
     * Triggers a refresh of the CodeMirror editor.
     * Call this after resizing or showing for the first time (if constructed hidden).
     */
    public void refresh() {
        codeMirror.refresh();
    }

    ////////////////////////////////////////////////////////////////
    // Private methods
    ////////////////////////////////////////////////////////////////

    private Editor initialize(HTMLTextAreaElement textArea) {
        codeMirror = CodeMirrorFactory.create(textArea, this::getHints);
        doc = codeMirror.getDoc();
        on("changes", this::compile);
        on("cursorActivity", this::updateOrCloseHints);
        return this;
    }

    private void on(String eventName, Runnable handler) {
        codeMirror.on(eventName, fnFromRunnable(handler));
    }

    private void compile() {
        String request = new CompilationRequest(getSourceCode(), params.imports, params.requireFull, params.className, -1).toString();
        if(currentCompileTask != null && !currentCompileTask.isSent()) {
            currentCompileTask.update(request);
            return;
        }
        currentCompileTask = params.compilationQueue.enqueue(request, response -> {
            CompilationResult cr = CompilationResultModel.parseCompilationResult(response);
            for(TextMarker marker : markers)
                marker.clear();
            for(Diag diag : cr.getDiagnostics())
                highlightError(diag);
            params.compilationEventHandler.accept(cr);
        });
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

    private static Position makePosition(long line, long ch) {
        Position result = new Objs().$cast(Position.class);
        result.line.set(line);
        result.ch.set(ch);
        return result;
    }

    private static Function.A0 fnFromRunnable(Runnable runnable) {
        return () -> {
            runnable.run();
            return null;
        };
    }

    ////////////////////////////////////////////////////////////////
    // Code completions (called hints in CodeMirror)
    ////////////////////////////////////////////////////////////////

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

    private void getHints(Consumer<Hints> cb, ShowHintOptions opts) {
        opts.completeSingle.set(false);
        Position cur0 = doc.getCursor();
        if(hintActive() && hintRelevant(cur0)) {
            cb.accept(makeHints());
            return;
        }
        int offset = (int) doc.indexFromPos(cur0) - setHintToken(cur0.line().intValue(), cur0.ch().intValue());
        String request = new CompilationRequest(getSourceCode(), params.imports, params.requireFull, params.className, offset).toString();
        if(currentCompletionTask != null && !currentCompletionTask.isSent()) {
            currentCompletionTask.update(request);
            return;
        }
        currentCompletionTask = params.completionQueue.enqueue(request, response -> {
            Position cur1 = doc.getCursor();
            if(hintRelevant(cur1)) {
                CompletionList cl = CompletionListModel.parseCompletionList(response);
                hintItems = cl.getItems();
                cb.accept(makeHints());
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
