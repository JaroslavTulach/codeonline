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

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Reads a {@link com.oracle.graalvm.codeonline.ntar ntar} archive from an {@link InputStream}.
 */
public final class NtarReader implements Iterable<NtarReader.Entry>, AutoCloseable {
    private final InputStream is;
    private final byte[] sizeBuf = new byte[4];

    public NtarReader(InputStream is) {
        this.is = is;
    }

    private boolean readSize() throws IOException {
        int off = 0;
        do {
            int read = is.read(sizeBuf, off, 4 - off);
            if(read == -1) {
                if(off == 0)
                    return false;
                else
                    throw new NtarException("Unexpected EOF");
            }
            off += read;
        } while(off < 4);
        return true;
    }

    private byte[] read() throws IOException {
        @SuppressWarnings("PointlessBitwiseExpression")
        int size = (sizeBuf[0] & 0xff) << 0 | (sizeBuf[1] & 0xff) << 8 | (sizeBuf[2] & 0xff) << 16 | (sizeBuf[3] & 0xff) << 24;
        byte[] buf = new byte[size];
        int off = 0;
        while(off < size) {
            int read = is.read(buf, off, size - off);
            if(read == -1)
                throw new NtarException("Unexpected EOF");
            off += read;
        }
        return buf;
    }

    @Override
    public Iterator<Entry> iterator() {
        return new Iterator<Entry>() {
            boolean askedNext, hasNext;

            @Override
            public boolean hasNext() {
                if(!askedNext) {
                    askedNext = true;
                    try {
                        hasNext = readSize();
                    } catch (IOException ex) {
                        throw new NtarException(ex);
                    }
                }
                return hasNext;
            }

            @Override
            public Entry next() {
                if(!hasNext())
                    throw new NoSuchElementException();
                askedNext = false;
                try {
                    String name = new String(read());
                    readSize();
                    byte[] content = read();
                    return new Entry(name, content);
                } catch (IOException ex) {
                    throw new NtarException(ex);
                }
            }
        };
    }

    @Override
    public void close() {
        try {
            is.close();
        } catch (IOException ex) {
            throw new NtarException(ex);
        }
    }

    public static final class Entry {
        public final String name;
        public final byte[] content;

        private Entry(String name, byte[] content) {
            this.name = name;
            this.content = content;
        }
    }

    public static final class NtarException extends RuntimeException {
        public NtarException(String message) {
            super(message);
        }

        public NtarException(Exception exception) {
            super(exception);
        }
    }
}
