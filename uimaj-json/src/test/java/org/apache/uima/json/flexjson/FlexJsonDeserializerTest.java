package org.apache.uima.json.flexjson;

import static org.apache.uima.util.CasCreationUtils.createCas;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.apache.uima.cas.CAS;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

@Deprecated
public class FlexJsonDeserializerTest {
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
    FlexJsonCasDeserializer deser = new FlexJsonCasDeserializer(jsonFactory
            .createParser(new File("src/test/resources/FlexJsonDeserializer/text_only.json")));

    deser.read(cas);

    assertThat(cas.getDocumentText()).isEqualTo("Hello world.");
  }

  @Test
  public void thatFeatureStructureArrayCanBeParsed() throws Exception {
    FlexJsonCasDeserializer deser = new FlexJsonCasDeserializer(jsonFactory.createParser(
            new File("src/test/resources/FlexJsonDeserializer/feature_structures_only.json")));

    deser.read(cas);

    assertThat(cas.getDocumentText()).isEqualTo("Hello world.");
  }
}
