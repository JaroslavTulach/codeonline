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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public final class Parser extends ParserBase {
    private static final int COND_HEADER = 1;
    private static final int COND_MEMBER = 2;
    private static final int COND_TYPE = 3;
    private static final int COND_NOT_LOCAL = 4;
    private static final int COND_LOCAL = 5;
    private static final int COND_ELSE = 6;
    private static final int COND_OTHER = 7;

    private static final byte[] INTERFACE = "interface".getBytes(StandardCharsets.US_ASCII);

    private final int size;
    private int statementEnd;
    private int currentCond, wordCount;

    public Parser(char[] source) {
        super(source);
        size = source.length;
    }

    private enum Scope {
        HEADER,
        GLOBAL,
        MEMBER,
        LOCAL,
    }

    public SourceRange scan(ArrayList<SourceRange> header, ArrayList<SourceRange> global, ArrayList<SourceRange> member, ArrayList<SourceRange> local) {
        int statementStart = 0;
        Scope prevScope = null;
        SourceRange prevRange = null;
        int currentChar = scanChar(true);
        for(;;) {
            Scope scope;
            try {
                currentChar = skipNonToken(currentChar, true);
                if(currentChar == EOF) {
                    return new SourceRange(statementStart, size);
                }
                scope = scanStatement(currentChar);
                currentChar = skipTrail();
            } catch(Eof e) {
                return new SourceRange(statementStart, size);
            }
            if(prevScope != scope) {
                prevRange = new SourceRange(statementStart, statementEnd);
                (switch(scope) {
                    case HEADER -> header;
                    case GLOBAL -> global;
                    case MEMBER -> member;
                    case LOCAL -> local;
                    default -> throw new AssertionError("Unknown enum constant");
                }).add(prevRange);
            } else {
                prevRange.end = statementEnd;
            }
            statementStart = statementEnd;
            prevScope = scope;
        }
    }

    private Scope scanStatement(int first) {
        int currentChar = scanWords(first);
        if(currentChar == ':') {
            scanToSemicolonOrBraces(':');
            return Scope.LOCAL;
        }
        switch(currentCond) {
            case COND_HEADER:
                scanToSemicolon(currentChar);
                return Scope.HEADER;
            case COND_TYPE:
                scanToBraces(currentChar);
                return Scope.GLOBAL;
            case COND_MEMBER:
            case COND_NOT_LOCAL:
                scanToSemicolonOrBraces(currentChar);
                return Scope.MEMBER;
            case COND_LOCAL:
                scanToSemicolon(currentChar);
                return Scope.LOCAL;
            case COND_ELSE:
                scanToSemicolonOrBraces(currentChar);
                return Scope.LOCAL;
        }
        switch(wordCount) {
            case 0:
                // Starting with a non-word.
                // This is definitely an expression-statement.
                scanToSemicolon(currentChar);
                return Scope.LOCAL;
            case 1:
                if(currentChar == '(') {
                    // Starting with a single word, followed by parentheses.
                    // Either a statement or a constructor, but constructors are not allowed.
                    scanToSemicolonOrBraces('(');
                    return Scope.LOCAL;
                }
                // fallthrough
            default:
                // Starting with multiple tokens before the first parenthesis.
                // This is a method if and only if the statement contains parentheses and ends with braces.
                return scanToSemicolonOrBraces(currentChar) ? Scope.MEMBER : Scope.LOCAL;
        }
    }

    private void scanToSemicolon(int first) {
        for(int currentChar = first; ; ) {
            if(currentChar == '{')
                scanEnclosed('{', '}');
            else if(currentChar == ';')
                return;
            currentChar = scanChar(false);
        }
    }

    private void scanToBraces(int first) {
        for(int currentChar = first; ; ) {
            if(currentChar == '{') {
                scanEnclosed('{', '}');
                return;
            }
            currentChar = scanChar(false);
        }
    }

    private boolean scanToSemicolonOrBraces(int first) {
        boolean insideWord = false;
        boolean foundParens = false;
        for(int currentChar = first; ; ) {
            switch(currentChar) {
                case '(':
                    scanEnclosed('(', ')');
                    foundParens = true;
                    break;
                case '{':
                    scanEnclosed('{', '}');
                    return foundParens;
                case '-':
                    currentChar = scanChar(false);
                    if(currentChar != '>')
                        continue; // not arrow, must match currentChar in switch, skip reading
                    // else was arrow, fall through
                case '=':
                    scanToSemicolon(' ');
                    return false;
                case ';':
                    return false;
                case 'n':
                    if(insideWord)
                        break;
                    insideWord = true;
                    currentChar = scanChar(false);
                    if(currentChar != 'e')
                        continue;
                    currentChar = scanChar(false);
                    if(currentChar != 'w')
                        continue;
                    currentChar = scanChar(false);
                    if(Character.isJavaIdentifierPart(currentChar))
                        continue;
                    scanToSemicolon(currentChar);
                    return false;
            }
            insideWord = Character.isJavaIdentifierPart(currentChar);
            currentChar = scanChar(false);
        }
    }

    private int scanWords(int first) {
        currentCond = COND_OTHER;
        wordCount = 0;
        for(int currentChar = first; ; ) {
            if(currentChar == '@') {
                int iface = 0;
                for(currentChar = skipNonToken(' ', false); Character.isJavaIdentifierPart(currentChar); currentChar = scanChar(false)) {
                    if(iface < INTERFACE.length && currentChar == INTERFACE[iface])
                        iface++;
                    else
                        iface = INTERFACE.length + 1;
                }
                if(iface == INTERFACE.length) {
                    // @interface definition
                    currentCond = Math.min(currentCond, COND_TYPE);
                    continue;
                }
                if(currentChar == '(') {
                    scanEnclosed('(', ')');
                    currentChar = skipNonToken(' ', false);
                }
                continue;
            }
            if(!Character.isJavaIdentifierPart(currentChar)) {
                return currentChar;
            }
            int currentState = Keyword.DEFAULT_STATE;
            do {
                currentState = Keyword.transition(currentState, currentChar);
            } while(Character.isJavaIdentifierPart(currentChar = scanChar(false)));
            Keyword kw = Keyword.valueFromState(currentState);
            if(kw == null) {
                wordCount++;
            } else {
                currentCond = Math.min(currentCond, switch(kw) {
                    case IMPORT, PACKAGE -> COND_HEADER;
                    case NATIVE, PRIVATE, PROTECTED, STATIC, TRANSIENT, VOLATILE -> COND_MEMBER;
                    case CLASS, ENUM, INTERFACE -> COND_TYPE;
                    case ABSTRACT, PUBLIC -> COND_NOT_LOCAL;
                    case DO, NEW -> COND_LOCAL;
                    case ELSE -> COND_ELSE;
                    default -> throw new AssertionError("Unknown enum constant");
                });
                wordCount++;
            }
            currentChar = skipNonToken(currentChar, false);
        }
    }

    private void scanEnclosed(int left, int right) {
        int open = 0;
        for(;;) {
            int currentChar = scanChar(false);
            if(currentChar == left) {
                open++;
            } else if(currentChar == right) {
                if(open == 0)
                    return;
                open--;
            }
        }
    }

    private int skipNonToken(int first, boolean allowEOF) {
        for(int currentChar = first; ; currentChar = scanChar(allowEOF)) {
            if(!Character.isWhitespace(currentChar)) {
                return currentChar;
            }
        }
    }

    private int skipTrail() {
        for(;;) {
            int prevIndex = getIndex();
            int currentChar = scanChar(true);
            if(currentChar == EOF) {
                statementEnd = size;
                return EOF;
            }
            if(currentChar == '\n') {
                statementEnd = getIndex(); // after current newline
                return scanChar(true);
            }
            if(!Character.isWhitespace(currentChar)) {
                statementEnd = prevIndex; // before current non-whitespace
                return currentChar;
            }
        }
    }
}
