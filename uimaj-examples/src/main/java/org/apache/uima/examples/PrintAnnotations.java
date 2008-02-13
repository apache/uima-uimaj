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

package org.apache.uima.examples;

import java.io.File;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.FloatArrayFS;
import org.apache.uima.cas.IntArrayFS;
import org.apache.uima.cas.StringArrayFS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.FileUtils;
import org.apache.uima.util.XMLInputSource;

/**
 * A simple example of how to extract information from the CAS. This example retrieves all
 * annotations of a specified type from a CAS and prints them (along with all of their features) to
 * a PrintStream.
 * 
 * 
 */
public class PrintAnnotations {

  /**
   * Prints all Annotations to a PrintStream.
   * 
   * @param aCAS
   *          the CAS containing the FeatureStructures to print
   * @param aOut
   *          the PrintStream to which output will be written
   */
  public static void printAnnotations(CAS aCAS, PrintStream aOut) {
    // get iterator over annotations
    FSIterator iter = aCAS.getAnnotationIndex().iterator();

    // iterate
    while (iter.isValid()) {
      FeatureStructure fs = iter.get();
      printFS(fs, aCAS, 0, aOut);
      iter.moveToNext();
    }
  }

  /**
   * Prints all Annotations of a specified Type to a PrintStream.
   * 
   * @param aCAS
   *          the CAS containing the FeatureStructures to print
   * @param aAnnotType
   *          the Type of Annotation to be printed
   * @param aOut
   *          the PrintStream to which output will be written
   */
  public static void printAnnotations(CAS aCAS, Type aAnnotType, PrintStream aOut) {
    // get iterator over annotations
    FSIterator iter = aCAS.getAnnotationIndex(aAnnotType).iterator();

    // iterate
    while (iter.isValid()) {
      FeatureStructure fs = iter.get();
      printFS(fs, aCAS, 0, aOut);
      iter.moveToNext();
    }
  }

