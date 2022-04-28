Apache uimaFIT (TM) v3.2.0
==========================

What's New in 3.2.0
-------------------

uimaFIT 3.2.0 is a feature and bugfix release. On supported platforms, it serves mostly as 
a drop-in replacement for previous uimaFIT 3.x versions. However, the behavior of the various
select methods was slightly adapted in edge cases to align with the update behavior of the UIMA Java
SDK SelectFS API.  For details, please refer to the migration section in the documentation in the
Apache UIMA Java SDK 3.2.0.

Notable changes in this release include:

#### New Features and improvements

* [UIMA-6242] - uimaFIT Maven plugin should fail on error by default
* [UIMA-6263] - CAS validation support
* [UIMA-6270] - Add selectOverlapping to (J)CasUtil
* [UIMA-6311] - Add generated resources output folder as resource folder
* [UIMA-6312] - Better PEAR parameter support
* [UIMA-6232] - Reduce overhead of createTypeSystemDescription() and friends

#### Bugs fixed

* [UIMA-6226] - uimaFIT maven plugin "generate" fails to import type systems from dependencies
* [UIMA-6240] - Failure to resolve type system imports when generating descriptors
* [UIMA-6275] - InitializableFactory is not smart enough to find a suitable classloader
* [UIMA-6286] - select following finds zero-width annotation at reference end position
* [UIMA-6292] - selectCovering is slow
* [UIMA-6294] - SelectFS.at(annotation) does not return the correct result
* [UIMA-6314] - Align preceding/following with predicate in UIMA core

 
A full list of issues addressed in this release can be found on the Apache issue tracker:

  https://issues.apache.org/jira/issues/?jql=project%20%3D%20UIMA%20AND%20fixVersion%20%3D%203.2.0uimaFIT


Supported Platforms
-------------------

uimaFIT 3.2.0 requires Java 1.8 or higher, UIMA 3.2.0 or higher, and the Spring Framework 4.3.30 or higher.
