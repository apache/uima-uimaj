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

/** New Name:  FeatureStructureClassGen */

package org.apache.uima.cas.impl;

import static org.objectweb.asm.Opcodes.AALOAD;
import static org.objectweb.asm.Opcodes.AASTORE;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ASM5;
import static org.objectweb.asm.Opcodes.ATHROW;
import static org.objectweb.asm.Opcodes.BALOAD;
import static org.objectweb.asm.Opcodes.BASTORE;
import static org.objectweb.asm.Opcodes.DALOAD;
import static org.objectweb.asm.Opcodes.DASTORE;
import static org.objectweb.asm.Opcodes.DLOAD;
import static org.objectweb.asm.Opcodes.DRETURN;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.FALOAD;
import static org.objectweb.asm.Opcodes.FASTORE;
import static org.objectweb.asm.Opcodes.FLOAD;
import static org.objectweb.asm.Opcodes.FRETURN;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.IALOAD;
import static org.objectweb.asm.Opcodes.IASTORE;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.LALOAD;
import static org.objectweb.asm.Opcodes.LASTORE;
import static org.objectweb.asm.Opcodes.LLOAD;
import static org.objectweb.asm.Opcodes.LRETURN;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.PUTSTATIC;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.SALOAD;
import static org.objectweb.asm.Opcodes.SASTORE;
import static org.objectweb.asm.Opcodes.V1_8;

import java.util.List;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
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
public class XXXdont_use_JCasCoverClassFactory {
 
  private static final boolean GET = true;
  private static final boolean SET = false;
  
  private static final String CAS_RUN_EX = "org/apache/uima/cas/CASRuntimeException";
  
  private int javaClassVersion = V1_8; // correspond to Java 8

  // shared state for the type
  private TypeImpl type;
  private TypeSystemImpl tsi;
  private ClassNode cn;  
  /**
   * x/y/z form 
   */
  private String typeJavaClassName;  // in x/y/z format
  private String typeJavaDescriptor;
  
  // shared state for a feature
  private FeatureImpl fi;
  private String domainClassName;
  private String rangeJavaDescriptor;
  private String rangeArrayElementJavaDescriptor;
  private String featureFieldName; 
  
  /**
   * Create - no customization case
   *          not used for TOP or other built-in predefined types
   * @return the class as a byte array
   */
  byte[] createJCasCoverClass(TypeImpl type) {
    this.type = type;
    this.tsi = (TypeSystemImpl) type.getTypeSystem();
    typeJavaDescriptor = type.getJavaDescriptor();
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
        fi.getGetterSetterName(GET), 
        "()" + rangeJavaDescriptor, null, null);
    InsnList il = mn.instructions;
    il.add(new VarInsnNode(ALOAD,  0));
    il.add(new FieldInsnNode(GETFIELD, domainClassName, featureFieldName, rangeJavaDescriptor));
    il.add(new InsnNode(getReturnInst(fi)));
    mn.maxStack = 1;  // for instructions, LIFO access only
    mn.maxLocals = 1; // local starts with this plus any args
    cn.methods.add(mn);
    
