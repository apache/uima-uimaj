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

package org.apache.uima.caseditor.editor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.LinkedList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.uima.ResourceSpecifierFactory;
import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.ConstraintFactory;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIntConstraint;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FSMatchConstraint;
import org.apache.uima.cas.FeaturePath;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.XCASDeserializer;
import org.apache.uima.cas.impl.XCASSerializer;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.caseditor.CasEditorPlugin;
import org.apache.uima.caseditor.editor.util.Span;
import org.apache.uima.caseditor.editor.util.StrictTypeConstraint;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.TypePriorities;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.FsIndexDescription_impl;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLParser;
import org.apache.uima.util.XMLSerializer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.xml.sax.SAXException;

/**
 * This document implementation is based on an uima cas object.
 */
public class DocumentUimaImpl extends AbstractDocument {

  // TODO: Remove field not needed anymore
  private final TypeSystem mTypeSystem;

  private CAS mCAS;

  private final DocumentFormat format;

  /**
   * Initializes a new instance.
   * 
   * @param project
   */
  public DocumentUimaImpl(CAS cas, InputStream in, DocumentFormat format) throws CoreException {

    mCAS = cas;

    mTypeSystem = cas.getTypeSystem();

    this.format = format;

    setContent(in);
  }

  /**
   * Retrieves the {@link CAS}.
   */
  public CAS getCAS() {
    return mCAS;
  }

  /**
   * Adds the given annotation to the {@link CAS}.
   */
  public void addFeatureStructure(FeatureStructure annotation) {
    mCAS.getIndexRepository().addFS(annotation);

    fireAddedFeatureStructure(annotation);
  }

  /**
	 *
	 */
  public void addFeatureStructures(Collection<? extends FeatureStructure> annotations) {
    for (FeatureStructure annotation : annotations) {
      addFeatureStructure(annotation);
    }
  }

  /**
   * Internally removes an annotation from the {@link CAS}.
   * 
   * @param featureStructure
   */
  private void removeAnnotationInternal(FeatureStructure featureStructure) {
    getCAS().getIndexRepository().removeFS(featureStructure);
  }

  /**
   * Removes the annotations from the {@link CAS}.
   */
  public void removeFeatureStructure(FeatureStructure annotation) {
    removeAnnotationInternal(annotation);

    fireRemovedFeatureStructure(annotation);
  }

  /**
   * Removes the given annotations from the {@link CAS}.
   */
  public void removeFeatureStructures(Collection<? extends FeatureStructure> annotationsToRemove) {

    for (FeatureStructure annotationToRemove : annotationsToRemove) {
      removeAnnotationInternal(annotationToRemove);
    }

    if (annotationsToRemove.size() > 0) {
      fireRemovedFeatureStructure(annotationsToRemove);
    }
  }

  /**
   * Notifies clients about the changed annotation.
   */
  public void update(FeatureStructure annotation) {
    fireUpdatedFeatureStructure(annotation);
  }

  /**
   * Notifies clients about the changed annotation.
   */
  public void updateFeatureStructure(Collection<? extends FeatureStructure> annotations) {
    fireUpdatedFeatureStructure(annotations);
  }

  public void changed() {
    fireChanged();
  }

  /**
   * Retrieves annotations of the given type from the {@link CAS}.
   */
  public Collection<AnnotationFS> getAnnotations(Type type) {
    FSIndex<AnnotationFS> annotationIndex = mCAS.getAnnotationIndex(type);

    StrictTypeConstraint typeConstrain = new StrictTypeConstraint(type);

    FSIterator<AnnotationFS> strictTypeIterator =
            mCAS.createFilteredIterator(annotationIndex.iterator(), typeConstrain);

    return fsIteratorToCollection(strictTypeIterator);
  }

  static Collection<AnnotationFS> fsIteratorToCollection(FSIterator<AnnotationFS> iterator) {
    LinkedList<AnnotationFS> annotations = new LinkedList<AnnotationFS>();
    while (iterator.hasNext()) {
      AnnotationFS annotation = (AnnotationFS) iterator.next();

      annotations.addFirst(annotation);
    }

    return annotations;
  }

  /**
   * Retrieves the given type from the {@link TypeSystem}.
   */
  public Type getType(String type) {
    return getCAS().getTypeSystem().getType(type);
  }

  public void switchView(String viewName) {
	  String oldViewName = mCAS.getViewName();
	  
	  mCAS = mCAS.getView(viewName);
	  
	  fireViewChanged(oldViewName, viewName);
  }
  
