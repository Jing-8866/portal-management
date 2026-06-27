package com.portal.main.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class ImportResult {
    private int totalRows;
    private int createdCount;
    private int updatedCount;
    private int failedCount;
    private List<String> errors = new ArrayList<>();
}
