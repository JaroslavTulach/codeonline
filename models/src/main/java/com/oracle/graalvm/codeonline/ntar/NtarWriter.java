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

package com.oracle.graalvm.codeonline.ntar;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Writes a {@link com.oracle.graalvm.codeonline.ntar ntar} archive to an {@link OutputStream}.
 */
public final class NtarWriter implements AutoCloseable {
    private final OutputStream os;

    public NtarWriter(OutputStream os) {
        this.os = new BufferedOutputStream(os);
    }

    private void write(byte[] bytes) throws IOException {
        int size = bytes.length;
        os.write(size >> 0);
        os.write(size >> 8);
        os.write(size >> 16);
        os.write(size >> 24);
        os.write(bytes);
    }

    public void put(String name, byte[] contents) throws IOException {
        write(name.getBytes());
        write(contents);
    }

    public void close() throws IOException {
        os.close();
    }
}
