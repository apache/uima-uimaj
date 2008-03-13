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

package org.apache.uima.tutorial.ex3;

import java.text.BreakIterator;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.tutorial.DateAnnot;
import org.apache.uima.tutorial.DateTimeAnnot;
import org.apache.uima.tutorial.TimeAnnot;

/**
 * Simple Date/Time annotator.
 */
public class TutorialDateTime extends JCasAnnotator_ImplBase {

  static abstract class Maker {
    abstract Annotation newAnnotation(JCas jcas, int start, int end);
  }

  JCas jcas;

  String input;

  ParsePosition pp = new ParsePosition(0);

  // Static vars holding patterns, and function pointers

  // n:nn nn:nn followed optionally with AM or PM

  // .*? (any number of arbitrary chars, minimum, not greedy)
  // \b followed by a word boundary
  // [0-2]? followed by the optionally the first digit, a 0, 1, or 2
  // \d:[0-6]\d followed by a digit and the colon char,and minutes
  // \s*?(AM|PM)? followed by optional white space (non greedy) and AM or PM
  static final Pattern hoursMinutesPattern = Pattern
          .compile("(?s)\\b([0-2]?\\d:[0-5]\\d\\s*(AM\\W|PM\\W|am\\W|pm\\W)?)");

  //
  static final DateFormat dfTimeShort = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.US);

  // .*? (any number of artibrary chars, non greedy
  // \b word boundary
  // [0-1]? optional first digit
  // \d digit of month
  // /
  // [0-3]? optional day of month 1st digit
  // \d
  // ((/[1-2]\d\d\d)|(/\d\d)|\s) // year is /nnnn or /nn or missing
  static final Pattern numericDatePattern = Pattern
          .compile("(?s)\\b([0-1]?\\d/[0-3]?\\d((/[1-2]\\d\\d\\d)|(/\\d\\d))?)\\W");

  static final DateFormat dfDateShort = DateFormat.getDateInstance(DateFormat.SHORT, Locale.US);

  // .*? (any number of artibrary chars, non greedy
  // \b word boundary
  // [Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Sept|Oct|Nov|Dec] Month
  // \.? optional period
  // \s+
  // [0-3]? optional day of month 1st digit
  // \d
  // (((,\s+)?[1-2]\d\d\d\W)|((,\s+)?\d\d\W)|\W) // year is /nnnn or /nn or missing
  static final String shortMonthNames = "(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Sept|Oct|Nov|Dec)";

  static final Pattern mediumDatePattern = Pattern.compile("(?s)\\b(" + shortMonthNames
          + "\\.?\\s[0-3]?\\d(((,\\s+)?[1-2]\\d\\d\\d)|((,\\s+)?\\d\\d))?)\\W");

  static final DateFormat dfDateMedium = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.US);

  // for long month names, exclude May since it is covered by short month names
  static final String longMonthNames = "(January|February|March|April|June|July|August|September|October|November|December)";

  static final Pattern longDatePattern = Pattern.compile("(?s)\\b(" + longMonthNames
          + "\\s[0-3]?\\d(((,\\s+)?[1-2]\\d\\d\\d)|((,\\s+)?\\d\\d))?)\\W");

  static final DateFormat dfDateLong = DateFormat.getDateInstance(DateFormat.LONG, Locale.US);

  static final NumberFormat numberFormat = NumberFormat.getInstance(Locale.US);

  // function pointers for new instances
  static final Maker dateAnnotationMaker = new Maker() {
    Annotation newAnnotation(JCas jcas, int start, int end) {
      return new DateAnnot(jcas, start, end);
    }
  };

  static final Maker timeAnnotationMaker = new Maker() {
    Annotation newAnnotation(JCas jcas, int start, int end) {
      return new TimeAnnot(jcas, start, end);
    }
  };

  static final String defaultYear = "2003";

  // PROCESS
  /**
   * The ResultSpecification controls what gets produced. For example, to only produce
   * DateAnnotations, change the descriptor for this component to specify it outputs only that type.
   */
  public void process(JCas aJCas) {
    jcas = aJCas;
    input = jcas.getDocumentText();

    // Create Annotations
    ResultSpecification resultSpec = getResultSpecification();
    if (resultSpec.containsType("org.apache.uima.tutorial.TimeAnnot",aJCas.getDocumentLanguage()))
      makeAnnotations(timeAnnotationMaker, hoursMinutesPattern, dfTimeShort);
    if (resultSpec.containsType("org.apache.uima.tutorial.DateAnnot",aJCas.getDocumentLanguage()))
      makeAnnotations(dateAnnotationMaker, numericDatePattern, dfDateShort);
    if (resultSpec.containsType("org.apache.uima.tutorial.DateAnnot",aJCas.getDocumentLanguage()))
      makeAnnotations(dateAnnotationMaker, mediumDatePattern, dfDateMedium);
    if (resultSpec.containsType("org.apache.uima.tutorial.DateAnnot",aJCas.getDocumentLanguage()))
      makeAnnotations(dateAnnotationMaker, longDatePattern, dfDateLong);
  }

  // HELPER METHODS

  void makeAnnotations(Maker m, BreakIterator b) {
    b.setText(input);
    for (int end = b.next(), start = b.first(); end != BreakIterator.DONE; start = end, end = b
            .next()) {

      // eliminate all-whitespace tokens
      boolean isWhitespace = true;
      for (int i = start; i < end; i++) {
        if (!Character.isWhitespace(input.charAt(i))) {
          isWhitespace = false;
          break;
        }
      }
      if (!isWhitespace) {
        m.newAnnotation(jcas, start, end).addToIndexes();
      }
    }
  }

  void makeAnnotations(Maker m, Pattern pattern, DateFormat dateFormat) {
    Matcher matcher = pattern.matcher(input);
    String matched;
    while (matcher.find()) {
      int start = matcher.start(1);
      matched = fixUpDateTimeStrings(matcher.group(1));
      DateTimeAnnot dtAnnot = (DateTimeAnnot) m.newAnnotation(jcas, start, matcher.end(1));
      pp.setIndex(0);
      Date dtSpec = dateFormat.parse(matched, pp);
      // System.out.println(dtAnnot.dtSpec);
      if (dtSpec != null) {
        dtAnnot.setShortDateString(dfDateShort.format(dtSpec));
      }
      dtAnnot.addToIndexes();
    }
  }

  String fixUpDateTimeStrings(String s) {
    String av; // append value
    pp.setIndex(0);
    if (-1 < s.indexOf(":")) { // have time string
      if (s.endsWith("AM") | s.endsWith("PM") | s.endsWith("am") | s.endsWith("pm"))
        return s;
      else {
        int hour = numberFormat.parse(s, pp).intValue();
        if (0 == hour)
          av = " AM";
        else if (hour < 9)
          av = " PM";
        else
          av = " AM";
        return s + av;
      }
    }

    // have date string
    return s + ", " + defaultYear; // in case no year available
  }

}
