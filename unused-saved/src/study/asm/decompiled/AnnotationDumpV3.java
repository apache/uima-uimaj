/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package asm.decompiled;

import java.util.*;
import org.objectweb.asm.*;

public class AnnotationDumpV3 implements Opcodes {

  public static byte[] dump() throws Exception {

    ClassWriter cw = new ClassWriter(0);
    FieldVisitor fv;
    MethodVisitor mv;
    AnnotationVisitor av0;

    cw.visit(52, ACC_PUBLIC + ACC_SUPER, "org/apache/uima/jcas/tcas/Annotation", null,
        "org/apache/uima/jcas/cas/AnnotationBase",
        new String[] { "org/apache/uima/cas/text/AnnotationFS" });

    cw.visitInnerClass("java/lang/invoke/MethodHandles$Lookup", "java/lang/invoke/MethodHandles",
        "Lookup", ACC_PUBLIC + ACC_FINAL + ACC_STATIC);

    {
      fv = cw.visitField(ACC_PUBLIC + ACC_FINAL + ACC_STATIC, "typeIndexID", "I", null, null);
      fv.visitEnd();
    }
    {
      fv = cw.visitField(ACC_PUBLIC + ACC_FINAL + ACC_STATIC, "type", "I", null, null);
      fv.visitEnd();
    }
    {
      fv = cw.visitField(ACC_PUBLIC + ACC_FINAL + ACC_STATIC, "_FI_begin", "I", null, null);
      fv.visitEnd();
    }
    {
      fv = cw.visitField(ACC_PUBLIC + ACC_FINAL + ACC_STATIC, "_FI_end", "I", null, null);
      fv.visitEnd();
    }
    {
      fv = cw.visitField(ACC_PRIVATE, "_F_begin", "I", null, null);
      fv.visitEnd();
    }
    {
      fv = cw.visitField(ACC_PRIVATE, "_F_end", "I", null, null);
      fv.visitEnd();
    }
    {
      mv = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
      mv.visitCode();
      mv.visitLdcInsn(Type.getType("Lorg/apache/uima/jcas/tcas/Annotation;"));
      mv.visitMethodInsn(INVOKESTATIC, "org/apache/uima/jcas/JCasRegistry", "register",
          "(Ljava/lang/Class;)I", false);
      mv.visitFieldInsn(PUTSTATIC, "org/apache/uima/jcas/tcas/Annotation", "typeIndexID", "I");
      mv.visitFieldInsn(GETSTATIC, "org/apache/uima/jcas/tcas/Annotation", "typeIndexID", "I");
      mv.visitFieldInsn(PUTSTATIC, "org/apache/uima/jcas/tcas/Annotation", "type", "I");
      mv.visitFieldInsn(GETSTATIC, "org/apache/uima/jcas/tcas/Annotation", "typeIndexID", "I");
      mv.visitMethodInsn(INVOKESTATIC, "org/apache/uima/jcas/JCasRegistry", "registerFeature",
          "(I)I", false);
      mv.visitFieldInsn(PUTSTATIC, "org/apache/uima/jcas/tcas/Annotation", "_FI_begin", "I");
      mv.visitFieldInsn(GETSTATIC, "org/apache/uima/jcas/tcas/Annotation", "typeIndexID", "I");
      mv.visitMethodInsn(INVOKESTATIC, "org/apache/uima/jcas/JCasRegistry", "registerFeature",
          "(I)I", false);
      mv.visitFieldInsn(PUTSTATIC, "org/apache/uima/jcas/tcas/Annotation", "_FI_end", "I");
      mv.visitInsn(RETURN);
      mv.visitMaxs(1, 0);
      mv.visitEnd();
    }
    {
      mv = cw.visitMethod(ACC_PUBLIC, "getTypeIndexID", "()I", null, null);
      mv.visitCode();
      mv.visitFieldInsn(GETSTATIC, "org/apache/uima/jcas/tcas/Annotation", "typeIndexID", "I");
      mv.visitInsn(IRETURN);
      mv.visitMaxs(1, 1);
      mv.visitEnd();
    }
    {
      mv = cw.visitMethod(ACC_PROTECTED, "<init>", "()V", null, null);
      mv.visitCode();
      mv.visitVarInsn(ALOAD, 0);
      mv.visitMethodInsn(INVOKESPECIAL, "org/apache/uima/jcas/cas/AnnotationBase", "<init>", "()V",
          false);
      mv.visitInsn(RETURN);
      mv.visitMaxs(1, 1);
      mv.visitEnd();
    }
    {
      mv = cw.visitMethod(ACC_PUBLIC, "<init>", "(Lorg/apache/uima/jcas/JCas;)V", null, null);
      mv.visitCode();
      mv.visitVarInsn(ALOAD, 0);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitMethodInsn(INVOKESPECIAL, "org/apache/uima/jcas/cas/AnnotationBase", "<init>",
          "(Lorg/apache/uima/jcas/JCas;)V", false);
      mv.visitInsn(RETURN);
      mv.visitMaxs(2, 2);
      mv.visitEnd();
    }
    {
      mv = cw.visitMethod(ACC_PUBLIC, "<init>",
          "(Lorg/apache/uima/cas/impl/TypeImpl;Lorg/apache/uima/cas/impl/CASImpl;)V", null, null);
      mv.visitCode();
      mv.visitVarInsn(ALOAD, 0);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitVarInsn(ALOAD, 2);
      mv.visitMethodInsn(INVOKESPECIAL, "org/apache/uima/jcas/cas/AnnotationBase", "<init>",
          "(Lorg/apache/uima/cas/impl/TypeImpl;Lorg/apache/uima/cas/impl/CASImpl;)V", false);
      mv.visitInsn(RETURN);
      mv.visitMaxs(3, 3);
      mv.visitEnd();
    }
    {
      mv = cw.visitMethod(ACC_PUBLIC, "getBegin", "()I", null, null);
      mv.visitCode();
      mv.visitVarInsn(ALOAD, 0);
      mv.visitFieldInsn(GETFIELD, "org/apache/uima/jcas/tcas/Annotation", "_F_begin", "I");
      mv.visitInsn(IRETURN);
      mv.visitMaxs(1, 1);
      mv.visitEnd();
    }
    {
      mv = cw.visitMethod(ACC_PUBLIC, "setBegin", "(I)V", null, null);
      mv.visitCode();
      mv.visitVarInsn(ALOAD, 0);
      mv.visitFieldInsn(GETFIELD, "org/apache/uima/jcas/tcas/Annotation", "_casView",
          "Lorg/apache/uima/cas/impl/CASImpl;");
      mv.visitVarInsn(ALOAD, 0);
      mv.visitFieldInsn(GETSTATIC, "org/apache/uima/jcas/tcas/Annotation", "_FI_begin", "I");
      mv.visitVarInsn(ALOAD, 0);
      mv.visitVarInsn(ILOAD, 1);
      mv.visitInvokeDynamicInsn("run",
          "(Lorg/apache/uima/jcas/tcas/Annotation;I)Ljava/lang/Runnable;",
          new Handle(Opcodes.H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "metafactory",
              "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;"
                  + "Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;"),
          new Object[] {
              Type.getType("()V"), new Handle(Opcodes.H_INVOKESPECIAL,
                  "org/apache/uima/jcas/tcas/Annotation", "lambda$0", "(I)V"),
              Type.getType("()V") });
      mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/uima/cas/impl/CASImpl",
          "setWithCheckAndJournalJFRI", "(Lorg/apache/uima/jcas/cas/TOP;ILjava/lang/Runnable;)V",
          false);
      mv.visitInsn(RETURN);
      mv.visitMaxs(5, 2);
      mv.visitEnd();
    }
    {
      mv = cw.visitMethod(ACC_PUBLIC, "getEnd", "()I", null, null);
      mv.visitCode();
      mv.visitVarInsn(ALOAD, 0);
      mv.visitFieldInsn(GETFIELD, "org/apache/uima/jcas/tcas/Annotation", "_F_end", "I");
      mv.visitInsn(IRETURN);
      mv.visitMaxs(1, 1);
      mv.visitEnd();
    }
    {
      mv = cw.visitMethod(ACC_PUBLIC, "setEnd", "(I)V", null, null);
      mv.visitCode();
      mv.visitVarInsn(ALOAD, 0);
      mv.visitFieldInsn(GETFIELD, "org/apache/uima/jcas/tcas/Annotation", "_casView",
          "Lorg/apache/uima/cas/impl/CASImpl;");
      mv.visitVarInsn(ALOAD, 0);
      mv.visitFieldInsn(GETSTATIC, "org/apache/uima/jcas/tcas/Annotation", "_FI_end", "I");
      mv.visitVarInsn(ALOAD, 0);
      mv.visitVarInsn(ILOAD, 1);
      mv.visitInvokeDynamicInsn("run",
          "(Lorg/apache/uima/jcas/tcas/Annotation;I)Ljava/lang/Runnable;",
          new Handle(Opcodes.H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "metafactory",
              "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;"),
          new Object[] {
              Type.getType("()V"), new Handle(Opcodes.H_INVOKESPECIAL,
                  "org/apache/uima/jcas/tcas/Annotation", "lambda$1", "(I)V"),
              Type.getType("()V") });
      mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/uima/cas/impl/CASImpl",
          "setWithCheckAndJournalJFRI", "(Lorg/apache/uima/jcas/cas/TOP;ILjava/lang/Runnable;)V",
          false);
      mv.visitInsn(RETURN);
      mv.visitMaxs(5, 2);
      mv.visitEnd();
    }
    {
      mv = cw.visitMethod(ACC_PUBLIC, "<init>", "(Lorg/apache/uima/jcas/JCas;II)V", null, null);
      mv.visitCode();
      mv.visitVarInsn(ALOAD, 0);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitMethodInsn(INVOKESPECIAL, "org/apache/uima/jcas/tcas/Annotation", "<init>",
          "(Lorg/apache/uima/jcas/JCas;)V", false);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitVarInsn(ILOAD, 2);
      mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/uima/jcas/tcas/Annotation", "setBegin", "(I)V",
          false);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitVarInsn(ILOAD, 3);
      mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/uima/jcas/tcas/Annotation", "setEnd", "(I)V",
          false);
      mv.visitInsn(RETURN);
      mv.visitMaxs(2, 4);
      mv.visitEnd();
    }
    {
      mv = cw.visitMethod(ACC_PUBLIC, "getCoveredText", "()Ljava/lang/String;", null, null);
      mv.visitCode();
      mv.visitVarInsn(ALOAD, 0);
      mv.visitFieldInsn(GETFIELD, "org/apache/uima/jcas/tcas/Annotation", "_casView",
          "Lorg/apache/uima/cas/impl/CASImpl;");
      mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/uima/cas/impl/CASImpl", "getDocumentText",
          "()Ljava/lang/String;", false);
      mv.visitVarInsn(ASTORE, 1);
      mv.visitVarInsn(ALOAD, 1);
      Label l0 = new Label();
      mv.visitJumpInsn(IFNONNULL, l0);
      mv.visitInsn(ACONST_NULL);
      mv.visitInsn(ARETURN);
      mv.visitLabel(l0);
      mv.visitFrame(Opcodes.F_APPEND, 1, new Object[] { "java/lang/String" }, 0, null);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/uima/jcas/tcas/Annotation", "getBegin", "()I",
          false);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/uima/jcas/tcas/Annotation", "getEnd", "()I",
          false);
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "substring", "(II)Ljava/lang/String;",
          false);
      mv.visitInsn(ARETURN);
      mv.visitMaxs(3, 2);
      mv.visitEnd();
    }
    {
      mv = cw.visitMethod(ACC_PUBLIC + ACC_DEPRECATED, "getStart", "()I", null, null);
      {
        av0 = mv.visitAnnotation("Ljava/lang/Deprecated;", true);
        av0.visitEnd();
      }
      mv.visitCode();
      mv.visitVarInsn(ALOAD, 0);
      mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/uima/jcas/tcas/Annotation", "getBegin", "()I",
          false);
      mv.visitInsn(IRETURN);
      mv.visitMaxs(1, 1);
      mv.visitEnd();
    }
    {
      mv = cw.visitMethod(ACC_PRIVATE + ACC_SYNTHETIC, "lambda$0", "(I)V", null, null);
      mv.visitCode();
      mv.visitVarInsn(ALOAD, 0);
      mv.visitVarInsn(ILOAD, 1);
      mv.visitInsn(DUP_X1);
      mv.visitFieldInsn(PUTFIELD, "org/apache/uima/jcas/tcas/Annotation", "_F_begin", "I");
      mv.visitInsn(RETURN);
      mv.visitMaxs(3, 2);
      mv.visitEnd();
    }
    {
      mv = cw.visitMethod(ACC_PRIVATE + ACC_SYNTHETIC, "lambda$1", "(I)V", null, null);
      mv.visitCode();
      mv.visitVarInsn(ALOAD, 0);
      mv.visitVarInsn(ILOAD, 1);
      mv.visitInsn(DUP_X1);
      mv.visitFieldInsn(PUTFIELD, "org/apache/uima/jcas/tcas/Annotation", "_F_end", "I");
      mv.visitInsn(RETURN);
      mv.visitMaxs(3, 2);
      mv.visitEnd();
    }
    cw.visitEnd();

    return cw.toByteArray();
  }
}
