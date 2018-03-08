package com.tierconnect.riot.migration;

import com.tierconnect.riot.appcore.dao.CassandraUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by agutierrez on 6/15/15.
 */
@Deprecated
public class CQLHelper {
    static Logger logger = Logger.getLogger(CQLHelper.class);

    public void executeCQLFile(String file) throws Exception {
        executeSQLScript(toString(file));
    }

    private String toString(String filename) {
        try {
            return IOUtils.toString(CQLHelper.class.getClassLoader().getResource(filename), Charset.forName("UTF-8"));
        } catch (IOException e) {
            return "";
        }
    }

    public void executeSQLScript(String sql) throws Exception {
        List<String> commands = new ArrayList<>();
        try {
            try (BufferedReader br = new BufferedReader(new StringReader(sql))) {
                boolean blockComment = false;
                StringBuilder command = new StringBuilder();
                for (String line; (line = br.readLine()) != null; ) {
                    line = line.trim();
                    if (line.startsWith("/*")) {
                        blockComment = true;
                    }
                    if (blockComment) {
                        if (line.endsWith("*/")) {
                            blockComment = false;
                            continue;
                        }
                    }
                    if (line.startsWith("--") || line.startsWith("//")) {
                        continue;
                    }
                    command.append(line);
                    if (line.endsWith(";")) {
                        commands.add(command.toString());
                        command = new StringBuilder();
                        continue;
                    }
                }
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
            throw new Exception(e);
        }
        for (String command : commands) {
            try {
                logger.info("Executing: " + command);
                CassandraUtils.getSession().execute(CassandraUtils.getSession().prepare(command).bind());
            } catch (Exception e) {
                logger.error("Error executing:" + command + " error:" + e.getMessage());
            }
        }
    }

}
