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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class MethodBodyEraserTest {
    static final String SIMPLE_CLASS_NAME = "SampleClass";
    static final String CLASS_NAME = "com.oracle.graalvm.codeonline.build.SampleClass";
    static byte[] origBytecode;

    @BeforeClass
    public static void setUpClass() throws IOException {
        origBytecode = MethodBodyEraserTest.class.getResourceAsStream(SIMPLE_CLASS_NAME + ".class").readAllBytes();
    }

    @Test
    public void testEraseMethodBodies() throws ReflectiveOperationException {
        byte[] bytecode = MethodBodyEraser.eraseMethodBodies(origBytecode);
        ClassLoader loader = new ClassLoader(null) {
            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                return super.loadClass(name); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                if(name.equals(CLASS_NAME)) {
                    return defineClass(CLASS_NAME, bytecode, 0, bytecode.length);
                }
                return super.findClass(name);
            }
        };
        Method method = loader.loadClass(CLASS_NAME).getMethod("sampleMethod", int.class);
        Assert.assertThrows(InvocationTargetException.class, () -> method.invoke(null, 42));
    }
}
