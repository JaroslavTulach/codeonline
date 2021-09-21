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

import com.oracle.graalvm.codeonline.ntar.NtarReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

public abstract class PlatformServices {
    private final HashMap<String, Iterable<NtarReader.Entry>> cache = new HashMap<>();

    protected abstract InputStream openExternalResource(String name) throws IOException;

    // Used by JavaFileManagerImpl, this method returns mutable objects
    // and expects them not to be mutated, therefore it should not be public.
    Iterable<NtarReader.Entry> loadArchive(String fileName) throws IOException {
        Iterable<NtarReader.Entry> result = cache.get(fileName);
        if(result == null) {
            result = loadArchiveUncached(fileName);
            cache.put(fileName, result);
        }
        return result;
    }

    private Iterable<NtarReader.Entry> loadArchiveUncached(String fileName) throws IOException {
        ArrayList<NtarReader.Entry> result = new ArrayList<>();
        try(NtarReader zip = new NtarReader(openExternalResource(fileName))) {
            for(NtarReader.Entry entry : zip)
                result.add(entry);
        }
        return result;
    }
}
