package com.checkers.hibernate.util;

import com.checkers.model.game.Game;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class ReaderDB {
    private SessionFactory sessionFactory;


    public ReaderDB() {
        sessionFactory = new Configuration().configure().buildSessionFactory();
    }

    public synchronized Game load(int gameId) {
        Session session = sessionFactory.openSession();
        return session.get(Game.class, gameId);
    }

}
