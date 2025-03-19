package net.alkarime.bean;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import net.alkarime.entities.UserTU;
import net.alkarime.service.UserService;

import java.io.Serializable;

@Named("userSessionBean")
@SessionScoped
public class UserBean implements Serializable {

    private String username;
    private String password;
    private String confirmPassword;
    private boolean loggedIn = false;

    @Inject
    private UserService userService;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public String register() {
        if (password.equals(confirmPassword)) {
            UserTU user = new UserTU(username, password);
            userService.saveUser(user);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage("Inscription réussie, veuillez vous connecter."));
            return "connexion.xhtml?faces-redirect=true";
        } else {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Erreur", "Les mots de passe ne correspondent pas."));
            return "inscription.xhtml";
        }
    }

    public String login() {
        UserTU user = userService.findUser(username, password);
        if (user != null) {
            loggedIn = true;
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage("Connexion réussie. Bienvenue " + username + "!"));
            return "soumettreCode.xhtml?faces-redirect=true";
        } else {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur de connexion", "Nom d'utilisateur ou mot de passe incorrect."));
            return "connexion.xhtml";
        }
    }

    public String logout() {
        loggedIn = false;
        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage("Vous avez été déconnecté."));
        return "index.xhtml?faces-redirect=true";
    }
}
