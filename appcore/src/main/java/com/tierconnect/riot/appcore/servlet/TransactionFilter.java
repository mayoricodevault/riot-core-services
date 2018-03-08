package com.tierconnect.riot.appcore.servlet;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import com.tierconnect.riot.sdk.dao.HibernateDAOUtils;
import org.hibernate.Session;
import org.hibernate.Transaction;

import com.tierconnect.riot.sdk.dao.HibernateSessionFactory;

public class TransactionFilter implements javax.servlet.Filter {

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// TODO Auto-generated method stub

	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		Session session = HibernateSessionFactory.getInstance()
				.getCurrentSession();
		Transaction transaction = session.getTransaction();
		
		if(transaction.isActive() == false){
			transaction.begin();
		}
		try {
			chain.doFilter(request, response);
			int status = ((HttpServletResponse) response).getStatus();
			if (status >= 200 && status <= 299) {
				transaction.commit();
			} else if (status >= 400 && status <= 599 && transaction.isActive()) {
				HibernateDAOUtils.rollback(transaction);
			}
		} catch (IOException | ServletException e) {
			HibernateDAOUtils.rollback(transaction);
			throw e;
		}
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub

	}

}
