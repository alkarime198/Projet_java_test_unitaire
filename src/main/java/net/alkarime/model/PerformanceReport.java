package net.alkarime.model;

public class PerformanceReport {
    private long executionTime;
    private long memoryUsed;
    private double cpuUsage;
    private long codeSize;

    public PerformanceReport(long executionTime, long memoryUsed, double cpuUsage, long codeSize) {
        this.executionTime = executionTime;
        this.memoryUsed = memoryUsed;
        this.cpuUsage = cpuUsage;
        this.codeSize = codeSize;
    }

    // Getters et setters
    public long getExecutionTime() { return executionTime; }
    public void setExecutionTime(long executionTime) { this.executionTime = executionTime; }
    public long getMemoryUsed() { return memoryUsed; }
    public void setMemoryUsed(long memoryUsed) { this.memoryUsed = memoryUsed; }
    public double getCpuUsage() { return cpuUsage; }
    public void setCpuUsage(double cpuUsage) { this.cpuUsage = cpuUsage; }
    public long getCodeSize() { return codeSize; }
    public void setCodeSize(long codeSize) { this.codeSize = codeSize; }
}