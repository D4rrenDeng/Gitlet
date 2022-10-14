package gitlet;


import java.io.Serializable;
import java.util.*;

/** Represents a gitlet commit object.
 *  does at a high level.
 *
 *  @author Darren Deng
 */
public class Commit implements Serializable {
    /**
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /** The message of this Commit. */
    private String message;
    /** The timestamp of this Commit. */
    private String date;
    /** Parent commit reference */
    private String parent;
    /** Second parent commit reference */
    private String secondParent;
    /** Files contained in this commit */
    private HashMap<String, String> files = new HashMap<>();

    public Commit() {

    }
    public Commit(String message, String date, String parent, String secondParent) {
        this.message = message;
        this.date = date;
        this.parent = parent;
        this.secondParent = secondParent;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    // return the file list with their SHAs
    public HashMap<String, String> getFiles() {
        return files;
    }

    // return the SHA of a file
    public String getFile(String fileName) {
        return files.get(fileName);
    }

    public void putFile(String key, String value) {
        files.put(key, value);
    }

    public boolean containsFile(String fileName) {
        return files.containsKey(fileName);
    }

    public void removeFile(String key) {
        files.remove(key);
    }

    public void setParents(String parent1, String parent2) {
        this.parent = parent1;
        this.secondParent = parent2;
    }

    public void setSecondParent(String parent2) {
        this.secondParent = parent2;
    }

    public String getParent() {
        return parent;
    }

    public String getSecondParent() {
        return secondParent;
    }

    public String getDate() {
        return date;
    }
    // copy constructor without copying the parents' commit
    public Commit copyCommit() {
        Commit newCommit = new Commit();
        newCommit.message = this.message;
        newCommit.date = String.format("%1$ta %1$tb %1$te %1$tH:%1$tM:%1$tS %1$tY %1$tz",
                new Date(System.currentTimeMillis()));
        newCommit.parent = null;
        newCommit.secondParent = null;
        newCommit.files = this.files;
        return newCommit;
    }
}
