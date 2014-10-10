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

package org.apache.uima.json;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;

import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.admin.CASFactory;
import org.apache.uima.cas.admin.TypeSystemMgr;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.IntegerList;
import org.apache.uima.jcas.cas.NonEmptyIntegerList;
import org.apache.uima.json.JsonCasSerializer.JsonContextFormat;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.FileUtils;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLParser;

public class JsonCasSerializerTest extends TestCase {
  /*********************************************************************
   *    I N S T R U C T I O N S                                        *
   *    for regenerating the expected:                                 *
   *                                                                   *
   *    a) set GENERATE_EXPECTED = true;                               *
   *    b) use the generateDir which generates into the newExpected dir*
   *    c) compare to see if OK                                        *
   *    d) change the destination to the real expected dir and rerun   *
   *    e) change the GENERATE_EXPECTED back to false, and the         *
   *              generateDir back to newExpected                      *
   *                                                                   *
   *    Testing for proper format:                                     *
   *      can use http://www.jsoneditoronline.org/                     *          
   *********************************************************************/
  
  private static final boolean GENERATE_EXPECTED = false;
//  private static final String generateDir = "src/test/resources/CasSerialization/expected/";
  private static final String generateDir = "src/test/resources/CasSerialization/newExpected/";


  private XMLParser parser = UIMAFramework.getXMLParser();
  private TypeSystemDescription tsd;
  private CASImpl cas;
  private JCas jcas;
  private TypeSystemImpl tsi;
  private TypeImpl topType;
  private JsonCasSerializer jcs;
  private TypeImpl annotationType;
  private TypeImpl allTypesType;
  private TypeImpl tokenType;
  private TypeImpl emptyIntListType;
 
  protected void setUp() throws Exception {
    super.setUp();
    jcs = new JsonCasSerializer();
  }

  protected void tearDown() throws Exception {
    super.tearDown();
  }
  
  /**
   * Use "all types" - 
   *   instances of:
   *     TOP
   *     Arrays:  Boolean, Byte, Short, Integer, Long, Float, Double, String, FS
   *     AnnotationBase, Annotation, DocumentAnnotation
   *     Lists: Integer, Float, String,  empty, not-empty
   *     
   *   features having values from all primitive value types:
   *     Boolean, Byte, Short, Integer, Long, Float, Double, String
   *     
   *   lists and arrays with / without the multiple-references-allowed flag
   *   
   *   lists with/without shared internal structure
   *   
   * Try all variants 
   *   boolean:  prettyprint, @context,  
   *             (without both context and views), errorHandler/noErrorHandler,
   *             JsonFactory/noJsonFactory, 
   *             typeFiltering,
   *             dynamic / static embedding
   *     @context: expanded-names, subtypes
   *   configuring using Jackson: JsonFactory, JsonGenerator
   *   views: 0, 1 or 2 views
   *   namespaces: with/without collision
   *   
   *   omission of unused types
   *   
   *   omission of not-reachable data
   *   
   *   lists that loop
   *   lists that start as different ones, then merge
   *      both with dynamic and static embedding
   *   
   *   File, Writer, OutputStream
   */

  public void testBasic() throws Exception {
    
    // test omits: context, subtypes, and expanded names
    // also test including / excluding type-name-reference  
    
    setupTypeSystem("nameSpaceNeeded.xml");
    String r = serialize();  // empty cas
    compareWithExpected("emptyCAS.txt", r);
    
    cas.addFsToIndexes(cas.createFS(topType));
    cas.addFsToIndexes(cas.createFS(tokenType));
    r = serialize();
    compareWithExpected("topAndTokenOnly.txt", r);

    
    // same thing, omitting context
    jcs.setJsonContext(JsonContextFormat.omitContext);
    r = serialize();
    compareWithExpected("topAndTokenOnlyNoContext.txt", r);
    
    jcs = new JsonCasSerializer();
    jcs.setJsonContext(JsonContextFormat.omitExpandedTypeNames);
    r = serialize();
    compareWithExpected("topAndTokenOnlyNoExpandedTypeNames.txt", r);
    
    jcs = new JsonCasSerializer();
    jcs.setJsonContext(JsonContextFormat.omitSubtypes);
    r = serialize();
    compareWithExpected("topAndTokenOnlyNoSubtypes.txt", r);
    
    
    
    
    cas = (CASImpl) cas.createView("basicView");
    cas.addFsToIndexes(cas.createFS(annotationType));
    r = serialize();
    compareWithExpected("topWithNamedViewOmits.txt", r);
    cas.reset();
    
    cas = (CASImpl) cas.getCurrentView(); // default view
    cas.addFsToIndexes(cas.createFS(annotationType));
    r = serialize();
    compareWithExpected("topWithDefaultViewOmits.txt", r);
    
    cas.reset();
    jcs.setJsonContext(JsonContextFormat.omitContext);
    cas.addFsToIndexes(cas.createFS(topType));
    r = serialize();
    compareWithExpected("topNoContext.txt", r);
        
    cas.reset();
    jcs = new JsonCasSerializer();
    jcs.setTypeSystemReference("A URI to TypeSystem");
    cas.addFsToIndexes(cas.createFS(topType));
    r = serialize();
    compareWithExpected("topExpandedNamesNoViews.txt", r);

    cas.reset();
    jcs.setJsonContext(JsonContextFormat.omitExpandedTypeNames);
    cas.addFsToIndexes(cas.createFS(topType));
    r = serialize();
    compareWithExpected("topNoExpandedTypeNames.txt", r);

  }
  
