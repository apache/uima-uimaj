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
package org.apache.uima.json.jsoncas2.model;

import static java.util.Collections.sort;
import static java.util.Comparator.comparing;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.uima.cas.CAS;

public class Views implements Iterable<CAS> {
  private final List<CAS> views;

  public Views(CAS aCas) {
    views = new ArrayList<>();
    aCas.getViewIterator().forEachRemaining(views::add);
    sort(views, comparing(CAS::getViewName));
  }

  @Override
  public Iterator<CAS> iterator() {
    return views.iterator();
  }

  public boolean isEmpty() {
    return views.isEmpty();
  }
}