    mn = new MethodNode(ASM5, ACC_PUBLIC, 
        fi.getGetterSetterName(SET), 
        "(" + rangeJavaDescriptor + ")V", null, null);
    il = mn.instructions;
    il.add(new VarInsnNode(ALOAD,  0));
    il.add(new VarInsnNode(getLoadInst(fi), 1));        // load ref, or primitive value
    il.add(new FieldInsnNode(PUTFIELD, domainClassName, featureFieldName, rangeJavaDescriptor));
    il.add(new InsnNode(RETURN));
    mn.maxStack = 2;   // 2 args (this and v) for putfield
    mn.maxLocals = 2;  // this and the index
    cn.methods.add(mn); 
  }
  
  private void addArrayFeatureGetSet() {
    MethodNode mn = new MethodNode(ASM5, ACC_PUBLIC,
        fi.getGetterSetterName(GET), 
        "(I)" + rangeArrayElementJavaDescriptor, null, null);
    InsnList il = mn.instructions;
    il.add(new VarInsnNode(ALOAD,  0));
    il.add(new FieldInsnNode(GETFIELD, domainClassName, featureFieldName, rangeJavaDescriptor));
    il.add(new VarInsnNode(ILOAD,  1));
    il.add(new InsnNode(getArrayLoadInst(fi)));
    il.add(new InsnNode(getReturnInst(fi)));
    mn.maxStack = 2;  // for array getter, needs array and index
    mn.maxLocals = 2; // this and index
    cn.methods.add(mn);

    mn = new MethodNode(ASM5, ACC_PUBLIC,
        fi.getGetterSetterName(SET), 
        "(I" + rangeArrayElementJavaDescriptor + ")V", null, null);
    il = mn.instructions;
    il.add(new VarInsnNode(ALOAD,  0));
    il.add(new FieldInsnNode(GETFIELD, domainClassName, featureFieldName, rangeJavaDescriptor));
    il.add(new VarInsnNode(ILOAD,  1));
    il.add(new VarInsnNode(getLoadInst(fi), 2));
    il.add(new InsnNode(getArrayStoreInst(fi)));
    il.add(new InsnNode(RETURN));
    mn.maxStack = 3;  // fieldref, index, value
    mn.maxLocals = 3; // this index and value
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
   *   need one map per class (e.g., static?)
   *   hard to use array indirection - many variants in signatures
   *
   *   separate getters/setters for space allocation - getters more referenced
   *
   * one of
   *   boolean, byte, short, int, long, float, double
   *   String, FeatureStructure,
   *   Object (used for additional java objects like Map, List)
   *   and arrays of the above
   *
   *   set of getters, one per return type
   *     each: switch on offset, return getXXX()
   *     
   *   set of setters, one per arg type:
   *     each: switch on offset, return setXXX(value).
   *     
   *   default for switch: for field not in group: 
   *     either: have all values in case statement, or
   *             just have introduced values, then super.xxx  
   *        
   */
  
  private void createSwitchSetter() {
    makeTypedSwitchSetter(tsi.booleanType);
    makeTypedSwitchSetter(tsi.byteType);
    makeTypedSwitchSetter(tsi.shortType);
    makeTypedSwitchSetter(tsi.intType);
    makeTypedSwitchSetter(tsi.longType);
    makeTypedSwitchSetter(tsi.floatType);
    makeTypedSwitchSetter(tsi.doubleType);
    makeTypedSwitchSetter(tsi.stringType);
    makeTypedSwitchSetter(tsi.topType);
    makeTypedSwitchSetter(tsi.javaObjectType);
    
    makeTypedSwitchSetter(tsi.booleanArrayType);
    makeTypedSwitchSetter(tsi.byteArrayType);
    makeTypedSwitchSetter(tsi.shortArrayType);
    makeTypedSwitchSetter(tsi.intArrayType);
    makeTypedSwitchSetter(tsi.longArrayType);
    makeTypedSwitchSetter(tsi.floatArrayType);
    makeTypedSwitchSetter(tsi.doubleArrayType);
    makeTypedSwitchSetter(tsi.stringArrayType);
    makeTypedSwitchSetter(tsi.topArrayType);
    makeTypedSwitchSetter(tsi.javaObjectArrayType);
  }
    
  private void makeTypedSwitchSetter(TypeImpl featureRangeType) {
    List<FeatureImpl> features = type.getFeaturesSharingRange(featureRangeType);
    if (null == features || features.size() == 0) {
      return;
    }
    final int nbrFeats = features.size();
       
    String rangeName = featureRangeType.isArray() ? ((TypeImplArray) featureRangeType).getComponentType().getName() + "Array" : featureRangeType.getShortName();
    String javaDesc = featureRangeType.getJavaDescriptor();
      
    // these two are for the method calls to the actual getters/setters
    String getterJavaDesc = "()" + javaDesc;
    String setterJavaDesc = "(" + javaDesc + ")V"; 
    
    MethodNode mn = new MethodNode(ASM5, ACC_PUBLIC,    //  Getter
        "_get" + rangeName, 
        "(I)" + javaDesc, null, null);
    InsnList il = mn.instructions;
    il.add(new VarInsnNode(ILOAD,  1));
    LabelNode[] labelNodes = new LabelNode[nbrFeats];   // one for each feature of this range type
    int[] offsets = new int[nbrFeats];
    for (int i = 0; i < nbrFeats; i++) {
      labelNodes[i] = new LabelNode(new Label());
      offsets[i] = features.get(i).getOffsetForGenerics();
    }
    LabelNode defaultLabelNode = new LabelNode(new Label());
    
    il.add(new LookupSwitchInsnNode(defaultLabelNode, offsets, labelNodes));
    
    for (int i = 0; i < nbrFeats; i++) {
      FeatureImpl fi = features.get(i);
      il.add(labelNodes[i]);
                        //   type,        nLocal, local, nStack, stack
      il.add(new FrameNode(Opcodes.F_SAME, 0,      null, 0,      null ));
      il.add(new VarInsnNode(ALOAD, 0));  // load this.
      il.add(new MethodInsnNode(INVOKEVIRTUAL, typeJavaClassName, fi.getGetterSetterName(GET), getterJavaDesc, false));
      il.add(new InsnNode(getReturnInst(fi)));
    }
    il.add(defaultLabelNode);
    il.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null ));
    il.add(new TypeInsnNode(NEW, CAS_RUN_EX));
    il.add(new InsnNode(DUP));
    il.add(new LdcInsnNode("INAPPROP_FEAT_X"));
    il.add(new MethodInsnNode(INVOKESPECIAL, CAS_RUN_EX, "<init>", "(Ljava/lang/String;)V", false));
    il.add(new InsnNode(ATHROW));
    mn.maxStack = 3;    
    mn.maxLocals = 2;  // this and offset
    cn.methods.add(mn);

    mn = new MethodNode(ASM5, ACC_PUBLIC,    //  Setter 
        "_set" + rangeName, 
        "(I" + javaDesc + ")V", null, null);
    il = mn.instructions;
    il.add(new VarInsnNode(ILOAD,  1));
    labelNodes = new LabelNode[nbrFeats];   // one for each feature
    for (int i = 0; i < nbrFeats; i++) {
      labelNodes[i] = new LabelNode(new Label());
    }
    defaultLabelNode = new LabelNode(new Label());
    
    il.add(new LookupSwitchInsnNode(defaultLabelNode, offsets, labelNodes));
    for (int i = 0; i < nbrFeats; i++) {
      FeatureImpl fi = features.get(i);
      il.add(labelNodes[i]);
                        //   type,        nLocal, local, nStack, stack
      il.add(new FrameNode(Opcodes.F_SAME, 0,      null, 0,      null ));
      il.add(new VarInsnNode(ALOAD, 0));  // load this.
      il.add(new VarInsnNode(getLoadInst(fi), 2));
      il.add(new MethodInsnNode(INVOKEVIRTUAL, typeJavaClassName, fi.getGetterSetterName(SET), setterJavaDesc, false));
      il.add(new InsnNode(RETURN));
    }
    il.add(defaultLabelNode);
    il.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null ));
    il.add(new TypeInsnNode(NEW, CAS_RUN_EX));
    il.add(new InsnNode(DUP));
    il.add(new LdcInsnNode("INAPPROP_FEAT_X"));
    il.add(new MethodInsnNode(INVOKESPECIAL, CAS_RUN_EX, "<init>", "(Ljava/lang/String;)V", false));
    il.add(new InsnNode(ATHROW));
    mn.maxStack = 3;    
    mn.maxLocals = 2;  // this and offset
    cn.methods.add(mn);   
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
