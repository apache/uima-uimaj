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
package org.apache.uima.fit.factory;

import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.ExternalResourceFactory.bindResource;
import static org.apache.uima.fit.factory.ExternalResourceFactory.bindResourceOnceWithoutNested;
import static org.apache.uima.fit.factory.ExternalResourceFactory.createNamedResourceDescription;
import static org.apache.uima.fit.factory.ExternalResourceFactory.createResourceDescription;
import static org.apache.uima.fit.factory.ExternalResourceFactory.createSharedResourceDescription;
import static org.apache.uima.fit.factory.JCasFactory.createJCas;
import static org.apache.uima.fit.pipeline.SimplePipeline.runPipeline;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import javax.naming.InitialContext;

import org.apache.uima.ResourceSpecifierFactory;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.ComponentTestBase;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.component.Resource_ImplBase;
import org.apache.uima.fit.component.initialize.ConfigurationParameterInitializer;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.fit.factory.locator.JndiResourceLocator;
import org.apache.uima.fit.internal.ResourceManagerFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.type.AnalyzedText;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.fit.util.SimpleNamedResourceManager;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.ParameterizedDataResource;
import org.apache.uima.resource.ResourceAccessException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.SharedResourceObject;
import org.apache.uima.util.CasCreationUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ExternalResourceFactoryTest extends ComponentTestBase {
  // https://issues.apache.org/jira/browse/UIMA-5555
  // private static final String EX_URI = "http://dum.my";

  private static final String EX_FILE_1 = "src/test/resources/data/docs/test.xcas";

  private static final String EX_FILE_3 = "src/test/resources/data/docs/test.xmi";

  @BeforeAll
  public static void initJNDI() throws Exception {
    var deDict = new Properties();
    deDict.setProperty("Hans", "proper noun");

    var ctx = new InitialContext();
    ctx.rebind("dictionaries/german", deDict);
  }

  /**
   * This is a test for a regression introduced in the UIMA SDK 2.6.0 RC 1 and which worked in UIMA
   * SDK 2.5.0.
   * <ul>
   * <li>Add a full delegate description to an AAE.</li>
   * <li>Serialize to XML: delegate description is serialized.</li>
   * <li>Call resolveImports()</li>
   * <li>Serialize to XML: delegate description is no longer serialized.</li>
   * </ul>
   * 
   * @see <a href="https://issues.apache.org/jira/browse/UIMA-3776">UIMA-3776</a>
   */
  @Test
  void testNoDelegatesToResolve() throws Exception {
    ResourceSpecifierFactory f = UIMAFramework.getResourceSpecifierFactory();
    AnalysisEngineDescription outer = f.createAnalysisEngineDescription();
    AnalysisEngineDescription inner = f.createAnalysisEngineDescription();
    outer.getDelegateAnalysisEngineSpecifiersWithImports().put("inner", inner);

    StringWriter outerXml = new StringWriter();
    outer.toXML(outerXml);

    // Resolving the imports removes the inner AE description
    outer.resolveImports(ResourceManagerFactory.newResourceManager());

    StringWriter outerXml2 = new StringWriter();
    outer.toXML(outerXml2);

    assertThat(outerXml.toString()).isEqualTo(outerXml2.toString());
  }

  /**
   * Illustrate two different paths of accessing a resource instance.
   * 
   * @see <a href="http://markmail.org/thread/tdd24gdbtoa3hje2">Apache UIMA user mailing list</a>
   */
  @Test
  void testAccessResourceFromAE() throws Exception {
    AnalysisEngine ae = createEngine(DummyAE3.class, DummyAE3.RES_KEY_1,
            createNamedResourceDescription("lala", AnnotatedResource.class,
                    AnnotatedResource.PARAM_VALUE, "1"));

    // Two difference paths to access the resource
    Object resourceInstance1 = ae.getUimaContext().getResourceObject(DummyAE3.RES_KEY_1);
    Object resourceInstance2 = ae.getResourceManager()
            .getResource(ae.getUimaContextAdmin().getQualifiedContextName() + DummyAE3.RES_KEY_1);

    assertEquals(resourceInstance1, resourceInstance2);
  }

  @Test
  void testScanBind() throws Exception {
    // Create analysis enginge description
    AnalysisEngineDescription desc = createEngineDescription(DummyAE.class);

    // Bind external resources
    bindResources(desc);

    // Test with the default resource manager implementation
    AnalysisEngine ae = UIMAFramework.produceAnalysisEngine(desc);
    assertNotNull(ae);
  }

  @Test
  void testDirectInjection() throws Exception {
    // Create analysis enginge description
    AnalysisEngineDescription desc = createEngineDescription(DummyAE2.class);

    // Bind external resources for DummyAE
    bindResources(desc);

    // Bind external resources for DummyAE2 - necessary because autowiring is disabled
    bindResourceOnceWithoutNested(desc, DummyAE2.RES_INJECTED_POJO1, "pojoName1");
    bindResourceOnceWithoutNested(desc, DummyAE2.RES_INJECTED_POJO2, "pojoName2");

    // Create a custom resource manager that allows to inject any Java object as an external
    // dependency
    final Map<String, Object> externalContext = new HashMap<String, Object>();
    externalContext.put("pojoName1", "Just an injected POJO");
    externalContext.put("pojoName2", new AtomicInteger(5));

    SimpleNamedResourceManager resMgr = new SimpleNamedResourceManager();
    resMgr.setExternalContext(externalContext);
    assertFalse(resMgr.isAutoWireEnabled());

    AnalysisEngine ae = UIMAFramework.produceAnalysisEngine(desc, resMgr, null);
    assertNotNull(ae);

    ae.process(ae.newJCas());
  }

  @Test
  void testDirectInjectionAutowire() throws Exception {
    // Create analysis engine description
    AnalysisEngineDescription desc = createEngineDescription(DummyAE2.class);

    // Bind external resources for DummyAE
    bindResources(desc);

    // Create a custom resource manager that allows to inject any Java object as an external
    // dependency
    final Map<String, Object> externalContext = new HashMap<String, Object>();
    externalContext.put(DummyAE2.RES_INJECTED_POJO1, "Just an injected POJO");
    externalContext.put(DummyAE2.RES_INJECTED_POJO2, new AtomicInteger(5));

    SimpleNamedResourceManager resMgr = new SimpleNamedResourceManager();
    resMgr.setExternalContext(externalContext);
    resMgr.setAutoWireEnabled(true);
    assertTrue(resMgr.isAutoWireEnabled());

    AnalysisEngine ae = UIMAFramework.produceAnalysisEngine(desc, resMgr, null);
    assertNotNull(ae);

    ae.process(ae.newJCas());
  }

  @Test
  void testMultiBinding() throws Exception {
    ExternalResourceDescription extDesc = createResourceDescription(ResourceWithAssert.class);

    // Binding external resource to each Annotator individually
    AnalysisEngineDescription aed1 = createEngineDescription(MultiBindAE.class, MultiBindAE.RES_KEY,
            extDesc);
    AnalysisEngineDescription aed2 = createEngineDescription(MultiBindAE.class, MultiBindAE.RES_KEY,
            extDesc);

    // Check the external resource was injected
    MultiBindAE.reset();
    AnalysisEngineDescription aed = createEngineDescription(aed1, aed2);
    AnalysisEngine ae = createEngine(aed);
    ae.process(ae.newJCas());

    // Check the external resource was injected
    MultiBindAE.reset();
    SimplePipeline.runPipeline(CasCreationUtils.createCas(aed), aed);
  }

  @Test
  void testMultiBoundNested() throws Exception {
    ExternalResourceDescription extDesc = createResourceDescription(
            IntermediateResourceWithAssert.class,
            IntermediateResourceWithAssert.PARAM_NESTED_RESOURCE,
            createResourceDescription(ResourceWithAssert.class));

    // Binding external resource to each Annotator individually
    AnalysisEngineDescription aed1 = createEngineDescription(MultiBindAE.class, MultiBindAE.RES_KEY,
            extDesc);
    AnalysisEngineDescription aed2 = createEngineDescription(MultiBindAE.class, MultiBindAE.RES_KEY,
            extDesc);

    // Check the external resource was injected
    MultiBindAE.reset();
    AnalysisEngineDescription aed = createEngineDescription(aed1, aed2);
    AnalysisEngine ae = createEngine(aed);
    ae.process(ae.newJCas());

    // Check the external resource was injected
    MultiBindAE.reset();
    // SimplePipeline.runPipeline(CasCreationUtils.createCas(aed.getAnalysisEngineMetaData()), aed);
    SimplePipeline.runPipeline(ae.newCAS(), aed);
  }

  /**
   * Test resource list.
   */
  @Test
  void testMultiValue() throws Exception {
    ExternalResourceDescription extDesc1 = createResourceDescription(ResourceWithAssert.class);
    ExternalResourceDescription extDesc2 = createResourceDescription(ResourceWithAssert.class);

    AnalysisEngineDescription aed = createEngineDescription(MultiValuedResourceAE.class,
            MultiValuedResourceAE.RES_RESOURCE_ARRAY, asList(extDesc1, extDesc2));

    AnalysisEngine ae = createEngine(aed);
    ae.process(ae.newJCas());
    ae.collectionProcessComplete();
  }

  /**
   * Test sharing a resource list between two AEs on the same aggregate.
   */
  @Test
  void testMultiValue2() throws Exception {
    MultiValuedResourceAE.resources.clear();

    ExternalResourceDescription extDesc1 = createResourceDescription(ResourceWithAssert.class);
    ExternalResourceDescription extDesc2 = createResourceDescription(ResourceWithAssert.class);

    AnalysisEngineDescription aed = createEngineDescription(
            createEngineDescription(MultiValuedResourceAE.class,
                    MultiValuedResourceAE.RES_RESOURCE_ARRAY, asList(extDesc1, extDesc2)),
            createEngineDescription(MultiValuedResourceAE.class,
                    MultiValuedResourceAE.RES_RESOURCE_ARRAY, asList(extDesc1, extDesc2)));

    AnalysisEngine ae = createEngine(aed);
    ae.process(ae.newJCas());

    // Check that the shared resources are really the same
    assertEquals(MultiValuedResourceAE.resources.get(0), MultiValuedResourceAE.resources.get(2));
    assertEquals(MultiValuedResourceAE.resources.get(1), MultiValuedResourceAE.resources.get(3));
  }

  /**
   * Test sharing a resource list across aggregates.
   */
  @Test
  void testMultiValue3() throws Exception {
    MultiValuedResourceAE.resources.clear();

    ExternalResourceDescription extDesc1 = createResourceDescription(ResourceWithAssert.class);
    ExternalResourceDescription extDesc2 = createResourceDescription(ResourceWithAssert.class);

    AnalysisEngineDescription aed = createEngineDescription(
            createEngineDescription(MultiValuedResourceAE.class,
                    MultiValuedResourceAE.RES_RESOURCE_ARRAY, asList(extDesc1, extDesc2)),
            createEngineDescription(createEngineDescription(MultiValuedResourceAE.class,
                    MultiValuedResourceAE.RES_RESOURCE_ARRAY, asList(extDesc1, extDesc2))));

    AnalysisEngine ae = createEngine(aed);
    ae.process(ae.newJCas());

    // Check that the shared resources are really the same
    assertEquals(MultiValuedResourceAE.resources.get(0), MultiValuedResourceAE.resources.get(2));
    assertEquals(MultiValuedResourceAE.resources.get(1), MultiValuedResourceAE.resources.get(3));
  }

  /**
   * Test nested resource lists.
   */
  @Test
  void testMultiValue4() throws Exception {
    ExternalResourceDescription extDesc1 = createResourceDescription(ResourceWithAssert.class);
    ExternalResourceDescription extDesc2 = createResourceDescription(ResourceWithAssert.class);

    ExternalResourceDescription extDesc3 = createResourceDescription(ResourceWithAssert.class);
    ExternalResourceDescription extDesc4 = createResourceDescription(ResourceWithAssert.class);

    ExternalResourceDescription mv1 = createResourceDescription(MultiValuedResource.class,
            MultiValuedResource.RES_RESOURCE_LIST,
            new ExternalResourceDescription[] { extDesc1, extDesc2 });

    ExternalResourceDescription mv2 = createResourceDescription(MultiValuedResource.class,
            MultiValuedResource.RES_RESOURCE_LIST,
            new ExternalResourceDescription[] { extDesc3, extDesc4 });

    AnalysisEngineDescription aed = createEngineDescription(MultiValuedResourceAE.class,
            MultiValuedResourceAE.RES_RESOURCE_ARRAY, asList(mv1, mv2));

    AnalysisEngine ae = createEngine(aed);
    ae.process(ae.newJCas());
    ae.collectionProcessComplete();
  }

  @Test
  void testNestedAggregateBinding() throws Exception {
    ExternalResourceDescription resourceDescription = createSharedResourceDescription("",
            DummyResource.class);

    AggregateBuilder builder = new AggregateBuilder();
    builder.add(createEngineDescription(ResourceDependent.class));
    AnalysisEngineDescription aggregateDescription = builder.createAggregateDescription();

    bindResource(aggregateDescription, DummyResource.RESOURCE_KEY, resourceDescription);

    JCas jCas = createJCas();
    jCas.setDocumentText("Hello");

    runPipeline(jCas, aggregateDescription);
    int count = 0;
    for (AnalyzedText annotation : JCasUtil.select(jCas, AnalyzedText.class)) {
      assertEquals("World", annotation.getText());
      count++;
    }

    assertEquals(1, count);
  }

  public static class DummyResource implements SharedResourceObject {

    public static final String RESOURCE_KEY = "DummyResource";

    public DummyResource() {
    }

    @Override
    public void load(DataResource aData) throws ResourceInitializationException {
      // Nothing to do
    }

    public String getText() {
      return "World";
    }
  }

  public static class ResourceDependent extends JCasAnnotator_ImplBase {

    @ExternalResource(key = DummyResource.RESOURCE_KEY)
    private DummyResource dummyResource;

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
      // Just marking up that the AE was executed as expected, so that it can be verified.
      AnalyzedText annotation = new AnalyzedText(aJCas, 0, aJCas.getDocumentText().length());
      annotation.setText(dummyResource.getText());
      annotation.addToIndexes();
    }
  }

  private static void bindResources(AnalysisEngineDescription desc) throws Exception {
    bindResource(desc, ResourceWithAssert.class);
    bindResource(desc, DummyAE.RES_KEY_1, AnnotatedResource.class, AnnotatedResource.PARAM_VALUE,
            "1");
    bindResource(desc, DummyAE.RES_KEY_2, AnnotatedResource.class, AnnotatedResource.PARAM_VALUE,
            "2");
    bindResource(desc, DummyAE.RES_KEY_3, AnnotatedParametrizedDataResource.class,
            AnnotatedParametrizedDataResource.PARAM_EXTENSION, ".lala");
    // https://issues.apache.org/jira/browse/UIMA-5555
    // bindResource(desc, DummySharedResourceObject.class, EX_URI,
    // DummySharedResourceObject.PARAM_VALUE, "3",
    // DummySharedResourceObject.PARAM_ARRAY_VALUE, new String[] {"1", "2", "3"});

    // An undefined URL may be used if the specified file/remote URL does not exist or if
    // the network is down.
    bindResource(desc, DummyAE.RES_SOME_URL, new File(EX_FILE_1).toURI().toURL());
    bindResource(desc, DummyAE.RES_SOME_OTHER_URL, new File(EX_FILE_3).toURI().toURL());
    bindResource(desc, DummyAE.RES_SOME_FILE, new File(EX_FILE_1));
    bindResource(desc, DummyAE.RES_JNDI_OBJECT, JndiResourceLocator.class,
            JndiResourceLocator.PARAM_NAME, "dictionaries/german");

    // https://issues.apache.org/jira/browse/UIMA-5555
    // createDependencyAndBind(desc, "legacyResource", DummySharedResourceObject.class, EX_URI,
    // DummySharedResourceObject.PARAM_VALUE, "3",
    // DummySharedResourceObject.PARAM_ARRAY_VALUE, new String[] {"1", "2", "3"});
  }

  public static class DummyAE extends JCasAnnotator_ImplBase {
    @ExternalResource
    ResourceWithAssert r;

    static final String RES_KEY_1 = "Key1";

    @ExternalResource(key = RES_KEY_1)
    AnnotatedResource configRes1;

    static final String RES_KEY_2 = "Key2";

    @ExternalResource(key = RES_KEY_2)
    AnnotatedResource configRes2;

    static final String RES_KEY_3 = "Key3";

    // https://issues.apache.org/jira/browse/UIMA-5555
    // @ExternalResource
    // DummySharedResourceObject sharedObject;

    static final String RES_SOME_URL = "SomeUrl";

    @ExternalResource(key = RES_SOME_URL)
    DataResource someUrl;

    static final String RES_SOME_OTHER_URL = "SomeOtherUrl";

    @ExternalResource(key = RES_SOME_OTHER_URL)
    DataResource someOtherUrl;

    static final String RES_SOME_FILE = "SomeFile";

    @ExternalResource(key = RES_SOME_FILE)
    DataResource someFile;

    static final String RES_JNDI_OBJECT = "JndiObject";

    @ExternalResource(key = RES_JNDI_OBJECT)
    Properties jndiPropertes;

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
      assertNotNull(r);

      assertNotNull(configRes1);
      assertEquals("1", configRes1.getValue());

      assertNotNull(configRes2);
      assertEquals("2", configRes2.getValue());

      try {
        DataResource configuredResource = (DataResource) getContext().getResourceObject(RES_KEY_3,
                new String[] { AnnotatedDataResource.PARAM_URI, "http://dum.my/conf" });
        assertNotNull(configuredResource);
        assertEquals("http://dum.my/conf.lala", configuredResource.getUri().toString());
      } catch (ResourceAccessException e) {
        throw new AnalysisEngineProcessException(e);
      }

      // https://issues.apache.org/jira/browse/UIMA-5555
      // assertNotNull(sharedObject);
      // assertEquals("3", sharedObject.getValue());
      // assertEquals(asList("1", "2", "3"), asList(sharedObject.getArrayValue()));
      //
      // assertNotNull(sharedObject);
      // assertEquals(EX_URI, sharedObject.getUrl().toString());

      assertNotNull(jndiPropertes);
      assertEquals("proper noun", jndiPropertes.get("Hans"));

      assertNotNull(someUrl);
      assertEquals(new File(EX_FILE_1).toURI().toString(), someUrl.getUri().toString());

      assertNotNull(someOtherUrl);
      assertEquals(new File(EX_FILE_3).toURI().toString(), someOtherUrl.getUri().toString());

      assertTrue(someFile.getUrl().toString().startsWith("file:"));
      assertThat(someFile.getUrl().toString())
              .as("URL [" + someFile.getUrl() + "] should end in [" + EX_FILE_1 + "]")
              .endsWith(EX_FILE_1);

      // https://issues.apache.org/jira/browse/UIMA-5555
      // try {
      // assertNotNull(getContext().getResourceObject("legacyResource"));
      // } catch (ResourceAccessException e) {
      // throw new AnalysisEngineProcessException(e);
      // }
    }
  }

  public static final class DummyAE2 extends DummyAE {
    static final String RES_INJECTED_POJO1 = "InjectedPojo1";

    @ExternalResource(key = RES_INJECTED_POJO1)
    String injectedString;

    static final String RES_INJECTED_POJO2 = "InjectedPojo2";

    @ExternalResource(key = RES_INJECTED_POJO2)
    Number injectedAtomicInt;

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
      super.process(aJCas);
      assertEquals("Just an injected POJO", injectedString);
      assertEquals(5, injectedAtomicInt.intValue());
    }
  }

  public static class DummyAE3 extends JCasAnnotator_ImplBase {
    static final String RES_KEY_1 = "Key1";

    @ExternalResource(key = RES_KEY_1)
    AnnotatedResource configRes1;

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
      assertNotNull(configRes1);
      assertEquals("1", configRes1.getValue());
    }
  }

  public static final class MultiValuedResourceAE
          extends org.apache.uima.fit.component.JCasAnnotator_ImplBase {
    static final String RES_RESOURCE_ARRAY = "resourceArray";

    @ExternalResource(key = RES_RESOURCE_ARRAY)
    ResourceWithAssert[] resourceArray;

    public static List<ResourceWithAssert> resources = new ArrayList<ResourceWithAssert>();

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
      assertThat(resourceArray) //
              .extracting(item -> item instanceof ResourceWithAssert) //
              .containsExactly(true, true);

      resources.add(resourceArray[0]);
      resources.add(resourceArray[1]);
      assertThat(resourceArray[0]).isNotSameAs(resourceArray[1]);

      System.out.printf("Element object 0: %d%n", resourceArray[0].hashCode());
      System.out.printf("Element object 1: %d%n", resourceArray[1].hashCode());

      for (ResourceWithAssert res : resourceArray) {
        res.doAsserts();
      }
    }
  }

  public static final class MultiValuedResource extends ResourceWithAssert {
    static final String RES_RESOURCE_LIST = "resourceList";

    @ExternalResource(key = RES_RESOURCE_LIST)
    List<ResourceWithAssert> resourceList;

    public static List<ResourceWithAssert> resources = new ArrayList<ResourceWithAssert>();

    @Override
    public void doAsserts() {
      assertThat(resourceList) //
              .extracting(item -> item instanceof ResourceWithAssert) //
              .containsExactly(true, true);
      assertThat(resourceList.get(0)).isNotSameAs(resourceList.get(1));

      resources.add(resourceList.get(0));
      resources.add(resourceList.get(1));

      System.out.printf("Element object 0: %d%n", resourceList.get(0).hashCode());
      System.out.printf("Element object 1: %d%n", resourceList.get(1).hashCode());

      for (ResourceWithAssert res : resourceList) {
        res.doAsserts();
      }
    }
  }

  /**
   * Example annotator that uses the share model object. In the process() we only test if the model
   * was properly initialized by uimaFIT
   */
  public static class MultiBindAE extends org.apache.uima.fit.component.JCasAnnotator_ImplBase {
    static int prevHashCode = -1;

    static final String RES_KEY = "Res";

    @ExternalResource(key = RES_KEY)
    ResourceWithAssert res;

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
      if (prevHashCode == -1) {
        prevHashCode = res.hashCode();
      } else {
        assertEquals(prevHashCode, res.hashCode());
      }

      System.out.println(getClass().getSimpleName() + ": " + res);
    }

    public static void reset() {
      prevHashCode = -1;
    }
  }

  public static class ResourceWithAssert extends Resource_ImplBase {
    public void doAsserts() {
      // Nothing by default
    }
  }

  public static class IntermediateResourceWithAssert extends ResourceWithAssert {

    public static final String PARAM_NESTED_RESOURCE = "nestedResource";

    @ExternalResource(key = PARAM_NESTED_RESOURCE)
    private ResourceWithAssert nestedResource;

    @Override
    public void doAsserts() {
      assertNotNull(nestedResource);
      nestedResource.doAsserts();
    }
  }

  public static final class AnnotatedResource extends Resource_ImplBase {
    public static final String PARAM_VALUE = "Value";

    @ConfigurationParameter(name = PARAM_VALUE, mandatory = true)
    private String value;

    public String getValue() {
      return value;
    }
  }

  public static final class AnnotatedDataResource extends Resource_ImplBase
          implements DataResource {
    public static final String PARAM_URI = "Uri";

    @ConfigurationParameter(name = PARAM_URI, mandatory = true)
    private String uri;

    public static final String PARAM_EXTENSION = "Extension";

    @ConfigurationParameter(name = PARAM_EXTENSION, mandatory = true)
    private String extension;

    @Override
    public InputStream getInputStream() throws IOException {
      return null;
    }

    @Override
    public URI getUri() {
      return URI.create(uri + extension);
    }

    @Override
    public URL getUrl() {
      return null;
    }
  }

  public static final class AnnotatedParametrizedDataResource extends Resource_ImplBase
          implements ParameterizedDataResource {
    public static final String PARAM_EXTENSION = "Extension";

    @ConfigurationParameter(name = PARAM_EXTENSION, mandatory = true)
    private String extension;

    @Override
    public DataResource getDataResource(String[] aParams) throws ResourceInitializationException {
      List<String> params = new ArrayList<String>(Arrays.asList(aParams));
      params.add(AnnotatedDataResource.PARAM_EXTENSION);
      params.add(extension);
      ExternalResourceDescription desc = ExternalResourceFactory.createNamedResourceDescription(
              null, AnnotatedDataResource.class, params.toArray(new String[params.size()]));
      return (DataResource) UIMAFramework.produceResource(desc.getResourceSpecifier(), null);
    }
  }

  public static final class DummySharedResourceObject implements SharedResourceObject {
    public static final String PARAM_VALUE = "Value";

    @ConfigurationParameter(name = PARAM_VALUE, mandatory = true)
    private String value;

    public static final String PARAM_ARRAY_VALUE = "arrayValue";

    @ConfigurationParameter(name = PARAM_ARRAY_VALUE, mandatory = true)
    private String[] arrayValue;

    // https://issues.apache.org/jira/browse/UIMA-5555
    // private URI uri;

    @Override
    public void load(DataResource aData) throws ResourceInitializationException {
      ConfigurationParameterInitializer.initialize(this, aData);
      // https://issues.apache.org/jira/browse/UIMA-5555
      // assertEquals(EX_URI, aData.getUri().toString());
      // uri = aData.getUri();
    }

    // https://issues.apache.org/jira/browse/UIMA-5555
    // public URI getUrl() {
    // return uri;
    // }

    public String getValue() {
      return value;
    }

    public String[] getArrayValue() {
      return arrayValue;
    }
  }
}
