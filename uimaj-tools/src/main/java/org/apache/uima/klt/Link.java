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

package org.apache.uima.klt;

import org.apache.uima.cas.CASException;
import org.apache.uima.jcas.cas.EmptyFSList;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.NonEmptyFSList;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.cas.TOP_Type;
import org.apache.uima.jcas.impl.JCas;

/**
 * A directional link between two instances Updated by JCasGen Thu Apr 21 11:20:08 EDT 2005 XML
 * source: descriptors/types/hutt.xml
 * 
 * @generated
 */
public class Link extends TOP {
  protected static java.util.LinkedList getLinks(FSList aLinks, Class aClass) {
    java.util.LinkedList retval = new java.util.LinkedList();
    if (aLinks != null)
      while (aLinks instanceof NonEmptyFSList) {
        NonEmptyFSList nLinks = (NonEmptyFSList) aLinks;
        Link link = (Link) nLinks.getHead();
        if (aClass.isInstance(link))
          retval.add(link);
        aLinks = nLinks.getTail();
      }
    return retval;
  }

  public static java.util.LinkedList getFromValues(FSList aLinks, Class aClass) {
    java.util.LinkedList retval = new java.util.LinkedList();
    if (aLinks != null)
      while (aLinks instanceof NonEmptyFSList) {
        NonEmptyFSList nLinks = (NonEmptyFSList) aLinks;
        Link link = (Link) nLinks.getHead();
        if (aClass.isInstance(link))
          retval.add(link.getFrom());
        aLinks = nLinks.getTail();
      }
    return retval;
  }

  public static java.util.LinkedList getToValues(FSList aLinks, Class aClass) {
    java.util.LinkedList retval = new java.util.LinkedList();
    if (aLinks != null)
      while (aLinks instanceof NonEmptyFSList) {
        NonEmptyFSList nLinks = (NonEmptyFSList) aLinks;
        Link link = (Link) nLinks.getHead();
        if (aClass.isInstance(link))
          retval.add(link.getTo());
        aLinks = nLinks.getTail();
      }
    return retval;
  }

  public static FSList getLinks(TOP obj) {
    if (obj instanceof EntityAnnotation)
      return ((EntityAnnotation) obj).getLinks();
    else if (obj instanceof RelationAnnotation)
      return ((RelationAnnotation) obj).getLinks();
    else if (obj instanceof Referent)
      return ((Referent) obj).getLinks();
    else
      return null;
  }

  public static void setLinks(TOP obj, FSList links) {
    if (obj instanceof EntityAnnotation)
      ((EntityAnnotation) obj).setLinks(links);
    else if (obj instanceof RelationAnnotation)
      ((RelationAnnotation) obj).setLinks(links);
    else if (obj instanceof Referent)
      ((Referent) obj).setLinks(links);
  }

  public static Link addLink(TOP aFrom, TOP aTo, Class linkClass, String aComponentId) {
    Link newLink = null;
    try {
      JCas jcas = aFrom.getCAS().getJCas();
      java.lang.reflect.Constructor linkConstructor = linkClass
              .getConstructor(new Class[] { JCas.class });
      newLink = (Link) linkConstructor.newInstance(new Object[] { jcas });
    } catch (Exception e) {
    }
    if (newLink != null) {
      newLink.setFrom(aFrom);
      newLink.setTo(aTo);
      newLink.setComponentId(aComponentId);
      addLink(aFrom, newLink);
      addLink(aTo, newLink);
    }
    return newLink;
  }

  public static void addLink(TOP obj, Link link) {
    JCas jcas = null;
    NonEmptyFSList newLinks = null;
    try {
      jcas = link.getCAS().getJCas();
      newLinks = new NonEmptyFSList(jcas);
    } catch (CASException e) {
      return;
    }
    FSList oldLinks = getLinks(obj);
    newLinks.setHead(link);
    if ((oldLinks == null) || (oldLinks instanceof EmptyFSList)) {
      setLinks(obj, newLinks);
      newLinks.setTail(new EmptyFSList(jcas));
    } else {
      NonEmptyFSList currentLinks = (NonEmptyFSList) oldLinks;
      while (!(currentLinks.getTail() instanceof EmptyFSList))
        currentLinks = (NonEmptyFSList) currentLinks.getTail();
      newLinks.setTail(currentLinks.getTail());
      currentLinks.setTail(newLinks);
    }
  }

