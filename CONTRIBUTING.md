# How to contribute to Apache UIMA Java SDK

Thank you for your intention to contribute to the Apache UIMA Java SDK project. 
As an open-source community, we highly appreciate external contributions to our project.

To make the process smooth for the project *committers* (those who review and accept changes) and *contributors* (those who propose new changes via pull requests), there are a few rules to follow.

## Contribution Guidelines

Please check out the [How to get involved](https://uima.apache.org/get-involved.html) to understand how contributions are made. 
A detailed list of coding standards can be found at [Apache UIMA Code Conventions](https://uima.apache.org/codeConventions.html) which also contains a list of coding guidelines that you should follow.
For pull requests, there is a [check list](PULL_REQUEST_TEMPLATE.md) with criteria for acceptable contributions.

## Preparing a Pull Request (PR)

In order to contribute to the  project, you need to create a **pull request**. 
This section briefly guides you through the best way of doing this:

* Before creating a pull request, create an issue in the issue tracker of the project to which
  you wish to contribute
* Fork the project on GitHub
* Create a branch based on the branch to which you wish to contribute. Normally, you should create
  this branch from the **main** branch of the respective project. In the case you want to fix
  a bug in the latest released version, you should consider to branch off the latest maintenance
  branch (e.g. **2.4.x**). If you are not sure, ask via the issue you have just created. Do **not**
  make changes directly to the master or maintenance branches in your fork. The name of the branch
  should be e.g. `feature/UIMA-[ISSUE-NUMBER]-[SHORT-ISSUE-DESCRIPTION]` or `bugfix/UIMA-[ISSUE-NUMBER]-[SHORT-ISSUE-DESCRIPTION]`.
* Now you make changes to your branch. When committing to your branch, use the format shown below
  for your commit messages.
```
[UIMA-<ISSUE-NUMBER>] <ISSUE TITLE>
<EMPTY LINE>
- <CHANGE 1>
- <CHANGE 2>
- ...
```
* You can create the pull request any time after your first commit. I.e. you do not have to wait
  until you are completely finished with your implementation. Creating a pull request early 
  tells other developers that you are actively working on an issue and facilitates asking questions
  about and discussing implementation details.
