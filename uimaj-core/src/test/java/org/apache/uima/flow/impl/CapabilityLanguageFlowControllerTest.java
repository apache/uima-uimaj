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
package org.apache.uima.flow.impl;

import static org.apache.uima.UIMAFramework.getXMLParser;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringWriter;

import org.apache.uima.flow.FlowControllerDescription;
import org.apache.uima.util.XMLInputSource;
import org.junit.jupiter.api.Test;

public class CapabilityLanguageFlowControllerTest {

  @Test
  public void thatGeneratedDefaultFlowDescriptionIsEqualToXmlDescription() throws Exception {
    FlowControllerDescription desc1 = CapabilityLanguageFlowController.getDescription();

    FlowControllerDescription desc2 = getXMLParser()
            .parseFlowControllerDescription(new XMLInputSource(
                    "src/test/resources/CapabilityLanguageFlowControllerTest/CapabilityLanguageFlowController.xml"));

    StringWriter desc1Writer = new StringWriter();
    desc1.toXML(desc1Writer);

    StringWriter desc2Writer = new StringWriter();
    desc2.toXML(desc2Writer);

    assertThat(desc2.toString()).isEqualTo(desc1.toString());
  }

  @Test
  public void thatChangesToDefaultFlowControllerDoNotCarryOver() throws Exception {
    FlowControllerDescription desc1 = CapabilityLanguageFlowController.getDescription();

    desc1.setImplementationName("otherImplementation");
    desc1.getMetaData().setName("otherName");

    FlowControllerDescription desc2 = CapabilityLanguageFlowController.getDescription();

    assertThat(desc2.getImplementationName())
            .isEqualTo(CapabilityLanguageFlowController.class.getName());
    assertThat(desc2.getMetaData().getName()).isEqualTo("Capability Language Flow Controller");
  }
}
