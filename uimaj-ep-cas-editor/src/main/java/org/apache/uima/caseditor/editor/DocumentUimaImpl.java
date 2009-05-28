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
import org.apache.uima.util.XMLSerializer;
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

  private final CAS mCAS;

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

    fireAddedAnnotation(annotation);
  }

  /**
	 *
	 */
  public void addFeatureStructures(Collection<FeatureStructure> annotations) {
    for (FeatureStructure annotation : annotations) {
      addFeatureStructure(annotation);
    }
  }

  /**
   * Remove all annotations. TODO: implement it
   */
  public void removeAnnotation() {
    // must be implemented
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

    fireRemovedAnnotation(annotation);
  }

  /**
   * Removes the given annotations from the {@link CAS}.
   */
  public void removeFeatureStructures(Collection<FeatureStructure> annotationsToRemove) {

    for (FeatureStructure annotationToRemove : annotationsToRemove) {
      removeAnnotationInternal(annotationToRemove);
    }

    if (annotationsToRemove.size() > 0) {
      fireRemovedAnnotations(annotationsToRemove);
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
  public void updateFeatureStructure(Collection<FeatureStructure> annotations) {
    fireUpdatedFeatureStructures(annotations);
  }

  public void changed() {
    fireChanged();
  }

  /**
   * Retrieves annotations of the given type from the {@link CAS}.
   */
  public Collection<AnnotationFS> getAnnotations(Type type) {
    FSIndex annotationIndex = mCAS.getAnnotationIndex(type);

    StrictTypeConstraint typeConstrain = new StrictTypeConstraint(type);

    FSIterator strictTypeIterator =
            mCAS.createFilteredIterator(annotationIndex.iterator(), typeConstrain);

    return fsIteratorToCollection(strictTypeIterator);
  }

  private Collection<AnnotationFS> fsIteratorToCollection(FSIterator iterator) {
    LinkedList<AnnotationFS> annotations = new LinkedList<AnnotationFS>();
    while (iterator.hasNext()) {
      AnnotationFS annotation = (AnnotationFS) iterator.next();

      annotations.addFirst(annotation);
    }

    return annotations;
  }

  /**
   * Retrieves the annotations in the given span.
   */
  @Override
  public Collection<AnnotationFS> getAnnotation(Type type, Span span) {
    ConstraintFactory cf = getCAS().getConstraintFactory();

    Type annotationType = getCAS().getAnnotationType();

    FeaturePath beginPath = getCAS().createFeaturePath();
    beginPath.addFeature(annotationType.getFeatureByBaseName("begin"));
    FSIntConstraint beginConstraint = cf.createIntConstraint();
    beginConstraint.geq(span.getStart());

    FSMatchConstraint embeddedBegin = cf.embedConstraint(beginPath, beginConstraint);

    FeaturePath endPath = getCAS().createFeaturePath();
    endPath.addFeature(annotationType.getFeatureByBaseName("end"));
    FSIntConstraint endConstraint = cf.createIntConstraint();
    endConstraint.leq(span.getEnd());

    FSMatchConstraint embeddedEnd = cf.embedConstraint(endPath, endConstraint);

    FSMatchConstraint strictType = new StrictTypeConstraint(type);

    FSMatchConstraint annotatioInSpanConstraint = cf.and(embeddedBegin, embeddedEnd);

    FSMatchConstraint annotationInSpanAndStrictTypeConstraint =
            cf.and(annotatioInSpanConstraint, strictType);

    FSIndex allAnnotations = getCAS().getAnnotationIndex(type);

    FSIterator annotationInsideSpanIndex =
            getCAS().createFilteredIterator(allAnnotations.iterator(),
            annotationInSpanAndStrictTypeConstraint);

    return fsIteratorToCollection(annotationInsideSpanIndex);
  }

  /**
   * Retrieves the given type from the {@link TypeSystem}.
   */
  public Type getType(String type) {
    return getCAS().getTypeSystem().getType(type);
  }

  /**
   * Retrieves the text.
   */
  public String getText() {
    return mCAS.getDocumentText();
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
}
