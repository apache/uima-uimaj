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

package org.apache.uima.uimacpp;

import org.apache.uima.UIMARuntimeException;
import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.analysis_engine.TypeOrFeature;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.admin.CASMgr;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.CASMgrSerializer;
import org.apache.uima.cas.impl.CASSerializer;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.impl.Serialization;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.internal.util.IntVector;

public class UimacppEngine {

  // Those types can not be created with CAS.createFS().
  private static String[] compatibleTafJniVersions = { "2.0" };

  static {
    String uimacpp_lib = "uima";
    String debug = System.getProperty("DEBUG_UIMACPP");
    if (debug != null) {
      String osname = System.getProperty("os.name");
      if (osname.startsWith("Windows")) {
        uimacpp_lib = "uimaD";
      }
    }
    System.loadLibrary(uimacpp_lib);
    try {
      String jniVersion = getTafJNIVersion();
      boolean compatible = false;
      for (int i = 0; i < compatibleTafJniVersions.length; i++) {
        if (jniVersion.equals(compatibleTafJniVersions[i])) {
          compatible = true;
          break;
        }
      }
      if (compatible) {
        createResourceManager();
      } else {
        throw new UIMARuntimeException(UIMARuntimeException.INCOMPATIBLE_TAF_JNI_LIBRARY,
                new Object[] { jniVersion });
      }

    } catch (UimacppException exc) {
      throw new UIMARuntimeException(exc.getEmbeddedException());
    }
  }

  /**
   * this field is the physical pointer to the C++ TafClEngine object this TafWrapper object is
   * associated with. Do not use explicitly anywhere. It is set automatically by the
   * <code>constructorJNI()</code> method and set to 0 by the <code>destructorJNI()</code>
   * method.
   */
  long cppEnginePointer = 0;

  CAS cas = null;

  boolean hasNext = false;

  // creation of TAF resource manager
  private static native void createResourceManagerJNI() throws InternalTafException;

  // configuration of the TAF resource manager
  private static native void configureResourceManagerJNI(String workDir, String dataDir)
          throws InternalTafException;

  // constructor
  private native void constructorJNI() throws InternalTafException;

  // destructor
  private native void destructorJNI() throws InternalTafException;

  // TAF functions
  private native void initializeJNI(String configFile) throws InternalTafException;

  private native void typeSystemInitJNI(String[] typeNames, String[] featureNames,
          int[] typeInheritance, int[] featDecls, int topTypeCode, int[] featureOffsets,
          int[] typeOrder, int[] stringSubTypes, String[] stringSubTypeValues,
          int[] stringSubTypeValuePos, String[] indexNames, int[] nameToIndexMap,
          int[] indexingStrategy, int[] comparatorIndex, int[] comparators)
          throws InternalTafException;

  private native void destroyJNI() throws InternalTafException;

  private native void resetJNI() throws InternalTafException;

  /*
   * private native void fillCASJNI(int[] heapArray, int[] fsIndex, String[] stringTable);
   */
  private native void fillCASJNI(int[] heapArray, int[] fsIndex, String[] stringTable,
          byte[] byteArray, short[] shortArray, long[] longArray);

  private native void processJNI(int isTCas, String sofaName, int[] resultSpecTypes,
          int[] resultSpecFeatures) throws InternalTafException;

  // set the parameter to true if you want per document data to be serialized
  // false only for type system and index definitions serialization
  private native void serializeCASJNI(boolean bSerializeData) throws InternalTafException;

  private native Object getSerializedDataJNI(int what) throws InternalTafException;

  private native void batchProcessCompleteJNI() throws InternalTafException;

  private native void collectionProcessCompleteJNI() throws InternalTafException;

  // JNI calls to support segmenter
  private native boolean hasNextSegmentJNI() throws InternalTafException;

  private native void nextSegmentJNI() throws InternalTafException;

  private native void serializeSegmentJNI(boolean bSerializeData) throws InternalTafException;

  private native Object getSerializedSegmentDataJNI(int what) throws InternalTafException;

  private native void releaseSegmentJNI() throws InternalTafException;

