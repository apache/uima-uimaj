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

import java.io.File;

import org.apache.uima.util.FileUtils;
import org.apache.vinci.transport.VinciClient;
import org.apache.vinci.transport.VinciFrame;

// TODO: Auto-generated Javadoc
/**
 * The Class VinciTAEClient.
 */
public class VinciTAEClient {

  /**
   * The main method.
   *
   * @param args the arguments
   */
  public static void main(String[] args) {
    System.out.println("Invoking the service...");
    try {
      String serviceName = args[0];
      VinciFrame query = new VinciFrame();
      VinciFrame response;

      if (args.length < 2) {
        query.fadd(Constants.VINCI_COMMAND, Constants.GETMETA);
        response = VinciClient.rpc(query, serviceName);
      } else {

        System.out.println("Analyzing Document...");
        File aFile = new File(args[1]);
        String fileData = FileUtils.file2String(aFile);

        VinciFrame data = new VinciFrame();
        VinciFrame key = new VinciFrame();

        key.fadd(Constants.VINCI_DETAG, fileData);
        data.fadd("KEYS", key);

        query.fadd(Constants.VINCI_COMMAND, Constants.ANNOTATE);
        query.fadd("DATA", data);

        System.out.println(query.toXML());
        response = VinciClient.rpc(query, serviceName);

      }
      // VinciFrame response = VinciClient.sen(query, serviceName);
      System.out.println("Response:\n" + response.toXML());
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println(e.getMessage());
    }

  }
}
