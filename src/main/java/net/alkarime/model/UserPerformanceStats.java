package net.alkarime.model;

public class UserPerformanceStats {
    private double averageExecutionTime;
    private double averageMemoryUsed;

    public UserPerformanceStats(double averageExecutionTime, double averageMemoryUsed) {
        this.averageExecutionTime = averageExecutionTime;
        this.averageMemoryUsed = averageMemoryUsed;
    }

    // Getters et setters
    public double getAverageExecutionTime() { return averageExecutionTime; }
    public void setAverageExecutionTime(double averageExecutionTime) { this.averageExecutionTime = averageExecutionTime; }
    public double getAverageMemoryUsed() { return averageMemoryUsed; }
    public void setAverageMemoryUsed(double averageMemoryUsed) { this.averageMemoryUsed = averageMemoryUsed; }
}