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

package org.apache.uima.analysis_component;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;

/**
 * Analysis Components are the primitive "building blocks" from which UIMA solutions are built. This
 * is the common superinterface for all user-developed components that take a CAS as input and may
 * produce CASes as output.
 * <p>
 * Typically, developers do not implement this interface directly. There are several abstract
 * classes that you can inherit from depending on the function that your component performs and
 * which CAS interface it uses:
 * <ul>
 * <li> Annotator: Receives an input CAS and updates it
 * <ul>
 * <li>{@link JCasAnnotator_ImplBase}: Uses JCas interface</li>
 * <li>{@link CasAnnotator_ImplBase}: Uses CASinterface
 * </ul>
 * </li>
 * <li>{@link org.apache.uima.collection.CasConsumer_ImplBase}: Receives an input CAS but does not
 * update it. May update a data structure based on information in the CASes it receives.</li>
 * <li> CasMultiplier: Receives an input CAS and, in addition to updating it, may output new CASes.
 * One common use of this is to split a CAS into pieces, emitting each piece as a separate output
 * CAS.
 * <ul>
 * <li>{@link JCasMultiplier_ImplBase}: Uses JCas interface</li>
 * <li>{@link CasMultiplier_ImplBase}: Uses CAS interface</li>
 * <li>{@link org.apache.uima.collection.CollectionReader_ImplBase}: A special type of
 * CasMultiplier that, for historical reasons, does not take an input CAS.</li>
 * </ul>
 * </li>
 * </ul>
 * <p>
 * The framework interacts with AnalysisComponents as follows:
 * <ol>
 * <li>The framework calls the AnalysisComponent's {@link #process(AbstractCas)} method with an
 * input CAS.</li>
 * <li>The framework then calls the AnalysisComponent's {@link #hasNext()} method, which should
 * return <code>true</code> if the AnalysisComponent intends to produce new output CASes, or
 * <code>false</code> if the AnalysisComponent will not produce new output CASes.</li>
 * <li>If the AnalysisComponent returns <code>true</code>, the framework will then call the
 * {@link #next()} method.</li>
 * <li>The AnalysisComponent, in its <code>next</code> method, can create a new CAS by calling
 * {@link UimaContext#getEmptyCas(Class)} (or instead, one of the helper methods in the ImplBase
 * class that it extended). It then populates the empty CAS and returns it.</li>
 * <li>Steps 2 &amp; 3 continue for each subsequent output CAS, until <code>hasNext()</code> returns
 * false.</li>
 * </ol>
 * 
 * From the time when <code>process</code> is called until the time when <code>hasNext</code>
 * returns false, the AnalysisComponent "owns" the CAS that was passed to <code>process</code>.
 * The AnalysisComponent is permitted to make changes to this CAS. Once <code>hasNext</code>
 * returns false, the AnalysisComponent releases control of the initial CAS. This means that the
 * AnalysisComponent must finish all updates to the initial CAS prior to returning false from
 * <code>hasNext</code>.
 * <p>
 * However, if the <code>process</code> method is called a second time, before <code>hasNext</code> has returned
 * false, this is a signal to the AnalysisComponent to cancel all processing of the previous CAS and begin
 * processing the new CAS instead.
 */
public interface AnalysisComponent {
  /**
   * Performs any startup tasks required by this component. The framework calls this method only
   * once, just after the AnalysisComponent has been instantiated.
   * <p>
   * The framework supplies this AnalysisComponent with a reference to the {@link UimaContext} that
   * it will use, for example to access configuration settings or resources. This AnalysisComponent
   * should store a reference to its the <code>UimaContext</code> for later use.
   * 
   * @param aContext
   *          Provides access to services and resources managed by the framework. This includes
   *          configuration parameters, logging, and access to external resources.
   * 
   * @throws ResourceInitializationException
   *           if this AnalysisComponent cannot initialize successfully.
   */
  void initialize(UimaContext aContext) throws ResourceInitializationException;

  /**
   * Alerts this AnalysisComponent that the values of its configuration parameters or external
   * resources have changed. This AnalysisComponent should re-read its configuration from the
   * {@link UimaContext} and take appropriate action to reconfigure itself.
   * <p>
   * In the abstract base classes provided by the framework, this is generally implemented by
   * calling <code>destroy</code> followed by <code>initialize</code> and
   * <code>typeSystemChanged</code>. If a more efficient implementation is needed, you can
   * override that implementation.
   * 
   * @throws ResourceConfigurationException
   *           if the configuration specified for this component is invalid.
   * @throws ResourceInitializationException
   *           if this component fails to reinitialize itself based on the new configuration.
   */
  void reconfigure() throws ResourceInitializationException, ResourceConfigurationException;

