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

import com.oracle.graalvm.codeonline.editor.cm.ShowHint.Hints;
import com.oracle.graalvm.codeonline.editor.cm.ShowHint.ShowHintOptions;
import java.util.function.Consumer;

@FunctionalInterface
public interface HintProvider {
    public void getHints(Consumer<Hints> callback, ShowHintOptions options);
}
