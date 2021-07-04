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

import com.oracle.graalvm.codeonline.js.PlatformServices;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

/**
 * Implements {@link JavaFileManager} using in-memory objects (not backed by files).
 * Class files are loaded as external ZIP files containing individual packages.
 * Text and binary files are not interchangeable.
 * The URIs used to identify the files do <em>not</em> resemble those used by the default file manager.
 */
public final class JavaFileManagerImpl implements JavaFileManager {
    private static final HashSet<String> availablePackageZips = new HashSet<>();

    private final HashMap<String, FileObjectImpl> filesMap;
    private final HashSet<String> loadedPackageZips = new HashSet<>();
    private final PlatformServices platformServices;

    private JavaFileManagerImpl(HashMap<String, FileObjectImpl> filesMap, PlatformServices platformServices) {
        this.filesMap = filesMap;
        this.platformServices = platformServices;
    }

    @Override
    public ClassLoader getClassLoader(Location location) {
        return null;
    }

    @Override
    public Iterable<JavaFileObject> list(Location location, String packageName, Set<JavaFileObject.Kind> kinds, boolean recurse) throws IOException {
        loadPackage(location, packageName);
        String packageNameDot = packageName.isEmpty() ? "" : packageName + ".";
        String[] prefixes = kinds.stream().map(kind -> getJavaFileObjectName(location, packageNameDot, kind)).toArray(String[]::new);
        return filesMap.values().stream().filter(file -> {
            for(String prefix : prefixes) {
                String uri = file.getName();
                if(uri.startsWith(prefix))
                    return recurse || uri.indexOf('.', prefix.length()) == -1;
            }
            return false;
        }).map(JavaFileObject.class::cast).collect(Collectors.toList());
    }

    @Override
    public String inferBinaryName(Location location, JavaFileObject file) {
        String name = file.getName();
        return name.substring(name.indexOf('.') + 1);
    }

    @Override
    public boolean isSameFile(FileObject a, FileObject b) {
        return a == b;
    }

    @Override
    public boolean handleOption(String current, Iterator<String> remaining) {
        return false;
    }

    @Override
    public boolean hasLocation(Location location) {
        return
            location == StandardLocation.CLASS_OUTPUT ||
            location == StandardLocation.CLASS_PATH ||
            location == StandardLocation.SOURCE_PATH ||
            location == StandardLocation.ANNOTATION_PROCESSOR_PATH ||
            location == StandardLocation.PLATFORM_CLASS_PATH;
    }

    @Override
    public JavaFileObject getJavaFileForInput(Location location, String className, JavaFileObject.Kind kind) throws IOException {
        int lastDot = className.lastIndexOf('.');
        String pkg = lastDot == -1 ? "" : className.substring(0, lastDot);
        loadPackage(location, pkg);
        String name = getJavaFileObjectName(location, className, kind);
        return (JavaFileObject) filesMap.get(name);
    }

