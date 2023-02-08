Apache uimaFIT (TM) v3.4.0
==========================

This is a feature and bugfix release. 

## What's Changed

**Improvements**
* ⭐️ Issue #195: Detect descriptors via SPI by @reckart in https://github.com/apache/uima-uimafit/pull/197, https://github.com/apache/uima-uimafit/pull/204
* ⭐️ Issue #196: Provide OSGI metadata in JARs by @reckart in https://github.com/apache/uima-uimafit/pull/199, https://github.com/apache/uima-uimafit/pull/202, https://github.com/apache/uima-uimafit/pull/203
* ⭐️ Issue #205: Managed CASes in tests should consider validators by @reckart in https://github.com/apache/uima-uimafit/pull/206
* ⭐️ Issue #209: Ability to override validator for a test by @reckart in https://github.com/apache/uima-uimafit/pull/210
* ⭐️ Issue #211: Conveniently skip validation by @reckart in https://github.com/apache/uima-uimafit/pull/212
* ⭐️ Issue #215: Improve descriptor scanning performance when there are many classloaders by @reckart in https://github.com/apache/uima-uimafit/pull/216
* ⭐️ Issue #220: Add getType signature accepting a type system by @reckart in https://github.com/apache/uima-uimafit/pull/221

**Bugs fixed**
* 🦟 Issue #207: Cannot override CAS content set in a BeforeEach method by @reckart in https://github.com/apache/uima-uimafit/pull/208
* 🦟 Issue #213: Avoid broken typesystem when using ManagedCas by @reckart in https://github.com/apache/uima-uimafit/pull/214

**Refactorings**

* ⚙️ Issue #198: Remove version overrides in Maven plugin modules by @reckart in https://github.com/apache/uima-uimafit/pull/200
* 🩹 Issue #218: Update dependencies by @reckart in https://github.com/apache/uima-uimafit/pull/219, https://github.com/apache/uima-uimafit/pull/222, https://github.com/apache/uima-uimafit/pull/223

For a full list of issues affecting this release, please see:

* [GitHub issues](issuesFixed/github-report.html) [[online](https://github.com/apache/uima-uimafit/issues?q=milestone%3A3.4.0)]


### Supported Platforms

uimaFIT 3.4.0 should be used in combination with 

* Java 1.8 or higher
* UIMA Java SDK 3.4.0 or higher
* Spring Framework 5.3.25 or higher
