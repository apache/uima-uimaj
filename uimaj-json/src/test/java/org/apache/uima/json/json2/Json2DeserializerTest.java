package org.apache.uima.json.json2;

import static org.apache.uima.util.CasCreationUtils.createCas;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.apache.uima.cas.CAS;
import org.apache.uima.json.json2.Json2CasDeserializer;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Json2DeserializerTest {
  private static CAS cas;

  private JsonFactory jsonFactory;

  @BeforeClass
  public static void setupOnce() throws Exception {
    cas = createCas();
  }

  @Before
  public void setup() throws Exception {
    jsonFactory = new JsonFactory();
    jsonFactory.setCodec(new ObjectMapper());
  }

  @Test
  public void thatQuotedStringCanBeParsed() throws Exception {
    Json2CasDeserializer deser = new Json2CasDeserializer(jsonFactory
            .createParser(new File("src/test/resources/Json2Deserializer/text_only.json")));

    deser.read(cas);

    assertThat(cas.getDocumentText()).isEqualTo("Hello world.");
  }

  @Test
  public void thatFeatureStructureArrayCanBeParsed() throws Exception {
    Json2CasDeserializer deser = new Json2CasDeserializer(jsonFactory.createParser(
            new File("src/test/resources/Json2Deserializer/feature_structures_only.json")));

    deser.read(cas);

    assertThat(cas.getDocumentText()).isEqualTo("Hello world.");
  }
}
