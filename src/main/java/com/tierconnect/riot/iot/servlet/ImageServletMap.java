package com.tierconnect.riot.iot.servlet;

import com.tierconnect.riot.iot.entities.LocalMap;
import com.tierconnect.riot.iot.services.LocalMapService;
import com.tierconnect.riot.sdk.dao.HibernateDAOUtils;
import com.tierconnect.riot.sdk.dao.HibernateSessionFactory;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Usage:
 *
 * http://localhost:8080/riot-core-services/imageServletFacility?id={facilityMap.id}
 * @author ezapata
 * 
 */
public class ImageServletMap extends HttpServlet
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Logger logger = Logger.getLogger(ImageServletMap.class);

	ServletConfig config;

	public void init( final ServletConfig config )
	{
		// final String context = config.getServletContext().getRealPath( "/" );
		this.config = config;
	}

	public void doGet( HttpServletRequest req, HttpServletResponse resp ) throws IOException {
		String id = req.getParameter( "id" );
		Session session = HibernateSessionFactory.getInstance().getCurrentSession();
		Transaction transaction = session.getTransaction();
		transaction.begin();
        LocalMap localMap = null;
		try {
			localMap = LocalMapService.getInstance().get(Long.valueOf(id));
			transaction.commit();
		} catch(Exception e) {
			HibernateDAOUtils.rollback(transaction);
		}
        // rsejas TODO: Check if is necessary return a Default image
        if (localMap != null) {
            resp.setContentType("image/png");
            OutputStream o = resp.getOutputStream();
            o.write(localMap.getImage());
            o.flush();
            o.close();
        } else {
            logger.debug("Local Map id: " + id + ", not found");
        }
	}
}
