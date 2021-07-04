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

package com.oracle.graalvm.codeonline.files;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

/**
 * Used by {@link JavaFileManagerImpl} to represent a file.
 */
abstract class FileContents {
    private long lastModified;
    {
        touch();
    }

    protected final void touch() {
        lastModified = System.currentTimeMillis();
    }

    public abstract InputStream openInputStream();
    public abstract OutputStream openOutputStream();
    public abstract Reader openReader();
    public abstract CharSequence getCharContent();
    public abstract Writer openWriter();

    public final long lastModified() {
        return lastModified;
    }
}
