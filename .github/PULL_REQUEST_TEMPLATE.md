Thank you for contributing to Apache UIMA uimaFIT.

In order to streamline the review of the contribution we ask you
to ensure the following steps have been taken:

### For all changes:
- [ ] Is there a JIRA ticket associated with this PR? Is it referenced in the commit message?
      Please include a link to the JIRA ticked in the PR description.
- [ ] Does your PR title start with UIMA-XXXX where XXXX is the JIRA number you are trying to resolve? 
      Pay particular attention to the hyphen "-" character.
- [ ] Has your PR been rebased against the latest commit within the target branch (typically master)?
- [ ] Is your initial contribution a single, squashed commit?
- [ ] Do the commit messages in your PR conform to the following format
```
[UIMA-<ISSUE-NUMBER>] <ISSUE TITLE>
<EMPTY LINE>
- <CHANGE 1>
- <CHANGE 2>
- ...
```

### For code changes:
- [ ] Have you ensured that the full suite of tests is executed via mvn clean install at the root project folder?
- [ ] Have you written or updated unit tests to verify your changes?
- [ ] If adding new dependencies to the code, are these dependencies licensed in a way that is compatible for inclusion under [ASF 2.0](http://www.apache.org/legal/resolved.html#category-a)? 
- [ ] If applicable, have you updated the LICENSE file, including the main LICENSE file in respective module folder?
- [ ] If applicable, have you updated the NOTICE file, including the main NOTICE file found in respective module folder?

### For documentation related changes:
- [ ] Have you ensured that format looks appropriate for the output in which it is rendered?

### Note:
Please ensure that once the PR is submitted, you check the Jenkins build status listed under 
"Checks" in the PR for issues and submit an update to your PR as soon as possible.
