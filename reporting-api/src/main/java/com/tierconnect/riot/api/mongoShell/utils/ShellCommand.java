package com.tierconnect.riot.api.mongoShell.utils;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.tools.ant.taskdefs.condition.Os;

import java.io.IOException;

/**
 * Created by achambi on 8/29/16.
 * A class to run a command in linux shell
 */
public class ShellCommand {

    public ShellCommand() {
    }

    /**
     * Logger to print in console errors, warnings and information.
     */
    private static Logger logger = Logger.getLogger(ShellCommand.class);

    /**
     * @param command the command string to run in shell.
     * @return {@link String} return the result command if the command not create a temp file.
     * @throws InterruptedException if the current thread is
     *                              {@linkplain Thread#interrupt() interrupted} by another
     *                              thread while it is waiting, then the wait is ended and
     *                              an {@link InterruptedException} is thrown.
     * @throws IOException          if current command contains error in input or output values.
     */
    public String executeCommand(String command) throws InterruptedException,
            IOException {
        return executeCommand(command, false, 0);
    }

    /**
     * @param command           the command string to run in shell.
     * @param ignoreErrorStream if this value is true it ignores the error stream.
     * @return {@link String} return the result command if the command not create a temp file.
     * @throws InterruptedException if the current thread is
     *                              {@linkplain Thread#interrupt() interrupted} by another
     *                              thread while it is waiting, then the wait is ended and
     *                              an {@link InterruptedException} is thrown.
     * @throws IOException          if current command contains error in input or output values.
     */
    public String executeCommand(String command, boolean ignoreErrorStream) throws
            IOException, SecurityException, NullPointerException, IndexOutOfBoundsException, InterruptedException {
        return executeCommand(command, ignoreErrorStream, 0);
    }

    /**
     * @param command           the command string to run in shell.
     * @param ignoreErrorStream if this value is true it ignores the error stream.
     * @param totalRetries      the number of intents to execute the command.
     * @return {@link String} return the result command if the command not create a temp file.
     * @throws InterruptedException if the current thread is
     *                              {@linkplain Thread#interrupt() interrupted} by another
     *                              thread while it is waiting, then the wait is ended and
     *                              an {@link InterruptedException} is thrown.
     * @throws IOException          if current command contains error in input or output values.
     */
    public String executeCommand(String command, boolean ignoreErrorStream, int totalRetries) throws
            IOException, SecurityException, NullPointerException, IndexOutOfBoundsException, InterruptedException {
        StringBuffer output;
        Process p;
        try {
            String[] cmdline;
            if (Os.isFamily(Os.FAMILY_WINDOWS)) {
                //on windows:
                cmdline = new String[]{"cmd", "/c", command};
            } else {
                //on linux
                cmdline = new String[]{"sh", "-c", command};
            }
            p = Runtime.getRuntime().exec(cmdline);
            p.waitFor();
            String error = FileUtils.loadInputStream(p.getErrorStream()).toString();
            if (p.exitValue() == 126 && totalRetries <= 10) { //retry, because write operation/execution permission is not finished
                int nextRetry = totalRetries + 1;
                logger.info("Retry File Execution because it's busy: Retry = " + nextRetry);
                return executeCommand(command, ignoreErrorStream, nextRetry);
            }
            if (StringUtils.isNotBlank(error) && !ignoreErrorStream) {
                logger.error("The function has errors and cannot be executed:\n" + error);
                throw new IOException("The function has syntax error.:\n" + error);
            }
            output = FileUtils.loadInputStream(p.getInputStream());
            if (output.indexOf("Error") != -1) {
                logger.error("The function has errors and cannot be executed:\n" + output);
                throw new IOException("The command returned error and can not be executed.:" + output);
            }
        } catch (SecurityException e) {
            logger.error("The Security Manager doesn't allow creation of the subprocess.", e);
            throw new SecurityException("The Security Manager doesn't allow creation of the subprocess.", e);
        } catch (IOException e) {
            logger.error("An error occurred Input/Output", e);
            throw new IOException("An error occurred Input/Output." + e.toString(), e);
        } catch (NullPointerException e) {
            logger.error("The command to run is null or one of the elements of command is null.", e);
            throw new NullPointerException("The command to run is null or one of the elements of command is null");
        } catch (IndexOutOfBoundsException e) {
            logger.error("The command to run is empty or length command is 0.", e);
            throw new IndexOutOfBoundsException("The command to run is empty or command length is 0.");
        }
        return output.toString();
    }
}
