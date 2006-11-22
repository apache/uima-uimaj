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

package org.apache.uima.collection.impl.metadata.cpe;

import java.util.ArrayList;

import org.apache.uima.collection.metadata.CasProcessorExecArg;
import org.apache.uima.collection.metadata.CasProcessorExecArgs;
import org.apache.uima.collection.metadata.CpeDescriptorException;

public class CasProcessorExecArgsImpl implements CasProcessorExecArgs {
  private static final long serialVersionUID = -719956786158518508L;

  private ArrayList args = new ArrayList();

  public CasProcessorExecArgsImpl() {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CASProcessorExecArgs#add(org.apache.uima.collection.metadata.CASProcessorExecArg)
   */
  public void add(CasProcessorExecArg aArg) {
    args.add(aArg);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CASProcessorExecArgs#get(java.lang.String)
   */
  public CasProcessorExecArg get(int aIndex) throws CpeDescriptorException {
    if (args.size() <= aIndex) {
      return (CasProcessorExecArg) args.get(aIndex);
    }
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CASProcessorExecArgs#getAll()
   */
  public CasProcessorExecArg[] getAll() {
    CasProcessorExecArgImpl[] argArray = new CasProcessorExecArgImpl[args.size()];
    args.toArray(argArray);
    return argArray;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CASProcessorExecArgs#remove(org.apache.uima.collection.metadata.CASProcessorExecArg)
   */
  public void remove(int aIndex) {
    if (args.size() <= aIndex) {
      args.remove(aIndex);
    }
  }

}
