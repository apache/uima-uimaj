package org.apache.uima.json.json2;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.apache.uima.util.CasCreationUtils.createCas;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;
import java.io.File;
import org.apache.uima.cas.CAS;
import org.apache.uima.json.json2.Json2CasDeserializer;
import org.apache.uima.json.json2.Json2CasSerializer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(value = Parameterized.class)
public class Json2CasDeserializeSerializeTest {
  @Parameters(name = "{index}: running on file {0}")
  public static Iterable<File> tsvFiles() {
    return asList(new File("src/test/resources/Json2SerializerTest/")
            .listFiles(file -> file.isDirectory()));
  }

  private CAS cas;
  private File referenceFolder;
  private File referenceFile;
  private File outputFile;
  private JsonFactory jsonFactory;

  public Json2CasDeserializeSerializeTest(File aFolder) throws Exception {
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
    Json2CasDeserializer deser = new Json2CasDeserializer(jsonFactory.createParser(referenceFile));
    deser.read(cas);

    Json2CasSerializer.builder().write(cas, outputFile);

    assertThat(contentOf(outputFile, UTF_8)).isEqualTo(contentOf(referenceFile, UTF_8));
    // assertEquals(contentOf(referenceFile, UTF_8), contentOf(outputFile, UTF_8), STRICT);
  }
}
