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

package org.apache.uima.jcas.jcasgenp;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.uima.tools.jcasgen.IError;
import org.apache.uima.tools.jcasgen.IProgressMonitor;
import org.apache.uima.tools.jcasgen.Jg;
import org.eclipse.emf.codegen.jmerge.JControlModel;
import org.eclipse.emf.codegen.jmerge.JMerger;
import org.eclipse.jdt.core.jdom.IDOMCompilationUnit;

public class MergerImpl implements org.apache.uima.tools.jcasgen.IMerge {
  static final String jControlModel = "jMergeCtl.xml";

  static JControlModel jControlModelInstance = null;

  public void doMerge(Jg jg, IProgressMonitor progressMonitor, String sourceContents,
          String targetContainer, String targetPath, String targetClassName, File targetFile)
          throws IOException {
    JMerger jMerger = new JMerger();
    IDOMCompilationUnit sourceCompilationUnit = jMerger
            .createCompilationUnitForContents(sourceContents);
    if (targetFile.exists()) {
      progressMonitor.subTask(jg.getString("updatingTarget", new Object[] { targetClassName }));

      synchronized (this) {
        if (null == jControlModelInstance) {
          String controlModelPath = jg.getClass().getResource(jControlModel).toString();
          jControlModelInstance = (new JControlModel(controlModelPath));
        }
      }
      jMerger.setControlModel(jControlModelInstance);
      // jMerger.setControlModel(
      // new JControlModel(
      // jg.getClass().getResource(jControlModel).toString()));
      try {

        jMerger.setSourceCompilationUnit(sourceCompilationUnit);
        jMerger.setTargetCompilationUnit(jMerger.createCompilationUnitForURI(targetPath));
      } catch (NullPointerException e) {
        jg.error.newError(IError.ERROR, jg.getString("nullPtr", new Object[] { targetClassName }),
                e);
      }

      // jMerger.getTargetPatternDictionary().dump();
      jMerger.merge();

      FileWriter fw = new FileWriter(targetPath);
      fw.write(jMerger.getTargetCompilationUnit().getContents());
      fw.close();
      // targetFile.setContents(
      // mergedContents,
      // true,
      // true,
      // new SubProgressMonitor(progressMonitor, 1));
    } else {
      progressMonitor.subTask(jg.getString("creatingTarget", new Object[] { targetClassName }));
      (new File(targetContainer)).mkdirs();
      FileWriter fw = new FileWriter(targetPath);
      fw.write(sourceContents);
      fw.close();
      // targetFile.setContents(
      // new ByteArrayInputStream(sourceContents.getBytes()),
      // true,
      // true,
      // new SubProgressMonitor(progressMonitor, 1));
    }
  }
}
