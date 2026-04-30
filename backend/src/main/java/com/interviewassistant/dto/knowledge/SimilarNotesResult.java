package com.interviewassistant.dto.knowledge;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class SimilarNotesResult {
    private String searchMethod;
    private List<NoteItem> notes;
}
