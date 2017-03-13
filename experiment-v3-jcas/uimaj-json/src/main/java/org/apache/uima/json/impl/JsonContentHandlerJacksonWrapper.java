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

package org.apache.uima.json.impl;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Arrays;

import javax.naming.OperationNotSupportedException;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter.FixedSpaceIndenter;

/**
 * Utility class that generates JSON output for UIMA descriptors and CASs
 * 
 * This class is built as a wrapper for a popular open-source implementation of JSON capabilities, "Jackson".
 * 
 * Unusually, it implements the ContentHandler interface, so it can be use with the existing code in UIMA which uses
 * content handlers for serialization.
 * 
 * Because of this, it wraps the IOExceptions that the Jackson package throws, into SAXExceptions that ContentHandlers throw.
 * 
 * Use: Create an instance, specifying the output as a Writer or OutputStream or File
 *        (These three forms are supported by the underlying Jackson impl)
 *        Specify also if doing pretty-printing
 *      Call from other serialization class that does walking (either MetaDataObject_impl or XmiCasSerializer),
 *        to this instance
 *        
 * This class is exposes the Jackson "Generator" API for streaming, and 
 * instances of the Jackson Factory instance for configuring.
 *        
 * The caller uses both this class and the Jackson Generator class.
 * 
 * This class lets the underlying Jackson PrettyPrinter classes track the indent level.
 * PrettyPrinting is implemented via customization of the Jackson PrettyPrinting classes
 * 
 */
public class JsonContentHandlerJacksonWrapper implements ContentHandler  {

  private final static char[] BLANKS = new char[80];
  static {
    Arrays.fill(BLANKS,  ' ');
  }
  
  public final static String SYSTEM_LINE_FEED;
  static {
      String lf = System.getProperty("line.separator");
      SYSTEM_LINE_FEED = (lf == null) ? "\n" : lf;
  }
    
  // this value is kept here, rather than in the caller, because some callers (e.g. MetaDataObject)
  //   have many instances (each element could be an instance).
  private final boolean isFormattedOutput;  // set true if pretty printing
  
  public boolean isFormattedOutput() {
    return isFormattedOutput;
  }

  private final JsonGenerator jg;           // the underlying Jackson Generator
  public JsonGenerator getJsonGenerator() {
    return jg;
  }
  
  private static JsonGenerator createGenerator(JsonFactory f, Object o) throws SAXException {
    try {
      if (o instanceof Writer) {
        return f.createGenerator((Writer)o);
      }
      if (o instanceof OutputStream) {
        return f.createGenerator((OutputStream)o);
      }
      if (o instanceof File) {
        return f.createGenerator((File)o, JsonEncoding.UTF8);
      }
      throw new RuntimeException(new OperationNotSupportedException(String.format("Object must be a Writer, OutputStream, or File, but was of class %s",
          o.getClass().getName())));
    } catch (IOException e) {
      throw new SAXException(e);
    }
  }

  private boolean doNl;               // a flag set by users, that does a newline at the next "significant" output 
  private final UimaJsonPrettyPrinter uimaPrettyPrinter;  // set to null or an instance of the prettyprinter

  /*
   * Constructors
   *   Variants:
   *     JsonFactory - a new one created, or one supplied
   *     isFormattedOutput - a pretty print flag, default false
   */
  
  /**
   * Makes a Json content handler that sends its output to the specified destination
   * @param destination - can be a File, an OutputStream, or a Writer
   * @throws SAXException wrapping an IOException
   */
  public JsonContentHandlerJacksonWrapper(Object destination) throws SAXException {
    this(new JsonFactory(), destination);
  }
  
  /**
   * Makes a Json content handler, using a specified JsonFactory instance that can 
   *   be configured according to the Jackson implementation.
   * The resulting content handler will send its output to the specified destination
   * @param jsonFactory -
   * @param o - where the output goes
   * @throws SAXException wrapping an IOException
   */
  public JsonContentHandlerJacksonWrapper(JsonFactory jsonFactory, Object o) throws SAXException {
    this(createGenerator(jsonFactory, o));
  }

