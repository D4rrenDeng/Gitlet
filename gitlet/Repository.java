package gitlet;

/* import edu.princeton.cs.algs4.StdOut;
import jdk.jshell.execution.Util;
import org.apache.commons.math3.stat.inference.GTest; */

import java.io.File;
import static gitlet.Utils.*;

import java.util.*;


/** Represents a gitlet repository.
 *  does at a high level.
 *
 *  @author Darren Deng
 */
public class Repository {
    /**
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    public static final File COMMIT_DIR = join(GITLET_DIR, "commit");
    public static final File STAGE_DIR = join(GITLET_DIR, "stage");
    public static final File BLOB_DIR = join(GITLET_DIR, "blob");
    public static final File STAGEADDFILE = join(STAGE_DIR, "Stage_Add");
    public static final File STAGEREMOVEFILE = join(STAGE_DIR, "Stage_Remove");
    public static final File ACTIVEBRANCH_FILE = join(GITLET_DIR, "activeBranch");
    public static final File BRANCH_FILE = join(GITLET_DIR, "branch");
    public static final File HEAD_FILE = join(GITLET_DIR, "head");
    public static final File REMOTE_FILE = join(GITLET_DIR, "remote");
    /** stage area for add and remove */
    private static HashMap<String, String> stageToAdd;
    private static HashSet<String> stageToRemove;
    private static String head;
    private static String activeBranch;
    private static HashMap<String, String> branches;
    private static HashMap<String, String> remote;

    public static void deserializeFiles() {
        stageToAdd = (HashMap) readObject(STAGEADDFILE, HashMap.class);
        stageToRemove = (HashSet) readObject(STAGEREMOVEFILE, HashSet.class);
        head = (String) readObject(HEAD_FILE, String.class);
        activeBranch = (String) readObject(ACTIVEBRANCH_FILE, String.class);
        branches = (HashMap) readObject(BRANCH_FILE, HashMap.class);
    }

    public static void serializeFiles() {
        writeObject(STAGEADDFILE, stageToAdd);
        writeObject(STAGEREMOVEFILE, stageToRemove);
        writeObject(HEAD_FILE, head);
        writeObject(ACTIVEBRANCH_FILE, activeBranch);
        writeObject(BRANCH_FILE, branches);
    }

    public static Commit deserializeCommit(String commitSha) {
        File commitFile = join(COMMIT_DIR, commitSha);
        Commit commit = (Commit) readObject(commitFile, Commit.class);
        return commit;
    }

    public static void initCommand() {
        if (GITLET_DIR.exists()) {
            System.out.println("A Gitlet version-control system already exists in "
                    + "the current directory.");
            System.exit(0);
        }
        GITLET_DIR.mkdir();
        COMMIT_DIR.mkdir();
        STAGE_DIR.mkdir();
        BLOB_DIR.mkdir();
        /** initialize staging area */
        stageToAdd = new HashMap<>();
        stageToRemove = new HashSet<>();
        branches = new HashMap<>();

        writeObject(STAGEADDFILE, stageToAdd);
        writeObject(STAGEREMOVEFILE, stageToRemove);
        /** create one commit with initial message */
        Commit newCommit = new Commit("initial commit", String.format(
                "%1$ta %1$tb %1$te %1$tH:%1$tM:%1$tS %1$tY %1$tz", new Date(0)), null, null);
        /** change file name to SHA-1 rendered as hexadecimal string */
        byte[] newCommitByte = serialize(newCommit);
        String sha = sha1(newCommitByte);
        activeBranch = "master";
        branches.put(activeBranch, sha);
        head = sha;
        /* write everything into files */
        writeObject(BRANCH_FILE, branches);
        writeObject(ACTIVEBRANCH_FILE, activeBranch);
        writeObject(HEAD_FILE, head);
        File initialCommitFile = join(COMMIT_DIR, sha);
        writeObject(initialCommitFile, newCommit);
    }

