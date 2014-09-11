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

package org.apache.uima.util;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UIMARuntimeException;
import org.apache.uima.cas.BooleanArrayFS;
import org.apache.uima.cas.ByteArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.DoubleArrayFS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FSMatchConstraint;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.FloatArrayFS;
import org.apache.uima.cas.IntArrayFS;
import org.apache.uima.cas.LongArrayFS;
import org.apache.uima.cas.ShortArrayFS;
import org.apache.uima.cas.StringArrayFS;
import org.apache.uima.cas.text.AnnotationFS;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Generates an *approximate* inline XML representation of a CAS.
 * Annotation types are represented as XML tags, features are represented as attributes.
 * 
 * Features whose values are FeatureStructures are not represented.
 * Feature values which are strings longer than 64 characters are truncated.
 * Feature values which are arrays of primitives are represented by
 * strings that look like [ xxx, xxx ]
 * 
 * The Subject of analysis is presumed to be a text string.
 * 
 * Some characters in the document's Subject-of-analysis
 * are replaced by blanks, because the characters aren't valid in xml documents.
 * 
 * It doesn't work for annotations which are overlapping, because these cannot
 * be properly represented as properly - nested XML.
 * 
 * To use this, make an instance of this class, and
 * (optionally) set the formattedOutput to true or false.
 * 
 * Then call one of the public methods to format or generate the Inline XML.
 */
public class CasToInlineXml {

  private boolean formattedOutput = true;

  /**
   * This destroy method does nothing.
   * 
   * @see org.apache.uima.resource.Resource#destroy()
   */
  public void destroy() {
  }

  /**
   * Formats a CAS as a String.
   * @param aCAS the cas to format as an xml string
   * @return the XML representation of the CAS
   * @throws CASException -
   */
  public String format(CAS aCAS) throws CASException {
    return generateXML(aCAS, null);
  }

  /**
   * Formats a CAS as a String. Only FeatureStructures matching the given filter will be output.
   * @param aCAS CAS
   * @param aFilter a filter to limit the Feature Structures 
   * @return the XML representation
   * @throws CASException -
   */
  public String format(CAS aCAS, FSMatchConstraint aFilter) throws CASException {
    return generateXML(aCAS, aFilter);
  }

  /**
   * Generates inline XML from a CAS.
   * 
   * @param aCAS
   *          CAS to generate from
   * @return the inline XML version of the CAS
   * @throws CASException -
   */
  public String generateXML(CAS aCAS) throws CASException {
    return generateXML(aCAS, null);
  }

