package com.verdantdays.service;

import com.verdantdays.model.Customer;
import com.verdantdays.model.Order;
import com.verdantdays.model.OrderItem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExcelExportService {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    public byte[] exportCustomers(List<Customer> customers) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Customers");
            CellStyle header = headerStyle(wb);
            String[] cols = {
                    "Name", "Phone", "Alternate Phone", "Email", "Address",
                    "Apartment", "Flat", "Subscribed", "Created At"
            };
            writeHeader(sheet, cols, header);
            int r = 1;
            for (Customer c : customers) {
                Row row = sheet.createRow(r++);
                int col = 0;
                set(row, col++, c.getName());
                set(row, col++, c.getPhoneNumber());
                set(row, col++, c.getAlternatePhone());
                set(row, col++, c.getEmail());
                set(row, col++, c.getAddress());
                set(row, col++, c.getApartment() != null ? c.getApartment().getName() : "");
                set(row, col++, c.getFlatNumber());
                set(row, col++, c.isSubscribedMonthly() ? "Yes" : "No");
                set(row, col, c.getCreatedAt() != null ? c.getCreatedAt().format(DT) : "");
            }
            autosize(sheet, cols.length);
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to export customers Excel", e);
        }
    }

    public byte[] exportOrders(List<Order> orders) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Orders");
            CellStyle header = headerStyle(wb);
            String[] cols = {
                    "Order Number", "Date", "Customer Name", "Customer Phone", "Apartment",
                    "Order Type", "Order Status", "Payment Type", "Payment Status",
                    "Subtotal", "Discount", "Total", "Amount Paid", "Items"
            };
            writeHeader(sheet, cols, header);
            int r = 1;
            for (Order o : orders) {
                Row row = sheet.createRow(r++);
                int col = 0;
                set(row, col++, o.getOrderNumber() != null ? o.getOrderNumber() : String.valueOf(o.getId()));
                set(row, col++, o.getCreatedAt() != null ? o.getCreatedAt().format(DT) : "");
                set(row, col++, o.getCustomer() != null ? o.getCustomer().getName() : "Walk-in");
                set(row, col++, o.getCustomer() != null ? o.getCustomer().getPhoneNumber() : "");
                String apt = "";
                if (o.getCustomer() != null && o.getCustomer().getApartment() != null) {
                    apt = o.getCustomer().getApartment().getName();
                }
                set(row, col++, apt);
                set(row, col++, o.getOrderType() != null ? o.getOrderType().name() : "");
                set(row, col++, o.getOrderStatus() != null ? o.getOrderStatus().name() : "");
                set(row, col++, o.getPaymentType() != null ? o.getPaymentType().name() : "");
                set(row, col++, o.getPaymentStatus() != null ? o.getPaymentStatus().name() : "");
                set(row, col++, o.getSubtotalAmount() != null ? o.getSubtotalAmount().toPlainString() : "0");
                set(row, col++, o.getDiscountAmount() != null ? o.getDiscountAmount().toPlainString() : "0");
                set(row, col++, o.getTotalAmount() != null ? o.getTotalAmount().toPlainString() : "0");
                set(row, col++, o.getAmountPaid() != null ? o.getAmountPaid().toPlainString() : "0");
                set(row, col, formatItems(o));
            }
            autosize(sheet, cols.length);
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to export orders Excel", e);
        }
    }

    private static String formatItems(Order o) {
        if (o.getItems() == null || o.getItems().isEmpty()) return "";
        return o.getItems().stream().map(ExcelExportService::itemLabel).collect(Collectors.joining("; "));
    }

    private static String itemLabel(OrderItem i) {
        String name = i.getProduct() != null && i.getProduct().getDisplayName() != null
                ? i.getProduct().getDisplayName() : "Item";
        return name + " x " + i.getQuantity();
    }

    private static CellStyle headerStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private static void writeHeader(Sheet sheet, String[] cols, CellStyle style) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < cols.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(cols[i]);
            cell.setCellStyle(style);
        }
    }

    private static void set(Row row, int col, String value) {
        row.createCell(col).setCellValue(value != null ? value : "");
    }

    private static void autosize(Sheet sheet, int cols) {
        for (int i = 0; i < cols; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}
