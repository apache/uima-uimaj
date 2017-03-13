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

package org.apache.uima.collection.impl.cpm;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.collection.impl.metadata.cpe.CpeDescriptorFactory;
import org.apache.uima.collection.metadata.CasProcessorErrorHandling;
import org.apache.uima.collection.metadata.CpeCasProcessor;
import org.apache.uima.collection.metadata.CpeCasProcessors;
import org.apache.uima.collection.metadata.CpeCollectionReader;
import org.apache.uima.collection.metadata.CpeCollectionReaderCasInitializer;
import org.apache.uima.collection.metadata.CpeCollectionReaderIterator;
import org.apache.uima.collection.metadata.CpeComponentDescriptor;
import org.apache.uima.collection.metadata.CpeConfiguration;
import org.apache.uima.collection.metadata.CpeDescription;
import org.apache.uima.collection.metadata.CpeIntegratedCasProcessor;
import org.apache.uima.collection.metadata.CpeLocalCasProcessor;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.XMLInputSource;

/**
 * reading an example Descriptor from FileSystem and adding other CasProcessors via API
 * 
 * After generating a new Descriptor with the modified data, the generated descriptor file must be
 * equivalent to the referenced file.
 */
public class CpeDescriptorSerialization_Test extends TestCase {

  private File testBaseDir;

  private CpeDescription cpeDesc = null;

  /**
   * @see junit.framework.TestCase#setUp()
   */
  protected void setUp() throws Exception {
    // get test base path setting
    this.testBaseDir = JUnitExtension.getFile("CpmTests/CpeAPITest");
  }

  /**
   * test to be sure, that first reading and then writing a discriptor produces the same file as the
   * original.(old configuration file format)
   * 
   * @throws Exception -
   */
  public void testReadDescriptor() throws Exception {

    // input file
    File cpeDescFile = JUnitExtension.getFile("CpmTests/CpeAPITest/refConf.xml");
    XMLInputSource in = new XMLInputSource(cpeDescFile);
    cpeDesc = UIMAFramework.getXMLParser().parseCpeDescription(in);

    // output file
    File outputFile = new File(this.testBaseDir, "outConf.xml");

    // serialize input file to output file
    ByteArrayOutputStream outStream = new ByteArrayOutputStream();
    cpeDesc.toXML(outStream);
    FileOutputStream fOut = new FileOutputStream(outputFile);
    fOut.write(outStream.toByteArray());
    fOut.close();

    // compare input and output
    equal(cpeDescFile, cpeDesc);

    outputFile.delete();
  }

  /**
   * test to be sure, that first reading and then writing a discriptor produces the same file as the
   * original (new configuration file format).
   * 
   * @throws Exception -
   */
  public void testReadDescriptor2() throws Exception {

    // input file
    File cpeDescFile = JUnitExtension.getFile("CpmTests/CpeAPITest/refConf2.xml");
    XMLInputSource in = new XMLInputSource(cpeDescFile);
    cpeDesc = UIMAFramework.getXMLParser().parseCpeDescription(in);

    // output file
    File outputFile = new File(this.testBaseDir, "outConf2.xml");

    // serialize input file to output file
    ByteArrayOutputStream outStream = new ByteArrayOutputStream();
    cpeDesc.toXML(outStream);
    FileOutputStream fOut = new FileOutputStream(outputFile);
    fOut.write(outStream.toByteArray());
    fOut.close();

    equal(cpeDescFile, cpeDesc);

    outputFile.delete();
  }

  /**
   * Create a remote CasProcesser via API and add him to an existing cpe configuration read from a
   * given descriptor from a file. Write the new descriptor back to a file and compare this with an
   * expected descriptor configuration.
   * 
   * @throws Exception -
   */
  public void testAddRemoteCasProcessor() throws Exception {

    // parse cpe descriptor file
    File cpeDescFile = JUnitExtension.getFile("CpmTests/CpeAPITest/refConf3.xml");
    XMLInputSource in = new XMLInputSource(cpeDescFile);
    cpeDesc = UIMAFramework.getXMLParser().parseCpeDescription(in);

    // init files
    File refFile = JUnitExtension.getFile("CpmTests/CpeAPITest/testRemoteDesc.xml");

    // generate a new casProcessor
    CpeCasProcessor casProcessor = CpeDescriptorFactory
            .produceRemoteCasProcessor("myTestCasPocessor");
    // set specifier path
    casProcessor.setDescriptor("any kind of string");
    // add CasProcessor to the CPM descriptor
    cpeDesc.addCasProcessor(casProcessor);

    casProcessor.addDeployParam("ES_REMOTE_DEPLOYMENT_KEY", "ES_REMOTE_DEPLOYMENT_VALUE");

    // these calls should ont add default errorhandling etc. to the casProcessor
    cpeDesc.getCpeCasProcessors().getAllCpeCasProcessors();
    cpeDesc.getCpeCasProcessors().getAllCpeCasProcessors();
    cpeDesc.getCpeCasProcessors().getAllCpeCasProcessors();

    equal(refFile, cpeDesc);

  }

