package net.alkarime.service;


import java.util.*;
import java.util.regex.*;
import javax.enterprise.context.ApplicationScoped;


@ApplicationScoped
public class QualiteAnalyseService { 
    private static final Pattern LOOP_PATTERN = Pattern.compile("for\\s*\\(.*?;.*?;.*?\\)\\s*\\{.*?\\}|while\\s*\\(.*?\\)\\s*\\{.*?\\}", Pattern.DOTALL);
    private static final Pattern INFINITE_LOOP_PATTERN = Pattern.compile("while\\s*\\([^;]*\\)\\s*\\{[^;]*\\}|for\\s*\\([^;]*;\\s*;[^;]*\\)");


    
    public Map<String, String> analyseQualiteCode(String codeContent) {
        Map<String, String> report = new HashMap<>();
        if (codeContent == null || codeContent.trim().isEmpty()) {
            report.put("Erreur", "Le contenu du code est vide ou nul.");
            return report;
        }

        // Analyse statique
        report.put("Cyclomatic Complexity", formatComplexity(calculateCyclomaticComplexity(codeContent)));
        
        // Détection des mauvaises pratiques
        String badPractices = detectBadPractices(codeContent);
        if (!"No bad practices detected.".equals(badPractices)) {
            report.put("Bad Practices", badPractices);
        }

        // Analyse des variables et méthodes inutilisées
        String unusedVars = detectUnusedVariables(codeContent);
        String unusedMethods = detectUnusedMethods(codeContent);
        
        if (!"No unused variables.".equals(unusedVars)) {
            report.put("Unused Variables", unusedVars);
        }
        if (!"No unused methods.".equals(unusedMethods)) {
            report.put("Unused Methods", unusedMethods);
        }

        return report;
    }
        
    public String detectUnusedVariables(String codeContent) {
        Set<String> declaredVariables = new HashSet<>();
        Set<String> usedVariables = new HashSet<>();

        Pattern variablePattern = Pattern.compile("\\b(?:int|String|boolean|double|char|long|float)\\s+([a-zA-Z_][a-zA-Z0-9_]*)");
        Matcher variableMatcher = variablePattern.matcher(codeContent);
        while (variableMatcher.find()) {
            declaredVariables.add(variableMatcher.group(1));
        }

        Pattern usagePattern = Pattern.compile("\\b([a-zA-Z_][a-zA-Z0-9_]*)\\b");
        Matcher usageMatcher = usagePattern.matcher(codeContent);
        while (usageMatcher.find()) {
            String usedVar = usageMatcher.group(1);
            if (declaredVariables.contains(usedVar) && !isKeyword(usedVar)) {
                usedVariables.add(usedVar);
            }
        }

        declaredVariables.removeAll(usedVariables);

        if (declaredVariables.isEmpty()) {
            return "Aucune variable inutilisée.";
        } else {
            return "Variables inutilisées : " + String.join(", ", declaredVariables);
        }
    
    }
    
    public String detectUnusedMethods(String codeContent) {
        Set<String> definedMethods = new HashSet<>();
        Set<String> calledMethods = new HashSet<>();
        
        Pattern methodPattern = Pattern.compile("public\\s+\\w+\\s+(\\w+)\\(.*?\\)");
        Matcher methodMatcher = methodPattern.matcher(codeContent);
        while (methodMatcher.find()) {
            definedMethods.add(methodMatcher.group(1));
        }
        
        Pattern callPattern = Pattern.compile("\\b(\\w+)\\(.*?\\)");
        Matcher callMatcher = callPattern.matcher(codeContent);
        while (callMatcher.find()) {
            calledMethods.add(callMatcher.group(1));
        }
        
        definedMethods.removeAll(calledMethods); 
        
        return definedMethods.isEmpty() ? "Aucune méthode inutilisée." : "Méthodes inutilisées détectées : " + definedMethods;
    }
    
    private String detectBadPractices(String codeContent) {
	    StringBuilder issues = new StringBuilder();
	    
	    if (hasNestedLoops(codeContent)) {
	        issues.append("- Boucles imbriquées détectées.\n");
	    }

	    if (hasDuplicateCode(codeContent)) {
	        issues.append("- Code dupliqué trouvé.\n");
	    }
	    
	    if (codeContent.contains("static") && codeContent.contains("public")) {
	        issues.append("- Usage excessif de variables globales ou statiques.\n");
	    }

	    return issues.length() > 0 ? issues.toString() : "Aucune mauvaise pratique détectée.";
	}
    
    private int calculateCyclomaticComplexity(String codeContent) {
        Pattern pattern = Pattern.compile("\\b(if|for|while|case|catch|&&|\\|\\|)\\b");
        Matcher matcher = pattern.matcher(codeContent);
        int complexity = 1;
        while (matcher.find()) {
            complexity++;
        }
        return complexity;
    }
        
