# Gitlet
My first large project built in Java. It is an implementation of a version-control system which is based on the project of CS61B, Berkeley's data structures class (See https://sp21.datastructur.es/materials/proj/proj2/proj2).
This version-control system mimics some of the basic features of the popular version-control system git. The supported functionalities are:
1. Saving the contents of entire directories of files. In Gitlet, this is called committing, and the saved contents themselves are called commits.
2. Restoring a version of one or more files or entire commits. In Gitlet, this is called checking out those files or that commit.
3. Viewing the history of your backups. In Gitlet, you view this history in something called the log.
4. Maintaining related sequences of commits, called branches.
5. Merging changes made in one branch into another.

The main commands that the system supports are:
Local commands:
  - init: java gitlet.Main init
  - add: java gitlet.Main add [file name]
  - commit: java gitlet.Main commit [message]
  - rm: java gitlet.Main rm [file name]
  - log: java gitlet.Main log
  - global-log: java gitlet.Main log
  - find: java gitlet.Main find [commit message]
  - status: java gitlet.Main status
  - checkout:
    - java gitlet.Main checkout -- [file name]
    - java gitlet.Main checkout [commit id] -- [file name]
    - java gitlet.Main checkout [branch name]
  - branch: java gitlet.Main branch [branch name]
  - rm-branch: java gitlet.Main rm-branch [branch name]
  - reset: java gitlet.Main reset [commit id]
  - merge: java gitlet.Main merge [branch name]
- Remote commands:
  - add-remote: java gitlet.Main add-remote [remote name] [name of remote directory]/.gitlet
  - rm-remote: java gitlet.Main rm-remote [remote name]
  - push: java gitlet.Main push [remote name] [remote branch name]
  - fetch: java gitlet.Main fetch [remote name] [remote branch name]
  - pull: java gitlet.Main pull [remote name] [remote branch name]
