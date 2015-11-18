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
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.FeatureDescription;

public class JCas_TypeTemplate implements Jg.IJCasTypeTemplate {

  public String generate(Object argument) {
    StringBuffer stringBuffer = new StringBuffer();

    stringBuffer.append("\n");
  
    Object [] args = (Object [])argument;
    Jg jg = (Jg)args[0];
    TypeDescription td = (TypeDescription)args[1]; 
   jg.packageName = jg.getJavaPkg(td);
    stringBuffer.append("/* First created by JCasGen ");
    stringBuffer.append(jg.getDate());
    stringBuffer.append(" */\n");
   if (0 != jg.packageName.length()) {
    stringBuffer.append("package ");
    stringBuffer.append(jg.packageName);
    stringBuffer.append(";\n");
   } 
    stringBuffer.append("\nimport org.apache.uima.jcas.JCas;\nimport org.apache.uima.jcas.JCasRegistry;\nimport org.apache.uima.cas.impl.TypeImpl;\nimport org.apache.uima.cas.Type;\n");
   if (td.getFeatures().length > 0) {
    stringBuffer.append("import org.apache.uima.cas.impl.FeatureImpl;\nimport org.apache.uima.cas.Feature;\n");
   } 
    stringBuffer.append("");
   for(Iterator i=jg.collectImports(td, true).iterator(); i.hasNext();) { 
 String imp = (String)i.next();
  if (!imp.equals(jg.getJavaNameWithPkg(td.getName()+"_Type"))) {
    stringBuffer.append("import ");
    stringBuffer.append(imp);
    stringBuffer.append(";\n");
   }} 
    stringBuffer.append("\n");
   String typeName = jg.getJavaName(td);
   String typeName_Type = typeName + "_Type"; 
    stringBuffer.append("/** ");
    stringBuffer.append(jg.nullBlank(td.getDescription()));
    stringBuffer.append("\n * Updated by JCasGen ");
    stringBuffer.append(jg.getDate());
    stringBuffer.append("\n * @generated */\npublic class ");
    stringBuffer.append(typeName_Type);
    stringBuffer.append(" extends ");
    stringBuffer.append(jg.getJavaName(td.getSupertypeName()) + "_Type");
    stringBuffer.append(" {\n  /** @generated */\n  @SuppressWarnings (\"hiding\")\n  public final static int typeIndexID = ");
    stringBuffer.append(typeName);
    stringBuffer.append(".typeIndexID;\n  /** @generated \n     @modifiable */\n  @SuppressWarnings (\"hiding\")\n  public final static boolean featOkTst = JCasRegistry.getFeatOkTst(\"");
    stringBuffer.append(td.getName());
    stringBuffer.append("\");\n");
   FeatureDescription [] fds = td.getFeatures();
   for (int i = 0; i < fds.length; i++) { 
     FeatureDescription fd = fds[i];

     String featName = fd.getName();
     String featUName = jg.uc1(featName);  // upper case first letter

     String rangeType = jg.getJavaRangeType(fd);
     String getSetNamePart = jg.sc(rangeType);
     String returnType = getSetNamePart.equals("Ref") ? "int" : rangeType;
     String getSetArrayNamePart = jg.getGetSetArrayNamePart(fd);
     
     String elemType = jg.getJavaRangeArrayElementType(fd);    
     if (jg.sc(elemType).equals("Ref")) 
       elemType = "int";   
     String casFeatCode = "casFeatCode_" + featName;

    stringBuffer.append(" \n  /** @generated */\n  final Feature casFeat_");
    stringBuffer.append(featName);
    stringBuffer.append(";\n  /** @generated */\n  final int     ");
    stringBuffer.append(casFeatCode);
    stringBuffer.append(";\n  /** @generated\n   * @param addr low level Feature Structure reference\n   * @return the feature value \n   */ \n  public ");
    stringBuffer.append(returnType);
    stringBuffer.append(" get");
    stringBuffer.append(featUName);
    stringBuffer.append("(int addr) {\n    ");
    stringBuffer.append("");
  
/* checks to insure that cas has the feature */

    stringBuffer.append("    if (featOkTst && casFeat_");
    stringBuffer.append(featName);
    stringBuffer.append(" == null)\n      jcas.throwFeatMissing(\"");
    stringBuffer.append(featName);
    stringBuffer.append("\", \"");
    stringBuffer.append(td.getName());
    stringBuffer.append("\");\n");
    stringBuffer.append("    return ll_cas.ll_get");
    stringBuffer.append(getSetNamePart);
    stringBuffer.append("Value(addr, ");
    stringBuffer.append(casFeatCode);
    stringBuffer.append(");\n  }\n  /** @generated\n   * @param addr low level Feature Structure reference\n   * @param v value to set \n   */    \n  public void set");
    stringBuffer.append(featUName);
    stringBuffer.append("(int addr, ");
    stringBuffer.append(returnType);
    stringBuffer.append(" v) {\n    ");
    stringBuffer.append("");
  
/* checks to insure that cas has the feature */

    stringBuffer.append("    if (featOkTst && casFeat_");
    stringBuffer.append(featName);
    stringBuffer.append(" == null)\n      jcas.throwFeatMissing(\"");
    stringBuffer.append(featName);
    stringBuffer.append("\", \"");
    stringBuffer.append(td.getName());
    stringBuffer.append("\");\n");
    stringBuffer.append("    ll_cas.ll_set");
    stringBuffer.append(getSetNamePart);
    stringBuffer.append("Value(addr, ");
    stringBuffer.append(casFeatCode);
    stringBuffer.append(", v);}\n    \n ");
  if (jg.hasArrayRange(fd)) {
    stringBuffer.append("  /** @generated\n   * @param addr low level Feature Structure reference\n   * @param i index of item in the array\n   * @return value at index i in the array \n   */\n  public ");
    stringBuffer.append(elemType);
    stringBuffer.append(" get");
    stringBuffer.append(featUName);
    stringBuffer.append("(int addr, int i) {\n    ");
    stringBuffer.append("");
  
/* checks to insure that cas has the feature */

    stringBuffer.append("    if (featOkTst && casFeat_");
    stringBuffer.append(featName);
    stringBuffer.append(" == null)\n      jcas.throwFeatMissing(\"");
    stringBuffer.append(featName);
    stringBuffer.append("\", \"");
    stringBuffer.append(td.getName());
    stringBuffer.append("\");\n");
    stringBuffer.append("    if (lowLevelTypeChecks)\n      return ll_cas.ll_get");
    stringBuffer.append(getSetArrayNamePart);
    stringBuffer.append("ArrayValue(ll_cas.ll_getRefValue(addr, ");
    stringBuffer.append(casFeatCode);
    stringBuffer.append("), i, true);\n    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, ");
    stringBuffer.append(casFeatCode);
    stringBuffer.append("), i);\n	return ll_cas.ll_get");
    stringBuffer.append(getSetArrayNamePart);
    stringBuffer.append("ArrayValue(ll_cas.ll_getRefValue(addr, ");
    stringBuffer.append(casFeatCode);
    stringBuffer.append("), i);\n  }\n   \n  /** @generated\n   * @param addr low level Feature Structure reference\n   * @param i index of item in the array\n   * @param v value to set\n   */ \n  public void set");
    stringBuffer.append(featUName);
    stringBuffer.append("(int addr, int i, ");
    stringBuffer.append(elemType);
    stringBuffer.append(" v) {\n    ");
    stringBuffer.append("");
  
/* checks to insure that cas has the feature */

    stringBuffer.append("    if (featOkTst && casFeat_");
    stringBuffer.append(featName);
    stringBuffer.append(" == null)\n      jcas.throwFeatMissing(\"");
    stringBuffer.append(featName);
    stringBuffer.append("\", \"");
    stringBuffer.append(td.getName());
    stringBuffer.append("\");\n");
    stringBuffer.append("    if (lowLevelTypeChecks)\n      ll_cas.ll_set");
    stringBuffer.append(getSetArrayNamePart);
    stringBuffer.append("ArrayValue(ll_cas.ll_getRefValue(addr, ");
    stringBuffer.append(casFeatCode);
    stringBuffer.append("), i, v, true);\n    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, ");
    stringBuffer.append(casFeatCode);
    stringBuffer.append("), i);\n    ll_cas.ll_set");
    stringBuffer.append(getSetArrayNamePart);
    stringBuffer.append("ArrayValue(ll_cas.ll_getRefValue(addr, ");
    stringBuffer.append(casFeatCode);
    stringBuffer.append("), i, v);\n  }\n");
   } 
    stringBuffer.append(" \n");
   } 
    stringBuffer.append("\n");
   if (td.getName().equals("uima.cas.Annotation")) { 
    stringBuffer.append("  ");
    stringBuffer.append("  /** @see org.apache.uima.cas.text.AnnotationFS#getCoveredText() \n    * @generated\n    * @param inst the low level Feature Structure reference \n    * @return the covered text \n    */ \n  public String getCoveredText(int inst) { \n    final CASImpl casView = ll_cas.ll_getSofaCasView(inst);\n    final String text = casView.getDocumentText();\n    if (text == null) {\n      return null;\n    }\n    return text.substring(getBegin(inst), getEnd(inst)); \n  }\n");
    stringBuffer.append("");
   } /* of Annotation if-statement */ 
    stringBuffer.append("\n\n  /** initialize variables to correspond with Cas Type and Features\n	 * @generated\n	 * @param jcas JCas\n	 * @param casType Type \n	 */\n  public ");
    stringBuffer.append(typeName_Type);
    stringBuffer.append("(JCas jcas, Type casType) {\n    super(jcas, casType);\n    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());\n\n");
   for (int i = 0; i < fds.length; i++) { 
     FeatureDescription fd = fds[i];

     String featName = fd.getName();


    stringBuffer.append(" \n    casFeat_");
    stringBuffer.append(featName);
    stringBuffer.append(" = jcas.getRequiredFeatureDE(casType, \"");
    stringBuffer.append(featName);
    stringBuffer.append("\", \"");
    stringBuffer.append(fd.getRangeTypeName());
    stringBuffer.append("\", featOkTst);\n    casFeatCode_");
    stringBuffer.append(featName);
    stringBuffer.append("  = (null == casFeat_");
    stringBuffer.append(featName);
    stringBuffer.append(") ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_");
    stringBuffer.append(featName);
    stringBuffer.append(").getCode();\n\n");
   } 
    stringBuffer.append("  }\n}\n\n\n\n    ");
    return stringBuffer.toString();
  }
}