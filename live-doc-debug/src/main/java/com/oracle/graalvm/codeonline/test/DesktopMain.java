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

package com.oracle.graalvm.codeonline.test;

import com.oracle.graalvm.codeonline.LiveDoc;
import com.oracle.graalvm.codeonline.editor.TaskQueue;
import com.oracle.graalvm.codeonline.compiler.common.PlatformServices;
import com.oracle.graalvm.codeonline.compiler.nbjavac.NBJavac;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import net.java.html.BrwsrCtx;
import net.java.html.boot.BrowserBuilder;
import org.netbeans.html.context.spi.Contexts;
import org.netbeans.html.json.spi.Transfer;
import org.netbeans.html.wstyrus.TyrusContext;

/**
 * Desktop client entry point.
 */
public final class DesktopMain {
    private DesktopMain() {
        throw new UnsupportedOperationException();
    }

    public static void main(String... args) throws Exception {
        BrowserBuilder.newBrowser().
            loadPage("index.html").
            loadClass(DesktopMain.class).
            invoke("onDesktopPageLoad", args).
            showAndWait();
        System.exit(0);
    }

    public static void onDesktopPageLoad() throws Exception {
        PlatformServices platformServices = new DesktopServices();
        NBJavac compiler = new NBJavac();
        TaskQueue<String, String> workerQueue = new TaskQueue<String, String>() {
            private final BrwsrCtx uiExecutor = BrwsrCtx.findDefault(DesktopMain.class);
            private final BrwsrCtx workerExecutor = Contexts.newBuilder().
                    register(Executor.class, Executors.newSingleThreadExecutor(), 99).
                    register(Transfer.class, new TyrusContext(), 198).
                    build();

            @Override
            protected void sendTask(String request) {
                workerExecutor.execute(() -> {
                    String response = compiler.handleRequest(platformServices, request);
                    uiExecutor.execute(() -> onResponse(response));
                });
            }
        };
        LiveDoc.onPageLoad(workerQueue);
    }

    private static final class DesktopServices implements PlatformServices {
        @Override
        public InputStream openExternalResource(String name) throws IOException {
            return getClass().getResourceAsStream("/extres/" + name);
        }
    }
}
