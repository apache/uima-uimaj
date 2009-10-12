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

package org.apache.uima.caseditor.editor.outline;

import org.apache.uima.caseditor.editor.CasEditorError;
import org.eclipse.jface.action.Action;

/**
 * This action triggers the switch of the outline style.
 */
public class SwitchStyleAction extends Action {
	
	private AnnotationOutline outline;
	
	SwitchStyleAction(AnnotationOutline outline) {
		this.outline = outline;
	}
	
	@Override
	public String getText() {
		return "Switch style";
	}
	
	@Override
	public void run() {
		
		if (OutlineStyles.MODE.equals(outline.currentStyle())) {
			outline.switchStyle(OutlineStyles.TYPE);
		}
		else if (OutlineStyles.TYPE.equals(outline.currentStyle())) {
			outline.switchStyle(OutlineStyles.MODE);
		}
		else {
			throw new CasEditorError("Unkown style!");
		}
	}	
}
