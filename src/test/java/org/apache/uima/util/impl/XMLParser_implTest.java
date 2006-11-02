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

package org.apache.uima.util.impl;

import java.io.File;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.TaeDescription;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLParser;
import org.apache.uima.util.XMLizable;

/**
 * @author alally
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class XMLParser_implTest extends TestCase
{

  /**
   * Constructor for XMLParser_implTest.
   * @param arg0
   */
  public XMLParser_implTest(String arg0)
  {
    super(arg0);
  }

  /*
   * @see TestCase#setUp()
   */
  protected void setUp() throws Exception
  {
    super.setUp();
    mXmlParser = UIMAFramework.getXMLParser();
    
    //Enable schema validation.  Note that this will enable schema validation
    //for tests that run after this too, but that's not so bad since we'd like
    //to test the schema.  This is currently the first test in CoreTests, so
    //schema validation will be enabled for the whole suite.
    mXmlParser.enableSchemaValidation(true);
  }

  public void testParse() throws Exception
  {
    try
    {
      //JTalentAndStringMatch.xml contains xincludes, 
      //JTalentAndStringMatch_Expanded.xml has had them manually expanded
      File withXIncludes = JUnitExtension.getFile("XmlParserTest/JTalentAndStringMatch.xml");
      File manuallyExpanded = JUnitExtension.getFile("XmlParserTest/JTalentAndStringMatch_Expanded.xml");
      
      //After passing JTalentAndStringMatch.xml through processXIncludes,
      //we should then be able to parse both files and get identical results.
      XMLizable obj1 = mXmlParser.parse(new XMLInputSource(withXIncludes));
      XMLizable obj2 = mXmlParser.parse(new XMLInputSource(manuallyExpanded));
      Assert.assertNotNull(obj1);    
      Assert.assertNotNull(obj2);    
      Assert.assertEquals(obj1,obj2); 
      
      //try similar test for XInclude containing xpointer
      withXIncludes = JUnitExtension.getFile("XmlParserTest/XIncludeXPointerTest.xml");
      manuallyExpanded = JUnitExtension.getFile("XmlParserTest/XIncludeXPointerTest_Expanded.xml");
      
      //After passing JTalentAndStringMatch.xml through processXIncludes,
      //we should then be able to parse both files and get identical results.
      obj1 = mXmlParser.parse(new XMLInputSource(withXIncludes));
      obj2 = mXmlParser.parse(new XMLInputSource(manuallyExpanded));
      Assert.assertNotNull(obj1); 
      //obj1.toXML(System.out);
      Assert.assertNotNull(obj2);    
      Assert.assertEquals(obj1,obj2); 
    }
    catch (Exception e)
    {
			JUnitExtension.handleException(e);
    }
    finally
    {
    	mXmlParser = null;
    }
  }   
  
  public void testParseXMLInputSourceParseOptions() throws Exception
  { 
		try
		{
			//test for env var refs
			File envVarRefTest = JUnitExtension.getFile("XmlParserTest/EnvVarRefTest.xml");
			System.setProperty("uima.test.var1","foo");
			System.setProperty("uima.test.var2","bar");
			TaeDescription taeDesc = UIMAFramework.getXMLParser().parseTaeDescription(
			  new XMLInputSource(envVarRefTest), new XMLParser.ParsingOptions(true,true));
      Assert.assertEquals("foo-bar",taeDesc.getMetaData().getName());
      
      //parse with env var ref expansion disabled
			taeDesc = UIMAFramework.getXMLParser().parseTaeDescription(
				new XMLInputSource(envVarRefTest), new XMLParser.ParsingOptions(true,false));
			Assert.assertEquals("<envVarRef>uima.test.var1</envVarRef>-<envVarRef>uima.test.var2</envVarRef>",
			                    taeDesc.getMetaData().getName());
			                  
            
		}
		catch (Exception e)
		{
			JUnitExtension.handleException(e);
		}
		finally
		{
			mXmlParser = null;
		}
  }
  
  private XMLParser mXmlParser;
}
