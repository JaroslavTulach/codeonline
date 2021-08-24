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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public final class WriteFile {
    public static void main(String[] args) throws IOException {
        if(args.length < 1) {
            throw new IllegalArgumentException("Usage: WriteFile <outputFile> <content lines ...>");
        }
        Path outputFile = Paths.get(args[0]);
        outputFile.getParent().toFile().mkdirs();
        Files.write(outputFile, Arrays.asList(args).subList(1, args.length), StandardCharsets.UTF_8);
    }
}