    @Override
    public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) {
        String name = getJavaFileObjectName(location, className, kind);
        return (JavaFileObject) filesMap.computeIfAbsent(name, uri -> new JavaFileObjectImpl(this, uri, null, kind));
    }

    @Override
    public FileObject getFileForInput(Location location, String packageName, String relativeName) {
        String name = getFileObjectName(location, packageName, relativeName);
        return filesMap.get(name);
    }

    @Override
    public FileObject getFileForOutput(Location location, String packageName, String relativeName, FileObject sibling) {
        String name = getFileObjectName(location, packageName, relativeName);
        return filesMap.computeIfAbsent(name, uri -> new FileObjectImpl(this, uri, null));
    }

    @Override
    public void flush() {
        // nothing to do
    }

    @Override
    public void close() {
        // nothing to do
    }

    @Override
    public int isSupportedOption(String option) {
        return -1;
    }

    private void loadPackage(Location location, String packageName) throws IOException {
        if(location != StandardLocation.CLASS_PATH && location != StandardLocation.PLATFORM_CLASS_PATH)
            return;
        String fileName = location + "-" + packageName + ".zip";
        if(!isPackageAvailable(fileName))
            return;
        if(!loadedPackageZips.add(fileName))
            return;
        String qualification = packageName.isEmpty() ? "" : packageName + '.';
        try(ZipInputStream zip = new ZipInputStream(platformServices.openExternalResource(fileName))) {
            for(;;) {
                ZipEntry entry = zip.getNextEntry();
                if(entry == null)
                    break;
                String entName = entry.getName();
                byte[] contents = zip.readAllBytes();
                //
                JavaFileObject.Kind kind = JavaFileObject.Kind.CLASS;
                String uri = getJavaFileObjectName(location, qualification + entName, kind);
                filesMap.put(uri, new JavaFileObjectImpl(this, uri, new BinaryFileContents(contents), kind));
            }
        } catch(IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static String getJavaFileObjectName(Location location, String className, JavaFileObject.Kind kind) {
        return "jfo:" + location + "/" + kind + "." + className;
    }

    private static String getFileObjectName(Location location, String packageName, String relativeName) {
        return "fo:" + location + "/" + packageName + "/" + relativeName;
    }

    public void debugDump() {
        for(Map.Entry<String, FileObjectImpl> ent : filesMap.entrySet()) {
            if(ent.getKey().startsWith("jfo:PLATFORM_CLASS_PATH/"))
                continue;
            System.out.println(ent.getKey() + " => " + ent.getValue().contents);
        }
    }

    private boolean isPackageAvailable(String requestedZip) {
        if(availablePackageZips.isEmpty()) {
            try(Scanner s = new Scanner(platformServices.openExternalResource("available.txt"))) {
                while(s.hasNextLine())
                    availablePackageZips.add(s.nextLine());
            }
        }
        return availablePackageZips.contains(requestedZip);
    }

    public static final class Builder {
        private final JavaFileManagerImpl result;

        public Builder(PlatformServices platformServices) {
            this.result = new JavaFileManagerImpl(new HashMap<>(), platformServices);
        }

        public Builder addSource(String name, String contents) {
            JavaFileObject.Kind kind = JavaFileObject.Kind.SOURCE;
            String uri = getJavaFileObjectName(StandardLocation.SOURCE_PATH, name, kind);
            result.filesMap.put(uri, new JavaFileObjectImpl(result, uri, new StringFileContents(contents), kind));
            return this;
        }

        public JavaFileManagerImpl build() {
            return result;
        }
    }

    private static class FileObjectImpl implements FileObject {
        private final JavaFileManagerImpl fm;
        private final String uri;
        private FileContents contents;

        FileObjectImpl(JavaFileManagerImpl fm, String uri, FileContents contents) {
            this.fm = fm;
            this.uri = uri;
            this.contents = contents;
        }

        @Override
        public URI toUri() {
            return URI.create(uri);
        }

        @Override
        public String getName() {
            return uri;
        }

        @Override
        public InputStream openInputStream() {
            return contents.openInputStream();
        }

        @Override
        public OutputStream openOutputStream() {
            if(contents == null)
                contents = new BinaryFileContents(null);
            return contents.openOutputStream();
        }

        @Override
        public Reader openReader(boolean bln) {
            return contents.openReader();
        }

        @Override
        public CharSequence getCharContent(boolean bln) {
            return contents.getCharContent();
        }

        @Override
        public Writer openWriter() {
            if(contents == null)
                contents = new TextFileContents();
            return contents.openWriter();
        }

        @Override
        public long getLastModified() {
            return contents.lastModified();
        }

        @Override
        public boolean delete() {
            fm.filesMap.remove(uri);
            return true;
        }
    }

    private static final class JavaFileObjectImpl extends FileObjectImpl implements JavaFileObject {
        private final Kind kind;

        JavaFileObjectImpl(JavaFileManagerImpl fm, String uri, FileContents contents, Kind kind) {
            super(fm, uri, contents);
            this.kind = kind;
        }

        @Override
        public Kind getKind() {
            return kind;
        }

        @Override
        public boolean isNameCompatible(String simpleName, Kind kind) {
            return this.kind.equals(kind) && getName().endsWith("." + simpleName);
        }

        @Override
        public NestingKind getNestingKind() {
            return null;
        }

        @Override
        public Modifier getAccessLevel() {
            return null;
        }
    }
}
