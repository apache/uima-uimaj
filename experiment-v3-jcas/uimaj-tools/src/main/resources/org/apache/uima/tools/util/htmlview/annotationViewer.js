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

/* 
 * This global variable is used to allow the user, by clicking the mouse, to "hold" the info frame (the frame that
 * displays the annotation information.  When this variable is set to true, the info frame will not be updated.
 */
var holdInfoFrame = false;

/*
 * Initializes the annotation viewer
 */
function init()
{
	var doc = parent.frames["infoFrame"].document;
    doc.open();
    //write stylesheet reference
    doc.write('<link rel="StyleSheet" href="annotations.css" type="text/css" media="screen">');
    //write "Annotations" heading
    doc.write("<h2>Annotations</h2><br>");
}


/*
 * Clears the info frame of all annotation information.
 */
function clearInfoFrame() 
{
  if (!holdInfoFrame)
  {
    var doc = parent.frames["infoFrame"].document;
    //clear info frame
    
    var node = doc.getElementsByTagName("body")[0];
    // remove all the entries in the <body> tag
    while (node.firstChild) {
    	node.removeChild(node.firstChild);
    }
    //write back the "Annotations" heading
    doc.write("<h2>Annotations</h2><br>");
  }
}  

/*
 * Handles toggling of the holdInfoFrame variable.
 */
function toggleHoldInfoFrame() 
{
  holdInfoFrame = !holdInfoFrame;
  if (!holdInfoFrame)
  {
	clearInfoFrame();
  }
} 


/*
 * Writes information about an annotation to the info frame.
 *
 * styleClass - the value of the "class" attribute assigned to the annotation.  This determines the style
 *              used to format the annotation (e.g. background color)
 * spannedText - the annotated text
 * annotType - the CAS Type of the annotation
 * features - an array containing feature information.  Each element of this array is a two-element array
 *            containing a feature name and its value, in that order.
 */
function writeAnnotationInfo(styleClass, spannedText, annotType, features)
{
  if (!holdInfoFrame)
  {
    var doc = parent.frames["infoFrame"].document;

    //write the annotation's spanned text, formatted the same way in which it appears in the document view
    doc.write('<span class="' + styleClass + '">');
    doc.write(spannedText);
    doc.write('</span><br>');

    //write CAS type and features
    doc.write('CAS Type: ' + annotType + '<br>');
    for (i = 0; i < features.length; i++)
    {
      doc.writeln(features[i][0] + ' = ' + features[i][1] + '<br>');           
    }
    doc.write('<br>');
    doc.write('<br>');   
  }
}


/*
 * Changes a CSS style rule (in the document frame only).  This function assumes that there is only one stylesheet.
 *
 * selector - the selector text for the sytle to be changed.  There must exist a CSS rule with this selector.
 * newStyle - new style description to be assigned to the rule
 *
 */
function changeStyle(selector,newStyle)
{
  var doc = parent.frames["docFrame"].document;
  
  //get array of CSS style rules - note how this is done is different in Netscape and IE
  var theRules = new Array();
  if (doc.styleSheets[0].cssRules)
    theRules = doc.styleSheets[0].cssRules
  else if (doc.styleSheets[0].rules)
    theRules = doc.styleSheets[0].rules
  else 
    return
    
  //find the rule with the specified selector text
  for (i=0; i < theRules.length; i++)
  {
    //do a case-insensitive compare, because the selector text case differs between IE and Netscape
    if (theRules[i].selectorText.toLowerCase() == selector.toLowerCase())	
    {
      //in Netsacpe, can't set a style to the empty string in order to clear it.  
      //instead, set it to 'background:none'
      if (newStyle == '')
        newStyle = "background:none";
      
      //set the style text - this is different in Netscape and IE, so try it both ways
      if (theRules[i].style.cssText)      
        theRules[i].style.cssText = newStyle;
      else
        theRules[i].cssText = newStyle;
    }
  }
}
