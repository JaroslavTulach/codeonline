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

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.TypePath;

public final class MethodBodyEraser extends ClassVisitor {
    private static final int FORCED_BYTECODE_VERSION = Opcodes.V1_8;

    private MethodBodyEraser() {
        super(Opcodes.ASM6, new ClassWriter(ClassWriter.COMPUTE_FRAMES));
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mw = super.visitMethod(access, name, desc, signature, exceptions);
        return new MethodVisitor(Opcodes.ASM6) {
            @Override
            public void visitEnd() {
                mw.visitEnd();
            }

            @Override
            public void visitCode() {
                mw.visitCode();
                // replace all of method body with "throw null;"
                mw.visitInsn(Opcodes.ACONST_NULL);
                mw.visitInsn(Opcodes.ATHROW);
            }

            @Override
            public void visitAttribute(Attribute attr) {
                mw.visitAttribute(attr);
            }

            @Override
            public void visitMaxs(int maxStack, int maxLocals) {
                // both ignored, recomputed by ClassWriter
                mw.visitMaxs(maxStack, maxLocals);
            }

            @Override
            public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
                return mw.visitParameterAnnotation(parameter, desc, visible);
            }

            @Override
            public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
                return mw.visitTypeAnnotation(typeRef, typePath, desc, visible);
            }

            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                if(desc.indexOf('+') != -1)
                    return null;
                return mw.visitAnnotation(desc, visible);
            }

            @Override
            public AnnotationVisitor visitAnnotationDefault() {
                return mw.visitAnnotationDefault();
            }

            @Override
            public void visitParameter(String name, int access) {
                mw.visitParameter(name, access);
            }
        };
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(FORCED_BYTECODE_VERSION, access, name, signature, superName, interfaces);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        if(descriptor.indexOf('+') != -1)
            return null;
        return super.visitAnnotation(descriptor, visible);
    }

    private byte[] toByteArray() {
        return ((ClassWriter) cv).toByteArray();
    }

    public static byte[] eraseMethodBodies(byte[] bytecode) {
        ClassReader classReader = new ClassReader(bytecode);
        MethodBodyEraser visitor = new MethodBodyEraser();
        classReader.accept(visitor, 0);
        return visitor.toByteArray();
    }
}
