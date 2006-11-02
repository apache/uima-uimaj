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

package org.apache.uima.impl.analysis_engine.service.vinci.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

/**
 * Network utilities.
 */
public class NetworkUtil
{
  /**
   * Gets the InetAddress of the localhost.  Attempts to get a non-loopback address
   * (i.e. not 127.0.0.1) if at all possible.  This utility is better than
   * InetAddress.getLocalHost() since the latter often fails on Linux.
   * 
   * @return the InetAddress of the localhost.  This will be a non-loopback address
   *   if one could be found.
   *   
   * @throws UnknownHostException if the local host address could not be obtained
   */
  public static InetAddress getLocalHostAddress() throws UnknownHostException 
  {
    //try the straightforward call first
    InetAddress localhost = InetAddress.getLocalHost();
    if (!localhost.isLoopbackAddress())
    {
      return localhost;
    }
    
    //the above often fails on Linux.  Try enumerating all NetworkInterfaces.
    try
    {
      Enumeration networkInterfaces = NetworkInterface.getNetworkInterfaces();
      while (networkInterfaces.hasMoreElements())
      {
        NetworkInterface networkInterface = (NetworkInterface)networkInterfaces.nextElement();
        Enumeration addresses = networkInterface.getInetAddresses();
        while (addresses.hasMoreElements())
        {
          InetAddress address = (InetAddress)addresses.nextElement();
          if (address instanceof Inet4Address && !address.isLoopbackAddress()) 
          {
            return address;
          }
        }
      }
    }
    catch (SocketException e)
    {
      //couldn't get network interfaces.  Give up.
    }
    
    //give up and just return the loopback address
    return localhost;
  }
}