    public static void addCommand(String fileName) {
        File file = join(CWD, fileName);

        if (!file.exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        }
        String content = readContentsAsString(file);
        deserializeFiles();
        Commit headCommit = deserializeCommit(head);

            /** check if the file in the current commit is the same as the working version */
        String sha = sha1(content);
        if (headCommit.containsFile(fileName)) {
            if (sha.equals(headCommit.getFile(fileName))) {
                if (stageToAdd.containsKey(fileName)) {
                    /* delete it if the current commit has the same version and
                    the file exists in the stage area */
                    stageToAdd.remove(fileName);
                }
                if (stageToRemove.contains(fileName)) {
                    stageToRemove.remove(fileName);
                }
            } else {
                stageToAdd.put(fileName, sha);
            }
        } else {
            stageToAdd.put(fileName, sha);
        }
        serializeFiles();
    }

    public static void commitCommand(String message) {
        deserializeFiles();
        Commit headCommit = deserializeCommit(head);
        Commit newHeadCommit = headCommit.copyCommit();
        newHeadCommit.setMessage(message);
        newHeadCommit.setParents(head, null);

        if (stageToAdd.isEmpty() && stageToRemove.isEmpty()) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        } else if (message.equals("")) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        }
        /* update existing file and add new file based on add staging area,
        also add every new/updated file to blob dir */
        for (Map.Entry<String, String> entry : stageToAdd.entrySet()) {
            newHeadCommit.putFile(entry.getKey(), entry.getValue());
            File newFile = join(CWD, entry.getKey());
            File newBlob = join(BLOB_DIR, entry.getValue());

            String newContent = readContentsAsString(newFile);
            writeContents(newBlob, newContent);
        }
        /* remove files from current commit based on rm staging area */
        for (String key : stageToRemove) {
            newHeadCommit.removeFile(key);
        }
        byte[] newHeadCommitByte = serialize(newHeadCommit);
        String sha = sha1(newHeadCommitByte);
        head = sha;
        branches.put(activeBranch, sha);
        File newCommitFile = join(COMMIT_DIR, sha);
        writeObject(newCommitFile, newHeadCommit);
        /* clear staging area */
        stageToAdd.clear();
        stageToRemove.clear();
        serializeFiles();
    }

    public static void removeCommand(String fileName) {
        File file = join(CWD, fileName);
        deserializeFiles();
        Commit headCommit = deserializeCommit(head);
        if (!stageToAdd.containsKey(fileName) && !headCommit.containsFile(fileName)) {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }
        if (stageToAdd.containsKey(fileName)) {
            stageToAdd.remove(fileName);
        }
        if (headCommit.containsFile(fileName)) {
            stageToRemove.add(fileName);
            restrictedDelete(file);
        }
        serializeFiles();
    }

    public static void logCommand() {
        String curSha = (String) readObject(HEAD_FILE, String.class);
        String log = "";
        while (curSha != null) {
            Commit curCommit = deserializeCommit(curSha);
            if (curCommit.getSecondParent() == null) {
                log = log + "===\ncommit " + curSha + "\nDate: " + curCommit.getDate()
                        + "\n" + curCommit.getMessage() + "\n\n";
            } else {
                log = log + "===\ncommit " + curSha + "\nMerge: "
                        + curCommit.getParent().substring(0, 7) + " "
                        + curCommit.getSecondParent().substring(0, 7) + "\nDate: "
                        + curCommit.getDate() + "\n" + curCommit.getMessage() + "\n\n";
            }
            curSha = curCommit.getParent();
        }
        System.out.print(log);
    }

    public static void globalLogCommand() {
        List<String> commits = plainFilenamesIn(COMMIT_DIR);
        String log = "";
        for (String commit : commits) {
            Commit curCommit = deserializeCommit(commit);
            if (curCommit.getSecondParent() == null) {
                log = log + "===\ncommit " + commit + "\nDate: " + curCommit.getDate() + "\n"
                        + curCommit.getMessage() + "\n\n";
            } else {
                log = log + "===\ncommit " + commit + "\nMerge: "
                        + curCommit.getParent().substring(0, 7) + " "
                        + curCommit.getSecondParent().substring(0, 7) + "\nDate: "
                        + curCommit.getDate() + "\n" + curCommit.getMessage() + "\n\n";
            }
        }
        System.out.print(log);
    }

    public static void findCommand(String message) {
        List<String> commits = plainFilenamesIn(COMMIT_DIR);
        String list = "";
        for (String commit : commits) {
            Commit curCommit = deserializeCommit(commit);
            if (curCommit.getMessage().equals(message)) {
                list = list + commit + "\n";
            }
        }
        if (list.isEmpty()) {
            System.out.println("Found no commit with that message.");
        } else {
            System.out.println(list);
        }
    }

    public static void statusCommand() {
        deserializeFiles();
        Commit headCommit = deserializeCommit(head);
        List<String> filesCWD = plainFilenamesIn(CWD);

        String status = "=== Branches ===\n";
        List<String> sortedBranchList = new ArrayList<>(branches.keySet());
        Collections.sort(sortedBranchList);
        for (String branch : sortedBranchList) {
            branch = branch.equals(activeBranch) ? "*" + branch : branch;
            status = status + branch + "\n";
        }
        status += "\n=== Staged Files ===\n";
        List<String> sortedStagedList = new ArrayList<>(stageToAdd.keySet());
        Collections.sort(sortedStagedList);
        for (String file : sortedStagedList) {
            status = status + file + "\n";
        }
        status += "\n=== Removed Files ===\n";
        List<String> sortedRemovedList = new ArrayList<>(stageToRemove);
        Collections.sort(sortedRemovedList);
        for (String file : sortedRemovedList) {
            status = status + file + "\n";
        }
        status += "\n=== Modifications Not Staged For Commit ===\n";
        List<String> modList = new ArrayList<>();
        for (String file : filesCWD) {
            if (headCommit.getFiles().containsKey(file)) {
                File curFile = join(CWD, file);
                String curContent = readContentsAsString(curFile);
                String curSha = sha1(curContent);
                if (!curSha.equals(headCommit.getFiles().get(file))
                        && !stageToAdd.containsKey(file)) {
                    modList.add(file + " (modified)");
                }
            }
        }
        /* check the staging area files in the CWD to see if it's changed or deleted */
        for (String file : filesCWD) {
            if (stageToAdd.containsKey(file)) {
                File curFile = join(CWD, file);
                if (curFile.exists()) {
                    String curContent = readContentsAsString(curFile);
                    String curSha = sha1(curContent);
                    if (!curSha.equals(stageToAdd.get(file))) {
                        modList.add(file + " (modified)");
                    }
                } else {
                    modList.add(file + " (deleted)");
                }
            }
        }
        /* not staged for removal, but tracked in the current commit and
        deleted from the working directory */
        for (String file : headCommit.getFiles().keySet()) {
            File curFile = join(CWD, file);
            if (!curFile.exists() && !stageToRemove.contains(file)) {
                modList.add(file + " (deleted)");
            }
        }
        Collections.sort(modList);
        for (String modFile : modList) {
            status = status + modFile + "\n";
        }
        status += "\n=== Untracked Files ===\n";
        List<String> untrackedList = new ArrayList<>();
        for (String file : filesCWD) {
            if (!stageToAdd.containsKey(file) && !headCommit.getFiles().containsKey(file)
                    || stageToRemove.contains(file)) {
                untrackedList.add(file);
            }
        }
        Collections.sort(untrackedList);
        for (String untrackedFile : untrackedList) {
            status = status + untrackedFile + "\n";
        }
        System.out.println(status);
    }

    public static void checkoutFileCommand(String fileName) {
        head = (String) readObject(HEAD_FILE, String.class);
        Commit headCommit = deserializeCommit(head);
        HashMap<String, String> headFiles = headCommit.getFiles();
        if (!headFiles.containsKey(fileName)) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
        File newFile = join(BLOB_DIR, headFiles.get(fileName));
        File newFileCWD = join(CWD, fileName);

        String newContent = readContentsAsString(newFile);
        writeContents(newFileCWD, newContent);
    }

    /* check for abbreviated commit ID */
    public static String abbCommidIDCheck(String commitID) {
        if (commitID.length() < 40) {
            List<String> commits = plainFilenamesIn(COMMIT_DIR);

            for (String commit : commits) {
                if (commitID.equals(commit.substring(0, commitID.length()))) {
                    commitID = commit;
                    break;
                }
            }
        }
        return commitID;
    }

    public static void checkoutFileCommand(String commitID, String fileName) {
        commitID = abbCommidIDCheck(commitID);
        File commitFile = join(COMMIT_DIR, commitID);

        if (!commitFile.exists()) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        Commit commit = (Commit) readObject(commitFile, Commit.class);
        HashMap<String, String> files = commit.getFiles();
        if (!files.containsKey(fileName)) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
        File newFile = join(BLOB_DIR, files.get(fileName));
        File newFileCWD = join(CWD, fileName);

        String newContent = readContentsAsString(newFile);
        writeContents(newFileCWD, newContent);
    }

    public static void checkoutBranchCommand(String branch) {
        deserializeFiles();

        /* if branch does not exist */
        if (!branches.containsKey(branch)) {
            System.out.println("No such branch exists.");
            System.exit(0);
        }
        /* if branch is the active branch */
        if (branch.equals(activeBranch)) {
            System.out.println("No need to checkout the current branch.");
            System.exit(0);
        }
        String newHead = branches.get(branch);
        Commit headCommit = deserializeCommit(head);
        HashMap<String, String> headFiles = headCommit.getFiles();
        Commit newHeadCommit = deserializeCommit(newHead);
        HashMap<String, String> newHeadFiles = newHeadCommit.getFiles();
        List<String> filesCWD = plainFilenamesIn(CWD);
        /* check if a working file is untracked in the current branch and would be
        overwritten by the checked-out branch */
        for (String file : filesCWD) {
            if (newHeadFiles.containsKey(file) && !headFiles.containsKey(file)) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
                System.exit(0);
            }
        }
        /* delete all the files that are tracked in the current branch but not tracked
        in the checked-out branch */
        for (HashMap.Entry<String, String> entry : headFiles.entrySet()) {
            if (!newHeadFiles.containsKey(entry.getKey())) {
                File file = join(CWD, entry.getKey());
                restrictedDelete(file);
            }
        }
        /* add all the files from the checked-out branch into the CWD */
        for (HashMap.Entry<String, String> entry : newHeadFiles.entrySet()) {
            File file = join(BLOB_DIR, entry.getValue());
            String content = readContentsAsString(file);
            File newFileCWD = join(CWD, entry.getKey());
            writeContents(newFileCWD, content);
        }
        /* update the current branch */
        writeObject(ACTIVEBRANCH_FILE, branch);
        /* update the new head */
        writeObject(HEAD_FILE, newHead);
        /* clear staging area */
        stageToAdd.clear();
        writeObject(STAGEADDFILE, stageToAdd);
        stageToRemove.clear();
        writeObject(STAGEREMOVEFILE, stageToRemove);
    }

    public static void branchCommand(String branchName) {
        deserializeFiles();

        if (branches.containsKey(branchName)) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }
        branches.put(branchName, head);
        serializeFiles();
    }

    public static void removeBranchCommand(String branchName) {
        deserializeFiles();

        if (!branches.containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        if (activeBranch.equals(branchName)) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }
        branches.remove(branchName);
        serializeFiles();
    }

    public static void resetCommand(String commitID) {
        commitID = abbCommidIDCheck(commitID);
        File commitFile = join(COMMIT_DIR, commitID);
        if (!commitFile.exists()) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }

        activeBranch = (String) readObject(ACTIVEBRANCH_FILE, String.class);
        String branch = activeBranch;
        branches = (HashMap) readObject(BRANCH_FILE, HashMap.class);
        branches.put(activeBranch, commitID);
        writeObject(BRANCH_FILE, branches);
        /* temporarily set activeBranch as null to avoid failure case in checkoutCommand */
        activeBranch = null;
        writeObject(ACTIVEBRANCH_FILE, activeBranch);
        checkoutBranchCommand(branch);
    }

    public static String findSplit(String branchHead, String givenBranch) {
        deserializeFiles();
        String givenBranchHead = branches.get(givenBranch);
        if (!stageToAdd.isEmpty() || !stageToRemove.isEmpty()) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }
        if (!branches.containsKey(givenBranch)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        if (activeBranch.equals(givenBranch)) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }
        HashSet<String> headAncestor = new HashSet<>();
        Stack<String> stack = new Stack<>();
        stack.push(branchHead);
        while (!stack.isEmpty()) {
            String curSha = stack.pop();
            headAncestor.add(curSha);
            Commit curCommit = deserializeCommit(curSha);
            if (curCommit.getParent() != null) {
                stack.push(curCommit.getParent());
            }
            if (curCommit.getSecondParent() != null) {
                stack.push(curCommit.getSecondParent());
            }
        }

        Queue<String> queue = new LinkedList<>();
        queue.offer(givenBranchHead);
        String splitPointSha = null;
        while (!queue.isEmpty()) {
            String curGivenSha = queue.poll();
            if (headAncestor.contains(curGivenSha)) {
                splitPointSha = curGivenSha;
                break;
            }
            Commit curGivenCommit = deserializeCommit(curGivenSha);
            if (curGivenCommit.getParent() != null) {
                queue.offer(curGivenCommit.getParent());
            }
            if (curGivenCommit.getSecondParent() != null) {
                queue.offer(curGivenCommit.getSecondParent());
            }
        }
        return splitPointSha;
    }

    public static void mergeFailCheck(String givenBranch, String splitPointSha) {
        deserializeFiles();
        List<String> filesCWD = plainFilenamesIn(CWD);
        String givenBranchSha = branches.get(givenBranch);
        Commit givenCommit = deserializeCommit(givenBranchSha);
        HashMap<String, String> givenFiles = givenCommit.getFiles();
        Commit splitCommit = deserializeCommit(splitPointSha);
        HashMap<String, String> splitFiles = splitCommit.getFiles();
        Commit curCommit = deserializeCommit(head);
        HashMap<String, String> curFiles = curCommit.getFiles();
        for (String file : filesCWD) {
            if (!curFiles.containsKey(file)) {
                String errorMessage = "There is an untracked file in the way; delete it, "
                        + "or add and commit it first.";
                if (!splitFiles.containsKey(file) && givenFiles.containsKey(file)) {
                    System.out.println(errorMessage);
                    System.exit(0);
                }
                if (splitFiles.containsKey(file) && givenFiles.containsKey(file)
                        && !splitFiles.get(file).equals(givenFiles.get(file))) {
                    System.out.println(errorMessage);
                    System.exit(0);
                }
            }
        }
        if (splitPointSha.equals(givenBranchSha)) {
            System.out.println("Given branch is an ancestor of the current branch.");
            System.exit(0);
        }
        if (splitPointSha.equals(head)) {
            checkoutBranchCommand(givenBranch);
            System.out.println("Current branch fast-forwarded.");
            System.exit(0);
        }
    }

    public static void mergeCommand(String givenBranch) {
        deserializeFiles();
        String givenBranchSha = branches.get(givenBranch);
        String splitPointSha = findSplit(head, givenBranch);
        mergeFailCheck(givenBranch, splitPointSha);
        Commit splitCommit = deserializeCommit(splitPointSha);
        HashMap<String, String> splitFiles = splitCommit.getFiles();
        Commit givenCommit = deserializeCommit(givenBranchSha);
        HashMap<String, String> givenFiles = givenCommit.getFiles();
        Commit curCommit = deserializeCommit(head);
        HashMap<String, String> curFiles = curCommit.getFiles();
        boolean mergeConflict = false;
        for (HashMap.Entry<String, String> entry : splitFiles.entrySet()) {
            if (givenFiles.containsKey(entry.getKey()) /* case 1 */
                    && curFiles.containsKey(entry.getKey())
                    && entry.getValue().equals(curFiles.get(entry.getKey()))
                    && !entry.getValue().equals(givenFiles.get(entry.getKey()))) {
                checkoutFileCommand(givenBranchSha, entry.getKey());
                stageToAdd.put(entry.getKey(), entry.getValue());
            } else if (entry.getValue().equals(curFiles.get(entry.getKey())) /* case 6 */
                    && !givenFiles.containsKey(entry.getKey())) {
                removeCommand(entry.getKey());
            } else if (curFiles.containsKey(entry.getKey())
                    && givenFiles.containsKey(entry.getKey())
                    && !entry.getValue().equals(curFiles.get(entry.getKey()))
                    && !entry.getValue().equals(givenFiles.get(entry.getKey()))
                    && !curFiles.get(entry.getKey()).equals(givenFiles.get(entry.getKey()))
                    || curFiles.containsKey(entry.getKey()) /* case 8*/
                    && !givenFiles.containsKey(entry.getKey())
                    && !entry.getValue().equals(curFiles.get(entry.getKey()))
                    || givenFiles.containsKey(entry.getKey())
                    && !curFiles.containsKey(entry.getKey())
                    && !entry.getValue().equals(givenFiles.get(entry.getKey()))) {
                mergeConflict = true;
                String curContent = curFiles.containsKey(entry.getKey())
                        ? readContentsAsString(join(BLOB_DIR, curFiles.get(entry.getKey()))) : "\n";
                String givenContent = givenFiles.containsKey(entry.getKey())
                        ? readContentsAsString(join(BLOB_DIR,
                        givenFiles.get(entry.getKey()))) : "\n";
                String newContent = "<<<<<<< HEAD\n" + curContent + "\n=======\n"
                        + givenContent + "\n>>>>>>>";
                File newFile = join(CWD, entry.getKey());
                writeContents(newFile, newContent);
                stageToAdd.put(entry.getKey(), sha1(newContent));
            }
        }
        for (HashMap.Entry<String, String> entry : givenFiles.entrySet()) {
            if (!splitFiles.containsKey(entry.getKey()) /* case 5 */
                    && !curFiles.containsKey(entry.getKey())) {
                checkoutFileCommand(givenBranchSha, entry.getKey());
                stageToAdd.put(entry.getKey(), entry.getValue());
            } else if (!splitFiles.containsKey(entry.getKey())
                    && curFiles.containsKey(entry.getKey()) /* case 8, not in splitpoint */
                    && !entry.getValue().equals(curFiles.get(entry.getKey()))) {
                mergeConflict = true;
                File curFile = join(BLOB_DIR, curFiles.get(entry.getKey()));
                File givenFile = join(BLOB_DIR, entry.getValue());
                String curContent = readContentsAsString(curFile);
                String givenContent = readContentsAsString(givenFile);
                String newContent = "<<<<<<< HEAD\n" + curContent + "\n=======\n"
                        + givenContent + "\n>>>>>>>";
                File newFile = join(CWD, entry.getKey());
                writeContents(newFile, newContent);
                stageToAdd.put(entry.getKey(), sha1(newContent));
            }
        }
        serializeFiles();
        String commitMessage = "Merged " + givenBranch + " into " + activeBranch + ".";
        commitCommand(commitMessage);
        if (mergeConflict) {
            System.out.println("Encountered a merge conflict.");
        }
        File headCommitFile = join(COMMIT_DIR, head);
        Commit headCommit = (Commit) readObject(headCommitFile, Commit.class);
        headCommit.setSecondParent(givenBranchSha);
        writeObject(headCommitFile, headCommit);
    }

    public static void addRemoteCommand(String remoteName, String remotePath) {
        if (!REMOTE_FILE.exists()) {
            remote = new HashMap<>();
        } else {
            remote = (HashMap) readObject(REMOTE_FILE, HashMap.class);
        }
        if (remote.containsKey(remoteName)) {
            System.out.println("A remote with that name already exists.");
            System.exit(0);
        }
        remote.put(remoteName, remotePath);
        writeObject(REMOTE_FILE, remote);
        File remoteFile = new File(remotePath);
        if (!remoteFile.exists()) {
            remoteFile.mkdir();
        }
    }

    public static void rmRemoteCommand(String remoteName) {
        remote = (HashMap) readObject(REMOTE_FILE, HashMap.class);
        if (!remote.containsKey(remoteName)) {
            System.out.println("A remote with that name does not exist.");
            System.exit(0);
        } else {
            remote.remove(remoteName);
        }
        writeObject(REMOTE_FILE, remote);
    }

    public static void pushCommand(String remoteName, String remoteBranchName) {
        head = (String) readObject(HEAD_FILE, String.class);
        HashSet<String> headAncestor = new HashSet<>();
        remote = (HashMap) readObject(REMOTE_FILE, HashMap.class);
        String remotePath = remote.get(remoteName);
        File remoteDir = new File(remotePath);
        if (!remoteDir.exists()) {
            System.out.println("Remote directory not found.");
            System.exit(0);
        }
        File remoteBranchFile = join(remotePath, "branch");
        File remoteHeadFile = join(remotePath, "head");
        HashMap<String, String> remoteBranches = readObject(remoteBranchFile, HashMap.class);

        String curCommitSha = head;
        /* find the ancesters of head commit */
        while (curCommitSha != null) {
            headAncestor.add(curCommitSha);
            Commit curCommit = deserializeCommit(curCommitSha);
            curCommitSha = curCommit.getParent();
        }
        if (remoteBranches.containsKey(remoteBranchName)) {
            String remoteHead = remoteBranches.get(remoteBranchName);
            if (!headAncestor.contains(remoteHead)) {
                System.out.println("Please pull down remote changes before pushing.");
                System.exit(0);
            } else {
                /* if remoteHead is in the history of the current branch,
                get the future commits */
                HashSet<String> newCommits = new HashSet<>();
                curCommitSha = head;
                while (curCommitSha != null) {
                    if (remoteHead.equals(curCommitSha)) {
                        break;
                    } else {
                        newCommits.add(curCommitSha);
                        Commit curCommit = deserializeCommit(curCommitSha);
                        curCommitSha = curCommit.getParent();
                    }
                }
                /* append the future commits to the remote branch */
                for (String curSha : newCommits) {
                    Commit curCommit = deserializeCommit(curSha);
                    HashMap<String, String> curFiles = curCommit.getFiles();
                    File remoteCommitFile =
                            join(remotePath, "commit", curSha);
                    writeObject(remoteCommitFile, curCommit);
                    for (String file : curFiles.values()) {
                        File curFile = join(BLOB_DIR, file);
                        String curContent = readContentsAsString(curFile);
                        File curRemoteFile = join(remotePath,
                                "blob", file);
                        writeContents(curRemoteFile, curContent);
                    }
                }
                remoteBranches.put(remoteBranchName, head);
            }
        } else {
            /* if the remote machine does not have the input branch,
            then simply add the branch to the remote Gitlet pointing to the current remote head */
            String remoteHead = (String) readObject(remoteHeadFile, String.class);
            remoteBranches.put(remoteBranchName, remoteHead);
        }
        writeObject(remoteBranchFile, remoteBranches);
        writeObject(remoteHeadFile, head);
    }

    public static void fetchCommand(String remoteName, String remoteBranchName) {
        remote = (HashMap) readObject(REMOTE_FILE, HashMap.class);
        String remotePath = remote.get(remoteName);
        File remoteDir = new File(remotePath);
        if (!remoteDir.exists()) {
            System.out.println("Remote directory not found.");
            System.exit(0);
        }

        File remoteBranchFile = join(remotePath, "branch");
        HashMap<String, String> remoteBranches = (HashMap) readObject(remoteBranchFile,
                HashMap.class);
        if (!remoteBranches.containsKey(remoteBranchName)) {
            System.out.println("That remote does not have that branch.");
            System.exit(0);
        }

        String remoteHead = remoteBranches.get(remoteBranchName);
        String curCommitSha = remoteHead;
        while (curCommitSha != null) {
            File remoteCommitFile = join(remotePath, "commit", curCommitSha);
            Commit remoteCommit = (Commit) readObject(remoteCommitFile, Commit.class);
            File curCommitFile = join(COMMIT_DIR, curCommitSha);
            if (!curCommitFile.exists()) {
                writeObject(curCommitFile, remoteCommit);
                HashMap<String, String> remoteFiles = remoteCommit.getFiles();
                for (String curFileSha : remoteFiles.values()) {
                    File remoteFile = join(remotePath, "blob", curFileSha);
                    File curFile = join(BLOB_DIR, curFileSha);
                    String remoteContent = readContentsAsString(remoteFile);
                    writeContents(curFile, remoteContent);
                }
            }
            curCommitSha = remoteCommit.getParent();
        }

        branches = (HashMap) readObject(BRANCH_FILE, HashMap.class);
        String newBranchName = remoteName + "/" + remoteBranchName;
        branches.put(newBranchName, remoteHead);
        writeObject(BRANCH_FILE, branches);
    }

    public static void pullCommand(String remoteName, String remoteBranchName) {
        fetchCommand(remoteName, remoteBranchName);
        String mergeBranch = remoteName + "/" + remoteBranchName;
        mergeCommand(mergeBranch);
    }
}
