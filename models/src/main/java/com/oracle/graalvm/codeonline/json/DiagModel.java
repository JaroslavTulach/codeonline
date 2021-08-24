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

package com.oracle.graalvm.codeonline.json;

import java.util.Locale;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import net.java.html.json.Model;
import net.java.html.json.Property;

@Model(className = "Diag", properties = {
    @Property(name = "kind", type = DiagModel.Kind.class),
    @Property(name = "position", type = long.class),
    @Property(name = "startPosition", type = long.class),
    @Property(name = "endPosition", type = long.class),
    @Property(name = "lineNumber", type = long.class),
    @Property(name = "columnNumber", type = long.class),
    @Property(name = "code", type = String.class),
    @Property(name = "message", type = String.class)
})
public final class DiagModel {
    public static final long NOPOS = Diagnostic.NOPOS;

    public enum Kind {
        ERROR,
        WARNING,
        NOTE;

        public static Kind from(Diagnostic.Kind kind) {
            switch(kind) {
                case ERROR:
                    return ERROR;
                case WARNING:
                case MANDATORY_WARNING:
                    return WARNING;
                case NOTE:
                case OTHER:
                    return NOTE;
                default:
                    throw new AssertionError();
            }
        }
    }

    public static Diag createDiag(Diagnostic<? extends JavaFileObject> diag) {
        return new Diag(
                Kind.from(diag.getKind()),
                diag.getPosition(),
                diag.getStartPosition(),
                diag.getEndPosition(),
                diag.getLineNumber(),
                diag.getColumnNumber(),
                diag.getCode(),
                diag.getMessage(Locale.getDefault())
        );
    }
}
