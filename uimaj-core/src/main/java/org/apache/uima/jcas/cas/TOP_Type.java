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

package org.apache.uima.jcas.cas;

import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FSGenerator;
import org.apache.uima.cas.impl.LowLevelCAS;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.jcas.JCas;

// *********************************
// * Implementation of TOP_Type *
// *   Not used, only present to avoid compile errors
// *   for old v2 style _Type classes
// *********************************
/**
 * hold Cas type information, link to JCas instance. One instance per Type, per CAS
 * 
 * @deprecated
 */
@Deprecated
public class TOP_Type {

  /**
   * each cover class when loaded sets an index. used in the JCas typeArray to go from the cover
   * class or class instance to the corresponding instance of the _Type class
   */
  public static final int typeIndexID = -1;

  // public final static int type = typeIndexID;

  /**
   * used to obtain reference to the TOP_Type instance
   * 
   * @return the type array index
   */
  public int getTypeIndexID() {
    return typeIndexID;
  }

  /** the Cas Type associated with this Java Cas Model class */
  public final Type casType; // JCas may refer to it?

  public final int casTypeCode; // public so getters/setters in another package can see

  /** reference to the Java Cas root */
  public final JCas jcas; // public so _type generators can find it

  /** ref to CAS for faster getters/setters */
  public final CASImpl casImpl;

  public final LowLevelCAS ll_cas;

  // switch this to true if debugging wanted
  protected final boolean lowLevelTypeChecks; // if true check int type

  protected final boolean lowLevelArrayBoundChecks;

  // next has to be public to be visible to inner class?
  public final boolean useExistingInstance; // if true, implements reuse of existing instance

  // generator used by the CAS system when it needs to make a new instance
  /**
   * This generator can be set up to either get the identical Java object (if it exists) or just
   * make a new one. If making a new one - the java object made cannot contain any other data
   * (because updates won't be reflected). Each class generated by JCasGen either does this (if it
   * has additional java fields imbedded) or not.
   */

  // protected because field shared by all subtypes
  // next field no longer needed except for backwards compatibility
  // new subtypes of TOP_Type use <class-name>.this instead
  protected final TOP_Type instanceOf_Type; // allow ref to this in inner class

  protected FSGenerator<?> getFSGenerator() {
    return null; // no longer used, but may be needed for compatibility with older existing
                 // JCasGen'd cover classes that might extend this class
  }
  //
  // private FSGenerator fsGenerator = new FSGenerator() {
  // @SuppressWarnings("unchecked")
  // public TOP createFS(int addr, CASImpl cas) {
  // if (TOP_Type.this.useExistingInstance) {
  // // Return eq fs instance if already created
  // TOP fs = TOP_Type.this.jcas.getJfsFromCaddr(addr);
  // if (null == fs) {
  // fs = new TOP(addr, TOP_Type.this);
  // TOP_Type.this.jcas.putJfsFromCaddr(addr, fs);
  // return fs;
  // }
  // return fs;
  // } else
  // return new TOP(addr, TOP_Type.this);
  // }
  // };

  // cas.getKnownJCas().getType(TOP.typeIndexID));}

  // private static final String casTypeName = CAS.TYPE_NAME_TOP;
  // private String jTypeName = CAS.TYPE_NAME_TOP;
  // protected String getCasTypeName() {return casTypeName;}
  // protected String getJTypeName() {return jTypeName;}

  /**
   * protected constructor - disable default constructor - never called.
   */
  protected TOP_Type() { // block default new operator
    instanceOf_Type = null;
    jcas = null;
    casTypeCode = 0;
    casType = null;
    casImpl = null;
    lowLevelTypeChecks = false;
    lowLevelArrayBoundChecks = false;
    useExistingInstance = true;
    ll_cas = null;
    // used for test mocking
    // throw new RuntimeException("Internal Error-this constructor should never be called.");
  }

  /*
   * Internal - this constructor is called when new CAS creates corresponding jcas instance. During
   * this process, all the types defined in the CAS are used to see if there are any corresponding
   * jcas type defs defined. If so, they are loaded. This constructor is called via its being the
   * superclass of classes being loaded.
   */

  // constructor execution order: 1: super, 2: instance expr, 3: body
  public TOP_Type(JCas jcas, Type casType) {
    this(jcas, casType, true);
  }

  /*
   * DO NOT USE - for backwards compatibility only.
   */
  // constructor execution order: 1: super, 2: instance expr, 3: body
  protected TOP_Type(JCas jcas, Type casType, boolean installGenerator) {
    this.jcas = jcas;
    casImpl = jcas.getCasImpl();
    ll_cas = casImpl;
    this.casType = casType;
    instanceOf_Type = this;
    casTypeCode = ((TypeImpl) this.casType).getCode();
    lowLevelTypeChecks = false;
    lowLevelArrayBoundChecks = false;
    useExistingInstance = true;

    // Add generator to CASImpl for this type
    // NOTE Getter used for FSGenerator to get the subtype instance value
    // NOTE Above comment is irrelevant - because this call has to be in
    // every subclass, because at the time this executes in TOP_Type,
    // it is because the subclass invoked it via super(x,y), and the
    // subclass's instance vars haven't yet been set
    // Solution: every superClass that could be instantiated (TOP is not one of them)
    // during the super calls will incorrectly set the generator for the casType, but
    // this is OK because after the supers all run, the bottom one runs and sets it correctly.

    // if (installGenerator) {
    // ((CASImpl) ll_cas).getFSClassRegistry().addGeneratorForType((TypeImpl) this.casType,
    // getFSGenerator());
    // }
  }

  // // ************ No Object support ********************
  // protected void checkType(int inst) {
  // if (!casImpl.getTypeSystemImpl().subsumes(casTypeCode, casImpl.getHeapValue(inst)))
  // invalidTypeArg(inst);
  // }
  //
  // protected void invalidTypeArg(int inst) {
  // CASRuntimeException e = new CASRuntimeException(CASRuntimeException.INAPPROP_TYPE,
  // new String[] { casType.getName(),
  // this.casImpl.getTypeSystemImpl().ll_getTypeForCode(casImpl.getHeapValue(inst)).getName() });
  // throw e;
  // }

  /**
   * add the corresponding FeatureStructure to all Cas indexes
   * 
   * @param inst
   *          the low level CAS Feature Structure reference
   */
  public void addToIndexes(int inst) {
    jcas.getLowLevelIndexRepository().ll_addFS(inst);
  }

  /**
   * remove the corresponding FeatureStructure from all Cas indexes
   * 
   * @param inst
   *          the low level CAS Feature Structure reference
   */
  public void removeFromIndexes(int inst) {
    jcas.getLowLevelIndexRepository().ll_removeFS(inst);
  }

  public int noObjCreate() {
    return casImpl.ll_createFS(casTypeCode);
  }
}
