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
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import net.java.html.BrwsrCtx;
import net.java.html.boot.BrowserBuilder;

/**
 * Desktop client entry point.
 */
public final class DesktopMain {
    private DesktopMain() {
        throw new UnsupportedOperationException();
    }

    public static void main(String... args) throws Exception {
        BrowserBuilder.newBrowser().
            loadPage("pages/index.html").
            loadClass(DesktopMain.class).
            invoke("onDesktopPageLoad", args).
            showAndWait();
        System.exit(0);
    }

    public static void onDesktopPageLoad() throws Exception {
        Main.onPageLoad(new DesktopServices());
    }

    private static final class DesktopServices extends PlatformServices {
        @Override
        public InputStream openExternalResource(String name) throws IOException {
            return getClass().getResourceAsStream("/pages/extres/" + name);
        }

        @Override
        public TaskQueue<String, String> getWorkerQueue() {
            return workerQueue;
        }

        private final TaskQueue<String, String> workerQueue = new TaskQueue<String, String>() {
            private final Executor uiExecutor = BrwsrCtx.findDefault(DesktopMain.class);
            private final Executor workerExecutor = Executors.newSingleThreadExecutor();

            @Override
            protected void sendTask(String request) {
                PlatformServices platformServices = DesktopServices.this;
                workerExecutor.execute(() -> {
                    String response = Main.executeTask(request, platformServices);
                    uiExecutor.execute(() -> onResponse(response));
                });
            }
        };
    }
}
