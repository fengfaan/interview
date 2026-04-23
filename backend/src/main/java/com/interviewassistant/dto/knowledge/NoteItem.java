package com.interviewassistant.dto.knowledge;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class NoteItem {
    private String id;
    private String title;
    private String direction;
    private List<String> tags;
    private String created;
    private String fileName;
}
