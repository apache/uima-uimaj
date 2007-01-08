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

package org.apache.uima.uimacpp;

import java.io.IOException;
import java.io.StringWriter;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UIMARuntimeException;
import org.apache.uima.UimaContext;
import org.apache.uima.UimaContextAdmin;
import org.apache.uima.analysis_component.AnalysisComponent_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.analysis_engine.annotator.AnnotatorConfigurationException;
import org.apache.uima.analysis_engine.annotator.AnnotatorInitializationException;
import org.apache.uima.analysis_engine.annotator.AnnotatorProcessException;
import org.apache.uima.analysis_engine.impl.AnalysisEngineImplBase;
import org.apache.uima.analysis_engine.metadata.SofaMapping;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.ComponentInfo;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.admin.CASMgr;
import org.apache.uima.cas.impl.CASMgrSerializer;
import org.apache.uima.cas.impl.Serialization;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.resource.metadata.ConfigurationParameterSettings;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;
import org.apache.uima.util.ProcessTrace;
import org.xml.sax.SAXException;

public class UimacppAnalysisComponent extends AnalysisComponent_ImplBase {

  private UimacppEngine engine;

  private AnalysisEngineImplBase ae;

  private AnalysisEngineDescription aeDescription;

  private Logger log;

  private boolean tsReinit;

  private UimaContext uimaContext;

  /**
   * resource bundle for log messages
   */
  private static final String LOG_RESOURCE_BUNDLE = "org.apache.uima.impl.log_messages";

  /**
   * current class
   */
  private static final Class CLASS_NAME = UimacppAnalysisComponent.class;

  public UimacppAnalysisComponent(AnalysisEngineDescription aeDescription, AnalysisEngineImplBase ae) {
    super();
    this.ae = ae;
    this.aeDescription = aeDescription;
    // TAF won't except the new <fsIndexCollection> element, but actuall it doesn't need it,
    // because the index definitions are transmitted with the serialized CAS. So we can
    // just null it out.
    this.aeDescription.getAnalysisEngineMetaData().setFsIndexCollection(null);
    this.tsReinit = true;
    // System.out.println("Data path: " + dataPath);
  }

  /**
   * @throws ResourceInitializationException
   * @see org.apache.uima.analysis_engine.annotator.BaseAnnotator#initialize(org.apache.uima.cas.CAS,
   *      org.apache.uima.analysis_engine.annotator.AnnotatorContext)
   */
  public void initialize(UimaContext context) throws ResourceInitializationException {
    try {
      this.uimaContext = context;
      // update the sofa mappings in the AE descriptor with the mappings
      // specified in the context if the AE descriptor is for an aggregate
      // Ae and contains sofa mappings
      if (!aeDescription.isPrimitive()) {
        ComponentInfo compInfo = ((UimaContextAdmin) context).getComponentInfo();
        SofaMapping[] aggSofaMapping = aeDescription.getSofaMappings();
        if (aggSofaMapping != null && aggSofaMapping.length > 0) {
          for (int i = 0; i < aggSofaMapping.length; i++) {
            String absoluteSofaName = compInfo
                    .mapToSofaID(aggSofaMapping[i].getAggregateSofaName());
            aggSofaMapping[i].setAggregateSofaName(absoluteSofaName);
          }
        }
      }
      this.log = context.getLogger();
      if (engine == null) {
        UimacppEngine.configureResourceManager(System.getProperty("java.io.tmpdir"), ae
                .getResourceManager().getDataPath());

        StringWriter strWriter = new StringWriter();
        aeDescription.toXML(strWriter);
        strWriter.close();
        engine = UimacppEngine.createJTafTAE(strWriter.getBuffer().toString());
      }
    } catch (UimacppException e) {
      logJTafException(e);
      throw new ResourceInitializationException(
              ResourceInitializationException.ERROR_INITIALIZING_FROM_DESCRIPTOR, new Object[] {
                  aeDescription.getAnalysisEngineMetaData().getName(),
                  aeDescription.getSourceUrlString() });
    } catch (SAXException e) {
      throw new ResourceInitializationException(
              ResourceInitializationException.ERROR_INITIALIZING_FROM_DESCRIPTOR, new Object[] {
                  aeDescription.getAnalysisEngineMetaData().getName(),
                  aeDescription.getSourceUrlString() });
    } catch (IOException e) {
      throw new ResourceInitializationException(
              ResourceInitializationException.ERROR_INITIALIZING_FROM_DESCRIPTOR, new Object[] {
                  aeDescription.getAnalysisEngineMetaData().getName(),
                  aeDescription.getSourceUrlString() });
    }
  }

