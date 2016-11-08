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
package org.apache.uima.jcasgen.impl;

import java.util.List;

import org.apache.uima.jcasgen.JCasClassDefinition;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;


public class JCasClassConversion implements Opcodes {
  
  final private String classname;  // e.g. "org/apache/uima/jcas/tcas/Annotation"
  final private ClassVisitor cv;
  final List<String> fieldNames;
  final List<String> fieldJavaTypes;
  
  JCasClassConversion(String classname, ClassVisitor cv, List<String> fieldNames, List<String> fieldJavaTypes) {
    this.classname = classname;
    this.cv = cv;
    this.fieldNames = fieldNames;
    this.fieldJavaTypes = fieldJavaTypes;
  }
  
  void generatePFS_I(String name) {
    cv.visitField(ACC_PUBLIC + ACC_FINAL + ACC_STATIC, name, "I", null, null).visitEnd();
  }
  
  void generateV3StandardFields() {
    generatePFS_I("typeIndexID");
    generatePFS_I("type");
  }
  
  void generateV3_FI_fields() {
    fieldNames.stream().forEachOrdered(
        name -> cv.visitField(ACC_PUBLIC + ACC_FINAL + ACC_STATIC, "_FI_" + name, "I", null, null).visitEnd());
  }
  
  void generateV3StandardMethods() {
    MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "getTypeIndexID", "()I", null, null);
    mv.visitCode();
    mv.visitFieldInsn(GETSTATIC, "org/apache/uima/jcas/tcas/Annotation", "typeIndexID", "I");
    mv.visitInsn(IRETURN);
    mv.visitMaxs(1, 1);
    mv.visitEnd();
  }
  
  void generateV3_local_data() {  // type is "I" etc
    final int size = fieldNames.size();
    for (int i = 0; i < size; i++) {
      cv.visitField(ACC_PRIVATE, "_F_" + fieldNames.get(i), fieldJavaTypes.get(i), null, null).visitEnd();
    }
  }
  
  void genv3_classInit() {
    MethodVisitor mv = cv.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);    
    genv3_commonStaticFieldInit(mv);
    genv3_registerFeatures(mv);
    mv.visitInsn(RETURN);
    mv.visitMaxs(1, 0);
    mv.visitEnd();
  }
  
  void genv3_commonStaticFieldInit(MethodVisitor mv) {
    mv.visitCode();
    mv.visitLdcInsn(Type.getType("Lorg/apache/uima/jcas/tcas/Annotation;"));
    mv.visitMethodInsn(INVOKESTATIC, "org/apache/uima/jcas/JCasRegistry", "register",
        "(Ljava/lang/Class;)I", false);
    mv.visitFieldInsn(PUTSTATIC, "org/apache/uima/jcas/tcas/Annotation", "typeIndexID", "I");
    mv.visitFieldInsn(GETSTATIC, "org/apache/uima/jcas/tcas/Annotation", "typeIndexID", "I");
    mv.visitFieldInsn(PUTSTATIC, "org/apache/uima/jcas/tcas/Annotation", "type", "I");    
  }
  
  void genv3_registerFeatures(MethodVisitor mv) {
    fieldNames.stream().forEachOrdered(
        name -> {
          mv.visitFieldInsn(GETSTATIC, "org/apache/uima/jcas/tcas/Annotation", "typeIndexID", "I");
          mv.visitMethodInsn(INVOKESTATIC, "org/apache/uima/jcas/JCasRegistry", "registerFeature",
              "(I)I", false);
          mv.visitFieldInsn(PUTSTATIC, "org/apache/uima/jcas/tcas/Annotation", "_FI_" + name, "I");
        });
  }
  
  /**
   * @param superclass eg "org/apache/uima/jcas/cas/AnnotationBase"
   */
  void genv3_0_arg_constructor(String superclass) {
    MethodVisitor mv = cv.visitMethod(ACC_PROTECTED, "<init>", "()V", null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESPECIAL, superclass, "<init>", "()V", false);
    mv.visitInsn(RETURN);
    mv.visitMaxs(1, 1);
    mv.visitEnd();
  }
  
  /**
   * @param superclass eg "org/apache/uima/jcas/cas/AnnotationBase"
   */
  void genv3_jcas_constructor(String superclass) {
    MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "<init>", "(Lorg/apache/uima/jcas/JCas;)V", null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitMethodInsn(INVOKESPECIAL, superclass, "<init>", "(Lorg/apache/uima/jcas/JCas;)V", false);
    mv.visitInsn(RETURN);
    mv.visitMaxs(2, 2);
    mv.visitEnd();
  }
  
  void genv3_2_arg_constructor(String superclass) {
    MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "<init>",
         "(Lorg/apache/uima/cas/impl/TypeImpl;"
        + "Lorg/apache/uima/cas/impl/CASImpl;)V", null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitMethodInsn(INVOKESPECIAL, superclass, "<init>",
         "(Lorg/apache/uima/cas/impl/TypeImpl;"
        + "Lorg/apache/uima/cas/impl/CASImpl;)V", false);
    mv.visitInsn(RETURN);
    mv.visitMaxs(3, 3);
    mv.visitEnd();
  }
  
  /**
   * 
   * @param name  name of field, e.g. begin
   * @param type Java type, eg. "I"
   */
  void genv3_getter(String name, String type) {
    MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "get" + up1st(name), "()" + type, null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, classname, "_F_" + name, type);
    mv.visitInsn(IRETURN);
    mv.visitMaxs(1, 1);
    mv.visitEnd();
  }
  
  void genv3_setter(String name, String type) {
    MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "set" + up1st(name), "(" + type + ")V", null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, classname, "_casView", "Lorg/apache/uima/cas/impl/CASImpl;");
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETSTATIC, classname, "_FI_" + name, "I");
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ILOAD, 1);
    mv.visitInvokeDynamicInsn("run",
        "(Lorg/apache/uima/jcas/tcas/Annotation;I)Ljava/lang/Runnable;",
        new Handle(Opcodes.H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "metafactory",
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;"
                + "Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;"),
        new Object[] {
            Type.getType("()V"), 
            new Handle(Opcodes.H_INVOKESPECIAL,
                       "org/apache/uima/jcas/tcas/Annotation", 
                       "lambda$0", 
                       "(I)V"),
            Type.getType("()V") });
    mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/uima/cas/impl/CASImpl",
        "setWithCheckAndJournalJFRI", "(Lorg/apache/uima/jcas/cas/TOP;ILjava/lang/Runnable;)V",
        false);
    mv.visitInsn(RETURN);
    mv.visitMaxs(5, 2);
    mv.visitEnd(); 
  }
  
  
  
  
  
  
  
  
  
  
  
  
  
  
