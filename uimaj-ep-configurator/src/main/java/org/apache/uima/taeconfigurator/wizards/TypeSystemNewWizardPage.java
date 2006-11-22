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

package org.apache.uima.taeconfigurator.wizards;

import org.eclipse.jface.viewers.ISelection;

/**
 * The "New" wizard page allows setting the container for the new file as well as the file name.
 * Will only accept file name without the extension OR with the extension that matches the expected
 * one (xml).
 */

public class TypeSystemNewWizardPage extends AbstractNewWizardPage {

  public TypeSystemNewWizardPage(ISelection selection) {
    super(selection, "big_t_s_.gif", "Type System Descriptor File",
                    "Create a new Type System Descriptor file", "typeSystemDescriptor.xml");
  }

}
