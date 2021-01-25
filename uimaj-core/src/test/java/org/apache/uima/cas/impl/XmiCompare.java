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

package org.apache.uima.cas.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.internal.util.Misc;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.CasIOUtils;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;

/**
 * This is a utility that compares two xmi CASs and prints out the differences.
 * 
 * It is run as a Java Application, not as a test
 * 
 * It takes two directories, with the xmi CASs to compare (must be named the same):
 *   each directory has 1 special file named: CAS_typeSystem_desc.xml - this is the type system
 *   each has n other files: xmi CASes to compare
 * It takes an int "skip" argument, to skip over that many CASs before starting comparison.
 *   
 * It supports comparing results of UIMA V2 with V3, using a convention:
 *   The input directories must have names that start with uv2-out  or uv3-out
 * 
 * Compare technique:
 *   Get a set of roots - the items that are in any index
 *   Sort that by type, and then by content.  
 *   
 *   There are 3 sets of compare-relaxers.
 *     - uv2 to uv2 
 *     - uv3 to uv3   
 *     - uv2 to uv3
 *   
 */
public class XmiCompare {

  private static final String BLANKS_89 = Misc.blanks.substring(0, 89);

  Path d1, d2;  // the input directories
  private String d2String;
  private String d1String;
  
  boolean isV2V2;
  boolean isV3V3;
  boolean isV2V3;
  
  CASImpl c1, c2;
  boolean isOk = true;

  private int itemCount = 0;

  private CasCompare cc;

  private int skip;

 

  public static void main(String[] args) {
    new XmiCompare().run(args);
  } 