    private String formatComplexity(int complexity) {
        String baseMessage = String.valueOf(complexity);
        if (complexity > 10) {
            return baseMessage + " (Complexité élevée - Considérez une refactorisation)";
        }
        return baseMessage;
    }

    public Map<String, String> analyseStatiqueCode(String codeContent) {
        Map<String, String> staticAnalysisReport = new HashMap<>();

        staticAnalysisReport.put("Linting", detectLintingIssues(codeContent));

        staticAnalysisReport.put("Code Smells", detectCodeSmells(codeContent));

        return staticAnalysisReport;
    }
    
    public Map<String, String> suggestRefactoring(String codeContent) {
        Map<String, String> refactoringSuggestions = new HashMap<>();
        refactoringSuggestions.put("Extraction de méthodes", detectLongMethods(codeContent));
        refactoringSuggestions.put("Réduction de complexité", detectHighCyclomaticComplexity(codeContent));
        return refactoringSuggestions;
    }
   
    public boolean hasPotentialInfiniteLoop(String codeContent) {
        Matcher matcher = INFINITE_LOOP_PATTERN.matcher(codeContent);
        return matcher.find();
    }
    
    public Map<String, String> optimisePerformances(String codeContent) {
        Map<String, String> performanceAnalysis = new HashMap<>();

        if (hasNestedLoops(codeContent)) {
            performanceAnalysis.put("Boucles imbriquées", "Évitez les boucles imbriquées pour améliorer la performance.");
        }

        performanceAnalysis.put("Consommation mémoire", "Utilisez des structures de données efficaces.");
        
        return performanceAnalysis;
    }
    
    private boolean isKeyword(String word) {
    	String[] keywords = {"abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue", 
    	                     "default", "do", "double", "else", "enum", "extends", "final", "finally", "float", "for", "goto", "if", 
    	                     "implements", "import", "instanceof", "int", "interface", "long", "native", "new", "package", "private", 
    	                     "protected", "public", "return", "short", "static", "strictfp", "super", "switch", "synchronized", 
    	                     "this", "throw", "throws", "transient", "try", "void", "volatile", "while"};
    	for (String keyword : keywords) {
    	    if (keyword.equals(word)) {
    	        return true;
    	    }
    	}
    	return false;
    }
    
    private boolean hasNestedLoops(String codeContent) {
    	int nestedLoops = 0;
    	Matcher matcher = LOOP_PATTERN.matcher(codeContent);
    	while (matcher.find()) {
    	    nestedLoops++;
    	}
    	return nestedLoops > 1; 
    	}
    
    private boolean hasDuplicateCode(String codeContent) {
    	String[] lines = codeContent.split("\n");
    	Set<String> uniqueLines = new HashSet<>();
    	int duplicateCount = 0;

    	for (String line : lines) {
    	    line = line.trim();
    	    if (line.isEmpty() || line.equals("{") || line.equals("}") || line.startsWith("import") || line.startsWith("//")) {
    	        continue;
    	    }
    	    if (!uniqueLines.add(line)) {
    	        duplicateCount++;
    	    }
    	}
    	return duplicateCount > 0;
    	}

    
    private String detectLintingIssues(String codeContent) {
    	Pattern camelCasePattern = Pattern.compile("\\b([a-z]+[A-Z][a-z]*)+\\b");
    	Matcher matcher = camelCasePattern.matcher(codeContent);
    	int lintIssues = 0;
    	while (matcher.find()) {
    	    lintIssues++;
    	}
    	return lintIssues > 0 ? "Détection de problèmes de style : " + lintIssues : "Aucun problème de style détecté.";
    	}

    	private String detectCodeSmells(String codeContent) {
    	if (codeContent.length() > 1000) {
    	    return "Code potentiellement trop long, envisagez de diviser.";
    	}
    	return "Aucun code smell détecté.";
    	}
    	
    	private String detectLongMethods(String codeContent) {
    		Pattern methodPattern = Pattern.compile("public .*\\{[^}]+\\}");
    		Matcher matcher = methodPattern.matcher(codeContent);
    		int longMethods = 0;
    		while (matcher.find() && matcher.group().length() > 300) {
    		    longMethods++;
    		}
    		return longMethods > 0 ? "Méthodes longues détectées : " + longMethods : "Aucune méthode longue.";
    		}
    
    	private String detectHighCyclomaticComplexity(String codeContent) {
    		int complexity = calculateCyclomaticComplexity(codeContent);
    		return complexity > 10 ? "Complexité élevée détectée (CC = " + complexity + ")" : "Complexité acceptable.";
    		}
    	       
}