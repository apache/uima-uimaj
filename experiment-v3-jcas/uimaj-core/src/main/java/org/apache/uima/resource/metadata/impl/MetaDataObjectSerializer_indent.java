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

package org.apache.uima.resource.metadata.impl;

import java.util.Arrays;

import org.apache.uima.util.XMLSerializer.CharacterValidatingContentHandler;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;


class MetaDataObjectSerializer_indent extends MetaDataObjectSerializer_plain {
  
  /**
   * Heuristics for comment and whitespace processing
   * 
   * Example:
   *    <!-- at top -->
   * <a>   <!-- same line -->
   *   <b/>
   *   <d> <!-- cmt --> <e/> </d>
   *   <c/>  <!-- same line -->
   *   <!-- unusual case, following final one at a level -->
   * </a>  <!-- same line -->
   *   <!-- at bottom -->
   * 
   * Each element has 2 calls: 
   *     startElement, endElement
   *   Surround these with:
   *     maybeOutputCommentsBefore
   * maybeOutputCommentsAfter
   * 
   * Detect top level (by fact that parent is null), and for top level:
   *   collect all above -%gt; output before startelement  
   *     BUT, note that the sax parser doesn't do callbacks for text (blank lines) before the
   *       start element, so all we can collect are the comment lines.
   *   collect all below -%gt; output after endelement
   * 
   * For normal element node, "start":
   *   --> output before element
   *     collect all prev white space siblings up to the one that contains the first newline 
   *         because the prev white space siblings before and including that one will
   *         have been outputted as part of the previous start or end tag's "after element" processing
   * 
   *       if no nl assume comments go with previous element, and skip here
   *       (stop looking if get null for getPreviousSibling())
   *       (stop looking if get other than comment or ignorable whitespace)
   *         (ignorable whitespace not always distinguishable from text that is whitespace?)
   * 
   *   --> output after element:
   *     if children:    eg:  <start> <!-- cmt --> 
   *       collect all up to and including first nl before first Element child
   *         (stop at first Element node; if no nl, then the source had multiple elements on one line:
   * associate the comments and whitespace with previous (and output them).
   * 
   *     if no children: - means it's written 
   *          <xxx/> or 
   *          <xxx></xxx>  
   *              Note:  <xxx>   something  </xxx> not possible, because then it would have some text children
   *       output nothing - after comments will be done following endElement call
   * 
   * For normal element node, "end":
   *   --> output before element
   *       collect all after last child Element; skip all up to first nl (assume before that, the comment goes with last child node)
   *       if no nl (e.g.   </lastChild> <!--  cmt -->  </elementBeingEnded> )
   *         assume comments go with previous element, and skip here
   *       (stop looking if get null for getNextSibling())
   *       (stop looking if get Element)
   * 
   *     if no element children - output nothing  
   *   --> output after element    
   *       if this element has no successor sibling elements
   *         collect all up to the null
   *       else  
   *         collect all up to and including first nl from getNextSibling().
   *           (stop at first Element)
   * 
   * For implied element nodes (no Java model object corresponding)
   * We have only the "parent" node, and the element name.  Try to do matching on the element name
   * In this case, we always are working with the children in the Dom infoset; we have a last-outputted reference
   *   Scan from last-outputted, to find element match, and then use that element as the "root".       
   * 
   */

  static private String lineEnd = System.getProperty("line.separator");

  private static final char[] blanks = new char[80];
  static {Arrays.fill(blanks, ' ');}

  private static boolean hasElementChildNode(Node n) {
    for (Node c = n.getFirstChild(); (c != null); c = c.getNextSibling()) {
      if (c instanceof Element) {
        return true;
      }
    }
    return false;
  }
  
  private static boolean isWhitespaceText(Node n) {
    if (!(n instanceof Text)) {
      return false;
    }
    Text t = (Text) n;
    String s = t.getData();
    for (int i = 0; i < s.length(); i++) {
      if (!Character.isWhitespace(s.charAt(i))) {
        return false;
      }
    }
    return true;
  }
  
  /**
   * Dom parsers if not operating in validating mode can't distinguish between
   * ignorable white space and non-ignorable white space.
   * 
   * So we use a heuristic instead - we see if the text is whitespace only, and if so,
   * we consider it to be ignorable white space.
   * 
   * @param n
   * @return true if node is a comment or is ignorable whitespace (approximately)
   */
  private static boolean isCoIw(Node n) {
    return (n != null) && ((n instanceof Comment) || isWhitespaceText(n));
  }
  
