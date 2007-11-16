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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.TypeSystemUtils;
import org.apache.uima.cas.impl.TypeSystemUtils.PathValid;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLParser;

/**
 * Class comment for IteratorTest.java goes here.
 * 
 */
public class TypeSystemUtilsTest extends TestCase {

  private CAS cas;

  public TypeSystemUtilsTest(String arg0) {
    super(arg0);
  }

  public void setUp() {

    File descriptorFile = JUnitExtension.getFile("CASTests/desc/pathValidationTS.xml");
    assertTrue("Descriptor must exist: " + descriptorFile.getAbsolutePath(), descriptorFile
        .exists());

    try {
      XMLParser parser = UIMAFramework.getXMLParser();
      TypeSystemDescription spec = (TypeSystemDescription) parser.parse(new XMLInputSource(
          descriptorFile));
      this.cas = CasCreationUtils.createCas(spec, null, new FsIndexDescription[] {});
    } catch (ResourceInitializationException e) {
      e.printStackTrace();
      assertTrue(false);
    } catch (InvalidXMLException e) {
      e.printStackTrace();
      assertTrue(false);
    } catch (IOException e) {
      e.printStackTrace();
      assertTrue(false);
    }

  }
  
  public void testPathValidation() {
    Type type1 = this.cas.getTypeSystem().getType("Type1");
    // Type1, f0/begin, always
    List<String> path = new ArrayList<String>();
    path.add("f0");
    path.add("begin");
    assertTrue(TypeSystemUtils.isPathValid(type1, path) == PathValid.ALWAYS);
    // Type1, f1, possible
    path = new ArrayList<String>();
    path.add("f1");
    assertTrue(TypeSystemUtils.isPathValid(type1, path) == PathValid.POSSIBLE);
    // Type1, f1/tail/tail, possible
    path = new ArrayList<String>();
    path.add("f1");
    path.add("tail");
    path.add("tail");
    assertTrue(TypeSystemUtils.isPathValid(type1, path) == PathValid.POSSIBLE);
    // Type1, f2, possible
    path = new ArrayList<String>();
    path.add("f2");
    assertTrue(TypeSystemUtils.isPathValid(type1, path) == PathValid.POSSIBLE);
    // Type1, nosuchfeature, never
    path = new ArrayList<String>();
    path.add("nosuchfeature");
    assertTrue(TypeSystemUtils.isPathValid(type1, path) == PathValid.NEVER);
    // Type1, <empty path>, always
    path = new ArrayList<String>();
    assertTrue(TypeSystemUtils.isPathValid(type1, path) == PathValid.ALWAYS);
    // t1, f1/f2/f3, always
    Type t1 = this.cas.getTypeSystem().getType("t1");
    path = new ArrayList<String>();
    path.add("f1");
    path.add("f2");
    path.add("f3");
    assertTrue(TypeSystemUtils.isPathValid(t1, path) == PathValid.ALWAYS);
  }

  public void tearDown() {
    this.cas = null;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(TypeSystemUtilsTest.class);
  }

}
