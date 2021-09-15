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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

public class DownloadLibraries {
    public static void main(String[] args) throws IOException {
        OutputStream currentOutput = new OutputStream() {
            @Override
            public void write(int i) {
                throw new IllegalStateException("Usage: DownloadLibraries [<outputFile>: [<inputUrl>]... ]...");
            }
        };
        try {
            for(String arg : args) {
                System.out.println("[DownloadLibraries] " + arg);
                if(arg.endsWith(":")) {
                    // new outputFile
                    currentOutput.close();
                    File newOutput = new File(arg.substring(0, arg.length() - 1)); // remove colon
                    newOutput.getParentFile().mkdirs();
                    currentOutput = new FileOutputStream(newOutput);
                } else {
                    // inputUrl
                    try(InputStream in = new URL(arg).openStream()) {
                        InputStreams.transferTo(in, currentOutput);
                    }
                }
            }
        } finally {
            currentOutput.close();
        }
    }
}
