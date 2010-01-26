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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.admin.CASFactory;
import org.apache.uima.cas.impl.CASCompleteSerializer;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.CASMgrSerializer;
import org.apache.uima.cas.impl.CASSerializer;
import org.apache.uima.cas.impl.Serialization;
import org.apache.uima.collection.CasConsumer;
import org.apache.uima.collection.CasConsumerDescription;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.metadata.Capability;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.xml.sax.SAXException;

public class CppUimajEngine {
  private String exceptionString = "";

  private AnalysisEngine ae = null;

  private CasConsumer cc = null;

  private boolean requiresTCas = true;

  private CASImpl casImpl = null;

  private int[] heap;

  private int[] indexedFSs;

  private String[] stringSymbolTable;

  private byte[] byteHeapArray;

  private short[] shortHeapArray;

  private long[] longHeapArray;

  private void logException(Exception exc) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    exc.printStackTrace(pw);
    exceptionString = sw.toString();
    pw.close();
  }

  public CppUimajEngine() {
  }

  public int initialize(String config, String dataPath, int[] typeInheritance,
          int[] typePriorities, int[] featureDefs, int[] featureOffset, String[] typeNames,
          String[] featureNames, int[] stringSubTypes, String[] stringSubTypeValues,
          int[] stringSubTypeValuePos, String[] indexIDs, int[] indexKinds, int[] compStarts,
          int[] compDefs) {
    int result = 0;
    try {
      // System.out.println("CppUimajEngine::initialize()");
      CASMgrSerializer serializer = new CASMgrSerializer();
      serializer.typeOrder = typePriorities;
      serializer.indexNames = indexIDs;

      // trivalliy construct the name to index map
      serializer.nameToIndexMap = new int[indexIDs.length];
      for (int i = 0; i < serializer.nameToIndexMap.length; ++i) {
        serializer.nameToIndexMap[i] = i;
      }

      serializer.indexingStrategy = indexKinds;
      serializer.comparatorIndex = compStarts;
      serializer.comparators = compDefs;

      serializer.typeNames = typeNames;
      serializer.featureNames = featureNames;
      serializer.typeInheritance = typeInheritance;
      serializer.featDecls = featureDefs;
      serializer.topTypeCode = 1;
      serializer.featureOffsets = featureOffset;
      serializer.stringSubtypes = stringSubTypes;
      serializer.stringSubtypeValues = stringSubTypeValues;
      serializer.stringSubtypeValuePos = stringSubTypeValuePos;

      byte[] bar = config.getBytes("UTF-16");
      ByteArrayInputStream bais = new ByteArrayInputStream(bar);
      XMLInputSource in = new XMLInputSource(bais, null);
      ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(in);
      bais.close();

      ResourceManager resMgr = UIMAFramework.newDefaultResourceManager();
      resMgr.setDataPath(dataPath);

      if (specifier instanceof CasConsumerDescription) {
        cc = UIMAFramework.produceCasConsumer(specifier);
        CasConsumerDescription ccdesc = (CasConsumerDescription) specifier;
        Capability[] capabilities = ccdesc.getCasConsumerMetaData().getCapabilities();
        for (int i = 0; i < capabilities.length; i++) {
          String[] inputsofas = capabilities[i].getInputSofas();
          if (inputsofas.length > 0)
            requiresTCas = false;
        }
      } else {
        ae = UIMAFramework.produceAnalysisEngine(specifier, resMgr, null);
      }

      casImpl = (CASImpl) CASFactory.createCAS();
      casImpl.commitTypeSystem();

      // Create the Base indexes in order to deserialize
      casImpl.initCASIndexes();
      casImpl.getIndexRepositoryMgr().commit();

      // deserialize into this CAS
      CASCompleteSerializer completeSerializer = new CASCompleteSerializer();
      completeSerializer.setCasMgrSerializer(serializer);
      completeSerializer.setCasSerializer(Serialization.serializeCAS(casImpl));

      casImpl.reinit(completeSerializer);

      // System.out.println(cc.getProcessingResourceMetaData().getName());
    } catch (Exception exc) {
      result = 1;
      logException(exc);
    }

    return result;
  }

  String stringTableToString(String[] s) {
    StringBuffer buffer = new StringBuffer();
    for (int i = 0; i < s.length; ++i) {
      buffer.append(i + ": " + s[i] + ", ");
    }
    return buffer.toString();
  }

  public int process(String doc, int[] heapArray, int[] fsIndex, String[] stringTable,
          int[] resultSpecTypes, int[] resultSpecFeatures, int sofaNum, byte[] aByteHeapArray,
          short[] aShortHeapArray, long[] aLongHeapArray) {
    int result = 0;
    try {
      // System.err.println("CppUimajEngine.process() called");

      casImpl.reset();

      // 1. deserialize CAS

      CASSerializer serializer = new CASSerializer();
      // set serialization data
      serializer.heapArray = heapArray;
      serializer.fsIndex = fsIndex;
      serializer.stringTable = stringTable;

      serializer.byteHeapArray = aByteHeapArray;
      serializer.shortHeapArray = aShortHeapArray;
      serializer.longHeapArray = aLongHeapArray;

      casImpl.reinit(serializer);

      // 2. create result spec
      if (ae != null) {

        ResultSpecification rs = ae.createResultSpecification(casImpl.getTypeSystem());
        for (int i = 0; i < resultSpecTypes.length; ++i) {
          // allAnnotatorFeatures is not considere here! (TODO)
          rs
                  .addResultType(casImpl.getTypeSystemImpl().ll_getTypeForCode(resultSpecTypes[i]).getName(),
                          false);
        }
        for (int i = 0; i < resultSpecFeatures.length; ++i) {
          rs.addResultFeature(casImpl.getTypeSystemImpl().ll_getFeatureForCode(resultSpecFeatures[i])
                  .getName());
        }
        // 3. call process with cas
        ae.process(casImpl, rs);

      } else if (cc != null) {
        // 3. call process with tcas or cas
        if (requiresTCas && sofaNum == 0) {
          result = 1;
          exceptionString = "This CasConsumer expects a View, but the Sofa from which to construct one is not specified.";
        } else if (sofaNum > 0) {
          CAS view = casImpl.getView(sofaNum);
          cc.processCas(view);
        } else {
          cc.processCas(casImpl);
        }
      }
      // 4. deserialize CAS again
      CASSerializer deSerializer = Serialization.serializeCAS(casImpl);

      saveSerializedCAS(deSerializer);

    } catch (Exception exc) {
      result = 1;
      logException(exc);
    }
    return result;
  }

  private void saveSerializedCAS(CASSerializer deSerializer) {
    heap = deSerializer.heapArray;
    indexedFSs = deSerializer.fsIndex;
    stringSymbolTable = deSerializer.stringTable;

    byteHeapArray = deSerializer.byteHeapArray;
    shortHeapArray = deSerializer.shortHeapArray;
    longHeapArray = deSerializer.longHeapArray;

  }

  public int[] getHeap() {
    return heap;
  }

  public int[] getIndexedFSs() {
    return indexedFSs;
  }

  public String[] getStringTable() {
    return stringSymbolTable;
  }

  public byte[] getByteHeap() {
    return byteHeapArray;
  }

  public short[] getShortHeap() {
    return shortHeapArray;
  }

  public long[] getLongHeap() {
    return longHeapArray;
  }

  public int destroy() {
    int result = 0;
    try {
      if (ae != null) {
        ae.destroy();
        ae = null;
      }
    } catch (Exception exc) {
      result = 1;
      logException(exc);
    }
    return result;
  }

  public int batchProcessComplete() {
    int result = 0;

    try {
      cc.batchProcessComplete(null);
    } catch (ResourceProcessException e) {
      logException(e);
      return 100;
    } catch (IOException e) {
      logException(e);
      return 100;
    }
    return result;
  }

  public int collectionProcessComplete() {
    int result = 0;

    try {
      cc.batchProcessComplete(null);
    } catch (ResourceProcessException e) {
      logException(e);
      return 100;
    } catch (IOException e) {
      logException(e);
      return 100;
    }
    return result;

  }

  public String resolveImports(String inDesc, String dataPath) {

    try {
      byte[] bar;
      bar = inDesc.getBytes("UTF-16");
      ByteArrayInputStream bais = new ByteArrayInputStream(bar);
      XMLInputSource in = new XMLInputSource(bais, null);
      ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(in);
      bais.close();
      ResourceManager resMgr = UIMAFramework.newDefaultResourceManager();
      resMgr.setDataPath(dataPath);
      if (specifier instanceof AnalysisEngineDescription) {

        AnalysisEngineDescription aeSpecifier = (AnalysisEngineDescription) specifier;

        aeSpecifier.getDelegateAnalysisEngineSpecifiers();
        aeSpecifier.getAnalysisEngineMetaData().resolveImports(resMgr);

        StringWriter writer = new StringWriter();
        aeSpecifier.toXML(writer);

        return writer.toString();
      } else if (specifier instanceof CasConsumerDescription) {
        CasConsumerDescription ccSpecifier = (CasConsumerDescription) specifier;
        ccSpecifier.getCasConsumerMetaData().resolveImports(resMgr);
        StringWriter writer = new StringWriter();
        ccSpecifier.toXML(writer);
        return writer.toString();
      }
    } catch (UnsupportedEncodingException e) {
      logException(e);
    } catch (InvalidXMLException e) {
      logException(e);
    } catch (IOException e) {
      logException(e);
    } catch (SAXException e) {
      logException(e);
    }

    return null;
  }

  protected void finalize() throws Throwable {
    if (ae != null) {
      destroy();
    }
  }

  public String getLastExceptionString() {
    return exceptionString;
  }

  // get UIMA Framework version
  public static String getVersion() {
    return UIMAFramework.getVersionString();
  }

}
