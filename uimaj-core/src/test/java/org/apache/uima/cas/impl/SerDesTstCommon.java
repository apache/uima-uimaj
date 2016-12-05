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
package org.apache.uima.cas.impl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;

/**
 * Serializer and Deserializer testing
 * Common code for testing
 * 
 * Has main method for creating resources to use in testing
 *   will update resources in SerDes4 or 6.  If you do this by mistake, just revert those resources.
 *   
 * Multi-threading:  
 * Create one instance of this class per instance of using class
 *   ** Not one instance per "setup" call **
 */
public abstract class SerDesTstCommon extends TestCase {

  // FIXME need to understand why includeUid is false, seems to be disabling some testing Nov 2016
  private static final boolean includeUid = false;
  private static final AtomicInteger aint = includeUid? new AtomicInteger(0) : null;
  
  /**
   * A version of Random that can pull numbers from a stream instead
   *   - useful for reproducing a previously generated set of random numbers
   *   - used when running against previously generated serialized data
   *
   */
  class MyRandom extends Random {

    @Override
    public int nextInt(int n) {
      int r = usePrevData ? readNextSavedInt() : super.nextInt(n);
      if (capture) writeSavedInt(r);
      if (r >= n) {
        throw new RuntimeException("Internal error - using prev value, max is " + n + ", val read was " + r);
      }
      return r;
    }

    @Override
    public int nextInt() {
      int r = usePrevData ? readNextSavedInt() : super.nextInt();
      if (capture) writeSavedInt(r);
      return r;
    }

    @Override
    public long nextLong() {
      int r = usePrevData ? readNextSavedInt() : super.nextInt();
      if (capture) writeSavedInt(r);      
      return r;
    }

    @Override
    public boolean nextBoolean() {
      int r = usePrevData ? readNextSavedInt() : super.nextInt(2);
      if (capture) writeSavedInt(r);      
      return r == 0;
    }

    @Override
    public float nextFloat() {
      int r = usePrevData ? readNextSavedInt() : super.nextInt(0x7ffff);
      if (capture) writeSavedInt(r);
      return Float.intBitsToFloat(r);
    }

    @Override
    public double nextDouble() {
      int r = usePrevData ? readNextSavedInt() : super.nextInt(0x7ffff);
      if (capture) writeSavedInt(r);
      return CASImpl.long2double((long) r);
    }

    @Override
    protected int next(int bits) {
      if (usePrevData) {
        throw new RuntimeException("invalid - never called");
      }
      return super.next(bits);
    }
    
  }
  
  // Create a random number generator to use for random seeds
  // one per thread (one per class instance)
  private final Random randomseed = new Random();
  
  { long seed = randomseed.nextLong();
    // long seed = 1_449_257_605_347_913_923L;   // to set a specific seed
    randomseed.setSeed(seed);  
    System.out.format("SerDesTstCommon Initial RandomSeed: %,d%n", seed);
  }
  
  /**
   * The random number generator - used throughout.
   * Can return a pre-computed stream of random numbers if usePrevData is true
   * Can save the stream of random numbers if capture is true
   * 
   */
  protected final Random           random      = new MyRandom();
  { random.setSeed(randomseed.nextLong()); }

  /** set to true to change FS creation to keep references to all created FS
   * needed for testing backward compatibility with delta cas
   * Done by adding to indexes FSs which otherwise would be lost
   */
  protected boolean isKeep = false;
  
  // saving random numbers 
  protected BufferedReader         savedIntsStream;
  protected OutputStreamWriter     savedIntsOutStream;
  private int                    savedIntSeq = 0;  // useful in debug mode
  protected boolean capture = false; // capture the serialized output
  protected boolean                usePrevData = false;

  protected boolean                doPlain     = false;

  protected void writeout(ByteArrayOutputStream baos, String fname) throws IOException {
    if (null == fname) {
      return;
    }
    BufferedOutputStream fos = setupFileOut(fname);
    fos.write(baos.toByteArray());
    fos.close();
  }

  // read and create byte array to use as 
  // test data for checking previously serialized
  protected byte[] readIn(String fname) throws IOException {
    File f = new File("src/test/resources/" + getTestRootName() + "/" + fname + ".binary");
    int len = (int) f.length();
    byte[] buffer = new byte[len];
    BufferedInputStream inStream = 
      new BufferedInputStream(
          new FileInputStream(f));
    int br = inStream.read(buffer);
    if (br != len) {
      throw new RuntimeException("Corrupted test saved ints stream");
    }
    inStream.close();
    return buffer;
  }

  protected abstract String getTestRootName();
  
  private BufferedOutputStream setupFileOut(String fname) throws IOException {
    if (null == fname) {
      return null;
    }
    File dir = new File("src/test/resources/" + getTestRootName() + "/");
    if (!dir.exists()) {
      dir.mkdirs();
    }
    
    System.out.println("debug out file name is " + fname);  
    

    return
      new BufferedOutputStream(
        new FileOutputStream(
          new File("src/test/resources/" + getTestRootName() + "/" + fname + ".binary")));

  }

  protected void initWriteSavedInts() {
    try {
      savedIntsOutStream = new OutputStreamWriter(setupFileOut("SavedInts"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected void initReadSavedInts() {
    try {
      savedIntsStream = new BufferedReader(new FileReader("src/test/resources/" + getTestRootName() + "/SavedInts.binary"));
      savedIntSeq = 0;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void writeSavedInt(int i) {
    try {
      savedIntsOutStream.write(Integer.toString(i) + '\n');
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

   
  private int readNextSavedInt() {
    try {
      String s = savedIntsStream.readLine();
//      System.out.println("debug savedInt " + savedIntSeq++ + ", value " + s);
      return Integer.parseInt(s);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}