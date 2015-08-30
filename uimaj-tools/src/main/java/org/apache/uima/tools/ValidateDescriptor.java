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

package org.apache.uima.tools;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UIMA_IllegalStateException;
import org.apache.uima.resource.ResourceCreationSpecifier;
import org.apache.uima.util.XMLInputSource;

/**
 * Command-line utility for validating a descriptor. Supports CollectionReader, CasInitializer,
 * AnalysisEngine, and CasConsumer descriptors.
 */
public class ValidateDescriptor {

  /**
   * Runs the ValidateDescriptor tool.
   * 
   * @param args
   *          takes one argument, the path to a descriptor file.
   */
  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      System.err
              .println("Usage: java " + ValidateDescriptor.class.getName() + " <descriptor file>");
      System.exit(1);
    }
    System.out.println("Parsing...");
    try {
      Object desc = UIMAFramework.getXMLParser().parse(new XMLInputSource(args[0]));
      if (desc instanceof ResourceCreationSpecifier) {
        System.out.println("Validating...");
        ((ResourceCreationSpecifier) desc).doFullValidation();
        System.out.println("Descriptor is valid.");
      } else {
        System.err.println("This type of descriptor is not supported by this tool.");
      }
    } catch (UIMA_IllegalStateException e) {
      // this is unforunately the type of Exception we get if you try to parse a CPE descriptor this
      // way
      System.err.println("This type of descriptor is not supported by this tool.");
    } catch (Exception e) {
      System.err.println("Descriptor is invalid.");
      e.printStackTrace();
    }
  }

}
