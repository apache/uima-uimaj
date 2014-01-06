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

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UIMAFramework;
import org.apache.uima.collection.impl.base_cpm.container.CasProcessorConfiguration;
import org.apache.uima.collection.impl.cpm.utils.CPMUtils;
import org.apache.uima.collection.impl.cpm.utils.Execute;
import org.apache.uima.collection.metadata.CasProcessorExecArg;
import org.apache.uima.collection.metadata.CpeCasProcessor;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.util.Level;
import org.apache.uima.util.UriUtils;

/**
 * Component responsible for configuring command line for java based CasProcessor. Each CasProcessor
 * is configured via CPE descriptor either statically (xml file) or dynamically by means of APIs. In
 * both cases, the java-based CasProcessor to be launched by the CPE must be properly setup for
 * launching. Its environment must be setup, command line, and any jvm arguments.
 * 
 * 
 */
public class JavaApplication extends RunnableApplication {
  /**
   * Creates an instance of component responsible for configuring java based CasProcessor.
   * 
   * @param aCasProcessorConfiguration -
   *          configuration for CasProcessor
   * @param aJaxbCasProcessorConfig
   * @throws ResourceConfigurationException passthru
   */
  public JavaApplication(CasProcessorConfiguration aCasProcessorConfiguration,
          CpeCasProcessor aJaxbCasProcessorConfig) throws ResourceConfigurationException {
    addApplicationInfo(aCasProcessorConfiguration, aJaxbCasProcessorConfig); 
  }

  /**
   * Sets up command line used to launch Cas Processor in a separate process. Combines environment
   * variables setup in the CPE descriptor with a System environment variables.
   * 
   * @param aCasProcessorConfiguration -
   *          access to Cas Processor configuration
   * @param aCasProcessor
   * @throws ResourceConfigurationException passthru
   */
  protected void addApplicationInfo(CasProcessorConfiguration aCasProcessorConfiguration,
          CpeCasProcessor aCasProcessor) throws ResourceConfigurationException {
    super.addApplicationInfo(aCasProcessorConfiguration, aCasProcessor);
    String[] cmdLine = addApplicationCmdLineArguments(aCasProcessorConfiguration, argList,
            executable);
    exec.setCmdLine(cmdLine);
  }

  /**
   * Adds to command line any program arguments configured for this Cas Processor in the CPE
   * descriptor
   * 
   * @param aCasProcessorConfiguration -
   *          Cas Processor configuration
   * @param argList -
   *          list of arguments set up in the CPE descriptor
   * @param aExecutable -
   *          executable program
   * 
   * @return - complete command line ready for use
   */
  protected String[] addApplicationCmdLineArguments(
          CasProcessorConfiguration aCasProcessorConfiguration, List argList, String aExecutable) 
      throws ResourceConfigurationException {
    ArrayList cmdArgs = new ArrayList();
    // build commandline
    cmdArgs.add(aExecutable);
    // This is only used for local deployment so hardcode VNS information used later to launch the
    // VNS.
    // The actual port used here is a default port, just before launching the process this port may
    // be changed to a different value since at this point we dont know what port the VNS will be
    // using.
    // The VNS starts up on port 9005 BUT if this port is not available the VNS will try to acquire
    // a
    // different one.
    cmdArgs.add("-DVNS_HOST=localhost"); // add host as a second object in the arraylist
    cmdArgs.add("-DVNS_PORT=9005"); // add port as a third object in the arraylist

    // The above are defaults and are used IFF the Cas Processor configuration does not specify
    // those explicitly.
    for (int i = 0; i < argList.size(); i++) {
      Object arg = argList.get(i);
      if (!(arg instanceof CasProcessorExecArg)) {
        continue;
      }
      String argValue = null;
      try {
        argValue = ((CasProcessorExecArg) arg).getArgValue();
      } catch (Exception e) {
        continue; // ignore
      }
      if (argValue == null || argValue.trim().length() == 0 || argValue.startsWith("-DVNS_HOST")
              || argValue.startsWith("-DVNS_PORT")) {
        continue; // skip
      }

      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_show_cmd_arg__FINEST",
                new Object[] { Thread.currentThread().getName(), String.valueOf(i), arg });
      }
      // Special case: if arg=-cp <classpath> we need to split this string into 2 due to the way
      // Java Runtime
      // object requirement that the command line params are seperate elements in the cmd array.
      if (argValue.trim().equalsIgnoreCase("-cp") && argValue.trim().length() > 3) // check if the
      // -cp contains
      // the classpath
      // string
      {
        String systemClasspath = getSysEnvVarValue("CLASSPATH");
        if (systemClasspath == null) {
          systemClasspath = "";
        }
        String classpath = argValue.trim().substring(3);
        // Overwrite location where arg was found.Split -cp and its value (classpath) into two.
        // First add the cp than add the classpath
        argList.add(i, "-cp");
        cmdArgs.add(arg);
        // add the actuall classpath to the arg list. This list is used to launch the application
        argList.add(i + 1, classpath + System.getProperty("path.separator") + systemClasspath);
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_show_cmd_classpath__FINEST",
                  new Object[] { Thread.currentThread().getName(), classpath });
        }
        continue; // next iteration will copy the classpath into cmdArgs
      }
      if ("${descriptor}".equals(argValue.trim())) {
        URL descriptorUrl = aCasProcessorConfiguration.getDescriptorUrl();
        String descriptorPath;
        try {
          descriptorPath = UriUtils.quote(descriptorUrl).getPath();
        } catch (URISyntaxException e) {
          throw new ResourceConfigurationException(e);
        }
        if (System.getProperty("os.name") != null
                && System.getProperty("os.name").toLowerCase().startsWith("linux")) {
          cmdArgs.add(descriptorPath);
        } else {
          cmdArgs.add("\"" + descriptorPath + "\"");
        }
      } else {
        cmdArgs.add(argValue);
      }

    }
    String[] cmdLine = null;
    // Due to the special case (-cp) we possible had modified the argument list. The new argument
    // list
    // needs to be copied (overwrite) previous argument list.
    if (cmdArgs.size() > 0) {
      // Overwrite the cmdLine setting with new settings
      cmdLine = new String[cmdArgs.size()];
      cmdArgs.toArray(cmdLine);
    }
    return cmdLine;
  }

  /**
   * Returns executable section of the CPE Descriptor for
   * 
   * @return executable section of the CPE Descriptor
   */
  public Execute getExecSpec() {
    return exec;
  }
}
