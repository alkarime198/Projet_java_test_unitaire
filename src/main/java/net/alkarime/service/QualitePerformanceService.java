package net.alkarime.service;

import javax.ejb.Stateless;
import javax.inject.Inject;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.*;

@Stateless
public class QualitePerformanceService {
    @Inject  // Ajoutez cette annotation
    private QualiteAnalyseService analyseService;
    
    private static final Pattern METHOD_PATTERN = Pattern.compile("\\b(?:public|private|protected)\\s+(?:[\\w<>\\[\\]]+\\s+)?([\\w]+)\\s*\\([^)]*\\)\\s*\\{");
    private static final Pattern COMPLEXITY_PATTERN = Pattern.compile("\\b(if|for|while|case|&&|\\|\\||\\?|catch)\\b");
    
    // Ajoutez le constructeur par défaut requis pour un EJB
    public QualitePerformanceService() {
    }

    // Vous pouvez garder ce constructeur si nécessaire, mais il ne sera pas utilisé par l'injection EJB
    public QualitePerformanceService(QualiteAnalyseService analyseService) {
        this.analyseService = analyseService;
    }

    // Nouvelles méthodes d'analyse de performance
    public Map<String, Object> analyserPerformance(String code) {
        Map<String, Object> performanceMetrics = new HashMap<>();
        
        // Analyse de la complexité algorithmique
        performanceMetrics.put("complexiteAlgorithmique", analyserComplexiteAlgorithmique(code));
        
        // Analyse de l'utilisation de la mémoire
        performanceMetrics.put("utilisationMemoire", analyserUtilisationMemoire(code));
        
        // Détection des goulots d'étranglement
        performanceMetrics.put("goulotsEtranglement", detecterGoulotsEtranglement(code));
        
        // Analyse des structures de données
        performanceMetrics.put("structuresDonnees", analyserStructuresDonnees(code));
        
        return performanceMetrics;
    }

    private Map<String, String> analyserComplexiteAlgorithmique(String code) {
        Map<String, String> complexityAnalysis = new HashMap<>();
        
        // Analyse de la complexité temporelle
        List<String> methodsWithComplexity = new ArrayList<>();
        Matcher methodMatcher = METHOD_PATTERN.matcher(code);
        
        while (methodMatcher.find()) {
            String methodName = methodMatcher.group(1);
            int complexity = calculateMethodComplexity(code.substring(methodMatcher.start()));
            String complexityClass = getComplexityClass(complexity);
            methodsWithComplexity.add(methodName + ": " + complexityClass);
        }
        
        complexityAnalysis.put("complexiteTemporelle", String.join("\n", methodsWithComplexity));
        
        // Analyse des boucles imbriquées
        int nestedLoops = countNestedLoops(code);
        complexityAnalysis.put("bouclesImbriquees", 
            nestedLoops > 2 ? "⚠️ Attention: " + nestedLoops + " niveaux de boucles imbriquées détectés" :
            nestedLoops + " niveaux de boucles imbriquées");
        
        return complexityAnalysis;
    }

    private Map<String, String> analyserUtilisationMemoire(String code) {
        Map<String, String> memoryAnalysis = new HashMap<>();
        
        // Analyse des allocations de mémoire
        Pattern arrayPattern = Pattern.compile("new\\s+[\\w<>]+\\s*\\[(.*?)\\]");
        Matcher arrayMatcher = arrayPattern.matcher(code);
        List<String> largeAllocations = new ArrayList<>();
        
        while (arrayMatcher.find()) {
            String size = arrayMatcher.group(1);
            if (size.matches("\\d+") && Integer.parseInt(size) > 1000000) {
                largeAllocations.add("Grande allocation détectée: " + size + " éléments");
            }
        }
        
        memoryAnalysis.put("allocationsImportantes", 
            largeAllocations.isEmpty() ? "Aucune allocation importante détectée" : 
            String.join("\n", largeAllocations));
        
        // Analyse des fuites potentielles
        List<String> potentialLeaks = detecterFuitesMemoire(code);
        memoryAnalysis.put("fuitesPotentielles", 
            potentialLeaks.isEmpty() ? "Aucune fuite potentielle détectée" :
            String.join("\n", potentialLeaks));
            
        return memoryAnalysis;
    }

