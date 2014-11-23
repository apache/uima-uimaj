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
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.uima.UIMARuntimeException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FSMatchConstraint;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.FloatArrayFS;
import org.apache.uima.cas.IntArrayFS;
import org.apache.uima.cas.StringArrayFS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Generates an inline XML representation of a CAS. Annotation types are represented as XML tags,
 * features are represented as attributes. Note that features whose values are FeatureStructures are
 * not represented.
 * 
 * @deprecated As of v2.0, use {@link org.apache.uima.util.CasToInlineXml} instead.
 */
@Deprecated
public class TCasToInlineXml implements TCasFormatter {
  /*
   * This destroy method does nothing.
   * 
   * @see org.apache.uima.resource.Resource#destroy()
   */
  public void destroy() {
  }

  /*
   * @see org.apache.uima.util.TCasFormatter#format(CAS)
   */
  public String format(CAS aCAS) throws CASException {
    return generateXML(aCAS, null);
  }

  /*
   * @see org.apache.uima.util.TCasFormatter#format(CAS, FSMatchConstraint)
   */
  public String format(CAS aCAS, FSMatchConstraint aFilter) throws CASException {
    return generateXML(aCAS, aFilter);
  }

  /*
   * Generates inline XML from a CAS.
   * 
   * @param aCAS
   *          CAS to generate from
   */
  public String generateXML(CAS aCAS) throws CASException {
    return generateXML(aCAS, null);
  }

  /*
   * Generates inline XML from a CAS.
   * 
   * @param aCAS
   *          CAS to generate from
   * @param aFilter
   *          constraint that determines which annotations are included in the output. If null (or
   *          omitted), all annotations are included.
   */
  public String generateXML(CAS aCAS, FSMatchConstraint aFilter) throws CASException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    XMLSerializer sax2xml = new XMLSerializer(byteArrayOutputStream);

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
    ArrayList<AnnotationFS> stack = new ArrayList<AnnotationFS>();
    int pos = 0;

