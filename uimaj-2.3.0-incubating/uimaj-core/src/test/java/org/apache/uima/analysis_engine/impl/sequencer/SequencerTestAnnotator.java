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

package org.apache.uima.analysis_engine.impl.sequencer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Comparator;

import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.analysis_engine.TypeOrFeature;
import org.apache.uima.analysis_engine.annotator.AnnotatorConfigurationException;
import org.apache.uima.analysis_engine.annotator.AnnotatorContext;
import org.apache.uima.analysis_engine.annotator.AnnotatorContextException;
import org.apache.uima.analysis_engine.annotator.AnnotatorInitializationException;
import org.apache.uima.analysis_engine.annotator.AnnotatorProcessException;
import org.apache.uima.analysis_engine.annotator.Annotator_ImplBase;
import org.apache.uima.analysis_engine.annotator.TextAnnotator;
import org.apache.uima.cas.CAS;
import org.apache.uima.test.junit_extension.JUnitExtension;

/**
 * Dummy annotator which does not processing. Annotator only writes his name and
 * his result specification to a text file
 * 
 */
public class SequencerTestAnnotator extends Annotator_ImplBase implements
      TextAnnotator {
   // annotator name
   private String name;

   private File testBaseDir;

   public SequencerTestAnnotator() {
      super();
      this.testBaseDir = JUnitExtension.getFile("SequencerTest");
   }

   /**
    * method to read configuration parameter for the annotator
    */
   private static String secureGetConfigParameterValue(
         AnnotatorContext context, String param, String defaultValue)
         throws AnnotatorContextException {
      String name = (String) context.getConfigParameterValue(param);
      if (name != null) {
         return name;
      }
      return defaultValue;
   }

   /**
    * @see org.apache.uima.analysis_engine.annotator.BaseAnnotator#initialize(org.apache.uima.analysis_engine.annotator.AnnotatorContext)
    */
   public void initialize(AnnotatorContext context)
         throws AnnotatorInitializationException,
         AnnotatorConfigurationException {

      try {
         // read annotator name from configuration parameter 'AnnotatorName'
         this.name = secureGetConfigParameterValue(context, "AnnotatorName",
               "defaultName");
      } catch (AnnotatorContextException e) {
         throw new AnnotatorConfigurationException(e);
      }

   }

   /**
    * @see org.apache.uima.analysis_engine.annotator.TextAnnotator#process(org.apache.uima.cas.text.CAS,
    *      org.apache.uima.analysis_engine.ResultSpecification)
    */
   public void process(CAS tcas, ResultSpecification resultSpec)
         throws AnnotatorProcessException {
      if (true) {
         try {
            // use standard output file
            File fp = new File(this.testBaseDir, "SequencerTest.txt");
            if (!fp.exists()) {
               fp.createNewFile();
            }
            if (fp.canWrite()) {
               // write result specification to the output file
               OutputStreamWriter writer = new OutputStreamWriter(
                     new FileOutputStream(fp, true), "UTF-8");
               writer.write("\nResultSpec for annotator " + this.name + ":\n");
               TypeOrFeature[] tofs = resultSpec.getResultTypesAndFeatures();
               // sort by name to ensure consistent output for testing purposes
               Arrays.sort(tofs, new Comparator<TypeOrFeature>() {
                  public int compare(TypeOrFeature o1, TypeOrFeature o2) {
                     return o1.getName().compareTo(o2.getName());
                  }
               });
               for (int i = 0; i < tofs.length; i++) {
                  writer.write(tofs[i].getName() + "\n");
               }
               writer.flush();
               writer.close();
            } else {
               throw new IOException("Cannot write to " + fp.getAbsolutePath());
            }
         } catch (IOException e) {
            // If an error occurs, throw new annotator exception
            throw new AnnotatorProcessException(e);
         }
      }
   }
}
