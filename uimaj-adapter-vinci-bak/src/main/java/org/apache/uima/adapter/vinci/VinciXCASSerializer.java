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

package org.apache.uima.adapter.vinci;

import java.io.IOException;

import org.xml.sax.SAXException;

import org.apache.uima.adapter.vinci.util.Constants;
import org.apache.uima.adapter.vinci.util.SaxVinciFrameBuilder;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.OutOfTypeSystemData;
import org.apache.uima.cas.impl.XCASSerializer;
import org.apache.vinci.transport.document.AFrame;

/**
 * A Vinci CAS Serializer for the XCAS spec.
 * 
 * 
 */
public class VinciXCASSerializer implements VinciCASSerializer
{
  
      

  /**
   * @see org.apache.uima.adapter.vinci.VinciCASSerializer#serialize(org.apache.uima.cas.text.TCAS, vinci.transport.VinciFrame,OutOfTypeSystemData)
   */
  public void serialize(CAS aCAS, AFrame aParentFrame, OutOfTypeSystemData aOutOfTypeSystemData)
    throws IOException, SAXException
  {
    //Serialize CAS to XCAS
    //Would be nice to serialize straight to parent frame frame, but we have
    //to change the tag name to KEYS to satisfy the TAE interface
    //spec - sigh.
    AFrame xcasHolder = new AFrame();
    XCASSerializer xcasSerializer = 
      new XCASSerializer(aCAS.getTypeSystem());
    SaxVinciFrameBuilder vinciFrameBuilder = new SaxVinciFrameBuilder();
    vinciFrameBuilder.setParentFrame(xcasHolder);  
    xcasSerializer.serialize(aCAS, vinciFrameBuilder, true, aOutOfTypeSystemData);
    AFrame xcasFrame = xcasHolder.fgetAFrame("CAS");
    aParentFrame.aadd(Constants.KEYS, xcasFrame);
  }

}
