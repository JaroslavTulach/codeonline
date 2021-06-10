package com.oracle.graalvm.codeonline.js;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Executed during build to download CodeMirror via HTTPS.
 */
public class DownloadLibraries {
    private static final String DOWNLOAD_URL = "https://codemirror.net/codemirror-5.61.1.zip";
    private static final Collection<String> ENTRIES_TO_EXTRACT = Collections.unmodifiableCollection(Arrays.asList(
            "codemirror-5.61.1/lib/codemirror.js",
            "codemirror-5.61.1/lib/codemirror.css",
            "codemirror-5.61.1/addon/hint/show-hint.js",
            "codemirror-5.61.1/addon/hint/show-hint.css",
            "codemirror-5.61.1/mode/clike/clike.js"
    ));

    public static void main(String[] args) throws IOException {
        if(args.length != 1) {
            throw new IllegalArgumentException();
        }
        Path targetDir = Paths.get(args[0]);
        if(!targetDir.toFile().mkdirs()) {
            System.out.println("DownloadLibraries: target directory already exists, skipping");
            return;
        }
        HashSet<String> remaining = new HashSet<>(ENTRIES_TO_EXTRACT);
        ZipInputStream zip = new ZipInputStream(new URL(DOWNLOAD_URL).openStream());
        do {
            ZipEntry entry = zip.getNextEntry();
            if(entry == null) {
                // reached end of ZIP without finding all entries (`remaining` is not empty)
                throw new NoSuchElementException("Missing ZIP entries: " + remaining);
            }
            String name = entry.getName();
            if(!remaining.remove(name)) {
                // was not in `remaining`
                continue;
            }
            String baseName = name.substring(name.lastIndexOf('/') + 1);
            Path target = targetDir.resolve(baseName);
            System.out.println("DownloadLibraries: extracting " + baseName);
            Files.copy(zip, target);
        } while(!remaining.isEmpty());
    }
}
