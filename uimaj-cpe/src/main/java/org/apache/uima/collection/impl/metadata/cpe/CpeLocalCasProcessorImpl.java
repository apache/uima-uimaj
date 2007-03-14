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

package org.apache.uima.collection.impl.metadata.cpe;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.collection.metadata.CasProcessorDeploymentParam;
import org.apache.uima.collection.metadata.CasProcessorDeploymentParams;
import org.apache.uima.collection.metadata.CasProcessorExecArg;
import org.apache.uima.collection.metadata.CasProcessorExecutable;
import org.apache.uima.collection.metadata.CasProcessorRunInSeperateProcess;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.collection.metadata.CpeLocalCasProcessor;
import org.apache.uima.collection.metadata.NameValuePair;

public class CpeLocalCasProcessorImpl extends CasProcessorCpeObject implements CpeLocalCasProcessor {

  private static final long serialVersionUID = -2239520502855587544L;

  public CpeLocalCasProcessorImpl() {
    super();
    try {
      super.setDeployment("local");
      addDefaults();
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  public CpeLocalCasProcessorImpl(boolean initializeWithDefaultValues) {
    super();
    try {
      super.setDeployment("local");
      if (initializeWithDefaultValues) {
        addDefaults();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  protected CpeLocalCasProcessorImpl(String aName, String aSoFa) throws CpeDescriptorException {
    super();
    try {
      super.setName(aName);
      // super.setContentTag(aSoFa);
      super.setDeployment("local");
      addDefaults();
    } catch (Exception e) {
      e.printStackTrace();
      throw new CpeDescriptorException(e.getMessage());
    }
  }

  private CasProcessorRunInSeperateProcess getBaseRunInSeperateProcess()
          throws CpeDescriptorException {
    CasProcessorRunInSeperateProcess sepProcess = getRunInSeperateProcess();
    if (sepProcess == null) {
      sepProcess = CpeDescriptorFactory.produceRunInSeperateProcess();
      CasProcessorExecutable exe = CpeDescriptorFactory.produceCasProcessorExecutable();
      exe.setExecutable("default");
      sepProcess.setExecutable(exe);
      setRunInSeperateProcess(sepProcess);
    }
    return sepProcess;
  }

  public void addExecArg(String aArgValue) throws CpeDescriptorException {
    CasProcessorRunInSeperateProcess sepProcess = getBaseRunInSeperateProcess();
    CasProcessorExecutable exe = sepProcess.getExecutable();
    CasProcessorExecArg execArg = CpeDescriptorFactory.produceCasProcessorExecArg();
    execArg.setArgValue(aArgValue);
    exe.addCasProcessorExecArg(execArg);
  }

  public void removeExecArg(int aIndex) throws CpeDescriptorException {
    CasProcessorRunInSeperateProcess rip = getRunInSeparateProcess();
    if (rip != null) {
      CasProcessorExecutable exe = rip.getExecutable();
      if (exe != null) {
        exe.removeCasProcessorExecArg(aIndex);
      }
    }
  }

  public List getExecArgs() throws CpeDescriptorException {
    ArrayList list = new ArrayList();
    CasProcessorRunInSeperateProcess rip = getRunInSeparateProcess();
    if (rip != null) {
      CasProcessorExecutable exe = rip.getExecutable();
      if (exe != null) {
        CasProcessorExecArg[] args = exe.getAllCasProcessorExecArgs();
        for (int i = 0; args != null && i < args.length; i++) {
          list.add(args[i]);
        }
      }
    }

    return list;
  }

  protected void addDefaults() throws CpeDescriptorException {
    if (getRunInSeperateProcess() == null) {
      CasProcessorRunInSeperateProcess sepProcess = CpeDescriptorFactory
              .produceRunInSeperateProcess();
      CasProcessorExecutable exe = CpeDescriptorFactory.produceCasProcessorExecutable();
      exe.setExecutable("default");

      sepProcess.setExecutable(exe);
      setRunInSeperateProcess(sepProcess);
    }
    if (getDeploymentParams() == null) {
      CasProcessorDeploymentParams deployParams = CpeDescriptorFactory.produceDeployParams();
      CasProcessorDeploymentParam param = CpeDescriptorFactory.produceDeployParam();
      param.setParameterName("vnsHost");
      param.setParameterType("String");
      param.setParameterValue("127.0.0.1");
      deployParams.add(param);
      param = CpeDescriptorFactory.produceDeployParam();
      param.setParameterName("vnsPort");
      param.setParameterType("String");
      param.setParameterValue("9904");
      deployParams.add(param);
      this.setDeploymentParams(deployParams);
    }

    super.addDefaults();

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CpeLocalCasProcessor#isJava()
   */
  public boolean isJava() throws CpeDescriptorException {
    CasProcessorRunInSeperateProcess rip = getRunInSeparateProcess();
    if (rip != null) {
      CasProcessorExecutable exe = rip.getExecutable();
      if (exe != null) {
        return (exe.getExecutable().equalsIgnoreCase("java"));
      }
    }
    return false;
  }

  /**
   * 
   * Adds a new env key to the list of env keys. If a kay with a given key name exists the new key
   * value replaces the old.
   */
  public void addExecEnv(String aEnvKeyName, String aEnvKeyValue) throws CpeDescriptorException {
    CasProcessorRunInSeperateProcess rip = getRunInSeparateProcess();
    if (rip != null) {
      CasProcessorExecutable exe = rip.getExecutable();
      if (exe != null) {
        ArrayList envs = exe.getEnvs();
        NameValuePair nvp;
        boolean replacedExisiting = false;

        for (int i = 0; envs != null && i < envs.size(); i++) {
          nvp = (NameValuePair) envs.get(i);
          if (nvp.getName().equals(aEnvKeyName)) {
            nvp.setValue(aEnvKeyValue);
            replacedExisiting = true;
            break; // done
          }
        }
        if (envs != null && !replacedExisiting) {
          nvp = new NameValuePairImpl(aEnvKeyName, aEnvKeyValue);
          envs.add(nvp);
        }
      }
    }

    // }
  }

  public List getExecEnv() throws CpeDescriptorException {
    CasProcessorRunInSeperateProcess rip = getRunInSeparateProcess();
    if (rip != null) {
      CasProcessorExecutable exe = rip.getExecutable();
      if (exe != null) {
        return exe.getEnvs();
      }
    }
    return new ArrayList(); // empty list
  }

  public void removeExecEnv(int aIndex) throws CpeDescriptorException {
    CasProcessorRunInSeperateProcess rip = getRunInSeparateProcess();
    if (rip != null) {
      CasProcessorExecutable exe = rip.getExecutable();
      if (exe != null) {
        if (aIndex > exe.getEnvs().size()) {
          return;
        }
        exe.getEnvs().remove(aIndex);
      }
    }
  }

  public void setExecutable(String aCasProcessorExecutable) throws CpeDescriptorException {
    CasProcessorRunInSeperateProcess rip = getRunInSeparateProcess();
    if (rip != null) {
      CasProcessorExecutable exe = rip.getExecutable();
      if (exe != null) {
        exe.setExecutable(aCasProcessorExecutable);
      }
    }
  }

  public String getExecutable() throws CpeDescriptorException {
    CasProcessorRunInSeperateProcess rip = getRunInSeparateProcess();
    if (rip != null) {
      CasProcessorExecutable exe = rip.getExecutable();
      if (exe != null) {
        return exe.getExecutable();
      }
    }
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CpeLocalCasProcessor#setIsJava(boolean)
   */
  public void setIsJava(boolean aJava) throws CpeDescriptorException {
    CasProcessorRunInSeperateProcess rip = getRunInSeparateProcess();
    if (rip != null) {

      CasProcessorExecutable exe = rip.getExecutable();
      if (exe != null && aJava) {
        exe.setExecutable("java");
      }
    }
  }

  public void setRunInSeperateProcess(CasProcessorRunInSeperateProcess aSepProcess)
          throws CpeDescriptorException {
    super.setRunInSeparateProcess(aSepProcess);
  }

  public CasProcessorRunInSeperateProcess getRunInSeperateProcess() throws CpeDescriptorException {
    return super.getRunInSeparateProcess();
  }

}