  private static final char[] nlca = new char[] { '\n' };

  final private CharacterValidatingContentHandler cc;
  
  public MetaDataObjectSerializer_indent(CharacterValidatingContentHandler cc) {
    super(cc);  // required for plain version to handle some things
    this.cc = cc;
  }
  
  @Override
  public void saveAndAddNodeStore(Node infoset) {
    cc.setLastOutputNode(infoset);
    addNodeStore();
  }

  @Override
  public void deleteNodeStore() {
    cc.lastOutputNodeClearLevel();
  }
  
  @Override
  public void addNodeStore() {
    cc.lastOutputNodeAddLevel();
  }
  
  public void outputStartElement(Node node, String aNamespace,
      String localname, String qname, Attributes attributes) throws SAXException {
    if (null == localname) { // happens for <flowConstraints>
      // <fixedFlow> <== after outputting this,
      // called writePropertyAsElement
      // But there is no <array>...
      //s <node>A</node>
      // <node>B</node>
      return;
    }
    maybeOutputCoIwBeforeStart(node);
    cc.startElement(aNamespace, localname, qname, attributes);
    maybeOutputCoIwAfterStart(node);
  }
  
  @Override
  public void outputEndElement(Node node, String aNamespace,
      String localname, String qname) throws SAXException {
    if (null == localname) {
      return;
    }
    maybeOutputCoIwBeforeEnd(node);
    cc.endElement(aNamespace, localname, qname);
    maybeOutputCoIwAfterEnd(node);
  }
  
  @Override
  public void outputStartElementForArrayElement(Node node,
      String nameSpace, String localName, String qName, Attributes attributes) throws SAXException {
    outputStartElement(node, nameSpace, localName, qName, attributes);
  }

  /**
   * CoIw = Comment or IgnorableWhitespace
   * 
   */

  private void maybeOutputCoIwBeforeStart(Node node) throws SAXException {
    int indent = cc.getIndent();

    if (null == node) {
      if (!cc.prevNL) {
        outputNL();
        outputIndent(indent);
      }
      return;
    }
    if (node.getParentNode() instanceof Document) {

      // Special handling for top node:
      // The SAX parser doesn't do callbacks for whitespace that come before the top node.
      // It does do callbacks for comments, though.

      // For this case, we do (one time) insert of "nl" as follows:
      // 1 nl before top element
      // 1 nl before each preceeding comment

      outputNL();

      for (Node c = node.getParentNode().getFirstChild(); c != node; c = c.getNextSibling()) {
        if (c instanceof Comment) {
          outputCoIw(c);
          outputNL();
        }
      }
      return;
    }
    for (Node p = getFirstPrevCoIw(node); p != node; p = p.getNextSibling()) {
      outputCoIw(p);
    }
  }

  private void maybeOutputCoIwAfterStart(Node node) throws SAXException {
    cc.nextIndent();
    if (null == node || (!hasElementChildNode(node))) {
      cc.prevNL = false;
      return;
    }

    outputCoIwAfterElement(node.getFirstChild());
  }
  
  private void maybeOutputCoIwBeforeEnd(Node node) throws SAXException {
    int indent = cc.prevIndent();

    if (null == node || (!hasElementChildNode(node))) {
      if (null == node) {
        if (cc.prevWasEndElement) {
          outputNL();
          outputIndent(indent);
        }
      }
      return;
    }

    Node n = node.getLastChild();
    Node np = null;
    boolean newlineFound = false;
    for (Node p = n; p != null && !(p instanceof Element) && (p.getNodeType() != Node.ATTRIBUTE_NODE); p = p.getPreviousSibling()) {
      if (hasNewline(p)) {
        newlineFound = true;
      }
      np = p;
    }
    if (!newlineFound) {
      return;
    }
    for (Node o = skipUpToFirstAfterNL(np); o != null; o = o.getNextSibling()) {
      outputCoIw(o);
    }
  }

  private void maybeOutputCoIwAfterEnd(Node node) throws SAXException {
    if (null == node) {
      return;
    }
    outputCoIwAfterElement(node.getNextSibling());
  }

