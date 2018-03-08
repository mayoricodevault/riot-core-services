package com.tierconnect.riot.iot.servlet;

import com.tierconnect.riot.appcore.utils.EncryptionUtils;
import com.tierconnect.riot.appcore.utils.Utilities;
import com.tierconnect.riot.iot.entities.ThingImage;
import com.tierconnect.riot.iot.reports_integration.TranslateResult;
import com.tierconnect.riot.iot.services.ThingImageService;
import com.tierconnect.riot.iot.services.ThingTypeFieldService;
import com.tierconnect.riot.sdk.dao.HibernateDAOUtils;
import com.tierconnect.riot.sdk.dao.HibernateSessionFactory;
import com.tierconnect.riot.sdk.dao.NotFoundException;
import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.Transaction;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * Created by agutierrez on 12/16/2014.
 */
public class ThingImageServlet extends HttpServletBase {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    ServletConfig config;

    public void init(final ServletConfig config) {
        this.config = config;
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String accept = request.getParameter("Accept");
        if (accept == null) {
            accept = request.getHeader("Accept");
        }
        Session session = HibernateSessionFactory.getInstance().getCurrentSession();
        Transaction transaction = session.getTransaction();
        try {
            String id = request.getParameter("id");
            if (id == null) {
                id = request.getPathInfo() == null ? "" : request.getPathInfo().replace("/", "");
            }
            // compatibility
            Long imageId = 0L;
            if (Utilities.isNumber(id)) {
                imageId = Long.valueOf(id);
            } else {
                if (Utilities.isEmptyOrNull(id)) {
                    throw new UserException("Parameter id was not sent");
                }
                if (ThingTypeFieldService.getInstance().isURL(id)) {
                    id = getParameterId(id);
                }
                String decrypt = EncryptionUtils.decrypt(TranslateResult.KEY_ID_IMAGE, TranslateResult.INIT_VECTOR_ID_IMAGE, id);
                if (decrypt == null) {
                    throw new UserException("Parameter id was not sent");
                }
                imageId = Long.valueOf(decrypt);
            }

            transaction.begin();
            ThingImage thingImage = ThingImageService.getInstance().get(imageId);
            if (thingImage == null) {
                throw new NotFoundException("Not Found Image with id " + id);
            }
            if (accept.contains("application/json")) {
                response.setContentType("application/json");
                PrintWriter out = response.getWriter();
                out.println(thingImage.publicMap());
                out.flush();
                out.close();
            } else {
                response.setContentType(thingImage.getContentType());
                OutputStream o = response.getOutputStream();
                o.write(thingImage.getImage());
            }
            transaction.commit();
        } catch (Exception ex) {
            handleException(response, ex);
        } finally {
            HibernateDAOUtils.rollback(transaction);
        }
    }

    private String getParameterId(String urlString) {
        try {
            URL url = new URL(urlString);
            String query = url.getQuery();
            String[] split = query.split("&");
            Map<String, String> params = new HashMap<String, String>();
            for (String param : split) {
                String name = param.split("=")[0];
                String value = param.split("=")[1];
                params.put(name, value);
            }
            urlString = params.get("id");
        } catch (MalformedURLException e) {
            throw new UserException("URL invalid", e);
        }
        return urlString;
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        Map ids = new HashMap();
        Map result = new HashMap();
        PrintWriter out = response.getWriter();

        boolean isMultipartContent = ServletFileUpload.isMultipartContent(request);
        if (!isMultipartContent) {
            result.put("error", "You are not trying to upload");
            out.println(toJson(result));
            return;
        }
        FileItemFactory factory = new DiskFileItemFactory();
        ServletFileUpload upload = new ServletFileUpload(factory);
        List<FileItem> fields = new ArrayList<>();
        try {
            fields = upload.parseRequest(request);
        } catch (FileUploadException e) {
            e.printStackTrace();
        }
        if (fields.size() == 0) {
            result.put("error", "No fields were sent");
            out.println(toJson(result));
            return;
        }
        Session session = HibernateSessionFactory.getInstance().getCurrentSession();
        Transaction transaction = session.getTransaction();
        try {
            transaction.begin();
            Iterator<FileItem> it = fields.iterator();
            while (it.hasNext()) {
                FileItem fileItem = it.next();
                boolean isFormField = fileItem.isFormField();
                if (isFormField) {
                    ids.put(fileItem.getFieldName(), fileItem.getString());
                } else {
                    String fieldName = fileItem.getFieldName();
                    String id = (String) ids.get(fieldName + "_id");
                    ThingImage thingImage;
                    if (StringUtils.isNotEmpty(id)) {
                        thingImage = ThingImageService.getInstance().get(Long.valueOf(id));
                        if (thingImage == null) {
                            throw new NotFoundException("Not Found Image with id " + id);
                        }
                    } else {
                        thingImage = new ThingImage();
                    }
                    thingImage.setContentType(fileItem.getContentType());
                    thingImage.setImage(fileItem.get());
                    thingImage.setFileName(fileItem.getName());
                    if (StringUtils.isNotEmpty(id)) {
                        ThingImageService.getInstance().update(thingImage);
                    } else {
                        ThingImageService.getInstance().insert(thingImage);
                    }
                    result.put(fieldName, thingImage.publicMap());
                }
            }
            transaction.commit();
            out.println(toJson(result));
        } catch (Exception ex) {
            handleException(response, ex);
        } finally {
            HibernateDAOUtils.rollback(transaction);
        }
    }


