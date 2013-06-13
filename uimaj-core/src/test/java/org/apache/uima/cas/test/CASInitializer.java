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

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.admin.CASFactory;
import org.apache.uima.cas.admin.CASMgr;
import org.apache.uima.cas.admin.FSIndexRepositoryMgr;
import org.apache.uima.cas.admin.TypeSystemMgr;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;

/**
 * Use this as your CAS factory.
 */
public class CASInitializer {

  public static CAS initCas(AnnotatorInitializer init) {
    // Create an initial CASMgr from the factory.
    CASMgr casMgr0 = CASFactory.createCAS();
    CASMgr casMgr = null;
    try {
      // this call does nothing: because 2nd arg is null
      CasCreationUtils.setupTypeSystem(casMgr0, (TypeSystemDescription) null);
      // Create a writable type system.
      TypeSystemMgr tsa = casMgr0.getTypeSystemMgr();
      // Next not needed, type system is already uncommitted
//      ((TypeSystemImpl) tsa).setCommitted(false);
      // do the type system tests
      init.initTypeSystem(tsa);
      // Commit the type system.
      ((CASImpl) casMgr0).commitTypeSystem();

      casMgr = CASFactory.createCAS(tsa);

      // Create the Base indexes.
      casMgr.initCASIndexes();
      // Commit the index repository.
      FSIndexRepositoryMgr irm = casMgr.getIndexRepositoryMgr();
      init.initIndexes(irm, casMgr.getTypeSystemMgr());
      irm.commit();
    } catch (ResourceInitializationException e) {
      throw new RuntimeException(e);
    } catch (CASException e) {
      throw new RuntimeException(e);
    }

    // Create the default text Sofa and return CAS view
    return casMgr.getCAS().getCurrentView();
  }

}
