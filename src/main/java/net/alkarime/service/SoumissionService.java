package net.alkarime.service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.alkarime.entities.SoumettreCode;
import net.alkarime.entities.UserTU;

@Stateless
public class SoumissionService {
	@PersistenceContext(unitName = "TestUnit")
    private EntityManager em;
	
	@EJB
    private CompilationService compilationService;
	
    private static final Logger logger = LoggerFactory.getLogger(SoumissionService.class);
     
    private static final String BASE_TEMP_DIR = "codes";
    private static final long MAX_FILE_AGE_DAYS = 7;


	public void enregistrerResultat(SoumettreCode soumettreCode) {
        if (soumettreCode == null || soumettreCode.getResultatExecuter() == null || soumettreCode.getResultatExecuter().isEmpty()) {
            throw new IllegalArgumentException("Le résultat ne peut pas être vide.");
        }

        logger.info("Code à enregistrer : " + soumettreCode.getCodeFilePath());

        if (soumettreCode.getCodeFilePath() == null || soumettreCode.getCodeFilePath().trim().isEmpty()) {
            logger.error("Le chemin du fichier de code soumis est vide !");
            throw new IllegalArgumentException("Le chemin du fichier ne peut pas être nul ou vide.");
        }

        if (soumettreCode.getDateSoumission() == null) {
            soumettreCode.setDateSoumission(LocalDateTime.now());
        }

        em.merge(soumettreCode);
        logger.info("Soumission de code par l'utilisateur: " + soumettreCode.getUser().getUsername());
    }

	public String saveCodeToFile(String code, String username) {
	    try {
	        File tempDir = new File(BASE_TEMP_DIR);
	        if (!tempDir.exists()) {
	            tempDir.mkdirs();
	        }
	        
	        // Nettoyage périodique des anciens fichiers
	        cleanOldFiles(tempDir);
	        
	        // Utilisation d'un format de nom de fichier plus structuré
	        String fileName = String.format("%s_%s_%d.java",
	            username,
	            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")),
	            System.nanoTime()
	        );
	        
	        File codeFile = new File(tempDir, fileName);
	        Files.write(codeFile.toPath(), code.getBytes(StandardCharsets.UTF_8));
	        return codeFile.getAbsolutePath();
	    } catch (Exception e) {
	        logger.error("Erreur lors de la sauvegarde du code: {}", e.getMessage(), e);
	        throw new RuntimeException("Erreur lors de la sauvegarde du code dans un fichier.", e);
	    }
	  }

	private void cleanOldFiles(File directory) {
	    if (!directory.exists()) return;
	    
	    LocalDateTime cutoffDate = LocalDateTime.now().minusDays(MAX_FILE_AGE_DAYS);
	    File[] files = directory.listFiles();
	    if (files != null) {
	        for (File file : files) {
	            if (file.isFile() && file.lastModified() < cutoffDate.toInstant(ZoneOffset.UTC).toEpochMilli()) {
	                boolean deleted = file.delete();
	                if (!deleted) {
	                    logger.warn("Impossible de supprimer le fichier: {}", file.getAbsolutePath());
	                }
	            }
	        }
	    }
	}
  
	 
}
