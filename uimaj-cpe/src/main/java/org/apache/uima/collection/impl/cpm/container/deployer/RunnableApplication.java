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

package org.apache.uima.collection.impl.cpm.container.deployer;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.apache.uima.UIMAFramework;
import org.apache.uima.collection.impl.base_cpm.container.CasProcessorConfiguration;
import org.apache.uima.collection.impl.cpm.utils.CPMUtils;
import org.apache.uima.collection.impl.cpm.utils.Execute;
import org.apache.uima.collection.metadata.CasProcessorExecArg;
import org.apache.uima.collection.metadata.CasProcessorRunInSeperateProcess;
import org.apache.uima.collection.metadata.CasProcessorRuntimeEnvParam;
import org.apache.uima.collection.metadata.CpeCasProcessor;
import org.apache.uima.collection.metadata.CpeLocalCasProcessor;
import org.apache.uima.internal.util.SystemEnvReader;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.util.Level;

public class RunnableApplication {
  protected String executable;

  protected Execute exec;

  protected ArrayList<String> environment = new ArrayList<String>();

  protected List argList = new ArrayList();
  
  private Properties sysEnvVars = null;

  /**
   * Sets up command line used to launch Cas Processor in a seperate process. Combines environment
   * variables setup in the CPE descriptor with a System environment variables.
   * 
   * @param aCasProcessorConfiguration -
   *          access to Cas Processor configuration
   * @param aCasProcessor
   * @throws ResourceConfigurationException wraps Exception
   */
  protected void addApplicationInfo(CasProcessorConfiguration aCasProcessorConfiguration,
          CpeCasProcessor aCasProcessor) throws ResourceConfigurationException {
    try {
      if (aCasProcessor instanceof CpeLocalCasProcessor) {
        CasProcessorRunInSeperateProcess rip = ((CpeLocalCasProcessor) aCasProcessor)
                .getRunInSeperateProcess();
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).log(Level.FINEST,
                  "+++++++++++++++++++++++++++++++++++++++++++++++++");
          UIMAFramework.getLogger(this.getClass()).log(Level.FINEST,
                  "+++++++++++++++++++++++++++++++++++++++++++++++++");
          UIMAFramework.getLogger(this.getClass()).log(Level.FINEST,
                  "Operating System is::::" + System.getProperty("os.name"));
          UIMAFramework.getLogger(this.getClass()).log(Level.FINEST,
                  "+++++++++++++++++++++++++++++++++++++++++++++++++");
          UIMAFramework.getLogger(this.getClass()).log(Level.FINEST,
                  "+++++++++++++++++++++++++++++++++++++++++++++++++");
        }
        executable = rip.getExecutable().getExecutable();
        List<CasProcessorRuntimeEnvParam> descrptrEnvVars = rip.getExecutable().getEnvs();

        exec = new Execute();

        try {
          sysEnvVars = SystemEnvReader.getEnvVars();
          if (System.getProperty("DEBUG") != null) {
            printSysEnvironment();
          }
        } catch (Throwable e) {
          e.printStackTrace();
        }

        environment.clear();
        boolean pathDone = false;
        boolean classpathDone = false;
        
        if (descrptrEnvVars != null) {

          // First copy all env vars from the CPE Descriptor treating PATH and CLASSPATH as special
          // cases
          
          for (CasProcessorRuntimeEnvParam descrptrEv : descrptrEnvVars) {
            String key = descrptrEv.getEnvParamName();
            String value = descrptrEv.getEnvParamValue();

            // Special Cases for PATH and CLASSPATH
            if (key.equalsIgnoreCase("PATH")) {
              String path = getSysEnvVarValue(key);
              if (path != null) {
                environment.add(key + "=" + value + System.getProperty("path.separator") + path);
              } else {
                environment.add(key + "=" + value + System.getProperty("path.separator"));
              }
              pathDone = true;
              continue;
            }

            if (key.equalsIgnoreCase("CLASSPATH")) {
              String classpath = getSysEnvVarValue(key);
              if (classpath != null) {
                environment.add(key + "=" + value + System.getProperty("path.separator")
                        + classpath);
              } else {
                environment.add(key + "=" + value + System.getProperty("path.separator"));
              }
              classpathDone = true;
              continue;
            }
            environment.add(key + "=" + value);
          }

        }
        // Now, copy all env vars from the current environment
        if (sysEnvVars != null) {
          Enumeration envKeys = sysEnvVars.keys();

          while (envKeys.hasMoreElements()) {
            String key = (String) envKeys.nextElement();
            // Skip those vars that we've already setup above
            if ((key.equalsIgnoreCase("PATH") && pathDone) || 
                (key.equalsIgnoreCase("CLASSPATH") && classpathDone)) {
              continue;
            }
            environment.add(key + "=" + sysEnvVars.getProperty(key));
          }
        }
        String[] envArray = new String[environment.size()];
        environment.toArray(envArray);
        exec.setEnvironment(envArray);
        
        CasProcessorExecArg[] args = rip.getExecutable().getAllCasProcessorExecArgs();
        for (int i = 0; args != null && i < args.length; i++) {
          argList.add(args[i]);
        }

      }
    } catch (Exception e) {
      throw new ResourceConfigurationException(e);
    }
  }

  /**
   * Displays current system environment settings
   * 
   */
  private void printSysEnvironment() {
    Properties sysEnv = null;
    try {
      sysEnv = SystemEnvReader.getEnvVars();
      Enumeration sysKeys = sysEnv.keys();
      while (sysKeys.hasMoreElements()) {
        String key = (String) sysKeys.nextElement();
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_launching_with_service_env__FINEST",
                  new Object[] { Thread.currentThread().getName(), key, sysEnv.getProperty(key) });
        }
      }
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  /**
   * Returns a value of a given environment variable
   * 
   * @param aKey -
   *          name of the environment variable
   * @return - value correspnding to environment variable
   */
  protected String getSysEnvVarValue(String aKey) {
    try {
      // Retrieve all env variables
//      sysEnv = SystemEnvReader.getEnvVars();  // already done, do only once
      Enumeration sysKeys = sysEnvVars.keys();
      while (sysKeys.hasMoreElements()) {
        String key = (String) sysKeys.nextElement();
        if (aKey.equalsIgnoreCase(key)) {
          return sysEnvVars.getProperty(key);
        }
      }
    } catch (Throwable e) {
      e.printStackTrace();
    }
    return null;
  }
}
