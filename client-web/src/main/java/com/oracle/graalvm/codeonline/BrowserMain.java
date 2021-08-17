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
import java.net.URL;
import java.util.function.Consumer;
import java.util.function.Function;
import net.java.html.js.JavaScriptBody;

/**
 * Web browser entry point.
 */
public class BrowserMain {
    private BrowserMain() {
        throw new UnsupportedOperationException();
    }

    public static void main(String... args) throws Exception {
        if(isMainThread()) {
            TaskQueue<String, String> workerQueue = new TaskQueue<String, String>() {
                {
                    registerWorkerCallback(this::onResponse);
                }

                @Override
                protected void sendTask(String request) {
                    BrowserMain.sendTask(request);
                }
            };
            Main.onPageLoad(workerQueue);
        } else {
            WebWorkerServices.workerMain(request -> Main.executeTask(request, new WebWorkerServices()));
        }
    }

    @JavaScriptBody(args = {}, body = "return 'window' in self;")
    private static native boolean isMainThread();

    @JavaScriptBody(args = {"c"}, body = "window.codeonlineWorker.onmessage = function(event) { c.@java.util.function.Consumer::accept(Ljava/lang/Object;)(event.data); };", javacall = true)
    static native void registerWorkerCallback(Consumer<String> c);

    @JavaScriptBody(args = {"request"}, body = "window.codeonlineWorker.postMessage(request);")
    static native void sendTask(String request);

    private static final class WebWorkerServices extends PlatformServices {
        @Override
        public InputStream openExternalResource(String name) throws IOException {
            // Relative URL, but has to specify protocol so that the correct handler is used.
            // TODO detect HTTPS
            return new URL("http:extres/" + name).openStream();
        }

        @JavaScriptBody(args = {"f"}, body = "self.onmessage = function(event) { self.postMessage(f.@java.util.function.Function::apply(Ljava/lang/Object;)(event.data)); };", javacall = true)
        static native boolean workerMain(Function<String, String> f);
    }
}
