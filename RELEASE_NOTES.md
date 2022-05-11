Apache uimaFIT (TM) v3.3.0
==========================

This is a feature and bugfix release. 

## Notable changes in this release

### New Features and improvements

* [UIMA-6431] - Use lambda functions as CAS processors
* [UIMA-6422] - `FSUtil.setFeature()` should offer signatures that accept a Feature
* [UIMA-6392] - Better delegate key generation in aggregate engine
* [UIMA-6424] - Upgrade uimaFIT to JUnit 5
* [UIMA-6426] - Upgrade to UIMA Java SDK 3.3.0
* [UIMA-6432] - Upgrade dependencies (uimaFIT 3.3.0)

### Bugs fixed

* [UIMA-6384] - Parallelism argument in `CpePipeline` is ignored
* [UIMA-6385] - Potential resource key clash in environments with multiple classloaders
* [UIMA-6391] - `CpePipeline` should kill CPE if reader throws exception
* [UIMA-6396] - uimaFIT maven plugin mixes up test and compile scopes
* [UIMA-6417] - Problems setting numeric parameter values
* [UIMA-6446] - Complexities around enhancing classes with their resource name
 
A full list of issues addressed in this release can be found on the Apache issue tracker:

  https://issues.apache.org/jira/issues/?jql=project%20%3D%20UIMA%20AND%20fixVersion%20%3D%203.3.0uimaFIT

### API changes

#### Inheritance of `@ResourceMetaData`

The `@ResourceMetaData` is no longer "inherited" by sub-classes of the annotated component class (cf.
UIMA-6446).

#### JUnit upgrade

The JUnit module has been upgraded from JUnit 4 to JUnit 5 along with the rest of the test code
switching to JUnit 5. If you use the unit test helpers from this module, you also have to upgrade
your tests to JUnit 5.

### Supported Platforms

uimaFIT 3.3.0 should be used in combination with 

* Java 1.8 or higher
* UIMA Java SDK 3.3.0 or higher
* Spring Framework 5.3.19 or higher
