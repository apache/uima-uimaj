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

package org.apache.uima.tutorial;

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FSGenerator;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.tcas.Annotation_Type;

/**
 * Updated by JCasGen Mon Nov 29 15:02:38 EST 2004
 * 
 * @generated
 */
public class Meeting_Type extends Annotation_Type {
  /** @generated */
  protected FSGenerator getFSGenerator() {
    return fsGenerator;
  }

  /** @generated */
  private final FSGenerator fsGenerator = new FSGenerator() {
    public FeatureStructure createFS(int addr, CASImpl cas) {
      if (instanceOf_Type.useExistingInstance) {
        // Return eq fs instance if already created
        FeatureStructure fs = instanceOf_Type.jcas.getJfsFromCaddr(addr);
        if (null == fs) {
          fs = new Meeting(addr, instanceOf_Type);
          instanceOf_Type.jcas.putJfsFromCaddr(addr, fs);
          return fs;
        }
        return fs;
      } else
        return new Meeting(addr, instanceOf_Type);
    }
  };

  /** @generated */
  public final static int typeIndexID = Meeting.typeIndexID;

  /**
   * @generated
   * @modifiable
   */
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("org.apache.uima.tutorial.Meeting");

  /** @generated */
  final Feature casFeat_room;

  /** @generated */
  final int casFeatCode_room;

  /** @generated */
  public int getRoom(int addr) {
    if (featOkTst && casFeat_room == null)
      this.jcas.throwFeatMissing("room", "org.apache.uima.tutorial.Meeting");
    return ll_cas.ll_getRefValue(addr, casFeatCode_room);
  }

  /** @generated */
  public void setRoom(int addr, int v) {
    if (featOkTst && casFeat_room == null)
      this.jcas.throwFeatMissing("room", "org.apache.uima.tutorial.Meeting");
    ll_cas.ll_setRefValue(addr, casFeatCode_room, v);
  }

  /** @generated */
  final Feature casFeat_date;

  /** @generated */
  final int casFeatCode_date;

  /** @generated */
  public int getDate(int addr) {
    if (featOkTst && casFeat_date == null)
      this.jcas.throwFeatMissing("date", "org.apache.uima.tutorial.Meeting");
    return ll_cas.ll_getRefValue(addr, casFeatCode_date);
  }

  /** @generated */
  public void setDate(int addr, int v) {
    if (featOkTst && casFeat_date == null)
      this.jcas.throwFeatMissing("date", "org.apache.uima.tutorial.Meeting");
    ll_cas.ll_setRefValue(addr, casFeatCode_date, v);
  }

  /** @generated */
  final Feature casFeat_startTime;

  /** @generated */
  final int casFeatCode_startTime;

  /** @generated */
  public int getStartTime(int addr) {
    if (featOkTst && casFeat_startTime == null)
      this.jcas.throwFeatMissing("startTime", "org.apache.uima.tutorial.Meeting");
    return ll_cas.ll_getRefValue(addr, casFeatCode_startTime);
  }

  /** @generated */
  public void setStartTime(int addr, int v) {
    if (featOkTst && casFeat_startTime == null)
      this.jcas.throwFeatMissing("startTime", "org.apache.uima.tutorial.Meeting");
    ll_cas.ll_setRefValue(addr, casFeatCode_startTime, v);
  }

  /** @generated */
  final Feature casFeat_endTime;

  /** @generated */
  final int casFeatCode_endTime;

  /** @generated */
  public int getEndTime(int addr) {
    if (featOkTst && casFeat_endTime == null)
      this.jcas.throwFeatMissing("endTime", "org.apache.uima.tutorial.Meeting");
    return ll_cas.ll_getRefValue(addr, casFeatCode_endTime);
  }

  /** @generated */
  public void setEndTime(int addr, int v) {
    if (featOkTst && casFeat_endTime == null)
      this.jcas.throwFeatMissing("endTime", "org.apache.uima.tutorial.Meeting");
    ll_cas.ll_setRefValue(addr, casFeatCode_endTime, v);
  }

  /**
   * initialize variables to correspond with Cas Type and Features
   * 
   * @generated
   */
  public Meeting_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl) this.casType, getFSGenerator());

    casFeat_room = jcas.getRequiredFeatureDE(casType, "room",
            "org.apache.uima.tutorial.RoomNumber", featOkTst);
    casFeatCode_room = (null == casFeat_room) ? JCas.INVALID_FEATURE_CODE
            : ((FeatureImpl) casFeat_room).getCode();

    casFeat_date = jcas.getRequiredFeatureDE(casType, "date", "org.apache.uima.tutorial.DateAnnot",
            featOkTst);
    casFeatCode_date = (null == casFeat_date) ? JCas.INVALID_FEATURE_CODE
            : ((FeatureImpl) casFeat_date).getCode();

    casFeat_startTime = jcas.getRequiredFeatureDE(casType, "startTime",
            "org.apache.uima.tutorial.TimeAnnot", featOkTst);
    casFeatCode_startTime = (null == casFeat_startTime) ? JCas.INVALID_FEATURE_CODE
            : ((FeatureImpl) casFeat_startTime).getCode();

    casFeat_endTime = jcas.getRequiredFeatureDE(casType, "endTime",
            "org.apache.uima.tutorial.TimeAnnot", featOkTst);
    casFeatCode_endTime = (null == casFeat_endTime) ? JCas.INVALID_FEATURE_CODE
            : ((FeatureImpl) casFeat_endTime).getCode();

  }
}
