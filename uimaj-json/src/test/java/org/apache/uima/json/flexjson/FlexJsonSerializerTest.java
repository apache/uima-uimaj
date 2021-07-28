package org.apache.uima.json.flexjson;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.uima.UIMAFramework.getResourceSpecifierFactory;
import static org.apache.uima.cas.CAS.TYPE_NAME_ANNOTATION;
import static org.apache.uima.json.flexjson.FlexJsonCasSerializer.ViewsMode.INLINE;
import static org.apache.uima.json.flexjson.FlexJsonCasSerializer.ViewsMode.SEPARATE;
import static org.apache.uima.util.CasCreationUtils.createCas;
import static org.assertj.core.api.Assertions.contentOf;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.skyscreamer.jsonassert.JSONCompareMode.STRICT;

import java.io.File;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.json.flexjson.FlexJsonCasSerializer;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

public class FlexJsonSerializerTest {
  public @Rule TemporaryFolder temp = new TemporaryFolder();
  public @Rule TestName name = new TestName();

  private JsonFactory jsonFactory;

  private File outputFile;
  private File referenceFile;

  @Before
  public void setup() throws Exception {
    jsonFactory = new JsonFactory();
    jsonFactory.setCodec(new ObjectMapper());

    outputFile = new File("target/test-output/" + getClass().getSimpleName() + "/"
            + name.getMethodName() + "/output.json");
    outputFile.getParentFile().mkdirs();

    referenceFile = new File("src/test/resources/" + getClass().getSimpleName() + "/"
            + name.getMethodName() + "/reference.json");
  }

  @Test
  public void multipleViewsAndSofas() throws Exception {
    CAS cas = createCas();
    CAS firstView = cas;
    firstView.setDocumentText("First view");
    CAS secondView = firstView.createView("secondView");
    secondView.setDocumentText("Second view");

    FlexJsonCasSerializer.write(cas, outputFile);

    assertEquals(contentOf(referenceFile, UTF_8), contentOf(outputFile, UTF_8), STRICT);
  }

  @Test
  public void featureStructureIndexedInMultipleViewsInline() throws Exception {
    CAS cas = createCas();
    FeatureStructure fs = cas.createFS(cas.getTypeSystem().getTopType());

    CAS firstView = cas;
    firstView.setDocumentText("First view");
    firstView.addFsToIndexes(fs);

    CAS secondView = cas.createView("secondView");
    secondView.setDocumentText("Second view");
    secondView.addFsToIndexes(fs);

    FlexJsonCasSerializer.builder() //
            .setViewsMode(INLINE) //
            .write(cas, outputFile);

    assertEquals(contentOf(referenceFile, UTF_8), contentOf(outputFile, UTF_8), STRICT);
  }

  @Test
  public void featureStructureIndexedInMultipleViewsSeparate() throws Exception {
    CAS cas = createCas();
    FeatureStructure fs = cas.createFS(cas.getTypeSystem().getTopType());

    CAS firstView = cas;
    firstView.setDocumentText("First view");
    firstView.addFsToIndexes(fs);

    CAS secondView = cas.createView("secondView");
    secondView.setDocumentText("Second view");
    secondView.addFsToIndexes(fs);

    FlexJsonCasSerializer.builder() //
            .setViewsMode(SEPARATE) //
            .write(cas, outputFile);

    assertEquals(contentOf(referenceFile, UTF_8), contentOf(outputFile, UTF_8), STRICT);
  }

  @Test
  public void customAnnotationType() throws Exception {
    String customTypeName = "custom.Annotation";

    TypeSystemDescription tsd = getResourceSpecifierFactory().createTypeSystemDescription();
    TypeDescription customTypeDesc = tsd.addType(customTypeName, "", TYPE_NAME_ANNOTATION);
    customTypeDesc.addFeature("value", "", CAS.TYPE_NAME_STRING);

    CAS cas = createCas(tsd, null, null, null);

    Type customType = cas.getTypeSystem().getType(customTypeName);
    AnnotationFS fs = cas.createAnnotation(customType, 0, 10);
    cas.addFsToIndexes(fs);

    FlexJsonCasSerializer.write(cas, outputFile);

    assertEquals(contentOf(referenceFile, UTF_8), contentOf(outputFile, UTF_8), STRICT);
  }
}
