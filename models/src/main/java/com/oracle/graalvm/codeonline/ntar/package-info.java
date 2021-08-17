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

/**
 * Classes implementing a custom simple archive format (named ntar).
 * <p>
 * A custom format is used instead of ZIP because
 * JDK 8 implements part of the ZIP format (the compression algorithm) in native code.
 * Instead of implementing all the complexity of ZIP in Java again, we use a simpler format.
 * <p>
 * This temporary solution will be in place until the upgrade to Java 11.
 * <p>
 * The file consists of zero or more entries, concatenated without any padding or delimiters.
 * Each entry is encoded as the concatenation of the following fields:
 * <ul>
 * <li>N (4 bytes): 32bit little-endian signed integer, must be positive
 * <li>name (N bytes): ASCII string, one character per byte, NOT null-terminated
 * <li>M (4 bytes): 32bit little-endian signed integer, must be positive
 * <li>content (M bytes): raw data
 * </ul>
 * Note: none of the fields have to be aligned.
 * <p>
 * Each archive file corresponds to a Java package,
 * and each its entry corresponds to a class in that package.
 * Entry name is the binary class name without package name
 * (e.g. {@code ArrayList}, {@code Map$Entry}, {@code FooFactory$1}).
 * Entry content is an entire class file.
 */
package com.oracle.graalvm.codeonline.ntar;
