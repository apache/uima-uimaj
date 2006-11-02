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

package org.apache.uima.analysis_engine;

import java.util.Map;
import java.util.Properties;

import org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.TCAS;
import org.apache.uima.collection.base_cpm.CasObjectProcessor;
import org.apache.uima.jcas.impl.JCas;
import org.apache.uima.resource.ConfigurableResource;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.metadata.ConfigurationParameterSettings;
import org.apache.uima.util.Logger;
import org.apache.uima.util.ProcessTrace;


/**
 * An Analysis Engine is a component responsible for analyzing unstructured
 * information, discovering and representing semantic content.  Unstructured
 * information includes, but is not restricted to, text documents.
 * <p>
 * An AnalysisEngine operates on an "analysis structure" (implemented by
 * {@link org.apache.uima.cas.CAS}).  The <code>CAS</code> contains the
 * artifact to be processed as well as semantic information already inferred 
 * from that artifact.  The AnalysisEngine analyzes this information and adds 
 * new information to the <code>CAS</code>. 
 * <p>
 * To create an instance of an Analysis Engine, an application should call
 * {@link org.apache.uima.UIMAFramework#produceAnalysisEngine(ResourceSpecifier)}.
 * <p>
 * A typical application interacts with the Analysis Engine interface as 
 * follows:
 * <ol>
 * <li>Call {@link #newCAS()} to create a new Common Analysis System appropriate
 *     for this AnalysisEngine.</li>
 * <li>Use the {@link CAS} interface to populate
 *     the <code>CAS</code> with the artifact to be analyzed any information
 *     known about this docuemnt (e.g. the language of a text document). </li>
 * <li>Optionally create a {@link org.apache.uima.analysis_engine.ResultSpecification}
 *     that identifies the results you would like this AnalysisEngine to generate
 *     (e.g. people, places, and dates).
 * <li>Call {@link #process(CAS,ResultSpecification)} - the AnalysisEngine will 
 *     perform its analysis.</li>
 * <li>Retrieve the results from the {@link CAS}.</li>
 * <li>Call {@link CAS#reset()} to clear out the <code>CAS</code> and prepare 
 *     for processing a new artifact.</li>
 * <li>Repeat steps 2 through 6 for each artifact to be processed.</li>
 * </ol>   
 * <p>
 * <b>Important:</b> It is highly recommended that you reuse <code>CAS</code> 
 * objects rather than calling <code>newCAS()</code> prior to each analysis.
 * This is because <code>CAS</code> objects may be expensive to create and
 * may consume a significant amount of memory.
 * <p>
 * Instead of using the {@link CAS} interface, applications may wish to use
 * the Java-object-based {@link JCas} interface.  In that case, the call to
 * <code>newCAS</code> from step 1 above would be replaced by {@link #newJCas()},
 * and the {@link #process(JCas,ResultSpecification)} method would be used.
 * <p>
 * Analysis Engine implementations may or may not be capable of simultaneously 
 * processing multiple documents in a multithreaded environment.  See the
 * documentation associated with the implementation or factory method 
 * (e.g. ({@link org.apache.uima.UIMAFramework#produceAnalysisEngine(ResourceSpecifier)})
 * that you are using.
 *
 * 
 */