  /**
   * Generates inline XML from a CAS using a passed in ContentHandler
   * 
   * @param aCAS
   *          CAS to generate from
   * @param aFilter
   *          constraint that determines which annotations are included in the output. If null (or
   *          omitted), all annotations are included.
   * @param aHandler the content handler to use
   * @throws CASException -
   */
  public void generateXML(CAS aCAS, FSMatchConstraint aFilter, ContentHandler aHandler) throws CASException {

    // get document text
    String docText = aCAS.getDocumentText();
    char[] docCharArray = docText.toCharArray();
    replaceInvalidXmlChars(docCharArray);

    // get iterator over annotations sorted by increasing start position and
    // decreasing end position
    FSIterator<AnnotationFS> iterator = aCAS.getAnnotationIndex().iterator();

    // filter the iterator if desired
    if (aFilter != null) {
      iterator = aCAS.createFilteredIterator(iterator, aFilter);
    }

    // This is basically a recursive algorithm that has had the recursion
    // removed through the use of an explicit Stack. We iterate over the
    // annotations, and if an annotation contains other annotations, we
    // push the parent annotation on the stack, process the children, and
    // then come back to the parent later.
    List<AnnotationFS> stack = new ArrayList<AnnotationFS>();
    int pos = 0;

    try {
      aHandler.startDocument();
      // write an artificial start tag
      aHandler.startElement("", "Document", "Document", new AttributesImpl());
      // now use null is a placeholder for this artificial Document annotation
      AnnotationFS curAnnot = null;

      while (iterator.isValid()) {
        // debug
        // FeatureStructure fs = iterator.get();
        // System.out.println("Type: " + fs.getType().getName() + ", Class:" +
        // fs.getClass().getName());
        // AnnotationFS nextAnnot = (AnnotationFS)fs;
        AnnotationFS nextAnnot = iterator.get();

        if (curAnnot == null || nextAnnot.getBegin() < curAnnot.getEnd()) {
          // nextAnnot's start point is within the span of curAnnot
          if (curAnnot == null || nextAnnot.getEnd() <= curAnnot.getEnd()) // crossover span check
          {
            // nextAnnot is contained within curAnnot

            // write text between current pos and beginning of nextAnnot
            try {
              aHandler.characters(docCharArray, pos, nextAnnot.getBegin() - pos);
              pos = nextAnnot.getBegin();
              aHandler.startElement("", nextAnnot.getType().getName(),
                  nextAnnot.getType().getName(), getFeatureAttributes(nextAnnot, aCAS));

              // push parent annotation on stack
              stack.add(curAnnot);
              // move on to next annotation
              curAnnot = nextAnnot;
            } catch (StringIndexOutOfBoundsException e) {
              System.err.println("Invalid annotation range: " + nextAnnot.getBegin() + ","
                  + nextAnnot.getEnd() + " in document of length " + docText.length());
            }
          }
          iterator.moveToNext();
        } else {
          // nextAnnot begins after curAnnot ends
          // write text between current pos and end of curAnnot
          try {
            aHandler.characters(docCharArray, pos, curAnnot.getEnd() - pos);
            pos = curAnnot.getEnd();
          } catch (StringIndexOutOfBoundsException e) {
            System.err.println("Invalid annotation range: " + curAnnot.getBegin() + ","
                + curAnnot.getEnd() + " in document of length " + docText.length());
          }
          aHandler.endElement("", curAnnot.getType().getName(), curAnnot.getType().getName());

          // pop next containing annotation off stack
          curAnnot = stack.remove(stack.size() - 1);
        }
      }

      // finished writing all start tags, now finish up
      if (curAnnot != null) {
        try {
          aHandler.characters(docCharArray, pos, curAnnot.getEnd() - pos);
          pos = curAnnot.getEnd();
        } catch (StringIndexOutOfBoundsException e) {
          System.err.println("Invalid annotation range: " + curAnnot.getBegin() + ","
              + curAnnot.getEnd() + "in document of length " + docText.length());
        }
        aHandler.endElement("", curAnnot.getType().getName(), curAnnot.getType().getName());

        while (!stack.isEmpty()) {
          curAnnot = stack.remove(stack.size() - 1); // pop
          if (curAnnot == null) {
            break;
          }
          try {
            aHandler.characters(docCharArray, pos, curAnnot.getEnd() - pos);
            pos = curAnnot.getEnd();
          } catch (StringIndexOutOfBoundsException e) {
            System.err.println("Invalid annotation range: " + curAnnot.getBegin() + ","
                + curAnnot.getEnd() + "in document of length " + docText.length());
          }
          aHandler.endElement("", curAnnot.getType().getName(), curAnnot.getType().getName());
        }
      }

      if (pos < docCharArray.length) {
        aHandler.characters(docCharArray, pos, docCharArray.length - pos);
      }
      aHandler.endElement("", "Document", "Document");
      aHandler.endDocument();

    } catch (SAXException e) {
      throw new UIMARuntimeException(e);
    }
  }


