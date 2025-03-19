package net.alkarime.service;

import net.alkarime.entities.UserTU;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class UserService {

    @PersistenceContext
    private EntityManager em;

    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public void saveUser(UserTU user) {
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);
        em.persist(user);
    }

    public UserTU findUser(String username, String password) {
        UserTU user = em.createQuery("SELECT u FROM UserTU u WHERE u.username = :username", UserTU.class)
                .setParameter("username", username)
                .getSingleResult();

        if (user != null && passwordEncoder.matches(password, user.getPassword())) {
            return user;
        }
        return null;
    }
}