  /**
   * Makes a Json content handler, and
   *   specifies a prettyprinting boolean flag (default is no prettyprinting).
   * The resulting content handler will send its output to the specified destination
   * @param o - where the output goes
   * @param isFormattedOutput -
   * @throws SAXException wrapping an IOException
   */
  public JsonContentHandlerJacksonWrapper(Object o, boolean isFormattedOutput) throws SAXException {
    this(new JsonFactory(), o, isFormattedOutput);
  }
  
  /**
   * Makes a Json content handler, using a specified JsonFactory instance that can 
   *   be configured according to the Jackson implementation, and
   *   specifies a prettyprinting boolean flag (default is no prettyprinting).
   * The resulting content handler will send its output to the specified destination
   * @param jsonFactory -
   * @param o where the output goes
   * @param isFormattedOutput - true for pretty printing
   * @throws SAXException wrapping an IOException
   */
  public JsonContentHandlerJacksonWrapper(JsonFactory jsonFactory, Object o, boolean isFormattedOutput) throws SAXException {
    this(
          createGenerator((null == jsonFactory) ? new JsonFactory() : jsonFactory, o), 
          isFormattedOutput);
  }
    
  /*  C o m m o n  */
  /**
   * Makes a Json content handler, using a specified JsonGenerator instance
   * @param jsonGenerator -
   */  
  public  JsonContentHandlerJacksonWrapper(JsonGenerator jsonGenerator) {this(jsonGenerator, false);}

  /**
   * Makes a Json content handler, using a specified JsonGenerator instance
   * @param jsonGenerator -
   * @param isFormattedOutput - set to true for prettyprinting, default is false
   */  
  public  JsonContentHandlerJacksonWrapper(JsonGenerator jsonGenerator, boolean isFormattedOutput) {
    this.jg = jsonGenerator;
    this.isFormattedOutput = isFormattedOutput;
    if (isFormattedOutput) {
      uimaPrettyPrinter = new UimaJsonPrettyPrinter();
      uimaPrettyPrinter.withoutSpacesInObjectEntries();
      jg.setPrettyPrinter(uimaPrettyPrinter);
    } else {
      uimaPrettyPrinter = null;
    }
  }

  /**
   * Call this to indicate that the prettyprinter should write a new line just before the next significant output.
   *   It won't do this before a "comma", and some other punctuation.
   * Has no effect if no prettyprinting is being done.
   */
  public void writeNlJustBeforeNext() {
    doNl = true;
  }
  
  /* prettyPrinting */
    
  private class UimaJsonPrettyPrinter extends DefaultPrettyPrinter implements PrettyPrinter {

    private static final long serialVersionUID = 1L;
    
    /*
     * add a new line after separators, and before array or object entries
     * (non-Javadoc)
     * @see com.fasterxml.jackson.core.util.DefaultPrettyPrinter#writeObjectEntrySeparator(com.fasterxml.jackson.core.JsonGenerator)
     */
    @Override
    public void writeObjectEntrySeparator(JsonGenerator jg) throws IOException, JsonGenerationException {
      super.writeObjectEntrySeparator(jg);
      maybeOutputNlOrBlank(jg);     
    }

    @Override
    public void writeArrayValueSeparator(JsonGenerator jg) throws IOException, JsonGenerationException {
      super.writeArrayValueSeparator(jg);
      maybeOutputNlOrBlank(jg);
    }

    @Override
    public void beforeArrayValues(JsonGenerator jg) throws IOException, JsonGenerationException {
      maybeOutputNl(jg);
    }

    @Override
    public void beforeObjectEntries(JsonGenerator jg) throws IOException, JsonGenerationException {
      maybeOutputNl(jg);
    }
    