  /**
   * Generates inline XML from a CAS.
   * 
   * @param aCAS
   *          CAS to generate from
   * @param aFilter
   *          constraint that determines which annotations are included in the output. If null (or
   *          ommitted), all annotations are included.
   * @throws CASException -
   * @return the inline form
   */
  public String generateXML(CAS aCAS, FSMatchConstraint aFilter) throws CASException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    XMLSerializer sax2xml = new XMLSerializer(byteArrayOutputStream, formattedOutput);
    generateXML(aCAS, aFilter, sax2xml.getContentHandler());
    // return XML string
    return new String(byteArrayOutputStream.toByteArray());
  }

  private Attributes getFeatureAttributes(FeatureStructure aFS, CAS aCAS) {
    AttributesImpl attrs = new AttributesImpl();
    for (Feature feat : aFS.getType().getFeatures()) {
      String featName = feat.getShortName();
      // how we get feature value depends on feature's range type)
      String rangeTypeName = feat.getRange().getName();
      if (feat.getRange().isPrimitive()) {
        String str = aFS.getFeatureValueAsString(feat);
        if (str == null) {
          attrs.addAttribute("", featName, featName, "CDATA", "null");
        } else {
          if (str.length() > 64) {
            str = str.substring(0, 64) + "...";
          }
          attrs.addAttribute("", featName, featName, "CDATA", str);
        }
      } else if (feat.getRange().isArray() && feat.getRange().getComponentType().isPrimitive()) {
        // TODO: there should be a better way to get any array value as a string array
        String[] vals = null;
        if (CAS.TYPE_NAME_STRING_ARRAY.equals(rangeTypeName)) {
          StringArrayFS arrayFS = (StringArrayFS) aFS.getFeatureValue(feat);
          if (arrayFS != null) {
            vals = arrayFS.toArray();
          }
        } else if (CAS.TYPE_NAME_INTEGER_ARRAY.equals(rangeTypeName)) {
          IntArrayFS arrayFS = (IntArrayFS) aFS.getFeatureValue(feat);
          if (arrayFS != null) {
            vals = arrayFS.toStringArray();
          }
        } else if (CAS.TYPE_NAME_FLOAT_ARRAY.equals(rangeTypeName)) {
          FloatArrayFS arrayFS = (FloatArrayFS) aFS.getFeatureValue(feat);
          if (arrayFS != null) {
            vals = arrayFS.toStringArray();
          }
        } else if (CAS.TYPE_NAME_BOOLEAN_ARRAY.equals(rangeTypeName)) {
          BooleanArrayFS arrayFS = (BooleanArrayFS) aFS.getFeatureValue(feat);
          if (arrayFS != null) {
            vals = arrayFS.toStringArray();
          }
        } else if (CAS.TYPE_NAME_BYTE_ARRAY.equals(rangeTypeName)) {
          ByteArrayFS arrayFS = (ByteArrayFS) aFS.getFeatureValue(feat);
          if (arrayFS != null) {
            vals = arrayFS.toStringArray();
          }
        } else if (CAS.TYPE_NAME_SHORT_ARRAY.equals(rangeTypeName)) {
          ShortArrayFS arrayFS = (ShortArrayFS) aFS.getFeatureValue(feat);
          if (arrayFS != null) {
            vals = arrayFS.toStringArray();
          }
        } else if (CAS.TYPE_NAME_LONG_ARRAY.equals(rangeTypeName)) {
          LongArrayFS arrayFS = (LongArrayFS) aFS.getFeatureValue(feat);
          if (arrayFS != null) {
            vals = arrayFS.toStringArray();
          }
        } else if (CAS.TYPE_NAME_DOUBLE_ARRAY.equals(rangeTypeName)) {
          DoubleArrayFS arrayFS = (DoubleArrayFS) aFS.getFeatureValue(feat);
          if (arrayFS != null) {
            vals = arrayFS.toStringArray();
          }
        }
        String attrVal;
        if (vals == null) {
          attrVal = "null";
        } else {
          StringBuffer buf = new StringBuffer();
          buf.append('[');
          for (int i = 0; i < vals.length - 1; i++) {
            buf.append(vals[i]);
            buf.append(',');
          }
          if (vals.length > 0) {
            buf.append(vals[vals.length - 1]);
          }
          buf.append(']');
          attrVal = buf.toString();
        }
        attrs.addAttribute("", featName, featName, "CDATA", attrVal);
      } else {
        // get value as FeatureStructure
        FeatureStructure fsVal = aFS.getFeatureValue(feat);
        if (fsVal == null) {
          attrs.addAttribute("", featName, featName, "CDATA", "null");
        } else {
          // record type name as value, and covered text if it's an annotation
          StringBuffer buf = new StringBuffer();
          buf.append(fsVal.getType().getShortName());

          if (fsVal instanceof AnnotationFS) {
            buf.append(" [");
            String str = ((AnnotationFS) fsVal).getCoveredText();
            if (str.length() > 64) {
              str = str.substring(0, 64) + "...";
            }
            buf.append(str);
            buf.append(']');
          }
          attrs.addAttribute("", featName, featName, "CDATA", buf.toString());
        }
      }
    }
    return attrs;
  }

  private void replaceInvalidXmlChars(char[] aChars) {
    for (int i = 0; i < aChars.length; i++) {
      if ((aChars[i] < 0x20 && aChars[i] != 0x09 && aChars[i] != 0x0A && aChars[i] != 0x0D)
          || (aChars[i] > 0xD7FF && aChars[i] < 0xE000) || aChars[i] == 0xFFFE
          || aChars[i] == 0xFFFF) {
        // System.out.println("Found invalid XML character: " + (int)aChars[i] + " at position " +
        // i); //temp
        aChars[i] = ' ';
      }
    }
  }


  /**
   * @return true if the output will be formatted
   */
  public boolean isFormattedOutput() {
    return formattedOutput;
  }
  
  /**
   * Set a flag that will be used to control how the ContentHandler
   * will be initialized - to either format or not, the generated Inline XML
   * @param formattedOutput true means to format the output, and is the default
   */
  public void setFormattedOutput(boolean formattedOutput) {
    this.formattedOutput = formattedOutput;
  }

}
