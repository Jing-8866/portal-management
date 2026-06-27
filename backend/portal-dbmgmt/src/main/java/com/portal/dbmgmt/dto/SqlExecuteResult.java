package com.portal.dbmgmt.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class SqlExecuteResult {
    /** query | update */
    private String type;
    private List<String> columns = new ArrayList<>();
    private List<List<Object>> rows = new ArrayList<>();
    private Integer affectedRows;
    private Integer rowCount;
    private Long duration;
    private String message;
    private Boolean truncated;
}
