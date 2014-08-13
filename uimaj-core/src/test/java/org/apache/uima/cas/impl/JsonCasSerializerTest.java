package org.apache.uima.cas.impl;

import junit.framework.TestCase;

public class JsonCasSerializerTest extends TestCase {

  protected void setUp() throws Exception {
    super.setUp();
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
   *     @context: expanded-names, supertypes, fsrefs
   *   configuring using Jackson: JsonFactory, JsonGenerator
   *   views: 0, 1 or 2 views
   *   for delta: 0 / non-0 modifications
   *                          above the line
   *                          below the line
   *   namespaces: with/without collision
   */

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
