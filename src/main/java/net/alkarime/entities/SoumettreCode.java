package net.alkarime.entities;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.io.Serializable;

@Entity
@Table(name = "soumettre_code")
public class SoumettreCode implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code_file_path", nullable = false)
    private String codeFilePath; 

    @Column(name = "date_soumission", nullable = false)
    private LocalDateTime dateSoumission;

    @Column(name = "resultat_executer")
    private String resultatExecuter;

    @Column(name = "temps_executer")
    private long tempsExecuter;

    @Column(name = "memory_used")
    private long memoryUsed; 

    @Column(name = "coverage_percentage")
    private int coveragePercentage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserTU user;
    
    @Column(name = "cpu_usage_percentage")
    private double cpuUsagePercentage;
    
    @Column(name = "peak_memory_used")
    private long peakMemoryUsed;
    
    @Column(name = "compilation_time")
    private long compilationTime;
    
    @Column(name = "code_size")
    private long codeSize;
    
    @Column(name = "version")
    private int version;

    @Column(name = "cpu_usage")
    private double cpuUsage;
    
    @Column(name = "code")
    private String code;
    
    @ManyToOne
    @JoinColumn(name = "parent_submission_id")
    private SoumettreCode parentSubmission;
    
    @OneToMany(mappedBy = "parentSubmission")
    private List<SoumettreCode> childSubmissions = new ArrayList<>();

    // Constructeurs
    public SoumettreCode() {
    }

    public SoumettreCode(String codeFilePath, LocalDateTime dateSoumission, String resultatExecuter,
                         long tempsExecuter, long memoryUsed, int coveragePercentage, UserTU user) {
        this.codeFilePath = codeFilePath;
        this.dateSoumission = dateSoumission;
        this.resultatExecuter = resultatExecuter;
        this.tempsExecuter = tempsExecuter;
        this.memoryUsed = memoryUsed;
        this.coveragePercentage = coveragePercentage;
        this.user = user;
    }

    // Getters et Setters originaux
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodeFilePath() {
        return codeFilePath;
    }

    public void setCodeFilePath(String codeFilePath) {
        this.codeFilePath = codeFilePath;
    }

    public LocalDateTime getDateSoumission() {
        return dateSoumission;
    }

    public void setDateSoumission(LocalDateTime dateSoumission) {
        this.dateSoumission = dateSoumission;
    }

    public String getResultatExecuter() {
        return resultatExecuter;
    }

    public void setResultatExecuter(String resultatExecuter) {
        this.resultatExecuter = resultatExecuter;
    }

    public long getTempsExecuter() {
        return tempsExecuter;
    }

    public void setTempsExecuter(long tempsExecuter) {
        this.tempsExecuter = tempsExecuter;
    }

    public long getMemoryUsed() {
        return memoryUsed;
    }

    public void setMemoryUsed(long memoryUsed) {
        this.memoryUsed = memoryUsed;
    }

    public int getCoveragePercentage() {
        return coveragePercentage;
    }

    public void setCoveragePercentage(int coveragePercentage) {
        this.coveragePercentage = coveragePercentage;
    }

    public UserTU getUser() {
        return user;
    }

    public void setUser(UserTU user) {
        this.user = user;
    }

    // Getters et setters pour les champs manquants
    public long getCompilationTime() {
        return compilationTime;
    }

    public void setCompilationTime(long compilationTime) {
        this.compilationTime = compilationTime;
    }

    public double getCpuUsage() {
        return cpuUsage;
    }

    public void setCpuUsage(double cpuUsage) {
        this.cpuUsage = cpuUsage;
    }

    public long getCodeSize() {
        return codeSize;
    }

    public void setCodeSize(long codeSize) {
        this.codeSize = codeSize;
    }
    
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public double getCpuUsagePercentage() {
        return cpuUsagePercentage;
    }

    public void setCpuUsagePercentage(double cpuUsagePercentage) {
        this.cpuUsagePercentage = cpuUsagePercentage;
    }

    public long getPeakMemoryUsed() {
        return peakMemoryUsed;
    }

    public void setPeakMemoryUsed(long peakMemoryUsed) {
        this.peakMemoryUsed = peakMemoryUsed;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public SoumettreCode getParentSubmission() {
        return parentSubmission;
    }

    public void setParentSubmission(SoumettreCode parentSubmission) {
        this.parentSubmission = parentSubmission;
    }

    public List<SoumettreCode> getChildSubmissions() {
        return childSubmissions;
    }

    public void setChildSubmissions(List<SoumettreCode> childSubmissions) {
        this.childSubmissions = childSubmissions;
    }

    @Override
    public String toString() {
        return "SoumettreCode{" +
                "id=" + id +
                ", codeFilePath='" + codeFilePath + '\'' +
                ", dateSoumission=" + dateSoumission +
                ", resultatExecuter='" + resultatExecuter + '\'' +
                ", tempsExecuter=" + tempsExecuter +
                ", memoryUsed=" + memoryUsed +
                ", coveragePercentage=" + coveragePercentage +
                ", cpuUsage=" + cpuUsage +
                ", compilationTime=" + compilationTime +
                ", codeSize=" + codeSize +
                ", code=" + code +
                ", user=" + (user != null ? user.getId() : null) +
                '}';
    }
}