package com.tierconnect.riot.dao;

import com.tierconnect.riot.appcore.entities.QUser;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.sdk.dao.GenericDAOHibernateImp;
import com.tierconnect.riot.sdk.dao.HibernateSessionFactory;

import org.apache.log4j.Logger;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.Assert;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by agutierrez on 16-04-14.
 */
public class HibernateTest {
    static Logger logger = Logger.getLogger(HibernateTest.class);

    public static void main(String[] args) {
        SessionFactory factory = HibernateSessionFactory.getInstance();

        GenericDAOHibernateImp<User, Long> genericDAO = new GenericDAOHibernateImp<User, Long>();
        genericDAO.setSessionFactory(factory);
        Transaction t =  factory.getCurrentSession().beginTransaction();
        User user = new User();
        String username = "aldo_gg" + System.currentTimeMillis();
        user.setUsername(username);
        genericDAO.insert(user);
        t.commit();
        Transaction t2 =  factory.getCurrentSession().beginTransaction();
        User userX;
        userX = genericDAO.selectById(User.class, user.getId());
        Assert.assertEquals(user.getUsername(), userX.getUsername());
        Assert.assertEquals(user.getId(), userX.getId());
        userX= genericDAO.selectBy(User.class, "username", username);
        Assert.assertEquals(user.getUsername(), userX.getUsername());
        Assert.assertEquals(user.getId(), userX.getId());
        userX= genericDAO.selectAllBy(User.class, "username", username).get(0);
        List<User> userList = genericDAO.selectAll(User.class);
        Assert.assertEquals(user.getUsername(), userX.getUsername());
        Assert.assertEquals(user.getId(), userX.getId());
        Map filter = new HashMap();
        filter.put("username", username);
        userX = genericDAO.selectBy(User.class, filter);
        Assert.assertEquals(user.getUsername(), userX.getUsername());
        Assert.assertEquals(user.getId(), userX.getId());
        userX = (User) genericDAO.selectAllBy(User.class, filter).get(0);
        Assert.assertEquals(user.getUsername(), userX.getUsername());
        Assert.assertEquals(user.getId(), userX.getId());
        String username2 = ((User) genericDAO.selectAllBy(User.class, filter).get(0)).getUsername();
        System.out.println("username2 = " + username2);

        userX = genericDAO.selectBy(QUser.user, QUser.user.username.eq(username));
        Assert.assertEquals(user.getUsername(), userX.getUsername());
        Assert.assertEquals(user.getId(), userX.getId());
        userX = genericDAO.selectAllBy(QUser.user, QUser.user.username.eq(username)).get(0);
        Assert.assertEquals(user.getUsername(), userX.getUsername());
        Assert.assertEquals(user.getId(), userX.getId());

        userX =  genericDAO.getQuery(QUser.user).where(QUser.user.username.eq(username)).uniqueResult(QUser.user);
        Assert.assertEquals(user.getUsername(), userX.getUsername());
        Assert.assertEquals(user.getId(), userX.getId());

        t2.commit();
        logger.info("OK");
        System.exit(0);
    }
}