    private List<String> detecterGoulotsEtranglement(String code) {
        List<String> bottlenecks = new ArrayList<>();
        
        // Détection des opérations coûteuses dans les boucles
        Pattern loopPattern = Pattern.compile("(for|while)\\s*\\([^{]+\\{([^}]+)\\}");
        Matcher loopMatcher = loopPattern.matcher(code);
        
        while (loopMatcher.find()) {
            String loopBody = loopMatcher.group(2);
            
            // Vérifier les opérations coûteuses
            if (loopBody.contains("new ")) {
                bottlenecks.add("⚠️ Allocation d'objets dans une boucle détectée");
            }
            if (loopBody.contains(".toString()") || loopBody.contains("String.format")) {
                bottlenecks.add("⚠️ Opérations sur les chaînes dans une boucle détectée");
            }
            if (loopBody.contains(".contains(") || loopBody.contains(".indexOf(")) {
                bottlenecks.add("⚠️ Recherche linéaire dans une boucle détectée");
            }
        }
        
        return bottlenecks;
    }

    private Map<String, String> analyserStructuresDonnees(String code) {
        Map<String, String> dataStructureAnalysis = new HashMap<>();
        
        // Analyse des collections utilisées
        Map<String, Integer> collectionUsage = new HashMap<>();
        Pattern collectionPattern = Pattern.compile("(?:ArrayList|LinkedList|HashSet|TreeSet|HashMap|TreeMap)<[^>]+>");
        Matcher collectionMatcher = collectionPattern.matcher(code);
        
        while (collectionMatcher.find()) {
            String collection = collectionMatcher.group();
            collectionUsage.merge(collection, 1, Integer::sum);
        }
        
        // Recommandations pour les structures de données
        List<String> recommendations = new ArrayList<>();
        collectionUsage.forEach((collection, count) -> {
            if (collection.contains("ArrayList") && code.contains(".get(") && code.contains(".remove(")) {
                recommendations.add("⚠️ Considérez utiliser LinkedList pour les opérations fréquentes d'insertion/suppression");
            }
            if (collection.contains("LinkedList") && code.contains(".get(")) {
                recommendations.add("⚠️ Considérez utiliser ArrayList pour les accès aléatoires fréquents");
            }
        });
        
        dataStructureAnalysis.put("collectionsUtilisees", 
            collectionUsage.isEmpty() ? "Aucune collection standard détectée" :
            collectionUsage.toString());
        dataStructureAnalysis.put("recommendations",
            recommendations.isEmpty() ? "Pas de recommandations particulières" :
            String.join("\n", recommendations));
            
        return dataStructureAnalysis;
    }

    // Méthodes utilitaires
    private int calculateMethodComplexity(String methodCode) {
        Matcher complexityMatcher = COMPLEXITY_PATTERN.matcher(methodCode);
        int complexity = 1;
        while (complexityMatcher.find()) {
            complexity++;
        }
        return complexity;
    }

    private String getComplexityClass(int complexity) {
        if (complexity <= 4) return "O(1) - Complexité constante";
        if (complexity <= 8) return "O(n) - Complexité linéaire";
        if (complexity <= 12) return "O(n²) - Complexité quadratique";
        return "O(n³) ou plus - Complexité élevée ⚠️";
    }

    private int countNestedLoops(String code) {
        Pattern loopPattern = Pattern.compile("(for|while)\\s*\\([^{]+\\{");
        Matcher matcher = loopPattern.matcher(code);
        int maxNesting = 0;
        int currentNesting = 0;
        
        while (matcher.find()) {
            currentNesting++;
            maxNesting = Math.max(maxNesting, currentNesting);
            
            int pos = matcher.end();
            int braces = 1;
            while (pos < code.length() && braces > 0) {
                char c = code.charAt(pos);
                if (c == '{') braces++;
                if (c == '}') {
                    braces--;
                    if (braces == 0) currentNesting--;
                }
                pos++;
            }
        }
        
        return maxNesting;
    }

    private List<String> detecterFuitesMemoire(String code) {
        List<String> potentialLeaks = new ArrayList<>();
        
        // Détection des ressources non fermées
        if (code.contains("new FileInputStream") || code.contains("new FileOutputStream")) {
            if (!code.contains("try-with-resources") && !code.contains(".close()")) {
                potentialLeaks.add("⚠️ Ressource de fichier potentiellement non fermée");
            }
        }
        
        // Détection des collections qui croissent indéfiniment
        Pattern collectionPattern = Pattern.compile("(List|Set|Map)<[^>]+>\\s+\\w+\\s*=");
        Matcher collectionMatcher = collectionPattern.matcher(code);
        while (collectionMatcher.find()) {
            String collection = collectionMatcher.group();
            if (code.contains("while") && code.contains(collection.split("\\s+")[2] + ".add")) {
                potentialLeaks.add("⚠️ Collection potentiellement croissante dans une boucle: " + collection);
            }
        }
        
        return potentialLeaks;
    }

public Map<String, Object> analyserMethodePerformance(String code) {
    Map<String, Object> resultats = new HashMap<>();
    Map<String, ExecutionMetrics> tempsMethodes = new HashMap<>();
    Set<NestedLoop> bouclesImbriquees = new HashSet<>(); // Évite les doublons

    Pattern methodPattern = METHOD_PATTERN;
    Matcher methodMatcher = methodPattern.matcher(code);

    while (methodMatcher.find()) {
        String methodName = methodMatcher.group(1);
        String methodContent = extractMethodContent(code, methodMatcher.end());
        
        // Mesure du temps d'exécution avec plusieurs essais
        ExecutionMetrics metrics = mesureMethodPerformance(methodContent);
        if (metrics.getAverageTime() <= 2000) { // 2ms = 2000μs
            tempsMethodes.put(methodName, metrics);
        }

        // Détection des boucles imbriquées avec le nom de la méthode
        bouclesImbriquees.addAll(detecterBouclesImbriquees(methodContent, methodName));
    }

    resultats.put("methodesRapides", tempsMethodes);
    resultats.put("bouclesImbriquees", new ArrayList<>(bouclesImbriquees));
    return resultats;
}

public static class ExecutionMetrics {
    private final long minTime;
    private final long maxTime;
    private final double averageTime;
    private final int numberOfExecutions;

