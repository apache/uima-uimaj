/* 
  Copyright 2010 Regents of the University of Colorado.  
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

import java.io.IOException;

import org.apache.uima.ResourceSpecifierFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_component.CasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.CasIterator;
import org.apache.uima.analysis_engine.metadata.FixedFlow;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.util.Progress;
import org.junit.Test;

/**
 * @author Richard Eckart de Castilho
 */
public class AggregateWithReaderTest {
	
	/**
	 * Demo of running a collection reader as part of an aggregate engine. This allows to run
	 * a pipeline an access the output CASes directly - no need to write the data to files.
	 */
	@Test
	public void demoAggregateWithReader() throws UIMAException {
		ResourceSpecifierFactory factory = UIMAFramework.getResourceSpecifierFactory();

		CollectionReaderDescription reader = factory.createCollectionReaderDescription();
		reader.getMetaData().setName("reader");
		reader.setImplementationName(SimpleReader.class.getName());

		AnalysisEngineDescription analyzer = factory.createAnalysisEngineDescription();
		analyzer.getMetaData().setName("analyzer");
		analyzer.setPrimitive(true);
		analyzer.setImplementationName(SimpleAnalyzer.class.getName());

		FixedFlow flow = factory.createFixedFlow();
		flow.setFixedFlow(new String[] { "reader", "analyzer" });

		AnalysisEngineDescription aggregate = factory.createAnalysisEngineDescription();
		aggregate.getMetaData().setName("aggregate");
		aggregate.getAnalysisEngineMetaData().setFlowConstraints(flow);
		aggregate.getAnalysisEngineMetaData().getOperationalProperties().setOutputsNewCASes(true);
		aggregate.getAnalysisEngineMetaData().getOperationalProperties()
				.setMultipleDeploymentAllowed(false);
		aggregate.setPrimitive(false);
		aggregate.getDelegateAnalysisEngineSpecifiersWithImports().put("reader", reader);
		aggregate.getDelegateAnalysisEngineSpecifiersWithImports().put("analyzer", analyzer);

		AnalysisEngine pipeline = UIMAFramework.produceAnalysisEngine(aggregate);
		CasIterator iterator = pipeline.processAndOutputNewCASes(pipeline.newCAS());
		while (iterator.hasNext()) {
			CAS cas = iterator.next();
			System.out.printf("[%s] is [%s]%n", cas.getDocumentText(), cas.getDocumentLanguage());
		}
	}

