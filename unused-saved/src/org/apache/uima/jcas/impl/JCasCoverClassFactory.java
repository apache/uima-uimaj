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
package org.apache.uima.cas.impl;

import java.util.List;

import org.apache.uima.cas.AbstractCas_ImplBase;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import static org.objectweb.asm.Opcodes.*;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Support for creating JCas cover classes 
 * 
 * These may be loaded under a PEAR class loader.
 * 
 *   Each typesystem has one definition
 *       made from merged typesystem info and
 *       the 1st found customization class (if any)
 *   
 *     Pears occurring in a pipeline that uses the above
 *     typesystem may have a different definition, loaded
 *     under the Pear's classloader. 
 *       - this means the instances cannot be cast among the
 *         different (but same-named) classes
 *   
 * Main operation: 
 *   locate the customization (if any).
 *   If found, read the compiled code, using ASM:
 *     visit fields:
 *       validate range type OK 
 *       at end: insert fields not already present
 *     visit methods:
 *       validate method impl OK 
 *       at end: insert methods not already present        
 * 
 *   at end, load result using appropriate class loader,
 *   store in typeSystem (perhaps under Pear classpath)
 *
 */
public class JCasCoverClassFactory {
 
  
  
  private int javaClassVersion; // correspond to Java 8 or 7
  public void setGeneratedJavaVersion(int i) {
    javaClassVersion = (i == 8 || i == V1_8) ? V1_8 : V1_7; 
  }

  // shared state for the type
  private TypeImpl type;
  private ClassNode cn;  
  /**
   * x/y/z form 
   */
  private String typeJavaClassName;
  private String typeJavaDescriptor;
  
  // shared state for a feature
  private FeatureImpl fi;
  private String domainClassName;
  private String rangeJavaDescriptor;
  private String rangeArrayElementJavaDescriptor;
  private String featureFieldName;
  private String featureShortName1stLetterUpperCase;
  
  
  /**
   * Create - no customization case
   *          not used for TOP or other built-in predefined types
   * @return the class as a byte array
   */
  byte[] createJCasCoverClass(TypeImpl type) {
    this.type = type;
    typeJavaDescriptor = type.getNameAsJavaDescriptor();
    typeJavaClassName = type.getName().replace('.', '/');
    cn = new ClassNode(ASM5); // java 8
    cn.version = javaClassVersion;
    cn.access = ACC_PUBLIC + ACC_SUPER;
    cn.name = typeJavaClassName;   
    cn.superName = type.getSuperType().getName().replace('.', '/');
//    cn.interfaces = typeImpl.getInterfaceNamesArray();   // TODO
    
    // add the "type" field - this has the int type code
    cn.fields.add(new FieldNode(ACC_PUBLIC + ACC_FINAL + ACC_STATIC,
        "type", "I", null, null));
    
    // add field declares, and getters and setters, and special getters/setters for array things    
    type.getMergedStaticFeaturesIntroducedByThisType().stream()
          .forEach(this::addFeatureFieldGetSet);
 
    addStaticInitAndConstructors();
    
    
    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
    cn.accept(cw);
    return cw.toByteArray();
  }
  
  private void addFeatureFieldGetSet(FeatureImpl fi) {
    
    // compute shared info for this feature
    this.fi = fi;
    featureFieldName = getFeatureFieldName(fi);
    featureShortName1stLetterUpperCase = fi.getShortName1stLetterUpperCase();
    domainClassName = fi.getDomain().getName().replace('.', '/');
    rangeJavaDescriptor = fi.getRangeAsJavaDescriptor();
    
    addFeatureField();  // add declares for fields
    addFeatureGetSet();
    TypeImpl range = (TypeImpl) fi.getRange();
    if (range.isArray()) {
      rangeArrayElementJavaDescriptor = fi.getRangeArrayElementAsJavaDescriptor();
      addArrayFeatureGetSet();
    }
  }
  
  private void addFeatureGetSet() {
    MethodNode mn = new MethodNode(ASM5, ACC_PUBLIC,    //  Get for non-array value
        "get" + featureShortName1stLetterUpperCase, 
        "()" + rangeJavaDescriptor, null, null);
    InsnList il = mn.instructions;
    il.add(new VarInsnNode(ALOAD,  0));
    il.add(new FieldInsnNode(GETFIELD, domainClassName, featureFieldName, rangeJavaDescriptor));
    il.add(new InsnNode(getReturnInst(fi)));
    mn.maxStack = 1;
    mn.maxLocals = 1;
    cn.methods.add(mn);
    
    mn = new MethodNode(ASM5, ACC_PUBLIC, 
        "set" + featureShortName1stLetterUpperCase, 
        "(" + rangeJavaDescriptor + ")V", null, null);
    il = mn.instructions;
    il.add(new VarInsnNode(ALOAD,  0));
    il.add(new VarInsnNode(getLoadInst(fi), 1));        // load ref, or primitive value
    il.add(new FieldInsnNode(PUTFIELD, domainClassName, featureFieldName, rangeJavaDescriptor));
    il.add(new InsnNode(RETURN));
    mn.maxStack = 2;
    mn.maxLocals = 2;
    cn.methods.add(mn); 
  }
  
