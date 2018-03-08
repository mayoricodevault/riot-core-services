package com.tierconnect.riot.iot.servlet;


import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.GroupResources;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.utils.BlobUtils;
import com.tierconnect.riot.sdk.dao.HibernateDAOUtils;
import com.tierconnect.riot.sdk.dao.HibernateSessionFactory;
import org.hibernate.Session;
import org.hibernate.Transaction;

import javax.imageio.ImageIO;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Servlet that returns an group image depending on the parameter id
 * @author pablo
 */
public class GroupImageServlet extends HttpServlet
{
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    ServletConfig config;

    public void init(final ServletConfig config)
    {
        // final String context = config.getServletContext().getRealPath( "/" );
        this.config = config;
    }

    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
    {
        Session session = HibernateSessionFactory.getInstance().getCurrentSession();
        Transaction transaction = session.getTransaction();
        try {
            transaction.begin();
            String groupCode = req.getParameter("id");
            //check for the small image parameter
            boolean isTemplate = req.getParameter("isTemplate") != null;
            boolean lastUpload = req.getParameter("lastUpload") != null;

            resp.setContentType("image/jpeg");

            String pathToWeb = config.getServletContext().getRealPath("/") + File.separator + "images-group" + File.separator;

            InputStream is = null;

            if (isTemplate && groupCode != null) {
                is = new FileInputStream(BlobUtils.getFileImage(pathToWeb, groupCode + ".png"));
            }else if (groupCode != null){
                Group group = GroupService.getInstance().getByCode(groupCode);
                if (group != null) {
                    List<GroupResources> groupResourcesList = new ArrayList<>(group.getGroupResources());
                    if (groupResourcesList.size() > 0) {
                        if (groupResourcesList.get(0).getImageTemplateName() != null && !lastUpload) {
                            is = new FileInputStream(BlobUtils.getFileImage(pathToWeb, groupResourcesList.get(0).getImageTemplateName() + ".png"));
                        }else if (groupResourcesList.get(0).getImageIcon() != null) {
                            byte[] blob = groupResourcesList.get(0).getImageIcon();
                            is = new ByteArrayInputStream(blob);
                        }
                    }
                }
            }
            if (is == null && !lastUpload) {
                is = new FileInputStream(BlobUtils.getFileImage(pathToWeb, "unknown.png"));
            }

            BufferedImage bi = ImageIO.read(is);

            OutputStream out = resp.getOutputStream();
            ImageIO.write(bi, "png", out);
            out.close();
            transaction.commit();
        } catch (Exception ex) {
            ex.printStackTrace();
            HibernateDAOUtils.rollback(transaction);
        }
    }

}