  /*
   * serialized CAS data components - supported args to getSerializedDataJNI and
   * getSegmentSerializedDataJNI. Many of these are unread and have been commented out but left here
   * for documentation purposes.
   */
  // private static final int TYPE_INH = 0;
  //
  // private static final int FEATURE_DEF = 1;
  //
  // private static final int FEATURE_OFFSET = 2;
  //
  // private static final int TYPE_SYMBOLS = 3;
  //
  // private static final int FEATURE_SYMBOLS = 4;
  //
  // private static final int TOPTYPE = 5;
  //
  // private static final int TYPE_PRIORITIES = 6;
  private static final int FSHEAP = 10;

  private static final int STRINGSYMBOL = 11;

  // private static final int DOCUMENT = 20;

  private static final int INDEXEDFSS = 30;

  // private static final int INDEXID = 40;
  //
  // private static final int INDEXKIND = 41;
  //
  // private static final int COMPARATORDEF = 42;
  //
  // private static final int COMPARATORSTART = 43;

  private static final int BYTEHEAP = 12;

  private static final int SHORTHEAP = 13;

  private static final int LONGHEAP = 14;

  // helper functions
  private static native String getErrorMessageJNI(long errorId) throws InternalTafException;

  private static native String getVersionJNI() throws InternalTafException;

  /**
   * Create the TAF resource manager.
   */
  private static void createResourceManager() throws UimacppException {
    try {
      createResourceManagerJNI();
    } catch (Exception exc) {
      throwJTafException(exc);
    }
  }

  /**
   * Configure the TAF Resource Manager.
   * @param workDirectory the work directory
   * @param dataDirectory the data directory
   * @throws UimacppException wraps any exception
   */
  public static void configureResourceManager(String workDirectory, String dataDirectory)
          throws UimacppException {
    try {
      configureResourceManagerJNI(workDirectory, dataDirectory);
    } catch (Exception exc) {
      throwJTafException(exc);
    }
  }

  public UimacppEngine() throws UimacppException {
    try {
      constructorJNI();
    } catch (Exception exc) {
      throwJTafException(exc);
    }
  }

  // ////////////////////////////////////////////////////////
  // implementation of the AE interface
  // getAnalysisEngineMetaData, getView, process, reset, and destroy.

  /**
   * create a TAF engine with a config file
   * @param configFile the configuration file used for initialization
   * @return a UimacppEngine
   * @throws UimacppException pass thru
   */
  public static UimacppEngine createJTafTAE(String configFile) throws UimacppException {
    UimacppEngine result = new UimacppEngine();
    result.initialize(configFile);

    return result;
  }

  static void printArray(String s, int[] ar) {
    System.out.println("int array " + s);
    for (int i = 0; i < ar.length; ++i) {
      System.out.println(i + ": " + ar[i]);
    }
    System.out.println();
  }

  static void printArray(String s, String[] ar) {
    System.out.println("String array " + s);
    for (int i = 0; i < ar.length; ++i) {
      System.out.println(i + ": " + ar[i]);
    }
    System.out.println();
  }

  // ////////////////////////////////////////////////////////

  /**
   * initialize Taf engine
   * 
   * @param configFile
   *          the configuration as a string (not a filename)
   */
  void initialize(String config) throws UimacppException {
    // for (int i = 0; i < casMgrSerializer.indexNames.length; i++) {
    // System.out.println(casMgrSerializer.indexNames[i]);
    // }
    try {
      initializeJNI(config);
    } catch (Exception exc) {
      throwJTafException(exc);
    }
  }

  // ////////////////////////////////////////////////////////

  /**
   * reinit Taf engine type system
   * 
   * @param serialized
   *          CAS definition
   */
  void typeSystemInit(CASMgrSerializer casMgrSerializer) throws UimacppException {
    // for (int i = 0; i < casMgrSerializer.indexNames.length; i++) {
    // System.out.println(casMgrSerializer.indexNames[i]);

    try {
      typeSystemInitJNI(casMgrSerializer.typeNames, casMgrSerializer.featureNames,
              casMgrSerializer.typeInheritance, casMgrSerializer.featDecls,
              casMgrSerializer.topTypeCode, casMgrSerializer.featureOffsets,
              casMgrSerializer.typeOrder, casMgrSerializer.stringSubtypes,
              casMgrSerializer.stringSubtypeValues, casMgrSerializer.stringSubtypeValuePos,
              casMgrSerializer.indexNames, casMgrSerializer.nameToIndexMap,
              casMgrSerializer.indexingStrategy, casMgrSerializer.comparatorIndex,
              casMgrSerializer.comparators);
    } catch (Exception exc) {
      throwJTafException(exc);
    }
  }

