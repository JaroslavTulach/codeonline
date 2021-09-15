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

package com.oracle.graalvm.codeonline.build;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public final class InputStreams {
    private InputStreams() {
        throw new UnsupportedOperationException();
    }

    public static byte[] readAllBytes(InputStream in) throws IOException {
        byte[] contents = new byte[1024];
        int offset = 0;
        for(;;) {
            int size = in.read(contents, offset, contents.length - offset);
            if(size == -1)
                return Arrays.copyOf(contents, offset);
            offset += size;
            if(offset * 2 > contents.length)
                contents = Arrays.copyOf(contents, contents.length * 2);
        }
    }

    public static void transferTo(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer, 0, buffer.length)) >= 0)
            out.write(buffer, 0, read);
    }
}
