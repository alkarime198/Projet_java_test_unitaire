 package net.alkarime.service;

import net.alkarime.entities.SoumettreCode;
import net.alkarime.service.QualitePerformanceService.ExecutionMetrics;
import net.alkarime.service.QualitePerformanceService.NestedLoop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.tools.*;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Stateless
public class CompilationService {
    private static final Logger logger = LoggerFactory.getLogger(CompilationService.class);
    private static final int TIMEOUT_SECONDS = 2;  
    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("EEEE dd MMMM yyyy 'à' HH:mm:ss", 
    	    new Locale("fr", "FR"));
     
    @Inject
    private QualiteAnalyseService qualiteService;    
    @Inject
    private QualitePerformanceService qualitePerformanceService;  
    
    private static final Runtime runtime = Runtime.getRuntime();
    private static final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
    
    public SoumettreCode compileAndRun(String codeFilePath, String className, String userInput) {
        long startTime = System.nanoTime();
        CpuSnapshot startCpu = takeCpuSnapshot();
        long initialMemory = getUsedMemory();

        SoumettreCode submission = new SoumettreCode();

        try {
            String codeContent = readCodeFromFile(codeFilePath);
            submission.setCodeFilePath(codeFilePath);
            submission.setCodeSize(codeContent.getBytes().length);
            submission.setCode(codeContent);

            ExecutionResult executionResult = compileAndExecuteCode(codeContent, className, userInput);

            long endTime = System.nanoTime();
            CpuSnapshot endCpu = takeCpuSnapshot();
            long finalMemory = getUsedMemory();

            long executionTime = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
            double cpuUsage = calculateCpuUsage(startCpu, endCpu, executionTime);
            long memoryUsed = finalMemory - initialMemory;

            submission.setTempsExecuter(executionTime);

            String rapport = generateRapportUnifie(submission, executionResult, executionTime, cpuUsage, memoryUsed);
            submission.setResultatExecuter(rapport);

        } catch (Exception e) {
            logger.error("Erreur lors de la compilation/exécution : {}", e.getMessage(), e);
            submission.setResultatExecuter(generateRapportErreur(e));
        }

        return submission;
    }  
    
    private static class CpuSnapshot {
        long systemTime;
        long threadCpuTime;
        double systemLoad;

        CpuSnapshot() {
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
            this.systemTime = System.nanoTime();
            this.threadCpuTime = threadBean.getCurrentThreadCpuTime();
            this.systemLoad = osBean.getSystemLoadAverage();
        }
    }

    private CpuSnapshot takeCpuSnapshot() {
        return new CpuSnapshot();
    }
    
