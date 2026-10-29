package com.verdantdays.model.dto;

import java.util.ArrayList;
import java.util.List;

public class SalesImportResult {
    private boolean dryRun;
    private int totalRows;
    private int successCount;
    private int skippedCount;
    private int errorCount;
    private int customersCreated;
    private int customersUpdated;
    private List<String> errors = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();

    public boolean isDryRun() { return dryRun; }
    public void setDryRun(boolean dryRun) { this.dryRun = dryRun; }
    public int getTotalRows() { return totalRows; }
    public void setTotalRows(int totalRows) { this.totalRows = totalRows; }
    public int getSuccessCount() { return successCount; }
    public void setSuccessCount(int successCount) { this.successCount = successCount; }
    public int getSkippedCount() { return skippedCount; }
    public void setSkippedCount(int skippedCount) { this.skippedCount = skippedCount; }
    public int getErrorCount() { return errorCount; }
    public void setErrorCount(int errorCount) { this.errorCount = errorCount; }
    public int getCustomersCreated() { return customersCreated; }
    public void setCustomersCreated(int customersCreated) { this.customersCreated = customersCreated; }
    public int getCustomersUpdated() { return customersUpdated; }
    public void setCustomersUpdated(int customersUpdated) { this.customersUpdated = customersUpdated; }
    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }
    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }

    public void addError(String msg) {
        errors.add(msg);
        errorCount++;
    }

    public void addWarning(String msg) {
        warnings.add(msg);
    }
}