  private void addArrayFeatureGetSet() {
    MethodNode mn = new MethodNode(ASM5, ACC_PUBLIC,
        "get" + featureShortName1stLetterUpperCase, 
        "(I)" + rangeArrayElementJavaDescriptor, null, null);
    InsnList il = mn.instructions;
    il.add(new VarInsnNode(ALOAD,  0));
    il.add(new FieldInsnNode(GETFIELD, domainClassName, featureFieldName, rangeJavaDescriptor));
    il.add(new VarInsnNode(ILOAD,  1));
    il.add(new InsnNode(getArrayLoadInst(fi)));
    il.add(new InsnNode(getReturnInst(fi)));
    mn.maxStack = 2;
    mn.maxLocals = 2;
    cn.methods.add(mn);

    mn = new MethodNode(ASM5, ACC_PUBLIC,
        "set" + featureShortName1stLetterUpperCase, 
        "(I" + rangeArrayElementJavaDescriptor + ")V", null, null);
    il = mn.instructions;
    il.add(new VarInsnNode(ALOAD,  0));
    il.add(new FieldInsnNode(GETFIELD, domainClassName, featureFieldName, rangeJavaDescriptor));
    il.add(new VarInsnNode(ILOAD,  1));
    il.add(new VarInsnNode(getLoadInst(fi), 2));
    il.add(new InsnNode(getArrayStoreInst(fi)));
    il.add(new InsnNode(RETURN));
    mn.maxStack = 2;
    mn.maxLocals = 2;
    cn.methods.add(mn);

  }
  
  private void addFeatureField() {
    cn.fields.add(new FieldNode(ACC_PRIVATE, featureFieldName, rangeJavaDescriptor, null, null));
  }
  
  /**
   * Get and Set by feature offset
   *   Use single inheritance to enforce constant offset 
   *     features defined higher in the hierarchy have lowest offsets
   *   dense, start from 1 (for possible future extensions/maintenance?)  
   *     
   * Each class defines the getter/setter for those features introduced by the class
   * 
   * get: feature offset mapped to get/set method
   *   try to avoid deep inheritance
   *   need one map per class (e.g., static?)
   *   has fully expanded array
   *   separate getters/setters
   *   
   * methods:
   *   many-different-return-values getXxxValueOffset(int offset)
   *   void setValueOffset(int offset, various-types)  boolean, byte, short, int, float, long, double ref
   *   
   *   These methods are in TOP, not replicated.
   *   
   *   They reference static values at each level of the hierarchy via 
   *      this.getterOffsetTable();  this.setterOffsetTable();
   *         returns a static array at the "this" level of the hierarchy
   *         key = offset, value = getter or setter method
   *        
   */
  
  private void generateGetSetByOffset() {
    
  }
  
  /**
   * Used to avoid collisions between field names for features and other things
   * @param feature the feature
   * @return -
   */
  private String getFeatureFieldName(FeatureImpl feature) {
    return feature.getShortName();
  }
  
