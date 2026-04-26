package com.carpeso.carpeso_backend.service;

import com.carpeso.carpeso_backend.model.Transaction;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import org.springframework.stereotype.Service;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import java.util.List;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class ReceiptService {

    private static final DeviceRgb RED = new DeviceRgb(220, 38, 38);
    private static final DeviceRgb LIGHT_GRAY = new DeviceRgb(249, 250, 251);
    private static final DeviceRgb DARK = new DeviceRgb(17, 24, 39);
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("MMMM dd, yyyy hh:mm a");

    public byte[] generateReceipt(Transaction t) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf, PageSize.A4);
        doc.setMargins(40, 50, 40, 50);

        PdfFont bold = PdfFontFactory.createFont("Helvetica-Bold");
        PdfFont regular = PdfFontFactory.createFont("Helvetica");

// Header — Red Bar
        Table header = new Table(UnitValue.createPercentArray(new float[]{1}))
                .setWidth(UnitValue.createPercentValue(100));
        Cell headerCell = new Cell()
                .setBackgroundColor(RED)
                .setPadding(20)
                .setBorderRadius(new com.itextpdf.layout.properties.BorderRadius(8));

// Add logo image
        try {
            String logoPath = "src/main/resources/static/logo.png";
            ImageData imageData = ImageDataFactory.create(logoPath);
            com.itextpdf.layout.element.Image logo =
                    new com.itextpdf.layout.element.Image(imageData);
            logo.setWidth(60).setHeight(60);
            logo.setHorizontalAlignment(
                    com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
            headerCell.add(logo);
        } catch (Exception e) {
            // Logo not found — skip
            System.out.println("Logo not found: " + e.getMessage());
        }

        headerCell.add(new Paragraph("CARPESO")
                .setFont(bold).setFontSize(28)
                .setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.CENTER));
        headerCell.add(new Paragraph("Drive the deal. Own the wheel.")
                .setFont(regular).setFontSize(11)
                .setFontColor(new DeviceRgb(254, 202, 202))
                .setTextAlignment(TextAlignment.CENTER));
        headerCell.add(new Paragraph("OFFICIAL SALES RECEIPT")
                .setFont(bold).setFontSize(13)
                .setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(6));
        header.addCell(headerCell.setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
        doc.add(header);

        doc.add(new Paragraph("\n"));

        // Receipt Number & Date
        Table meta = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .setWidth(UnitValue.createPercentValue(100));
        meta.addCell(new Cell()
                .add(new Paragraph("Receipt No.").setFont(regular).setFontSize(9)
                        .setFontColor(new DeviceRgb(107, 114, 128)))
                .add(new Paragraph(t.getReceiptNumber() != null ? t.getReceiptNumber() : "N/A")
                        .setFont(bold).setFontSize(13).setFontColor(RED))
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
        meta.addCell(new Cell()
                .add(new Paragraph("Date").setFont(regular).setFontSize(9)
                        .setFontColor(new DeviceRgb(107, 114, 128)))
                .add(new Paragraph(t.getCreatedAt() != null ? t.getCreatedAt().format(FMT) : "N/A")
                        .setFont(regular).setFontSize(10).setFontColor(DARK))
                .setTextAlignment(TextAlignment.RIGHT)
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
        doc.add(meta);

        doc.add(new LineSeparator(new SolidLine(1f))
                .setMarginTop(8).setMarginBottom(16));

        // Personal Letter
        doc.add(new Paragraph("\n"));
        doc.add(new Paragraph(
                "Dear " + (t.getBuyer() != null ? t.getBuyer().getFullName() : "Valued Customer") + ",")
                .setFont(bold).setFontSize(12)
                .setFontColor(DARK)
                .setMarginBottom(8));

        doc.add(new Paragraph(
                "Good day! We are pleased to inform you that your vehicle purchase has been " +
                        "successfully processed. This vehicle is now officially registered under your name. " +
                        "Thank you for choosing Carpeso as your trusted automotive marketplace.")
                .setFont(regular).setFontSize(11)
                .setFontColor(DARK)
                .setMarginBottom(8));

        doc.add(new Paragraph(
                "Please keep this receipt as proof of your purchase. Should you encounter any " +
                        "issues with your vehicle, you may file a warranty claim through our platform " +
                        "within the warranty period indicated below.")
                .setFont(regular).setFontSize(11)
                .setFontColor(DARK)
                .setMarginBottom(16));

        doc.add(new LineSeparator(new SolidLine(1f))
                .setMarginBottom(16));

        // Buyer Info
        doc.add(new Paragraph("BUYER INFORMATION")
                .setFont(bold).setFontSize(10)
                .setFontColor(RED)
                .setMarginBottom(8));

        Table buyerTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .setWidth(UnitValue.createPercentValue(100));

        addInfoRow(buyerTable, "Full Name",
                t.getBuyer() != null ? t.getBuyer().getFullName() : "N/A", bold, regular);
        addInfoRow(buyerTable, "Email",
                t.getBuyer() != null ? t.getBuyer().getEmail() : "N/A", bold, regular);
        addInfoRow(buyerTable, "Phone",
                t.getBuyer() != null && t.getBuyer().getPhone() != null
                        ? t.getBuyer().getPhone() : "N/A", bold, regular);
        addInfoRow(buyerTable, "Delivery Address",
                t.getDeliveryAddress() != null ? t.getDeliveryAddress() : "N/A", bold, regular);

        doc.add(buyerTable);

        doc.add(new LineSeparator(new SolidLine(1f))
                .setMarginTop(12).setMarginBottom(12));

        // Vehicle Info
        doc.add(new Paragraph("VEHICLE DETAILS")
                .setFont(bold).setFontSize(10)
                .setFontColor(RED)
                .setMarginBottom(8));

        Table vehicleTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .setWidth(UnitValue.createPercentValue(100));

        if (t.getVehicle() != null) {
            addInfoRow(vehicleTable, "Brand & Model",
                    t.getVehicle().getBrand() + " " + t.getVehicle().getModel(), bold, regular);
            addInfoRow(vehicleTable, "Year",
                    String.valueOf(t.getVehicle().getYear()), bold, regular);
            addInfoRow(vehicleTable, "Color",
                    t.getVehicle().getColor() != null ? t.getVehicle().getColor() : "N/A", bold, regular);
            addInfoRow(vehicleTable, "Plate Number",
                    t.getVehicle().getPlateNumber() != null ? t.getVehicle().getPlateNumber() : "N/A", bold, regular);
            addInfoRow(vehicleTable, "Engine Number",
                    t.getVehicle().getEngineNumber() != null ? t.getVehicle().getEngineNumber() : "N/A", bold, regular);
            addInfoRow(vehicleTable, "Chassis Number",
                    t.getVehicle().getChassisNumber() != null ? t.getVehicle().getChassisNumber() : "N/A", bold, regular);
        }

        doc.add(vehicleTable);

        doc.add(new LineSeparator(new SolidLine(1f))
                .setMarginTop(12).setMarginBottom(12));

        // Payment Info
        doc.add(new Paragraph("PAYMENT DETAILS")
                .setFont(bold).setFontSize(10)
                .setFontColor(RED)
                .setMarginBottom(8));

        Table payTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .setWidth(UnitValue.createPercentValue(100));

        addInfoRow(payTable, "Payment Mode",
                t.getPaymentMode() != null ? t.getPaymentMode().name().replace("_", " ") : "N/A",
                bold, regular);
        addInfoRow(payTable, "Transaction Status",
                t.getStatus() != null ? t.getStatus().name() : "N/A", bold, regular);

        doc.add(payTable);

        // Total Amount Box
        doc.add(new Paragraph("\n"));
        Table totalTable = new Table(UnitValue.createPercentArray(new float[]{1}))
                .setWidth(UnitValue.createPercentValue(100));
        Cell totalCell = new Cell()
                .setBackgroundColor(LIGHT_GRAY)
                .setPadding(16)
                .setBorderRadius(new com.itextpdf.layout.properties.BorderRadius(8))
                .setBorder(new com.itextpdf.layout.borders.SolidBorder(RED, 2));
        totalCell.add(new Paragraph("TOTAL AMOUNT")
                .setFont(regular).setFontSize(10)
                .setFontColor(new DeviceRgb(107, 114, 128))
                .setTextAlignment(TextAlignment.CENTER));
        totalCell.add(new Paragraph("₱ " + String.format("%,.2f",
                t.getAmount() != null ? t.getAmount().doubleValue() : 0))
                .setFont(bold).setFontSize(28)
                .setFontColor(RED)
                .setTextAlignment(TextAlignment.CENTER));
        totalTable.addCell(totalCell.setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
        doc.add(totalTable);

        // Warranty Info
        if (t.getWarrantyStartDate() != null) {
            doc.add(new Paragraph("\n"));
            doc.add(new LineSeparator(new SolidLine(1f))
                    .setMarginBottom(12));
            doc.add(new Paragraph("WARRANTY INFORMATION")
                    .setFont(bold).setFontSize(10)
                    .setFontColor(RED)
                    .setMarginBottom(8));

            Table warrantyTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                    .setWidth(UnitValue.createPercentValue(100));
            addInfoRow(warrantyTable, "Warranty Start",
                    t.getWarrantyStartDate().format(FMT), bold, regular);
            addInfoRow(warrantyTable, "Warranty End",
                    t.getWarrantyEndDate() != null ? t.getWarrantyEndDate().format(FMT) : "N/A",
                    bold, regular);
            if (t.getVehicle() != null && t.getVehicle().getWarrantyDetails() != null) {
                addInfoRow(warrantyTable, "Coverage",
                        t.getVehicle().getWarrantyDetails(), bold, regular);
            }
            doc.add(warrantyTable);
        }

        // Footer
        doc.add(new Paragraph("\n\n"));
        doc.add(new LineSeparator(new SolidLine(0.5f)).setMarginBottom(8));
        doc.add(new Paragraph("Thank you for choosing Carpeso!")
                .setFont(bold).setFontSize(11)
                .setFontColor(RED)
                .setTextAlignment(TextAlignment.CENTER));
        doc.add(new Paragraph("This is an official receipt. Please keep this for your records.")
                .setFont(regular).setFontSize(8)
                .setFontColor(new DeviceRgb(156, 163, 175))
                .setTextAlignment(TextAlignment.CENTER));
        doc.add(new Paragraph("Carpeso © 2026 — All Rights Reserved")
                .setFont(regular).setFontSize(8)
                .setFontColor(new DeviceRgb(156, 163, 175))
                .setTextAlignment(TextAlignment.CENTER));

        doc.close();
        return baos.toByteArray();
    }

    private void addInfoRow(Table table, String label, String value,
                            PdfFont bold, PdfFont regular) {
        table.addCell(new Cell()
                .add(new Paragraph(label).setFont(bold).setFontSize(9)
                        .setFontColor(new DeviceRgb(107, 114, 128)))
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
                .setPaddingBottom(6));
        table.addCell(new Cell()
                .add(new Paragraph(value).setFont(regular).setFontSize(10)
                        .setFontColor(new DeviceRgb(17, 24, 39)))
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
                .setPaddingBottom(6));
    }

    public byte[] generateSalesReport(
            List<com.carpeso.carpeso_backend.dto.response.TransactionResponse> transactions,
            String period) throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf, PageSize.A4);
        doc.setMargins(40, 50, 40, 50);

        PdfFont bold = PdfFontFactory.createFont("Helvetica-Bold");
        PdfFont regular = PdfFontFactory.createFont("Helvetica");

        // Header
        Table header = new Table(UnitValue.createPercentArray(new float[]{1}))
                .setWidth(UnitValue.createPercentValue(100));
        Cell headerCell = new Cell()
                .setBackgroundColor(RED)
                .setPadding(20)
                .setBorderRadius(new com.itextpdf.layout.properties.BorderRadius(8));

        try {
            String logoPath = "src/main/resources/static/logo.png";
            ImageData imageData = ImageDataFactory.create(logoPath);
            com.itextpdf.layout.element.Image logo =
                    new com.itextpdf.layout.element.Image(imageData);
            logo.setWidth(50).setHeight(50);
            logo.setHorizontalAlignment(
                    com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
            headerCell.add(logo);
        } catch (Exception e) {
            System.out.println("Logo not found: " + e.getMessage());
        }

        headerCell.add(new Paragraph("CARPESO")
                .setFont(bold).setFontSize(24)
                .setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.CENTER));
        headerCell.add(new Paragraph("SALES REPORT — " + period.toUpperCase())
                .setFont(bold).setFontSize(14)
                .setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(4));
        headerCell.add(new Paragraph("Generated: " +
                java.time.LocalDateTime.now().format(FMT))
                .setFont(regular).setFontSize(10)
                .setFontColor(new DeviceRgb(254, 202, 202))
                .setTextAlignment(TextAlignment.CENTER));
        header.addCell(headerCell.setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
        doc.add(header);
        doc.add(new Paragraph("\n"));

        // Filter by period
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        var filtered = transactions.stream().filter(t ->
                java.util.List.of("DELIVERED", "COMPLETED").contains(t.getStatus())
        ).filter(t -> {
            if (t.getCreatedAt() == null) return false;
            return switch (period) {
                case "day" -> t.getCreatedAt().toLocalDate().equals(now.toLocalDate());
                case "month" -> t.getCreatedAt().getMonth() == now.getMonth()
                        && t.getCreatedAt().getYear() == now.getYear();
                default -> t.getCreatedAt().getYear() == now.getYear();
            };
        }).collect(java.util.stream.Collectors.toList());

        // Summary
        double totalRevenue = filtered.stream()
                .mapToDouble(t -> t.getAmount() != null ? t.getAmount().doubleValue() : 0)
                .sum();

        doc.add(new Paragraph("SUMMARY")
                .setFont(bold).setFontSize(12)
                .setFontColor(RED).setMarginBottom(8));

        Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1}))
                .setWidth(UnitValue.createPercentValue(100));
        for (String[] cell : new String[][]{
                {"Total Orders", String.valueOf(filtered.size()), ""},
                {"Total Revenue", "₱" + String.format("%,.2f", totalRevenue), ""},
                {"Period", period.toUpperCase(), ""},
        }) {
            summaryTable.addCell(new Cell()
                    .add(new Paragraph(cell[0]).setFont(regular).setFontSize(9)
                            .setFontColor(new DeviceRgb(107, 114, 128)))
                    .add(new Paragraph(cell[1]).setFont(bold).setFontSize(14)
                            .setFontColor(RED))
                    .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
                    .setBackgroundColor(LIGHT_GRAY).setPadding(12));
        }
        doc.add(summaryTable);
        doc.add(new Paragraph("\n"));

        // Transactions Table
        doc.add(new Paragraph("TRANSACTIONS")
                .setFont(bold).setFontSize(12)
                .setFontColor(RED).setMarginBottom(8));

        Table table = new Table(UnitValue.createPercentArray(new float[]{0.5f, 2f, 2f, 1.5f, 1.5f, 1.5f}))
                .setWidth(UnitValue.createPercentValue(100));

        for (String h : new String[]{"#", "Buyer", "Vehicle", "Amount", "Payment", "Date"}) {
            table.addHeaderCell(new Cell()
                    .add(new Paragraph(h).setFont(bold).setFontSize(9)
                            .setFontColor(ColorConstants.WHITE))
                    .setBackgroundColor(RED)
                    .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
                    .setPadding(8));
        }

        for (var t : filtered) {
            String[] row = {
                    String.valueOf(t.getId()),
                    t.getBuyerFullName() != null ? t.getBuyerFullName() : "—",
                    (t.getVehicleBrand() != null ? t.getVehicleBrand() : "") + " " +
                            (t.getVehicleModel() != null ? t.getVehicleModel() : ""),
                    "₱" + String.format("%,.2f", t.getAmount() != null ? t.getAmount().doubleValue() : 0),
                    t.getPaymentMode() != null ? t.getPaymentMode().replace("_", " ") : "—",
                    t.getCreatedAt() != null ? t.getCreatedAt().format(
                            java.time.format.DateTimeFormatter.ofPattern("MM/dd/yyyy")) : "—",
            };
            for (String cell : row) {
                table.addCell(new Cell()
                        .add(new Paragraph(cell).setFont(regular).setFontSize(9))
                        .setBorder(new com.itextpdf.layout.borders.SolidBorder(
                                new DeviceRgb(229, 231, 235), 0.5f))
                        .setPadding(6));
            }
        }
        doc.add(table);

        // Footer
        doc.add(new Paragraph("\n\n"));
        doc.add(new Paragraph("© 2026 Carpeso — Confidential Sales Report")
                .setFont(regular).setFontSize(8)
                .setFontColor(new DeviceRgb(156, 163, 175))
                .setTextAlignment(TextAlignment.CENTER));

        doc.close();
        return baos.toByteArray();
    }
}