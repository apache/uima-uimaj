/*
 Copyright 2011
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

package org.uimafit.factory;

import static org.apache.uima.fit.factory.TypePrioritiesFactory.createTypePriorities;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static org.junit.Assert.assertEquals;

import org.apache.uima.fit.factory.TypePrioritiesFactory;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.metadata.TypePriorities;
import org.apache.uima.util.CasCreationUtils;
import org.junit.Test;

/**
 * Tests for the {@link TypePrioritiesFactory}.
 *
 * @author Richard Eckart de Castilho
 */
public class TypePrioritiesFactoryTest {

	@Test
	public void testCreateTypePrioritiesClassOfQArray() throws Exception {
		TypePriorities prio = createTypePriorities(Annotation.class);

		CasCreationUtils.createCas(createTypeSystemDescription(), prio, null);

		assertEquals(1, prio.getPriorityLists().length);
		assertEquals(1, prio.getPriorityLists()[0].getTypes().length);
		assertEquals("uima.tcas.Annotation", prio.getPriorityLists()[0].getTypes()[0]);
	}
}
