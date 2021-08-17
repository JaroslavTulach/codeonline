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

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.function.Consumer;

public abstract class TaskQueue<Q, R> {
    private final Queue<Task> queue = new ArrayDeque<>();
    private Consumer<R> currentCallback = null;

    public final Task enqueue(Q request, Consumer<R> callback) {
        if(callback == null) {
            throw new NullPointerException("Callback must not be null");
        }
        if(currentCallback == null) {
            currentCallback = callback;
            sendTask(request);
            return new Task<Q, R>(null, null);
        } else {
            Task<Q, R> task = new Task<>(request, callback);
            queue.add(task);
            return task;
        }
    }

    protected final void onResponse(R response) {
        Consumer<R> callback = currentCallback;
        if(queue.isEmpty()) {
            currentCallback = null;
        } else {
            Task<Q, R> task = queue.remove();
            currentCallback = task.callback;
            sendTask(task.request);
            task.markSent();
        }
        callback.accept(response);
    }

    protected abstract void sendTask(Q request);

    public static final class Task<Q, R> {
        private Q request;
        private Consumer<R> callback;

        private Task(Q request, Consumer<R> callback) {
            this.request = request;
            this.callback = callback;
        }

        public boolean isSent() {
            return callback == null;
        }

        public void update(Q newRequest) {
            if(isSent())
                throw new IllegalStateException("Task already sent");
            request = newRequest;
        }

        private void markSent() {
            request = null;
            callback = null;
        }
    }
}