    try {
      ContentHandler handler = sax2xml.getContentHandler();
      handler.startDocument();
      // write an artificial start tag
      handler.startElement("", "Document", "Document", new AttributesImpl());
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
              handler.characters(docCharArray, pos, nextAnnot.getBegin() - pos);
              pos = nextAnnot.getBegin();
              handler.startElement("", nextAnnot.getType().getName(),
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
            handler.characters(docCharArray, pos, curAnnot.getEnd() - pos);
            pos = curAnnot.getEnd();
          } catch (StringIndexOutOfBoundsException e) {
            System.err.println("Invalid annotation range: " + curAnnot.getBegin() + ","
                    + curAnnot.getEnd() + " in document of length " + docText.length());
          }
          handler.endElement("", curAnnot.getType().getName(), curAnnot.getType().getName());

          // pop next containing annotation off stack
          curAnnot = stack.remove(stack.size() - 1);
        }
      }

      // finished writing all start tags, now finish up
      if (curAnnot != null) {
        try {
          handler.characters(docCharArray, pos, curAnnot.getEnd() - pos);
          pos = curAnnot.getEnd();
        } catch (StringIndexOutOfBoundsException e) {
          System.err.println("Invalid annotation range: " + curAnnot.getBegin() + ","
                  + curAnnot.getEnd() + "in document of length " + docText.length());
        }
        handler.endElement("", curAnnot.getType().getName(), curAnnot.getType().getName());

        while (!stack.isEmpty()) {
          curAnnot = stack.remove(stack.size() - 1); // pop
          if (curAnnot == null) {
            break;
          }
          try {
            handler.characters(docCharArray, pos, curAnnot.getEnd() - pos);
            pos = curAnnot.getEnd();
          } catch (StringIndexOutOfBoundsException e) {
            System.err.println("Invalid annotation range: " + curAnnot.getBegin() + ","
                    + curAnnot.getEnd() + "in document of length " + docText.length());
          }
          handler.endElement("", curAnnot.getType().getName(), curAnnot.getType().getName());
        }
      }

      if (pos < docCharArray.length) {
        handler.characters(docCharArray, pos, docCharArray.length - pos);
      }
      handler.endElement("", "Document", "Document");
      handler.endDocument();

      // return XML string
      return new String(byteArrayOutputStream.toByteArray(),"UTF-8");
    } catch (SAXException e) {
      throw new UIMARuntimeException(e);
    } catch (UnsupportedEncodingException e) {
      throw new UIMARuntimeException(e);
    }
  }

  private final Attributes getFeatureAttributes(FeatureStructure aFS, CAS aCAS) {
    AttributesImpl attrs = new AttributesImpl();

    Type stringType = aCAS.getTypeSystem().getType(CAS.TYPE_NAME_STRING);

    List<Feature> aFeatures = aFS.getType().getFeatures();
    Iterator<Feature> iter = aFeatures.iterator();
    while (iter.hasNext()) {
      Feature feat = iter.next();
      String featName = feat.getShortName();
      // how we get feature value depends on feature's range type)
      String rangeTypeName = feat.getRange().getName();
      if (aCAS.getTypeSystem().subsumes(stringType, feat.getRange())) // must check for subtypes
      // of string
      {
        String str = aFS.getStringValue(feat);
        if (str == null) {
          attrs.addAttribute("", featName, featName, "CDATA", "null");
        } else {
          if (str.length() > 64) {
            str = str.substring(0, 64) + "...";
          }
          attrs.addAttribute("", featName, featName, "CDATA", str);
        }
      } else if (CAS.TYPE_NAME_INTEGER.equals(rangeTypeName)) {
        attrs
                .addAttribute("", featName, featName, "CDATA", Integer.toString(aFS
                        .getIntValue(feat)));
      } else if (CAS.TYPE_NAME_FLOAT.equals(rangeTypeName)) {
        attrs
                .addAttribute("", featName, featName, "CDATA", Float.toString(aFS
                        .getFloatValue(feat)));
      } else if (CAS.TYPE_NAME_STRING_ARRAY.equals(rangeTypeName)) {
        StringArrayFS arrayFS = (StringArrayFS) aFS.getFeatureValue(feat);
        if (arrayFS == null) {
          attrs.addAttribute("", featName, featName, "CDATA", "null");
        } else {
          StringBuffer buf = new StringBuffer();
          String[] vals = arrayFS.toArray();
          buf.append('[');
          for (int i = 0; i < vals.length - 1; i++) {
            buf.append(vals[i]);
            buf.append(',');
          }
          if (vals.length > 0) {
            buf.append(vals[vals.length - 1]);
          }
          buf.append(']');
          attrs.addAttribute("", featName, featName, "CDATA", buf.toString());
        }
      } else if (CAS.TYPE_NAME_INTEGER_ARRAY.equals(rangeTypeName)) {
        IntArrayFS arrayFS = (IntArrayFS) aFS.getFeatureValue(feat);
        if (arrayFS == null) {
          attrs.addAttribute("", featName, featName, "CDATA", "null");
        } else {
          StringBuffer buf = new StringBuffer();
          int[] vals = arrayFS.toArray();
          buf.append('[');
          for (int i = 0; i < vals.length - 1; i++) {
            buf.append(vals[i]);
            buf.append(',');
          }
          if (vals.length > 0) {
            buf.append(vals[vals.length - 1]);
          }
          buf.append(']');
          attrs.addAttribute("", featName, featName, "CDATA", buf.toString());
        }
      } else if (CAS.TYPE_NAME_FLOAT_ARRAY.equals(rangeTypeName)) {
        FloatArrayFS arrayFS = (FloatArrayFS) aFS.getFeatureValue(feat);
        if (arrayFS == null) {
          attrs.addAttribute("", featName, featName, "CDATA", "null");
        } else {
          StringBuffer buf = new StringBuffer();
          float[] vals = arrayFS.toArray();
          buf.append('[');
          for (int i = 0; i < vals.length - 1; i++) {
            buf.append(vals[i]);
            buf.append(',');
          }
          if (vals.length > 0) {
            buf.append(vals[vals.length - 1]);
          }
          buf.append(']');
          attrs.addAttribute("", featName, featName, "CDATA", buf.toString());
        }
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
}
