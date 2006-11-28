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

package org.apache.uima.adapter.vinci;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.logging.Handler;
import java.util.logging.LogManager;

import org.apache.uima.UIMAFramework;
import org.apache.uima.adapter.vinci.util.Constants;
import org.apache.uima.adapter.vinci.util.Descriptor;
import org.apache.uima.adapter.vinci.util.NetworkUtil;
import org.apache.uima.adapter.vinci.util.SaxVinciFrameBuilder;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.SofaFS;
import org.apache.uima.cas.impl.OutOfTypeSystemData;
import org.apache.uima.internal.util.UIMALogFormatter;
import org.apache.uima.internal.util.UIMAStreamHandler;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.metadata.ProcessingResourceMetaData;
import org.apache.uima.util.CasPool;
import org.apache.uima.util.Level;
import org.apache.uima.util.XMLInputSource;
import org.apache.vinci.transport.Frame;
import org.apache.vinci.transport.ServiceDownException;
import org.apache.vinci.transport.ServiceException;
import org.apache.vinci.transport.Transportable;
import org.apache.vinci.transport.VinciFrame;
import org.apache.vinci.transport.VinciServableAdapter;
import org.apache.vinci.transport.VinciServer;
import org.apache.vinci.transport.document.AFrame;

/**
 * Main class for a Vinci Analysis Engine service. This class can also be used to deploy CAS
 * Consumers as Vinci services.
 * 
 * The main method takes one argument - the path to the service deployment descriptor.
 */
public class VinciAnalysisEngineService_impl extends VinciServableAdapter {
  private VinciServer _server = null;

  private AnalysisEngine mAE = null;

  private CasPool mCasPool = null;

  private Descriptor descriptor = null;

  // debug mode flag
  private boolean debug = false;

  private int serviceInstanceId = -1;

  /**
   * Instantiate Analysis Engine from a given descriptor, debug mode, and instance Id
   * 
   * @param aResourceSpecifierPath -
   *          descriptor location
   */
  public VinciAnalysisEngineService_impl(String serviceConfigPath, boolean debug, String instanceId)
          throws Exception {
    this(serviceConfigPath, debug);
    serviceInstanceId = Integer.parseInt(instanceId);
  }

  /**
   * Instantiate Analysis Engine service from a given descriptor - possibly in debug mode.
   * 
   * @param aResourceSpecifierPath -
   *          descriptor location
   */
  public VinciAnalysisEngineService_impl(String serviceConfigPath, boolean debug) throws Exception {
    this.debug = debug;

    UIMAFramework.getLogger().log(Level.FINE, "VinciAnalysisEngineService_impl: constructor");
    // Instantiate an object which holds configuration data: resource
    // specifier path,
    // serializer class, service name, etc
    descriptor = new Descriptor(serviceConfigPath);
    String aResourceSpecifierPath = descriptor.getResourceSpecifierPath();
    UIMAFramework.getLogger().log(Level.CONFIG,
            "Resource Specifier Path::" + aResourceSpecifierPath);

    ResourceSpecifier resourceSpecifier = UIMAFramework.getXMLParser().parseResourceSpecifier(
            new XMLInputSource(aResourceSpecifierPath));

    // create CAS Object Processor
    if (mAE == null) {
      UIMAFramework.getLogger().log(Level.FINE,
              "VinciAnalysisEngineService_impl: creating CAS Processor");
      mAE = UIMAFramework
              .produceAnalysisEngine(resourceSpecifier, descriptor.getInstanceCount(), 0);
    }
    // create pool of CASes
    if (mCasPool == null) {
      mCasPool = new CasPool(descriptor.getInstanceCount(), mAE.getProcessingResourceMetaData());
    }

    // invoke type system init once on CAS Processor
    CAS cas = mCasPool.getCas();
    try {
      mAE.typeSystemInit(cas.getTypeSystem());
    } finally {
      mCasPool.releaseCas(cas);
    }
  }

  /**
   * Instantiate Analysis Engine service from a given descriptor.
   * 
   * @param aResourceSpecifierPath -
   *          descriptor location
   */
  public VinciAnalysisEngineService_impl(String serviceConfigPath) throws Exception {
    this(serviceConfigPath, false);
  }

  /**
   * Extracts AE metadata
   * 
   * @return Frame containing extracted meta data
   * @exception when
   *              there is a failure processing
   */
  private Frame getMetaData() throws Exception {
    UIMAFramework.getLogger().log(Level.FINEST, "VinciAnalysisEngineService.getMetaData()");
    // get metadata
    ProcessingResourceMetaData md = mAE.getProcessingResourceMetaData();
    // convert to vinci frame
    AFrame response = new AFrame();
    SaxVinciFrameBuilder vinciFrameBuilder = new SaxVinciFrameBuilder();
    vinciFrameBuilder.setParentFrame(response);
    vinciFrameBuilder.startDocument();
    md.toXML(vinciFrameBuilder);
    vinciFrameBuilder.endDocument();
    return response;
  }

