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

import java.util.Arrays;
import java.util.IntSummaryStatistics;
import java.util.LinkedHashMap;

public enum Keyword {
    ABSTRACT, DO, CLASS, ELSE, ENUM, IMPORT, INTERFACE, NATIVE, NEW, PACKAGE, PRIVATE, PROTECTED, PUBLIC, STATIC, TRANSIENT, VOLATILE;

    public static final int FAIL_STATE;
    public static final int DEFAULT_STATE;
    public static final int FIRST_KEYWORD_STATE;

    private static final Keyword[] VALUES = values();
    private static final byte[] TRANSITIONS;
    private static final int ALPHABET_START;
    private static final int ALPHABET_SIZE;

    static {
        String[] keywords = Arrays.stream(VALUES).map(Keyword::name).map(String::toLowerCase).toArray(String[]::new);
        {
            IntSummaryStatistics charStats = Arrays.stream(keywords).flatMapToInt(String::chars).summaryStatistics();
            ALPHABET_START = charStats.getMin();
            ALPHABET_SIZE = charStats.getMax() - ALPHABET_START + 1;
        }

        LinkedHashMap<String, Integer> states = new LinkedHashMap<>(); // maps prefixes to state indices
        states.put(null, FAIL_STATE = states.size());
        states.put("", DEFAULT_STATE = states.size());
        FIRST_KEYWORD_STATE = states.size();
        for(String keyword : keywords) {
            states.putIfAbsent(keyword, states.size());
        }
        for(String keyword : keywords) {
            for(int i = 1; i < keyword.length(); i++) {
                states.putIfAbsent(keyword.substring(0, i), states.size());
            }
        }
        assert states.size() <= 128 : "Too many states, state index will not fit in a byte";

        TRANSITIONS = new byte[ALPHABET_SIZE * states.size()];
        for(String keyword : keywords) {
            for(int i = 0; i < keyword.length(); i++) {
                int fromState = states.get(keyword.substring(0, i));
                int toState = states.get(keyword.substring(0, i + 1));
                int input = keyword.charAt(i) - ALPHABET_START;
                TRANSITIONS[ALPHABET_SIZE * fromState + input] = (byte) toState;
            }
        }
    }

    public static int transition(int fromState, int inputSymbol) {
        int input = inputSymbol - ALPHABET_START;
        if(0 <= input && input < ALPHABET_SIZE)
            return TRANSITIONS[ALPHABET_SIZE * fromState + input];
        else
            return FAIL_STATE;
    }

    public static Keyword valueFromState(int state) {
        int index = state - FIRST_KEYWORD_STATE;
        if(0 <= index && index < VALUES.length)
            return VALUES[index];
        else
            return null;
    }
}
