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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Used by {@link JavaFileManagerImpl} to represent a read-write text file.
 */
final class TextFileContents extends FileContents {
    private final StringWriter contents = new StringWriter() {
        @Override
        public void flush() {
            super.flush();
            touch();
        }

        @Override
        public void close() throws IOException {
            super.close();
            touch();
        }
    };

    @Override
    public InputStream openInputStream() {
        throw new UnsupportedOperationException("This file does not support byte access.");
    }

    @Override
    public OutputStream openOutputStream() {
        throw new UnsupportedOperationException("This file does not support byte access.");
    }

    @Override
    public Reader openReader() {
        return new StringReader(contents.toString());
    }

    @Override
    public CharSequence getCharContent() {
        return contents.toString();
    }

    @Override
    public Writer openWriter() {
        contents.getBuffer().setLength(0);
        return contents;
    }
}
