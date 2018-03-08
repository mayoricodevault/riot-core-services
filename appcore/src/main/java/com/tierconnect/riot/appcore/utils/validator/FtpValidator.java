package com.tierconnect.riot.appcore.utils.validator;

import com.tierconnect.riot.appcore.entities.Connection;
import com.tierconnect.riot.appcore.entities.ConnectionType;
import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;


public class FtpValidator implements ConnectionValidator {
    private int status;
    private String cause;

    @Override public int getStatus() {
        return status;
    }

    @Override public String getCause() {
        return cause;
    }

    @Override public boolean testConnection(ConnectionType connectionType, String properties) {
        JSONParser parser = new JSONParser();
        FTPClient ftpClient = null;
        try {
            JSONObject ftpProperties = (JSONObject) parser.parse(properties);
            boolean secure = Boolean.parseBoolean(ftpProperties.get("secure").toString());
            if (secure){
                ftpClient = new FTPSClient();
                ftpClient.setConnectTimeout(15000);
                ftpClient.connect(ftpProperties.get("host").toString());
                if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
                    status = ftpClient.getReplyCode();
                    cause = ftpClient.getReplyString();
                    return false;
                }
                boolean loggedIn = ftpClient.login(ftpProperties.get("username").toString(),
                    Connection.decode(ftpProperties.get("password").toString()));
                if (!loggedIn) {
                    status = ftpClient.getReplyCode();
                    cause = ftpClient.getReplyString();
                    return false;
                }
            }else {
                ftpClient = new FTPClient();
                ftpClient.setConnectTimeout(15000);
                ftpClient.connect(ftpProperties.get("host").toString(),
                    Integer.parseInt(ftpProperties.get("port").toString()));
                if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
                    status = ftpClient.getReplyCode();
                    cause = ftpClient.getReplyString();
                    return false;
                }
                boolean loggedIn = ftpClient.login(ftpProperties.get("username").toString(),
                    Connection.decode(ftpProperties.get("password").toString()));
                if (!loggedIn) {
                    status = ftpClient.getReplyCode();
                    cause = ftpClient.getReplyString();
                    return false;
                }
            }
            status = 200;
            cause = "Succeed";
            return true;
        } catch (ConnectException | UnknownHostException e) {
            status = 403;
            cause = e.getMessage();
            return false;
        } catch (SSLException e) {
            status = 500;
            cause = e.getMessage();
            return false;
        } catch (Exception e) {
            status = 400;
            cause = e.getMessage();
            return false;
        } finally {
            if (ftpClient != null && ftpClient.isConnected()) {
                try {
                    ftpClient.logout();
                    ftpClient.disconnect();
                } catch (IOException e) {
                    throw new UserException(e.getMessage());
                }
            }
        }
    }
}
