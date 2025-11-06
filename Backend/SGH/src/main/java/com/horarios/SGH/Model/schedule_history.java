package com.horarios.SGH.Model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity(name = "schedule_history")
@Table(name = "schedule_execution_history")
public class schedule_history {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "executed_by", length = 100)
    private String executedBy;
    
    @Column(name = "executed_at", columnDefinition = "TIMESTAMP")
    private LocalDateTime executedAt;

    // RUNNING | SUCCESS | FAILED
    @Column(name = "execution_status", length = 20)
    private String status;

    @Column(name = "total_generated")
    private int totalGenerated;

    @Column(name = "execution_message", length = 1000)
    private String message;

    // Parámetros de ejecución (si ya existen, conserva tus nombres)
    @Column(name = "period_start", columnDefinition = "DATE")
    private LocalDate periodStart;
    
    @Column(name = "period_end", columnDefinition = "DATE")
    private LocalDate periodEnd;
    
    @Column(name = "dry_run_flag")
    private boolean dryRun;
    
    @Column(name = "force_flag")
    private boolean force;

    @Column(name = "execution_params", length = 500)
    private String params;

    public schedule_history(Integer id, String executedBy, LocalDateTime executedAt, String status, int totalGenerated,
            String message, LocalDate periodStart, LocalDate periodEnd, boolean dryRun, boolean force, String params) {
        this.id = id;
        this.executedBy = executedBy;
        this.executedAt = executedAt;
        this.status = status;
        this.totalGenerated = totalGenerated;
        this.message = message;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.dryRun = dryRun;
        this.force = force;
        this.params = params;
    }

    public schedule_history() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getExecutedBy() {
        return executedBy;
    }

    public void setExecutedBy(String executedBy) {
        this.executedBy = executedBy;
    }

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(LocalDateTime executedAt) {
        this.executedAt = executedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getTotalGenerated() {
        return totalGenerated;
    }

    public void setTotalGenerated(int totalGenerated) {
        this.totalGenerated = totalGenerated;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(LocalDate periodStart) {
        this.periodStart = periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(LocalDate periodEnd) {
        this.periodEnd = periodEnd;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }
}