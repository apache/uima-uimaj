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

/*
 * Isolates Eclipse 3.2 content assist types
 */
package org.apache.uima.taeconfigurator.editors.ui.dialogs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.fieldassist.ContentAssistField;

public class ContentAssistField32 {
  
  final private ContentAssistField caf;
  
  ContentAssistField32(Composite tc, TypesWithNameSpaces candidatesToPickFrom) {
    TypesWithNameSpaces32 twns32 = new TypesWithNameSpaces32(candidatesToPickFrom);
    caf = new ContentAssistField(tc, SWT.BORDER, new org.eclipse.jface.fieldassist.TextControlCreator(), 
            new org.eclipse.jface.fieldassist.TextContentAdapter(), twns32,
            null, null);
    caf.getContentAssistCommandAdapter().setProposalAcceptanceStyle(org.eclipse.jface.fieldassist.ContentProposalAdapter.PROPOSAL_REPLACE);
  }

  public Text getControl() {
    return (Text) caf.getControl();
  }
}
