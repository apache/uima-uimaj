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

package org.apache.uima.caseditor.ide;

import java.io.IOException;
import java.io.InputStream;
import java.util.ResourceBundle;

import org.apache.uima.caseditor.CasEditorPlugin;
import org.apache.uima.caseditor.core.model.dotcorpus.DotCorpus;
import org.apache.uima.caseditor.core.model.dotcorpus.DotCorpusSerializer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * TODO: add javadoc here
 */
public class CasEditorIdePlugin extends AbstractUIPlugin {
  public static final String ID = "org.apache.uima.caseditor.ide";

  /**
   * The shared instance.
   */
  private static CasEditorIdePlugin sPlugin;

  /**
   * Resource bundle.
   */
  private ResourceBundle mResourceBundle;
  
  /**
   * The constructor.
   */
  public CasEditorIdePlugin() {
    super();

    sPlugin = this;
  }

  /**
   * This method is called upon plug-in activation
   *
   * @param context
   * @throws Exception -
   */
  @Override
  public void start(BundleContext context) throws Exception {
	    super.start(context);
	    
	    // Backward compatibility: Migrate old Cas Editor Projects
	    
	    // Scan for all Nlp nature projects
	    IProject projects[] = ResourcesPlugin.getWorkspace().getRoot().getProjects();
	    
	    for (IProject project : projects) {
	      // if nlp nature project
	      
	      if (project.isOpen() && project.hasNature("org.apache.uima.caseditor.NLPProject")) {
	        
	        // if ts property is not set ... 
	        String typeSystemLocation;
	        try {
	          typeSystemLocation = project.getPersistentProperty(new QualifiedName("", 
	        		  TypeSystemLocationPropertyPage.TYPE_SYSTEM_PROPERTY));
	        } catch (CoreException e) {
	          typeSystemLocation = null;
	        }
	        
	        if (typeSystemLocation == null) {
	          // 1. Read dotCorpus
	          IFile dotCorpusFile = project.getFile(".corpus");
	          
	          
	          if (dotCorpusFile.exists()) {
	            
	            InputStream dotCorpusIn = null;
	            
	            try {
	              dotCorpusIn = dotCorpusFile.getContents();
	            }
	            catch (CoreException e) {
	              log(e);
	            }
	            
	            IFile typeSystemFile = null;
	            if (dotCorpusIn != null) {
	              try {
	                DotCorpus dotCorpus = DotCorpusSerializer.parseDotCorpus(dotCorpusIn);
	                
	                if (dotCorpus.getTypeSystemFileName() != null)
	                    typeSystemFile = project.getFile(dotCorpus.getTypeSystemFileName());
	              }
	              finally {
	                try {
	                  dotCorpusIn.close();
	                }
	                catch (IOException e) {
	                  log(e);
	                }
	              }
	            }
	            
	            if (typeSystemFile != null && typeSystemFile.exists()) {
	              // 2. Set type system file accordingly
	              TypeSystemLocationPropertyPage.setTypeSystemLocation(project, typeSystemFile.getFullPath().toString());
	              
	              // 3. Try to copy dotCorpus file to type system location
	              try {
	                dotCorpusFile.copy(project.getFile(typeSystemFile.getParent().getProjectRelativePath() + "/" 
	                        + ".style-" + typeSystemFile.getName()).getFullPath(), true, null);
	                CasEditorPlugin.getDefault().setShowMigrationDialog();
	              } catch (CoreException e) {
	                log(e);
	              }
	            }
	          }
	        }
	      }
	    }
	  }

  /**
   * This method is called when the plug-in is stopped.
   *
   * @param context
   * @throws Exception -
   */
  @Override
  public void stop(BundleContext context) throws Exception {
    super.stop(context);

    sPlugin = null;
    mResourceBundle = null;
  }

  /**
   * Returns the shared instance.
   *
   * @return the TaePlugin
   */
  public static CasEditorIdePlugin getDefault() {
    return sPlugin;
  }

  /**
   * Log the throwable.
   *
   * @param t
   */
  public static void log(Throwable t) {
    getDefault().getLog().log(new Status(IStatus.ERROR, ID, IStatus.OK, t.getMessage(), t));
  }

  public static void logError(String message) {
    getDefault().getLog().log(new Status(IStatus.ERROR, ID, message));
  }
}
