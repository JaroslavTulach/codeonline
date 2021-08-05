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

import com.oracle.graalvm.codeonline.ntar.NtarWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.tools.JavaFileManager.Location;
import javax.tools.StandardLocation;

/**
 * Executed during build to put all requested classes to ZIP archives grouped by Java package.
 */
public final class PrepareClassPath {
    private PrepareClassPath() {
        throw new UnsupportedOperationException();
    }

    public static void main(String[] args) throws IOException {
        File outputDir = new File(args[0]);
        File platformClassPath = getLibRtJar();
        File[] classPath = Arrays.stream(args).skip(1).map(File::new).toArray(File[]::new);
        outputDir.mkdirs();
        try(PrintStream printStream = new PrintStream(new File(outputDir, "available.txt"))) {
            processPackages(outputDir, StandardLocation.PLATFORM_CLASS_PATH, platformClassPath, printStream::println);
            for(File classPathElem : classPath) {
                processPackages(outputDir, StandardLocation.CLASS_PATH, classPathElem, printStream::println);
            }
        }
    }

    private static File getLibRtJar() {
        String javaHome = System.getProperty("java.home");
        // use version 8 JDK, since newer JDKs do not seem to have lib/rt.jar
        // TODO fix for newer JDKs and remove this
        javaHome = "/usr/lib/jvm/java-8-openjdk";
        File libRtJar = Paths.get(javaHome, "jre", "lib", "rt.jar").toFile();
        if(libRtJar.exists())
            return libRtJar;
        else
            return Paths.get(javaHome, "lib", "rt.jar").toFile();
    }

    private static void processPackages(File outDir, Location location, File inFile, Consumer<String> outputFileList) throws IOException {
        HashMap<String, HashMap<String, byte[]>> packages = new HashMap<>();
        try(ZipInputStream in = new ZipInputStream(new FileInputStream(inFile))) {
            for(;;) {
                ZipEntry entry = in.getNextEntry();
                if(entry == null)
                    break;
                if(entry.isDirectory())
                    continue;
                String entName = entry.getName();
                final String SUFFIX = ".class";
                if(!entName.endsWith(SUFFIX))
                    continue;
                int suffixIndex = entName.length() - SUFFIX.length();
                String packageName;
                String simpleName;
                int lastSlash = entName.lastIndexOf("/");
                if(lastSlash == -1) {
                    packageName = "";
                    simpleName = entName.substring(0, suffixIndex);
                } else {
                    packageName = entName.substring(0, lastSlash);
                    simpleName = entName.substring(lastSlash + 1, suffixIndex);
                }
                byte[] contents = MethodBodyEraser.eraseMethodBodies(InputStreams.readAllBytes(in));
                HashMap<String, byte[]> classes = packages.computeIfAbsent(packageName, ignoredPackageName -> new HashMap<>());
                classes.putIfAbsent(simpleName, contents);
            }
        }
        for(Map.Entry<String, HashMap<String, byte[]>> packageClasses : packages.entrySet()) {
            String packageName = packageClasses.getKey().replace('/', '.');
            HashMap<String, byte[]> classes = packageClasses.getValue();
            String outFileName = location + "-" + packageName + ".zip";
            outputFileList.accept(outFileName);
            File outFile = new File(outDir, outFileName);
            try(NtarWriter out = new NtarWriter(new FileOutputStream(outFile))) {
                for(Map.Entry<String, byte[]> entry : classes.entrySet()) {
                    String simpleName = entry.getKey();
                    byte[] contents = entry.getValue();
                    out.put(simpleName, contents);
                }
            }
        }
    }
}
