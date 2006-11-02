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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.taeconfigurator.editors.MultiPageEditor;

/**
 * Model part: names of the defined types, augmented with their supertypes
 * This means that some of the built-ins (those in the super chain) will be included.
 */
public class DefinedTypesWithSupers extends AbstractModelPart {
  
  private Set cachedResult;
  
  public DefinedTypesWithSupers(MultiPageEditor pModelRoot) {
    super(pModelRoot);
    cachedResult = new HashSet(modelRoot.INITIAL_SIZE_TYPE_COLLECTIONS);
  }
  
	/**
	 * @return a set of strings, including not only types
	 * defined in this TAE, but also supertypes of said types.
	 */
	public Set get() {
		if(dirty) {
		  update();
		  dirty = false;;
		}
		return cachedResult;
	}
	
	private void update() {
    cachedResult.clear();
		
		// for aggregates, this is the fully-merged type system
		// for all systems, it is the type system with imports resolved
		TypeSystemDescription typeSystemDescription = modelRoot.getMergedTypeSystemDescription();
			
		if(typeSystemDescription == null) 
		  return; // cleared table
		
		TypeDescription[] types = typeSystemDescription.getTypes();
		TypeSystem typeSystem = modelRoot.descriptorTCAS.get().getTypeSystem();
		
    String typeName; 
    Map allTypes = modelRoot.allTypes.get();
		for (int i = 0; i < types.length; i++) {
			cachedResult.add(typeName = types[i].getName());			
			Type nextType = (Type) allTypes.get(typeName);
			while (nextType != null) {
				nextType = typeSystem.getParent(nextType);
				if (nextType != null) 
					cachedResult.add(nextType.getName());
			}
		}
	}
}
