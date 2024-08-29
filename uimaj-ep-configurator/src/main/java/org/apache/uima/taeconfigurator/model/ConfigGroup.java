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

  /** The cpd. */
  private ConfigurationParameterDeclarations cpd;

  /** The kind. */
  private int kind;

  /** The named cg. */
  private ConfigurationGroup namedCg = null;

  /** The Constant NOT_IN_ANY_GROUP. */
  public static final int NOT_IN_ANY_GROUP = 1;

  /** The Constant COMMON. */
  public static final int COMMON = 2;

  /** The Constant NAMED_GROUP. */
  public static final int NAMED_GROUP = 4;

  /**
   * Instantiates a new config group.
   *
   * @param aCpd
   *          the a cpd
   * @param aKind
   *          the a kind
   */
  public ConfigGroup(ConfigurationParameterDeclarations aCpd, int aKind) {
    cpd = aCpd;
    kind = aKind;
    fixupCpd();
  }

  /**
   * Instantiates a new config group.
   *
   * @param aCpd
   *          the a cpd
   * @param aNamedCg
   *          the a named cg
   */
  public ConfigGroup(ConfigurationParameterDeclarations aCpd, ConfigurationGroup aNamedCg) {
    cpd = aCpd;
    kind = NAMED_GROUP;
    namedCg = aNamedCg;
    fixupCpd();
  }

  /**
   * Fixup cpd.
   */
  private void fixupCpd() {
    if (null == cpd.getConfigurationParameters())
      cpd.setConfigurationParameters(AbstractSection.configurationParameterArray0);
    if (null == cpd.getCommonParameters())
      cpd.setCommonParameters(AbstractSection.configurationParameterArray0);
    if (null == cpd.getConfigurationGroups())
      cpd.setConfigurationGroups(AbstractSection.configurationGroupArray0);
    ConfigurationGroup[] cgs = cpd.getConfigurationGroups();
    for (int i = 0; i < cgs.length; i++) {
      if (null == cgs[i].getConfigurationParameters())
        cgs[i].setConfigurationParameters(AbstractSection.configurationParameterArray0);
    }
  }

  /**
   * Gets the config parms.
   *
   * @return the config parms
   */
  public ConfigurationParameter[] getConfigParms() {
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

  /**
   * Gets the name.
   *
   * @return the name
   */
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

  /**
   * Gets the name array.
   *
   * @return the name array
   */
  public String[] getNameArray() {
    switch (kind) {
      case NOT_IN_ANY_GROUP:
        return new String[] { AbstractSectionParm.NOT_IN_ANY_GROUP };
      case COMMON:
        return new String[] { AbstractSectionParm.COMMON_GROUP };
      case NAMED_GROUP:
        return namedCg.getNames();
      default:
        throw new InternalErrorCDE("invalid state");
    }
  }

  /**
   * Sets the name array.
   *
   * @param names
   *          the new name array
   */
  public void setNameArray(String[] names) {
    if (kind != NAMED_GROUP)
      throw new InternalErrorCDE("invalid call");
    namedCg.setNames(names);
  }

  /**
   * Gets the kind.
   *
   * @return the kind
   */
  public int getKind() {
    return kind;
  }

  /**
   * Gets the cpd.
   *
   * @return the cpd
   */
  public ConfigurationParameterDeclarations getCPD() {
    return cpd;
  }
}
