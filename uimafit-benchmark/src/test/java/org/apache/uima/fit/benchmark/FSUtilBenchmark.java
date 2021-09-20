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
package org.apache.uima.fit.benchmark;

import static org.apache.uima.fit.util.FSUtil.setFeature;

import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.type.Token;
import org.apache.uima.jcas.JCas;
import org.junit.BeforeClass;
import org.junit.Test;

public class FSUtilBenchmark {
	private static JCas jcas;
	private static Token fs;

	@BeforeClass
  public static void setupOnce() throws Exception {
  	jcas = JCasFactory.createText("test");
  	fs = new Token(jcas, 0, 1);
  	fs.addToIndexes();
  }

	@Test
  public void benchmarkSetFeature() {
    Benchmark template = new Benchmark("TEMPLATE")
      .timer(System::nanoTime, t -> t / 1000)
      .repeat(1_000_000);

    new Benchmark("set feature string JCas", template)
      .measure(() -> fs.setPos("NN"))
      .run();

    new Benchmark("set feature string", template)
      .measure(() -> setFeature(fs, "pos", "NN"))
      .run();
  }
}