  /**
   * Completes the processing of a batch of CASes. The size of a batch is determined based on
   * configuration provided by the application that is using this component. The purpose of
   * <code>batchProcessComplete</code> is to give this AnalysisComponent the change to flush
   * information from memory to persistent storage. In the event of an error, this allows the
   * processing to be restarted from the end of the last completed batch.
   * <p>
   * If this component's descriptor declares that it is <code>recoverable</code>, then this
   * component is <i>required</i> to be restartable from the end of the last completed batch.
   * 
   * @throws AnalysisEngineProcessException
   *           if this component encounters a problem in flushing its state to persistent storage
   */
  void batchProcessComplete() throws AnalysisEngineProcessException;

  /**
   * Notifies this AnalysisComponent that processing of an entire collection has been completed. In
   * this method, this component should finish writing any output relating to the current
   * collection.
   * 
   * @throws AnalysisEngineProcessException
   *           if this component encounters a problem in its end-of-collection processing
   */
  void collectionProcessComplete() throws AnalysisEngineProcessException;

  /**
   * Frees all resources held by this AnalysisComponent. The framework calls this method only once,
   * when it is finished using this component.
   */
  void destroy();

  /**
   * Inputs a CAS to the AnalysisComponent. The AnalysisComponent "owns" this CAS until such time as
   * {@link #hasNext()} is called and returns false or until <code>process</code> is called again
   * (see class description).
   * 
   * @param aCAS
   *          A CAS that this AnalysisComponent should process. The framework will ensure that aCAS
   *          implements the specific CAS interface specified by the
   *          {@link #getRequiredCasInterface()} method.
   * 
   * @throws AnalysisEngineProcessException
   *           if a problem occurs during processing
   */
  void process(AbstractCas aCAS) throws AnalysisEngineProcessException;

  /**
   * Asks if this AnalysisComponent has another CAS to output. If this method returns true, then a
   * call to {@link #next()} should retrieve the next output CAS. When this method returns false,
   * the AnalysisComponent gives up control of the initial CAS that was passed to its
   * {@link #process(AbstractCas)} method.
   * 
   * @return true if this AnalysisComponent has another CAS to output, false if not.
   * 
   * @throws AnalysisEngineProcessException
   *           if a problem occurs during processing
   */
  boolean hasNext() throws AnalysisEngineProcessException;

  /**
   * Gets the next output CAS. The framework will only call this method after first calling
   * {@link #hasNext()} and checking that it returns true.
   * <p>
   * The AnalysisComponent can obtain a new CAS by calling {@link UimaContext#getEmptyCas(Class)}
   * (or instead, one of the helper methods in the ImplBase class that it extended).
   * 
   * @return the next output CAS.
   * 
   * @throws AnalysisEngineProcessException
   *           if a problem occurs during processing
   */
  AbstractCas next() throws AnalysisEngineProcessException;

  /**
   * Returns the specific CAS interface that this AnalysisComponent requires the framework to pass
   * to its {@link #process(AbstractCas)} method.
   * 
   * @return the required CAS interface. This must specify a subtype of {@link AbstractCas}.
   */
  Class<? extends AbstractCas> getRequiredCasInterface();

  /**
   * Returns the maximum number of CAS instances that this AnalysisComponent expects to use at the
   * same time. This only applies to CasMultipliers. Most CasMultipliers will only need one CAS at a
   * time. Only if there is a clear need should this be overridden to return something greater than
   * 1.
   * 
   * @return the number of CAS instances required by this AnalysisComponent.
   */
  int getCasInstancesRequired();

  /**
   * Sets the ResultSpecification that this AnalysisComponent should use. The ResultSpecification is
   * a set of types and features that this AnalysisComponent is asked to produce. An Analysis
   * Component may (but is not required to) optimize its processing by omitting the generation of
   * any types or features that are not part of the ResultSpecification.
   * 
   * @param aResultSpec
   *          the ResultSpecification for this Analysis Component to use.
   */
  void setResultSpecification(ResultSpecification aResultSpec);
}