public interface AnalysisEngine 
   extends ConfigurableResource, CasObjectProcessor
{
  

  /**
   * Initializes this <code>Resource</code> from a <code>ResourceSpecifier</code>.
   * Applications do not need to call this method.  It is called automatically
   * by the <code>ResourceFactory</code> and cannot be called a second time.
   * <p>
   * The <code>AnalysisEngine</code> interface defines several optional
   * parameter that can be passed in the <code>aAdditionalParams</code> Map -
   * see the <code>PARAM</code> constants on this interface.
   * 
   * @see org.apache.uima.resource.Resource#initialize(ResourceSpecifier,Map)
   */
  public boolean initialize(ResourceSpecifier aSpecifier, Map aAdditionalParams)
    throws ResourceInitializationException;  
    
  /**
   * Gets the metadata that describes this <code>AnalysisEngine</code>. 
   * This is just a convenience method that calls {@link #getMetaData} and
   * casts the result to a {@link AnalysisEngineMetaData}.
   * 
   * @return an object containing all metadata for this AnalysisEngine
   */
  public AnalysisEngineMetaData getAnalysisEngineMetaData();
  
  
  /**
   * Creates a new Common Analysis System appropriate for this Analysis
   * Engine.  An application can pre-populate this CAS, then pass it to the
   * {@link #process(CAS, ResultSpecification)} method.  Then, when the process
   * method returns, the CAS will contain the results of the analysis.
   * <p>
   * <b>Important:</b> CAS creation is expensive, so if at all possible an
   * application should reuse CASes.  When a CAS instance is no longer being
   * used, call its {@link CAS#reset()} method, which will remove all prior
   * analysis information, and then reuse that same CAS instance for another
   * call to {@link #process(CAS,ResultSpecification)}.
   * <p>
   * Note that the CAS allows multiple subjects of analysis (e.g. documents) and
   * defines a separate "view" for each of them.  If your application wants to
   * work with a single subject of analysis, call the method {@link CAS#getDefaultView()}
   * and operate on the returned view.
   * 
   * @return a new <code>CAS</code> appropriate for this AnalysisEngine.
   * 
   * @throws ResourceInitializationException if a CAS could not be created 
   *    because this AnalysisEngine's CAS metadata (type system, 
   *    type priorities, or FS indexes) are invalid.  Ideally this would be
   *    checked at AnalysisEngine initialization time, and it will likely be
   *    moved in the future.
   */
  public CAS newCAS()
    throws ResourceInitializationException;


  /**
   * For backwards compatibility with v1.x only.
   * As of v2.0, replace with <code>newCAS().getDefaultView()</code>.
   */
  public TCAS newTCAS()
    throws ResourceInitializationException;   


  /**
   * Similar to {@link #newCAS()} but wraps the new CAS objects with the
   * Java-object-based {@link JCas} interface.
   * <p>
   * Note: the JCas that is returned is equivalent to what you would get
   * if you called <code>newCAS().getDefaultView().getJCas()</code>.
   * That is, this method returns a view of the default Sofa, NOT a Base CAS.
   * <p> 
   * <b>Important:</b> CAS creation is expensive, so if at all possible an
   * application should reuse CASes.  When a JCas instance is no longer being
   * used, call its {@link JCas#reset()} method, which will remove all prior
   * analysis information, and then reuse that same JCas instance for another
   * call to {@link #process(JCas,ResultSpecification)}.  
   * 
   * @return a new <code>CAS</code> appropriate for this AnalysisEngine.
   * 
   * @throws ResourceInitializationException if a CAS could not be created 
   *    because this AnalysisEngine's CAS metadata (type system, 
   *    type priorities, or FS indexes) are invalid.  Ideally this would be
   *    checked at AnalysisEngine initialization time, and it will likely be
   *    moved in the future.
   */
  public JCas newJCas()
    throws ResourceInitializationException;


  /**
   * Invokes this AnalysisEngine's analysis logic.  Prior to calling this method, 
   * the caller must ensure that the CAS has been populated with the artifact to 
   * be analyzed as well as any inputs required by this AnalysisEngine (as 
   * defined by this AnalysisEngine's 
   * {@link org.apache.uima.resource.metadata.Capability} specification.)
   * 
   * @param aCAS the CAS containing the inputs to the processing.  Analysis
   *    results will also be written to this CAS.
   * @param aResultSpec  a list of outputs that this AnalysisEngine should 
   *    produce.
   * 
   * @return an object containing information about which
   *   AnalysisEngine components have executed and information, such as timing, 
   *   about that execution.
   * 
   * @throws ResultNotSupportedException  if this AnalysisEngine is not 
   *    capable of producing the results requested in <code>aResultSpec</code>. 
   * @throws AnalysisEngineProcessException  if a failure occurs during processing.     
   */
  public ProcessTrace process(CAS aCAS, ResultSpecification aResultSpec) 
    throws ResultNotSupportedException, AnalysisEngineProcessException;
    

  /**
   * Invokes this AnalysisEngine's analysis logic.  Prior to calling this 
   * method, the caller must ensure that the CAS has been populated with the 
   * artifact to be analyzed as well as any inputs required by this
   * AnalysisEngine (as defined by this AnalysisEngine's 
   * {@link org.apache.uima.resource.metadata.Capability} specification.)
   * <p>
   * This version of <code>process</code> does not take a
   * {@link ResultSpecification} parameter.  This instructs the 
   * Analysis Engine to produce ALL possible results.
   * 
   * @param aCAS the CAS containing the inputs to the processing.  Analysis
   *    results will also be written to this CAS.
   *
   * @return an object containing information about which
   *   AnalysisEngine components have executed and information, such as timing, 
   *   about that execution.
   *  
   * @throws AnalysisEngineProcessException  if a failure occurs during processing.     
   */
  public ProcessTrace process(CAS aCAS) 
    throws AnalysisEngineProcessException;
    
  /**
   * Invokes this AnalysisEngine's analysis logic.  Prior to calling this method, 
   * the caller must ensure that the CAS has been populated with the artifact to 
   * be analyzed as well as any inputs required by this AnalysisEngine (as 
   * defined by this AnalysisEngine's 
   * {@link org.apache.uima.resource.metadata.Capability} specification.)
   * <p>
   * This version of this method takes a <code>ProcessTrace</code> object
   * as a parameter.  This allows trace events to be written to an existing
   * <code>ProcessTrace</code> rather than a new one.
   * 
   * @param aCAS the CAS containing the inputs to the processing.  Analysis
   *    results will also be written to this CAS.
   * @param aResultSpec  a list of outputs that this AnalysisEngine should 
   *    produce.
   * @param aTrace the object to which trace events will be recorded
   * 
   * @throws ResultNotSupportedException  if this AnalysisEngine is not 
   *    capable of producing the results requested in <code>aResultSpec</code>. 
   * @throws AnalysisEngineProcessException  if a failure occurs during processing.     
   */
  public void process(CAS aCAS, ResultSpecification aResultSpec, 
                      ProcessTrace aTrace) 
    throws ResultNotSupportedException, AnalysisEngineProcessException;
  
  
  /**
   * Invokes this AnalysisEngine's analysis logic.    Prior to calling this 
   * method, the caller must ensure that the CAS has been populated with the 
   * artifact to be analyzed as well as any inputs required by this
   * AnalysisEngine (as defined by this AnalysisEngine's 
   * {@link org.apache.uima.resource.metadata.Capability} specification.)
   * <p>
   * This version of this method is not normally used directly by applictaions.
   * It is used to call Analysis Engines that are components within an aggregate
   * Analysis Engine, so that they can share all information in the
   * {@link AnalysisProcessData} object, which includes the CAS and the
   * ProcessTrace. 
   * 
   * @param aResultSpec  a list of outputs that this AnalysisEngine should 
   *    produce. A <code>null</code> result specification is equivalent to
   *    a request for all possible results.
   * @param aProcessData  the data that will be modified during processing.  This 
   *    includes the CAS and the ProcessTrace.
   * 
   * @throws ResultNotSupportedException  if this AnalysisEngine is not 
   *    capable of producing the results requested in <code>aResultSpec</code>. 
   * @throws AnalysisEngineProcessException  if a failure occurs during processing.
   * 
   * @deprecated This is no longer used by the framework and was never intended
   *   for users to call.  Use {#link #process(CAS)} instead.
   */
  public void process(AnalysisProcessData aProcessData,
    ResultSpecification aResultSpec)
    throws ResultNotSupportedException, AnalysisEngineProcessException;


  /**
   * Similar to {@link #process(CAS)} but uses the Java-object-based 
   * {@link JCas} interface instead of the general {@link CAS} interface.
   * 
   * @param aJCas the JCas containing the inputs to the processing.  Analysis
   *    results will also be written to this JCas.
   *
   * @return an object containing information about which
   *   AnalysisEngine components have executed and information, such as timing, 
   *   about that execution.
   *  
   * @throws AnalysisEngineProcessException  if a failure occurs during processing.     
   */
  public ProcessTrace process(JCas aJCas) 
    throws AnalysisEngineProcessException;
    
  /**
   * Similar to {@link #process(CAS,ResultSpecification)} but uses the 
   * Java-object-based {@link JCas} interface instead of the general 
   * {@link CAS} interface.
   * 
   * @param aJCas the JCas containing the inputs to the processing.  Analysis
   *    results will also be written to this JCas.
   * @param aResultSpec  a list of outputs that this AnalysisEngine should 
   *    produce.
   *
   * @return an object containing information about which
   *   AnalysisEngine components have executed and information, such as timing, 
   *   about that execution.
   *  
   * @throws ResultNotSupportedException  if this AnalysisEngine is not 
   *    capable of producing the results requested in <code>aResultSpec</code>. 
   * @throws AnalysisEngineProcessException  if a failure occurs during processing.     
   */
  public ProcessTrace process(JCas aJCas, ResultSpecification aResultSpec) 
    throws ResultNotSupportedException, AnalysisEngineProcessException;

  /**
   * Similar to {@link #process(CAS)} but uses the Java-object-based 
   * {@link JCas} interface instead of the general {@link CAS} interface.
   * 
   * @param aJCas the JCas containing the inputs to the processing.  Analysis
   *    results will also be written to this JCas.
   * @param aResultSpec  a list of outputs that this AnalysisEngine should 
   *    produce.
   * @param aTrace the object to which trace events will be recorded
   *  
   * @throws ResultNotSupportedException  if this AnalysisEngine is not 
   *    capable of producing the results requested in <code>aResultSpec</code>. 
   * @throws AnalysisEngineProcessException  if a failure occurs during processing.     
   */
  public void process(JCas aJCas, ResultSpecification aResultSpec, 
                      ProcessTrace aTrace) 
    throws ResultNotSupportedException, AnalysisEngineProcessException;

  /**
   * Processes a CAS, possibly producing multiple CASes as a result.  
   * The application uses the {@link  CasIterator} interface to step through the 
   * output CASes. 
   * <p>
   * If this Analysis Engine does not produce output CASes, then the 
   * <code>CasIterator</code> will return no elements.  You can check if an
   * AnalysisEngine is capable of producing output CASes by checking the
   * {@link org.apache.uima.resource.metadata.OperationalProperties#getOutputsNewCASes()}
   * operational property 
   * (<code>getAnalysisEngineMetaData().getOperationalProperties().getOutputsNewCASes()</code>).
   * <p>
   * Once this method is called, the AnalysisEngine "owns" <code>aCAS</code>
   * until such time as the {@link CasIterator#hasNext()} method returns false.
   * That is, the caller should not attempt to modify or access the input CAS until
   * it has read all of the elements from the CasIterator.  If the caller wants to 
   * abort the processing before having read all of the output CASes, it may call 
   * {@link CasIterator#release()}, which will stop further processing from 
   * occurring, and ownership of <code>aCAS</code> will revert to the caller.
   *
   * @param aCAS the CAS to be processed
   * 
   * @return an object for iterating through any output CASes
   * 
   * @throws AnalysisEngineProcessException if a failure occurs during processing
   */
  CasIterator processAndOutputNewCASes(CAS aCAS) throws AnalysisEngineProcessException;

  /**
   * Processes a JCAS, possibly producing multiple JCASes as a result.  
   * The application uses the {@link JCasIterator} interface to step through the 
   * output JCASes. 
   * <p>
   * If this Analysis Engine does not produce output CASes, then the 
   * <code>CasIterator</code> will return no elements.  You can check if an
   * AnalysisEngine is capable of producing output CASes by checking the
   * {@link org.apache.uima.resource.metadata.OperationalProperties#getOutputsNewCASes()}
   * operational property 
   * (<code>getAnalysisEngineMetaData().getOperationalProperties().getOutputsNewCASes()</code>).
   * <p>
   * Once this method is called, the AnalysisEngine "owns" <code>aJCAS</code>
   * until such time as the {@link JCasIterator#hasNext()} method returns false.
   * That is, the caller should not attempt to modify or access the input JCAS until
   * it has read all of the elements from the JCasIterator.  outputCASes, it may call 
   * {@link JCasIterator#release()}, which will stop further processing from 
   * occurring, and ownership of <code>aJCAS</code> will revert to the caller.
   *
   * @param aJCAS the JCAS to be processed
   * 
   * @return an object for iterating through any output JCASes
   * 
   * @throws AnalysisEngineProcessException if a failure occurs during processing
   */
  JCasIterator processAndOutputNewCASes(JCas aJCAS) throws AnalysisEngineProcessException;

  /**
   * Notifies this AnalysisEngine that processing of a batch has
   * completed.  It is up to the caller to determine the size of a batch.
   * Components (particularly CAS Consumers) inside this Analysis Engine
   * may respond to this event, for example by writing data to the disk.
   *
   * @throws AnalysisEngineProcessException if an exception occurs during processing
   */
  public void batchProcessComplete()
    throws AnalysisEngineProcessException;


  /**
   * Notifies this AnalysisEngine that processing of an entire collection has
   * completed.  It is up to the caller to determine when this has occurred.
   * Components (particularly CAS Consumers) inside this Analysis Engine
   * may respond to this event, for example by writing data to the disk.
   *
   * @throws AnalysisEngineProcessException if an exception occurs during processing
   */
  public void collectionProcessComplete()
    throws AnalysisEngineProcessException;
  
  /**
   * A factory method used to create an instance of 
   * {@link ResultSpecification} for use with this AnalysisEngine.  Applications
   * use this method to construct <code>ResultSpecification</code>s to pass
   * to this AnalysisEngine's {@link #process(CAS,ResultSpecification)} method.
   * 
   * @return a new instance of <code>ResultSpecification</code>
   */
  public ResultSpecification createResultSpecification();


  /**
   * Gets the {@link ResourceManager} used by this AnalysisEngine.  
   * 
   * @return this AnalysisEngine's ResourceManager
   */
  public ResourceManager getResourceManager();
	
  /**
   * Reconfigures this Resource using the most current parameter settings.
   * Calls to {@link #setConfigParameterValue(String,String,Object)} do not
   * take effect until this method is called.
   * <p>
   * If this is an aggregate Analysis Engine, configuration parameter settings
   * will be "pushed down" to delegate Analysis Engines that also declare those
   * parameters.  All annotators will be informed of the change by a call to their 
   * {@link org.apache.uima.analysis_engine.annotator.BaseAnnotator#reconfigure()}
   * methods.
   * 
   * @throws ResourceConfigurationException if the configuration is not valid
   */
  public void reconfigure() throws ResourceConfigurationException;

  /**
   * Gets the Logger that this Analysis Engine is currently using.
   *  
   * @return this AnalysisEngine's logger
   */
  public Logger getLogger();

  /**
   * Sets the Logger that this Analysis Engine will use.  If this method is
   * not called, the default logger 
   * ({@link org.apache.uima.UIMAFramework#getLogger()}) will be used.
   *  
   * @param aLogger the logger for this Analysis Engine to use
   */
  public void setLogger(Logger aLogger);

  /**
   * Gets the names of the features that are defined on one of the CAS types
   * that this AE inputs or outputs.  When a AE's capabilities are declared
   * with <code>allAnnotatorFeatures == true</code>, this method can be used
   * to determine all of the feature names.
   *  
   * @param aTypeName type for which to get features
   * @return an array of feature names.  If <code>aTypeName</code> is not
   *   defined, <code>null</code> will be returned.
   */
  public String[] getFeatureNamesForType(String aTypeName);

  /**
   * Gets the performance tuning settings in effect for this Analysis Engine.
   * @return performance tuning settings
   */
  public Properties getPerformanceTuningSettings();
  
  /**
   * Sets the list of output types and features that the application wants
   * this AnalysisEngine to produce.  This is only a guideline.  Annotators may 
   * use this information to avoid doing unnecessary work, but they are
   * not required to do so.
   *  
   * @param aResultSpec specifies the list of output types and features that the
   *   application is interested in.
   */
  public void setResultSpecification(ResultSpecification aResultSpec);
  
  
  /**
   * Gets an object that can be used to do monitoring or management
   * of this AnalysisEngine.  
   *  
   * @return an object exposing a management interface to this AE
   */
  public AnalysisEngineManagement getManagementInterface();
  
  /**
   * Key for the initialization parameter whose value is a reference to
   * the {@link ResourceManager} that this AnalysisEngine should use.  This
   * value is used as a key in the <code>aAdditionalParams</code> Map that is 
   * passed to the {@link #initialize(ResourceSpecifier,Map)} method.
   */
  public static final String PARAM_RESOURCE_MANAGER = "RESOURCE_MANAGER";


  /**
   * Key for the initialization parameter whose value is a
   * {@link ConfigurationParameterSettings} object that holds configuration
   * settings that will be used to configure this AnalysisEngine, overridding
   * any conflicting settings specified in this AnalysisEngine's Descriptor.
   * This value is used as a key in the <code>aAdditionalParams</code> Map that 
   * is passed to the {@link #initialize(ResourceSpecifier,Map)} method.
   */
  public static final String PARAM_CONFIG_PARAM_SETTINGS = "CONFIG_PARAM_SETTINGS";  

  /**
   * Key for the initialization parameter whose value is the number of
   * simultaneous calls to {@link #process(CAS,ResultSpecification)} that
   * will be supported.  Analysis Engine implementations may use this to
   * create a pool of objects (e.g. Annotators), each of which can process
   * only one request at a time.  Applications should be careful about setting
   * this number higher than is necessary, since large pools of objects may
   * increase initialiation time and consume resources.  Not all analysis
   * engine implementations pay attention to this parameter. 
   * <p> 
   * This value is used as a key in the <code>aAdditionalParams</code> Map that 
   * is passed to the {@link #initialize(ResourceSpecifier,Map)} method.
   */
  public static final String PARAM_NUM_SIMULTANEOUS_REQUESTS = "NUM_SIMULTANEOUS_REQUESTS";  

  /**
   * Key for the initialization parameter whose value is the maximum number of
   * milliseconds to wait for a pooled object 
   * (see {@link #PARAM_NUM_SIMULTANEOUS_REQUESTS}) to become available to
   * serve a {@link #process(CAS,ResultSpecification)} request.  If the
   * processing has not begun within this time, an exception will be thrown.
   * A value of zero will cause the AnalysisEngine to wait forever. Not all 
   * analysis* engine implementations pay attention to this parameter.
   * <p> 
   * This value is used as a key in the <code>aAdditionalParams</code> Map that 
   * is passed to the {@link #initialize(ResourceSpecifier,Map)} method.
   */
  public static final String PARAM_TIMEOUT_PERIOD = "TIMEOUT_PERIOD";  

  /**
   * Key for the initialization parameter whose value is 
   * a JMX MBeanServer instance, with which this AnalysisEngine will register
   * an MBean that allows monitoring of the AE's performance through JMX.
   * If this is null, the platform MBean Server (Java 1.5 only) will be used.
   *  
   * <p> 
   * This value is used as a key in the <code>aAdditionalParams</code> Map that 
   * is passed to the {@link #initialize(ResourceSpecifier,Map)} method.
   */
  public static final String PARAM_MBEAN_SERVER = "MBEAN_SERVER";  
}
