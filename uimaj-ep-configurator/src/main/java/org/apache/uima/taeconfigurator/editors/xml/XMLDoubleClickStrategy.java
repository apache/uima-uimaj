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

package org.apache.uima.taeconfigurator.editors.xml;

import org.eclipse.jface.text.*;


/**
 * The Class XMLDoubleClickStrategy.
 */
public class XMLDoubleClickStrategy implements ITextDoubleClickStrategy {
  
  /** The text. */
  protected ITextViewer fText;

  /* (non-Javadoc)
   * @see org.eclipse.jface.text.ITextDoubleClickStrategy#doubleClicked(org.eclipse.jface.text.ITextViewer)
   */
  @Override
  public void doubleClicked(ITextViewer part) {
    int pos = part.getSelectedRange().x;

    if (pos < 0)
      return;

    fText = part;

    if (!selectComment(pos)) {
      selectWord(pos);
    }
  }

  /**
   * Select comment.
   *
   * @param caretPos the caret pos
   * @return true, if successful
   */
  protected boolean selectComment(int caretPos) {
    IDocument doc = fText.getDocument();
    int startPos, endPos;

    try {
      int pos = caretPos;
      char c = ' ';

      while (pos >= 0) {
        c = doc.getChar(pos);
        if (c == '\\') {
          pos -= 2;
          continue;
        }
        if (c == '\n' || c == '\"')
          break;
        --pos;
      }

      if (c != '\"')
        return false;

      startPos = pos;

      pos = caretPos;
      int length = doc.getLength();
      c = ' ';

      while (pos < length) {
        c = doc.getChar(pos);
        if (c == Character.LINE_SEPARATOR || c == '\"')
          break;
        ++pos;
      }
      if (c != '\"')
        return false;

      endPos = pos;

      int offset = startPos + 1;
      int len = endPos - offset;
      fText.setSelectedRange(offset, len);
      return true;
    } catch (BadLocationException x) {
    }

    return false;
  }

  /**
   * Select word.
   *
   * @param caretPos the caret pos
   * @return true, if successful
   */
  protected boolean selectWord(int caretPos) {

    IDocument doc = fText.getDocument();
    int startPos, endPos;

    try {

      int pos = caretPos;
      char c;

      while (pos >= 0) {
        c = doc.getChar(pos);
        if (!Character.isJavaIdentifierPart(c))
          break;
        --pos;
      }

      startPos = pos;

      pos = caretPos;
      int length = doc.getLength();

      while (pos < length) {
        c = doc.getChar(pos);
        if (!Character.isJavaIdentifierPart(c))
          break;
        ++pos;
      }

      endPos = pos;
      selectRange(startPos, endPos);
      return true;

    } catch (BadLocationException x) {
    }

    return false;
  }

  /**
   * Select range.
   *
   * @param startPos the start pos
   * @param stopPos the stop pos
   */
  private void selectRange(int startPos, int stopPos) {
    int offset = startPos + 1;
    int length = stopPos - offset;
    fText.setSelectedRange(offset, length);
  }
}
