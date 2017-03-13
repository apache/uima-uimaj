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

package org.apache.uima.cas;

/**
 * This class may not be used.
 * 
 * It must contain the same named constants in LowLevelCAS, in the same order, so that
 *    TypeClass.values()[ LowLevelCAS.TYPE_CLASS_XYZ ] returns TYPE_CLASS_XYZ. *
 */
public enum TypeClass {

   TYPE_CLASS_INVALID,           // 0
   TYPE_CLASS_INT,               // 1
   TYPE_CLASS_FLOAT,             // 2
   TYPE_CLASS_STRING,            // 3
   
   TYPE_CLASS_INTARRAY,          // 4 
   TYPE_CLASS_FLOATARRAY,        // 5
   TYPE_CLASS_STRINGARRAY,       // 6
   TYPE_CLASS_FSARRAY,           // 7 
   
   TYPE_CLASS_FS,                // 8 
   TYPE_CLASS_BOOLEAN,           // 9
   TYPE_CLASS_BYTE,              // 10
   TYPE_CLASS_SHORT,             // 11
   TYPE_CLASS_LONG,              // 12
   TYPE_CLASS_DOUBLE,            // 13
   
   TYPE_CLASS_BOOLEANARRAY,      // 14
   TYPE_CLASS_BYTEARRAY,         // 15 
   TYPE_CLASS_SHORTARRAY,        // 16 
   TYPE_CLASS_LONGARRAY,         // 17 
   TYPE_CLASS_DOUBLEARRAY,       // 18 
   TYPE_CLASS_JAVAOBJECT,        // 19 
   TYPE_CLASS_JAVAOBJECTARRAY    // 20
}
