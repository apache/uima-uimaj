package org.apache.uima.cas.impl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;

import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Marker;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.JsonCasSerializer.JsonCasFormat;
import org.apache.uima.cas.impl.JsonCasSerializer.JsonContextFormat;
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
  private TypeSystemImpl tsi;
  private TypeImpl topType;
  private JsonCasSerializer jcs;
  private TypeImpl annotationType;
  private TypeImpl allTypesType;
  
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
   *   boolean:  prettyprint, marker/delta, @context, views, 
   *             (without both context and views), byId/byType, errorHandler/noErrorHandler,
   *             JsonFactory/noJsonFactory, 
   *             typeFiltering
   *     @context: expanded-names, supertypes, fsrefs
   *   configuring using Jackson: JsonFactory, JsonGenerator
   *   views: 0, 1 or 2 views
   *   for delta: 0 / non-0 modifications   
   *                          above the line
   *                          below the line
   *                        mods with/without indexing update
   *   namespaces: with/without collision
   *   
   *   File, Writer, OutputStream
   */

  public void testBasic() throws Exception {
    setupTypeSystem("nameSpaceNeeded.xml");
    cas.addFsToIndexes(cas.createFS(topType));
    String r = serialize();
    compareWithExpected("top.txt", r);
    cas.reset();
    
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
    jcs.setJsonContext(JsonContextFormat.omitContext);
    jcs.setCasViews(false);
    cas.addFsToIndexes(cas.createFS(topType));
    r = serialize();
    compareWithExpected("topNoContextNoViews.txt", r);
    
    cas.reset();
    jcs.setJsonContext(JsonContextFormat.includeExpandedTypeNames);
    cas.addFsToIndexes(cas.createFS(topType));
    r = serialize();
    compareWithExpected("topExpandedNamesNoViews.txt", r);

    cas.reset();
    jcs.setJsonContext(JsonContextFormat.omitExpandedTypeNames);
    cas.addFsToIndexes(cas.createFS(topType));
    r = serialize();
    compareWithExpected("topNoContextNoViews.txt", r);

    cas.reset();
    jcs.setJsonContext(JsonContextFormat.includeFeatureRefs);
    jcs.setJsonContext(JsonContextFormat.includeSuperTypes);
    cas.addFsToIndexes(cas.createFS(topType));
    r = serialize();
    compareWithExpected("topFeatRefsSupertypesNoViews.txt", r);
  }
  
  public void testNameSpaceCollision() throws Exception {
    setupTypeSystem("nameSpaceNeeded.xml");
    Type t1 = tsi.getType("org.apache.uima.test.Token");
    Type t2 = tsi.getType("org.apache.uimax.test.Token");
    Type t3 = tsi.getType("org.apache.uima.test2.Token");
    cas = (CASImpl)cas.getCurrentView();
    
    cas.addFsToIndexes(cas.createFS(t1));
    cas.addFsToIndexes(cas.createFS(t2));
    
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
    
  }
  
  public void testAllValues() throws Exception {
    setupTypeSystem("allTypes.xml");
    setAllValues(0);

    jcs.setPrettyPrint(true);
    String r = serialize();
    compareWithExpected("allValuesOmits.txt", r);

    jcs.jsonCasFormatEnable(JsonCasFormat.INDEX_TYPE);
    r = serialize();
    compareWithExpected("allValuesByTypeOmits.txt", r);
    
    jcs.setOmitDefaultValues(false);
    r = serialize();
    compareWithExpected("allValuesByType.txt", r);
    
    jcs.jsonCasFormatEnable(JsonCasFormat.INDEX_ID);
    r = serialize();
    compareWithExpected("allValues.txt", r);
    
    jcs.jsonCasFormatEnable(JsonCasFormat.OMIT_ID);
    r = serialize();
    compareWithExpected("allValuesNoIDs.txt", r);

    jcs.jsonCasFormatDisable(JsonCasFormat.OMIT_ID);
    jcs.jsonCasFormatEnable(JsonCasFormat.OMIT_TYPE);    
    r = serialize();
    compareWithExpected("allValuesNoTypes.txt", r);

    jcs.jsonCasFormatEnable(JsonCasFormat.OMIT_ID);
    r = serialize();
    compareWithExpected("allValuesNoIDNoTypes.txt", r);

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
  
  public void testDelta() throws Exception {
    setupTypeSystem("allTypes.xml");
    
    setAllValues(0);
    Marker marker = cas.createMarker();
    setAllValues(1);
    
    jcs.setPrettyPrint(true);
    String r = serialize();
    compareWithExpected("delta.txt", r);
    
    jcs.setDeltaCas(marker);
    r = serialize();
    compareWithExpected("delta2.txt", r);
    
  }
  
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
   
  
  private void setupTypeSystem(String tsdName) throws InvalidXMLException, IOException, ResourceInitializationException {
    File tsdFile = JUnitExtension.getFile("CasSerialization/desc/" + tsdName);
    tsd = parser.parseTypeSystemDescription(new XMLInputSource(tsdFile));
    cas = (CASImpl) CasCreationUtils.createCas(tsd, null, null);
    tsi = cas.getTypeSystemImpl();
    topType = (TypeImpl) tsi.getTopType();
    annotationType = tsi.annotType; 
    allTypesType = (TypeImpl) tsi.getType("org.apache.uima.test.AllTypes");
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
