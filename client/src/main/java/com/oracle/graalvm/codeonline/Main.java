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
import com.oracle.graalvm.codeonline.js.TaskQueue;
import com.oracle.graalvm.codeonline.json.CompilationResultModel;
import com.oracle.graalvm.codeonline.json.CompletionListModel;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import net.java.html.BrwsrCtx;
import net.java.html.boot.BrowserBuilder;
import net.java.html.lib.dom.Element;
import net.java.html.lib.dom.NodeListOf;

/**
 * Desktop client entry point and common client code.
 */
public final class Main {
    private Main() {
        throw new UnsupportedOperationException();
    }

    public static void main(String... args) throws Exception {
        BrowserBuilder.newBrowser().
            loadPage("pages/index.html").
            loadClass(Main.class).
            invoke("onDesktopPageLoad", args).
            showAndWait();
        System.exit(0);
    }

    public static void onPageLoad(PlatformServices services) throws Exception {
        services.registerCodeMirrorModule();
        net.java.html.lib.codemirror.CodeMirror.Exports.registerHelper("hint", "clike", JavaHintHelper.create());

        NodeListOf<?> elems = net.java.html.lib.dom.Exports.document.getElementsByClassName("codeonline");

        // Copy the list because we will be removing from it.
        int numElems = elems.length().intValue();
        Element[] elemsCopy = new Element[numElems];
        for(int i = 0; i < numElems; i++) {
            elemsCopy[i] = Element.$as(elems.$get(i));
        }

        // Replace each element with an interactive editor.
        for(Element element : elemsCopy) {
            Editor.from(element, services);
        }
    }

    public static void onDesktopPageLoad() throws Exception {
        onPageLoad(new DesktopServices());
    }

    public static String executeTask(String request, PlatformServices platformServices) {
        // TODO use JSON, detect class name
        if(request.startsWith("/")) {
            String[] split = request.split("/", 3);
            int pos = Integer.parseInt(split[1]);
            String source = split[2];
            Compilation c = new Compilation();
            JavaFileManagerImpl files = new JavaFileManagerImpl.Builder(platformServices)
                    .addSource("Main", source)
                    .build();
            c.setFiles(files);
            boolean success = c.completion(pos);
            return CompletionListModel.createCompletionList(success, c.getCompletions()).toString();
        }
        String source = request;
        Compilation c = new Compilation();
        JavaFileManagerImpl files = new JavaFileManagerImpl.Builder(platformServices)
                .addSource("Main", source)
                .build();
        c.setFiles(files);
        boolean success = c.compile();
        return CompilationResultModel.createCompilationResult(success, c.getDiagnostics()).toString();
    }

    private static final class DesktopServices extends PlatformServices {
        @Override
        public InputStream openExternalResource(String name) throws IOException {
            return new FileInputStream(Paths.get("target", "extres", name).toFile());
        }

        @Override
        public TaskQueue<String, String> getWorkerQueue() {
            return workerQueue;
        }

        private final TaskQueue<String, String> workerQueue = new TaskQueue<String, String>() {
            private final Executor uiExecutor = BrwsrCtx.findDefault(Main.class);
            private final Executor workerExecutor = Executors.newSingleThreadExecutor();

            @Override
            protected void sendTask(String request) {
                PlatformServices platformServices = DesktopServices.this;
                workerExecutor.execute(() -> {
                    String response = executeTask(request, platformServices);
                    uiExecutor.execute(() -> onResponse(response));
                });
            }
        };
    }
}
