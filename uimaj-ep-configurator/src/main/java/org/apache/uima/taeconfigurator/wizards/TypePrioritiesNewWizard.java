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

import java.text.MessageFormat;

import org.eclipse.ui.INewWizard;

/**
 * Create a new file resource in the provided container. 
 * If the container resource (a folder or a project) is selected 
 * in the workspace when the wizard is opened, it will accept 
 * it as the target container. If a sample multi-page editor 
 * is registered for the same extension, it will be able to open it.
 */

public class TypePrioritiesNewWizard extends AbstractNewWizard implements INewWizard {

  public static final String TYPEPRIORITIES_TEMPLATE = 
    "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
    + "<typePriorities " + XMLNS_PART
    + "<name>{0}</name>\n"
    + "<description></description>\n"
    + "<version>1.0</version>\n"
    + "<vendor></vendor>\n"
    + "{1}"
    + "</typePriorities>\n";

	public TypePrioritiesNewWizard() {
		super("New Type Priorities Descriptor File");
	}
	
	public void addPages() {
		page = new TypePrioritiesNewWizardPage(selection);
		addPage(page);
	}

	public String getPrototypeDescriptor(String name) {
	  return MessageFormat.format(TYPEPRIORITIES_TEMPLATE, new Object[] {
        name, "<priorityLists></priorityLists>\n"});
	}

}