  /**
   * Sets the content. The XCAS {@link InputStream} gets parsed.
   */
  private void setContent(InputStream content) throws CoreException {

    SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
    saxParserFactory.setValidating(false);

    SAXParser saxParser;

    try {
      saxParser = saxParserFactory.newSAXParser();
    } catch (ParserConfigurationException e) {
      String message = e.getMessage() != null ? e.getMessage() : "";

      IStatus s = new Status(IStatus.ERROR, CasEditorPlugin.ID, IStatus.OK, message, e);

      throw new CoreException(s);
    } catch (SAXException e) {
      String message = e.getMessage() != null ? e.getMessage() : "";

      IStatus s = new Status(IStatus.ERROR, CasEditorPlugin.ID, IStatus.OK, message, e);

      throw new CoreException(s);
    }

    if (DocumentFormat.XCAS.equals(format)) {
      XCASDeserializer dezerializer = new XCASDeserializer(mTypeSystem);

      try {
        saxParser.parse(content, dezerializer.getXCASHandler(mCAS));
      } catch (IOException e) {
        String message = e.getMessage() != null ? e.getMessage() : "";

        IStatus s = new Status(IStatus.ERROR, CasEditorPlugin.ID, IStatus.OK, message, e);

        throw new CoreException(s);
      } catch (SAXException e) {
        String message = e.getMessage() != null ? e.getMessage() : "";

        IStatus s = new Status(IStatus.ERROR, CasEditorPlugin.ID, IStatus.OK, message, e);

        throw new CoreException(s);
      }
    } else if (DocumentFormat.XMI.equals(format)) {
      XmiCasDeserializer dezerializer = new XmiCasDeserializer(mTypeSystem);

      try {
        saxParser.parse(content, dezerializer.getXmiCasHandler(mCAS));
      } catch (IOException e) {
        String message = e.getMessage() != null ? e.getMessage() : "";

        IStatus s = new Status(IStatus.ERROR, CasEditorPlugin.ID, IStatus.OK, message, e);

        throw new CoreException(s);
      } catch (SAXException e) {
        String message = e.getMessage() != null ? e.getMessage() : "";

        IStatus s = new Status(IStatus.ERROR, CasEditorPlugin.ID, IStatus.OK, message, e);

        throw new CoreException(s);
      }
    } else {
      throw new CoreException(new Status(IStatus.ERROR, CasEditorPlugin.ID, IStatus.OK,
              "Unkown file format!", null));
    }
  }

  /**
   * Serializes the {@link CAS} to the given {@link OutputStream} in the XCAS format.
   */
  public void serialize(OutputStream out) throws CoreException {

    if (DocumentFormat.XCAS.equals(format)) {
      XCASSerializer xcasSerializer = new XCASSerializer(mCAS.getTypeSystem());

      XMLSerializer xmlSerialzer = new XMLSerializer(out, true);

      try {
        xcasSerializer.serialize(mCAS, xmlSerialzer.getContentHandler());
      } catch (IOException e) {
        String message = e.getMessage() != null ? e.getMessage() : "";

        IStatus s = new Status(IStatus.ERROR, CasEditorPlugin.ID, IStatus.OK, message, e);

        throw new CoreException(s);
      } catch (SAXException e) {
        String message = e.getMessage() != null ? e.getMessage() : "";

        IStatus s = new Status(IStatus.ERROR, CasEditorPlugin.ID, IStatus.OK, message, e);

        throw new CoreException(s);
      }
    } else if (DocumentFormat.XMI.equals(format)) {
      XmiCasSerializer xmiSerializer = new XmiCasSerializer(mCAS.getTypeSystem());

      XMLSerializer xmlSerialzer = new XMLSerializer(out, true);

      try {
        xmiSerializer.serialize(mCAS, xmlSerialzer.getContentHandler());
      } catch (SAXException e) {
        String message = e.getMessage() != null ? e.getMessage() : "";

        IStatus s = new Status(IStatus.ERROR, CasEditorPlugin.ID, IStatus.OK, message, e);

        throw new CoreException(s);
      }
    } else {
      throw new CoreException(new Status(IStatus.ERROR, CasEditorPlugin.ID, IStatus.OK,
              "Unkown file format!", null));
    }
  }
  
  public static CAS getVirginCAS(IFile typeSystemFile) throws CoreException {
    ResourceSpecifierFactory resourceSpecifierFactory = UIMAFramework.getResourceSpecifierFactory();

    IFile extensionTypeSystemFile = typeSystemFile;

    InputStream inTypeSystem;

    if (extensionTypeSystemFile != null && extensionTypeSystemFile.exists()) {
      inTypeSystem = extensionTypeSystemFile.getContents();
    } else {
      return null;
    }

    XMLInputSource xmlTypeSystemSource = new XMLInputSource(inTypeSystem,
            extensionTypeSystemFile.getLocation().toFile());

    XMLParser xmlParser = UIMAFramework.getXMLParser();

    TypeSystemDescription typeSystemDesciptor;

    try {
      typeSystemDesciptor = (TypeSystemDescription) xmlParser.parse(xmlTypeSystemSource);

      typeSystemDesciptor.resolveImports();
    } catch (InvalidXMLException e) {

      String message = e.getMessage() != null ? e.getMessage() : "";

      // TODO: Change plugin ID
      IStatus s = new Status(IStatus.ERROR, "org.apache.uima.dev", IStatus.OK, message, e);

      throw new CoreException(s);
    }

    TypePriorities typePriorities = resourceSpecifierFactory.createTypePriorities();

    FsIndexDescription indexDesciptor = new FsIndexDescription_impl();
    indexDesciptor.setLabel("TOPIndex");
    indexDesciptor.setTypeName("uima.cas.TOP");
    indexDesciptor.setKind(FsIndexDescription.KIND_SORTED);

    CAS cas;
    try {
      cas = CasCreationUtils.createCas(typeSystemDesciptor, typePriorities,
              new FsIndexDescription[] { indexDesciptor });
    } catch (ResourceInitializationException e) {
      String message = e.getMessage() != null ? e.getMessage() : "";

      // TODO: Change plugin ID
      IStatus s = new Status(IStatus.ERROR, "org.apache.uima.dev", IStatus.OK, message, e);

      throw new CoreException(s);
    }

    return cas;
  }
}
