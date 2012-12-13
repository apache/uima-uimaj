/*
 Copyright 2009
 Ubiquitous Knowledge Processing (UKP) Lab
 Technische Universitaet Darmstadt
 All rights reserved.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package org.apache.uima.fit.descriptor;

import org.apache.uima.resource.Resource;

/**
 * Get instance of external resource. This resource implements the {@link Resource} interface and
 * can thus be used as an external resource. However, it serves only as a proxy to get the actual
 * shared resource. Parameters that help locating the shared resource can be passed using the
 * regular UIMA external resources mechanism.
 * <p>
 * A {@link ExternalResourceLocator} can be bound to a component field of the type of resource it
 * produces.
 * 
 * @see ExternalResource#api()
 * @author Richard Eckart de Castilho
 */
public interface ExternalResourceLocator extends Resource {
	/**
	 * @return the resource
	 */
	public Object getResource();
}
