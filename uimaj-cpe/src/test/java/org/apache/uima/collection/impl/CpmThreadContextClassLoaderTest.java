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

package org.apache.uima.collection.impl;

import static java.lang.Thread.currentThread;
import static java.lang.Thread.sleep;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.uima.Constants.JAVA_FRAMEWORK_NAME;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_component.CasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.collection.CollectionProcessingEngine;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.collection.impl.cpm.engine.CPMExecutorService;
import org.apache.uima.collection.impl.cpm.utils.TestStatusCallbackListener;
import org.apache.uima.collection.impl.metadata.cpe.CpeDescriptorFactory;
import org.apache.uima.resource.ResourceCreationSpecifier;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.Progress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests that the worker threads started by the CPM run under the thread context class loader (TCCL)
 * of the control thread from which processing was started (see issue #254).
 * <p>
 * The components (collection reader, annotator, CAS consumer) are defined as inner classes and
 * their descriptors are generated programmatically into a temporary directory, so the test does not
 * rely on any checked-in XML descriptors.
 * <p>
 * The TCCL of each worker thread that runs a component is recorded into {@link #OBSERVED}, keyed by
 * the role of the component. The tests then assert that every observed loader is the loader that was
 * set on the control thread before {@code process()} was called.
 * <p>
 * This test lives in the same package as {@link CollectionProcessingEngine_impl} so that it can
 * reach the CPM's executor through the package-visible {@link CollectionProcessingEngine_impl#getCPM()}
 * accessor without widening any production API or resorting to reflection.
 */
class CpmThreadContextClassLoaderTest {

  private static final String ROLE_READER = "reader";

  private static final String ROLE_ANNOTATOR = "annotator";

  private static final String ROLE_CONSUMER = "consumer";

  private static final int DOCUMENT_COUNT = 5;

  /** Records, per role, the TCCL of every worker thread that ran a component. */
  static final Map<String, List<ClassLoader>> OBSERVED = new ConcurrentHashMap<>();

  static void recordTccl(String role) {
    OBSERVED.computeIfAbsent(role, k -> new CopyOnWriteArrayList<>())
            .add(currentThread().getContextClassLoader());
  }

  @BeforeEach
  void setUp() throws Exception {
    OBSERVED.clear();
    // The generated descriptors are minimal; skip schema validation as the other CPM tests do.
    UIMAFramework.getXMLParser().enableSchemaValidation(false);
  }

  /**
   * Baseline / specification test: with a freshly created CPE, all worker threads should run under
   * the TCCL set on the control thread. Note that this may already pass without a fix, because a
   * freshly created thread inherits the TCCL of the thread that constructs it - and the first CPM
   * worker is created from the control thread. It documents the intended contract and guards against
   * regressions.
   */
  @Test
  void testWorkerThreadsRunUnderControlThreadTccl(@TempDir Path tempDir) throws Exception {
    var original = currentThread().getContextClassLoader();
    var controlLoader = new URLClassLoader(new URL[0], original);

    var cpe = buildCpe(3, tempDir);
    try {
      currentThread().setContextClassLoader(controlLoader);
      runToCompletion(cpe);
    } finally {
      currentThread().setContextClassLoader(original);
    }

    assertAllObservedLoadersAre(controlLoader);
  }

  /**
   * Provokes the actual bug: a {@link ThreadPoolExecutor} does not reset the TCCL of its worker
   * threads between tasks. Here we pre-warm the CPM's executor with worker threads that are left
   * with a "stale" TCCL. When processing is then started under a different (control) loader, the CPM
   * reuses those pooled threads, so the workers - and everything they spawn - run under the stale
   * loader instead of the control loader.
   * <p>
   * This test is expected to FAIL before the fix (workers observe the stale loader) and to pass once
   * the worker threads properly adopt the control thread's TCCL.
   */
  @Test
  void testWorkerThreadsRunUnderControlThreadTcclEvenWhenPoolIsPrewarmed(
          @TempDir Path tempDir) throws Exception {
    var original = currentThread().getContextClassLoader();
    var controlLoader = new URLClassLoader(new URL[0], original);
    var staleLoader = new URLClassLoader(new URL[0], original);

    var threadCount = 3;
    var cpe = buildCpe(threadCount, tempDir);

    // Leave a number of pooled worker threads parked with a stale TCCL.
    prewarmPoolWithStaleTccl(executorOf(cpe), staleLoader, threadCount + 2);

    try {
      currentThread().setContextClassLoader(controlLoader);
      runToCompletion(cpe);
    } finally {
      currentThread().setContextClassLoader(original);
    }

    assertAllObservedLoadersAre(controlLoader);
  }

  /**
   * Submits {@code count} tasks that each set their worker thread's context class loader to
   * {@code staleLoader} and then block until all of them are running (forcing the pool to create
   * {@code count} distinct worker threads). They are then released and return to the pool, where
   * they remain parked - still carrying the stale loader.
   */
  private void prewarmPoolWithStaleTccl(CPMExecutorService executor, ClassLoader staleLoader,
          int count) throws Exception {
    var allRunning = new CountDownLatch(count);
    var release = new CountDownLatch(1);

    var futures = new Future<?>[count];
    for (var i = 0; i < count; i++) {
      futures[i] = executor.submit(() -> {
        currentThread().setContextClassLoader(staleLoader);
        allRunning.countDown();
        try {
          release.await();
        } catch (InterruptedException e) {
          currentThread().interrupt();
        }
      });
    }

    assertTrue(allRunning.await(10, SECONDS),
            "Pre-warm tasks did not all start; could not create stale worker threads");
    release.countDown();
    for (var f : futures) {
      f.get(10, SECONDS);
    }

    // Give the workers a moment to return to the pool and park (so they are reused by the CPM).
    sleep(250);
  }

  private CPMExecutorService executorOf(CollectionProcessingEngine cpe) {
    return ((CollectionProcessingEngine_impl) cpe).getCPM().cpmExecutorService;
  }

  private void assertAllObservedLoadersAre(ClassLoader expected) {
    for (var role : asList(ROLE_READER, ROLE_ANNOTATOR, ROLE_CONSUMER)) {
      var loaders = OBSERVED.get(role);
      assertTrue(loaders != null && !loaders.isEmpty(),
              "No worker thread recorded a TCCL for role '" + role + "'");
      for (var cl : loaders) {
        assertSame(expected, cl,
                "Worker thread for role '" + role + "' ran under an unexpected TCCL: " + cl
                        + " (expected the control thread's loader " + expected + ")");
      }
    }
  }

  private void runToCompletion(CollectionProcessingEngine cpe) throws Exception {
    var listener = new TestStatusCallbackListener();
    cpe.addStatusCallbackListener(listener);

    cpe.process();

    var deadline = System.currentTimeMillis() + 30_000;
    while (!listener.isFinished() && System.currentTimeMillis() < deadline) {
      sleep(5);
    }
    if (!listener.isFinished()) {
      fail("CPM did not finish within the timeout (aborted=" + listener.getAbortedCount() + ")");
    }
  }

  private CollectionProcessingEngine buildCpe(int threadCount, Path tempDir) throws Exception {
    var readerDesc = writeCollectionReaderDescriptor(tempDir);
    var annotatorDesc = writeAnalysisEngineDescriptor(tempDir);
    var consumerDesc = writeCasConsumerDescriptor(tempDir);

    var cpeDesc = CpeDescriptorFactory.produceDescriptor();
    cpeDesc.setInputQueueSize(2);
    cpeDesc.setOutputQueueSize(2);
    cpeDesc.setProcessingUnitThreadCount(threadCount);

    var annotator = CpeDescriptorFactory.produceCasProcessor("RecordingAnnotator");
    annotator.setCpeComponentDescriptor(CpeDescriptorFactory.produceComponentDescriptor(annotatorDesc));
    cpeDesc.addCasProcessor(annotator);

    var consumer = CpeDescriptorFactory.produceCasProcessor("RecordingConsumer");
    consumer.setCpeComponentDescriptor(CpeDescriptorFactory.produceComponentDescriptor(consumerDesc));
    cpeDesc.addCasProcessor(consumer);

    cpeDesc.addCollectionReader(readerDesc);

    return UIMAFramework.produceCollectionProcessingEngine(cpeDesc, null, null);
  }

  private String writeCollectionReaderDescriptor(Path tempDir) throws Exception {
    var desc = UIMAFramework.getResourceSpecifierFactory()
            .createCollectionReaderDescription();
    desc.setFrameworkImplementation(JAVA_FRAMEWORK_NAME);
    desc.setImplementationName(RecordingReader.class.getName());
    desc.getCollectionReaderMetaData().setName("RecordingReader");
    return serialize(desc, tempDir, "RecordingReader.xml");
  }

  private String writeAnalysisEngineDescriptor(Path tempDir) throws Exception {
    var desc = UIMAFramework.getResourceSpecifierFactory()
            .createAnalysisEngineDescription();
    desc.setFrameworkImplementation(JAVA_FRAMEWORK_NAME);
    desc.setPrimitive(true);
    desc.setAnnotatorImplementationName(RecordingAnnotator.class.getName());
    desc.getAnalysisEngineMetaData().setName("RecordingAnnotator");
    return serialize(desc, tempDir, "RecordingAnnotator.xml");
  }

  private String writeCasConsumerDescriptor(Path tempDir) throws Exception {
    var desc = UIMAFramework.getResourceSpecifierFactory()
            .createCasConsumerDescription();
    desc.setFrameworkImplementation(JAVA_FRAMEWORK_NAME);
    desc.setImplementationName(RecordingConsumer.class.getName());
    desc.getCasConsumerMetaData().setName("RecordingConsumer");
    return serialize(desc, tempDir, "RecordingConsumer.xml");
  }

  private String serialize(ResourceCreationSpecifier desc, Path tempDir,
          String fileName) throws Exception {
    var file = tempDir.resolve(fileName);
    try (var out = Files.newOutputStream(file)) {
      desc.toXML(out);
    }
    return file.toAbsolutePath().toString();
  }

  /*
   * Components - defined as inner classes so the test needs no checked-in descriptor XML.
   */

  public static class RecordingReader extends CollectionReader_ImplBase {
    private int produced = 0;

    @Override
    public void getNext(CAS aCAS) {
      recordTccl(ROLE_READER);
      aCAS.setDocumentText("document " + produced);
      produced++;
    }

    @Override
    public boolean hasNext() {
      return produced < DOCUMENT_COUNT;
    }

    @Override
    public Progress[] getProgress() {
      return null;
    }

    @Override
    public void close() {
      // nothing to do
    }
  }

  public static class RecordingAnnotator extends CasAnnotator_ImplBase {
    @Override
    public void process(CAS aCAS) throws AnalysisEngineProcessException {
      recordTccl(ROLE_ANNOTATOR);
    }
  }

  public static class RecordingConsumer extends CasConsumer_ImplBase {
    @Override
    public void processCas(CAS aCAS) throws ResourceProcessException {
      recordTccl(ROLE_CONSUMER);
    }
  }
}
