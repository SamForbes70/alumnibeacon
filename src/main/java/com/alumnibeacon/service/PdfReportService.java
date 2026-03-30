package com.alumnibeacon.service;

import com.alumnibeacon.model.Investigation;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

@Service
public class PdfReportService {

    private static final Logger log = LoggerFactory.getLogger(PdfReportService.class);

    // AlumniBeacon brand colours
    private static final DeviceRgb CHARCOAL    = new DeviceRgb(0x1C, 0x25, 0x26);
    private static final DeviceRgb MAGENTA     = new DeviceRgb(0x9C, 0x2A, 0x6B);
    private static final DeviceRgb GOLD        = new DeviceRgb(0xC9, 0xA6, 0x6B);
    private static final DeviceRgb LIGHT_GREY  = new DeviceRgb(0xF9, 0xFA, 0xFB);
    private static final DeviceRgb MID_GREY    = new DeviceRgb(0x6B, 0x72, 0x80);

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMMM yyyy");

    public byte[] generateReport(Investigation inv) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf  = new PdfDocument(writer);
            Document doc     = new Document(pdf, PageSize.A4);
            doc.setMargins(40, 50, 40, 50);

            addHeader(doc, inv);
            addDivider(doc);
            addSubjectSection(doc, inv);
            addDivider(doc);
            addFindingsSection(doc, inv);
            addDivider(doc);
            addConfidenceSection(doc, inv);
            addDivider(doc);
            addSourcesSection(doc, inv);
            addFooter(doc, inv);