  /**
   * Destructive mechanism for removing a link from an FSList of links.
   * 
   * @param obj
   *          The object containing the link
   * @param link
   *          The link
   */
  public static void removeLink(TOP obj, Link link) {
    FSList list = Link.getLinks(obj);
    if (list instanceof NonEmptyFSList) {
      NonEmptyFSList pointer = (NonEmptyFSList) list;
      if (link.equals(pointer.getHead())) {
        Link.setLinks(obj, pointer.getTail());
        return;
      }
      while (pointer.getTail() instanceof NonEmptyFSList) {
        NonEmptyFSList nextPointer = (NonEmptyFSList) pointer.getTail();
        if (nextPointer.getHead().equals(link)) {
          pointer.setTail(nextPointer.getTail());
          return;
        }
        pointer = nextPointer;
      }
    }
  }

  public String toString() {
    return "(LINK: " + getFrom() + ", " + getTo() + ")";
  }

  /**
   * @generated
   * @ordered
   */
  public final static int typeIndexID = JCas.getNextIndex();

  /**
   * @generated
   * @ordered
   */
  public final static int type = typeIndexID;

  /** @generated */
  public int getTypeIndexID() {
    return typeIndexID;
  }

  /**
   * Never called. Disable default constructor
   * 
   * @generated
   */
  protected Link() {
  }

  /**
   * Internal - constructor used by generator
   * 
   * @generated
   */
  public Link(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }

  /** @generated */
  public Link(JCas jcas) {
    super(jcas);
    readObject();
  }

  /**
   * <!-- begin-user-doc --> Write your own initialization here <!-- end-user-doc -->
   * 
   * @generated modifiable
   */
  private void readObject() {
  }

  // *--------------*
  // * Feature: from

  /**
   * getter for from - gets
   * 
   * @generated
   */
  public TOP getFrom() {
    if (Link_Type.featOkTst && ((Link_Type) jcasType).casFeat_from == null)
      JCas.throwFeatMissing("from", "org.apache.uima.klt.Link");
    return (TOP) (jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr,
            ((Link_Type) jcasType).casFeatCode_from)));
  }

  /**
   * setter for from - sets
   * 
   * @generated
   */
  public void setFrom(TOP v) {
    if (Link_Type.featOkTst && ((Link_Type) jcasType).casFeat_from == null)
      JCas.throwFeatMissing("from", "org.apache.uima.klt.Link");
    jcasType.ll_cas.ll_setRefValue(addr, ((Link_Type) jcasType).casFeatCode_from, jcasType.ll_cas
            .ll_getFSRef(v));
  }

  // *--------------*
  // * Feature: to

  /**
   * getter for to - gets
   * 
   * @generated
   */
  public TOP getTo() {
    if (Link_Type.featOkTst && ((Link_Type) jcasType).casFeat_to == null)
      JCas.throwFeatMissing("to", "org.apache.uima.klt.Link");
    return (TOP) (jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr,
            ((Link_Type) jcasType).casFeatCode_to)));
  }

  /**
   * setter for to - sets
   * 
   * @generated
   */
  public void setTo(TOP v) {
    if (Link_Type.featOkTst && ((Link_Type) jcasType).casFeat_to == null)
      JCas.throwFeatMissing("to", "org.apache.uima.klt.Link");
    jcasType.ll_cas.ll_setRefValue(addr, ((Link_Type) jcasType).casFeatCode_to, jcasType.ll_cas
            .ll_getFSRef(v));
  }

  // *--------------*
  // * Feature: componentId

  /**
   * getter for componentId - gets
   * 
   * @generated
   */
  public String getComponentId() {
    if (Link_Type.featOkTst && ((Link_Type) jcasType).casFeat_componentId == null)
      JCas.throwFeatMissing("componentId", "org.apache.uima.klt.Link");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Link_Type) jcasType).casFeatCode_componentId);
  }

  /**
   * setter for componentId - sets The unique ID of the component that created this instance
   * 
   * @generated
   */
  public void setComponentId(String v) {
    if (Link_Type.featOkTst && ((Link_Type) jcasType).casFeat_componentId == null)
      JCas.throwFeatMissing("componentId", "org.apache.uima.klt.Link");
    jcasType.ll_cas.ll_setStringValue(addr, ((Link_Type) jcasType).casFeatCode_componentId, v);
  }

}