    private double getCpuLoad() {
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            return ((com.sun.management.OperatingSystemMXBean) osBean).getProcessCpuLoad() * 100.0;
        }
        // Fallback si l'implémentation spécifique n'est pas disponible
        return osBean.getSystemLoadAverage() * 100.0;
    }

    private double calculateCpuUsage(CpuSnapshot start, CpuSnapshot end, long executionTimeMillis) {
        if (executionTimeMillis <= 0) return 0.0;

        // Calcul basé sur le temps CPU du thread
        long cpuTime = end.threadCpuTime - start.threadCpuTime;
        long totalTime = end.systemTime - start.systemTime;
        
        if (totalTime <= 0) return 0.0;

        // Calcul du pourcentage d'utilisation
        double cpuUsage = (cpuTime * 100.0) / totalTime;
        
        // Ajustement basé sur la charge système moyenne
        double systemLoadDiff = Math.max(0, end.systemLoad - start.systemLoad);
        double adjustedCpuUsage = cpuUsage * (1 + systemLoadDiff);

        // Normalisation
        return Math.min(100.0, Math.max(0.0, adjustedCpuUsage));
    }

    private long getUsedMemory() {
        return runtime.totalMemory() - runtime.freeMemory();
    }
    private ExecutionResult compileAndExecuteCode(String codeContent, String className, String userInput) 
            throws Exception {
        long startTime = System.nanoTime();
        
        // Préparation de la compilation
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new CompilationException("Aucun compilateur Java disponible");
        }

        // Création d'un répertoire temporaire pour les fichiers compilés
        File tempDir = Files.createTempDirectory("compile").toFile();
        tempDir.deleteOnExit();

        // Compilation
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        
        // Préparer les fichiers de classe
        Map<String, File> classFiles = splitClassesIntoFiles(codeContent, tempDir);
        
        // Compiler le code
        CompilationResult compilationResult = compileCode(compiler, classFiles, tempDir, diagnostics);
        
        if (compilationResult.isSuccess()) {
            // Exécution du code compilé
            String output = executeCompiledClass(className, tempDir, userInput);
            return new ExecutionResult(output, System.nanoTime() - startTime);
        } else {
            return new ExecutionResult(compilationResult.getError(), 0);
        }
    }
    
    private String formatCompilationErrors(DiagnosticCollector<JavaFileObject> diagnostics) {
        StringBuilder errorMsg = new StringBuilder("Erreurs de compilation :\n");
        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
            errorMsg.append(String.format("Ligne %d: %s%n", 
                diagnostic.getLineNumber(), 
                diagnostic.getMessage(null)));
        }
        return errorMsg.toString();
    }
    

    private long getCpuTime() {
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        return threadBean.getCurrentThreadCpuTime();
    }
  
    private static class CompilationResult {
        private final boolean success;
        private final String error;

        public CompilationResult(boolean success, String error) {
            this.success = success;
            this.error = error;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getError() {
            return error;
        }
    }

    private CompilationResult compileCode(JavaCompiler compiler, Map<String, File> classFiles, 
                                        File tempDir, DiagnosticCollector<JavaFileObject> diagnostics) {
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null)) {
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT, Arrays.asList(tempDir));
            Iterable<? extends JavaFileObject> compilationUnits = 
                fileManager.getJavaFileObjectsFromFiles(classFiles.values());
            JavaCompiler.CompilationTask task = 
                compiler.getTask(null, fileManager, diagnostics, null, null, compilationUnits);

            boolean success = task.call();
            return new CompilationResult(success, success ? null : formatCompilationErrors(diagnostics));
        } catch (IOException e) {
            return new CompilationResult(false, "Erreur d'E/S pendant la compilation : " + e.getMessage());
        }
    }
    
    private static class ExecutionResult {
        private final String output;
        private final long executionTime;

        public ExecutionResult(String output, long executionTime) {
            this.output = output;
            this.executionTime = executionTime;
        }

        public String getOutput() {
            return output;
        }

        public long getExecutionTime() {
            return executionTime;
        }
    }

    private String readCodeFromFile(String codeFilePath) throws IOException {
        File codeFile = new File(codeFilePath);
        return new String(Files.readAllBytes(codeFile.toPath()));
    }
    
    private String generateRapportUnifie(
            SoumettreCode submission,
            ExecutionResult executionResult,
            long executionTime,
            double cpuUsage,
            long memoryUsed) {

        final long MEMORY_THRESHOLD = 1024 * 1024 * 10; // 10 MB
        StringBuilder rapport = new StringBuilder();
        String codeContent = submission.getCode();

        // En-tête
        rapport.append("╔════════════════════════════════════════════════════════╗\n")
               .append("║                RAPPORT D'ANALYSE DÉTAILLÉ               ║\n")
               .append("╚════════════════════════════════════════════════════════╝\n\n")
               .append("📅 Date d'analyse : ")
               .append(DATE_FORMATTER.format(new Date()))
               .append("\n\n");

        // Section 1: Résultat d'exécution
        rapport.append("📊 1. RÉSULTAT D'EXÉCUTION\n")
               .append("========================\n")
               .append(executionResult.getOutput())
               .append("\n\n");

        // Section 2: Métriques de performance
        rapport.append("📈 2. MÉTRIQUES DE PERFORMANCE\n")
               .append("===========================\n");
        
        // Calcul correct de la mémoire utilisée
        long actualMemoryUsed = Math.max(0, memoryUsed); // Évite les valeurs négatives
        
        // Métriques de base avec format corrigé pour le CPU
        rapport.append("▪ Temps d'exécution : ").append(executionTime).append(" ms\n")
               .append("▪ Utilisation CPU : ").append(String.format("%.2f%%", Math.max(0, Math.min(100, cpuUsage))))
               .append(cpuUsage > 50 ? " ⚠️ (Élevée)" : "").append("\n")
               .append("▪ Mémoire utilisée : ").append(formatBytes(actualMemoryUsed))
               .append(actualMemoryUsed > MEMORY_THRESHOLD ? " ⚠️ (Élevée)" : "").append("\n")
               .append("▪ Taille du code : ").append(formatBytes(submission.getCodeSize())).append("\n\n");

        // Section 3: Analyse de performance détaillée
        Map<String, Object> performanceMetrics = qualitePerformanceService.analyserPerformance(codeContent);
        rapport.append("🔍 3. ANALYSE DE PERFORMANCE DÉTAILLÉE\n")
               .append("==================================\n");
        
        // Complexité algorithmique
        Map<String, String> complexityAnalysis = (Map<String, String>) performanceMetrics.get("complexiteAlgorithmique");
        rapport.append("◉ Complexité algorithmique :\n")
               .append("  ").append(complexityAnalysis.get("complexiteTemporelle")).append("\n")
               .append("  ").append(complexityAnalysis.get("bouclesImbriquees")).append("\n\n");

        // Section 4: Analyse de la mémoire
        Map<String, String> memoryAnalysis = (Map<String, String>) performanceMetrics.get("utilisationMemoire");
        rapport.append("🔍 4. ANALYSE DE LA MÉMOIRE\n")
               .append("========================\n")
               .append("◉ Analyse mémoire :\n")
               .append("  ").append(memoryAnalysis.get("allocationsImportantes")).append("\n")
               .append("  ").append(memoryAnalysis.get("fuitesPotentielles")).append("\n\n");

        // Section 5: Goulots d'étranglement
        List<String> bottlenecks = (List<String>) performanceMetrics.get("goulotsEtranglement");
        rapport.append("🔍 5. ANALYSE DES GOULOTS D'ÉTRANGLEMENT\n")
               .append("====================================\n");
        if (bottlenecks.isEmpty()) {
            rapport.append("✅ Aucun goulot d'étranglement détecté\n");
        } else {
            bottlenecks.forEach(b -> rapport.append("▪ ").append(b).append("\n"));
        }
        rapport.append("\n");

        // Section 6: Structures de données
        Map<String, String> dataStructureAnalysis = (Map<String, String>) performanceMetrics.get("structuresDonnees");
        rapport.append("🔍 6. ANALYSE DES STRUCTURES DE DONNÉES\n")
               .append("===================================\n")
               .append("◉ Collections utilisées: ").append(dataStructureAnalysis.get("collectionsUtilisees")).append("\n")
               .append("◉ Recommandations: ").append(dataStructureAnalysis.get("recommendations")).append("\n\n");

        // Section 7: Analyse des méthodes rapides
        // Section 7 et 8: Ajouter l'analyse des méthodes
        Map<String, Object> methodPerformance = qualitePerformanceService.analyserMethodePerformance(codeContent);
        appendMethodPerformanceSection(rapport, methodPerformance);


        // Section 9: Analyse de qualité
        rapport.append("🔍 9. ANALYSE DE QUALITÉ\n")
               .append("=====================\n");
        Map<String, String> qualityAnalysis = qualiteService.analyseQualiteCode(codeContent);
        for (Map.Entry<String, String> entry : qualityAnalysis.entrySet()) {
            rapport.append("◉ ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }

        // Section 10: Optimisations suggérées
        Map<String, String> performanceAnalysis = qualiteService.optimisePerformances(codeContent);
        rapport.append("\n📊 10. SUGGESTIONS D'OPTIMISATION\n")
               .append("==============================\n");
        for (Map.Entry<String, String> entry : performanceAnalysis.entrySet()) {
            rapport.append("◉ ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }

        // Section 11: Refactoring suggéré
        Map<String, String> refactoringSuggestions = qualiteService.suggestRefactoring(codeContent);
        rapport.append("\n🔄 11. SUGGESTIONS DE REFACTORING\n")
               .append("==============================\n");
        for (Map.Entry<String, String> entry : refactoringSuggestions.entrySet()) {
            rapport.append("◉ ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }

        // Section 12: Analyse statique
        Map<String, String> staticAnalysis = qualiteService.analyseStatiqueCode(codeContent);
        rapport.append("\n🔬 12. ANALYSE STATIQUE DU CODE\n")
               .append("============================\n");
        for (Map.Entry<String, String> entry : staticAnalysis.entrySet()) {
            rapport.append("◉ ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }

        // Section 13: Résumé et recommandations prioritaires
        rapport.append("\n📋 13. RÉSUMÉ ET RECOMMANDATIONS PRIORITAIRES\n")
               .append("==========================================\n");
        
        List<String> priorityIssues = new ArrayList<>();
        if (cpuUsage > 50) {
            priorityIssues.add("⚠️ Optimisation CPU requise (utilisation > 50%)");
        }
        if (actualMemoryUsed > MEMORY_THRESHOLD) {
            priorityIssues.add("⚠️ Optimisation mémoire recommandée");
        }
        if (executionTime > 1000) {
            priorityIssues.add("⚠️ Temps d'exécution élevé (> 1s)");
        }
        
        if (qualiteService.hasPotentialInfiniteLoop(codeContent)) {
            priorityIssues.add("⚠️ Risque de boucle infinie détecté");
        }
        
        if (priorityIssues.isEmpty()) {
            rapport.append("✅ Aucun problème critique détecté\n");
        } else {
            rapport.append("Points d'attention prioritaires :\n");
            for (String issue : priorityIssues) {
                rapport.append("▪ ").append(issue).append("\n");
            }
        }

        // Pied de page
        rapport.append("\n════════════════════════════════════════════════════\n")
               .append("📌 Rapport généré automatiquement - ")
               .append(DATE_FORMATTER.format(new Date()))
               .append("\n════════════════════════════════════════════════════");

        return rapport.toString();
    }

    	// Méthode utilitaire pour formater les bytes
    	private String formatBytes(long bytes) {
    	    if (bytes < 1024) return bytes + " B";
    	    int exp = (int) (Math.log(bytes) / Math.log(1024));
    	    String pre = "KMGTPE".charAt(exp-1) + "";
    	    return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    	}
    private String generateRapportErreur(Exception e) {
        StringBuilder sb = new StringBuilder();
        sb.append("╔═══════════════════════════════════════════╗\n");
        sb.append("║            RAPPORT D'ERREUR              ║\n");
        sb.append("╚═══════════════════════════════════════════╝\n\n");
        sb.append("📅 Date : ").append(DATE_FORMATTER.format(new Date())).append("\n");
        sb.append("❌ Type d'erreur : ").append(e.getClass().getSimpleName()).append("\n");
        sb.append("📝 Message : ").append(e.getMessage()).append("\n\n");
        sb.append("═══════════════════════════════════════════\n");
        return sb.toString();
    }
    private String formatCodeSnippet(String snippet) {
        return Arrays.stream(snippet.split("\n"))
                     .map(line -> "    " + line)  // Ajoute une indentation
                     .collect(Collectors.joining("\n"));
    }

    private void appendMethodPerformanceSection(StringBuilder rapport, Map<String, Object> methodPerformance) {
        // Section 7: Méthodes rapides
        rapport.append("\n🔍 7. ANALYSE DES MÉTHODES RAPIDES\n")
               .append("================================\n");

        @SuppressWarnings("unchecked")
        Map<String, ExecutionMetrics> methodesRapides = 
            (Map<String, ExecutionMetrics>) methodPerformance.get("methodesRapides");

        if (methodesRapides == null || methodesRapides.isEmpty()) {
            rapport.append("✅ Aucune méthode avec un temps d'exécution ≤ 2ms détectée\n");
        } else {
            rapport.append("Méthodes avec temps d'exécution optimal (≤ 2ms) :\n");
            methodesRapides.forEach((methodName, metrics) -> 
                rapport.append("▪ ").append(methodName)
                       .append("\n - Temps moyen : ").append(String.format("%.2f", metrics.getAverageTime())).append("μs")
                       .append("\n - Temps min : ").append(metrics.getMinTime()).append("μs")
                       .append("\n - Temps max : ").append(metrics.getMaxTime()).append("μs")
                       .append("\n - Nombre d'exécutions : ").append(metrics.getNumberOfExecutions())
                       .append("\n")
            );
        }

        // Section 8: Boucles imbriquées
        rapport.append("\n🔍 8. ANALYSE DES BOUCLES IMBRIQUÉES\n")
               .append("==================================\n");

        @SuppressWarnings("unchecked")
        List<NestedLoop> bouclesImbriquees = 
            (List<NestedLoop>) methodPerformance.get("bouclesImbriquees");

        if (bouclesImbriquees == null || bouclesImbriquees.isEmpty()) {
            rapport.append("✅ Aucune boucle imbriquée détectée\n");
        } else {
            rapport.append("⚠️ Boucles imbriquées détectées :\n\n");
            
            // Trier les boucles par méthode et ligne
            bouclesImbriquees.sort(Comparator
                .comparing(NestedLoop::getMethodName)
                .thenComparing(NestedLoop::getLineNumber));
            
            String currentMethod = "";
            for (NestedLoop loop : bouclesImbriquees) {
                // Ajouter un séparateur entre les méthodes différentes
                if (!currentMethod.equals(loop.getMethodName())) {
                    if (!currentMethod.isEmpty()) {
                        rapport.append("\n");
                    }
                    currentMethod = loop.getMethodName();
                    rapport.append("📎 Méthode: ").append(currentMethod).append("\n");
                }
                
                rapport.append("▪ Ligne ").append(loop.getLineNumber())
                       .append(": ").append(loop.getOuterLoopType().toLowerCase())
                       .append(" → ").append(loop.getInnerLoopType().toLowerCase())
                       .append("\n")
                       .append("  Code source:\n")
                       .append(formatCodeSnippet(loop.getCodeSnippet()))
                       .append("\n");
            }

            // Ajouter des recommandations spécifiques
            rapport.append("\n📌 Recommandations d'optimisation:\n")
                  .append("1. Évaluez la possibilité de paralléliser certaines opérations\n")
                  .append("2. Considérez l'utilisation de streams pour simplifier le code\n")
                  .append("3. Vérifiez si certaines opérations peuvent être pré-calculées\n")
                  .append("4. Examinez si les boucles peuvent être fusionnées ou optimisées\n");
        }
    }
    
    public class StringJavaFileObject extends SimpleJavaFileObject {
        private final String sourceCode;

        public StringJavaFileObject(String className, String sourceCode) {
            super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.sourceCode = sourceCode;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return sourceCode;
        }
    }
       
    private String generateTestCode(String userCode, String className) {
        if (userCode.contains("class ")) {
            return userCode;
        }
        return "public class " + className + " {\n" +
               "    public static void main(String[] args) {\n" +
               "        " + userCode + "\n" +
               "    }\n" +
               "}";
    }
    private String executeCompiledClass(String testClassName, File classDir, String userInput) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future = executor.submit(() -> {
            try {
                URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]{classDir.toURI().toURL()});
                Class<?> cls = Class.forName(testClassName, true, classLoader);
                Method mainMethod = cls.getMethod("main", String[].class);
                mainMethod.setAccessible(true);

                InputStream inputStream = validateUserInput(userInput);
                System.setIn(inputStream);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PrintStream printStream = new PrintStream(baos, true, "UTF-8");
                PrintStream originalOut = System.out;
                System.setOut(printStream);

                mainMethod.invoke(null, (Object) new String[]{});
                System.setOut(originalOut);

                return new String(baos.toByteArray(), "UTF-8");

            } catch (Exception e) {
                return "Erreur d'exécution : " + e.getMessage();
            }
        });

        try {
            return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            return "Erreur : Boucle infinie détectée ou exécution trop longue.";
        } catch (Exception e) {
            return "Erreur d'exécution : " + e.getMessage();
        } finally {
            future.cancel(true);
            executor.shutdown();
        }
    }

    private InputStream validateUserInput(String userInput) throws Exception {
        try {
            Integer.parseInt(userInput);
            return new ByteArrayInputStream(userInput.getBytes());
        } catch (NumberFormatException e) {
            return new ByteArrayInputStream(userInput.getBytes());
        }
    }
  
    public static String extractClassName(String codeContent) {
        int classIndex = codeContent.indexOf("class ");
        if (classIndex != -1) {
            int classNameStart = classIndex + 6;
            int classNameEnd = codeContent.indexOf(" ", classNameStart);
            int braceEnd = codeContent.indexOf("{", classNameStart);
            if (classNameEnd == -1 || (braceEnd != -1 && braceEnd < classNameEnd)) {
                classNameEnd = braceEnd;
            }
            return codeContent.substring(classNameStart, classNameEnd).trim();
        }
        return "UnknownClass";
    }
    
    private Map<String, File> splitClassesIntoFiles(String codeContent, File tempDir) throws IOException {
        Map<String, File> classFiles = new HashMap<>();
        String[] classes = codeContent.split("(?<=\\})\\s*(?=public class|class)");
        
        for (String classCode : classes) {
            String className = extractClassName(classCode);
            File classFile = new File(tempDir, className + ".java");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(classFile))) {
                writer.write(classCode);
            }
            classFiles.put(className, classFile);
        }
        return classFiles;
    }

}