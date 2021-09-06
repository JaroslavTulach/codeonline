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

package com.oracle.graalvm.codeonline.compiler.auto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;

public final class GeneratedSource {
    public static final int NOPOS = -1;

    private final StringBuilder sb;
    private final int[] rangeOutputStarts;
    private final SourceRange[] ranges;
    private final char[] source;

    private int rangeIndex;

    private GeneratedSource(int size, int rangeCount, char[] source) {
        this.sb = new StringBuilder(size);
        this.rangeOutputStarts = new int[rangeCount];
        this.ranges = new SourceRange[rangeCount];
        this.source = source;
    }

    private GeneratedSource(String src) {
        this.sb = new StringBuilder(src);
        this.rangeOutputStarts = new int[1];
        this.ranges = new SourceRange[] { new SourceRange(0, src.length()) };
        this.source = null;
    }

    public static GeneratedSource from(String src, String imports) {
        try {
            final String GLOBAL_TO_LOCAL = "\nclass _CodeOnlineMain { void main() {\n";
            final String LOCAL_TO_MEMBER = "\n}\n";
            final String MEMBER_TO_GLOBAL = "\n}\n";
            char[] chars = src.toCharArray();
            ArrayList<SourceRange> header = new ArrayList<>();
            ArrayList<SourceRange> global = new ArrayList<>();
            ArrayList<SourceRange> member = new ArrayList<>();
            ArrayList<SourceRange> local = new ArrayList<>();
            SourceRange rest = new Parser(chars).scan(header, global, member, local);
            int totalLength = chars.length + imports.length() + GLOBAL_TO_LOCAL.length() + LOCAL_TO_MEMBER.length() + MEMBER_TO_GLOBAL.length();
            int rangeCount = header.size() + global.size() + member.size() + local.size();
            if(rest != null)
                rangeCount++;
            GeneratedSource result = new GeneratedSource(totalLength, rangeCount, chars);
            result.addAll(header);
            result.append(imports);
            result.addAll(global);
            result.append(GLOBAL_TO_LOCAL);
            result.addAll(local);
            result.append(LOCAL_TO_MEMBER);
            result.addAll(member);
            result.append(MEMBER_TO_GLOBAL);
            if(rest != null)
                result.add(rest);
            return result;
        } catch(Throwable t) {
            t.printStackTrace();
            return new GeneratedSource(src);
        }
    }

    private void append(String s) {
        sb.append(s);
    }

    private void add(SourceRange range) {
        rangeOutputStarts[rangeIndex] = sb.length();
        ranges[rangeIndex] = range;
        sb.append(source, range.start, range.end - range.start);
        rangeIndex++;
    }

    private void addAll(Iterable<SourceRange> ranges) {
        for(SourceRange range : ranges)
            add(range);
    }

    public String generate() {
        assert rangeIndex == ranges.length;
        return sb.toString();
    }

    public int genOffsetFromOrig(int inputOffset) {
        for(int i = 0; i < ranges.length; i++) {
            if(ranges[i].contains(inputOffset))
                return rangeOutputStarts[i] + inputOffset - ranges[i].start;
        }
        throw new IndexOutOfBoundsException();
    }

    private int origOffsetFromGen(int outputOffset) {
        int idx = Arrays.binarySearch(rangeOutputStarts, outputOffset);
        if(idx == -1)
            return 0; // before first element
        int sign = idx >> 31; // 0 or -1
        idx ^= sign;
        idx += sign;
        assert rangeOutputStarts[idx] <= outputOffset;
        assert idx + 1 == rangeOutputStarts.length || rangeOutputStarts[idx + 1] > outputOffset;
        SourceRange range = ranges[idx];
        return Math.min(outputOffset - rangeOutputStarts[idx] + range.start, range.end);
    }

    private <T> Diagnostic<T> convertDiagnostic(Diagnostic<T> diag) {
        return new Diagnostic<T>() {
            @Override
            public Diagnostic.Kind getKind() {
                return diag.getKind();
            }

            @Override
            public T getSource() {
                return diag.getSource();
            }

            @Override
            public long getPosition() {
                return origOffsetFromGen((int) diag.getPosition());
            }

            @Override
            public long getStartPosition() {
                return origOffsetFromGen((int) diag.getStartPosition());
            }

            @Override
            public long getEndPosition() {
                return origOffsetFromGen((int) diag.getEndPosition());
            }

            @Override
            public long getLineNumber() {
                return NOPOS;
            }

            @Override
            public long getColumnNumber() {
                return NOPOS;
            }

            @Override
            public String getCode() {
                return diag.getCode();
            }

            @Override
            public String getMessage(Locale locale) {
                return diag.getMessage(locale);
            }
        };
    }

    public DiagnosticListener<JavaFileObject> diagsConverter(DiagnosticListener<JavaFileObject> origListener) {
        return diag -> origListener.report(convertDiagnostic(diag));
    }
}
