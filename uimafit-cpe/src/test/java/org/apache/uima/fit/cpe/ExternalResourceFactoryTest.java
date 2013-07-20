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

package org.apache.uima.fit.cpe;

import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createAggregateDescription;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createPrimitiveDescription;
import static org.apache.uima.fit.factory.ExternalResourceFactory.createExternalResourceDescription;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.Resource_ImplBase;
import org.apache.uima.fit.cpe.CpePipelineTest.Reader;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.ExternalResourceFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.ParameterizedDataResource;
import org.apache.uima.resource.ResourceInitializationException;
import org.junit.Test;

/**
 * Test case for {@link ExternalResource} annotations.
 * 
 */
public class ExternalResourceFactoryTest {
  @Test
  public void testMultiBinding() throws Exception {
    ExternalResourceDescription extDesc = createExternalResourceDescription(ResourceWithAssert.class);

    // Binding external resource to each Annotator individually
    AnalysisEngineDescription aed1 = createPrimitiveDescription(MultiBindAE.class,
            MultiBindAE.RES_KEY, extDesc);
    AnalysisEngineDescription aed2 = createPrimitiveDescription(MultiBindAE.class,
            MultiBindAE.RES_KEY, extDesc);

    // Check the external resource was injected
    MultiBindAE.reset();
    AnalysisEngineDescription aaed = createAggregateDescription(aed1, aed2);
    CpePipeline.runPipeline(CollectionReaderFactory.createDescription(Reader.class), aaed);
  }

  /**
   * Test resource list.
   */
  @Test
  public void testMultiValue() throws Exception {
    ExternalResourceDescription extDesc1 = createExternalResourceDescription(ResourceWithAssert.class);
    ExternalResourceDescription extDesc2 = createExternalResourceDescription(ResourceWithAssert.class);

    AnalysisEngineDescription aed = createPrimitiveDescription(MultiValuedResourceAE.class,
            MultiValuedResourceAE.RES_RESOURCE_ARRAY, asList(extDesc1, extDesc2));

    CpePipeline.runPipeline(CollectionReaderFactory.createDescription(Reader.class), aed);
  }

  /**
   * Test sharing a resource list between two AEs on the same aggregate.
   */
  @Test
  public void testMultiValue2() throws Exception {
    MultiValuedResourceAE.resources.clear();

    ExternalResourceDescription extDesc1 = createExternalResourceDescription(ResourceWithAssert.class);
    ExternalResourceDescription extDesc2 = createExternalResourceDescription(ResourceWithAssert.class);

    AnalysisEngineDescription aed = createAggregateDescription(
            createPrimitiveDescription(MultiValuedResourceAE.class,
                    MultiValuedResourceAE.RES_RESOURCE_ARRAY, asList(extDesc1, extDesc2)),
            createPrimitiveDescription(MultiValuedResourceAE.class,
                    MultiValuedResourceAE.RES_RESOURCE_ARRAY, asList(extDesc1, extDesc2)));

    CpePipeline.runPipeline(CollectionReaderFactory.createDescription(Reader.class), aed);

    // Check that the shared resources are really the same
    assertEquals(MultiValuedResourceAE.resources.get(0), MultiValuedResourceAE.resources.get(2));
    assertEquals(MultiValuedResourceAE.resources.get(1), MultiValuedResourceAE.resources.get(3));
  }

  /**
   * Test sharing a resource list across aggregates.
   */
  @Test
  public void testMultiValue3() throws Exception {
    MultiValuedResourceAE.resources.clear();

    ExternalResourceDescription extDesc1 = createExternalResourceDescription(ResourceWithAssert.class);
    ExternalResourceDescription extDesc2 = createExternalResourceDescription(ResourceWithAssert.class);

    AnalysisEngineDescription aed = createAggregateDescription(
            createPrimitiveDescription(MultiValuedResourceAE.class,
                    MultiValuedResourceAE.RES_RESOURCE_ARRAY, asList(extDesc1, extDesc2)),
            createAggregateDescription(createPrimitiveDescription(MultiValuedResourceAE.class,
                    MultiValuedResourceAE.RES_RESOURCE_ARRAY, asList(extDesc1, extDesc2))));

    CpePipeline.runPipeline(CollectionReaderFactory.createDescription(Reader.class), aed);

    // Check that the shared resources are really the same
    assertEquals(MultiValuedResourceAE.resources.get(0), MultiValuedResourceAE.resources.get(2));
    assertEquals(MultiValuedResourceAE.resources.get(1), MultiValuedResourceAE.resources.get(3));
  }

