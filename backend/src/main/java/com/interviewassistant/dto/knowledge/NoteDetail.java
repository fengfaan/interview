package com.interviewassistant.dto.knowledge;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class NoteDetail extends NoteItem {
    private String content;

    public NoteDetail(String id, String title, String direction,
                      List<String> tags, String created,
                      String fileName, String content) {
        super(id, title, direction, tags, created, fileName);
        this.content = content;
    }
}
