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
package org.apache.uima.tools.viewer;

import java.util.Enumeration;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;

import junit.framework.TestCase;

import org.apache.uima.cas.BooleanArrayFS;
import org.apache.uima.cas.ByteArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.DoubleArrayFS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FloatArrayFS;
import org.apache.uima.cas.IntArrayFS;
import org.apache.uima.cas.LongArrayFS;
import org.apache.uima.cas.ShortArrayFS;
import org.apache.uima.cas.StringArrayFS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.admin.CASFactory;
import org.apache.uima.cas.admin.CASMgr;
import org.apache.uima.cas.admin.FSIndexRepositoryMgr;
import org.apache.uima.cas.admin.TypeSystemMgr;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.CasCreationUtils;

public class CasAnnotationViewerTest extends TestCase {
  CasAnnotationViewer viewer;

  private CAS cas;

  private Type annotationType;

  private Type exampleType;

  private Feature floatFeature;

  private Feature stringFeature;

  private Feature byteFeature;

  private Feature booleanFeature;

  private Feature shortFeature;

  private Feature longFeature;

  private Feature doubleFeature;

  private Feature intArrayFeature;

  private Feature floatArrayFeature;

  private Feature stringArrayFeature;

  private Feature byteArrayFeature;

  private Feature booleanArrayFeature;

  private Feature shortArrayFeature;

  private Feature longArrayFeature;

  private Feature doubleArrayFeature;

  protected void setUp() throws Exception {
    viewer = new CasAnnotationViewer();

    CASMgr casMgr = CASFactory.createCAS();
    CasCreationUtils.setupTypeSystem(casMgr, (TypeSystemDescription) null);
    // Create a writable type system.
    TypeSystemMgr tsa = casMgr.getTypeSystemMgr();
    // Add new types and features.
    annotationType = tsa.getType(CAS.TYPE_NAME_ANNOTATION);
    assertTrue(annotationType != null);

    // new primitive types
    exampleType = tsa.addType("test.primitives.Example", annotationType);

    floatFeature = tsa.addFeature("floatFeature", exampleType, tsa.getType(CAS.TYPE_NAME_FLOAT));
    stringFeature = tsa.addFeature("stringFeature", exampleType, tsa.getType(CAS.TYPE_NAME_STRING));
    booleanFeature = tsa.addFeature("boolFeature", exampleType, tsa.getType(CAS.TYPE_NAME_BOOLEAN));
    byteFeature = tsa.addFeature("byteFeature", exampleType, tsa.getType(CAS.TYPE_NAME_BYTE));
    shortFeature = tsa.addFeature("shortFeature", exampleType, tsa.getType(CAS.TYPE_NAME_SHORT));
    longFeature = tsa.addFeature("longFeature", exampleType, tsa.getType(CAS.TYPE_NAME_LONG));
    doubleFeature = tsa.addFeature("doubleFeature", exampleType, tsa.getType(CAS.TYPE_NAME_DOUBLE));

    intArrayFeature = tsa.addFeature("intArrayFeature", exampleType, tsa
            .getType(CAS.TYPE_NAME_INTEGER_ARRAY));
    floatArrayFeature = tsa.addFeature("floatArrayFeature", exampleType, tsa
            .getType(CAS.TYPE_NAME_FLOAT_ARRAY), false);
    stringArrayFeature = tsa.addFeature("stringArrayFeature", exampleType, tsa
            .getType(CAS.TYPE_NAME_STRING_ARRAY), false);
    booleanArrayFeature = tsa.addFeature("boolArrayFeature", exampleType, tsa
            .getType(CAS.TYPE_NAME_BOOLEAN_ARRAY));
    byteArrayFeature = tsa.addFeature("byteArrayFeature", exampleType, tsa
            .getType(CAS.TYPE_NAME_BYTE_ARRAY), false);
    shortArrayFeature = tsa.addFeature("shortArrayFeature", exampleType, tsa
            .getType(CAS.TYPE_NAME_SHORT_ARRAY));
    longArrayFeature = tsa.addFeature("longArrayFeature", exampleType, tsa
            .getType(CAS.TYPE_NAME_LONG_ARRAY));
    doubleArrayFeature = tsa.addFeature("doubleArrayFeature", exampleType, tsa
            .getType(CAS.TYPE_NAME_DOUBLE_ARRAY), false);

    // Commit the type system.
    ((CASImpl) casMgr).commitTypeSystem();

    // Create the Base indexes.
    casMgr.initCASIndexes();
    FSIndexRepositoryMgr irm = casMgr.getIndexRepositoryMgr();
    // init.initIndexes(irm, casMgr.getTypeSystemMgr());
    irm.commit();

    cas = casMgr.getCAS().getView(CAS.NAME_DEFAULT_SOFA);

  }

