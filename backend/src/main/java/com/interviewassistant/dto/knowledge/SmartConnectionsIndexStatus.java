package com.interviewassistant.dto.knowledge;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class SmartConnectionsIndexStatus {
    private boolean available;
    private String dataDir;
    private List<String> modelKeys;
    private int sourceCount;
    private int blockCount;
    private int vectorCount;
    private Map<Integer, Integer> dimensions;
    private String message;
}