    public ExecutionMetrics(long min, long max, double avg, int executions) {
        this.minTime = min;
        this.maxTime = max;
        this.averageTime = avg;
        this.numberOfExecutions = executions;
    }

    public long getMinTime() { return minTime; }
    public long getMaxTime() { return maxTime; }
    public double getAverageTime() { return averageTime; }
    public int getNumberOfExecutions() { return numberOfExecutions; }
}

private ExecutionMetrics mesureMethodPerformance(String methodContent) {
    final int ITERATIONS = 5;
    long[] times = new long[ITERATIONS];
    
    for (int i = 0; i < ITERATIONS; i++) {
        long startTime = System.nanoTime();
        analyserComplexiteAlgorithmique(methodContent);
        times[i] = TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - startTime);
    }
    
    // Calculer les statistiques
    long minTime = Arrays.stream(times).min().orElse(0);
    long maxTime = Arrays.stream(times).max().orElse(0);
    double avgTime = Arrays.stream(times).average().orElse(0.0);
    
    return new ExecutionMetrics(minTime, maxTime, avgTime, ITERATIONS);
    }

    private String extractMethodContent(String code, int startIndex) {
        int braceCount = 1;
        int endIndex = startIndex;
        
        while (braceCount > 0 && endIndex < code.length()) {
            char c = code.charAt(endIndex);
            if (c == '{') braceCount++;
            if (c == '}') braceCount--;
            endIndex++;
        }
        
        return code.substring(startIndex, endIndex);
    }


    public static class NestedLoop {
        private final int lineNumber;
        private final String methodName;
        private final String outerLoopType;
        private final String innerLoopType;
        private final String codeSnippet;
        
        // Constructeur corrigé pour accepter LoopType
        public NestedLoop(int lineNumber, String methodName, 
                         LoopType outerLoopType, LoopType innerLoopType, 
                         String codeSnippet) {
            this.lineNumber = lineNumber;
            this.methodName = methodName;
            this.outerLoopType = outerLoopType.name();
            this.innerLoopType = innerLoopType.name();
            this.codeSnippet = codeSnippet;
        }

        // Getters restent les mêmes
        public int getLineNumber() { return lineNumber; }
        public String getMethodName() { return methodName; }
        public String getOuterLoopType() { return outerLoopType; }
        public String getInnerLoopType() { return innerLoopType; }
        public String getCodeSnippet() { return codeSnippet; }
    }


		private List<NestedLoop> detecterBouclesImbriquees(String methodContent, String methodName) {
		    List<NestedLoop> nestedLoops = new ArrayList<>();
		    String[] lines = methodContent.split("\n");
		    Stack<LoopInfo> loopStack = new Stack<>();
		    int lineOffset = 0;
		    
		    // Trouver l'offset de la ligne de début de la méthode dans le code complet
		    Pattern methodPattern = Pattern.compile("\\b(?:public|private|protected)\\s+(?:[\\w<>\\[\\]]+\\s+)?(" + methodName + ")\\s*\\(");
		    Matcher methodMatcher = methodPattern.matcher(methodContent);
		    if (methodMatcher.find()) {
		        lineOffset = methodContent.substring(0, methodMatcher.start()).split("\n").length;
		    }
		    
		    for (int i = 0; i < lines.length; i++) {
		        String line = lines[i].trim();
		        
		        // Détecter les boucles avec une regex plus précise
		        Matcher forMatcher = Pattern.compile("\\bfor\\s*\\(").matcher(line);
		        Matcher whileMatcher = Pattern.compile("\\bwhile\\s*\\(").matcher(line);
		        Matcher doMatcher = Pattern.compile("\\bdo\\s*\\{").matcher(line);
		        
		        LoopType currentLoopType = null;
		        if (forMatcher.find()) {
		            currentLoopType = LoopType.FOR;
		        } else if (whileMatcher.find()) {
		            currentLoopType = LoopType.WHILE;
		        } else if (doMatcher.find()) {
		            currentLoopType = LoopType.DO_WHILE;
		        }
		        
		        if (currentLoopType != null) {
		            int actualLineNumber = i + 1 + lineOffset;
		            
		            if (!loopStack.isEmpty()) {
		                LoopInfo parentLoop = loopStack.peek();
		                int distance = actualLineNumber - parentLoop.lineNumber;
		                
		                String codeSnippet = extractRelevantCodeWithContext(lines, i, parentLoop.lineStart, 2);
		                
		                // Création de NestedLoop mise à jour pour utiliser LoopType
		                nestedLoops.add(new NestedLoop(
		                    actualLineNumber,
		                    methodName,
		                    parentLoop.loopType,    // Passe directement le LoopType
		                    currentLoopType,        // Passe directement le LoopType
		                    codeSnippet
		                ));
		            }
		            
		            loopStack.push(new LoopInfo(currentLoopType, actualLineNumber, i));
		        }
		        
		        // Gérer les accolades fermantes
		        if (line.contains("}")) {
		            if (!loopStack.isEmpty()) {
		                loopStack.pop();
		            }
		        }
		    }
		    
		    return nestedLoops;
		}
		
		private enum LoopType {
		    FOR, WHILE, DO_WHILE
		}
		
		private String extractRelevantCodeWithContext(String[] lines, int currentLine, int startLine, int contextLines) {
		    StringBuilder snippet = new StringBuilder();
		    
		    // Calculer les limites avec le contexte
		    int start = Math.max(startLine - contextLines, 0);
		    int end = Math.min(currentLine + contextLines + 1, lines.length);
		    
		    // Ajouter chaque ligne avec son numéro
		    for (int i = start; i < end; i++) {
		        String line = lines[i].trim();
		        // Marquer les lignes de boucles avec un indicateur
		        if (i == startLine || i == currentLine) {
		            snippet.append("→ "); // Flèche pour indiquer les lignes de boucles
		        } else {
		            snippet.append("  "); // Indentation pour les autres lignes
		        }
		        snippet.append(line).append("\n");
		    }
		    
		    return snippet.toString().trim();
		}
    
		private void processLoop(String loopType, int lineIndex, String[] lines,
		        Stack<LoopInfo> loopStack, List<NestedLoop> nestedLoops,
		        String methodName) {
		    
		    // Convertir la chaîne loopType en LoopType
		    LoopType type = LoopType.valueOf(loopType.toUpperCase());
		    
		    // Créer une nouvelle instance de LoopInfo avec tous les paramètres requis
		    LoopInfo currentLoop = new LoopInfo(type, lineIndex + 1, lineIndex);

		    if (!loopStack.isEmpty()) {
		        LoopInfo parentLoop = loopStack.peek();
		        // Calculer la distance entre les boucles
		        int distance = currentLoop.lineNumber - parentLoop.lineNumber;

		        // Si la boucle imbriquée est dans une portée raisonnable (par exemple, moins de 10 lignes)
		        if (distance < 10) {
		            // Extraire le snippet de code pertinent
		            String codeSnippet = extractRelevantCode(lines, parentLoop.lineNumber - 1,
		                    currentLoop.lineNumber - 1);

		            nestedLoops.add(new NestedLoop(
		                parentLoop.lineNumber,
		                methodName,
		                parentLoop.loopType,  // Utilise directement le LoopType de parentLoop
		                currentLoop.loopType, // Utilise directement le LoopType de currentLoop
		                codeSnippet
		            ));
		        }
		    }

		    loopStack.push(currentLoop);
		}	

    private static class LoopInfo {
        private final LoopType loopType;
        private final int lineNumber;
        private final int lineStart;
        
        // Constructeur corrigé
        LoopInfo(LoopType loopType, int lineNumber, int lineStart) {
            this.loopType = loopType;
            this.lineNumber = lineNumber;
            this.lineStart = lineStart;
        }
    }
		
		private String extractRelevantCode(String[] lines, int startLine, int endLine) {
		StringBuilder snippet = new StringBuilder();
		int contextLines = 2; // Nombre de lignes de contexte à inclure
		
		// Début du snippet
		int start = Math.max(0, startLine - contextLines);
		int end = Math.min(lines.length, endLine + contextLines + 1);
		
		for (int i = start; i < end; i++) {
		snippet.append(lines[i].trim()).append("\n");
		}
		
		return snippet.toString().trim();
		}
		
		    private int countLines(String code) {
		        return code.split("\n").length;
		    }
    
}