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
package org.apache.uima.pear.tools;

import org.apache.uima.InternationalizedRuntimeException;



public class PearInstallerException extends InternationalizedRuntimeException 
{
   private static final long serialVersionUID = 6261840563059646801L;

   public PearInstallerException(String aResourceBundleName, String aMessageKey, Object[] aArguments) {
     super(aResourceBundleName, aMessageKey, aArguments, null);
   }
   
   public PearInstallerException(String aResourceBundleName, String aMessageKey) {
     super(aResourceBundleName, aMessageKey, null, null);
   }

   public PearInstallerException(String aResourceBundleName, String aMessageKey, Throwable aCause) {
     super(aResourceBundleName, aMessageKey, null, aCause);
   }

   public PearInstallerException(String aResourceBundleName, String aMessageKey, Object[] aArguments, Throwable aCause) {
     super(aResourceBundleName, aMessageKey, aArguments, aCause);
   }

}