  public Descriptor getDescriptor() {
    return descriptor;
  }

  /**
   * Analyzes a given document by a AnalysisEngine. When completed this method returns a VinciFrame
   * containing XCAS translated into a set of Vinci subFrames. Each subframe containing one
   * annotation with all its attributes.
   * 
   * @param aRequestFrame
   *          request frame
   * 
   * @return VinciFrame containing XCAS translated into a set of Vinci subframes.
   * @exception Exception
   *              if there is an error during processing
   */
  private Transportable analyze(CASTransportable ct) throws Exception {
    CAS cas = ct.getCas();
    try {
      long annotStartTime = System.currentTimeMillis();
      mAE.process(cas);
      int annotationTime = (int) (System.currentTimeMillis() - annotStartTime);
      if (debug) {
        System.out.println("Annotation took: " + annotationTime + "ms");
      }
      ct.getExtraDataFrame().fset(Constants.ANNOTATION_TIME, annotationTime);
      // Extract CAS
      // UIMAFramework.getLogger().log("CAS ACount::" +
      // cas.getAnnotationIndex().size());
      int totalAnnots = 0;
      SofaFS sofa;
      FSIterator sItr = cas.getSofaIterator();
      while (sItr.isValid()) {
        sofa = (SofaFS) sItr.get();
        totalAnnots += cas.getView(sofa).getAnnotationIndex().size();
        sItr.moveToNext();
      }
      UIMAFramework.getLogger().log(Level.FINEST, "CAS ACount::" + totalAnnots);
      ct.setCommand(null);
      return ct;
    } catch (Exception ex) {
      ct.cleanup();
      throw ex;
    }
  }

  /**
   * Main method called by the Vinci Service Layer. All requests coming in from clients go through
   * this method. Each request comes in as a VinciFrame and is expected to contain a valid
   * VINCI:COMMAND. Currently, two such operations are supported: 1) Annotate - triggers document
   * analysis 2) GetData - triggers return of the AE meta data ( descriptor)
   * 
   * @param {@link org.apache.vinci.transport.Transportable} -
   *          a VinciFrame containing client request
   * @return {@link org.apache.vinci.transport.Transportable} - a VinciFrame containg result of
   *         performing the service
   */

  public Transportable eval(Transportable doc) throws ServiceException {
    try {
      CASTransportable ct = (CASTransportable) doc;
      String op = ct.getCommand();
      if (Constants.GETMETA.equals(op)) {
        ct.cleanup();
        return this.getMetaData();
      } else if (Constants.PROCESS_CAS.equals(op) || Constants.ANNOTATE.equals(op)) {
        return analyze(ct);
      } else if (Constants.BATCH_PROCESS_COMPLETE.equals(op)) {
        ct.cleanup();
        mAE.batchProcessComplete();
        return null; // oneway method call; do NOT return anythingl
        // not even an empty frame
      } else if (Constants.COLLECTION_PROCESS_COMPLETE.equals(op)) {
        ct.cleanup();
        mAE.collectionProcessComplete();
        return new VinciFrame(); // no return value - return empty
        // frame
      } else if (Constants.IS_STATELESS.equals(op)) {
        ct.cleanup();
        return new AFrame().fadd("Result", mAE.isStateless());
      } else if (Constants.IS_READONLY.equals(op)) {
        ct.cleanup();
        return new AFrame().fadd("Result", mAE.isReadOnly());
      } else if (Constants.SHUTDOWN.equals(op)) {
        stop();
        System.exit(1);
      }
      ct.cleanup();
      return new VinciFrame().fadd("Error", "Invalid Operation:" + op);
    } catch (Exception e) {
      e.printStackTrace();
      UIMAFramework.getLogger().log(Level.WARNING, e.getMessage(), e);
      // send back a Vinci frame with an Error key, whose value is the
      // exception message. Be careful not to try to send a null message,
      // since VinciFrame.fadd(key,null) does not add the key at all.
      String msg = e.getMessage();
      if (msg == null) {
        msg = "Error Processing Request";
      }
      return new VinciFrame().fadd("Error", msg);
    }
  }

