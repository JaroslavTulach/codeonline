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
        net.java.html.lib.codemirror.CodeMirror.Exports.registerHelper("hint", "clike", new JavaHintHelper());

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

    public static Object executeTask(Object request, PlatformServices platformServices) {
        String source = (String) request;
        Compilation c = new Compilation();
        JavaFileManagerImpl files = new JavaFileManagerImpl.Builder(platformServices)
                .addSource("Main", source)
                .build();
        c.setFiles(files);
        c.compile();
        return c.getDiagnostics();
    }

    private static final class DesktopServices extends PlatformServices {
        @Override
        public InputStream openExternalResource(String name) throws IOException {
            return new FileInputStream(Paths.get("target", "extres", name).toFile());
        }

        @Override
        public TaskQueue<Object, Object> getWorkerQueue() {
            return workerQueue;
        }

        private final TaskQueue<Object, Object> workerQueue = new TaskQueue<Object, Object>() {
            private final Executor uiExecutor = BrwsrCtx.findDefault(Main.class);
            private final Executor workerExecutor = Executors.newSingleThreadExecutor();

            @Override
            protected void sendTask(Object request) {
                PlatformServices platformServices = DesktopServices.this;
                workerExecutor.execute(() -> {
                    Object response = executeTask(request, platformServices);
                    uiExecutor.execute(() -> onResponse(response));
                });
            }
        };
    }
}
