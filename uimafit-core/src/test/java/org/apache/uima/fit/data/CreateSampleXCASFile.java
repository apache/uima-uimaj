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
package org.apache.uima.fit.data;

import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.impl.XCASSerializer;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.testing.factory.TokenBuilder;
import org.apache.uima.fit.type.Sentence;
import org.apache.uima.fit.type.Token;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.XMLSerializer;
import org.xml.sax.SAXException;

/**
 */

public class CreateSampleXCASFile {

  public static void main(String[] args) throws UIMAException, SAXException, IOException {
    TokenBuilder<Token, Sentence> tokenBuilder = new TokenBuilder<Token, Sentence>(Token.class,
            Sentence.class, "pos", "stem");
    JCas jCas = JCasFactory.createJCas();
    // quote from http://www.gutenberg.org/files/20417/20417-h/20417-h.htm
    String text = "... the more knowledge advances the more it becomes possible to condense it into little books.";
    tokenBuilder.buildTokens(jCas, text,
            "... the more knowledge advances the more it becomes possible to condense it into little books . ",
            ". T M K A T M I B P T C I I L B .",
            "... the more knowledge advance the more it become possible to condense it into little book . ");

    FileOutputStream out = new FileOutputStream("src/test/resources/data/docs/test.xcas");
    XCASSerializer ser = new XCASSerializer(jCas.getTypeSystem());
    XMLSerializer xmlSer = new XMLSerializer(out, false);
    ser.serialize(jCas.getCas(), xmlSer.getContentHandler());
    out.close();
  }
}
