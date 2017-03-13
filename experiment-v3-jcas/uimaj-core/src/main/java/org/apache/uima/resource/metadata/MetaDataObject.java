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

package org.apache.uima.resource.metadata;

import java.net.URL;
import java.util.List;

import org.apache.uima.UIMA_IllegalArgumentException;
import org.apache.uima.UIMA_UnsupportedOperationException;
import org.apache.uima.util.NameClassPair;
import org.apache.uima.util.XMLizable;

/**
 * An object used to represent metadata of a <code>Resource</code>.
 * <p>
 * A list of all attributes on a <code>MetaDataObject</code> can be obtained by calling its
 * {@link #listAttributes()} method. The values of attributes can be get and set by using the
 * {@link #getAttributeValue(String)} and {@link #setAttributeValue(String,Object)} methods. The
 * value of an attribute may be any Object, including another <code>MetaDataObject</code>.
 * <p>
 * <code>MetaDataObject</code>s are not required to allow modification of their attributes'
 * values. An application should check the {@link #isModifiable()} method to determine if attribute
 * values can be modified. Calling {@link #setAttributeValue(String,Object)} on an unmodifiable
 * object will result in a {@link org.apache.uima.UIMA_UnsupportedOperationException}.
 * 
 * 
 * 
 */
public interface MetaDataObject extends Cloneable, java.io.Serializable, XMLizable {

  /**
   * Retrieves all attributes on this <code>MetaDataObject</code>.
   * 
   * @return a List containing {@link org.apache.uima.util.NameClassPair} objects, each of which
   *         contains the name of a parameter and the Class of its value. For primitive types, the
   *         wrapper classes will be returned (e.g. <code>java.lang.Integer</code> instead of
   *         int).
   * @deprecated - use getAttributes() instead, don't override it, use getAdditionalAttributes to specify additional ones
   */
  @Deprecated
  public List<NameClassPair> listAttributes();

  /**
   * Retrieves the value of an attribute of this <code>MetaDataObject</code>.
   * 
   * @param aName
   *          the name of the parameter to get
   * 
   * @return the value of the parameter named <code>aName</code>. Returns <code>null</code> if
   *         there is no attribute with that name.
   */
  public Object getAttributeValue(String aName);

  /**
   * Returns whether this <code>MetaDataObject</code> allows the values of its attributes to be
   * modified.
   * 
   * @return true if and only if this object's attributes may be modified.
   */
  public boolean isModifiable();

  /**
   * Sets the value of an attribute of this <code>MetaDataObject</code>. Applications should
   * first check the {@link #isModifiable()} method; calling
   * {@link #setAttributeValue(String, Object)} on an unmodifiable <code>MetaDataObject</code>
   * will result in an exception.
   * 
   * @param aName
   *          the name of the parameter to set
   * @param aValue
   *          the value to assign to the parameter
   * 
   * @throws UIMA_UnsupportedOperationException
   *           if this <code>MetaDataObject</code> is not modifiable.
   * @throws UIMA_IllegalArgumentException
   *           if the given value is not appropriate for the given attribute.
   */
  public void setAttributeValue(String aName, Object aValue);

  /**
   * Creates a clone of this <code>MetaDataObject</code>. This performs a "deep" copy by cloning
   * all attribute values that are also MetaDataObjects.
   * 
   * @return a clone of this <code>MetaDataObject</code>
   */
  public Object clone();

  /**
   * Determines if this object is equal to another. Two MetaDataObjects are equal if they share the
   * same attributes and the same values for those attributes.
   * 
   * @param aObj
   *          an object with which to compare this object
   * 
   * @return true if and only if this object equals <code>aObj</code>
   */
  public boolean equals(Object aObj);

  /**
   * Gets the URL from which this object was parsed. When this object is parsed from an XML file,
   * this is set by the parser to the URL of the source file XML file. If the object has been
   * created by some other method, the source URL will not be known, and this method will return
   * null.
   * <p>
   * This setting is used to resolve imports and is also included in exception messages to indicate
   * the source of the problem.
   * 
   * @return the source URL from which this object was parsed
   */
  public URL getSourceUrl();

  /**
   * If the sourceURL of this object is non-null, returns its string representation. If it is null,
   * returns "&lt;unknown&gt;". Useful for error messages.
   * 
   * @return the source URL as a string, or "&lt;unknown&gt;"
   */
  public String getSourceUrlString();

  /**
   * Sets the URL from which this object was parsed. Typically only the XML parser sets this. This
   * recursively sets the source URL of all descendants of this object.
   * 
   * @param aUrl
   *          the location of the XML file from which this object was parsed
   */
  public void setSourceUrl(URL aUrl);
}
