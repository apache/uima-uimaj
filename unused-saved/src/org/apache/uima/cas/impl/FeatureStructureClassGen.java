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
import static org.objectweb.asm.Opcodes.F_SAME;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
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
 * Support for creating Feature Structure Classes
 * 
 *   In v3, Feature Structures are represented as instances of Java classes
 *   
 *   See the package-info for a description of the structure of these classes.
 *   
 *   The class name corresponds to the UIMA Type name including the package.
 *     - some exceptions for internal types
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
public class FeatureStructureClassGen {
  private final static boolean GET = true;
  private final static boolean SET = false;
  private final static boolean[] GET_SET = {GET, SET};

  private final static int JAVA_CLASS_VERSION = V1_8; // correspond to Java 8 
  private static final String CAS_RUN_EX = "org/apache/uima/cas/CASRuntimeException";
  
  // shared state for the type
  private TypeImpl type;
  private ClassNode cn;  
  /**
   * x/y/z form 
   */
  private String typeJavaClassName;  // in x/y/z format
  private String typeJavaDescriptor;
  
  // shared state for a feature
  private FeatureImpl fi;
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
    typeJavaDescriptor = type.getJavaDescriptor();
    typeJavaClassName = type.getName().replace('.', '/');
    cn = new ClassNode(ASM5); // java 8
    cn.version = JAVA_CLASS_VERSION;
    cn.access = ACC_PUBLIC + ACC_SUPER;
    cn.name = typeJavaClassName;   
    cn.superName = type.getSuperType().getName().replace('.', '/');
//    cn.interfaces = typeImpl.getInterfaceNamesArray();   // TODO
    
    // add the "_typeImpl" field - this has a ref to the TypeImpl for this class
    cn.fields.add(new FieldNode(ACC_PUBLIC + ACC_FINAL + ACC_STATIC,
        "_typeImpl", "Lorg/apache/uima/type_system/impl/TypeImpl;", null, null));
    
    // add field declares, and getters and setters, and special getters/setters for array things    
    type.getMergedStaticFeaturesIntroducedByThisType().stream()
          .forEach(this::addFeatureFieldGetSet);
 
    addStaticInitAndConstructors();
    
