package com.ucv.database;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class HibernateUtil {
    private static final SessionFactory sessionFactory = buildSessionFactory();

    private HibernateUtil() {
        // TODO document why this constructor is empty
    }

    private static SessionFactory buildSessionFactory() {
        try {

            Configuration configuration = new Configuration().configure("hibernate.cfg.xml");
            return configuration.buildSessionFactory();
        } catch (Exception ex) {
            System.err.println("Initializarea SessionFactory a eșuat." + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }
    public static Session getCurrentSession(){
        return sessionFactory.openSession();
    }
    public static void closeSession(){
     getCurrentSession().close();
    }
    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }
}