  void run(String[] args) {
    try {
      itemCount = 0;
      // alternative to supplying args- hard code the path :-)
      if (args == null || args.length == 0) {
        d1 = Paths.get("some-explicit-coded-path/uv2-out-some-suffix");
        d2 = Paths.get("some-explicit-coded-path/uv2-out-some-other-suffix");
        d1 = Paths.get("c:/a/t/ipd2018/uv2-out-b4-2");
//        d2 = Paths.get("c:/a/t/ipd2018/uv2-out-b4");
//        d1 = Paths.get("c:/a/t/ipd2018/uv2-out-measAnnot-fsiter-2c-getSurroundSent");
        d2 = Paths.get("c:/a/t/ipd2018/uv2-out-measAnnot-fsiter-2d-getSurroundSent-partial");
        
        d1 = Paths.get("c:/a/t/ipd2018/uv2-out-b4");
        d1 = Paths.get("c:/a/t/ipd2018/uv2-out-jp-merge-outer");
        d2 = Paths.get("c:/a/t/ipd2018/uv2-out-jp-merge");

        
//        skip = 725;  // optional skip amount
      } else {
        d1 = Paths.get(args[0]);
        d2 = Paths.get(args[1]);
        skip = Integer.parseInt(args[2]);
      }
      d1String = d1.toString();
      d2String = d2.toString();

      boolean d1v2 = d1String.contains("uv2-out");
      boolean d2v2 = d2String.contains("uv2-out");
      boolean d1v3 = d1String.contains("uv3-out");
      boolean d2v3 = d2String.contains("uv3-out");
      
      isV2V2 = d1v2 && d2v2;
      isV3V3 = d1v3 && d2v3;
      isV2V3 = (d1v2 && d2v3) || (d1v3 && d2v2);
      
      System.out.println("Comparing " + d1String + " to " + d2String);
      
      if (isV2V2) System.out.println("\napplying fixups for v2 versus v2 comparison");
      if (isV2V3) System.out.println("\napplying fixups for v2 versus v3 comparison");
      if (isV3V3) System.out.println("\napplying fixups for v3 versus v3 comparison");

      // read the type system descriptor
      File typeSystemFile = Paths.get(d2String, "CAS_typeSystem_desc.xml").toFile();
      TypeSystemDescription typeSystemDescription = UIMAFramework.getXMLParser().parseTypeSystemDescription(
          new XMLInputSource(typeSystemFile));
      
      c1 = (CASImpl) CasCreationUtils.createCas(typeSystemDescription, null, null);
      c2 = (CASImpl) CasCreationUtils.createCas(typeSystemDescription, null, null);
      
      Stream<Path> pathStream = Files.walk(d1, FileVisitOption.FOLLOW_LINKS);      
      pathStream.forEach(p1 -> maybeCompare(p1));
      pathStream.close();
      
    } catch (ResourceInitializationException | InvalidXMLException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Compares one xmi file, if it can find the other one
   * Skips non-xmi files
   * 
   * @param p1 - path of file to compare
   * @throws IOException 
   * @throws FileNotFoundException 
   */
  void maybeCompare(Path p1) {
    String p1name = p1.toString();
    if (!p1name.endsWith(".xmi")) {
      return;
    }
    
    Path p2 = Paths.get(d2String, p1.getFileName().toString());
    
    if (!p2.toFile().exists()) {
      return;
    }
    
    itemCount++;
    
    System.out.format("%,5d Comparing %s:",
        itemCount,
        p1.getFileName().toString());
    
    if (itemCount <= skip) {
      System.out.println(" skipped");
      return;
    }

    try {
      CasIOUtils.load(new FileInputStream(p1.toFile()), c1);
      CasIOUtils.load(new FileInputStream(p2.toFile()), c2);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
             
    compareNumberOfFSsByType(c1, c2);
    
    cc = new CasCompare(c1, c2);
    cc.compareAll(true);

    
    // put temporary customizations here.
//  removeAllExcept("SomeType");

    customizeCompare();
    
    isOk = cc.compareCASes();
     
    if (isOk) {
      System.out.println("  compare:OK");
    } 

  } 
  
  public static void compareNumberOfFSsByType(CAS cas1, CAS cas2) {
    CASImpl ci1 = (CASImpl)cas1;
    CASImpl ci2 = (CASImpl)cas2;
    Iterator<FsIndex_singletype<TOP>> il1 = ci1.indexRepository.streamNonEmptyIndexes(TOP.class).collect(Collectors.toList()).iterator();
    Iterator<FsIndex_singletype<TOP>> il2 = ci2.indexRepository.streamNonEmptyIndexes(TOP.class).collect(Collectors.toList()).iterator();

    StringBuilder sb = new StringBuilder();
    StringBuilder sba = new StringBuilder();
    boolean isSame = il1.hasNext() || il2.hasNext();
    while( il1.hasNext() || il2.hasNext()) {
      sb.setLength(0);
      String ts1 = null, ts2 = null;
      int sz1 = 0, sz2 = 0;
      FsIndex_singletype<TOP> idx;
      if (il1.hasNext()) {
        idx = il1.next();
        ts1 = idx.getType().getName();
        sz1 = idx.size();
        sb.append(String.format("%-83s %,5d", ts1, sz1));
      } else {
        isSame = false;
        sb.append(BLANKS_89);
      }
      if (il2.hasNext()) {
        idx = il2.next();
        ts2 = idx.getType().getName();
        sz2 = idx.size();
        String m = (ts2.equals(ts1) && sz2 == sz1) 
                     ? "same"
                     : ts2;
        sb.append(String.format(" %,5d %s", sz2, m));
      } else {
        isSame = false;
      }
      sba.append(sb).append('\n');
      if (isSame) {
        isSame = ts1.equals(ts2) && sz1 == sz2;
      }
    }

    if (!isSame) {
      System.out.println(sba);
    }
    System.out.format(" %s", isSame ? "Same number of types" : "Different numbers of types");
//    System.out.println(" skp cpr");
//    if (true) return;
  }
  
  private void customizeCompare() {
    if (isV2V2) {
      customizeV2V2();
    }

    if (isV3V3) {
      customizeV3V3();
    }
    
    if (isV2V3) {
      customizeV2V3();
    }
  }
  
  private void customizeV2V2() {
//  List<Runnable> r  = sortFSArray("com.ibm.watsonx.nlp_med.common_types.UMLS.Concept", "innerConcepts");
//  
//  for (Runnable a : r) {
//    if (a != null) {
//      a.run();
//    }
//  }
  
  // before sortStringArray
//  fixupTermMentionTypesUnknown();
//  cc.compareStringsAsEqual("com.ibm.watsonx.nlp_di.cas_term.Term", "mentionTypes", new String[]{"CATEGORY", "UNKNOWN"}, 1);
//  cc.compareStringsAsEqual("com.ibm.watsonx.nlp_di.cas_term.Term", "mentionTypes", new String[]{"CATEGORY", "UNKNOWN"}, 0);
//  cc.compareStringsAsEqual("com.ibm.watsonx.nlp_di.cas_term.Term", "mentionTypes", new String[]{"CATEGORY", "UNKNOWN"}, 2);

//  sortStringArray("com.ibm.watsonx.nlp_di.cas_term.Term", "mentionTypes");
//  v2FixupMentionType_mi("com.ibm.watsonx.nlp_di.hutt.UnitOfMeasurement");
    cc.addStringCongruenceSet("com.ibm.watsonx.nlp_di.xsg.PseudoParagraph", "ruleId", new String[]{"Odd", "Even"}, -1);
//  fixupComponentId("com.ibm.watsonx.nlp_di.hutt.Person",
//                   "R2/2.2.2/Pre/NONDEFROLE/\\Monitor",
//                   "R2/2.2.2/Pre/NONDEFROLE/\\monitor");
//  fixupComponentId("com.ibm.watsonx.nlp_di.hutt.NondefiningRole", 
//                   "R2/2.2.2/Pre/NONDEFROLE/\\Monitor",
//                   "R2/2.2.2/Pre/NONDEFROLE/\\monitor");
//
//  fixupComponentId("com.ibm.watsonx.nlp_di.hutt.Symptom", 
//                   "R2/2.2.2/Pre/NONDEFROLE/\\Sleeping",
//                   "R2/2.2.2/Pre/NONDEFROLE/\\sleeping");
//  fixupComponentId("com.ibm.watsonx.nlp_di.hutt.Symptom", 
//      "R2/2.2.2/Main/SYMPTOM/\\Sleeping",
//      "R2/2.2.2/Main/SYMPTOM/\\sleeping");
  }
  
  private void customizeV3V3() {
    List<Runnable> r = new ArrayList<>(); 
//  r.addAll(sortFSArray("com.ibm.watsonx.nlp_di.cas_term.Term", "outgoingLinks"));
//  r.addAll(sortFSArray("com.ibm.watsonx.nlp_di.cas_term.Term", "incomingLinks"));
//  r.addAll(sortFSArray("com.ibm.watsonx.nlp_di.hutt.Predicate", "arguments"));
//  r.addAll(sortFSArray("com.ibm.watsonx.nlp_di.cas_term.Expression", "termLinks"));      
//  r.addAll(sortFSArray("com.ibm.watsonx.nlp_med.common_types.UMLS.Concept", "innerConcepts"));
//  r.addAll(sortFSArray("com.ibm.watsonx.nlp_di.common_types.generic_relation.GenericRelation", "args"));
//  r.addAll(sortFSArray("com.ibm.watsonx.nlp_di.hutt.Predicate", "sources"));


  for (Runnable a : r) {
    if (a != null) {
      a.run();
    }
  }

//  v2FixupMentionType_mi("com.ibm.watsonx.nlp_di.hutt.UnitOfMeasurement");
//  cc.addStringCongruenceSet("com.ibm.watsonx.nlp_di.xsg.PseudoParagraph", "ruleId", new String[]{"Odd", "Even"}, -1);
//  fixupTermMentionTypesUnknown();
//  fixupComponentId("com.ibm.watsonx.nlp_di.hutt.UsState", 
//      "R2/2.2.2/Pre/STATE/State",
//      "R2/2.2.2/Pre/STATE/state");
//  fixupComponentId("com.ibm.watsonx.nlp_di.hutt.UsState", 
//      "R2/2.2.2/Pre/STATE/\\States",
//      "R2/2.2.2/Pre/STATE/\\states");
  }
  
  private void customizeV2V3() {
    List<Runnable> r = new ArrayList<>();
//  r = sortFSArray("com.ibm.watsonx.nlp_di.cas_term.Term", "outgoingLinks");
//  r.addAll(sortFSArray("com.ibm.watsonx.nlp_di.cas_term.Term", "incomingLinks"));
//  r.addAll(sortFSArray("com.ibm.watsonx.nlp_di.hutt.Predicate", "arguments"));
//  r.addAll(sortFSArray("com.ibm.watsonx.nlp_di.cas_term.Expression", "termLinks"));
//  r.addAll(sortFSArray("com.ibm.watsonx.nlp_med.common_types.UMLS.Concept", "innerConcepts"));
//  r.addAll(sortFSArray("com.ibm.watsonx.nlp_di.common_types.generic_relation.GenericRelation", "args"));
//  r.addAll(sortFSArray("com.ibm.watsonx.nlp_di.hutt.Predicate", "sources"));
  
    for (Runnable a : r) {
      if (a != null) {
        a.run();
      }
    }
  
  // from v2:
  // before sortStringArray
//  fixupTermMentionTypesUnknown();
//  sortStringArray("com.ibm.watsonx.nlp_di.cas_term.Term", "mentionTypes");
//  v2FixupMentionType_mi("com.ibm.watsonx.nlp_di.hutt.UnitOfMeasurement");
//  fixupComponentId("com.ibm.watsonx.nlp_di.hutt.Person",
//                   "R2/2.2.2/Pre/NONDEFROLE/\\Monitor",
//                   "R2/2.2.2/Pre/NONDEFROLE/\\monitor");
//  fixupComponentId("com.ibm.watsonx.nlp_di.hutt.NondefiningRole", 
//                   "R2/2.2.2/Pre/NONDEFROLE/\\Monitor",
//                   "R2/2.2.2/Pre/NONDEFROLE/\\monitor");
//
//  fixupComponentId("com.ibm.watsonx.nlp_di.hutt.Symptom", 
//                   "R2/2.2.2/Pre/NONDEFROLE/\\Sleeping",
//                   "R2/2.2.2/Pre/NONDEFROLE/\\sleeping");
//  fixupComponentId("com.ibm.watsonx.nlp_di.hutt.Symptom", 
//      "R2/2.2.2/Main/SYMPTOM/\\Sleeping",
//      "R2/2.2.2/Main/SYMPTOM/\\sleeping");
//
//  
//  
//  sortStringArray("com.ibm.watsonx.nlp_di.hutt.Predicate", "argumentLabels");
//  canonicalizeStringFirstVariant("com.ibm.watsonx.nlp_med.common_types.UMLS.SignOrSymptom", "conceptName", "variants");
    cc.addStringCongruenceSet("com.ibm.watsonx.nlp_di.xsg.PseudoParagraph", "ruleId", new String[]{"Odd", "Even"}, -1);
    canonicalizeString("com.ibm.watsonx.nlp_di.xsg.PseudoParagraph", "ruleId", new String[]{"Odd", "Even"}, "Odd");
  } 
 
  private List<Runnable> sortFSArray(String typename, String featurename) {
    List<Runnable> r = sortFSArray(typename, featurename, c1);
    r.addAll(sortFSArray(typename, featurename, c2));
    return r;
  }
  
  private void v2FixupMentionType_mi(String t) {
    v2FixupMentionType_mi(t, c1);
    v2FixupMentionType_mi(t, c2);    
  }
  
  private void v2FixupMentionType_mi(String t, CASImpl cas) {
    TypeSystem ts = cas.getTypeSystem();
    Type type = ts.getType(t);
    Feature f_mentionType = type.getFeatureByBaseName("mentionType");
    Feature f_componentId = type.getFeatureByBaseName("componentId");
    cas.select(type)
       .allViews()
       .filter(fs -> "R2/2.2.2/Main/UNITOFM/_mi".equals(fs.getStringValue(f_componentId)))
       .forEach(fs -> {
           fs.setStringValue(f_componentId, "R2/2.2.2/Main/UNITOFM/_mile");
           fs.setStringValue(f_mentionType, "CATEGORY");
         });
  }
  
  private void fixupComponentId(String t, String s1, String s2) {
    fixupComponentId(t, s1, s2, c1);
    fixupComponentId(t, s1, s2, c2);
  }
  
  private void fixupComponentId(String t, String s1, String s2, CASImpl cas) {
    TypeSystem ts = cas.getTypeSystem();
    Type type = ts.getType(t);
    Feature f_componentId = type.getFeatureByBaseName("componentId");
    cas.select(type)
       .allViews()
       .filter(fs -> s1.equals(fs.getStringValue(f_componentId)))
       .forEach(fs -> {
           fs.setStringValue(f_componentId, s2);
         });    
  }
  
  private void fixupTermMentionTypesUnknown() {
    fixupTermMentionTypesUnknown(c1);
    fixupTermMentionTypesUnknown(c2);
  }

  private void fixupTermMentionTypesUnknown(CASImpl cas) {
    TypeSystem ts = cas.getTypeSystem();
    Type type = ts.getType("com.ibm.watsonx.nlp_di.cas_term.Term");
    Feature f_mentionTypes = type.getFeatureByBaseName("mentionTypes");
    cas.select(type)
       .allViews()
       .map(fs -> (StringArray) fs.getFeatureValue(f_mentionTypes))
       .filter(fs -> fs != null && fs.contains("UNKNOWN"))
       .forEach(fs -> 
         {
           for (int i = 0; i < fs.size(); i++) {
             if ("UNKNOWN".equals(fs.get(i))) {
               fs.set(i, "CATEGORY");
             }
           }
         });
  }
  
  private List<Runnable> sortFSArray(String typename, String featurename, CASImpl cas) {
    TypeSystem ts = cas.getTypeSystem();
    Type type = ts.getType(typename);
    Feature feat = ts.getFeatureByFullName(typename + ":" + featurename);
    return cas.select(type).allViews().map(fs -> 
        cc.sortFSArray((FSArray)fs.getFeatureValue(feat))).collect(Collectors.toList());
  }
  
  private void sortStringArray(String t, String f) {
    sortStringArray(t, f, c1);
    sortStringArray(t, f, c2);
  }
  
  private void sortStringArray(String t, String f, CASImpl cas) {
    TypeSystem ts = cas.getTypeSystem();
    Type type = ts.getType(t);
    Feature feat = ts.getFeatureByFullName(t + ":" + f);
    cas.select(type).allViews().forEach(fs ->
      { StringArray sa = (StringArray) fs.getFeatureValue(feat);
        if (sa != null && sa.size() > 2) {
          Arrays.sort(sa._getTheArray());
        }
      });
  }
  
  
  
  private void canonicalizeStringFirstVariant(String t, String f, String v) {
    canonicalizeStringFirstVariant(t, f, v, c1);
    canonicalizeStringFirstVariant(t, f, v, c2);
  }
  
  void canonicalizeStringFirstVariant(String t, String f, String v, CASImpl cas) {
    TypeSystem ts = cas.getTypeSystem();
    Type type = ts.getType(t);
    Feature feat = ts.getFeatureByFullName(t + ":" + f);
    Feature featv = ts.getFeatureByFullName(t + ":" + v);  // v is the variant array
    cas.select(type).allViews().forEach(fs ->
      { StringArray sa = (StringArray) fs.getFeatureValue(featv);
        if (sa != null && sa.size() > 2) {
          String item = fs.getStringValue(feat);
          if (sa.contains(item)) {
            fs.setStringValue(feat, sa.get(0));
          }
        }
      });
  }
    
  private void canonicalizeString(String t, String f, String[] filter, String cv) {
    canonicalizeString(t, f, filter, cv, c1);
    canonicalizeString(t, f, filter, cv, c2);
  }

  void canonicalizeString(String t, String f, String[] filter, String cv, CASImpl cas) {
    TypeSystem ts = cas.getTypeSystem();
    Type type = ts.getType(t);
    Feature feat = ts.getFeatureByFullName(t + ":" + f);
    cas.select(type).allViews().forEach(fs ->
      { String item = fs.getStringValue(feat);
        if (Misc.contains(filter, item)) {
            fs.setStringValue(feat, cv);
        }
      });    
  }

  void removeAllExcept(String v) {
    removeAllExcept(v, c1);
    removeAllExcept(v, c2);
  }
  
  void removeAllExcept(String v, CASImpl c) {
    Iterator<AnnotationFS> it = c.getAnnotationIndex().iterator();
    while (it.hasNext()) {
      TOP item = (TOP) it.next();
      if (item._getTypeImpl().getName().contains(v)) {
        continue;
      }
      item.removeFromIndexes();
    }
  }
}
