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

package org.apache.uima.adapter.vinci.util;

import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import org.apache.vinci.transport.document.AFrame;

/**
 * A SAX content handler that builds a VinciFrame corresponding to the SAX events received.
 * 
 * 
 */
public class SaxVinciFrameBuilder extends DefaultHandler implements ContentHandler {

  private Stack mOpenFrames;

  private String mCurrentFrameName;

  private Attributes mCurrentFrameAttrs;

  private StringBuffer mCharContentBuffer;

  /**
   * Sets the parent frame, to which frames built by the object will be added. This MUST be called
   * before parsing a document.
   * 
   * @param aParentFrame
   *          the parent frame
   */
  public void setParentFrame(AFrame aParentFrame) {
    mOpenFrames = new Stack();
    mOpenFrames.push(aParentFrame);
  }

  /**
   * @see org.xml.sax.ContentHandler#startDocument()
   */
  public void startDocument() throws SAXException {
    mCurrentFrameName = null;
    mCurrentFrameAttrs = null;
    mCharContentBuffer = new StringBuffer();
  }

  /**
   * Called for each start tag encountered.
   * 
   * @param namespaceURI
   *          Required if the namespaces property is true.
   * @param attributes
   *          The specified or defaulted attributes.
   * @param localName
   *          The local name (without prefix), or the empty string if Namespace processing is not
   *          being performed.
   * @param qualifiedName
   *          The qualified name (with prefix), or the empty string if qualified names are not
   *          available.
   */
  public void startElement(String namespaceURI, String localName, String qualifiedName,
                  org.xml.sax.Attributes attributes) throws SAXException {
    // I would like to create a VinciFrame here and put it on the
    // mOpenFrames stack, but I don't know whether to create a VinciFrame or
    // a FrameLeaf until I see whether there are child elements. So instead
    // the element information is stored temporarily until the next call to
    // startElement (indicating a non-leaf) or endElement (indicating a leaf)

    // first, check to see whether we have a frame for which this temporary info
    // was stored from the previous call to startElement.
    if (mCurrentFrameName != null) {
      // create a non-leaf frame
      AFrame frame = new AFrame();

      // add this frame to its parent frame
      // (the one on top of stack)
      AFrame parent = (AFrame) mOpenFrames.peek();
      org.apache.vinci.transport.Attributes vinciAttrs = parent.aadd(mCurrentFrameName, frame);

      // set attributes
      if (mCurrentFrameAttrs != null) {
        for (int i = 0; i < mCurrentFrameAttrs.getLength(); i++) {
          String attrName = getName(mCurrentFrameAttrs.getLocalName(i), mCurrentFrameAttrs
                          .getQName(i));
          vinciAttrs.fadd(attrName, mCurrentFrameAttrs.getValue(i));
        }
      }

      // put new frame on stack
      mOpenFrames.push(frame);
    }

    // now store the information for the new element
    String elemName = getName(localName, qualifiedName);
    mCurrentFrameName = elemName;
    mCurrentFrameAttrs = attributes;
  }

  /**
   * @see org.xml.sax.ContentHandler#characters(char[],int,int)
   */
  public void characters(char[] ch, int start, int length) {
    mCharContentBuffer.append(ch, start, length);
  }

  /**
   * @see org.xml.sax.ContentHandler#endElement(String,String,String)
   */
  public void endElement(String namespaceURI, String localName, String qualifiedName) {
    // if we're ending a leaf frame (which we know because mCurrentFrame is
    // non-null), we must create the appropriate FrameLeaf object and add
    // it to the top frame on the stack.
    if (mCurrentFrameName != null) {
      // the getLeafContent method just returns the contents of the
      // mCharContentBuffer, but exists so subclasses can override for
      // specialized behavior (such as supressing certain content).
      String leafContent = getLeafContent(mCurrentFrameName, mCurrentFrameAttrs, mCharContentBuffer);

      // add leaf to parent frame
      AFrame parent = (AFrame) mOpenFrames.peek();
      org.apache.vinci.transport.Attributes vinciAttrs = parent
                      .aadd(mCurrentFrameName, leafContent);

      // set attributes
      if (mCurrentFrameAttrs != null) {
        for (int i = 0; i < mCurrentFrameAttrs.getLength(); i++) {
          String attrName = getName(mCurrentFrameAttrs.getLocalName(i), mCurrentFrameAttrs
                          .getQName(i));
          vinciAttrs.fadd(attrName, mCurrentFrameAttrs.getValue(i));
        }
      }

      mCurrentFrameName = null;
      mCurrentFrameAttrs = null;
      mCharContentBuffer = new StringBuffer();
    } else {
      // ending a non-leaf frame; pop the stack
      mOpenFrames.pop();
    }

  }

  /**
   * Gets the content to be included in a FrameLeaf. This method just returns the contents of the
   * provided StringBuffer, but subclasses can override to provide specialized content.
   * 
   * @param aFrameName
   *          name of the FrameLeaf
   * @param aAttributes
   *          attributes of FrameLeaf
   * @param aContentBuf
   *          StringBuffer containing the character data obtained from the SAX parser
   * 
   * @return the data to be included in the Vinci FrameLeaf
   */
  protected String getLeafContent(String aFrameName, Attributes aAttributes,
                  StringBuffer aContentBuf) {
    return aContentBuf.toString();
  }

  /**
   * If the first String parameter is nonempty, return it, else return the second string parameter.
   * 
   * @param s1
   *          The string to be tested.
   * @param s2
   *          The alternate String.
   * @return s1 if it isn't empty, else s2.
   */
  protected String getName(String s1, String s2) {
    if (s1 == null || "".equals(s1))
      return s2;
    else
      return s1;
  }
}
