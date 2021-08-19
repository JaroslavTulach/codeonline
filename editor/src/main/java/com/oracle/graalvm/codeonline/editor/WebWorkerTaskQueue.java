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

import net.java.html.lib.Function;
import net.java.html.lib.Objs;
import net.java.html.lib.dom.EventListener;
import net.java.html.lib.dom.MessageEvent;
import net.java.html.lib.dom.Worker;

public class WebWorkerTaskQueue extends TaskQueue<String, String> {
    private final Worker worker;

    public WebWorkerTaskQueue(Worker worker) {
        worker.addEventListener("message", EventListener.$as((Function.A1) event -> {
            onResponse(Objs.$as(String.class, MessageEvent.$as(event).data.get()));
            return null;
        }));
        this.worker = worker;
    }

    @Override
    protected void sendTask(String request) {
        worker.postMessage(request);
    }
}
