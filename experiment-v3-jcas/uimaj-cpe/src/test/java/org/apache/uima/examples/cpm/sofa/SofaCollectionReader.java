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

package org.apache.uima.examples.cpm.sofa;

import java.io.IOException;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.SofaFS;
import org.apache.uima.cas.SofaID;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.impl.SofaID_impl;
import org.apache.uima.util.Progress;

/*
 * Created on Aug 11, 2004
 * 
 * To change the template for this generated file go to Window&gt;Preferences&gt;Java&gt;Code
 * Generation&gt;Code and Comments
 */

/**
 * Creates a Text SofA in the cas. 
 */
public class SofaCollectionReader extends CollectionReader_ImplBase {
  boolean hasMore = true;

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.CollectionReader#getNext(org.apache.uima.cas.CAS)
   */
  public void getNext(CAS aCAS) throws IOException, CollectionException {
    String text = "this beer is good";
    try {
      this.getCasInitializer().initializeCas(text, aCAS);
    } catch (NullPointerException e) {
      // Create the Source text Sofa
      SofaID sofaid = getUimaContext().mapToSofaID("InputText");
      // System.out.println("COLLECTIONREADER: real sofa name for InputText " + sofaid.getSofaID());

      SofaFS ls = aCAS.createSofa(sofaid, "text");
      ls.setLocalSofaData(text);
    }
    hasMore = false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.base_cpm.BaseCollectionReader#hasNext()
   */
  public boolean hasNext() throws IOException, CollectionException {
    return hasMore;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.base_cpm.BaseCollectionReader#getProgress()
   */
  public Progress[] getProgress() {
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.base_cpm.BaseCollectionReader#close()
   */
  public void close() throws IOException {

  }

}
