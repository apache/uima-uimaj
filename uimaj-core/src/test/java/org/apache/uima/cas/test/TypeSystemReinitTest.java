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

package org.apache.uima.cas.test;

import java.util.ArrayList;

import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.admin.CASMgr;
import org.apache.uima.cas.impl.CASCompleteSerializer;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.Serialization;
import org.apache.uima.cas.impl.TCASImpl;
import org.apache.uima.cas.text.TCAS;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.resource.metadata.impl.TypeSystemDescription_impl;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.FileSystemCollectionReader;
import org.apache.uima.util.XMLInputSource;

/**
 * @author alally
 */
public class TypeSystemReinitTest extends TestCase 
{
  public void testReinitCASCompleteSerializer() throws Exception
  {
    try
    {
	    AnalysisEngineDescription aed =
	      UIMAFramework.getXMLParser().parseAnalysisEngineDescription(
	        new XMLInputSource(JUnitExtension.getFile("TextAnalysisEngineImplTest/TestPrimitiveTae1.xml")));
	   CollectionReaderDescription crd =  FileSystemCollectionReader.getDescription();
	   
	   ArrayList l = new ArrayList();
	   l.add(aed);
	   l.add(crd);
	   TCAS cas1 = CasCreationUtils.createTCas(l);
	   cas1.setDocumentText("foo");
	   CASCompleteSerializer ser = Serialization.serializeCASComplete((CASMgr)cas1);
	   
	   TCAS tcas2 = CasCreationUtils.createTCas(new TypeSystemDescription_impl(), null, null);
	   CASImpl cas2 = ((TCASImpl)tcas2).getBaseCAS();
	   tcas2.setDocumentText("bar");
	       
	   //reinit
	   cas2.reinit(ser);
	   TCAS tcas3 = cas2.getTCAS();

	   assertTrue(tcas2 == tcas3);
	   assertNotNull(cas1.getTypeSystem().getType("NamedEntity"));
	   assertNotNull(tcas3.getTypeSystem().getType("NamedEntity"));
	   
	   FeatureStructure fs = tcas3.createFS(tcas3.getTypeSystem().getType("NamedEntity"));
	   tcas3.getIndexRepository().addFS(fs);
    }
    catch(Exception e)
    {
      JUnitExtension.handleException(e);
    }
  }
}
