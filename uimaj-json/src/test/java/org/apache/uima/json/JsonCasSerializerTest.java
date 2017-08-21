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

import java.io.ByteArrayOutputStream;
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
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.ByteArray;
import org.apache.uima.jcas.cas.EmptyFSList;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.IntegerList;
import org.apache.uima.jcas.cas.NonEmptyFSList;
import org.apache.uima.jcas.cas.NonEmptyIntegerList;
import org.apache.uima.json.JsonCasSerializer.JsonContextFormat;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.test.AllTypes;
import org.apache.uima.test.RefTypes;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.FileUtils;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLParser;
import org.apache.uima.util.XMLSerializer;
import org.custommonkey.xmlunit.XMLAssert;
import org.xml.sax.SAXException;

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
   *    f) repeat for JsonXmiCasSerializerTest                         *                                          
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
  protected JsonCasSerializer jcs;
  private TypeImpl annotationType;
  private TypeImpl allTypesType;
  private TypeImpl tokenType;
  private TypeImpl emptyIntListType;
  protected boolean doJson;
  private FeatureStructure fsa1;    // elements of FS Array
  private FeatureStructure fsa2;    // or just extra FS; these are not initially indexed
  private FeatureStructure fsa3;
  private FSArray fsaa;    // a feature structure array
 
  protected void setUp() throws Exception {
    jcs = new JsonCasSerializer();
    jcs.setOmit0Values(true);
    doJson = true;
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
    serializeAndCompare("emptyCAS.txt");  // empty cas
    
    cas.addFsToIndexes(cas.createFS(topType));
    cas.addFsToIndexes(cas.createFS(tokenType));
    serializeAndCompare("topAndTokenOnly.txt");

    
    // same thing, omitting context
    jcs.setJsonContext(JsonContextFormat.omitContext);
    serializeAndCompare("topAndTokenOnlyNoContext.txt");
    
    jcs = new JsonCasSerializer();
    jcs.setJsonContext(JsonContextFormat.omitExpandedTypeNames).setOmit0Values(true);
    serializeAndCompare("topAndTokenOnlyNoExpandedTypeNames.txt");
    
    jcs = new JsonCasSerializer();
    jcs.setJsonContext(JsonContextFormat.omitSubtypes).setOmit0Values(true);
    serializeAndCompare("topAndTokenOnlyNoSubtypes.txt");
    
    
    
    
    cas = (CASImpl) cas.createView("basicView");
    cas.addFsToIndexes(cas.createFS(annotationType));
    serializeAndCompare("topWithNamedViewOmits.txt");
    cas.reset();
    
    cas = (CASImpl) cas.getCurrentView(); // default view
    cas.addFsToIndexes(cas.createFS(annotationType));
    serializeAndCompare("topWithDefaultViewOmits.txt");
    
    cas.reset();
    jcs.setJsonContext(JsonContextFormat.omitContext);
    cas.addFsToIndexes(cas.createFS(topType));
    serializeAndCompare("topNoContext.txt");
        
    cas.reset();
    jcs = new JsonCasSerializer().setOmit0Values(true);
    jcs.setTypeSystemReference("A URI to TypeSystem");
    cas.addFsToIndexes(cas.createFS(topType));
    serializeAndCompare("topExpandedNamesNoViews.txt");

    cas.reset();
    jcs.setJsonContext(JsonContextFormat.omitExpandedTypeNames);
    cas.addFsToIndexes(cas.createFS(topType));
    serializeAndCompare("topNoExpandedTypeNames.txt");

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
    
    serializeAndCompare("nameSpaceCollisionOmits.txt");
    
    jcs.setOmit0Values(false);
    serializeAndCompare("nameSpaceCollision.txt");
    
    cas.addFsToIndexes(cas.createFS(t3));
    serializeAndCompare("nameSpaceCollision2.txt");

    jcs.setOmit0Values(true);
    serializeAndCompare("nameSpaceCollision2Omits.txt");

    jcs.setPrettyPrint(true);
    serializeAndCompare("nameSpaceCollision2ppOmits.txt");

    jcs.setOmit0Values(false);
    serializeAndCompare("nameSpaceCollision2pp.txt");
    
    // filtering 
    TypeSystemMgr tsMgr = CASFactory.createTypeSystem();
    Type a2t = tsMgr.getType(CAS.TYPE_NAME_ANNOTATION);
    // filter out the 2 types causing namespaces to be needed.
    tsMgr.addType("org.apache.uima.test.Token", a2t);
    tsMgr.commit();
    jcs = new JsonCasSerializer().setOmit0Values(true);
    jcs.setFilterTypes((TypeSystemImpl) tsMgr);
    serializeAndCompare("nameSpaceNoCollsionFiltered.txt");
    
    // filter, but not enough - should have 1 collison
    tsMgr = CASFactory.createTypeSystem();
    a2t = tsMgr.getType(CAS.TYPE_NAME_ANNOTATION);
    // filter out the 2 types causing namespaces to be needed.
    tsMgr.addType("org.apache.uima.test.Token", a2t);
    tsMgr.addType("org.apache.uimax.test.Token", a2t);
    tsMgr.commit();
    jcs.setFilterTypes((TypeSystemImpl) tsMgr);
    serializeAndCompare("nameSpaceCollsionFiltered.txt");    
    
  }
  
  public void testAllValues() throws Exception {
    setupTypeSystem("allTypes.xml");
    setAllValues(0);
    jcs.setPrettyPrint(true).setOmit0Values(true);
    serializeAndCompare("allValuesOmits.txt");
    
    jcs.setOmit0Values(false);
    serializeAndCompare("allValuesNoOmits.txt");
    
    jcs.setStaticEmbedding();
    serializeAndCompare("allValuesStaticNoOmits.txt");
    
  }
  
  public void testMultipleViews() throws Exception {
    setupTypeSystem("allTypes.xml");
    setAllValues(1);
    cas = (CASImpl) cas.createView("View2");
    setAllValues(0);

    jcs.setPrettyPrint(true);
    serializeAndCompare("multipleViews.txt");
        
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
    serializeAndCompare("twoListMerge.txt");
    
    jcs.setStaticEmbedding();
    serializeAndCompare("twoListMergeStatic.txt");
   
    cas.reset();
    jcs = new JsonCasSerializer().setOmit0Values(true);
    jcs.setPrettyPrint(true);
    fss[0] = emptyIntList();
    fss[1] = intList(33, fss[0]);   // value 33, linked to 0
    fss[2] = intList(22, fss[1]);   
    fss[3] = intList(11, fss[2]);

    cas.addFsToIndexes(fss[3]);
    serializeAndCompare("indexedSingleList.txt");
    
    jcs.setStaticEmbedding();
    serializeAndCompare("indexedSingleListStatic.txt");
    
  }
  
  /**
   * Testing various cases
   * 
   * FS is both indexed, and is referenced
   * 
   * FS is referenced, and in turn references an embeddable item
   * FS is referenced, and in turn references a shared item
   * 
   */
  
  public void testRefs() throws Exception {
    setupTypeSystem("refTypes.xml");
    jcs.setPrettyPrint(true);
    jcs.setJsonContext(JsonContextFormat.omitContext);

      
    //  make root FS that is indexed and has a ref 
    RefTypes root = new RefTypes(jcas);
    root.addToIndexes();
    
    RefTypes ref1 = new RefTypes(jcas);
    ref1.addToIndexes();  // is both referenced and indexed

    root.setAFS(ref1);
    
    serializeAndCompare("indexedAndRef.txt");

    arrayOrListRefstst(true);
    arrayOrListRefstst(false);
  }

  public void arrayOrListRefstst(boolean tstArray) throws Exception {
    
    // using dynamic embedding
    // an element is multiply-referenced if it is both in the index (referenced by the "view") and is referenced 
    //   by an FSRef in a feature or a slot in an FSArray
    
    jcas.reset();
    
    //  make root FS that is indexed and has a ref 
    RefTypes root = new RefTypes(jcas);
    root.addToIndexes();
       
    // Test list or array with 1 non-embeddable
    RefTypes refa1 = new RefTypes(jcas);
    RefTypes refa2 = new RefTypes(jcas);
    RefTypes refa3 = new RefTypes(jcas);
    
    
    FSArray a = new FSArray(jcas,  3);
    a.set(0, refa1);
    a.set(1, refa2);
    a.set(2, refa3);

    NonEmptyFSList l0 = new NonEmptyFSList(jcas);
    NonEmptyFSList l1 = new NonEmptyFSList(jcas);
    NonEmptyFSList l2 = new NonEmptyFSList(jcas);
    EmptyFSList tailEnd = new EmptyFSList(jcas);    
    l0.setTail(l1);
    l1.setTail(l2);;
    l2.setTail(tailEnd);
    l0.setHead(refa1);
    l1.setHead(refa2);
    l2.setHead(refa3);;
         
    if (tstArray) {
      root.setAArrayFS(a);  // is not (yet) multiply referenced
    } else {
      root.setAListFs(l0);
    }
    
    String sfx = (tstArray) ? "a" : "l";
    // all embeddable:
    //   because ref1,2,3 are not index, and FSArray isn't either
    serializeAndCompare("array-all-embeddable-" + sfx + ".txt");
    // 1 not embeddable, at all 3 positions
    refa1.addToIndexes();
    //   ref1 is multiply indexed
    serializeAndCompare("array-a1-not-" + sfx + ".txt");
    refa1.removeFromIndexes();
    refa2.addToIndexes();
    serializeAndCompare("array-a2-not-" + sfx + ".txt");
    refa2.removeFromIndexes();
    refa3.addToIndexes();
    serializeAndCompare("array-a3-not-" + sfx + ".txt");
    
    // 3 not embeddable:
    refa1.addToIndexes();
    refa2.addToIndexes();
    serializeAndCompare("array-non-embeddable-" + sfx + ".txt");
    
    // FSArray not embeddable
    if (tstArray) {
      a.addToIndexes();  
    } else {
      l0.addToIndexes();
    }
    
    serializeAndCompare("array-self-non-embeddable-" + sfx + ".txt");
    
    
    // all embeddable, FSArray not
    refa1.removeFromIndexes();
    refa2.removeFromIndexes();
    refa3.removeFromIndexes();
    serializeAndCompare("array-self-items-all-embeddable-" + sfx + ".txt");        
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
  
  private FeatureStructure setAllValues(int v) throws CASException {
    JCas jcas = cas.getJCas();
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
    ByteArray fsAbyte    = new ByteArray(jcas, s1 ? 2 : 0);
    if (s1) {
      fsAbyte.set(0, (byte) 15);
      fsAbyte.set(1,  (byte) 0xee);
    }
    FeatureStructure fsAshort   = cas.createShortArrayFS(s2 ? 2 : 0);
    FeatureStructure fsAstring  = cas.createStringArrayFS(s1 ? 1 : 0);
    
    fsa1 = cas.createFS(allTypesType);
    fsa2 = cas.createFS(allTypesType);
    fsa3 = cas.createFS(allTypesType);
    
    fsaa = new FSArray(jcas, 3);
    fsaa.set(0, fsa1);
    fsaa.set(1, fsa2);
    fsaa.set(2, fsa3);;
    
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
    if (!doJson) {
      expectedResultsName = expectedResultsName.replace(".txt", ".xml");
    }

    if (GENERATE_EXPECTED) {
      String generateDirPlus = generateDir + ((doJson) ? "json/" : "xmi/"); 
      File d = new File (generateDirPlus);
      d.mkdirs();
      File file = new File (generateDirPlus  + expectedResultsName);
      FileWriter writer = new FileWriter(file);
      writer.write(r);
      writer.close();
      return r;
    } else {
    File expectedResultsFile = JUnitExtension.getFile("CasSerialization/expected/" + ((doJson) ? "json/" : "xmi/") + expectedResultsName);
    return FileUtils.file2String(expectedResultsFile, "utf-8");
    }
  }
  
  private void serializeAndCompare(String expectedResultsName) throws Exception {
    String r = serialize();
    compareWithExpected(expectedResultsName, r);
  }
  
  private void compareWithExpected(String expectedResultsName, String r) throws IOException, SAXException {
    r = canonicalizeNewLines(r);
    String expected = getExpected(expectedResultsName, r);
    String ce = canonicalizeNewLines(expected);
    if (doJson) {
      assertEquals(ce, r);
    } else {
      XMLAssert.assertXMLEqual(ce, r);
    }
  }
  
  private String canonicalizeNewLines(String r) {
    return  r.replace("\n\r", "\n").replace("\r\n", "\n").replace('\r',  '\n');
  }
  
  private String serialize() throws Exception {    
    StringWriter sw = null;
    ByteArrayOutputStream baos = null;
    try {
      if (doJson) {
        sw = new StringWriter();
        jcs.serialize(cas, sw);
        return sw.toString();
      } else {
        XmiCasSerializer xcs = new XmiCasSerializer(jcs.getCss().getFilterTypes());
        baos = new ByteArrayOutputStream();
        
        XMLSerializer sax2xml = new XMLSerializer(baos, jcs.getCss().isFormattedOutput);
        xcs.serialize(cas, sax2xml.getContentHandler(), null);
        return baos.toString("UTF-8");
      }
    } catch (Exception e) {
      System.err.format("Exception occurred. The string produced so far was: %n%s%n",
          (sw == null) ? baos.toString("UTF-8") : sw.toString());
      throw e;
    }
  }
    
}