    createSwitchGettersAndSetters();
    
    
    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
    cn.accept(cw);
    return cw.toByteArray();
  }
  
  /**
   * Not called for built-in types
   * @param type the type to generate
   * @return the bytecode for that type
   */
  byte[] createJCas_TypeCoverClass(TypeImpl type) {
    this.type = type;
    typeJavaDescriptor = type.getJavaDescriptor();
    typeJavaClassName = type.getName().replace('.', '/') + "_Type";
    cn = new ClassNode(ASM5); // java 8
    cn.version = JAVA_CLASS_VERSION;
    cn.access = ACC_PUBLIC + ACC_SUPER;
    cn.name = typeJavaClassName;   
    cn.superName = type.getSuperType().getName().replace('.', '/') + "_Type";

    // TODO
    
    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
    cn.accept(cw);
    return cw.toByteArray();
  }
  
  private void addFeatureFieldGetSet(FeatureImpl fi) {
    
    // compute shared info for this feature
    this.fi = fi;
    featureFieldName = getFeatureFieldName(fi);
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
    for (boolean isGet : GET_SET) {
      MethodNode mn = new MethodNode(ASM5, ACC_PUBLIC,    //  Get for non-array value
          fi.getGetterSetterName(isGet), 
          isGet ? ("()" + rangeJavaDescriptor) 
                : "(" + rangeJavaDescriptor + ")V", 
          null, null);
      InsnList il = mn.instructions;
      il.add(new VarInsnNode(ALOAD,  0));
      if (isGet) {
        il.add(new FieldInsnNode(GETFIELD, typeJavaClassName, featureFieldName, rangeJavaDescriptor));
        il.add(new InsnNode(getReturnInst(fi)));
      } else {
        il.add(new VarInsnNode(getLoadInst(fi), 1));        // load ref, or primitive value
        il.add(new FieldInsnNode(PUTFIELD, typeJavaClassName, featureFieldName, rangeJavaDescriptor));
        il.add(new InsnNode(RETURN));
      }
      final boolean is2slotValue = ((TypeImpl) fi.getRange()).isLongOrDouble();
      mn.maxStack  = isGet ? 1 : is2slotValue ? 3 : 2;
      mn.maxLocals = isGet ? 1 : is2slotValue ? 3 : 2;
      cn.methods.add(mn);    
    }    
  }
  
  private void addArrayFeatureGetSet() {
    for (boolean isGet : GET_SET) {
      MethodNode mn = new MethodNode(ASM5, ACC_PUBLIC,
          fi.getGetterSetterName(isGet),
          isGet ? "(I)" + rangeArrayElementJavaDescriptor
                : "(I" + rangeArrayElementJavaDescriptor + ")V",
          null, null);
      InsnList il = mn.instructions;
      il.add(new VarInsnNode(ALOAD,  0));
      il.add(new FieldInsnNode(GETFIELD, typeJavaClassName, featureFieldName, rangeJavaDescriptor));
      il.add(new VarInsnNode(ILOAD,  1));
      if (isGet) {
        il.add(new InsnNode(getArrayLoadInst(fi)));
        il.add(new InsnNode(getReturnInst(fi)));
      } else {
        il.add(new VarInsnNode(getArrayLoadInst(fi), 2));  // load the value to be set into the array slot
        il.add(new InsnNode(getArrayStoreInst(fi)));
        il.add(new InsnNode(RETURN));
      }
      
      final boolean is2slotValue = ((TypeImpl) fi.getRange()).isLongOrDouble();
      mn.maxStack = isGet ? 2 : (is2slotValue ? 4 : 3);
      mn.maxLocals = isGet ? 2 : (is2slotValue ? 4 : 3);
      cn.methods.add(mn);
    }
  }
  
  private void addFeatureField() {
    cn.fields.add(new FieldNode(ACC_PRIVATE, featureFieldName, rangeJavaDescriptor, null, null));
  }
  
  /**
   * To support get/set by Feature instead of by name (a form of indirection),
   * generate a method which returns an array of UIMA MethodReferences 
   * (see Functional Interfaces in org.apache.uima.jcas.impl package)
   * which can be used to get / set the value of the feature; 
   * @see methods in {@link FeatureStructureImpl}
   *   
   * The method created is       
   */
  
  private void createSwitchGettersAndSetters() {    
    Arrays.stream(allTypes).sequential().forEach(this::makeTypedSwitchGetterSetter);
  }
    
  private void makeTypedSwitchGetterSetter(TypeImpl featureRangeType) {
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

    LabelNode[] labelNodesGet = new LabelNode[nbrFeats];
    LabelNode[] labelNodesSet = new LabelNode[nbrFeats];
    int[] keys = new int[nbrFeats];
    for (int i = 0; i < nbrFeats; i++) {
      labelNodesGet[i] = new LabelNode(new Label());
      labelNodesSet[i] = new LabelNode(new Label());
      keys[i] = features.get(i).getOffsetForGenerics();
    }
    Arrays.sort(keys);
    
    LabelNode defaultLabelNodeGet = new LabelNode(new Label());
    LabelNode defaultLabelNodeSet = new LabelNode(new Label());

    for (boolean isGet : GET_SET) {
      MethodNode mn = new MethodNode(ASM5, ACC_PUBLIC,    
          "_" + (isGet ? "get" : "set") + rangeName,  // _ avoids name collision with other getters/setters 
          isGet ? ("(I)" + javaDesc) : 
                  ("(I" + javaDesc + ")V"), null, null);
      InsnList il = mn.instructions;
      il.add(new VarInsnNode(ILOAD,  1)); // load the switch int   
      il.add(new LookupSwitchInsnNode(isGet ? defaultLabelNodeGet : defaultLabelNodeSet, 
                                      keys, 
                                      isGet ? labelNodesGet : labelNodesSet));   
      
      for (int i = 0; i < nbrFeats; i++) {
        final FeatureImpl fi = features.get(i);
        il.add((isGet ? labelNodesGet : labelNodesSet)[i]);
        il.add(new FrameNode(F_SAME, 0, null, 0, null));
        il.add(new VarInsnNode(ALOAD, 0));  // load this
        if (isGet) {
          il.add(new MethodInsnNode(INVOKEVIRTUAL, typeJavaClassName, fi.getGetterSetterName(GET), getterJavaDesc, false));
          il.add(new InsnNode(getReturnInst(fi)));
        } else {
          // setter  - here we might insert code to do index corruption fixup
          il.add(new VarInsnNode(getLoadInst(fi), 2));  // load the value or value ref
          il.add(new MethodInsnNode(INVOKEVIRTUAL, typeJavaClassName, fi.getGetterSetterName(SET), setterJavaDesc, false));
          il.add(new InsnNode(RETURN));
        }
      }
      
      // default - throw
      il.add(isGet? defaultLabelNodeGet : defaultLabelNodeSet);
      il.add(new FrameNode(F_SAME, 0, null, 0, null));
      il.add(new TypeInsnNode(NEW, CAS_RUN_EX));
      il.add(new InsnNode(DUP));
      il.add(new LdcInsnNode("INAPPROP_FEAT_X"));
      il.add(new MethodInsnNode(INVOKESPECIAL, CAS_RUN_EX, "<init>", "(Ljava/lang/String;)V", false));
      il.add(new InsnNode(ATHROW));
  
      boolean is2slotValue = featureRangeType.isLongOrDouble();
      mn.maxStack = 3;   // for throw
      mn.maxLocals = isGet ? 2 : (is2slotValue ? 4 : 3);
      
      cn.methods.add(mn);
    }
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
    il.add(new MethodInsnNode(INVOKESTATIC, 
        "org/apache/uima/type_system/impl/TypeSystemImpl", 
        "getTypeImplBeingLoaded", 
        "()Lorg/apache/uima/type_system/impl/TypeImpl;", 
        false));
    il.add(new FieldInsnNode(PUTSTATIC, 
        "pkg/sample/name/SeeSample", 
        "_typeImpl", 
        "Lorg/apache/uima/type_system/impl/TypeImpl;"));
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
    if (type.isAnnotation) {
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
  
  private int getLoadInst(FeatureImpl fi) {            // load from local variable
    return getLoadInst(((TypeImpl) fi.getRange()).getCode());
  }
  
  private int getLoadInst(int typeCode) {
    switch (typeCode) {
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
  
  private int getArrayElementLoadInst(FeatureImpl fi) {
    return getLoadInst(((TypeImpl) ((TypeImplArray) fi.getRange()).getComponentType()).getCode());
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
