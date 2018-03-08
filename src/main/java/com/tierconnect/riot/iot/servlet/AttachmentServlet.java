package com.tierconnect.riot.iot.servlet;

import com.tierconnect.riot.appcore.utils.EncryptionUtils;
import com.tierconnect.riot.appcore.utils.Utilities;
import com.tierconnect.riot.iot.reports_integration.TranslateResult;
import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.commons.io.FileUtils;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * Created by vealaro on 4/5/17.
 */
@WebServlet("/attachment/download")
public class AttachmentServlet extends HttpServletBase {

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String pathFile = request.getParameter("file");
            if (Utilities.isEmptyOrNull(pathFile)) {
                throw new UserException("Parameter \"file\" was not sent");
            }
            pathFile = EncryptionUtils.decrypt(TranslateResult.KEY_ID_IMAGE, TranslateResult.INIT_VECTOR_ID_IMAGE, pathFile);
            if (pathFile == null) {
                throw new UserException("File not exists");
            }
            File file = new File(pathFile);
            if (!file.exists()) {
                throw new UserException("File not exists");
            }
            String[] splitName = pathFile.split("\\.");
            String extension = splitName[splitName.length - 1];
            String[] splitFileName = pathFile.split("\\/");
            String fileName = splitFileName[splitFileName.length - 1];
            // file
            response.setContentType("application/" + extension);
            response.setContentLength((int) file.length());
            response.setHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", fileName));
            FileUtils.copyFile(file, response.getOutputStream());
        } catch (Exception e) {
            handleException(response, e);
        }
    }
}