  public void testNameSpaceCollision() throws Exception {
    setupTypeSystem("nameSpaceNeeded.xml");
    Type t1 = tsi.getType("org.apache.uima.test.Token");
    Type t2 = tsi.getType("org.apache.uimax.test.Token");
    Type t3 = tsi.getType("org.apache.uima.test2.Token");
    cas = (CASImpl)cas.getCurrentView();
    
    cas.addFsToIndexes(cas.createFS(t1));
    cas.addFsToIndexes(cas.createFS(t2));
    cas.addFsToIndexes(cas.createFS(t3));
    
    String r = serialize();
    compareWithExpected("nameSpaceCollisionOmits.txt", r);
    
    jcs.setOmitDefaultValues(false);
    r = serialize();
    compareWithExpected("nameSpaceCollision.txt", r);
    
    cas.addFsToIndexes(cas.createFS(t3));
    r = serialize();
    compareWithExpected("nameSpaceCollision2.txt", r);

    jcs.setOmitDefaultValues(true);
    r = serialize();
    compareWithExpected("nameSpaceCollision2Omits.txt", r);

    jcs.setPrettyPrint(true);
    r = serialize();
    compareWithExpected("nameSpaceCollision2ppOmits.txt", r);

    jcs.setOmitDefaultValues(false);
    r = serialize();
    compareWithExpected("nameSpaceCollision2pp.txt", r);
    
    // filtering 
    TypeSystemMgr tsMgr = CASFactory.createTypeSystem();
    Type a2t = tsMgr.getType(CAS.TYPE_NAME_ANNOTATION);
    // filter out the 2 types causing namespaces to be needed.
    tsMgr.addType("org.apache.uima.test.Token", a2t);
    tsMgr.commit();
    jcs = new JsonCasSerializer();
    jcs.setFilterTypes((TypeSystemImpl) tsMgr);
    r = serialize();
    compareWithExpected("nameSpaceNoCollsionFiltered.txt", r);
    
    // filter, but not enough - should have 1 collison
    tsMgr = CASFactory.createTypeSystem();
    a2t = tsMgr.getType(CAS.TYPE_NAME_ANNOTATION);
    // filter out the 2 types causing namespaces to be needed.
    tsMgr.addType("org.apache.uima.test.Token", a2t);
    tsMgr.addType("org.apache.uimax.test.Token", a2t);
    tsMgr.commit();
    jcs.setFilterTypes((TypeSystemImpl) tsMgr);
    r = serialize();
    compareWithExpected("nameSpaceCollsionFiltered.txt", r);    
    
  }
  
  public void testAllValues() throws Exception {
    setupTypeSystem("allTypes.xml");
    setAllValues(0);

    jcs.setPrettyPrint(true);
    String r = serialize();
    compareWithExpected("allValuesOmits.txt", r);
    
    jcs.setOmitDefaultValues(false);
    r = serialize();
    compareWithExpected("allValuesNoOmits.txt", r);
    
    jcs.setStaticEmbedding();
    r = serialize();
    compareWithExpected("allValuesStaticNoOmits.txt", r);
    
  }
  
