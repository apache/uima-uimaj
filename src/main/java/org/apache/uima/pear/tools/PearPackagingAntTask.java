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
package org.apache.uima.pear.tools;

import java.util.ArrayList;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Parameter;

/**
 * Class to create and ANT task for packaging a PEAR file. 
 * 
 * See the example below on how to add the target to your ant build.
 * 
 * <code>
 *  &lt;target name="pearPackaging"&gt;
 *     &lt;taskdef name="packagePear"
 *        classname="org.apache.uima.pear.tools.PearPackagingAntTask"
 *        classpath="class/path/to/uima-core.jar"/&gt;
 *     &lt;packagePear componentID="SamplePear" 
 *        mainComponentDesc="desc/mainComponentDesc.xml" 
 *        classpath="$main_root/pear;component;classpath" 
 *        mainComponentDir="/home/mainComponentDir" 
 *        targetDir="/home/pears"&gt;
 *        &lt;envVar name="var1" value="value1"/&gt;
 *        &lt;envVar name="var2" value="value2"/&gt;
 *    &lt;/packagePear&gt;
 *    &lt;/target&gt;
 * </code>
 * 
 */
public class PearPackagingAntTask extends Task {

  private String componentID = null;

  private String mainComponentDesc = null;

  private String classpath = null;

  private String datapath = null;

  private String mainComponentDir = null;

  private String targetDir = null;

  private ArrayList params = new ArrayList();

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.tools.ant.Task#execute()
   */
  public void execute() throws BuildException {
    super.execute();

    // get environment variables for the pear component
    Properties props = null;
    if (this.params.size() > 0) {
      props = new Properties();
      for (int i = 0; i < this.params.size(); i++) {
        Parameter param = (Parameter) this.params.get(i);
        props.setProperty(param.getName(), param.getValue());
      }
    }

    //call pear pacakger
    try {
      PackageCreator.generatePearPackage(this.componentID, this.mainComponentDesc, this.classpath,
              this.datapath, this.mainComponentDir, this.targetDir, props);
    } catch (PackageCreatorException ex) {
      throw new BuildException(ex);
    }
  }

  /**
   * @param envVar
   */
  public void addEnvVar(Parameter envVar) {
    this.params.add(envVar);
  }

  /**
   * @param classpath
   *          the classpath to set
   */
  public void setClasspath(String classpath) {
    this.classpath = classpath;
  }

  /**
   * @param componentID
   *          the componentID to set
   */
  public void setComponentID(String componentID) {
    this.componentID = componentID;
  }

  /**
   * @param datapath
   *          the datapath to set
   */
  public void setDatapath(String datapath) {
    this.datapath = datapath;
  }

  /**
   * @param mainComponentDesc
   *          the mainComponentDesc to set
   */
  public void setMainComponentDesc(String mainComponentDesc) {
    this.mainComponentDesc = mainComponentDesc;
  }

  /**
   * @param mainComponentDir
   *          the mainComponentDir to set
   */
  public void setMainComponentDir(String mainComponentDir) {
    this.mainComponentDir = mainComponentDir;
  }

  /**
   * @param targetDir
   *          the targetDir to set
   */
  public void setTargetDir(String targetDir) {
    this.targetDir = targetDir;
  }

}
