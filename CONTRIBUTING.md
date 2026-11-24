# How to contribute to Apache UIMA Java SDK

Thank you for your intention to contribute to the Apache UIMA Java SDK project. 
As an open-source community, we highly appreciate external contributions to our project.

To make the process smooth for the project *committers* (those who review and accept changes) and *contributors* (those who propose new changes via pull requests), there are a few rules to follow.

## Contribution Guidelines

Please check out [How to get involved](https://uima.apache.org/get-involved.html#contributing) to understand how contributions are made. 
A detailed list of coding standards can be found at [Apache UIMA Code Conventions](https://uima.apache.org/codeConventions.html) which also contains a list of coding guidelines that you should follow.
For pull requests, there is a [check list](.github/PULL_REQUEST_TEMPLATE.md) with criteria for acceptable contributions.

## Preparing a Pull Request (PR)

In order to contribute to the  project, you need to create a **pull request**. 
This section briefly guides you through the best way of doing this:

* Before creating a pull request, create an issue in the issue tracker of the project to which
  you wish to contribute
* Fork the project on GitHub
* Create a branch based on the branch to which you wish to contribute. Normally, you should create
  this branch from the **main** branch of the respective project. In the case you want to fix
  a bug in the latest released version, you should consider to branch off the latest maintenance
  branch (e.g. **release/3.6.x**). If you are not sure, ask via the issue you have just created. Do **not**
  make changes directly to the main or maintenance branches in your fork. The name of the branch
  should start with `feature/`, `bugfix/` or `refactoring/`, followed by the issue number and a short
  description, e.g. `bugfix/123-Short-issue-description`.
* Now you make changes to your branch. When committing to your branch, use the format shown below
  for your commit messages.
```
Issue #<ISSUE-NUMBER>: <ISSUE TITLE>
<EMPTY LINE>
- <CHANGE 1>
- <CHANGE 2>
- ...
```
* You can create the pull request any time after your first commit. I.e. you do not have to wait
  until you are completely finished with your implementation. Creating a pull request early 
  tells other developers that you are actively working on an issue and facilitates asking questions
  about and discussing implementation details.
* In the commit message, describe the change, not the history.
* Pull requests follow the [check list](.github/PULL_REQUEST_TEMPLATE.md).
* Update `uimaj-documentation` when user-visible behavior changes.

## Use of AI tools

Apache UIMA follows the
[ASF generative tooling guidance](https://www.apache.org/legal/generative-tooling.html).
In short:

* AI tools may help write code. The contributor reviews every line, can explain it, and is
  responsible for it, including that the tool's terms allow the output to be contributed under
  the Apache License.
* Never include third-party code or data without a compatible license.
* Disclose AI help in the commit message with an `Assisted-by:` trailer naming the tool and its
  version and the model and its version, for example
  `Assisted-by: ExampleCoder 2.1.0 (example-model 4.5)`.
* What is posted on issues and pull requests is the contributor's own words. AI-generated text is
  quoted and marked as such.
* When in doubt, ask on the issue or on dev@uima.apache.org.

## Build

* JDK 21 and Maven 3.9 or higher (`mvn`, there is no wrapper). With `-pl`, add `-am`.
* Run `mvn verify` on the touched modules before proposing a change. This runs the tests, the
  license header check (Apache RAT), the dependency analysis (`dependency:analyze-only`, which
  fails on unused or undeclared dependencies) and the API compatibility check (japicmp). Say in the
  pull request which checks ran, which failed and which were skipped.
* Do not introduce new dependencies without a discussion and approval. New dependencies must be
  under a [Category A license](https://www.apache.org/legal/resolved.html#category-a), and the
  `LICENSE` and `NOTICE` files of the affected modules and of the project root must be updated.
* The modules are OSGi bundles. A new package, or a package that moves, may need an export or
  import in the bundle configuration of its module.
* Bugfix releases come from `release/<major>.<minor>.x` branches, new features from `main`.
  Maintenance branches may use an older Java release.

## Code

* Every new file carries the Apache license header, including test resources where the format
  allows comments. RAT fails the build otherwise.
* Formatting follows the
  [Apache UIMA Eclipse formatter profile](https://uima.apache.org/downloads/ApacheUima_EclipseCodeStylePrefs.xml).
  The repository carries the same settings in `src/main/eclipse`; `installEclipseSettings.sh`
  installs them into the modules. In short:
  * Indent with 2 spaces, no tabs.
  * Lines, including comments, wrap at 100 characters. Wrapped lines continue with an indent of
    8 spaces.
  * Opening braces go at the end of the line. `else`, `catch` and `finally` go on the same line
    as the preceding closing brace.
  * No package (wildcard) imports such as `import org.apache.uima.*`.
  * Within a class, order the definitions as: static variables, instance variables,
    constructors, methods.
  * Do not reformat code you do not otherwise change.
* Keep the public API binary and source compatible. Do not remove or change the signature of
  public or protected members; deprecate instead and keep the old member working. If an
  incompatible change is unavoidable, it needs a discussion on the issue first.
* Use existing helpers before writing new ones. Check the JDK, Apache Commons where it is already
  a dependency of the module, and the utilities of the package you work in (for uimaFIT,
  `JCasUtil`, `CasUtil`, `FSCollectionFactory` and the factories). Do not duplicate existing
  functionality without a clear technical reason.
* Never rely on the default charset; name the charset explicitly (`StandardCharsets.UTF_8`).
* Prefer `var` for local variables.
* Name repeated literals as constants. Avoid unnecessary allocations in performance-critical code
  such as CAS access, indexing and serialization.
* Changed `Serializable` classes need a new `serialVersionUID` where the serialized form changes.
  Changes to serialization formats (XMI, XCAS, binary, JSON) must stay readable by and for the
  supported older versions unless the issue says otherwise.

## Javadoc and comments

* New public and protected APIs, and changes to a documented contract, get short, accurate
  Javadoc. Overrides use `{@inheritDoc}` or no Javadoc. New public types and methods get `@since`
  with the upcoming version.
* Describe the contract, not the history. No issue numbers or design narrative in source; that
  goes in the issue.
* Avoid em dashes in new or changed Javadoc, comments and documentation. Do not edit unrelated
  text only to remove them.
* Use the terms the code and the manuals already use (CAS, feature structure, type system,
  analysis engine, ...); do not invent new ones.

## Tests

* Test first for behavior changes: the test fails before the fix.
* Tests use JUnit 5 and AssertJ. Many inputs go in a `@ParameterizedTest`, not a loop.
* Reuse shared fixtures and the test utility modules instead of copying helpers into each test
  class.
* Assert exception type and message (`assertThatExceptionOfType`). Do not reimplement the code
  under test as the expected value.
* Tests log through a logger at debug level instead of writing to `System.out` or `System.err`.
* Do not add tests that repeat a case another test already covers.
