package gitlet;

import java.io.File;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author Darren Deng
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        // TODO: what if args is empty?
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }
        String firstArg = args[0];

        switch(firstArg) {
            case "init":
                validateNumArgs(args, 1);
                Repository.initCommand();
                break;
            case "add":
                validateGitletDir();
                validateNumArgs(args, 2);
                Repository.addCommand(args[1]);
                break;
            case "commit":
                validateGitletDir();
                validateNumArgs(args, 2);
                Repository.commitCommand(args[1]);
                break;
            case "rm":
                validateGitletDir();
                validateNumArgs(args, 2);
                Repository.removeCommand(args[1]);
                break;
            case "log":
                validateGitletDir();
                validateNumArgs(args, 1);
                Repository.logCommand();
                break;
            case "global-log":
                validateGitletDir();
                validateNumArgs(args, 1);
                Repository.globalLogCommand();
                break;
            case "find":
                validateGitletDir();
                validateNumArgs(args, 2);
                Repository.findCommand(args[1]);
                break;
            case "status":
                validateGitletDir();
                validateNumArgs(args, 1);
                Repository.statusCommand();
                break;
            case "checkout":
                validateGitletDir();
                if (args.length == 2) {
                    Repository.checkoutBranchCommand(args[1]);
                } else if (args.length == 3) {
                    if (!args[1].equals("--")) {
                        System.out.println("Incorrect operands.");
                        System.exit(0);
                    }
                    Repository.checkoutFileCommand(args[2]);
                } else if (args.length == 4) {
                    if (!args[2].equals("--")) {
                        System.out.println("Incorrect operands.");
                        System.exit(0);
                    }
                    Repository.checkoutFileCommand(args[1], args[3]);
                } else {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                break;
            case "branch":
                validateGitletDir();
                validateNumArgs(args, 2);
                Repository.branchCommand(args[1]);
                break;
            case "rm-branch":
                validateGitletDir();
                validateNumArgs(args, 2);
                Repository.removeBranchCommand(args[1]);
                break;
            case "reset":
                validateGitletDir();
                validateNumArgs(args, 2);
                Repository.resetCommand(args[1]);
                break;
            case "merge":
                validateGitletDir();
                validateNumArgs(args, 2);
                Repository.mergeCommand(args[1]);
                break;
            case "add-remote":
                validateGitletDir();
                validateNumArgs(args, 3);
                Repository.addRemoteCommand(args[1], args[2]);
                break;
            case "rm-remote":
                validateGitletDir();
                validateNumArgs(args, 2);
                Repository.rmRemoteCommand(args[1]);
                break;
            case "push":
                validateGitletDir();
                validateNumArgs(args, 3);
                Repository.pushCommand(args[1], args[2]);
                break;
            case "fetch":
                validateGitletDir();
                validateNumArgs(args, 3);
                Repository.fetchCommand(args[1], args[2]);
                break;
            case "pull":
                validateGitletDir();
                validateNumArgs(args, 3);
                Repository.pullCommand(args[1], args[2]);
                break;
            default:
                System.out.print("No command with that name exists.");
                System.exit(0);
        }
    }

    public static void validateNumArgs(String[] var, int num) {
        if (var.length != num) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
    }

    public static void validateGitletDir() {
        File CWD = new File(System.getProperty("user.dir"));
        File GITLET_DIR = Utils.join(CWD, ".gitlet");
        if (!GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
    }
}