//  public static byte[] convert2to3(JCasClassDefinition jd) {
//    ClassReader cr = new ClassReader(jd.getBytes());
//    ClassWriter cw = new ClassWriter(0);
//    
//    ClassVisitor cv = new ClassVisitor(ASM5, cw) {
//
//      @Override
//      public void visit(int version, int access, String name, String signature, String superName,
//          String[] interfaces) {
//        if (access != (ACC_PUBLIC + ACC_SUPER) || signature != null) {
//          jd.setInvalidJCasDefinition();
//          throw new RuntimeException();
//        }
//        super.visit(version, access, name, signature, superName, interfaces);
//      }
//
//      @Override
//      public FieldVisitor visitField(int access, String name, String desc, String signature,
//          Object value) {
//        
//        return super.visitField(access, name, desc, signature, value);
//      }
//
//      @Override
//      public MethodVisitor visitMethod(int access, String name, String desc, String signature,
//          String[] exceptions) {
//        // TODO Auto-generated method stub
//        return super.visitMethod(access, name, desc, signature, exceptions);
//      }
//
//      @Override
//      public void visitEnd() {
//        // TODO Auto-generated method stub
//        super.visitEnd();
//      }
//      
//    };
//    
//    
//  }
  
  private String up1st(String s) {
    return Character.toUpperCase(s.charAt(0)) + s.substring(1);
  }
}
