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
package org.apache.uima.json.jsoncas2.ser;

import java.io.IOException;
import java.util.Iterator;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.resource.metadata.TypeSystemDescription;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;

public class TypeSystemDeserializer extends CasDeserializer_ImplBase<TypeSystemDescription> {
  private static final long serialVersionUID = 7137336340824618031L;

  public TypeSystemDeserializer() {
    super(TypeSystemDescription.class);
  }

  @Override
  public TypeSystemDescription deserialize(JsonParser aParser, DeserializationContext aCtxt)
          throws IOException, JsonProcessingException {
    JsonNode node = aParser.readValueAsTree();

    if (!node.isObject()) {
      throw new JsonParseException(aParser, "Type system declaration must be a JSON object");
    }

    CAS cas = getCas(aCtxt);

    TypeSystemDescription tsd = UIMAFramework.getResourceSpecifierFactory()
            .createTypeSystemDescription();

    Iterator<String> typeNameIterator = node.fieldNames();
    while (typeNameIterator.hasNext()) {
      String typeName = typeNameIterator.next();
      node.get(typeName);
      // FIXME !!!
    }

    return tsd;
  }
}
