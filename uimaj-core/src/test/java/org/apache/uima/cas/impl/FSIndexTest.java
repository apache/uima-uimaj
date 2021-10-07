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

import static org.apache.uima.UIMAFramework.getResourceSpecifierFactory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.junit.Test;

public class FSIndexTest {

//  static {
//    System.setProperty(TypeSystemImpl.ENABLE_STRICT_TYPE_SOURCE_CHECK, "true");
//  }
  
  @Test
  public void thatTypeSystemChangesCanBeHandled() throws Exception {

    String myTypeName = "my.MyType";

    TypeSystemDescription tsd1 = getResourceSpecifierFactory().createTypeSystemDescription();
    tsd1.addType(myTypeName, "", CAS.TYPE_NAME_ANNOTATION);
    tsd1.addType("some.OtherType1", "", CAS.TYPE_NAME_ANNOTATION);

    TypeSystemDescription tsd2 = getResourceSpecifierFactory().createTypeSystemDescription();
    tsd2.addType(myTypeName, "", CAS.TYPE_NAME_ANNOTATION);
    tsd2.addType("some.OtherType2", "", CAS.TYPE_NAME_ANNOTATION);

    CAS cas1 = CasCreationUtils.createCas(tsd1, null, null, null);
    CAS cas2 = CasCreationUtils.createCas(tsd2, null, null, null);

    assertThat(cas1.getTypeSystem())
            .as("There exists two CASes in the JVM which have different type systems "
                    + "containing the same internal and exteral types")
            .isNotEqualTo(cas2.getTypeSystem());

    assertThat(cas1.getTypeSystem().getType(myTypeName))
            .as("The internal type is the same in both type systems")
            .isEqualTo(cas2.getTypeSystem().getType(myTypeName));

    Type myTypeFromCas2 = getType(cas2, myTypeName);

    if (TypeSystemImpl.IS_ENABLE_STRICT_TYPE_SOURCE_CHECK) {
      assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> {
        AnnotationFS brokenFs = cas1.createAnnotation(myTypeFromCas2, 0, 0);
        cas1.addFsToIndexes(brokenFs);
      }).withMessageContaining("in CAS with different type system");
    }
    else {
      List<String> capturedOutput = captureOutput(() -> {
        AnnotationFS brokenFs = cas1.createAnnotation(myTypeFromCas2, 0, 0);
        cas1.addFsToIndexes(brokenFs);
        return null;
      });

      assertThat(capturedOutput.get(0)).contains("in CAS with different type system");
      assertThat(capturedOutput.get(1)).contains("on index using different type system");
    }
  }

  private List<String> captureOutput(Callable<Void> code) throws Exception {
    org.apache.logging.log4j.core.Logger rootLogger = (org.apache.logging.log4j.core.Logger) org.apache.logging.log4j.LogManager
            .getRootLogger();

    List<String> messages = new ArrayList<>();

    Filter filter = new AbstractFilter() {
      @Override
      public Result filter(LogEvent event) {
        messages.add(event.getMessage().getFormattedMessage());
        return Result.ACCEPT;
      }
    };

    ConsoleAppender app = (ConsoleAppender) rootLogger.get().getAppenders().values().stream()
            .findFirst().get();
    app.addFilter(filter);

    try {
      code.call();
      return messages;
    } finally {
      app.removeFilter(filter);
    }
  }

  private Type getType(CAS cas, String typename) {
    return cas.getTypeSystem().getType(typename);
  }
}