  /**
   * Test nested resource lists.
   */
  @Test
  public void testMultiValue4() throws Exception {
    ExternalResourceDescription extDesc1 = createExternalResourceDescription(ResourceWithAssert.class);
    ExternalResourceDescription extDesc2 = createExternalResourceDescription(ResourceWithAssert.class);

    ExternalResourceDescription extDesc3 = createExternalResourceDescription(ResourceWithAssert.class);
    ExternalResourceDescription extDesc4 = createExternalResourceDescription(ResourceWithAssert.class);

    ExternalResourceDescription mv1 = createExternalResourceDescription(MultiValuedResource.class,
            MultiValuedResource.RES_RESOURCE_LIST, new ExternalResourceDescription[] { extDesc1,
                extDesc2 });

    ExternalResourceDescription mv2 = createExternalResourceDescription(MultiValuedResource.class,
            MultiValuedResource.RES_RESOURCE_LIST, new ExternalResourceDescription[] { extDesc3,
                extDesc4 });

    AnalysisEngineDescription aed = createPrimitiveDescription(MultiValuedResourceAE.class,
            MultiValuedResourceAE.RES_RESOURCE_ARRAY, asList(mv1, mv2));

    CpePipeline.runPipeline(CollectionReaderFactory.createDescription(Reader.class), aed);
  }

  public static final class MultiValuedResourceAE extends
          org.apache.uima.fit.component.JCasAnnotator_ImplBase {
    static final String RES_RESOURCE_ARRAY = "resourceArray";

    @ExternalResource(key = RES_RESOURCE_ARRAY)
    ResourceWithAssert[] resourceArray;

    public static List<ResourceWithAssert> resources = new ArrayList<ResourceWithAssert>();

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
      assertNotNull("Resource array is null", resourceArray);
      assertEquals(2, resourceArray.length);
      assertTrue("Resource array element 0 is not a DummyResource",
              resourceArray[0] instanceof ResourceWithAssert);
      assertTrue("Resource array element 1 is not a DummyResource",
              resourceArray[1] instanceof ResourceWithAssert);
      assertTrue(resourceArray[0] != resourceArray[1]);

      resources.add(resourceArray[0]);
      resources.add(resourceArray[1]);

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
      assertNotNull("Resource array is null", resourceList);
      assertEquals(2, resourceList.size());
      assertTrue("Resource array element 0 is not a MultiValuedResourceAE",
              resourceList.get(0) instanceof ResourceWithAssert);
      assertTrue("Resource array element 1 is not a MultiValuedResourceAE",
              resourceList.get(1) instanceof ResourceWithAssert);
      assertTrue(resourceList.get(0) != resourceList.get(1));

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

  public static final class AnnotatedDataResource extends Resource_ImplBase implements DataResource {
    public static final String PARAM_URI = "Uri";

    @ConfigurationParameter(name = PARAM_URI, mandatory = true)
    private String uri;

    public static final String PARAM_EXTENSION = "Extension";

    @ConfigurationParameter(name = PARAM_EXTENSION, mandatory = true)
    private String extension;

    public InputStream getInputStream() throws IOException {
      return null;
    }

    public URI getUri() {
      return URI.create(uri + extension);
    }

    public URL getUrl() {
      return null;
    }
  }

  public static final class AnnotatedParametrizedDataResource extends Resource_ImplBase implements
          ParameterizedDataResource {
    public static final String PARAM_EXTENSION = "Extension";

    @ConfigurationParameter(name = PARAM_EXTENSION, mandatory = true)
    private String extension;

    public DataResource getDataResource(String[] aParams) throws ResourceInitializationException {
      List<String> params = new ArrayList<String>(Arrays.asList(aParams));
      params.add(AnnotatedDataResource.PARAM_EXTENSION);
      params.add(extension);
      ExternalResourceDescription desc = ExternalResourceFactory.createExternalResourceDescription(
              null, AnnotatedDataResource.class, params.toArray(new String[params.size()]));
      return (DataResource) UIMAFramework.produceResource(desc.getResourceSpecifier(), null);
    }
  }
}
