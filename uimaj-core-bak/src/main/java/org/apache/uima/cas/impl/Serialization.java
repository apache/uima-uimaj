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

package org.apache.uima.cas.impl;

import java.io.InputStream;
import java.io.OutputStream;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.admin.CASMgr;
import org.apache.uima.cas.text.TCAS;
import org.apache.uima.cas.text.TCASMgr;

public class Serialization {

    public static CASSerializer serializeCAS(CAS cas) {
        CASSerializer ser = new CASSerializer();
        ser.addCAS((CASImpl) cas);
        return ser;
    }

    public static CASSerializer serializeNoMetaData(CAS cas) {
        CASSerializer ser = new CASSerializer();
        ser.addNoMetaData((CASImpl) cas);
        return ser;
    }

    public static CASMgrSerializer serializeCASMgr(CASMgr casMgr) {
        CASMgrSerializer ser = new CASMgrSerializer();
        ser.addTypeSystem((TypeSystemImpl) casMgr.getCAS().getTypeSystem());
        ser.addIndexRepository((FSIndexRepositoryImpl) ((CASImpl) casMgr.getCAS()).getBaseIndexRepository());
        return ser;
    }

    public static CASCompleteSerializer serializeCASComplete(CASMgr casMgr) {
        return new CASCompleteSerializer((CASImpl) casMgr);
    }

    public static void deserializeCASComplete(CASCompleteSerializer casCompSer,
            CASMgr casMgr) {
        ((CASImpl) casMgr).reinit(casCompSer);
    }

    public static void deserializeTCASComplete(
            CASCompleteSerializer casCompSer, CASMgr casMgr) {
        ((TCASImpl) casMgr).reinit(casCompSer);
    }

    public static CASMgr createCASMgr(CASMgrSerializer ser) {
        return new CASImpl(ser);
    }

//    public static TCASMgr createTCASMgr(CASMgrSerializer ser) {
//        return new TCASImpl(ser);
//    }

    public static CAS createCAS(CASMgr casMgr, CASSerializer casSer) {
        ((CASImpl) casMgr).reinit(casSer);
        return casMgr.getCAS();
    }

    public static TCAS createTCAS(TCASMgr tcasMgr, CASSerializer casSer) {
        ((TCASImpl) tcasMgr).reinit(casSer);
        return tcasMgr.getTCAS();
    }

    public static void serializeCAS(CAS cas, OutputStream ostream) {
        CASSerializer ser = new CASSerializer();
        ser.addCAS((CASImpl) cas, ostream);        
    }
    
    public static void deserializeCAS(CAS cas, InputStream istream) {
        ((CASImpl) cas).reinit(istream);        
    }

    
    
}