	/**
	 * Demo of disguising a reader as a CAS multiplier. This works because internally, UIMA wraps
	 * the reader in a CollectionReaderAdapter. This nice thing about this is, that in principle
	 * it would be possible to define sofa mappings. However, UIMA-2419 prevents this.
	 */
	@Test
	public void demoAggregateWithDisguisedReader() throws UIMAException {
		ResourceSpecifierFactory factory = UIMAFramework.getResourceSpecifierFactory();
		
		AnalysisEngineDescription reader = factory.createAnalysisEngineDescription();
		reader.getMetaData().setName("reader");
		reader.setPrimitive(true);
		reader.setImplementationName(SimpleReader.class.getName());
		reader.getAnalysisEngineMetaData().getOperationalProperties().setOutputsNewCASes(true);
	
		AnalysisEngineDescription analyzer = factory.createAnalysisEngineDescription();
		analyzer.getMetaData().setName("analyzer");
		analyzer.setPrimitive(true);
		analyzer.setImplementationName(SimpleAnalyzer.class.getName());
	
		FixedFlow flow = factory.createFixedFlow();
		flow.setFixedFlow(new String[] { "reader", "analyzer" });
	
		AnalysisEngineDescription aggregate = factory.createAnalysisEngineDescription();
		aggregate.getMetaData().setName("aggregate");
		aggregate.setPrimitive(false);
		aggregate.getAnalysisEngineMetaData().setFlowConstraints(flow);
		aggregate.getAnalysisEngineMetaData().getOperationalProperties().setOutputsNewCASes(true);
		aggregate.getAnalysisEngineMetaData().getOperationalProperties()
				.setMultipleDeploymentAllowed(false);
		aggregate.getDelegateAnalysisEngineSpecifiersWithImports().put("reader", reader);
		aggregate.getDelegateAnalysisEngineSpecifiersWithImports().put("analyzer", analyzer);
	
		AnalysisEngine pipeline = UIMAFramework.produceAnalysisEngine(aggregate);
		CasIterator iterator = pipeline.processAndOutputNewCASes(pipeline.newCAS());
		while (iterator.hasNext()) {
			CAS cas = iterator.next();
			System.out.printf("[%s] is [%s]%n", cas.getDocumentText(), cas.getDocumentLanguage());
		}
	}

//	@Test
//	public void testAggregateBuilderWithReaderAndSofaMapping() throws UIMAException {
//		CollectionReaderDescription reader = CollectionReaderFactory.createDescription(TestReader.class);
//		
//		AnalysisEngineDescription readerAed = UIMAFramework.getResourceSpecifierFactory().createAnalysisEngineDescription();
//		readerAed.setAnnotatorImplementationName(reader.getImplementationName());
//		readerAed.setExternalResourceDependencies(reader.getExternalResourceDependencies());
//		readerAed.setFrameworkImplementation(reader.getFrameworkImplementation());
//		readerAed.setImplementationName(reader.getImplementationName());
//		readerAed.setMetaData(reader.getMetaData());
////		readerAed.getAnalysisEngineMetaData().getOperationalProperties().setModifiesCas(true);
////		readerAed.getAnalysisEngineMetaData().getOperationalProperties().setOutputsNewCASes(true);
//		readerAed.setPrimitive(true);
//		readerAed.setResourceManagerConfiguration(reader.getResourceManagerConfiguration());
//		readerAed.setSourceUrl(reader.getSourceUrl());
//		
//		AggregateBuilder builder = new AggregateBuilder();
//		builder.add(readerAed, 
//				ViewNames.INITIAL_VIEW, "A");
//		builder.add(AnalysisEngineFactory.createPrimitiveDescription(Annotator3.class), 
//				ViewNames.INITIAL_VIEW, "A",
//				ViewNames.REVERSE_VIEW, "B");
//		
//		builder.setFlowControllerDescription(createFlowControllerDescription(FixedFlowController.class,
//				FixedFlowController.PARAM_ACTION_AFTER_CAS_MULTIPLIER, "drop"));
//		
//		AnalysisEngineDescription aggregateEngineDesc = builder.createAggregateDescription();
//		aggregateEngineDesc.getAnalysisEngineMetaData().getOperationalProperties().setOutputsNewCASes(true);
//
//		AnalysisEngine aggregateEngine = createAggregate(aggregateEngineDesc);
//		
//		jCas.reset();
//		JCasIterator ji = aggregateEngine.processAndOutputNewCASes(jCas);
//		while (ji.hasNext()) {
//			JCas jc = ji.next();
//			FSIterator<SofaFS> i = jc.getSofaIterator();
//			while (i.hasNext()) {
//				SofaFS s = i.next();
//				System.out.printf("%s - %s%n", s.getSofaID(), jc.getView(s.getSofaID()).getDocumentText());
//			}
//
//			assertEquals("Anyone up for a game of Foosball?", jc.getView("A").getDocumentText());
//			assertEquals("?llabsooF fo emag a rof pu enoynA", jc.getView("B").getDocumentText());
//		}
//	}
	
	public static class SimpleReader extends CollectionReader_ImplBase {
		private boolean done = false;

		public void getNext(CAS aCAS) throws IOException, CollectionException {
			aCAS.setDocumentText("Anyone up for a game of Foosball?");
			done = true;
		}

		public boolean hasNext() throws IOException, CollectionException {
			return !done;
		}

		public Progress[] getProgress() {
			return new Progress[0];
		}

		public void close() throws IOException {
			// Nothing to do
		}
	}

	public static class SimpleAnalyzer extends CasAnnotator_ImplBase {

		@Override
		public void process(CAS aCas) throws AnalysisEngineProcessException {
			aCas.setDocumentLanguage("en");
		}
	}
}