  /**
   * Create a local CasProcesser via API and add him to an existing cpe configuration read from a
   * given descriptor from a file. Write the new descriptor back to a file and compare this with an
   * expected descriptor configuration.
   * 
   * @throws Exception -
   */
  public void testAddLocalCasProcessor() throws Exception {

    // parse cpe descriptor file
    File cpeDescFile = JUnitExtension.getFile("CpmTests/CpeAPITest/refConf3.xml");
    XMLInputSource in = new XMLInputSource(cpeDescFile);
    cpeDesc = UIMAFramework.getXMLParser().parseCpeDescription(in);

    // output file
    File refFile = JUnitExtension.getFile("CpmTests/CpeAPITest/testLocalDesc.xml");

    // Create Detag CasProcessor
    CpeLocalCasProcessor localProcessor = CpeDescriptorFactory.produceLocalCasProcessor(
            "DupShingle Miner", "Detag:DetagContent");
    localProcessor.setDescriptor("c://cpm/annotators/example.xml");
    localProcessor.setExecutable("/some/path/executable.sh");
    localProcessor.addDeployParam("Parm1", "Value1x");
    cpeDesc.addCasProcessor(localProcessor);

    // these calls should ont add default errorhandling etc. to the casProcessor
    cpeDesc.getCpeCasProcessors().getAllCpeCasProcessors();
    cpeDesc.getCpeCasProcessors().getAllCpeCasProcessors();
    cpeDesc.getCpeCasProcessors().getAllCpeCasProcessors();

    equal(refFile, cpeDesc);

  }

  /**
   * Create a integrated CasProcesser via API and add him to an existing cpe configuration read from
   * a given descriptor from a file. Write the new descriptor back to a file and compare this with
   * an expected descriptor configuration.
   * 
   * @throws Exception -
   */
  public void testAddIntegratedCasProcessor() throws Exception {

    // parse cpe descriptor file
    File cpeDescFile = JUnitExtension.getFile("CpmTests/CpeAPITest/refConf3.xml");
    XMLInputSource in = new XMLInputSource(cpeDescFile);
    cpeDesc = UIMAFramework.getXMLParser().parseCpeDescription(in);

    // output file
    File refFile = JUnitExtension.getFile("CpmTests/CpeAPITest/testIntegratedDesc.xml");

    // add a new iCpe
    CpeIntegratedCasProcessor integratedProcessor = CpeDescriptorFactory
            .produceCasProcessor("WF Writer_X");
    integratedProcessor.setDescriptor("c://cpm/conf/consumers/bla.xml");
    integratedProcessor.setBatchSize(99);
    cpeDesc.addCasProcessor(integratedProcessor);

    // every call adds default errorhandling etc. to the casProcessor
    cpeDesc.getCpeCasProcessors().getAllCpeCasProcessors();
    cpeDesc.getCpeCasProcessors().getAllCpeCasProcessors();
    cpeDesc.getCpeCasProcessors().getAllCpeCasProcessors();

    ByteArrayOutputStream outStream = new ByteArrayOutputStream();
    cpeDesc.toXML(outStream);

    equal(refFile, cpeDesc);

  }

