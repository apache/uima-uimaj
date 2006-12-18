package org.apache.uima.cas.test;

import java.io.File;

import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.analysis_engine.annotator.JTextAnnotator_ImplBase;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.LowLevelCAS;
import org.apache.uima.cas.impl.LowLevelTypeSystem;
import org.apache.uima.jcas.impl.JCas;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.XMLInputSource;

public class StringSubtypeTest extends TestCase {

  private static final String specifier = "./CASTests/desc/StringSubtypeTest.xml";

  private static final String definedValue1 = "aa";

  private static final String definedValue2 = "bb";

  private static final String definedValue3 = "cc";

  private static final String undefinedValue = "dd";

  private static final String annotationTypeName = "org.apache.uima.cas.test.TestAnnotation";

  private static final String stringSetFeatureName = "stringSetFeature";

  private JCas jcas;

  private AnalysisEngine ae;

  public static class Annotator extends JTextAnnotator_ImplBase {

    public void process(JCas aJCas, ResultSpecification aResultSpec) {
      // Does nothing, not used in this test.
    }

  }

  public StringSubtypeTest(String arg0) {
    super(arg0);
  }

  protected void setUp() throws Exception {
    super.setUp();
    File specifierFile = JUnitExtension.getFile(specifier);
    XMLInputSource in = new XMLInputSource(specifierFile);
    ResourceSpecifier resourceSpecifier = UIMAFramework.getXMLParser().parseResourceSpecifier(in);
    this.ae = UIMAFramework.produceAnalysisEngine(resourceSpecifier);
    this.jcas = this.ae.newJCas();
  }

  protected void tearDown() throws Exception {
    super.tearDown();
    this.ae.destroy();
    this.jcas = null;
  }

  public void testJcas() {
    TestAnnotation annot = new TestAnnotation(this.jcas);
    annot.setStringSetFeature(definedValue1);
    annot.setStringSetFeature(definedValue2);
    annot.setStringSetFeature(definedValue3);
    boolean exCaught = false;
    try {
      annot.setStringSetFeature(undefinedValue);
    } catch (CASRuntimeException e) {
      exCaught = true;
    }
    assertTrue(exCaught);
  }

  public void testLowLevelCas() {
    LowLevelCAS cas = this.jcas.getLowLevelCas();
    LowLevelTypeSystem ts = cas.ll_getTypeSystem();
    final int annotType = ts.ll_getCodeForTypeName(annotationTypeName);
    final int addr = cas.ll_createFS(annotType);
    final int stringSetFeat = ts.ll_getCodeForFeatureName(annotationTypeName
	+ TypeSystem.FEATURE_SEPARATOR + stringSetFeatureName);
    cas.ll_setStringValue(addr, stringSetFeat, definedValue1);
    cas.ll_setStringValue(addr, stringSetFeat, definedValue2);
    cas.ll_setStringValue(addr, stringSetFeat, definedValue3);
    boolean exCaught = false;
    try {
      cas.ll_setStringValue(addr, stringSetFeat, undefinedValue);
    } catch (CASRuntimeException e) {
      exCaught = true;
    }
    assertTrue(exCaught);
  }

}