  private void addStaticInitAndConstructors() {
    // class init - 
    // instance init method
    MethodNode mn = new MethodNode(ASM5, ACC_STATIC, "<clinit>", "()V", null, null);
    InsnList il = mn.instructions;
    il.add(new LdcInsnNode(Type.getType(typeJavaDescriptor)));
    il.add(new MethodInsnNode(INVOKESTATIC, "org/apache/uima/jcas/JCasRegistry", "register", "(Ljava/lang/Class;)I", false));
    il.add(new FieldInsnNode(PUTSTATIC, typeJavaClassName, "type", "I"));
    il.add(new InsnNode(RETURN));
    mn.maxStack = 1;
    mn.maxLocals = 0;
    cn.methods.add(mn);
    
    // instance constructors method
    
    mn = new MethodNode(ACC_PUBLIC, "<init>", "(ILorg/apache/uima/jcas/cas/TOP_Type;)V", null, null);
    il = mn.instructions;
    il.add(new VarInsnNode(ALOAD, 0));
    il.add(new VarInsnNode(ILOAD, 1));
    il.add(new VarInsnNode(ALOAD, 2));
    il.add(new MethodInsnNode(INVOKESPECIAL, "org/apache/uima/jcas/tcas/Annotation", "<init>", "(ILorg/apache/uima/jcas/cas/TOP_Type;)V", false));
    il.add(new InsnNode(RETURN));
    mn.maxStack = 3;
    mn.maxLocals = 3;
    cn.methods.add(mn);
    
    mn = new MethodNode(ACC_PUBLIC, "<init>", "(Lorg/apache/uima/jcas/JCas;)V", null, null);
    il = mn.instructions;
    il.add(new VarInsnNode(ALOAD, 0));
    il.add(new VarInsnNode(ALOAD, 1));
    il.add(new MethodInsnNode(INVOKESPECIAL, "org/apache/uima/jcas/tcas/Annotation", "<init>", "(Lorg/apache/uima/jcas/JCas;)V", false));
    il.add(new InsnNode(RETURN));
    mn.maxStack = 2;
    mn.maxLocals = 2;
    cn.methods.add(mn);
    
    // constructor for annotation
    if (type.isAnnotationType()) {
      mn = new MethodNode(ACC_PUBLIC, "<init>", "(Lorg/apache/uima/jcas/JCas;II)V", null, null);
      il = mn.instructions;
      il.add(new VarInsnNode(ALOAD, 0));
      il.add(new VarInsnNode(ALOAD, 1));
      il.add(new MethodInsnNode(INVOKESPECIAL, "org/apache/uima/jcas/tcas/Annotation", "<init>", "(Lorg/apache/uima/jcas/JCas;)V", false));
      il.add(new VarInsnNode(ALOAD, 0));
      il.add(new VarInsnNode(ILOAD, 2));
      il.add(new MethodInsnNode(INVOKEVIRTUAL, "org/apache/uima/tutorial/RoomNumberv3", "setBegin", "(I)V", false));
      il.add(new VarInsnNode(ALOAD, 0));
      il.add(new VarInsnNode(ILOAD, 3));
      il.add(new MethodInsnNode(INVOKEVIRTUAL, "org/apache/uima/tutorial/RoomNumberv3", "setEnd", "(I)V", false));
      il.add(new InsnNode(RETURN));
      mn.maxStack = 2;
      mn.maxLocals = 4;
      cn.methods.add(mn);
    }
  }
  
  private int getReturnInst(FeatureImpl feature) {
    switch (((TypeImpl) feature.getRange()).getCode()) {
    case TypeSystemImpl.booleanTypeCode : return IRETURN;
    case TypeSystemImpl.byteTypeCode    : return IRETURN;
    case TypeSystemImpl.shortTypeCode   : return IRETURN;
    case TypeSystemImpl.intTypeCode     : return IRETURN;
    case TypeSystemImpl.longTypeCode    : return LRETURN;
    case TypeSystemImpl.floatTypeCode   : return FRETURN;
    case TypeSystemImpl.doubleTypeCode  : return DRETURN;
    default                             : return ARETURN;
    }
  }
  
  private int getLoadInst(FeatureImpl feature) {            // load from local variable
    switch (((TypeImpl) feature.getRange()).getCode()) {
    case TypeSystemImpl.booleanTypeCode : return ILOAD;
    case TypeSystemImpl.byteTypeCode    : return ILOAD;
    case TypeSystemImpl.shortTypeCode   : return ILOAD;
    case TypeSystemImpl.intTypeCode     : return ILOAD;
    case TypeSystemImpl.longTypeCode    : return LLOAD;
    case TypeSystemImpl.floatTypeCode   : return FLOAD;
    case TypeSystemImpl.doubleTypeCode  : return DLOAD;
    default                             : return ALOAD;
    }
  }
  
  private int getArrayLoadInst(FeatureImpl feature) {      // load from array
    switch (((TypeImpl) feature.getRange()).getCode()) {
    case TypeSystemImpl.booleanTypeCode : return BALOAD;
    case TypeSystemImpl.byteTypeCode    : return BALOAD;
    case TypeSystemImpl.shortTypeCode   : return SALOAD;
    case TypeSystemImpl.intTypeCode     : return IALOAD;
    case TypeSystemImpl.longTypeCode    : return LALOAD;
    case TypeSystemImpl.floatTypeCode   : return FALOAD;
    case TypeSystemImpl.doubleTypeCode  : return DALOAD;
    default                             : return AALOAD;
    }
  }
    
  private int getArrayStoreInst(FeatureImpl feature) {     // store into array
    switch (((TypeImpl) feature.getRange()).getCode()) {
    case TypeSystemImpl.booleanTypeCode : return BASTORE;
    case TypeSystemImpl.byteTypeCode    : return BASTORE;
    case TypeSystemImpl.shortTypeCode   : return SASTORE;
    case TypeSystemImpl.intTypeCode     : return IASTORE;
    case TypeSystemImpl.longTypeCode    : return LASTORE;
    case TypeSystemImpl.floatTypeCode   : return FASTORE;
    case TypeSystemImpl.doubleTypeCode  : return DASTORE;
    default                             : return AASTORE;
    }
  }
}
