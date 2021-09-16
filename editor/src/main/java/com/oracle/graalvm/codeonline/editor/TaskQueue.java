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

/**
 * A simple queue of <em>mutable</em> tasks.
 * Users/clients post tasks to receive results, implementers process tasks (see below).
 * This class is <em>not</em> thread-safe, it must be created in and only accessed from the client thread.
 * A task may be in three states: queued, sent (being processed), completed.
 * At most one task is being processed at any time.
 * <p>
 * An user of this class (e.g. UI) calls {@link #enqueue(Object, Consumer)},
 * passing the request and a callback, and in return it receives a handle to a queued or sent task
 * (depending on whether the worker is busy).
 * The handle can be used to further modify the task, but only while it is queued.
 * Once the task is completed, the callback is called.
 * <p>
 * An implementation of this class is responsible for asynchronously processing the tasks.
 * It receives tasks by implementing {@link #sendTask(Object)}
 * and returns results by calling {@link #onResponse(Object)}.
 * <p>
 * Null request objects and null response objects have no special meaning.
 * @param <Q> the type of request objects
 * @param <R> the type of response objects
 */
public abstract class TaskQueue<Q, R> {
    private final Queue<Task> queue = new ArrayDeque<>();
    private Consumer<R> currentCallback = null;

    /**
     * Adds a new task to the queue or sends it immediately if all previous tasks were processed.
     * @param request the request object
     * @param callback must not be null, the response will be passed to it on completion
     * @return a new object, representing the task
     */
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

    /**
     * Implementation should call this method when a task is completed.
     * It must be called exactly once per task.
     * The call must happen on the same thread as all other accesses to this class.
     * @param response the result of the task
     */
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

    /**
     * Implementation receives requests for processing through this method.
     * The call happens on the same thread as all other accesses to this class.
     * @param request the request object from the client
     */
    protected abstract void sendTask(Q request);

    /**
     * A handle used by the client to refer to a task.
     * Objects of this class are not passed to or used by the implementations.
     * @param <Q> the type of request objects
     * @param <R> the type of response objects
     */
    public static final class Task<Q, R> {
        private Q request;
        private Consumer<R> callback;

        private Task(Q request, Consumer<R> callback) {
            this.request = request;
            this.callback = callback;
        }

        /**
         * Checks whether a task was already sent (i.e. it is being processed or completed).
         * @return true if the task is sent or completed, false if it is queued
         */
        public boolean isSent() {
            return callback == null;
        }

        /**
         * Replaces the request object with another.
         * This method may be called if and only if {@link #isSent()} returns false.
         * @param newRequest the request object to be delivered instead
         */
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