  public void testMultipleViews() throws Exception {
    setupTypeSystem("allTypes.xml");
    setAllValues(1);
    cas = (CASImpl) cas.createView("View2");
    setAllValues(0);

    jcs.setPrettyPrint(true);
    String r = serialize();
    compareWithExpected("multipleViews.txt", r);
        
  }
  public void testDynamicLists() throws Exception {
    setupTypeSystem("allTypes.xml");
    
    FeatureStructure[] fss = new FeatureStructure[20];
    fss[0] = emptyIntList();
    fss[1] = intList(33, fss[0]);   // value 33, linked to 0
    fss[2] = intList(22, fss[1]);   
    fss[3] = intList(11, fss[2]);
    
    fss[4] = intList(110, fss[2]);  // joins at 2
    
    jcas.addFsToIndexes(fss[3]);;
    jcas.addFsToIndexes(fss[4]);
   
    jcs.setPrettyPrint(true);
    String r = serialize();
    compareWithExpected("twoListMerge.txt", r);
    
    jcs.setStaticEmbedding();
    r = serialize();
    compareWithExpected("twoListMergeStatic.txt", r);
    
  }

  private FeatureStructure emptyIntList() {
    return cas.createFS(emptyIntListType);
  }
  
  private FeatureStructure intList(int v, FeatureStructure next) {
    NonEmptyIntegerList fs = new NonEmptyIntegerList(jcas);
    fs.setHead(v);
    fs.setTail((IntegerList) next);
    return fs;
  }
  
//  public void testDelta() throws Exception {
//    setupTypeSystem("allTypes.xml");
//    
//    setAllValues(0);
//    Marker marker = cas.createMarker();
//    setAllValues(1);
//    
//    jcs.setPrettyPrint(true);
//    String r = serialize();
//    compareWithExpected("delta.txt", r);
//    
//    jcs.setDeltaCas(marker);
//    r = serialize();
//    compareWithExpected("delta2.txt", r);
//    
//  }
  
