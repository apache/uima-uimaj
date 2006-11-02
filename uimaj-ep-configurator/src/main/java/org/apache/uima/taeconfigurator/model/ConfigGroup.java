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

package org.apache.uima.taeconfigurator.model;

import org.apache.uima.resource.metadata.ConfigurationGroup;
import org.apache.uima.resource.metadata.ConfigurationParameter;
import org.apache.uima.resource.metadata.ConfigurationParameterDeclarations;
import org.apache.uima.taeconfigurator.InternalErrorCDE;
import org.apache.uima.taeconfigurator.editors.ui.AbstractSection;
import org.apache.uima.taeconfigurator.editors.ui.AbstractSectionParm;

/**
 * Instances of this class model the 3 varients of configuration parameter sets.
 * 
 */
public class ConfigGroup {

  private ConfigurationParameterDeclarations cpd;
  private int kind;
  private ConfigurationGroup namedCg = null;
  
  public final static int NOT_IN_ANY_GROUP = 1;
  public final static int COMMON = 2;
  public final static int NAMED_GROUP = 4;
  
  public ConfigGroup(ConfigurationParameterDeclarations aCpd, int aKind) {
    cpd = aCpd;
    kind = aKind; 
    fixupCpd();
  }

  public ConfigGroup(ConfigurationParameterDeclarations aCpd, ConfigurationGroup aNamedCg) {
    cpd = aCpd;
    kind = NAMED_GROUP;
    namedCg = aNamedCg;
    fixupCpd();
  }
  
  private void fixupCpd() {
    if (null == cpd.getConfigurationParameters())
      cpd.setConfigurationParameters(AbstractSection.configurationParameterArray0);
    if (null == cpd.getCommonParameters())
      cpd.setCommonParameters(AbstractSection.configurationParameterArray0);
    if (null == cpd.getConfigurationGroups())
      cpd.setConfigurationGroups(AbstractSection.configurationGroupArray0);
    ConfigurationGroup [] cgs = cpd.getConfigurationGroups();
    for (int i = 0; i < cgs.length; i++) {
      if (null == cgs[i].getConfigurationParameters()) 
        cgs[i].setConfigurationParameters(AbstractSection.configurationParameterArray0);
    }
  }
  
  public ConfigurationParameter [] getConfigParms () {
    switch (kind) {
    case NOT_IN_ANY_GROUP:
      return cpd.getConfigurationParameters();
    case COMMON:
      return cpd.getCommonParameters();
    case NAMED_GROUP:
      return namedCg.getConfigurationParameters();
    default:
      throw new InternalErrorCDE("invalid state");
    }
  }
  
  public String getName() {
    switch (kind) {
    case NOT_IN_ANY_GROUP:
      return AbstractSectionParm.NOT_IN_ANY_GROUP;
    case COMMON:
      return AbstractSectionParm.COMMON_GROUP;
    case NAMED_GROUP:
      return AbstractSectionParm.groupNameArrayToString(namedCg.getNames());
    default:
      throw new InternalErrorCDE("invalid state");
    }
  }
  
  public String [] getNameArray() {
    switch (kind) {
    case NOT_IN_ANY_GROUP:
      return new String [] {AbstractSectionParm.NOT_IN_ANY_GROUP};
    case COMMON:
      return new String [] {AbstractSectionParm.COMMON_GROUP};
    case NAMED_GROUP:
      return namedCg.getNames();
    default:
      throw new InternalErrorCDE("invalid state");
    } 
  }
  
  public void setNameArray(String [] names) {
    if (kind != NAMED_GROUP)
      throw new InternalErrorCDE("invalid call");
    namedCg.setNames(names);
  }
  
  public int getKind() {
    return kind;
  }
  
  public ConfigurationParameterDeclarations getCPD() {
    return cpd;
  }
}