    private void maybeOutputNl(JsonGenerator jg) throws IOException {
      if (doNl) {
        maybeOutputNlIndent();
        doNl = false;
      }      
    }
    
    private void maybeOutputNlOrBlank(JsonGenerator jg) throws IOException {
      if (doNl) {
        maybeOutputNl(jg);  // resets doNl
      } else {
        jg.writeRaw(' ');
      }
    }
    
    private void maybeOutputNlIndent () throws IOException {
      if (isFormattedOutput) {
        jg.writeRaw(SYSTEM_LINE_FEED);
        jg.writeRaw(BLANKS, 0, Math.min(BLANKS.length, _nesting << 1)); // nesting * 2 for spaces per level
      }
    } 
  }
  
  /**
   * Assumes a Json object has been started, and adds property fields to it
   * This method allows reusing common code in the caller.
   * The attr values can be 
   *   arrays (expressed as a valid Json String including the separators, used by XmiCasSerializer only 
   *   strings (which will be scanned for needed escaping and surrounded by necessary quotes
   *   other (which are assumed to not need surrounding quotes
   * @param atts
   */
  private void outputAttrsAsProperties(Attributes atts) {
    if (null != atts) {
      try {        
        for (int i = 0; i < atts.getLength(); i++) {
          String val = atts.getValue(i);
          if (val != null && (!val.equals(""))) {
            final String prefix = atts.getQName(i);
            final String attType = atts.getType(i);
            if ("array".equals(atts.getType(i))) {
              jg.writeArrayFieldStart(prefix);
              jg.writeRawValue(val);  // assumes the caller has formatted the array values properly
                                      // caller 
              jg.writeEndArray();
              continue;
            }
            
            if ("string".equals(attType)) {
              jg.writeStringField(prefix, val);
              continue;
            } 
            
            if ("boolean".equals(attType)) {
              jg.writeBooleanField(prefix, val.equals("true"));
              continue;
            } 
                        
            jg.writeFieldName(prefix);
            jg.writeRawValue(val);     
          }
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }        
  }

  
  /* *****************************
   * mostly unused methods to make this 
   * a ContentHandler
   *******************************/
  private void unsupported() { throw new UnsupportedOperationException();} 
  public void characters(char[] ch, int start, int length) throws SAXException {unsupported();}
  public void endDocument() throws SAXException {}
  public void endPrefixMapping(String prefix) throws SAXException {}
  public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {unsupported();}
  public void processingInstruction(String target, String data) throws SAXException {unsupported();}
  public void setDocumentLocator(Locator locator) {unsupported();}
  public void skippedEntity(String name) throws SAXException {unsupported();}
  public void startDocument() throws SAXException {}
  public void startPrefixMapping(String prefix, String uri) throws SAXException {}
  public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
    try {
      if (qName != null) {
        jg.writeStartObject();
        jg.writeFieldName(qName);
        jg.writeStartObject();
      }
      outputAttrsAsProperties(atts);
    } catch (IOException e) {
      throw new SAXException(e);
    }
  }
  
  public void endElement(String uri, String localName, String qName) throws SAXException {
    try {
    if (null != qName) {
      jg.writeEndObject();
    } 
    jg.writeEndObject();
    } catch (IOException e) {
      throw new SAXException(e);
    }
  }

  
  public void withoutNl() {
    if (isFormattedOutput) {
      uimaPrettyPrinter.indentObjectsWith(FSIWN);
      uimaPrettyPrinter.indentArraysWith(FSIWN);
    }
  }
  
  public void withNl() {
    if (isFormattedOutput) {
      uimaPrettyPrinter.indentObjectsWith(DefaultPrettyPrinter.Lf2SpacesIndenter.instance);
    }
  }
  
  private static class FixedSpaceIndenterWithNesting extends FixedSpaceIndenter {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean isInline() { return false; }  
  }
  
  private static final FixedSpaceIndenterWithNesting FSIWN = new FixedSpaceIndenterWithNesting();

}