  /**
   * Starts this service and associates a ShutdownHook to handle gracefull shutdown.
   */
  protected void start() {
    try {
      ShutdownHook shutdownHook = new ShutdownHook(this);
      Runtime.getRuntime().addShutdownHook(shutdownHook);
      String serviceName = getDescriptor().getServiceName();

      // get hostname of this machine, to send to VNS
      String serviceHost = System.getProperty("LOCAL_HOST");
      if (serviceHost == null) {
        serviceHost = NetworkUtil.getLocalHostAddress().getHostAddress();
      }
      if (serviceInstanceId > 0) {
        _server = new VinciServer(serviceName, serviceHost, this, 0, serviceInstanceId);
      } else {

        _server = new VinciServer(serviceName, serviceHost, this);
      }
      UIMAFramework.getLogger().log(
              Level.FINEST,
              "VinciCasObjectProcessorService_impl: Starting Server with Socket Timeout:"
                      + descriptor.getServerSocketTimeout());
      System.out
              .println("VinciCasObjectProcessorService_impl: Starting Server with Socket Timeout:"
                      + descriptor.getServerSocketTimeout());
      _server.setSocketTimeout(descriptor.getServerSocketTimeout());

      _server.serve();
    } catch (ServiceDownException e) {
      UIMAFramework.getLogger().log(Level.SEVERE, e.getMessage());
      System.out.println("\nFailed to contact VNS! Make sure you've specified the correct "
              + "VNS_HOST and that VNS is up and running.");
    }

    catch (Exception e) {
      e.printStackTrace();
    }

    System.exit(1);
  }

  /**
   * Terminate this service
   */
  public void stop() {
    try {
      if (_server != null) {
        _server.shutdown(Constants.SHUTDOWN_MSG);
        UIMAFramework.getLogger().log(Level.INFO, Constants.SHUTDOWN_MSG);
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  public static void main(String[] args) {
    try {
      // check arguments
      if (args.length < 1) {
        System.err.println("Usage: java " + VinciAnalysisEngineService_impl.class.getName()
                + " <deployment descriptor file>");
        System.exit(1);
      }
      File descriptorFile = new File(args[0]);
      if (!descriptorFile.exists()) {
        System.err.println("Deployment descriptor \"" + args[0] + "\" does not exist.");
        System.exit(1);
      }

      // are we in debug mode?
      boolean debug = System.getProperty("DEBUG") != null;

      // If VNS_HOST system property is not set, use default value
      if (System.getProperty("VNS_HOST") == null) {
        System.out.println("No VNS_HOST specified; using default " + Constants.DEFAULT_VNS_HOST);
        System.setProperty("VNS_HOST", Constants.DEFAULT_VNS_HOST);
      }
      String logFile = System.getProperty("LOG");
      if (logFile != null) {
        if (logFile.equalsIgnoreCase("stdout")) {
          redirectLoggerOutput(System.out);
        } else {
          redirectLoggerOutput(new FileOutputStream(logFile));
        }
      }
      VinciAnalysisEngineService_impl vinciService;

      if (args != null && args.length > 1) {
        vinciService = new VinciAnalysisEngineService_impl(descriptorFile.toString(), debug,
                args[1]);
      } else {

        vinciService = new VinciAnalysisEngineService_impl(descriptorFile.toString(), debug);
      }
      vinciService.start();
    } catch (Exception ex) {
      UIMAFramework.getLogger().log(Level.SEVERE, ex.getMessage(), ex);
      ex.printStackTrace();
    }
  }

  /**
   * Redirects all logger output for this JVM to the given output stream.
   */
  private static void redirectLoggerOutput(OutputStream out) {
    // get root logger handlers - root logger is parent of all loggers
    Handler[] handlers = LogManager.getLogManager().getLogger("").getHandlers();

    // remove all current handlers
    for (int i = 0; i < handlers.length; i++) {
      LogManager.getLogManager().getLogger("").removeHandler(handlers[i]);
    }

    // add new UIMAStreamHandler with the given output stream
    UIMAStreamHandler streamHandler = new UIMAStreamHandler(out, new UIMALogFormatter());
    streamHandler.setLevel(java.util.logging.Level.ALL);
    LogManager.getLogManager().getLogger("").addHandler(streamHandler);
  }

  /**
   * Class that handles service shutdowns (including Ctrl-C)
   * 
   */
  static class ShutdownHook extends Thread {
    VinciAnalysisEngineService_impl server;

    public ShutdownHook(VinciAnalysisEngineService_impl instance) {
      server = instance;
    }

    public void run() {
      server.stop();
    }
  }

  /**
   * @see org.apache.vinci.transport.TransportableFactory#makeTransportable()
   */
  public synchronized Transportable makeTransportable() {
    return new CASTransportable(mCasPool, new OutOfTypeSystemData(), null, false);
  }

}
