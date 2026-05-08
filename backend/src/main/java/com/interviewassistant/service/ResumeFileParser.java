package com.interviewassistant.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

@Slf4j
@Service
public class ResumeFileParser {

    public ParsedResult parse(byte[] content, String fileName) {
        if (fileName == null) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".pdf")) {
            return parsePdf(content);
        } else if (lowerName.endsWith(".docx")) {
            return parseDocx(content);
        } else {
            throw new IllegalArgumentException("不支持的文件格式，仅支持 PDF 和 DOCX 文件");
        }
    }

    ParsedResult parsePdf(byte[] content) {
        try (PDDocument doc = Loader.loadPDF(content)) {
            int pageCount = doc.getNumberOfPages();
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String rawText = stripper.getText(doc);
            String cleaned = cleanText(rawText);
            String warning = null;
            if (cleaned.isBlank()) {
                warning = "该 PDF 可能为扫描件，未提取到文字内容，请直接粘贴简历内容";
            }
            return new ParsedResult(cleaned, pageCount, warning);
        } catch (IOException e) {
            throw new RuntimeException("PDF 文件解析失败: " + e.getMessage(), e);
        }
    }

    ParsedResult parseDocx(byte[] content) {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(content))) {
            StringBuilder sb = new StringBuilder();
            for (XWPFParagraph para : doc.getParagraphs()) {
                String text = para.getText();
                if (text != null && !text.isBlank()) {
                    sb.append(text).append("\n");
                }
            }
            for (XWPFTable table : doc.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    List<XWPFTableCell> cells = row.getTableCells();
                    StringBuilder rowText = new StringBuilder();
                    for (int i = 0; i < cells.size(); i++) {
                        if (i > 0) rowText.append("\t");
                        rowText.append(cells.get(i).getText().trim());
                    }
                    String line = rowText.toString().trim();
                    if (!line.isEmpty()) {
                        sb.append(line).append("\n");
                    }
                }
            }
            String cleaned = cleanText(sb.toString());
            String warning = null;
            if (cleaned.isBlank()) {
                warning = "DOCX 文件未提取到文字内容";
            }
            return new ParsedResult(cleaned, 1, warning);
        } catch (IOException e) {
            throw new RuntimeException("DOCX 文件解析失败: " + e.getMessage(), e);
        }
    }

    String cleanText(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String cleaned = text;
        // Remove page number patterns: "第 X 页", "Page X of Y", "Page X", standalone number lines
        cleaned = cleaned.replaceAll("(?m)^\\s*第\\s*\\d+\\s*页.*$", "");
        cleaned = cleaned.replaceAll("(?mi)^\\s*Page\\s+\\d+(\\s+of\\s+\\d+)?\\s*$", "");
        cleaned = cleaned.replaceAll("(?m)^\\s*\\d+\\s*$", "");
        // Collapse 3+ consecutive blank lines to 2
        cleaned = cleaned.replaceAll("(\r?\\n){3,}", "\n\n");
        return cleaned.trim();
    }

    public record ParsedResult(String text, int pageCount, String warning) {
    }
}
