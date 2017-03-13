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

package org.apache.uima.collection.impl.cpm.utils;

import java.io.IOException;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.annotator.AnnotatorConfigurationException;
import org.apache.uima.analysis_engine.annotator.AnnotatorInitializationException;
import org.apache.uima.analysis_engine.annotator.AnnotatorProcessException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;


public class FunctionErrorStore {

  private static final String LS = System.getProperties().getProperty("line.separator");

  private static int allCountedExceptions = 0;

  private String functionError = "Exception";

  private String functionName = "";

  private int functionCounter = 0;

  private int functionCounted = 0;

  private static int collectionReaderCount = 0;

  private static int collectionReaderGetNextCount = 0;

  private static int annotatorCount = 0;

  private static int annotatorProcessCount = 0;

  private static int casConsumerCount = 0;

  private static int casConsumerProcessCount = 0;

  private Logger logger;

  private final static Level LOG_LEVEL = Level.SEVERE;

  protected FunctionErrorStore(String exception, int functionCount, String functionName) {
    functionCounter = functionCount;
    functionError = exception;
    this.functionName = functionName;
    logger = UIMAFramework.getLogger(this.getClass());
    // logger = Logger_impl.getInstance();
  }

  // exceptions from JTextAnnotator_ImplBase.process
  public synchronized void methodeCalled1() throws AnnotatorProcessException {
    functionCounted++;

    if (functionCounted >= functionCounter) {
      exceptionThrown();
      logger.log(LOG_LEVEL, "the function " + functionName
              + " is trying to throw the following exception: " + functionError);
      if (functionError.equals("AnnotatorProcessException")) {
        throw new AnnotatorProcessException();
      } else {
        throwAnException(functionError);
      }
    }
  }

  // exceptions from JTextAnnotator_ImplBase.initialize and JTextAnnotator_ImplBase.reconfigure
  public synchronized void methodeCalled2() throws AnnotatorConfigurationException,
          AnnotatorInitializationException {
    functionCounted++;

    if (functionCounted >= functionCounter) {
      exceptionThrown();
      logger.log(LOG_LEVEL, "the function " + functionName
              + " is trying to throw the following exception: " + functionError);
      if (functionError.equals("AnnotatorConfigurationException")) {
        throw new AnnotatorConfigurationException();
      } else if (functionError.equals("AnnotatorInitializationException")) {
        throw new AnnotatorInitializationException();
      } else {
        throwAnException(functionError);
      }
    }
  }

  // exceptions from ErrorTestCasConsumer.processCas
  public synchronized void methodeCalled3() throws ResourceProcessException {
    functionCounted++;

    if (functionCounted >= functionCounter) {
      exceptionThrown();
      logger.log(LOG_LEVEL, "the function " + functionName
              + " is trying to throw the following exception: " + functionError);
      if (functionError.equals("ResourceProcessException")) {
        throw new ResourceProcessException();
      } else {
        throwAnException(functionError);
      }
    }
  }

  // exceptions from ErrorTestCasConsumer.initialize
  public synchronized void methodeCalled4() throws ResourceInitializationException {
    functionCounted++;

    if (functionCounted >= functionCounter) {
      exceptionThrown();
      logger.log(LOG_LEVEL, "the function " + functionName
              + " is trying to throw the following exception: " + functionError);
      if (functionError.equals("ResourceInitializationException")) {
        throw new ResourceInitializationException();
      } else {
        throwAnException(functionError);
      }
    }
  }

  // exceptions from ErrorTestCollectionReader.initialize
  public synchronized void methodeCalled5() throws ResourceInitializationException {
    functionCounted++;

    if (functionCounted >= functionCounter) {
      exceptionThrown();
      logger.log(LOG_LEVEL, "the function " + functionName
              + " is trying to throw the following exception: " + functionError);
      if (functionError.equals("ResourceInitializationException")) {
        throw new ResourceInitializationException();
      } else {
        throwAnException(functionError);
      }
    }
  }

  // exceptions from ErrorTestCasConsumer.initialize
  public synchronized void methodeCalled6() throws IOException, CollectionException {
    functionCounted++;

    if (functionCounted >= functionCounter) {
      exceptionThrown();
      logger.log(LOG_LEVEL, "the function " + functionName
              + " is trying to throw the following exception: " + functionError);
      if (functionError.equals("IOException")) {
        throw new IOException();
      } else if (functionError.equals("CollectionException")) {
        throw new CollectionException();
      } else {
        throwAnException(functionError);
      }
    }
  }

