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

package org.apache.uima.jcas.test;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.admin.CASFactory;
import org.apache.uima.cas.admin.CASMgr;
import org.apache.uima.cas.admin.FSIndexRepositoryMgr;
import org.apache.uima.cas.admin.TypeSystemMgr;
import org.apache.uima.cas.impl.CASImpl;

/**
 * Use this as your CAS factory.
 */
public class CASInitializer {

  public static CAS initCas(AnnotatorInitializer init) throws CASException {
    // Create an initial CASMgr from the factory.
    CASMgr casMgr = CASFactory.createCAS();
    // Create a writable type system.
    TypeSystemMgr tsa = casMgr.getTypeSystemMgr();
    // assert(tsa != null);
    // Create a CASMgr. Ensures existence of AnnotationFS type.
    init.initTypeSystem(tsa);
    // Commit the type system.
    ((CASImpl) casMgr).commitTypeSystem();
    // assert(tsa.isCommitted());
    // Create the CAS indexes.
    casMgr.initCASIndexes();

    // tcasMgr.initCASIndexes();
    FSIndexRepositoryMgr irm = casMgr.getIndexRepositoryMgr();
    init.initIndexes(irm, casMgr.getTypeSystemMgr());
    // Commit the index repository.
    irm.commit();
    // assert(cas.getIndexRepositoryMgr().isCommitted());

    return casMgr.getCAS().getCurrentView();
  }

}
