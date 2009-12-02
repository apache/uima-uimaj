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

package org.apache.uima.caseditor.ui.action;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.CAS;
import org.apache.uima.caseditor.CasEditorPlugin;
import org.apache.uima.caseditor.core.model.CorpusElement;
import org.apache.uima.caseditor.core.model.NlpProject;
import org.apache.uima.caseditor.core.uima.CasConsumerConfiguration;
import org.apache.uima.caseditor.uima.CorporaCollectionReader;
import org.apache.uima.collection.CollectionProcessingManager;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.collection.EntityProcessStatus;
import org.apache.uima.collection.StatusCallbackListener;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.ProcessingResourceMetaData;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLParser;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;

/**
 * This action launches the cpm with the configured cas consumer.
 *
 * TODO: move over to core plugin
 */
public class ConsumerActionRunnable implements IRunnableWithProgress
{
    private CasConsumerConfiguration mConfiguration;

    private Collection<CorpusElement> mCorpora;

    private boolean mIsProcessing = true;

    /**
     * Initializes a new instance.
     *
     * @param config
     * @param corpora
     */
    public ConsumerActionRunnable(CasConsumerConfiguration config,
            Collection<CorpusElement> corpora)
    {
        mConfiguration = config;

        mCorpora = corpora;
    }

    public void run(IProgressMonitor monitor)
            throws InvocationTargetException, InterruptedException
    {
        monitor.beginTask("Consuming", IProgressMonitor.UNKNOWN);

        monitor.subTask("Initializing, please stand by.");

        InputStream inCollectionReaderDescripton = getClass()
                .getResourceAsStream("CorporaCollectionReader.xml");

        // TODO: inCollectionReaderDescripton check for null

        CollectionReaderDescription collectionReaderDescripton;
        try
        {
            collectionReaderDescripton = (CollectionReaderDescription)
            UIMAFramework.getXMLParser().parseResourceSpecifier(
                    new XMLInputSource(inCollectionReaderDescripton,
                            new File("")));
        }
        catch (InvalidXMLException e)
        {
            throw new InvocationTargetException(e, "CorporaCollectionReader.xml"
                    + " could ne be parsed!");
        }

        NlpProject project =
                mConfiguration.getConsumerElement().getNlpProject();

        InputStream inTypeSystemDescription;
        try
        {
            inTypeSystemDescription =
                project.getDotCorpus().getTypeSystemFile().getContents();
        }
        catch (CoreException e)
        {
            throw new InvocationTargetException(e);
        }

        TypeSystemDescription typeSystemDescriptor;
        try
        {
            typeSystemDescriptor = UIMAFramework.getXMLParser()
            .parseTypeSystemDescription(
                    new XMLInputSource(inTypeSystemDescription, new File("")));

            typeSystemDescriptor.resolveImports();
        }
        catch (InvalidXMLException e)
        {
            throw new InvocationTargetException(e);
        }

//      set type system to collection reader
        ProcessingResourceMetaData collectionReaderMetaData =
            collectionReaderDescripton.getCollectionReaderMetaData();

        collectionReaderMetaData.setTypeSystem(typeSystemDescriptor);

        XMLParser xmlParser = UIMAFramework.getXMLParser();

        InputStream inIndex = getClass().getResourceAsStream(
                "Index.xml");

        if (inIndex == null)
        {
            throw new InvocationTargetException(null,
                    "org/apache/uima/caseditor/ui/action/Index.xml"
                    + " is missing on the classpath");
        }

        XMLInputSource xmlIndexSource = new XMLInputSource(inIndex,
                new File(""));

        FsIndexDescription indexDesciptor;

        try
        {
            indexDesciptor = (FsIndexDescription) xmlParser
            .parse(xmlIndexSource);
        }
        catch (InvalidXMLException e)
        {
            throw new InvocationTargetException(e);
        }

        collectionReaderMetaData.setFsIndexes(new FsIndexDescription[]
                {indexDesciptor});

        CollectionReader collectionReader;
        try
        {
            collectionReader = UIMAFramework
                    .produceCollectionReader(collectionReaderDescripton);
        }
        catch (ResourceInitializationException e)
        {
            throw new InvocationTargetException(e);
        }

        ((CorporaCollectionReader) collectionReader).setCorpora(mCorpora);

        InputStream in = getClass().getResourceAsStream("DummyTAE.xml");

//      load dummy descriptor
        ResourceSpecifier textAnalysisEngineSpecifier;
        try
        {
            textAnalysisEngineSpecifier = UIMAFramework.getXMLParser()
                    .parseResourceSpecifier(
                    new XMLInputSource(in, new File("")));
        }
        catch (InvalidXMLException e)
        {
            throw new InvocationTargetException(e);
        }

        AnalysisEngine textAnalysisEngine;
        try
        {
            textAnalysisEngine = UIMAFramework
                    .produceAnalysisEngine(textAnalysisEngineSpecifier);
        }
        catch (ResourceInitializationException e)
        {
            throw new InvocationTargetException(e);
        }

        CollectionProcessingManager collectionProcessingEngine = UIMAFramework
                .newCollectionProcessingManager();

        try
        {
            collectionProcessingEngine.setAnalysisEngine(textAnalysisEngine);
        }
        catch (ResourceConfigurationException e)
        {
            throw new InvocationTargetException(e);
        }

        try
        {
            collectionProcessingEngine.addCasConsumer(mConfiguration
                    .createConsumer());
        }
        catch (ResourceConfigurationException e)
        {
            throw new InvocationTargetException(e);
        }

        collectionProcessingEngine.setPauseOnException(false);

        collectionProcessingEngine.addStatusCallbackListener(
                new StatusCallbackListener(){

            public void entityProcessComplete(CAS cas,
                    EntityProcessStatus status)
            {
                // not implemented
            }

            public void aborted()
            {
                finishProcessing();
            }

            public void batchProcessComplete()
            {
                // not implemented
            }

            public void collectionProcessComplete()
            {
                finishProcessing();
            }

            public void initializationComplete()
            {
                // not implemented
            }

            public void paused()
            {
                // not implemented
            }

            public void resumed()
            {
                // not implemented
            }

            private void finishProcessing()
            {
                synchronized (ConsumerActionRunnable.this)
                {
                    mIsProcessing = false;
                    ConsumerActionRunnable.this.notifyAll();
                }
            }
        });

        monitor.subTask("Feeding comsumer, please stand by.");

        try
        {
            collectionProcessingEngine.process(collectionReader);
        }
        catch (ResourceInitializationException e)
        {
            throw new InvocationTargetException(e);
        }

        synchronized(this)
        {
            // TODO: for cancel poll here
            // and call .stop()
            while (mIsProcessing)
            {
                wait();
            }
        }

        // refresh cas processor directory
        IResource processorFolder = mConfiguration.getBaseFolder();

        try {
          processorFolder.refreshLocal(IResource.DEPTH_INFINITE, monitor);
    	} catch (CoreException e) {
    		// maybe this fails, sorry
    		CasEditorPlugin.log(e);
    	}

        monitor.done();
    }
}