  // exceptions from ErrorTestCollectionReader.initialize
  public synchronized void methodeCalled7() throws IOException {
    functionCounted++;

    if (functionCounted >= functionCounter) {
      exceptionThrown();
      logger.log(LOG_LEVEL, "the function " + functionName
              + " is trying to throw the following exception: " + functionError);
      if (functionError.equals("IOException")) {
        throw new IOException();
      } else {
        throwAnException(functionError);
      }
    }
  }

  // exceptions from ErrorTestCollectionReader.initialize
  public synchronized void methodeCalled8() {
    functionCounted++;

    if (functionCounted >= functionCounter) {
      exceptionThrown();
      logger.log(LOG_LEVEL, "the function " + functionName
              + " is trying to throw the following exception: " + functionError);
      throwAnException(functionError);
    }
  }

  // runtime exceptions
  private void throwAnException(String exception) {
    if (exception.equals("IndexOutOfBoundsException")) {
      throw new IndexOutOfBoundsException();
    } else if (exception.equals("OutOfMemoryError")) {
      throw new OutOfMemoryError();
    } else if (exception.equals("NullPointerException")) {
      throw new NullPointerException();
    } else if (exception.equals("RuntimeException")) {
      throw new RuntimeException();
    }
  }

  /**
   * indicates that an exception was or will be thrown
   */
  private void exceptionThrown() {
    allCountedExceptions++; // all counted exception
    functionCounted = 0; // function call counter since the last exception
    // System.out.println("Exception: " + allCountedExceptions);
  }

  /**
   * reset all static values for a new test cycle for instance
   */
  public static synchronized void resetCount() {
    allCountedExceptions = 0;
    collectionReaderCount = 0;
    annotatorCount = 0;
    casConsumerCount = 0;
    casConsumerProcessCount = 0;
    annotatorProcessCount = 0;
    collectionReaderGetNextCount = 0;
  }

  /**
   * @return allCountedExceptions since last run
   */
  public static int getCount() {
    return allCountedExceptions;
  }

  /**
   * @return the number of annotators
   */
  public static int getAnnotatorCount() {
    return annotatorCount;
  }

  /**
   * @return the CasConsumer count calls
   */
  public static int getCasConsumerCount() {
    return casConsumerCount;
  }

  /**
   * @return the number of CollectionReader instances
   */
  public static int getCollectionReaderCount() {
    return collectionReaderCount;
  }

  /**
   * increases the (instance) count of annotators
   */
  public static synchronized void increaseAnnotatorCount() {
    annotatorCount++;
  }

  /**
   * increases the (instance) count of CasConsumers
   */
  public static synchronized void increaseCasConsumerCount() {
    casConsumerCount++;
  }

  /**
   * increase the (instance) count of CollectionReaders
   */
  public static synchronized void increaseCollectionReaderCount() {
    collectionReaderCount++;
  }

  /**
   * increase the CasConsumer 'process'-methode calls count
   */
  public static synchronized void increaseCasConsumerProcessCount() {
    casConsumerProcessCount++;
  }

  /**
   * @return the number of process calls
   */
  public static int getCasConsumerProcessCount() {
    return casConsumerProcessCount;
  }

  /**
   * increase the count of 'process'-methode calls for the Annotator
   */
  public static synchronized void increaseAnnotatorProcessCount() {
    annotatorProcessCount++;
  }

  /**
   * @return the number of 'process'-methode calls
   */
  public static int getAnnotatorProcessCount() {
    return annotatorProcessCount;
  }

  /**
   * increase the count of the 'getNext'-methode calls for the CollectionReader
   */
  public static synchronized void increaseCollectionReaderGetNextCount() {
    collectionReaderGetNextCount++;
  }

  /**
   * @return the number of 'getNext'-methode calls
   */
  public static int getCollectionReaderGetNextCount() {
    return collectionReaderGetNextCount;
  }

  public static String printStats() {
    StringBuffer sb = new StringBuffer();
    sb.append("All counted Exceptions: " + allCountedExceptions + LS);
    sb.append("CollectionReader instances: " + collectionReaderCount + LS);
    sb
            .append("CollectionReader 'getNext'-methode call count: "
                    + collectionReaderGetNextCount + LS);
    sb.append("Annotator instances: " + annotatorCount + LS);
    sb.append("Annotator 'process'-methode call count: " + annotatorProcessCount + LS);
    sb.append("CasConsumer instances: " + casConsumerCount + LS);
    sb.append("CasConsumer 'process'-methode call count:" + casConsumerProcessCount + LS);
    return sb.toString();
  }
}
