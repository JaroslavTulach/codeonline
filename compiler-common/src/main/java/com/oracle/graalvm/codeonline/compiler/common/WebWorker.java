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

package com.oracle.graalvm.codeonline.compiler.common;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import net.java.html.lib.Function;
import net.java.html.lib.Objs;
import net.java.html.lib.dom.EventListener;
import net.java.html.lib.dom.Exports;
import net.java.html.lib.dom.MessageEvent;

public final class WebWorker {
    public static void main(Compiler compiler) {
        WebWorkerServices platformServices = new WebWorkerServices(Exports.location.protocol() + "extres/");
        Exports.addEventListener("message", EventListener.$as((Function.A1) event -> {
            String messageData = Objs.$as(String.class, MessageEvent.$as(event).data.get());
            Exports.postMessage(compiler.handleRequest(platformServices, messageData), null);
            return null;
        }));
    }

    private static final class WebWorkerServices extends PlatformServices {
        private final String externalResourcesBase;

        WebWorkerServices(String externalResourcesBase) {
            this.externalResourcesBase = externalResourcesBase;
        }

        @Override
        public InputStream openExternalResource(String name) throws IOException {
            return new URL(externalResourcesBase + name).openStream();
        }
    }
}
