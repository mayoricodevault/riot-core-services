package com.tierconnect.riot.commons.utils;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.tools.ant.taskdefs.condition.Os;

import java.io.IOException;

/**
 * Created by achambi on 8/29/16.
 * A class to run a command in linux/osx and windows shell
 */
public class ShellCommandUtil {

    private static Logger logger = Logger.getLogger(ShellCommandUtil.class);
    private static boolean jsonFormatterRetry = true;

    /**
     * Executes the specified string command in a separate process.
     *
     * @param command a specified system command.
     * @return A StringBuffer to contains the command result.
     */
    public static StringBuffer executeCommand(String command) throws InterruptedException, IOException {
        StringBuffer output;
        Process p;
        try {
            String[] cmdline;
            if (Os.isFamily(Os.FAMILY_WINDOWS)) {
                //on windows:
                cmdline = new String[]{"cmd", "/c", command.replaceAll("<GREP>", " | findstr /V \"I NETWORK\"")};
            } else {
                //on linux
                cmdline = new String[]{"sh", "-c", command.replaceAll("<GREP>", " | grep -v \"I NETWORK\"")};
            }
            p = Runtime.getRuntime().exec(cmdline);
            p.waitFor();
            String error = Reader.loadInputStream(p.getErrorStream()).toString();
            if (StringUtils.isNotBlank(error)) {
                logger.error("The function has syntax error.:\n" + error);
                throw new IOException("The function has syntax error.:\n" + error);
            }
            output = Reader.loadInputStream(p.getInputStream());
            if (output.indexOf("Error") != -1) {
                if (output.indexOf("JSONFormatter") != -1 && jsonFormatterRetry){
                    jsonFormatterRetry = false;
                    return executeCommand(command);
                }
                logger.error("The function has errors and cannot be executed:\n" + output);
                throw new IOException("The function has errors and cannot be executed:\n" + output);
            }
        } catch (SecurityException e) {
            logger.error("The Security Manager doesn't allow creation of the subprocess.", e);
            throw new SecurityException("The Security Manager doesn't allow creation of the subprocess.", e);
        } catch (IOException e) {
            logger.error("An error occurred Input/Output", e);
            throw new IOException("An error occurred Input/Output." + e.getMessage(), e);
        } catch (NullPointerException e) {
            logger.error("The command to run is null or one of the elements of command is null.", e);
            throw new NullPointerException("The command to run is null or one of the elements of command is null");
        } catch (IndexOutOfBoundsException e) {
            logger.error("The command to run is empty or length command is 0.", e);
            throw new IndexOutOfBoundsException("The command to run is empty or command length is 0.");
        } catch (InterruptedException e) {
            logger.error("The command to run is empty or length command is 0.", e);
            throw new InterruptedException("The command to run is empty or command length is 0.");
        }
        return output;
    }

}
