package net.alkarime.bean;

import javax.inject.Named;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.io.Serializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.alkarime.entities.SoumettreCode;
import net.alkarime.entities.UserTU;
import net.alkarime.service.CompilationService;
import net.alkarime.service.SoumissionService;
import net.alkarime.service.UserService;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

@Named("codeSubmissionBean")
@RequestScoped
public class SoumettreCodeBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(SoumettreCodeBean.class);

    private String code;
    private String resultat;
    private long tempsExecuter;
    private String errorMessage;
    private String userInput;

    private String codeFilePath;
   // private SoumettreCode soumettreCode;
    private long memoryUsed;
    
    @Inject
    private SoumissionService soumissionService;
    
    @Inject
    private CompilationService compilationService;

    @Inject
    private UserBean userBean;
    
    @Inject
    private UserService userService;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getResultat() {
        return resultat;
    }

    public void setResultat(String resultat) {
        this.resultat = resultat;
    }

    public long getTempsExecuter() {
        return tempsExecuter;
    }

    public void setTempsExecuter(long tempsExecuter) {
        this.tempsExecuter = tempsExecuter;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getUserInput() {
        return userInput;
    }

    public void setUserInput(String userInput) {
        this.userInput = userInput;
    }
   
    public boolean isLoggedIn() {
        return userBean.isLoggedIn();
    }
    
    public long getMemoryUsed() {
        return memoryUsed;
    }

    public void setMemoryUsed(long memoryUsed) {
        this.memoryUsed = memoryUsed;
    }

    public void submitCode() {
        if (!userBean.isLoggedIn()) {
            errorMessage = "Vous devez être connecté pour soumettre du code.";
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, errorMessage, null));
            return;
        }

        try {
            if (code == null || code.isEmpty()) {
                throw new IllegalArgumentException("Le code soumis ne peut pas être vide !");
            }
            if (code.length() > 2000) {
                throw new IllegalArgumentException("Le code soumis est trop long (max 2000 caractères) !");
            }

            UserTU currentUser = userService.findUser(userBean.getUsername(), userBean.getPassword());

            codeFilePath = soumissionService.saveCodeToFile(code, currentUser.getUsername());
            logger.info("Code sauvegardé dans le fichier : " + codeFilePath);

            long memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

            String nomClasse = CompilationService.extractClassName(code);
            SoumettreCode submissionResult = compilationService.compileAndRun(codeFilePath, nomClasse, userInput);

            long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            this.memoryUsed = memoryAfter - memoryBefore;

            this.resultat = submissionResult.getResultatExecuter();
            this.tempsExecuter = submissionResult.getTempsExecuter();

            logger.info("Code soumis avec succès : " + resultat);
        } catch (Exception e) {
            logger.error("Erreur lors de la soumission du code : " + e.getMessage());
            errorMessage = "Erreur : " + e.getMessage();
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, errorMessage, null));
        }
    }
}
