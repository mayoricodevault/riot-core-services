package com.tierconnect.riot.dao;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.log4j.Logger;

//import com.tierconnect.riot.examples.vetclinic.entities.Dog;
//import com.tierconnect.riot.examples.vetclinic.entities.Flea;

/**
 * 
 * @author tcrown
 *
 */
public class HibernateTest2 
{
    static Logger logger = Logger.getLogger(HibernateTest2.class);

    public static void main(String[] args) 
    {
    	Map<String, String> properties = new HashMap<String, String>();
    	
        //properties.put( "javax.persistence.jdbc.driver", "org.gjt.mm.mysql.Driver" );
        properties.put( "javax.persistence.jdbc.url", "jdbc:mysql://localhost:3306/terry_test" );
        //properties.put( "javax.persistence.jdbc.user", "root" );
        //properties.put( "javax.persistence.jdbc.password", "control123!" );
        properties.put( "hibernate.show_sql", "true" );
        properties.put( "hibernate.hbm2ddl.auto", "update" );
        
        EntityManagerFactory emf = Persistence.createEntityManagerFactory( "riot", properties );
        
        EntityManager em = emf.createEntityManager();
        
        em.getTransaction().begin();
        
//        Dog d = new Dog();
//        d.setName( "pig" );
//        d.setFleas( new LinkedList<Flea>() );
//        
//        for( int i = 0; i < 3; i++ )
//        {
//        	Flea flea = new Flea();
//        	flea.setName( "" + i );
//        	d.getFleas().add( flea );
//        	flea.setDog( d );
//        	//em.persist( flea );
//        }
//        em.persist( d );
//        
//        em.flush();
//        
//        em.getTransaction().commit();
//        
//        Dog dog = em.find( Dog.class, 1L );
//        
//        System.out.println( "********************** dog=" + dog );
//        for( Flea flea : dog.getFleas()  )
//        {
//        	 System.out.println( "********************** flea.id=" + flea.getId() );
//        }
        
        System.exit( 0 );
    }
}