  private FeatureStructure setAllValues(int v) {
    boolean s1 = v == 0;
    boolean s2 = v == 1;
    FeatureStructure fs = cas.createFS(allTypesType);
    cas.addFsToIndexes(fs);

    FeatureStructure fs2 = cas.createFS(allTypesType);
    
    fs.setBooleanValue(allTypesType.getFeatureByBaseName("aBoolean"), s1 ? true : false);
    fs.setByteValue   (allTypesType.getFeatureByBaseName("aByte"), s1 ? (byte) -117 : (byte) 0);
    fs.setShortValue  (allTypesType.getFeatureByBaseName("aShort"), s1 ? (short) -112 : (short) 0);
    fs.setIntValue    (allTypesType.getFeatureByBaseName("aInteger"), s1 ? 0 : 1);
    fs.setLongValue   (allTypesType.getFeatureByBaseName("aLong"), s2 ? 4321 : 1234);
    fs.setFloatValue  (allTypesType.getFeatureByBaseName("aFloat"), s1 ?  1.3F : Float.NaN);
    fs.setDoubleValue (allTypesType.getFeatureByBaseName("aDouble"), s2 ? Float.NEGATIVE_INFINITY : 2.6);
    fs.setStringValue (allTypesType.getFeatureByBaseName("aString"),  "some \"String\"");
    fs.setFeatureValue(allTypesType.getFeatureByBaseName("aFS"),  fs2);
    
    FeatureStructure fsAboolean = cas.createBooleanArrayFS(s1 ? 1 : 0);
    FeatureStructure fsAbyte    = cas.createByteArrayFS(s1 ? 2 : 0);
    FeatureStructure fsAshort   = cas.createShortArrayFS(s2 ? 2 : 0);
    FeatureStructure fsAstring  = cas.createStringArrayFS(s1 ? 1 : 0);
    
    FeatureStructure fsMrAboolean = cas.createBooleanArrayFS(1);
    FeatureStructure fsMrAbyte    = cas.createByteArrayFS(2);
    FeatureStructure fsMrAshort   = cas.createShortArrayFS(0);
    FeatureStructure fsMrAstring  = cas.createStringArrayFS(1);
    
    fs.setFeatureValue (allTypesType.getFeatureByBaseName("aArrayBoolean"), fsAboolean);
    fs.setFeatureValue (allTypesType.getFeatureByBaseName("aArrayByte"),     fsAbyte);
    fs.setFeatureValue (allTypesType.getFeatureByBaseName("aArrayShort"),    fsAshort);
    fs.setFeatureValue (allTypesType.getFeatureByBaseName("aArrayString"),   fsAstring);
    
    fs.setFeatureValue (allTypesType.getFeatureByBaseName("aArrayMrBoolean"),  fsMrAboolean);
    fs.setFeatureValue (allTypesType.getFeatureByBaseName("aArrayMrByte"),     fsMrAbyte);
    fs.setFeatureValue (allTypesType.getFeatureByBaseName("aArrayMrShort"),    fsMrAshort);
    fs.setFeatureValue (allTypesType.getFeatureByBaseName("aArrayMrString"),   fsMrAstring);

    
    FeatureStructure fsLinteger0 = cas.createFS(tsi.getType(CAS.TYPE_NAME_EMPTY_INTEGER_LIST));
    
    FeatureStructure fsLstring0  = cas.createFS(tsi.getType(CAS.TYPE_NAME_NON_EMPTY_STRING_LIST));
    FeatureStructure fsLstring1  = cas.createFS(tsi.getType(CAS.TYPE_NAME_EMPTY_STRING_LIST));
    fsLstring0.setStringValue (tsi.getFeatureByFullName(CAS.TYPE_NAME_NON_EMPTY_STRING_LIST + ":head"), "testStr");
    fsLstring0.setFeatureValue (tsi.getFeatureByFullName(CAS.TYPE_NAME_NON_EMPTY_STRING_LIST + ":tail"), fsLstring1);
    
    
    FeatureStructure fsLfs0  = cas.createFS(tsi.getType(CAS.TYPE_NAME_NON_EMPTY_FS_LIST));
    FeatureStructure fsLfs1  = cas.createFS(tsi.getType(CAS.TYPE_NAME_EMPTY_FS_LIST));
    fsLfs0.setFeatureValue (tsi.getFeatureByFullName(CAS.TYPE_NAME_NON_EMPTY_FS_LIST + ":tail"), fsLfs1);
    
    fs.setFeatureValue (allTypesType.getFeatureByBaseName("aListInteger"), fsLinteger0);
    fs.setFeatureValue (allTypesType.getFeatureByBaseName("aListString"), fsLstring0);
    fs.setFeatureValue (allTypesType.getFeatureByBaseName("aListFs"), fsLfs0);
    
    cas.addFsToIndexes(fs);
    return fs;
  }
   
  
  private void setupTypeSystem(String tsdName) throws InvalidXMLException, IOException, ResourceInitializationException, CASException {
    File tsdFile = JUnitExtension.getFile("CasSerialization/desc/" + tsdName);
    tsd = parser.parseTypeSystemDescription(new XMLInputSource(tsdFile));
    cas = (CASImpl) CasCreationUtils.createCas(tsd, null, null);
    jcas = cas.getJCas();
    tsi = cas.getTypeSystemImpl();
    topType = (TypeImpl) tsi.getTopType();
    annotationType = (TypeImpl) tsi.getType("uima.tcas.Annotation"); 
    allTypesType = (TypeImpl) tsi.getType("org.apache.uima.test.AllTypes");
    tokenType = (TypeImpl) tsi.getType("org.apache.uima.test.Token");
    emptyIntListType = (TypeImpl) tsi.getType("uima.cas.EmptyIntegerList");
//    nonEmptyIntListType = (TypeImpl) tsi.getType("uima.cas.NonEmptyIntegerList");
//    emptyFSListType = (TypeImpl) tsi.getType("uima.cas.EmptyFSList");
//    nonEmptyFSListType = (TypeImpl) tsi.getType("uima.cas.NonEmptyFSList");
    
  }
  
  private String getExpected(String expectedResultsName, String r) throws IOException {
    if (GENERATE_EXPECTED) {
      File d = new File (generateDir);
      d.mkdirs();
      File file = new File (generateDir + expectedResultsName);
      FileWriter writer = new FileWriter(file);
      writer.write(r);
      writer.close();
      return r;
    } else {
    File expectedResultsFile = JUnitExtension.getFile("CasSerialization/expected/" + expectedResultsName);
    return FileUtils.file2String(expectedResultsFile, "utf-8");
    }
  }
  
  private void compareWithExpected (String expectedResultsName, String r) throws IOException {
    r = canonicalizeNewLines(r);
    String expected = getExpected(expectedResultsName, r);
    assertEquals(canonicalizeNewLines(expected), r);
  }
  
  private String canonicalizeNewLines(String r) {
    return  r.replace("\n\r", "\n").replace("\r\n", "\n").replace('\r',  '\n');
  }
  
  private String serialize() throws Exception {    
    StringWriter sw = new StringWriter();
    try {
    jcs.serialize(cas, sw);
    } catch (Exception e) {
      System.err.format("Exception occurred. The string produced so far was: %n%s%n", sw.toString());
      throw e;
    }
    return sw.toString();
  }
    
}
