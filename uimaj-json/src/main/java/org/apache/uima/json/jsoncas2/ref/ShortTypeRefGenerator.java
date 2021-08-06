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
package org.apache.uima.json.jsoncas2.ref;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import org.apache.uima.cas.Type;

public class ShortTypeRefGenerator implements Function<Type, String> {
  private Set<String> usedNames = new HashSet<>();

  @Override
  public String apply(Type aType) {
    if (!usedNames.contains(aType.getShortName())) {
      usedNames.add(aType.getShortName());
      return aType.getShortName();
    }

    int n = 1;
    String newName;
    while (usedNames.contains(newName = aType.getShortName() + "-" + n)) {
      n++;
    }

    usedNames.add(newName);
    return newName;
  }
}