  /**
   * @see org.apache.uima.analysis_engine.annotator.BaseAnnotator#typeSystemInit()
   */
  public void typeSystemInit(TypeSystem ts) throws AnnotatorConfigurationException,
          AnnotatorInitializationException {
    // set flag to update TAF type system on next call to process
    this.tsReinit = true;
  }

  /**
   * @see org.apache.uima.analysis_engine.annotator.BaseAnnotator#reconfigure(java.lang.String[])
   */
  public void reconfigure() {
    // destroy engine; it will be reinitialized on next call to process
    destroy();
    engine = null;
    // get new config. settings
    ConfigurationParameterSettings settings = ae.getUimaContextAdmin().getConfigurationManager()
            .getCurrentConfigParameterSettings(ae.getUimaContextAdmin().getQualifiedContextName());
    aeDescription.getAnalysisEngineMetaData().setConfigurationParameterSettings(settings);
  }

  /**
   * @see org.apache.uima.analysis_engine.annotator.BaseAnnotator#process(org.apache.uima.analysis_engine.ResultSpecification)
   */
  public void process(CAS cas, ResultSpecification aResultSpec) throws AnnotatorProcessException {
    try {
      if (engine == null) {
        UimacppEngine.configureResourceManager(System.getProperty("java.io.tmpdir"), ae
                .getResourceManager().getDataPath());

        StringWriter strWriter = new StringWriter();
        aeDescription.toXML(strWriter);
        strWriter.close();
        engine = UimacppEngine.createJTafTAE(strWriter.getBuffer().toString());
        this.tsReinit = true;
      }
      if (this.tsReinit) {
        this.tsReinit = false;
        CASMgrSerializer serializer = Serialization.serializeCASMgr((CASMgr) cas);
        engine.typeSystemInit(serializer);
      }
      engine.process(aResultSpec, cas, false);
    } catch (UimacppException e) {
      logJTafException(e);
      throw new AnnotatorProcessException(e);
    } catch (SAXException e) {
      throw new AnnotatorProcessException(e);
    } catch (IOException e) {
      throw new AnnotatorProcessException(e);
    }
  }