  public void equal(File aReferenceDescriptorFile, CpeDescription aGeneratedDescriptor) {
    try {
      
      XMLInputSource in = new XMLInputSource(aReferenceDescriptorFile);
      CpeDescription referenceDesc = UIMAFramework.getXMLParser().parseCpeDescription(in);

      // First Check Collection Reader
      CpeCollectionReader[] readers = referenceDesc.getAllCollectionCollectionReaders();
      if (readers[0] != null) {
        if (aGeneratedDescriptor.getAllCollectionCollectionReaders() == null
                || aGeneratedDescriptor.getAllCollectionCollectionReaders()[0] == null) {

          fail("<collectionReader> element not found in generated CPE Descriptor");
        } else {
          // Collection Reader element is present in the generated descriptor
          if (readers[0].getCollectionIterator() != null) {
            assertTrue(isCollectionIteratorTheSame(readers[0].getCollectionIterator(),
                    aGeneratedDescriptor.getAllCollectionCollectionReaders()[0]
                            .getCollectionIterator()));
          }

          if (readers[0].getCasInitializer() != null) {
            assertTrue(isCasInitializerTheSame(readers[0].getCasInitializer(), aGeneratedDescriptor
                    .getAllCollectionCollectionReaders()[0].getCasInitializer()));

          }
        }
      }
      CpeConfiguration refCpeConfig = referenceDesc.getCpeConfiguration();
      if (refCpeConfig != null) {
        if (aGeneratedDescriptor.getCpeConfiguration() == null) {
          fail("<cpeConfig> element not found in generated CPE Descriptor");
        } else {
          isConfigTheSame(refCpeConfig, aGeneratedDescriptor.getCpeConfiguration());

        }
      }
      CpeCasProcessors refCasProcessors = referenceDesc.getCpeCasProcessors();
      if (refCasProcessors != null) {
        if (aGeneratedDescriptor.getCpeCasProcessors() == null) {
          fail("<casProcessors> element not found in generated CPE Descriptor");
        } else {
          compareCasProcessors(referenceDesc.getCpeCasProcessors(), aGeneratedDescriptor
                  .getCpeCasProcessors());
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void compareCasProcessors(CpeCasProcessors aRefCasProcessors,
          CpeCasProcessors aGenCasProcessors) {
    assertEquals(aRefCasProcessors.getCasPoolSize(), aGenCasProcessors.getCasPoolSize());
    assertEquals(aRefCasProcessors.getConcurrentPUCount(), aGenCasProcessors.getConcurrentPUCount());
    try {
      int expectedCasProcessorCount = aRefCasProcessors.getAllCpeCasProcessors().length;
      if (aGenCasProcessors.getAllCpeCasProcessors() == null) {
        fail("Generated CPE Descriptor does not have any Cas Processors. Expected <"
                + expectedCasProcessorCount + "> Cas Processors");
      } else {
        assertEquals("Generated CPE Descriptor does not have expected number of Cas Processors. "
                + "Expected <" + expectedCasProcessorCount + "> Cas Processors. Got <"
                + aGenCasProcessors.getAllCpeCasProcessors().length + ">",
                expectedCasProcessorCount, aGenCasProcessors.getAllCpeCasProcessors().length);
      }
      CpeCasProcessor[] refCasProcessorList = aRefCasProcessors.getAllCpeCasProcessors();
      for (int i = 0; i < aRefCasProcessors.getAllCpeCasProcessors().length; i++) {
        if (refCasProcessorList[i].getDeployment().equals("integrated")) {
          compareIntegratedCasProcessors(refCasProcessorList[i], aGenCasProcessors
                  .getAllCpeCasProcessors()[i]);
        } else if (refCasProcessorList[i].getDeployment().equals("remote")) {
          compareRemoteCasProcessors(refCasProcessorList[i], aGenCasProcessors
                  .getAllCpeCasProcessors()[i]);
        } else if (refCasProcessorList[i].getDeployment().equals("local")) {
          compareLocalCasProcessors(refCasProcessorList[i], aGenCasProcessors
                  .getAllCpeCasProcessors()[i]);
        }
      }
    } catch (Exception e) {
      fail("Exception::" + e.getMessage()
              + " thrown while retrieving Cas Processors From CPE Descriptor");
    }
  }

  public void compareCommonElements(CpeCasProcessor aRefCasProcessor,
          CpeCasProcessor aGenCasProcessor) {
    assertEquals(aRefCasProcessor.getDeployment(), aGenCasProcessor.getDeployment());
    assertEquals(aRefCasProcessor.getName(), aGenCasProcessor.getName());
    assertEquals(aRefCasProcessor.getDescriptor(), aGenCasProcessor.getDescriptor());
    if (aRefCasProcessor.getDeploymentParams() == null) {
      if (aGenCasProcessor.getDeploymentParams() != null) {
        fail(aRefCasProcessor.getDeployment()
                + " Cas Processor with name="
                + aGenCasProcessor.getName()
                + " contains <deploymentParameters> element. Expected to find empty <deploymentParameters> element");
      }
    }
    if (aRefCasProcessor.getCasProcessorFilter() == null) {
      if (aGenCasProcessor.getCasProcessorFilter() != null) {
        fail(aRefCasProcessor.getDeployment() + " Cas Processor with name="
                + aGenCasProcessor.getName()
                + " contains <filter> element. Expected to find empty <filter> element");
      }
    }
    if (aRefCasProcessor.getErrorHandling() != null) {
      compareErrorHandling(aRefCasProcessor.getErrorHandling(),
              aGenCasProcessor.getErrorHandling(), aRefCasProcessor.getName());
    }
    if (aRefCasProcessor.getCheckpoint() != null) {
      if (aGenCasProcessor.getCheckpoint() == null) {
        fail(aRefCasProcessor.getDeployment() + " Cas Processor with name="
                + aGenCasProcessor.getName() + " is missing <checkpoint> element.");
      } else {
        assertEquals("Generated Cas Processor:" + aGenCasProcessor.getName()
                + " batch size does match expected value", aRefCasProcessor.getCheckpoint()
                .getBatchSize(), aGenCasProcessor.getCheckpoint().getBatchSize());
      }
    }

  }

  public void compareLocalCasProcessors(CpeCasProcessor aRefCasProcessor,
          CpeCasProcessor aGenCasProcessor) {
    compareCommonElements(aRefCasProcessor, aGenCasProcessor);

  }

  public void compareRemoteCasProcessors(CpeCasProcessor aRefCasProcessor,
          CpeCasProcessor aGenCasProcessor) {
    compareCommonElements(aRefCasProcessor, aGenCasProcessor);
  }

  public void compareIntegratedCasProcessors(CpeCasProcessor aRefCasProcessor,
          CpeCasProcessor aGenCasProcessor) {
    compareCommonElements(aRefCasProcessor, aGenCasProcessor);
  }

  public void compareErrorHandling(CasProcessorErrorHandling aRefCasProcessorErrorHandling,
          CasProcessorErrorHandling aGenCasProcessorErrorHandling, String aComponentName) {
    if (aGenCasProcessorErrorHandling == null) {
      fail("Integrated Cas Processor with name=" + aComponentName
              + " is missing <errorHandling> element.");
    } else {
      if (aRefCasProcessorErrorHandling.getErrorRateThreshold() != null) {
        if (aGenCasProcessorErrorHandling.getErrorRateThreshold() == null) {
          fail("Expected element <errorRateThreshold> is missing from the CPE Descriptor for Cas Processor with name:"
                  + aComponentName);
        } else {
          assertEquals(aRefCasProcessorErrorHandling.getErrorRateThreshold().getAction(),
                  aGenCasProcessorErrorHandling.getErrorRateThreshold().getAction());
          assertEquals(aRefCasProcessorErrorHandling.getErrorRateThreshold().getMaxErrorCount(),
                  aGenCasProcessorErrorHandling.getErrorRateThreshold().getMaxErrorCount());
          assertEquals(aRefCasProcessorErrorHandling.getErrorRateThreshold()
                  .getMaxErrorSampleSize(), aGenCasProcessorErrorHandling.getErrorRateThreshold()
                  .getMaxErrorSampleSize());
        }
      }
      if (aRefCasProcessorErrorHandling.getMaxConsecutiveRestarts() != null) {
        if (aGenCasProcessorErrorHandling.getMaxConsecutiveRestarts() == null) {
          fail("Expected element <maxConsecutiveRestarts> is missing from the CPE Descriptor for Cas Processor with name:"
                  + aComponentName);
        } else {
          assertEquals(aRefCasProcessorErrorHandling.getMaxConsecutiveRestarts().getAction(),
                  aGenCasProcessorErrorHandling.getMaxConsecutiveRestarts().getAction());
          assertEquals(aRefCasProcessorErrorHandling.getMaxConsecutiveRestarts().getRestartCount(),
                  aGenCasProcessorErrorHandling.getMaxConsecutiveRestarts().getRestartCount());
          assertEquals(aRefCasProcessorErrorHandling.getMaxConsecutiveRestarts()
                  .getWaitTimeBetweenRetries(), aGenCasProcessorErrorHandling
                  .getMaxConsecutiveRestarts().getWaitTimeBetweenRetries());
        }
      }
      if (aRefCasProcessorErrorHandling.getTimeout() != null) {
        if (aGenCasProcessorErrorHandling.getTimeout() == null) {
          fail("Expected element <timeout> is missing from the CPE Descriptor for Cas Processor with name:"
                  + aComponentName);
        } else {
          assertEquals(aRefCasProcessorErrorHandling.getTimeout().get(),
                  aGenCasProcessorErrorHandling.getTimeout().get());
        }
      }
    }

  }

  public void isConfigTheSame(CpeConfiguration aRefCpeConfig, CpeConfiguration aGenCpeConfig) {
    try {
      assertEquals(aRefCpeConfig.getNumToProcess(), aGenCpeConfig.getNumToProcess());
      assertEquals(aRefCpeConfig.getDeployment(), aGenCpeConfig.getDeployment());
      assertEquals(aRefCpeConfig.getTimerImpl(), aGenCpeConfig.getTimerImpl());
      if (aRefCpeConfig.getStartingEntityId() != null) {
        assertEquals(aRefCpeConfig.getStartingEntityId(), aGenCpeConfig.getStartingEntityId());
      } else {
        if (aGenCpeConfig.getStartingEntityId() != null) {
          assertTrue(
                  "Generated Cpe Descriptor has an invalid value for <startAt> in <cpeConfig>.Expected no value OR empty string. Instead got value of:"
                          + aGenCpeConfig.getStartingEntityId(), aGenCpeConfig
                          .getStartingEntityId().trim().length() == 0);
        }
      }
      if (aRefCpeConfig.getCheckpoint() != null) {
        if (aGenCpeConfig.getCheckpoint() == null) {
          fail("Expected <checkpoint> element in <cpeConfig> not found in generated Cpe Descriptor.");
        } else {
          if (aRefCpeConfig.getCheckpoint().getFilePath() != null) {
            if (aGenCpeConfig.getCheckpoint().getFilePath() == null) {
              fail("Expected attribute 'file' in <checkpoint> element in <cpeConfig> not found in generated Cpe Descriptor.");
            } else {
              assertEquals(aRefCpeConfig.getCheckpoint().getFilePath(), aGenCpeConfig
                      .getCheckpoint().getFilePath());
            }
          }
          assertEquals(aRefCpeConfig.getCheckpoint().getBatchSize(), aGenCpeConfig.getCheckpoint()
                  .getBatchSize());
          assertEquals(aRefCpeConfig.getCheckpoint().getFrequency(), aGenCpeConfig.getCheckpoint()
                  .getFrequency());
        }
      }
    } catch (Exception e) {
      fail("Exception::" + e.getMessage()
              + " thrown comparing configuration <cpeConfig> in CPE Descriptor");
    }
  }

  public void compareDescriptors(CpeComponentDescriptor aRefDescriptor,
          CpeComponentDescriptor aGenDescritptor, String aComponentName) {
    if (aRefDescriptor.getInclude() != null) {
      if (aGenDescritptor.getInclude() == null) {
        fail("Component Descriptor of " + aComponentName
                + " missing from a generated CPE Descriptor");
      } else {
        String refFilePath = aRefDescriptor.getInclude().get();
        String genFilePath = aGenDescritptor.getInclude().get();

        if (refFilePath != null) {
          assertEquals(
                  "Component Descriptor in Generated CPE Descriptor has unexpected value. Expected:"
                          + refFilePath + " Got:" + genFilePath + " Instead.", refFilePath,
                  genFilePath);
        }
      }
    }
  }

  public boolean isCollectionIteratorTheSame(CpeCollectionReaderIterator aRefIterator,
          CpeCollectionReaderIterator aGenIterator) {
    if (aRefIterator.getDescriptor() != null) {
      if (aGenIterator.getDescriptor() == null) {
        return false;
      } else {
        if (aRefIterator.getDescriptor().getInclude() != null) {
          if (aGenIterator.getDescriptor().getInclude() == null) {
            return false;
          } else {
            String refFilePath = aRefIterator.getDescriptor().getInclude().get();
            String genFilePath = aGenIterator.getDescriptor().getInclude().get();

            if (refFilePath != null) {
              boolean ret = refFilePath.equals(genFilePath);
              return ret;
            }
          }
        }
      }
    }

    return false;
  }

  public boolean isCasInitializerTheSame(CpeCollectionReaderCasInitializer aRefCasInitializer,
          CpeCollectionReaderCasInitializer aGenCasInitializer) {
    if (aRefCasInitializer.getDescriptor() != null) {
      if (aGenCasInitializer.getDescriptor() == null) {
        return false;
      } else {
        if (aRefCasInitializer.getDescriptor().getInclude() != null) {
          if (aGenCasInitializer.getDescriptor().getInclude() == null) {
            return false;
          } else {
            String refFilePath = aRefCasInitializer.getDescriptor().getInclude().get();
            String genFilePath = aGenCasInitializer.getDescriptor().getInclude().get();

            if (refFilePath != null) {
              boolean ret = refFilePath.equals(genFilePath);
              return ret;
            }
          }
        }
      }
    }

    return false;
  }

}