  /**
   * de-initializes the TAF engine.
   * @throws UimacppException wraps any exception
   */
  public void destroy() throws UimacppException {
    try {
      destroyJNI();
    } catch (Exception exc) {
      throwJTafException(exc);
    }
  }

  private static void serializeResultSpecification(ResultSpecification rs, CASImpl cas,
          IntVector resultSpecTypes, IntVector resultSpecFeatures) {
    TypeOrFeature[] tofs = rs.getResultTypesAndFeatures();
    TypeSystemImpl tsImpl = cas.getTypeSystemImpl();
    for (int i = 0; i < tofs.length; ++i) {
      if (tofs[i].isType()) {
        TypeImpl t = (TypeImpl) tsImpl.getType(tofs[i].getName());
        resultSpecTypes.add(t.getCode());
      } else {
        FeatureImpl f = (FeatureImpl) tsImpl.getFeatureByFullName(tofs[i].getName());
        resultSpecFeatures.add(f.getCode());
      }
    }
  }

  /**
   * process the document.
   * @param rs the result specification
   * @param aCas the CAS
   * @param casIsEmpty tbd
   * @throws UimacppException wraps any exception
   */
  public void process(ResultSpecification rs, CAS aCas, boolean casIsEmpty) throws UimacppException {

    int isTCas=0;
    String sofaName=aCas.getViewName();
    if (sofaName != null) {
      isTCas=1;
    }

    cas = aCas.getCurrentView();

    try {
      resetJNI();

      if (!casIsEmpty) {
        CASSerializer casSerializerIn = Serialization.serializeCAS(cas);
        /**
         * fillCASJNI(casSerializerIn.heapArray, casSerializerIn.fsIndex,
         * casSerializerIn.stringTable);
         */

        fillCASJNI(casSerializerIn.heapArray, casSerializerIn.fsIndex, casSerializerIn.stringTable,
                casSerializerIn.byteHeapArray, casSerializerIn.shortHeapArray,
                casSerializerIn.longHeapArray);
      }

      IntVector resultSpecTypes = new IntVector();
      IntVector resultSpecFeatures = new IntVector();
      if (rs != null) {

        serializeResultSpecification(rs, (CASImpl) cas, resultSpecTypes, resultSpecFeatures);
      }

      processJNI(isTCas, sofaName, resultSpecTypes.toArray(), resultSpecFeatures.toArray());

      // call hasNext() to see if this returns segments
      // if there are no segments this will get the
      // CAS data.
      if (hasNext()) {
        return;
      }

    } catch (Exception exc) {
      throwJTafException(exc);
    }
  }

  /**
   * hasNext
   * @return true if there's a next element
   * @throws UimacppException wraps any exception
   */
  public boolean hasNext() throws UimacppException {

    try {
      hasNext = hasNextSegmentJNI();
      // get the CAS data of the original input CAS.
      if (!hasNext) {
        CASSerializer casSerializerOut = new CASSerializer();
        // per document data
        serializeCASJNI(true);
        casSerializerOut.heapMetaData = null;
        casSerializerOut.heapArray = (int[]) getSerializedDataJNI(FSHEAP);
        casSerializerOut.fsIndex = (int[]) getSerializedDataJNI(INDEXEDFSS);
        casSerializerOut.stringTable = (String[]) getSerializedDataJNI(STRINGSYMBOL);

        casSerializerOut.byteHeapArray = (byte[]) getSerializedDataJNI(BYTEHEAP);
        casSerializerOut.shortHeapArray = (short[]) getSerializedDataJNI(SHORTHEAP);
        casSerializerOut.longHeapArray = (long[]) getSerializedDataJNI(LONGHEAP);

        CASMgr casMgr = (CASMgr) cas;
        CAS newCAS = Serialization.createCAS(casMgr, casSerializerOut);
        if (newCAS != casMgr) {
          throw new RuntimeException("CASMgr and CAS should be identical");
        }
      }
    } catch (Exception exc) {
      throwJTafException(exc);
    }
    return hasNext;
  }

