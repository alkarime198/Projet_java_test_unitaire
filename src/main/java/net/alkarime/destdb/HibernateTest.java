package net.alkarime.destdb;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import net.alkarime.entities.SoumettreCode;
import net.alkarime.entities.UserTU;

import java.time.LocalDateTime;

public class HibernateTest {

    public static void main(String[] args) {
        EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("TestUnit");
        EntityManager entityManager = entityManagerFactory.createEntityManager();

        try {
            entityManager.getTransaction().begin();

            UserTU user = new UserTU("modou", "fall");
            entityManager.persist(user);

            SoumettreCode soumission = new SoumettreCode(
                    "System.out.println(\"Hello World!\");",
                    LocalDateTime.now(),
                    "Success",
                    150,
                    512,
                    85,
                    user
            );

            entityManager.persist(soumission);
            entityManager.getTransaction().commit();

            System.out.println("Données insérées avec succès :");
            System.out.println("Utilisateur : " + user);
            System.out.println("Soumission de code : " + soumission);

        } catch (Exception e) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            e.printStackTrace();
        } finally {
            entityManager.close();
            entityManagerFactory.close();
        }
    }
}