  public void testAddAnnotationToTree() throws Exception {
    try {
      // create an annotation
      createExampleFS(this.cas);
      FSIterator iter = this.cas.getAnnotationIndex(exampleType).iterator();
      AnnotationFS annot = (AnnotationFS) iter.get();

      // init viewer
      viewer.setCAS(this.cas);

      // add to tree
      viewer.addAnnotationToTree(annot);

      // inspect results
      TreeModel model = viewer.getSelectedAnnotationTree().getModel();
      DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
      assertEquals("Annotations", root.getUserObject().toString());
      DefaultMutableTreeNode typeNode = (DefaultMutableTreeNode) root.getChildAt(0);
      assertEquals("Example", typeNode.getUserObject().toString());
      DefaultMutableTreeNode fsNode = (DefaultMutableTreeNode) typeNode.getChildAt(0);
      Enumeration children = fsNode.children();
      assertEquals("begin = 1", ((DefaultMutableTreeNode) children.nextElement()).getUserObject()
              .toString());
      assertEquals("end = 5", ((DefaultMutableTreeNode) children.nextElement()).getUserObject()
              .toString());
      assertEquals("floatFeature = " + (float) 99.99, ((DefaultMutableTreeNode) children
              .nextElement()).getUserObject().toString());
      assertEquals("stringFeature = aaaaaaa", ((DefaultMutableTreeNode) children.nextElement())
              .getUserObject().toString());
      assertEquals("boolFeature = true", ((DefaultMutableTreeNode) children.nextElement())
              .getUserObject().toString());
      assertEquals("byteFeature = 122", ((DefaultMutableTreeNode) children.nextElement())
              .getUserObject().toString());
      assertEquals("shortFeature = " + Short.MIN_VALUE, ((DefaultMutableTreeNode) children
              .nextElement()).getUserObject().toString());
      assertEquals("longFeature = " + Long.MIN_VALUE, ((DefaultMutableTreeNode) children
              .nextElement()).getUserObject().toString());
      assertEquals("doubleFeature = " + Double.MAX_VALUE, ((DefaultMutableTreeNode) children
              .nextElement()).getUserObject().toString());

      assertEquals("intArrayFeature = [" + Integer.MAX_VALUE + "," + (Integer.MAX_VALUE - 1)
              + ",42," + (Integer.MIN_VALUE + 1) + "," + Integer.MIN_VALUE + "]",
              ((DefaultMutableTreeNode) children.nextElement()).getUserObject().toString());
      assertEquals("floatArrayFeature = [" + Float.MAX_VALUE + ","
              + (float) (Float.MAX_VALUE / 1000.0) + "," + 42.0 + ","
              + (float) (Float.MIN_VALUE * 1000) + "," + Float.MIN_VALUE + "]",
              ((DefaultMutableTreeNode) children.nextElement()).getUserObject().toString());
      assertEquals("stringArrayFeature = [zzzzzz,yyyyyy,xxxxxx,wwwwww,vvvvvv]",
              ((DefaultMutableTreeNode) children.nextElement()).getUserObject().toString());
      assertEquals("boolArrayFeature = [true,false,true,false,true,false,true,false]",
              ((DefaultMutableTreeNode) children.nextElement()).getUserObject().toString());
      assertEquals("byteArrayFeature = [8,16,64,-128,-1]", ((DefaultMutableTreeNode) children
              .nextElement()).getUserObject().toString());
      assertEquals("shortArrayFeature = [" + Short.MAX_VALUE + "," + (Short.MAX_VALUE - 1) + ","
              + (Short.MAX_VALUE - 2) + "," + (Short.MAX_VALUE - 3) + "," + (Short.MAX_VALUE - 4)
              + "]", ((DefaultMutableTreeNode) children.nextElement()).getUserObject().toString());
      assertEquals("longArrayFeature = [" + Long.MAX_VALUE + "," + (Long.MAX_VALUE - 1) + ","
              + (Long.MAX_VALUE - 2) + "," + (Long.MAX_VALUE - 3) + "," + (Long.MAX_VALUE - 4)
              + "]", ((DefaultMutableTreeNode) children.nextElement()).getUserObject().toString());
      assertEquals("doubleArrayFeature = [" + Double.MAX_VALUE + "," + Double.MIN_VALUE + ","
              + Double.parseDouble("1.5555") + "," + Double.parseDouble("99.000000005") + ","
              + Double.parseDouble("4.44444444444444444") + "]", ((DefaultMutableTreeNode) children
              .nextElement()).getUserObject().toString());

    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }
  
  public void testAddAnnotationToTreeJCas() throws Exception {
    this.cas.getJCas();
    testAddAnnotationToTree();
  }

  private void createExampleFS(CAS cas) throws Exception {
    // Set the document text
    cas.setDocumentText("this beer is good");

    // create an FS of exampleType and index it
    AnnotationFS fs = cas.createAnnotation(exampleType, 1, 5);
    cas.getIndexRepository().addFS(fs);

    // create Array FSs
    StringArrayFS strArrayFS = cas.createStringArrayFS(5);
    strArrayFS.set(0, "zzzzzz");
    strArrayFS.set(1, "yyyyyy");
    strArrayFS.set(2, "xxxxxx");
    strArrayFS.set(3, "wwwwww");
    strArrayFS.set(4, "vvvvvv");

    IntArrayFS intArrayFS = cas.createIntArrayFS(5);
    intArrayFS.set(0, Integer.MAX_VALUE);
    intArrayFS.set(1, Integer.MAX_VALUE - 1);
    intArrayFS.set(2, 42);
    intArrayFS.set(3, Integer.MIN_VALUE + 1);
    intArrayFS.set(4, Integer.MIN_VALUE);

    FloatArrayFS floatArrayFS = cas.createFloatArrayFS(5);
    floatArrayFS.set(0, Float.MAX_VALUE);
    floatArrayFS.set(1, (float) (Float.MAX_VALUE / 1000.0));
    floatArrayFS.set(2, (float) 42);
    floatArrayFS.set(3, (float) (Float.MIN_VALUE * 1000.0));
    floatArrayFS.set(4, Float.MIN_VALUE);

    ByteArrayFS byteArrayFS = cas.createByteArrayFS(5);
    byteArrayFS.set(0, (byte) 8);
    byteArrayFS.set(1, (byte) 16);
    byteArrayFS.set(2, (byte) 64);
    byteArrayFS.set(3, (byte) 128);
    byteArrayFS.set(4, (byte) 255);

    BooleanArrayFS boolArrayFS = cas.createBooleanArrayFS(8);
    boolean val = false;
    for (int i = 0; i < 8; i++) {
      boolArrayFS.set(i, val = !val);
    }

    ShortArrayFS shortArrayFS = cas.createShortArrayFS(5);
    shortArrayFS.set(0, Short.MAX_VALUE);
    shortArrayFS.set(1, (short) (Short.MAX_VALUE - 1));
    shortArrayFS.set(2, (short) (Short.MAX_VALUE - 2));
    shortArrayFS.set(3, (short) (Short.MAX_VALUE - 3));
    shortArrayFS.set(4, (short) (Short.MAX_VALUE - 4));

    LongArrayFS longArrayFS = cas.createLongArrayFS(5);
    longArrayFS.set(0, Long.MAX_VALUE);
    longArrayFS.set(1, Long.MAX_VALUE - 1);
    longArrayFS.set(2, Long.MAX_VALUE - 2);
    longArrayFS.set(3, Long.MAX_VALUE - 3);
    longArrayFS.set(4, Long.MAX_VALUE - 4);

    DoubleArrayFS doubleArrayFS = cas.createDoubleArrayFS(5);
    doubleArrayFS.set(0, Double.MAX_VALUE);
    doubleArrayFS.set(1, Double.MIN_VALUE);
    doubleArrayFS.set(2, Double.parseDouble("1.5555"));
    doubleArrayFS.set(3, Double.parseDouble("99.000000005"));
    doubleArrayFS.set(4, Double.parseDouble("4.44444444444444444"));

    // set features of fs
    fs.setStringValue(stringFeature, "aaaaaaa");
    fs.setFloatValue(floatFeature, (float) 99.99);

    fs.setFeatureValue(intArrayFeature, intArrayFS);
    fs.setFeatureValue(floatArrayFeature, floatArrayFS);
    fs.setFeatureValue(stringArrayFeature, strArrayFS);

    // fs.setByteValue(byteFeature, Byte.MAX_VALUE);
    fs.setByteValue(byteFeature, (byte) 'z');
    fs.setFeatureValue(byteArrayFeature, byteArrayFS);
    fs.setBooleanValue(booleanFeature, true);
    fs.setFeatureValue(booleanArrayFeature, boolArrayFS);
    fs.setShortValue(shortFeature, Short.MIN_VALUE);
    fs.setFeatureValue(shortArrayFeature, shortArrayFS);
    fs.setLongValue(longFeature, Long.MIN_VALUE);
    fs.setFeatureValue(longArrayFeature, longArrayFS);
    fs.setDoubleValue(doubleFeature, Double.MAX_VALUE);
    fs.setFeatureValue(doubleArrayFeature, doubleArrayFS);

    cas.getIndexRepository().addFS(fs);
  }
}
