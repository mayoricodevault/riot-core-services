package com.tierconnect.riot.dao;

import com.tierconnect.riot.appcore.entities.QUser;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.sdk.dao.GenericDAOJPAImp;

import org.junit.Assert;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by agutierrez on 15-04-14.
 */
public class JPATest {

    public static void main (String[] args) {
        Map properties = new HashMap();
        properties.put("javax.persistence.jdbc.driver", "org.gjt.mm.mysql.Driver");
        properties.put("javax.persistence.jdbc.url", "jdbc:mysql://localhost:3306/riot_main");
        properties.put("javax.persistence.jdbc.user", "root");
        properties.put("javax.persistence.jdbc.password", "control123!");
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("Appcore/NG", properties);
        EntityManager entityManager = emf.createEntityManager();
        GenericDAOJPAImp<User, Long> genericDAO = new GenericDAOJPAImp<User, Long>();
        genericDAO.setEntityManager(entityManager);
        entityManager.getTransaction().begin();
        User user = new User();
        String username = "aldo_gg" + System.currentTimeMillis();
        user.setUsername(username);
        genericDAO.insert(user);
        entityManager.getTransaction().commit();
        User userX;
        userX = genericDAO.selectById(User.class, user.getId());
        Assert.assertEquals(user.getUsername(), userX.getUsername());
        Assert.assertEquals(user.getId(), userX.getId());
        userX= genericDAO.selectBy(User.class, "username", username);
        Assert.assertEquals(user.getUsername(), userX.getUsername());
        Assert.assertEquals(user.getId(), userX.getId());
        userX= genericDAO.selectAllBy(User.class, "username", username).get(0);
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
        userX = genericDAO.selectBy(QUser.user, QUser.user.username.eq(username));
        Assert.assertEquals(user.getUsername(), userX.getUsername());
        Assert.assertEquals(user.getId(), userX.getId());
        userX = genericDAO.selectAllBy(QUser.user, QUser.user.username.eq(username)).get(0);
        Assert.assertEquals(user.getUsername(), userX.getUsername());
        Assert.assertEquals(user.getId(), userX.getId());

        userX =  genericDAO.getQuery(QUser.user).where(QUser.user.username.eq(username)).uniqueResult(QUser.user);
        Assert.assertEquals(user.getUsername(), userX.getUsername());
        Assert.assertEquals(user.getId(), userX.getId());

        System.out.println("Ok");
        System.exit(0);

    }
}
