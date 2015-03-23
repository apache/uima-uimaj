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
package org.apache.uima.impl;

import org.apache.uima.cas.AbstractCas;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.ComponentInfo;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.impl.CasManager_impl;

/**
 * Shared code refactored from other spots to reduce duplication
 * and improve maintainability
 *
 */
public class Util {
  
  public static CASImpl getStartingView(CAS cas, boolean sofaAware, ComponentInfo componentInfo) {
    // OLD behavior:
      // if this is a sofa-aware component, give it the Base CAS
      // if it is a sofa-unaware component, give it whatever view maps to the _InitialView
    // NEW behavior:
      // always return whatever view maps to the _InitialView
    CASImpl ci;
    // need to set the componentInfo for the getView to find the sofa mappings
    // Do this *before* the getView call below
    // note: this is in a shared view part of the CAS
    cas.setCurrentComponentInfo(componentInfo);  
//    if (sofaAware) {
//      ci = ((CASImpl) cas).getBaseCAS();
//    } else {
//      ci = (CASImpl) cas.getView(CAS.NAME_DEFAULT_SOFA);
//    }
    ci = (CASImpl) cas.getView(CAS.NAME_DEFAULT_SOFA);
    
    return ci;
  }
  
  public static AbstractCas setupViewSwitchClassLoadersLockCas(
      CAS cas, 
      boolean sofaAware, 
      ComponentInfo componentInfo,
      ResourceManager resourceManager, 
      Class<? extends AbstractCas> casInterface) {
    CASImpl ci = getStartingView(cas, sofaAware, componentInfo);
    // get requested interface to CAS (CAS or JCas)
    // next will create JCas if needed, but not already created
    // must precede the switchClassLoader call - that one needs the JCas link, if it is being used
    AbstractCas r = CasManager_impl.getCasInterfaceStatic(ci, casInterface);
    // This cas will be unlocked and its class loader restored when the
    //   next() method returns it
    // Insure the same view is passed for switching/restoring  https://issues.apache.org/jira/browse/UIMA-2211
    ci.switchClassLoaderLockCasCL(resourceManager.getExtensionClassLoader());    
    return r;
  }

  public static <T extends AbstractCas> T setupViewSwitchClassLoaders(
      CAS cas, 
      boolean sofaAware, 
      ComponentInfo componentInfo,
      ResourceManager resourceManager, 
      Class<T> casInterface) {
    CASImpl ci = getStartingView(cas, sofaAware, componentInfo);
    // get requested interface to CAS (CAS or JCas)
    // next will create JCas if needed, but not already created
    // must precede the switchClassLoader call - that one needs the JCas link, if it is being used
    T r = CasManager_impl.<T>getCasInterfaceStatic(ci, casInterface);
    // This cas will be unlocked and its class loader restored when the
    //   next() method returns it
    // Insure the same view is passed for switching/restoring  https://issues.apache.org/jira/browse/UIMA-2211
    ci.switchClassLoader(resourceManager.getExtensionClassLoader());    
    return r;
  }
  

}
