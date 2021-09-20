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

import com.oracle.graalvm.codeonline.editor.Editor;
import com.oracle.graalvm.codeonline.editor.EditorParams;
import com.oracle.graalvm.codeonline.editor.TaskQueue;
import com.oracle.graalvm.codeonline.json.CompilationResult;
import com.oracle.graalvm.codeonline.json.Diag;
import com.oracle.graalvm.codeonline.json.DiagModel;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.java.html.lib.Function;
import net.java.html.lib.Objs;
import net.java.html.lib.dom.Document;
import net.java.html.lib.dom.Element;
import net.java.html.lib.dom.EventListener;
import static net.java.html.lib.dom.Exports.btoa;
import net.java.html.lib.dom.HTMLAnchorElement;
import net.java.html.lib.dom.HTMLButtonElement;
import net.java.html.lib.dom.HTMLElement;
import net.java.html.lib.dom.HTMLTextAreaElement;
import net.java.html.lib.dom.Node;
import net.java.html.lib.dom.Text;

final class LiveDocEditor {
    private static final String CSS_CLASS_PREFIX = "codeonline-";

    private final Document document;
    private Editor editor;
    private String origSource;
    private String fileName;
    private Indicator errorIndicator, warningIndicator, noteIndicator, noDiagIndicator;
    private List<Diag> diags;

    LiveDocEditor(Document ownerDocument) {
        this.document = ownerDocument;
    }

    ////////////////////////////////////////////////////////////////
    // Parsing attributes
    ////////////////////////////////////////////////////////////////

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

    private static String getString(String attr, String defaultValue) {
        if(attr == null)
            return defaultValue;
        return attr;
    }

    private static boolean getBoolean(String attr, boolean defaultValue) {
        if(attr == null) // not present
            return defaultValue;
        if(Boolean.toString(defaultValue).equalsIgnoreCase(attr)) // present but explicitly default value
            return defaultValue;
        return !defaultValue;
    }

    ////////////////////////////////////////////////////////////////
    // Generating the widget
    ////////////////////////////////////////////////////////////////

    void initialize(TaskQueue<String, String> universalQueue, Element oldElement) {
        // parse attributes
        origSource = unIndent(oldElement.textContent());
        String imports = getImports(oldElement.getAttribute("data-imports"));
        String className = getString(oldElement.getAttribute("data-class"), "Main");
        boolean requireFull = getBoolean(oldElement.getAttribute("data-require-full"), false);
        fileName = className + ".java";
        // generate toolbar
        HTMLElement container = createElement("div", "main",
                createElement("div", "toolbar",
                        createElement("span", "buttons",
                                createButton("Save", this::save),
                                createButton("Revert", this::revert)
                        ),
                        createElement("span", "indicators",
                                (errorIndicator = createIndicator("error", () -> getDiags(DiagModel.Kind.ERROR))).wrapper,
                                (warningIndicator = createIndicator("warning", () -> getDiags(DiagModel.Kind.WARNING))).wrapper,
                                (noteIndicator = createIndicator("note", () -> getDiags(DiagModel.Kind.NOTE))).wrapper,
                                (noDiagIndicator = createIndicator("ok", () -> "No errors or warnings")).wrapper
                        )
                )
        );
        // put it all to the document
        oldElement.parentNode().replaceChild(container, oldElement);
        // add textarea below toolbar
        HTMLTextAreaElement ta = container.appendChild(document.createElement("textarea")).$cast(HTMLTextAreaElement.class);
        ta.value.set(origSource);
        // change textarea to proper editor
        editor = Editor.from(ta, new EditorParams(universalQueue, universalQueue, this::onCompilation, imports, className, requireFull));
        // export some functions for use from JavaScript
        container.$set("codeonline", createJsApi());
    }

    private Indicator createIndicator(String kind, Supplier<String> descSupplier) {
        final Text counter, desc;
        final HTMLElement wrapper = createElement("span", "indicator-wrapper",
                createElement("span", "indicator " + CSS_CLASS_PREFIX + kind + "-indicator",
                        counter = document.createTextNode("")
                ),
                createElement("span", "indicator-desc",
                        desc = document.createTextNode("")
                )
        );
        wrapper.tabIndex.set(0);
        wrapper.addEventListener("focus", listener(() -> {
            desc.data.set(descSupplier.get());
        }));
        return new Indicator(wrapper, counter, desc);
    }

    private HTMLElement createButton(String text, Runnable onClick) {
        HTMLButtonElement button = document.createElement("button").$cast(HTMLButtonElement.class);
        button.appendChild(document.createTextNode(text));
        button.addEventListener("click", listener(onClick));
        return button;
    }

    private Objs createJsApi() {
        Objs objs = new Objs();
        objs.$set("getSourceCode", (Function.A0) editor::getSourceCode);
        objs.$set("setSourceCode", fnFromConsumer(editor::setSourceCode));
        objs.$set("refresh", fnFromRunnable(editor::refresh));
        return objs;
    }

    private static EventListener listener(Runnable runnable) {
        return EventListener.$as(fnFromRunnable(runnable));
    }

    private static Function.A0 fnFromRunnable(Runnable runnable) {
        return () -> {
            runnable.run();
            return null;
        };
    }

    private static <A> Function.A1<A, Void> fnFromConsumer(Consumer<A> consumer) {
        return arg -> {
            consumer.accept(arg);
            return null;
        };
    }

    private HTMLElement createElement(String tagName, String className, Node... children) {
        HTMLElement elem = document.createElement(tagName);
        if(className != null)
            elem.className.set(CSS_CLASS_PREFIX + className);
        for(Node ch : children)
            elem.appendChild(ch);
        return elem;
    }

    ////////////////////////////////////////////////////////////////
    // Displaying compilation results
    ////////////////////////////////////////////////////////////////

    private void onCompilation(CompilationResult cr) {
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
        }
        errorIndicator.report(numErrors);
        warningIndicator.report(numWarnings);
        noteIndicator.report(numNotes);
        noDiagIndicator.report(noDiags);
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

    private static final class Indicator {
        final HTMLElement wrapper;
        final Text counter;
        final Text desc;

        Indicator(HTMLElement wrapper, Text counter, Text desc) {
            this.wrapper = wrapper;
            this.counter = counter;
            this.desc = desc;
        }

        void report(int count) {
            counter.data.set(Integer.toString(count));
            report(count != 0);
        }

        void report(boolean visible) {
            if(visible) {
                wrapper.classList().add(CSS_CLASS_PREFIX + "indicator-wrapper-visible");
            } else {
                wrapper.classList().remove(CSS_CLASS_PREFIX + "indicator-wrapper-visible");
            }
        }
    }

    ////////////////////////////////////////////////////////////////
    // User actions
    ////////////////////////////////////////////////////////////////

    private void save() {
        HTMLAnchorElement a = document.createElement("a").$cast(HTMLAnchorElement.class);
        a.href.set("data:text/x-java;base64," + btoa(editor.getSourceCode()));
        a.setAttribute("download", fileName);
        a.click();
    }

    private void revert() {
        editor.setSourceCode(origSource);
    }
}
