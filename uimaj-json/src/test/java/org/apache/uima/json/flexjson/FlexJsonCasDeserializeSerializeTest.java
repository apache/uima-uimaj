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
package org.apache.uima.json.flexjson;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.apache.uima.util.CasCreationUtils.createCas;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;

import java.io.File;

import org.apache.uima.cas.CAS;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

@Deprecated
@Ignore("The deserializer is not really implemented...")
@RunWith(value = Parameterized.class)
public class FlexJsonCasDeserializeSerializeTest {
  @Parameters(name = "{index}: running on file {0}")
  public static Iterable<File> tsvFiles() {
    return asList(new File("src/test/resources/FlexJsonSerializerTest/")
            .listFiles(file -> file.isDirectory()));
  }

  private CAS cas;
  private File referenceFolder;
  private File referenceFile;
  private File outputFile;
  private JsonFactory jsonFactory;

  public FlexJsonCasDeserializeSerializeTest(File aFolder) throws Exception {
    referenceFolder = aFolder;
    referenceFile = new File(referenceFolder, "reference.json");
    outputFile = new File("target/test-output/" + getClass().getSimpleName() + "/"
            + referenceFolder.getName() + "/output.json");
    outputFile.getParentFile().mkdirs();

    cas = createCas();

    jsonFactory = new JsonFactory();
    jsonFactory.setCodec(new ObjectMapper());
  }

  @Test
  public void testReadWrite() throws Exception {
    FlexJsonCasDeserializer deser = new FlexJsonCasDeserializer(
            jsonFactory.createParser(referenceFile));
    deser.read(cas);

    FlexJsonCasSerializer.builder().write(cas, outputFile);

    assertThat(contentOf(outputFile, UTF_8)).isEqualTo(contentOf(referenceFile, UTF_8));
    // assertEquals(contentOf(referenceFile, UTF_8), contentOf(outputFile, UTF_8), STRICT);
  }
}
