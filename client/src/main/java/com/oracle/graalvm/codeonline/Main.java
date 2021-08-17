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

import com.oracle.graalvm.codeonline.editor.TaskQueue;
import com.oracle.graalvm.codeonline.editor.Editor;
import com.oracle.graalvm.codeonline.editor.EditorParams;
import net.java.html.lib.dom.Element;
import net.java.html.lib.dom.NodeListOf;

public final class Main {
    private Main() {
        throw new UnsupportedOperationException();
    }

    public static void onPageLoad(TaskQueue<String, String> queue) throws Exception {
        EditorParams params = new EditorParams(queue);

        NodeListOf<?> elems = net.java.html.lib.dom.Exports.document.getElementsByClassName("codeonline");

        // Copy the list because we will be removing from it.
        int numElems = elems.length().intValue();
        Element[] elemsCopy = new Element[numElems];
        for(int i = 0; i < numElems; i++) {
            elemsCopy[i] = Element.$as(elems.$get(i));
        }

        // Replace each element with an interactive editor.
        for(Element element : elemsCopy) {
            Editor.from(element, params);
        }
    }
}