    //Update One
    @Override
    public void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        Map result = new HashMap();
        boolean isMultipartContent = ServletFileUpload.isMultipartContent(request);
        if (!isMultipartContent) {
            result.put("error", "You are not trying to upload");
            out.println(toJson(result));
            return;
        }
        FileItemFactory factory = new DiskFileItemFactory();
        ServletFileUpload upload = new ServletFileUpload(factory);
        List<FileItem> fields = new ArrayList<>();
        try {
            fields = upload.parseRequest(request);
        } catch (FileUploadException e) {
            e.printStackTrace();
        }
        if (fields.size() == 0) {
            result.put("error", "No multipart/content fields were sent");
            out.println(toJson(result));
            return;
        }
        Session session = HibernateSessionFactory.getInstance().getCurrentSession();
        Transaction transaction = session.getTransaction();
        try {
            String id = request.getParameter("id");
            if (id == null) {
                id = request.getPathInfo() == null ? "" : request.getPathInfo().replace("/", "");
            }
            if (StringUtils.isEmpty(id)) {
                throw new UserException("Parameter id was not sent");
            }
            transaction.begin();
            Iterator<FileItem> it = fields.iterator();
            while (it.hasNext()) {
                FileItem fileItem = it.next();
                boolean isFormField = fileItem.isFormField();
                if (isFormField) {
                    result.put("error", "No multipart/content fields were sent");
                    out.println(toJson(result));
                    return;
                } else {
                    String fieldName = fileItem.getFieldName();
                    ThingImage thingImage;
                    if (StringUtils.isNotEmpty(id)) {
                        thingImage = ThingImageService.getInstance().get(Long.valueOf(id));
                    } else {
                        thingImage = new ThingImage();
                    }
                    thingImage.setContentType(fileItem.getContentType());
                    thingImage.setImage(fileItem.get());
                    thingImage.setFileName(fileItem.getName());
                    if (StringUtils.isNotEmpty(id)) {
                        ThingImageService.getInstance().update(thingImage);
                    } else {
                        ThingImageService.getInstance().insert(thingImage);
                    }
                    result.put(fieldName, thingImage.publicMap());
                }
                break;
            }
            transaction.commit();
            out.println(toJson(result));
        } catch (Exception ex) {
            handleException(response, ex);
        } finally {
            HibernateDAOUtils.rollback(transaction);
        }

    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Session session = HibernateSessionFactory.getInstance().getCurrentSession();
        Transaction transaction = session.getTransaction();
        try {
            String id = request.getParameter("id");
            if (id == null) {
                id = request.getPathInfo() == null ? "" : request.getPathInfo().replace("/", "");
            }
            if (StringUtils.isEmpty(id)) {
                throw new UserException("Parameter id was not sent");
            }

            transaction.begin();
            ThingImageService thingImageService = ThingImageService.getInstance();
            ThingImage thingImage = thingImageService.get(Long.valueOf(id));
            if (thingImage == null) {
                throw new NotFoundException("Not Found Image with id " + id);
            }
            thingImageService.delete(thingImage);
            transaction.commit();
        } catch (Exception ex) {
            handleException(response, ex);
        } finally {
            HibernateDAOUtils.rollback(transaction);
        }
    }

}
