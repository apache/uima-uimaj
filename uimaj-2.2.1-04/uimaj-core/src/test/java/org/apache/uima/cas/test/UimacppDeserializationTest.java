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

package org.apache.uima.cas.test;

import junit.framework.TestCase;

/**
 * Class comment for UimacppDeserializationTest.java goes here.
 * 
 */
public class UimacppDeserializationTest extends TestCase {

  /**
   * Constructor for UimacppDeserializationTest.
   * 
   * @param arg0
   */
  public UimacppDeserializationTest(String arg0) {
    super(arg0);
  }

  // Test case does not work: need serialized TAF form
  // keep a null test here to avoid having suite complain there are no tests here
  public void testNothing() {
    assertTrue(true);
  }

  /*
   * public void testDeserialization() { // Get file handle to serialized CAS. File dataDir = new
   * File(TestPropertyReader.getJUnitTestBasePath()); assertTrue(dataDir.exists());
   * assertTrue(dataDir.isDirectory()); File serializedForm = new File(dataDir, "cascomplete.ser"); //
   * Deserialize ObjectInputStream ois = null; CASCompleteSerializer ser = null; try { ois = new
   * ObjectInputStream(new FileInputStream(serializedForm)); ser = (CASCompleteSerializer)
   * ois.readObject(); ois.close(); } catch (IOException e) { e.printStackTrace();
   * assertTrue(false); } catch (ClassNotFoundException e) { assertTrue(false); } CAS cas = null;
   * try { CASMgr casMgr = CASFactory.createCAS(); Serialization.deserializeCASComplete(ser,
   * casMgr); cas = casMgr.getCurrentView(); } catch (CASException e) { assertTrue(false); }
   * assertTrue(cas != null); System.out.println("Document text:");
   * System.out.println(cas.getDocumentText());
   * 
   * System.out.println("Type system:\n" + cas.getTypeSystem().toString());
   * 
   * TypeSystem ts = cas.getTypeSystem(); Type ttDocType = ts.getType("uima.tt.DocumentAnnotation");
   * assertTrue(ttDocType != null); Type annotType = ts.getType(CAS.TYPE_NAME_ANNOTATION);
   * assertTrue(annotType != null); assertTrue(ts.subsumes(annotType, ttDocType)); Feature beginFeat =
   * ts.getFeatureByFullName(CAS.FEATURE_FULL_NAME_BEGIN);
   * assertTrue(ttDocType.getAppropriateFeatures().contains(beginFeat)); Vector feats =
   * ttDocType.getAppropriateFeatures(); System.out.println("Features defined for " +
   * ttDocType.getName()); for (int i = 0; i < feats.size(); i++) { System.out.println(" " +
   * ((Feature)feats.get(i)).getName()); }
   * 
   * FSIndex annotationIndex = cas.getAnnotationIndex(); assertTrue(annotationIndex != null);
   * System.out.println( "Number of annotations in index: " + annotationIndex.size()); Feature
   * markupFeat = cas.getTypeSystem().getFeatureByFullName(
   * "uima.tt.DocStructureAnnotation:markupTag"); System.out.println("Annotations: "); FSIterator it =
   * annotationIndex.iterator(); AnnotationFS annot; for (it.moveToFirst(); it.isValid();
   * it.moveToNext()) { annot = (AnnotationFS) it.get(); System.out.println( annot.getType() + ": " +
   * annot.getBegin() + " - " + annot.getEnd() + ": " + annot.getCoveredText()); }
   * 
   * FSIndexRepository ir = cas.getIndexRepository(); Iterator labelIt = ir.getLabels();
   * System.out.println("Index labels: "); while (labelIt.hasNext()) { System.out.println(" " +
   * (String) labelIt.next()); }
   * 
   * Type docType = cas.getTypeSystem().getType(CASMgr.DOCUMENT_TYPE); FSIndex docIndex =
   * cas.getAnnotationIndex(docType); Vector featVector = docType.getAppropriateFeatures();
   * System.out.println("Features defined for docType: "); for (int i = 0; i < featVector.size();
   * i++) { System.out.println(" " + ((Feature)featVector.get(i)).getShortName()); }
   * assertTrue(docIndex != null); it = docIndex.iterator(); for (it.moveToFirst(); it.isValid();
   * it.moveToNext()) { annot = (AnnotationFS) it.get(); System.out.println( annot.getType() + ": " +
   * annot.getBegin() + " - " + annot.getEnd()); System.out.println(" " + annot.getCoveredText()); } //
   * String text = null; // try { // text = cas.getDocumentText(); // } catch (CASException e) { //
   * assertTrue(false); // } // assertTrue(text != null); }
   */
  public static void main(String[] args) {
    junit.textui.TestRunner.run(UimacppDeserializationTest.class);
  }

}
