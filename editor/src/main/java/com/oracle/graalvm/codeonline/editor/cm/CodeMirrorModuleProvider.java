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

package com.oracle.graalvm.codeonline.editor.cm;

import net.java.html.js.JavaScriptBody;
import net.java.html.js.JavaScriptResource;
import net.java.html.lib.Exports;
import net.java.html.lib.Modules;
import net.java.html.lib.Objs;

@JavaScriptResource("all.js")
final class CodeMirrorModuleProvider extends Modules.Provider {
    private CodeMirrorModuleProvider() {}

    @Override
    protected Objs find(String name) {
        if(name.equals("CodeMirror") || name.equals("ShowHint"))
            return Exports.$as(Exports.eval("CodeMirror"));
        return null;
    }

    public static void register() {
        initializeCodeMirror();
        new CodeMirrorModuleProvider();
    }

    @JavaScriptBody(args = {}, body = "")
    private static native void initializeCodeMirror();
}
