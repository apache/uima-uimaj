package org.apache.uima.cas.impl;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.XmiCasSerializer.JsonContextFormat;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.FileUtils;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLParser;
import org.xml.sax.SAXException;

public class JsonCasSerializerTest extends TestCase {

  private XMLParser parser = UIMAFramework.getXMLParser();
  private TypeSystemDescription tsd;
  private CASImpl cas;
  private TypeSystemImpl tsi;
  private TypeImpl topType;
  private XmiCasSerializer xcs;
  private String expectedResults;
  private TypeImpl annotationType;
  
  protected void setUp() throws Exception {
    super.setUp();
    xcs = new XmiCasSerializer(null);
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
   * @throws SAXException 
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
    assertEquals(getExpected("top.txt"), r);
    cas.reset();
    
    cas = (CASImpl) cas.createView("basicView");
    cas.addFsToIndexes(cas.createFS(annotationType));
    r = serialize();
    assertEquals(getExpected("topWithNamedView.txt"), r);
    cas.reset();
    
    cas = (CASImpl) cas.getCurrentView(); // default view
    cas.addFsToIndexes(cas.createFS(annotationType));
    r = serialize();
    assertEquals(getExpected("topWithDefaultView.txt"), r);
    
    cas.reset();
    xcs.setJsonContext(JsonContextFormat.omitContext);
    cas.addFsToIndexes(cas.createFS(topType));
    r = serialize();
    assertEquals(getExpected("topNoContext.txt"), r);
    
    cas.reset();
    xcs.setJsonContext(JsonContextFormat.omitContext);
    xcs.setCasViews(false);
    cas.addFsToIndexes(cas.createFS(topType));
    r = serialize();
    assertEquals(getExpected("topNoContextNoViews.txt"), r);
    
    cas.reset();
    xcs.setJsonContext(JsonContextFormat.includeExpandedTypeNames);
    cas.addFsToIndexes(cas.createFS(topType));
    r = serialize();
    assertEquals(getExpected("topExpandedNamesNoViews.txt"), r);

    cas.reset();
    xcs.setJsonContext(JsonContextFormat.omitExpandedTypeNames);
    cas.addFsToIndexes(cas.createFS(topType));
    r = serialize();
    assertEquals(getExpected("topNoContextNoViews.txt"), r);

    cas.reset();
    xcs.setJsonContext(JsonContextFormat.includeFeatureRefs);
    xcs.setJsonContext(JsonContextFormat.includeSuperTypes);
    cas.addFsToIndexes(cas.createFS(topType));
    r = serialize();
    assertEquals(getExpected("topFeatRefsSupertypesNoViews.txt"), r);
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
    assertEquals(getExpected("nameSpaceCollision.txt"), r);
    
    cas.addFsToIndexes(cas.createFS(t3));
    r = serialize();
    assertEquals(getExpected("nameSpaceCollision2.txt"), r);
  }
  
  
  private void setupTypeSystem(String tsdName) throws InvalidXMLException, IOException, ResourceInitializationException {
    File tsdFile = JUnitExtension.getFile("CasSerialization/desc/" + tsdName);
    tsd = parser.parseTypeSystemDescription(new XMLInputSource(tsdFile));
    cas = (CASImpl) CasCreationUtils.createCas(tsd, null, null);
    tsi = cas.getTypeSystemImpl();
    topType = (TypeImpl) tsi.getTopType();
    annotationType = tsi.annotType; 
  }
  
  private String getExpected(String expectedResultsName) throws IOException {
    File expectedResultsFile = JUnitExtension.getFile("CasSerialization/expected/" + expectedResultsName);
    return expectedResults = FileUtils.file2String(expectedResultsFile, "utf-8");
  }
  
  private String serialize() throws SAXException {    
    StringWriter sw = new StringWriter();
    xcs.serializeJson(cas, sw);
    return sw.toString();
  }
  
  public void testJsonSerializeCASObject() {
  }

  public void testJsonSerializeCASTypeSystemObject() {
  }

  public void testJsonSerializeCASTypeSystemObjectBooleanMarker() {
  }

  public void testJsonSerializeCASContentHandlerErrorHandlerMarker() {
  }

  public void testSerializeJsonCASObject() {
  }

  public void testSerializeJsonCASJsonContentHandlerJacksonWrapper() {
  }

}
