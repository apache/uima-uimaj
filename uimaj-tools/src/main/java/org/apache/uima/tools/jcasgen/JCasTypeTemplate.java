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

package org.apache.uima.tools.jcasgen;

import java.util.Iterator;

import org.apache.uima.resource.metadata.FeatureDescription;
import org.apache.uima.resource.metadata.TypeDescription;

public class JCasTypeTemplate implements Jg.IJCasTypeTemplate {

  @Override
  public String generate(Object argument) {
    StringBuilder stringBuilder = new StringBuilder();

    stringBuilder.append("\n\n");

    /*
     * *********************************************************************************************
     * **** File generated from uimaj-tools project:
     * /src/main/javajet/jcasgen/templates/JCasType.javajet Edit that file, and rerun the jet
     * expander, found in the uimaj-jet-expander project in svn Install that project into Eclipse
     * using File -- import -- Projects from Folder or Archive then select the uimaj-jet-expander
     * folder
     ***************************************************************************************************/

    Object[] args = (Object[]) argument;
    Jg jg = (Jg) args[0];
    TypeDescription td = (TypeDescription) args[1];
    jg.packageName = jg.getJavaPkg(td);
    stringBuilder.append("   \n/* Apache UIMA v3 - First created by JCasGen ");
    stringBuilder.append(jg.getDate());
    stringBuilder.append(" */\n\n");
    if (0 != jg.packageName.length()) {
      stringBuilder.append("package ");
      stringBuilder.append(jg.packageName);
      stringBuilder.append(";\n \n");
    } else
      jg.error.newError(IError.WARN, jg.getString("pkgMissing", new Object[] { td.getName() }),
              null);
    stringBuilder.append("\n");
    FeatureDescription[] fds = td.getFeatures();
    if (fds.length > 0) {
      stringBuilder
              .append("import java.lang.invoke.CallSite;\nimport java.lang.invoke.MethodHandle;\n");
    }
    stringBuilder.append(
            "\nimport org.apache.uima.cas.impl.CASImpl;\nimport org.apache.uima.cas.impl.TypeImpl;\n");
    if (fds.length > 0) {
      stringBuilder.append("import org.apache.uima.cas.impl.TypeSystemImpl;\n");
    }
    stringBuilder.append(
            "import org.apache.uima.jcas.JCas; \nimport org.apache.uima.jcas.JCasRegistry;\n\n\n");
    for (Iterator<String> i = jg.collectImports(td, false).iterator(); i.hasNext();) {
      stringBuilder.append("import ");
      stringBuilder.append(i.next());
      stringBuilder.append(";\n");
    }
    stringBuilder.append("\n\n");
    String typeName = jg.getJavaName(td);

    stringBuilder.append("/** ");
    stringBuilder.append(jg.nullBlank(td.getDescription()));
    stringBuilder.append("\n * Updated by JCasGen ");
    stringBuilder.append(jg.getDate());
    stringBuilder.append("\n * XML source: ");
    stringBuilder.append(jg.xmlSourceFileName);
    stringBuilder.append("\n * @generated */\npublic class ");
    stringBuilder.append(typeName);
    stringBuilder.append(" extends ");
    stringBuilder.append(jg.getJavaName(td.getSupertypeName()));
    stringBuilder.append(
            " {\n \n  /** @generated\n   * @ordered \n   */\n  @SuppressWarnings (\"hiding\")\n  public final static String _TypeName = \"");
    stringBuilder.append(jg.getJavaNameWithPkg(td.getName()));
    stringBuilder.append(
            "\";\n  \n  /** @generated\n   * @ordered \n   */\n  @SuppressWarnings (\"hiding\")\n  public final static int typeIndexID = JCasRegistry.register(");
    stringBuilder.append(typeName);
    stringBuilder.append(
            ".class);\n  /** @generated\n   * @ordered \n   */\n  @SuppressWarnings (\"hiding\")\n  public final static int type = typeIndexID;\n  /** @generated\n   * @return index of the type  \n   */\n  @Override\n  public              int getTypeIndexID() {return typeIndexID;}\n \n ");

    StringBuilder localData = new StringBuilder();
    StringBuilder featRegistry = new StringBuilder();

    featRegistry.append("  /* Feature Adjusted Offsets */\n");

    for (FeatureDescription fd : fds) {

      String featName = fd.getName();
      String featUName = jg.uc1(featName); // upper case first letter
      if (Jg.reservedFeatureNames.contains(featUName)) {
        jg.error.newError(IError.ERROR,
                jg.getString("reservedNameUsed", new Object[] { featName, td.getName() }), null);
      }

      localData.append("  public final static String _FeatName_").append(featName).append(" = \"")
              .append(featName).append("\";\n");

      featRegistry.append("  private final static CallSite _FC_").append(featName)
              .append(" = TypeSystemImpl.createCallSite(").append(typeName).append(".class, ")
              .append("\"").append(featName).append("\");\n");
      featRegistry.append("  private final static MethodHandle _FH_").append(featName)
              .append(" = _FC_").append(featName).append(".dynamicInvoker();\n");

    } /* of Features iteration */
    stringBuilder.append(
            "\n  /* *******************\n   *   Feature Offsets *\n   * *******************/ \n   \n");
    stringBuilder.append(localData);
    stringBuilder.append("\n\n");
    stringBuilder.append(featRegistry);
    stringBuilder.append(
            "\n   \n  /** Never called.  Disable default constructor\n   * @generated */\n  @Deprecated\n  @SuppressWarnings (\"deprecation\")\n  protected ");
    stringBuilder.append(typeName);
    stringBuilder.append(
            "() {/* intentionally empty block */}\n    \n  /** Internal - constructor used by generator \n   * @generated\n   * @param casImpl the CAS this Feature Structure belongs to\n   * @param type the type of this Feature Structure \n   */\n  public ");
    stringBuilder.append(typeName);
    stringBuilder.append(
            "(TypeImpl type, CASImpl casImpl) {\n    super(type, casImpl);\n    readObject();\n  }\n  \n  /** @generated\n   * @param jcas JCas to which this Feature Structure belongs \n   */\n  public ");
    stringBuilder.append(typeName);
    stringBuilder.append("(JCas jcas) {\n    super(jcas);\n    readObject();   \n  } \n\n");
    if (jg.isSubTypeOfAnnotation(td)) {
      stringBuilder.append(
              "\n  /** @generated\n   * @param jcas JCas to which this Feature Structure belongs\n   * @param begin offset to the begin spot in the SofA\n   * @param end offset to the end spot in the SofA \n  */  \n  public ");
      stringBuilder.append(typeName);
      stringBuilder.append(
              "(JCas jcas, int begin, int end) {\n    super(jcas);\n    setBegin(begin);\n    setEnd(end);\n    readObject();\n  }   \n");
    }
    stringBuilder.append(
            "\n  /** \n   * <!-- begin-user-doc -->\n   * Write your own initialization here\n   * <!-- end-user-doc -->\n   *\n   * @generated modifiable \n   */\n  private void readObject() {/*default - does nothing empty block */}\n     \n");
    for (FeatureDescription fd : td.getFeatures()) {

      String featName = fd.getName();
      String featUName = jg.uc1(featName); // upper case first letter
      if (Jg.reservedFeatureNames.contains(featUName))
        jg.error.newError(IError.ERROR,
                jg.getString("reservedNameUsed", new Object[] { featName, td.getName() }), null);

      String featDesc = jg.nullBlank(fd.getDescription());
      String featDescCmt = featDesc;

      String rangeType = jg.getJavaRangeType2(fd);
      String elemType = jg.getJavaRangeArrayElementType2(fd);
      boolean isRangeTypeGeneric = jg.isRangeTypeGeneric(fd);
      boolean isElemTypeGeneric = jg.isElementTypeGeneric(fd);

      stringBuilder.append(" \n    \n  //*--------------*\n  //* Feature: ");
      stringBuilder.append(featName);
      stringBuilder.append("\n\n  /** getter for ");
      stringBuilder.append(featName);
      stringBuilder.append(" - gets ");
      stringBuilder.append(featDescCmt);
      stringBuilder.append("\n   * @generated\n   * @return value of the feature \n   */\n");
      if (isRangeTypeGeneric) {
        stringBuilder.append("  @SuppressWarnings(\"unchecked\")\n");
      }
      stringBuilder.append("  public ");
      stringBuilder.append(rangeType);
      stringBuilder.append(" get");
      stringBuilder.append(featUName);
      stringBuilder.append("() { \n    return ");
      stringBuilder.append(jg.getFeatureValue(fd, td));
      stringBuilder.append(";\n  }\n    \n  /** setter for ");
      stringBuilder.append(featName);
      stringBuilder.append(" - sets ");
      stringBuilder.append(featDescCmt);
      stringBuilder.append(
              " \n   * @generated\n   * @param v value to set into the feature \n   */\n  public void set");
      stringBuilder.append(featUName);
      stringBuilder.append("(");
      stringBuilder.append(rangeType);
      stringBuilder.append(" v) {\n    ");
      stringBuilder.append(jg.setFeatureValue(fd, td));
      stringBuilder.append(";\n  }    \n    \n  ");
      if (jg.hasArrayRange(fd)) {
        stringBuilder.append("  \n  /** indexed getter for ");
        stringBuilder.append(featName);
        stringBuilder.append(" - gets an indexed value - ");
        stringBuilder.append(featDescCmt);
        stringBuilder.append(
                "\n   * @generated\n   * @param i index in the array to get\n   * @return value of the element at index i \n   */\n");
        if (isRangeTypeGeneric || isElemTypeGeneric) {
          stringBuilder.append("  @SuppressWarnings(\"unchecked\")\n");
        }
        stringBuilder.append("  public ");
        stringBuilder.append(elemType);
        stringBuilder.append(" get");
        stringBuilder.append(featUName);
        stringBuilder.append("(int i) {\n     return ");
        stringBuilder.append(jg.getArrayFeatureValue(fd, td));
        stringBuilder.append(";\n  } \n\n  /** indexed setter for ");
        stringBuilder.append(featName);
        stringBuilder.append(" - sets an indexed value - ");
        stringBuilder.append(featDescCmt);
        stringBuilder.append(
                "\n   * @generated\n   * @param i index in the array to set\n   * @param v value to set into the array \n   */\n");
        if (isRangeTypeGeneric || isElemTypeGeneric) {
          stringBuilder.append("  @SuppressWarnings(\"unchecked\")\n  ");
        }
        stringBuilder.append("  public void set");
        stringBuilder.append(featUName);
        stringBuilder.append("(int i, ");
        stringBuilder.append(elemType);
        stringBuilder.append(" v) {\n    ");
        stringBuilder.append(jg.setArrayFeatureValue(fd, td));
        stringBuilder.append(";\n  }  \n  ");
      } /* of hasArray */
      stringBuilder.append("");
    } /* of Features iteration */
    stringBuilder.append("");
    if (td.getName().equals("uima.cas.Annotation")) {
      stringBuilder.append("  ");
      stringBuilder.append(
              "  /** Constructor with begin and end passed as arguments \n    * @generated\n    * @param jcas JCas this Annotation is in\n    * @param begin the begin offset\n    * @param end the end offset\n    */\n  public Annotation(JCas jcas, int begin, int end) { \n	  this(jcas); // forward to constructor \n	  this.setBegin(begin); \n	  this.setEnd(end); \n  } \n  \n  /** @see org.apache.uima.cas.text.AnnotationFS#getCoveredText() \n    * @generated\n    * @return the covered Text \n    */ \n  public String getCoveredText() { \n    final CAS casView = this.getView();\n    final String text = casView.getDocumentText();\n    if (text == null) {\n      return null;\n    }\n    return text.substring(getBegin(), getEnd());\n  } \n  \n  /** @deprecated \n    * @generated\n    * @return the begin offset \n    */\n  public int getStart() {return getBegin();}\n");
      stringBuilder.append("");
    } /* of Annotation if-statement */
    stringBuilder.append("}\n\n    ");
    return stringBuilder.toString();
  }
}