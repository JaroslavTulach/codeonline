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

public abstract class ParserBase {
    protected static final int EOF = -1;

    private final char[] source;
    private int nextIndex;

    protected ParserBase(char[] source) {
        this.source = source;
    }

    protected final int getIndex() {
        return nextIndex;
    }

    protected final int scanChar(boolean allowEOF) {
        int c = scanRawChar(allowEOF);
        switch(c) {
            case '/':
                return scanRawComment(allowEOF);
            case '"':
            case '\'':
                for(;;) {
                    int d = scanRawChar();
                    if(d == c)
                        return '#';
                    if(d == '\\')
                        nextIndex++;
                }
            default:
                return c;
        }
    }

    private int scanRawComment(boolean allowEOF) {
        // was '/'
        int backup = nextIndex;
        switch(scanRawChar()) {
            case '/':
                for(;;) {
                    int c;
                    switch(c = scanRawChar(allowEOF)) {
                        case '\n':
                        case EOF:
                            return c;
                    }
                }
            case '*':
                int c;
                do {
                    while(scanRawChar() != '*');
                    while((c = scanRawChar()) == '*');
                } while(c != '/');
                return ' ';
            default:
                nextIndex = backup;
                return '/';
        }
    }

    private int scanRawChar() {
        char c = getRawChar(nextIndex++);
        if(c == '\\' && getRawChar(nextIndex) == 'u') {
            while(getRawChar(++nextIndex) == 'u');
            int d3 = scanRawHexDigit();
            int d2 = scanRawHexDigit();
            int d1 = scanRawHexDigit();
            int d0 = scanRawHexDigit();
            return Math.max(d3 * 0x1000 + d2 * 0x100 + d1 * 0x10 + d0, 0);
        }
        return c;
    }

    private int scanRawChar(boolean allowEOF) {
        if(allowEOF && nextIndex == source.length)
            return EOF;
        return scanRawChar();
    }

    private int scanRawHexDigit() {
        char d = getRawChar(nextIndex++);
        if(d >= '0' && d <= '9')
            return d - '0';
        if(d >= 'a' && d <= 'f')
            return d - 'a' + 10;
        if(d >= 'A' && d <= 'F')
            return d - 'A' + 10;
        return -0x10000;
    }

    private char getRawChar(int index) {
        if(index >= source.length)
            throw Eof.INSTANCE;
        return source[index];
    }

    protected static final class Eof extends RuntimeException {
        private static final Eof INSTANCE = new Eof();

        private Eof() {
            setStackTrace(new StackTraceElement[0]);
        }
    }
}