  /**
   * Prints a FeatureStructure to a PrintStream.
   * 
   * @param aFS
   *          the FeatureStructure to print
   * @param aCAS
   *          the CAS containing the FeatureStructure
   * @param aNestingLevel
   *          number of tabs to print before each line
   * @param aOut
   *          the PrintStream to which output will be written
   */
  public static void printFS(FeatureStructure aFS, CAS aCAS, int aNestingLevel, PrintStream aOut) {
    Type stringType = aCAS.getTypeSystem().getType(CAS.TYPE_NAME_STRING);

    printTabs(aNestingLevel, aOut);
    aOut.println(aFS.getType().getName());

    // if it's an annotation, print the first 64 chars of its covered text
    if (aFS instanceof AnnotationFS) {
      AnnotationFS annot = (AnnotationFS) aFS;
      String coveredText = annot.getCoveredText();
      printTabs(aNestingLevel + 1, aOut);
      aOut.print("\"");
      if (coveredText.length() <= 64) {
        aOut.print(coveredText);
      } else {
        aOut.println(coveredText.substring(0, 64) + "...");
      }
      aOut.println("\"");
    }

    // print all features
    List aFeatures = aFS.getType().getFeatures();
    Iterator iter = aFeatures.iterator();
    while (iter.hasNext()) {
      Feature feat = (Feature) iter.next();
      printTabs(aNestingLevel + 1, aOut);
      // print feature name
      aOut.print(feat.getShortName());
      aOut.print(" = ");
      // prnt feature value (how we get this depends on feature's range type)
      String rangeTypeName = feat.getRange().getName();
      if (aCAS.getTypeSystem().subsumes(stringType, feat.getRange())) // must check for subtypes of
                                                                      // string
      {
        String str = aFS.getStringValue(feat);
        if (str == null) {
          aOut.println("null");
        } else {
          aOut.print("\"");
          if (str.length() > 64) {
            str = str.substring(0, 64) + "...";
          }
          aOut.print(str);
          aOut.println("\"");
        }
      } else if (CAS.TYPE_NAME_INTEGER.equals(rangeTypeName)) {
        aOut.println(aFS.getIntValue(feat));
      } else if (CAS.TYPE_NAME_FLOAT.equals(rangeTypeName)) {
        aOut.println(aFS.getFloatValue(feat));
      } else if (CAS.TYPE_NAME_STRING_ARRAY.equals(rangeTypeName)) {
        StringArrayFS arrayFS = (StringArrayFS) aFS.getFeatureValue(feat);
        if (arrayFS == null) {
          aOut.println("null");
        } else {
          String[] vals = arrayFS.toArray();
          aOut.print("[");
          for (int i = 0; i < vals.length - 1; i++) {
            aOut.print(vals[i]);
            aOut.print(',');
          }
          if (vals.length > 0) {
            aOut.print(vals[vals.length - 1]);
          }
          aOut.println("]\"");
        }
      } else if (CAS.TYPE_NAME_INTEGER_ARRAY.equals(rangeTypeName)) {
        IntArrayFS arrayFS = (IntArrayFS) aFS.getFeatureValue(feat);
        if (arrayFS == null) {
          aOut.println("null");
        } else {
          int[] vals = arrayFS.toArray();
          aOut.print("[");
          for (int i = 0; i < vals.length - 1; i++) {
            aOut.print(vals[i]);
            aOut.print(',');
          }
          if (vals.length > 0) {
            aOut.print(vals[vals.length - 1]);
          }
          aOut.println("]\"");
        }
      } else if (CAS.TYPE_NAME_FLOAT_ARRAY.equals(rangeTypeName)) {
        FloatArrayFS arrayFS = (FloatArrayFS) aFS.getFeatureValue(feat);
        if (arrayFS == null) {
          aOut.println("null");
        } else {
          float[] vals = arrayFS.toArray();
          aOut.print("[");
          for (int i = 0; i < vals.length - 1; i++) {
            aOut.print(vals[i]);
            aOut.print(',');
          }
          if (vals.length > 0) {
            aOut.print(vals[vals.length - 1]);
          }
          aOut.println("]\"");
        }
      } else // non-primitive type
      {
        FeatureStructure val = aFS.getFeatureValue(feat);
        if (val == null) {
          aOut.println("null");
        } else {
          printFS(val, aCAS, aNestingLevel + 1, aOut);
        }
      }
    }
  }

  /**
   * Prints tabs to a PrintStream.
   * 
   * @param aNumTabs
   *          number of tabs to print
   * @param aOut
   *          the PrintStream to which output will be written
   */
  private static void printTabs(int aNumTabs, PrintStream aOut) {
    for (int i = 0; i < aNumTabs; i++) {
      aOut.print("\t");
    }
  }

  /**
   * Main program for testing this class. Ther are two required arguments - the path to the XML
   * descriptor for the TAE to run and an input file. Additional arguments are Type or Feature names
   * to be included in the ResultSpecification passed to the TAE.
   */
  public static void main(String[] args) {
    try {
      File taeDescriptor = new File(args[0]);
      File inputFile = new File(args[1]);

      // get Resource Specifier from XML file or TEAR
      XMLInputSource in = new XMLInputSource(taeDescriptor);
      ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(in);

      // create Analysis Engine
      AnalysisEngine ae = UIMAFramework.produceAnalysisEngine(specifier);
      // create a CAS
      CAS cas = ae.newCAS();

      // build ResultSpec if Type and Feature names were specified on commandline
      ResultSpecification resultSpec = null;
      if (args.length > 2) {
        resultSpec = ae.createResultSpecification(cas.getTypeSystem());
        for (int i = 2; i < args.length; i++) {
          if (args[i].indexOf(':') > 0) // feature name
          {
            resultSpec.addResultFeature(args[i]);
          } else {
            resultSpec.addResultType(args[i], false);
          }
        }
      }

      // read contents of file
      String document = FileUtils.file2String(inputFile);

      // send doc through the AE
      cas.setDocumentText(document);
      ae.process(cas, resultSpec);

      // print results
      Type annotationType = cas.getTypeSystem().getType(CAS.TYPE_NAME_ANNOTATION);
      PrintAnnotations.printAnnotations(cas, annotationType, System.out);

      // destroy AE
      ae.destroy();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