            doc.close();
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("PDF generation failed for investigation {}", inv.getId(), e);
            throw new RuntimeException("PDF generation failed", e);
        }
    }

    // ── Header ──────────────────────────────────────────────────────────────
    private void addHeader(Document doc, Investigation inv) {
        // Brand bar
        Table headerTable = new Table(UnitValue.createPercentArray(new float[]{70, 30}))
                .useAllAvailableWidth();

        Cell titleCell = new Cell()
                .add(new Paragraph("AlumniBeacon")
                        .setFontSize(22)
                        .setBold()
                        .setFontColor(MAGENTA))
                .add(new Paragraph("OSINT Investigation Report")
                        .setFontSize(10)
                        .setFontColor(MID_GREY))
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
                .setPaddingBottom(8);

        Cell dateCell = new Cell()
                .add(new Paragraph("Report Generated")
                        .setFontSize(8)
                        .setFontColor(MID_GREY)
                        .setTextAlignment(TextAlignment.RIGHT))
                .add(new Paragraph(inv.getUpdatedAt() != null
                        ? inv.getUpdatedAt().format(DATE_FMT)
                        : "—")
                        .setFontSize(10)
                        .setFontColor(CHARCOAL)
                        .setTextAlignment(TextAlignment.RIGHT))
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
                .setPaddingBottom(8);

        headerTable.addCell(titleCell);
        headerTable.addCell(dateCell);
        doc.add(headerTable);
    }

    // ── Subject section ──────────────────────────────────────────────────────
    private void addSubjectSection(Document doc, Investigation inv) {
        doc.add(sectionHeading("SUBJECT DETAILS"));

        Table t = new Table(UnitValue.createPercentArray(new float[]{30, 70})).useAllAvailableWidth();
        addRow(t, "Full Name",    inv.getSubjectName());
        addRow(t, "Date of Birth", inv.getSubjectDob() != null ? inv.getSubjectDob() : "Not provided");
        addRow(t, "Last Known Location", inv.getSubjectLastKnownAddress() != null ? inv.getSubjectLastKnownAddress() : "Not provided");
        addRow(t, "Last Known Employer", inv.getSubjectLastKnownEmployer() != null ? inv.getSubjectLastKnownEmployer() : "Not provided");
        addRow(t, "Investigation ID", inv.getId().toString());
        addRow(t, "Status",        inv.getStatus().name());
        doc.add(t);
    }

    // ── Findings section ─────────────────────────────────────────────────────
    private void addFindingsSection(Document doc, Investigation inv) {
        doc.add(sectionHeading("INVESTIGATION FINDINGS"));

        if (inv.getResultJson() == null || inv.getResultJson().isBlank()) {
            doc.add(new Paragraph("No findings available.").setFontColor(MID_GREY).setFontSize(10));
            return;
        }

        // Parse result JSON and display key fields
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode result = mapper.readTree(inv.getResultJson());

            Table t = new Table(UnitValue.createPercentArray(new float[]{30, 70})).useAllAvailableWidth();

            addRowIfPresent(t, "Email Address",  result, "found_email");
            addRowIfPresent(t, "Phone Number",   result, "found_phone");
            addRowIfPresent(t, "Current Address",result, "found_address");
            addRowIfPresent(t, "LinkedIn Profile",result, "found_linkedin");
            addRowIfPresent(t, "Current Employer",result, "found_employer");
            addRowIfPresent(t, "Summary",        result, "summary");

            doc.add(t);

        } catch (Exception e) {
            doc.add(new Paragraph("Results available but could not be parsed for display.")
                    .setFontColor(MID_GREY).setFontSize(10));
        }
    }

    // ── Confidence section ───────────────────────────────────────────────────
    private void addConfidenceSection(Document doc, Investigation inv) {
        doc.add(sectionHeading("CONFIDENCE ASSESSMENT"));

        Integer score = inv.getConfidenceScore();
        String label  = score == null ? "N/A" :
                        score >= 70   ? "HIGH" :
                        score >= 40   ? "MEDIUM" : "LOW";
        DeviceRgb colour = score == null ? MID_GREY :
                           score >= 70  ? new DeviceRgb(0x05, 0x96, 0x69) :
                           score >= 40  ? new DeviceRgb(0xD9, 0x77, 0x06) :
                                          new DeviceRgb(0xDC, 0x26, 0x26);

        Paragraph scorePara = new Paragraph()
                .add(new Text(score != null ? score + "% " : "N/A ").setFontSize(28).setBold().setFontColor(colour))
                .add(new Text(label).setFontSize(14).setFontColor(colour));
        doc.add(scorePara);

        doc.add(new Paragraph(
                "Confidence score reflects the quality and quantity of corroborating data sources. " +
                "Scores above 70% indicate high reliability. Scores below 40% require manual verification.")
                .setFontSize(9).setFontColor(MID_GREY).setMarginTop(4));
    }

    // ── Sources section ──────────────────────────────────────────────────────
    private void addSourcesSection(Document doc, Investigation inv) {
        doc.add(sectionHeading("DATA SOURCES"));

        try {
            if (inv.getResultJson() != null) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode result = mapper.readTree(inv.getResultJson());
                com.fasterxml.jackson.databind.JsonNode sources = result.get("sources");

                if (sources != null && sources.isArray()) {
                    List list = new List().setSymbolIndent(8).setListSymbol("•");
                    for (com.fasterxml.jackson.databind.JsonNode src : sources) {
                        ListItem item = new ListItem();
                        item.add(new Paragraph(src.asText()).setFontSize(10).setFontColor(CHARCOAL));
                        list.add(item);
                    }
                    doc.add(list);
                    return;
                }
            }
        } catch (Exception ignored) {}

        doc.add(new Paragraph("Source information not available.").setFontColor(MID_GREY).setFontSize(10));
    }

    // ── Footer ───────────────────────────────────────────────────────────────
    private void addFooter(Document doc, Investigation inv) {
        doc.add(new Paragraph("\n"));
        addDivider(doc);
        doc.add(new Paragraph(
                "CONFIDENTIAL — This report is generated by AlumniBeacon and is intended solely for the " +
                "authorised recipient. The information contained herein has been obtained from publicly " +
                "available sources and is provided for legitimate alumni reconnection purposes only. " +
                "Use of this report must comply with the Australian Privacy Act 1988 and applicable data " +
                "protection legislation. AlumniBeacon accepts no liability for decisions made based on " +
                "this report without independent verification.")
                .setFontSize(7)
                .setFontColor(MID_GREY)
                .setMarginTop(8));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private Paragraph sectionHeading(String text) {
        return new Paragraph(text)
                .setFontSize(9)
                .setBold()
                .setFontColor(MAGENTA)
                .setMarginTop(16)
                .setMarginBottom(6)
                .setCharacterSpacing(1.2f);
    }

    private void addDivider(Document doc) {
        doc.add(new LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(0.5f))
                .setStrokeColor(new DeviceRgb(0xE5, 0xE7, 0xEB))
                .setMarginTop(4)
                .setMarginBottom(4));
    }

    private void addRow(Table t, String label, String value) {
        t.addCell(new Cell()
                .add(new Paragraph(label).setFontSize(9).setFontColor(MID_GREY).setBold())
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
                .setBackgroundColor(LIGHT_GREY)
                .setPadding(6));
        t.addCell(new Cell()
                .add(new Paragraph(value != null ? value : "—").setFontSize(10).setFontColor(CHARCOAL))
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
                .setPadding(6));
    }

    private void addRowIfPresent(Table t, String label,
                                  com.fasterxml.jackson.databind.JsonNode node, String field) {
        com.fasterxml.jackson.databind.JsonNode val = node.get(field);
        if (val != null && !val.isNull() && !val.asText().isBlank()) {
            addRow(t, label, val.asText());
        }
    }
}