  /**
   * Output comments and ignorable whitespace after an element.
   * Comments following an element can either be grouped with the preceeding element or with the following one.
   *   e.g.    <element>   <!--comment 1-->
   *             <!--comment 2-->
   *             <!--comment 3-->
   * <subelement>
   * 
   *   We arbitrarily group comment 1 with the element, and comment 2 and 3 with the subelement.
   *   This is for purposes of when they get processed and put out.
   *   This also affects what happens when new elements are "inserted" by an editor.
   * 
   * This routine outputs only the whitespace and comment on the same line (e.g., 
   * it stops after outputting the ignorable whitespace that contains a nl.)
   * If find text which is not whitespace, don't output anything.
   *   Use case: 
   * <someElement> some text
   * 
   * @param startNode - the node corresponding to the start or end element just outputted
   * @throws DOMException passthru
   * @throws SAXException passthru
   */
  private void outputCoIwAfterElement(Node startNode) throws DOMException, SAXException {
    if (null != startNode) {
      // scan for last node to output
      // we do this first so we output nothing if have non-blank text somewhere
      Node lastNode = null;
      for (Node n = startNode;
                (n != null) && 
                ((n instanceof Comment) || (n instanceof Text));  // keep going as long as have non-Element nodes
      n = n.getNextSibling()) {
        if ((n instanceof Text) && !isWhitespaceText(n)) {
          return;  // catch case <someElement> some text
        }
        lastNode = n;
      }
      if (null == lastNode) {
        return;
      }
      for (Node n = startNode;; n = n.getNextSibling()) {
        outputCoIw(n);
        if (hasNewline(n)) {
          cc.prevNL = true;
          return;  // return after outputting up to and including 1st new line
        }
        if (n == lastNode) {
          return;
        }
      }
    }
    cc.prevNL = false;
  }
  
  /**
   * Scan backwards from argument node, continuing until get something other than
   * comment or ignorable whitespace.
   * Return the first node after a nl 
   *   If no nl found, return original node
   * 
   * NOTE: never called with original == the top node
   * @param r - guaranteed non-null
   * @return first node after a new line
   */
  private Node getFirstPrevCoIw(Node original) {
    boolean newlineFound = false;
    Node p = original; // tracks one behind r
    for (Node r = p.getPreviousSibling(); isCoIw(r); r = r.getPreviousSibling()) {
      if (hasNewline(r)) {
        newlineFound = true;
      }
      p = r;
    }
    if (!newlineFound) {
      return original;
    }
    return skipUpToFirstAfterNL(p);
  }

  /**
   * Skip nodes going forwards until find one with a nl, then return the one following
   * @param n must not be null, and there must be a NL in the siblings
   * @return node following the one with a new line
   */
  private Node skipUpToFirstAfterNL(Node n) {
    while (!hasNewline(n)) {
      n = n.getNextSibling();
    }
    return n.getNextSibling();
  }

  private boolean hasNewline(Node n) {
    if (n instanceof Comment) {
      return false;
    }
    CharacterData c = (CharacterData) n;
    return (-1) != c.getData().indexOf('\n');
  }
  
  /**
   * Scan from last output node the child nodes, looking for a matching element.
   * Side effect if found - set lastoutput node to the found one.
   * @param elementName
   * @return null (if no match) or matching node
   */
  @Override
  public Node findMatchingSubElement(String elementName) {
    if (null == elementName) {
      return null;
    }
    Node lastOutput = cc.getLastOutputNode();
    Node n = null;

    if (lastOutput == null) {
      lastOutput = cc.getLastOutputNodePrevLevel();
      if (lastOutput == null) {
        return null;
      }
      n = lastOutput.getFirstChild();
    } else {
      n = lastOutput.getNextSibling();
    }
    for (; n != null; n = n.getNextSibling()) {
      if ((n instanceof Element) && 
          elementName.equals(((Element)n).getTagName())) {
        cc.setLastOutputNode(n);
        return n;
      }
    }
    return null;
  }
  
  /*
   * If necessary replace any internal new-lines with the platform's line separator
   */
  private void outputCoIw(Node p) throws DOMException, SAXException {
    if (p instanceof Comment) {
      String cmt = ((Comment) p).getData();
      if (!lineEnd.equals("\n")) {
        cmt = cmt.replace("\n", lineEnd);
      }
      cc.comment(cmt.toCharArray(), 0, cmt.length());
    } else {
      String s = p.getTextContent();
      cc.characters(s.toCharArray(), 0, s.length());
    }

  }

  private void outputIndent(int indent) throws SAXException {
    cc.ignorableWhitespace(blanks, 0, Math.min(80, indent));
  }

  private void outputNL() throws SAXException {
    cc.ignorableWhitespace(nlca, 0, 1);
    cc.prevNL = true;
  }

}