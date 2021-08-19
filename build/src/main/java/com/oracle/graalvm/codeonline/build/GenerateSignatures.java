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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.tools.JavaFileManager.Location;
import javax.tools.StandardLocation;

/**
 * Executed during build to put all requested classes to ZIP archives grouped by Java package.
 */
public final class GenerateSignatures {
    private GenerateSignatures() {
        throw new UnsupportedOperationException();
    }

    public static void main(String[] args) throws IOException {
        if(args.length != 4) {
            throw new IllegalArgumentException("Usage: GenerateSignatures ${project.packaging} <outputDir> ${org.frgaal:compiler:jar} ${codeonline.classpath}");
        }
        if(args[0].equals("pom")) {
            System.out.println("This is a POM project, skipping GenerateSignatures");
            return;
        }
        File outputDir = new File(args[1]);
        File platformClassPath = new File(args[2]);
        File[] userClassPath;
        if(args[3] == null || args[3].isEmpty()) {
            System.out.println("Warning: ${codeonline.classpath} was empty. Only Java library classes will be available for code completion and highlighting.");
            System.out.println("To enable access to your library, specify <codeonline.classpath>...</codeonline.classpath> in your project <properties>.");
            userClassPath = new File[0];
        } else {
            userClassPath = Arrays.stream(args[3].split(File.pathSeparator)).map(File::new).toArray(File[]::new);
        }
        outputDir.mkdirs();
        try(PrintStream printStream = new PrintStream(new File(outputDir, "available.txt"))) {
            writePackages(outputDir, StandardLocation.PLATFORM_CLASS_PATH, readCtSym(platformClassPath), printStream::println);
            for(File classPathElem : userClassPath) {
                writePackages(outputDir, StandardLocation.CLASS_PATH, readJar(classPathElem), printStream::println);
            }
        }
    }

    private static HashMap<String, HashMap<String, byte[]>> readJar(File inFile) throws IOException {
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
        return packages;
    }

    private static HashMap<String, HashMap<String, byte[]>> readCtSym(File inFile) throws IOException {
        Pattern entNamePattern = Pattern.compile("META-INF/ct[.]sym/@8@/@/(.*)/(@)[.]class".replace("@", "[^/-]*"));
        HashMap<String, HashMap<String, byte[]>> packages = new HashMap<>();
        try(ZipInputStream in = new ZipInputStream(new FileInputStream(inFile))) {
            for(;;) {
                ZipEntry entry = in.getNextEntry();
                if(entry == null)
                    break;
                if(entry.isDirectory())
                    continue;
                String entName = entry.getName();
                Matcher m = entNamePattern.matcher(entName);
                if(!m.matches())
                    continue;
                String packageName = m.group(1);
                String simpleName = m.group(2);
                byte[] contents = MethodBodyEraser.eraseMethodBodies(InputStreams.readAllBytes(in));
                HashMap<String, byte[]> classes = packages.computeIfAbsent(packageName, ignoredPackageName -> new HashMap<>());
                classes.putIfAbsent(simpleName, contents);
            }
        }
        return packages;
    }

    private static void writePackages(File outDir, Location location, HashMap<String, HashMap<String, byte[]>> packages, Consumer<String> outputFileList) throws IOException {
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