  public void process(CAS aCAS) throws AnalysisEngineProcessException {
    // TODO Auto-generated method stub
    try {
      process(aCAS, null);
    } catch (AnnotatorProcessException e) {
      throw new AnalysisEngineProcessException(e);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_component.AnalysisComponent#process(org.apache.uima.cas.AbstractCas)
   */
  public void process(AbstractCas aCAS) throws AnalysisEngineProcessException {
    this.process((CAS) aCAS);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_component.AnalysisComponent#hasNext()
   */
  public boolean hasNext() throws AnalysisEngineProcessException {
    try {
      if (engine != null) {
        // System.out.println("hasNext() " + engine.hasNext());
        return engine.hasNext();
      }
    } catch (UimacppException e) {
      logJTafException(e);
      throw new UIMARuntimeException(e);
    }
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_component.AnalysisComponent#next()
   */
  public AbstractCas next() throws AnalysisEngineProcessException {

    try {
      if (engine != null) {
        CAS cas = getEmptyCAS();
        engine.next(cas);
        return cas;
      }
    } catch (UimacppException e) {
      logJTafException(e);
      throw new UIMARuntimeException(e);
    }
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_component.AnalysisComponent#getRequiredCasInterface()
   */
  public Class getRequiredCasInterface() {
    return CAS.class;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_component.AnalysisComponent#getCasInstancesRequired()
   */
  public int getCasInstancesRequired() {
    return 1;
  }

  /**
   * 
   */
  public void batchProcessComplete() throws AnalysisEngineProcessException {
    try {
      if (engine != null) {
        engine.batchProcessComplete();
      }
    } catch (UimacppException e) {
      logJTafException(e);
      throw new UIMARuntimeException(e);
    }
  }

  /**
   * 
   */
  public void collectionProcessComplete() throws AnalysisEngineProcessException {
    try {
      if (engine != null) {
        engine.collectionProcessComplete();
      }
    } catch (UimacppException e) {
      logJTafException(e);
      throw new UIMARuntimeException(e);
    }
  }

  /**
   * @see org.apache.uima.analysis_engine.annotator.BaseAnnotator#destroy()
   */
  public void destroy() {
    try {
      if (engine != null) {
        engine.destroy();
        engine = null;
      }
    } catch (UimacppException e) {
      logJTafException(e);
      throw new UIMARuntimeException(e);
    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#finalize()
   */
  protected void finalize() throws Throwable {
    destroy();
  }

  // -------------------------------------------------------------
  // These methods are need to support logging of TAF annotator
  // messages using the Java logger mechanism.
  // -------------------------------------------------------------

  /**
   * Get the logging level of the logger for TAFAnnotator. TAF only supports three levels of
   * logging. All logging levels INFO and below are mapped to the TAF message level.
   */
  public static int getLoggingLevel() {

    Logger uimacppLogger = UIMAFramework.getLogger(UimacppAnalysisComponent.class);

    if (uimacppLogger.isLoggable(Level.FINEST) || uimacppLogger.isLoggable(Level.FINER)
            || uimacppLogger.isLoggable(Level.FINE) || uimacppLogger.isLoggable(Level.CONFIG)
            || uimacppLogger.isLoggable(Level.INFO)) {
      return TAF_LOGLEVEL_MESSAGE;
    } else if (uimacppLogger.isLoggable(Level.WARNING)) {
      return TAF_LOGLEVEL_WARNING;
    } else if (uimacppLogger.isLoggable(Level.SEVERE)) {
      return TAF_LOGLEVEL_ERROR;
    } else {
      return TAF_LOGLEVEL_OFF;
    }
  }

  // log a message
  public static void log(int msglevel, String sourceClass, String sourceMethod, String message) {
    Logger uimacppLogger = UIMAFramework.getLogger(UimacppAnalysisComponent.class);
    Level level = Level.INFO; // default
    if (msglevel == TAF_LOGLEVEL_MESSAGE) {
      level = Level.INFO;
    } else if (msglevel == TAF_LOGLEVEL_WARNING) {
      level = Level.WARNING;
    } else if (msglevel == TAF_LOGLEVEL_ERROR) {
      level = Level.SEVERE;
    }
    if (sourceMethod.length() > 0)
      uimacppLogger.log(level, sourceClass + "::" + sourceMethod + ": " + message);
    else
      uimacppLogger.log(level, sourceClass + ": " + message);

    // TODO: add Logger method log(level, sourceClass, sourceMethod, message);
  }

  private static final int TAF_LOGLEVEL_OFF = 0;

  private static final int TAF_LOGLEVEL_MESSAGE = 1;

  private static final int TAF_LOGLEVEL_WARNING = 2;

  private static final int TAF_LOGLEVEL_ERROR = 3;

  private void logJTafException(UimacppException e) {
    if (e.getEmbeddedException() instanceof InternalTafException) {
      InternalTafException tafExc = (InternalTafException) e.getEmbeddedException();
      long errorCode = tafExc.getTafErrorCode();
      String errorName = "";
      try {
        errorName = UimacppEngine.getErrorMessage(errorCode);
      } catch (UimacppException jtafexc) {
        log.logrb(Level.SEVERE, CLASS_NAME.getName(), "logJTafException", LOG_RESOURCE_BUNDLE,
                "UIMA_error_while_getting_name__SEVERE", jtafexc.getMessage());
        errorName = "";
      }

      log.logrb(Level.SEVERE, CLASS_NAME.getName(), "logJTafException", LOG_RESOURCE_BUNDLE,
              "UIMA_taf_internal_exception__SEVERE",
              new Object[] { new Long(errorCode), errorName });
    }
    Exception et = e.getEmbeddedException();

    // log exception
    log.log(Level.SEVERE, et.getMessage(), et);
  }

  protected CAS getEmptyCAS() {
    return (CAS) uimaContext.getEmptyCas(CAS.class);
  }

}
