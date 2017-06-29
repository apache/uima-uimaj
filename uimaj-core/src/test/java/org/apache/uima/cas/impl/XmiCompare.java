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
import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.internal.util.Misc;
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
 *   
 * Operation: 
 *   Prepare two CASs with the type system specified.
 *   Iterate over one of the set of xmi's:
 *     find the other corresponding xmi by name match
 *     load both
 *     compare the two cas's
 *     print the results
 * 
 * Compare technique:
 *   Get a set of roots - the items that are in any index
 *   Sort that by type, and then by content.  
 *   
 *   Compare: to compare FSRefs, follow the refs (but track, so don't get into loop)
 *              -- set to compare all elements of arrays  
 */
public class XmiCompare {

  private static final String BLANKS_89 = Misc.blanks.substring(0, 89);

  Path d1, d2;  // the input directories
  String d2String;
  CASImpl c1, c2;
  boolean isOk = true;
 

  public static void main(String[] args) {
    new XmiCompare().run(args);
  } 

  void run(String[] args) {
    try {
      // alternative to supplying args- hard code the path :-)
      if (args == null || args.length == 0) {
//        d1 = Paths.get("C:/au/t/uimaj/comparev2v3watsonx/med_nlp_with_20_notes_v2_afterFix1/out");
        d1 = Paths.get("C:/au/t/uimaj/comparev2v3watsonx/uimav3test_with_20_notes/out");
//        d1 = Paths.get("C:/au/t/uimaj/comparev2v3watsonx/med_nlp_with_20_notes_v2_2/out");    
//        d2 = Paths.get("C:/au/t/uimaj/comparev2v3watsonx/med_nlp_with_20_notes_v2/out");    
//        d2 = Paths.get("C:/au/t/uimaj/comparev2v3watsonx/med_nlp_with_20_notes_v2_2a/out");
        d2 = Paths.get("C:/au/t/uimaj/comparev2v3watsonx/med_nlp_with_20_notes_v2_afterFix/out");
      } else {
        d1 = Paths.get(args[0]);
        d2 = Paths.get(args[1]);
      }
      System.out.println("Comparing " + d1 + " to " + d2);
      d2String = d2.toString();
      
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

    try {
      CasIOUtils.load(new FileInputStream(p1.toFile()), c1);
      CasIOUtils.load(new FileInputStream(p2.toFile()), c2);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
          
    Iterator<FsIndex_singletype<FeatureStructure>> il1 = c1.indexRepository.streamNonEmptyIndexes(c1.getTypeSystemImpl().getTopType()).collect(Collectors.toList()).iterator();
    Iterator<FsIndex_singletype<FeatureStructure>> il2 = c2.indexRepository.streamNonEmptyIndexes(c2.getTypeSystemImpl().getTopType()).collect(Collectors.toList()).iterator();
    
    StringBuilder sb = new StringBuilder();
    boolean isSame = il1.hasNext() || il2.hasNext();
    while( il1.hasNext() || il2.hasNext()) {
      sb.setLength(0);
      String ts1 = null, ts2 = null;
      int sz1 = 0, sz2 = 0;
      FsIndex_singletype<FeatureStructure> idx;
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
        sb.append(String.format(" %,5d %s", sz2, ts2));
      } else {
        isSame = false;
      }
      System.out.println(sb.toString());
      if (isSame) {
        isSame = ts1.equals(ts2) && sz1 == sz2;
      }
    }

    System.out.println(isSame ? "Same number of types" : "Different numbers of types");
    
    CasCompare cc = new CasCompare(c1, c2);
//        cc.compareStringArraysAsSets(true);
    cc.compareArraysByElement(true);
    cc.compareAll(true);
    isOk = cc.compareCASes();
    
    if (isOk) {
      System.out.println("\n***************\n" +
                           "* COMPARE OK  *\n" +
                           "***************\n\n" );
    }
 
  }  
 
}
