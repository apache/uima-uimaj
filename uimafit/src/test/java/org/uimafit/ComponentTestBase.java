/*
 Copyright 2009-2010	Regents of the University of Colorado.
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

package org.uimafit;

import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypePriorities;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.junit.Before;
import org.uimafit.factory.TypePrioritiesFactory;
import org.uimafit.factory.TypeSystemDescriptionFactory;
import org.uimafit.testing.factory.TokenBuilder;
import org.uimafit.type.Sentence;
import org.uimafit.type.Token;

/**
 * 
 * @author Philip Ogren
 * 
 */
public class ComponentTestBase {

	private static ThreadLocal<JCas> JCAS = new ThreadLocal<JCas>();
	private static ThreadLocal<TypeSystemDescription> TYPE_SYSTEM_DESCRIPTION = new ThreadLocal<TypeSystemDescription>();
	private static ThreadLocal<TypePriorities> TYPE_PRIORITIES = new ThreadLocal<TypePriorities>();
	private static ThreadLocal<TokenBuilder<Token, Sentence>> TOKEN_BUILDER = new ThreadLocal<TokenBuilder<Token, Sentence>>();

	static {
		try {
			TYPE_SYSTEM_DESCRIPTION.set(TypeSystemDescriptionFactory.createTypeSystemDescription());

			TypePriorities tp = TypePrioritiesFactory.createTypePriorities(new String[] {
					"org.uimafit.type.Sentence", "org.uimafit.type.AnalyzedText",
					"org.uimafit.type.Token" });
			TYPE_PRIORITIES.set(tp);

			JCas jCas = CasCreationUtils.createCas(TYPE_SYSTEM_DESCRIPTION.get(), tp, null)
					.getJCas();
			JCAS.set(jCas);

			TokenBuilder<Token, Sentence> tb = new TokenBuilder<Token, Sentence>(Token.class,
					Sentence.class, "pos", "stem");
			TOKEN_BUILDER.set(tb);
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	protected JCas jCas;
	protected TypeSystemDescription typeSystemDescription;
	protected TypePriorities typePriorities;
	protected TokenBuilder<Token, Sentence> tokenBuilder;

	/**
	 * we do not want to create a new JCas object every time we run a test because it is expensive
	 * (~100ms on my laptop). Instead, we will have one JCas per thread sitting around that we will
	 * reset everytime a new test is called.
	 */
	@Before
	public void setUp() {
		jCas = JCAS.get();
		jCas.reset();
		typeSystemDescription = TYPE_SYSTEM_DESCRIPTION.get();
		typePriorities = TYPE_PRIORITIES.get();
		tokenBuilder = TOKEN_BUILDER.get();
	}

}
