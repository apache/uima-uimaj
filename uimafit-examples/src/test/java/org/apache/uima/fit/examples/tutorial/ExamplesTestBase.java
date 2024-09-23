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
package org.apache.uima.fit.examples.tutorial;

import static org.apache.uima.fit.factory.JCasFactory.createJCas;
import static org.apache.uima.fit.factory.TypePrioritiesFactory.createTypePriorities;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;

import org.apache.uima.fit.examples.tutorial.type.DateAnnotation;
import org.apache.uima.fit.examples.tutorial.type.DateTimeAnnotation;
import org.apache.uima.fit.examples.tutorial.type.Meeting;
import org.apache.uima.fit.examples.tutorial.type.RoomNumber;
import org.apache.uima.fit.examples.tutorial.type.TimeAnnotation;
import org.apache.uima.fit.examples.tutorial.type.UimaAcronym;
import org.apache.uima.fit.examples.tutorial.type.UimaMeeting;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypePriorities;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.junit.jupiter.api.BeforeEach;

public class ExamplesTestBase {
  private static ThreadLocal<JCas> JCAS = new ThreadLocal<JCas>();

  private static ThreadLocal<TypeSystemDescription> TYPE_SYSTEM_DESCRIPTION = new ThreadLocal<TypeSystemDescription>();

  private static ThreadLocal<TypePriorities> TYPE_PRIORITIES = new ThreadLocal<TypePriorities>();

  static {
    try {
      TypeSystemDescription tsd = createTypeSystemDescription(
              "org.apache.uima.fit.examples.TypeSystem");
      TYPE_SYSTEM_DESCRIPTION.set(tsd);

      TypePriorities tp = createTypePriorities(DateAnnotation.class, DateTimeAnnotation.class,
              Meeting.class, RoomNumber.class, TimeAnnotation.class, UimaAcronym.class,
              UimaMeeting.class);
      TYPE_PRIORITIES.set(tp);

      JCas jCas = createJCas(tsd);
      JCAS.set(jCas);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  protected JCas jCas;

  protected TypeSystemDescription typeSystemDescription;

  protected TypePriorities typeSystemPriorities;

  /**
   * we do not want to create a new JCas object every time we run a test because it is expensive
   * (~100ms on my laptop). Instead, we will have one JCas per thread sitting around that we will
   * reset everytime a new test is called.
   */
  @BeforeEach
  public void setUp() {
    jCas = JCAS.get();
    jCas.reset();
    typeSystemDescription = TYPE_SYSTEM_DESCRIPTION.get();
    typeSystemPriorities = TYPE_PRIORITIES.get();
  }
}