  /**
   * next
   * @param segment tbd 
   * @throws UimacppException wraps any exception
   */
  public void next(CAS segment) throws UimacppException {

    try {

      // get the CAS data of CAS produce by segmenter component.
      if (hasNext) {

        nextSegmentJNI();
        CASSerializer casSerializerOut = new CASSerializer();
        // per document data
        serializeSegmentJNI(true);
        casSerializerOut.heapMetaData = null;
        casSerializerOut.heapArray = (int[]) getSerializedSegmentDataJNI(FSHEAP);
        casSerializerOut.fsIndex = (int[]) getSerializedSegmentDataJNI(INDEXEDFSS);
        casSerializerOut.stringTable = (String[]) getSerializedSegmentDataJNI(STRINGSYMBOL);

        casSerializerOut.byteHeapArray = (byte[]) getSerializedSegmentDataJNI(BYTEHEAP);
        casSerializerOut.shortHeapArray = (short[]) getSerializedSegmentDataJNI(SHORTHEAP);
        casSerializerOut.longHeapArray = (long[]) getSerializedSegmentDataJNI(LONGHEAP);
        releaseSegmentJNI();

        CASMgr casMgr = (CASMgr) segment;
        CAS newCAS = Serialization.createCAS(casMgr, casSerializerOut);
        if (newCAS != casMgr) {
          throw new RuntimeException("CASMgr and CAS should be identical");
        }
      } else {
        throw new RuntimeException("This analysis component has no CASs to return.");
      }
    } catch (Exception exc) {
      throwJTafException(exc);
    }

  }

  /**
   * batchProcessComplete
   * @throws UimacppException wraps any exception
   */
  public void batchProcessComplete() throws UimacppException {
    try {
      batchProcessCompleteJNI();
    } catch (Exception exc) {
      throwJTafException(exc);
    }
  }

  /**
   * CasConsumer collectionProcessComplete
   * @throws UimacppException wraps any exception
   */
  public void collectionProcessComplete() throws UimacppException {
    try {
      collectionProcessCompleteJNI();
    } catch (Exception exc) {
      throwJTafException(exc);
    }
  }

  /**
   * helper function to get the error message for some TAF error ID.
   * @param errorCode the code used as the key to look up the error message
   * @return the error message
   * @throws UimacppException wraps any exception
   */
  public static String getErrorMessage(long errorCode) throws UimacppException {
    try {
      return getErrorMessageJNI(errorCode);
    } catch (Exception exc) {
      throwJTafException(exc);
    }
    return null;
  }

  /**
   * helper function to get the TAF JNI version.
   * @return Taf JNI Version
   * @throws UimacppException wraps any exception
   */
  public static String getTafJNIVersion() throws UimacppException {
    try {
      return getVersionJNI();
    } catch (Exception exc) {
      throwJTafException(exc);
    }
    return null;
  }

  static void throwJTafException(Exception exc) throws UimacppException {
    if (exc instanceof InternalTafException) {
      InternalTafException itExc = (InternalTafException) exc;

      // check special errors
      long l = itExc.getTafErrorCode();
      switch ((int) l) {
        case 1000:
        case 5000:
        case 10000:
          throw new OutOfMemoryError();
      }

      // TafException tafException = new TafException(
      // itExc.getTafErrorCode(), exc.getMessage() );
      throw new UimacppException(itExc);
    } else {
      throw new UimacppException(exc);
    }
  }

  // //////////////////////////////////////////////////
  protected void finalize() throws Throwable {
    synchronized (this) {
      if (cppEnginePointer != 0) {
        try {
          // sets cppEnginePointer to 0
          destructorJNI();
        } catch (Exception exc) {
          throw new UIMARuntimeException(exc);
        }
      }
      super.finalize();
    }
  }

}

class InternalTafException extends Exception {
  private static final long serialVersionUID = -6558646639254861394L;

  long errorCode = -1;

  public InternalTafException(String message, long error) {
    super(message + " (" + error + ")");
    errorCode = error;
  }

  public long getTafErrorCode() {
    return errorCode;
  }

  public String getMessage() {
    return super.getMessage();
  }

}
