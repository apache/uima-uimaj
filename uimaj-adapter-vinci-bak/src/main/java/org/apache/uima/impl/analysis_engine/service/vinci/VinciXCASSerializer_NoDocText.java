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

package org.apache.uima.impl.analysis_engine.service.vinci;

import java.io.IOException;

import org.xml.sax.SAXException;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.OutOfTypeSystemData;
import org.apache.uima.cas.impl.XCASSerializer;
import org.apache.uima.cas.text.TCAS;
import org.apache.uima.impl.analysis_engine.service.vinci.util.Constants;
import org.apache.uima.impl.analysis_engine.service.vinci.util.SaxVinciFrameBuilder;
import org.apache.uima.impl.analysis_engine.service.vinci.util.UimaSaxVinciFrameBuilder;
import org.apache.vinci.transport.document.AFrame;

/**
 * A Vinci CAS Serializer for the XCAS spec which does NOT include the
 * full text of the document.  However, for each annotation it DOES include
 * that annotation's spanned text as the content of its Vinci FrameLeaf.
 * 
 * 
 */
public class VinciXCASSerializer_NoDocText implements VinciCASSerializer
{
  
      

  /**
   * @see org.apache.uima.impl.analysis_engine.service.vinci.VinciCASSerializer#serialize(org.apache.uima.cas.text.TCAS, vinci.transport.VinciFrame,OutOfTypeSystemData)
   */
  public void serialize(CAS aCAS, AFrame aParentFrame, OutOfTypeSystemData aOutOfTypeSystemData)
    throws IOException, SAXException
  {
    //Serialize CAS to XCAS
    //Would be nice to serialize straight to parent frame frame, but we have
    //to change the tag name to KEYS to satisfy the TAE interface
    //spec - sigh.
    SaxVinciFrameBuilder vinciFrameBuilder;
    AFrame xcasHolder = new AFrame();
    XCASSerializer xcasSerializer = 
      new XCASSerializer(aCAS.getTypeSystem());
	if (((CASImpl)aCAS).isBackwardCompatibleCas()) {
		String docText = aCAS.getView(CAS.NAME_DEFAULT_SOFA).getDocumentText();
		vinciFrameBuilder = 
			new UimaSaxVinciFrameBuilder(true,true,docText);
		}
	else {
		vinciFrameBuilder = 
			new UimaSaxVinciFrameBuilder(true,true,null);
	}
    vinciFrameBuilder.setParentFrame(xcasHolder);  
    xcasSerializer.serialize(aCAS, vinciFrameBuilder, false, aOutOfTypeSystemData);
    AFrame xcasFrame = xcasHolder.fgetAFrame("CAS");
    aParentFrame.aadd(Constants.KEYS, xcasFrame);
  }

}
