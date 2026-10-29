package com.verdantdays.service;

import com.verdantdays.model.*;
import com.verdantdays.model.dto.SalesImportResult;
import com.verdantdays.repository.ApartmentRepository;
import com.verdantdays.repository.CustomerRepository;
import com.verdantdays.repository.OrderRepository;
import com.verdantdays.repository.ProductRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One-shot historical sales import from the Verdant Days Google Sheet / Excel export.
 * Apartments must already exist in the app with the same name as the sheet "Apartment" column.
 * Does not deduct stock.
 */
@Service
public class SalesImportService {

    private static final Pattern FLAT_IN_NAME = Pattern.compile("\\(([^)]+)\\)");
    private static final DateTimeFormatter[] DATE_FORMATS = new DateTimeFormatter[]{
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("d-MMM-yyyy").toFormatter(Locale.ENGLISH),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("d-MMMM-yyyy").toFormatter(Locale.ENGLISH),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("dd-MMM-yyyy").toFormatter(Locale.ENGLISH),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("d/M/yyyy").toFormatter(Locale.ENGLISH),
            DateTimeFormatter.ISO_LOCAL_DATE
    };

    private static final List<ColumnQty> QTY_COLUMNS = List.of(
            new ColumnQty("Coconut Oil 500ml", Product.OilType.COCONUT, Product.Size.ML_500),
            new ColumnQty("Coconut Oil 1L", Product.OilType.COCONUT, Product.Size.L_1),
            new ColumnQty("Groundnut Oil 500ml", Product.OilType.GROUNDNUT, Product.Size.ML_500),
            new ColumnQty("Groundnut Oil 1Ltr", Product.OilType.GROUNDNUT, Product.Size.L_1),
            new ColumnQty("Gingelly Oil 500ml", Product.OilType.SESAME, Product.Size.ML_500),
            new ColumnQty("Gingelly Oil 1Ltr", Product.OilType.SESAME, Product.Size.L_1)
    );

    @Autowired
    private ApartmentRepository apartmentRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private OrderRepository orderRepository;

    public SalesImportResult importFile(MultipartFile file, boolean dryRun) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File is required");
        }
        String name = Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);
        List<Map<String, String>> rows;
        if (name.endsWith(".xlsx") || name.endsWith(".xls")) {
            rows = parseExcel(file.getInputStream());
        } else {
            rows = parseCsv(file.getInputStream());
        }
        return importRows(rows, dryRun);
    }

    public SalesImportResult importRows(List<Map<String, String>> rows, boolean dryRun) {
        SalesImportResult result = new SalesImportResult();
        result.setDryRun(dryRun);
        result.setTotalRows(rows.size());

        Map<String, Product> productsByKey = loadProductMap();
        for (ColumnQty col : QTY_COLUMNS) {
            if (!productsByKey.containsKey(key(col.oilType, col.size))) {
                result.addError("Missing active product for " + col.header + " (" + col.oilType + " / " + col.size + ")");
            }
        }
        if (result.getErrorCount() > 0) {
            return result;
        }

        Map<String, AtomicInteger> monthSeq = new HashMap<>();
        int createdCustomers = 0;
        int updatedCustomers = 0;

        for (int i = 0; i < rows.size(); i++) {
            int sheetRow = i + 2; // header is row 1
            Map<String, String> row = rows.get(i);
            try {
                RowOutcome outcome = processRow(row, sheetRow, productsByKey, monthSeq, dryRun, result);
                if (outcome.skipped) {
                    result.setSkippedCount(result.getSkippedCount() + 1);
                } else {
                    result.setSuccessCount(result.getSuccessCount() + 1);
                    createdCustomers += outcome.customersCreated;
                    updatedCustomers += outcome.customersUpdated;
                }
            } catch (Exception e) {
                result.addError("Row " + sheetRow + ": " + e.getMessage());
            }
        }

        result.setCustomersCreated(createdCustomers);
        result.setCustomersUpdated(updatedCustomers);
        return result;
    }

    private RowOutcome processRow(Map<String, String> row, int sheetRow, Map<String, Product> productsByKey,
                                  Map<String, AtomicInteger> monthSeq, boolean dryRun, SalesImportResult result) {
        String dateRaw = cell(row, "Date");
        String customerName = cell(row, "Customer Name");
        String apartmentName = cell(row, "Apartment");
        String phoneRaw = cell(row, "Customer Phone");
        String amountRaw = cell(row, "Amount");
        String statusRaw = cell(row, "Status");
        String paymentRaw = cell(row, "Payment Type");

        if (isBlank(dateRaw) && isBlank(customerName) && isBlank(amountRaw)) {
            return RowOutcome.skipped();
        }
        if (isBlank(dateRaw)) throw new RuntimeException("Date is required");
        if (isBlank(customerName)) throw new RuntimeException("Customer Name is required");
        if (isBlank(amountRaw)) throw new RuntimeException("Amount is required");

        LocalDate orderDate = parseDate(dateRaw);
        BigDecimal sheetAmount = parseDecimal(amountRaw);
        if (sheetAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Amount must be > 0");
        }

        List<LineQty> lines = new ArrayList<>();
        for (ColumnQty col : QTY_COLUMNS) {
            int qty = parseQty(cell(row, col.header));
            if (qty > 0) {
                Product p = productsByKey.get(key(col.oilType, col.size));
                lines.add(new LineQty(p, qty));
            }
        }
        if (lines.isEmpty()) {
            throw new RuntimeException("No product quantities found");
        }

        Apartment apartment = null;
        if (!isBlank(apartmentName)) {
            apartment = apartmentRepository.findFirstByNameIgnoreCaseAndActiveTrue(apartmentName.trim())
                    .orElseThrow(() -> new RuntimeException(
                            "Apartment not found: \"" + apartmentName.trim()
                                    + "\". Create it in the app with the exact same name, then re-import."));
        }

        Order.PaymentType paymentType = parsePaymentType(paymentRaw);
        Order.PaymentStatus paymentStatus = parsePaymentStatus(statusRaw);

        boolean walkIn = customerName.trim().equalsIgnoreCase("Walk-In")
                || customerName.trim().equalsIgnoreCase("Walk In");

        if (dryRun) {
            if (walkIn && apartment == null) {
                result.addWarning("Row " + sheetRow + ": walk-in order (no customer)");
            }
            return RowOutcome.success(0, 0);
        }

        Customer customer = null;
        int created = 0;
        int updated = 0;
        if (!(walkIn && apartment == null)) {
            ResolveCustomer rc = resolveCustomer(customerName.trim(), phoneRaw, apartment, walkIn);
            customer = rc.customer;
            created = rc.created ? 1 : 0;
            updated = rc.updated ? 1 : 0;
        }

        createHistoricalOrder(customer, lines, sheetAmount, paymentType, paymentStatus, orderDate, monthSeq);
        return RowOutcome.success(created, updated);
    }

    private void createHistoricalOrder(Customer customer, List<LineQty> lines, BigDecimal sheetAmount,
                                         Order.PaymentType paymentType, Order.PaymentStatus paymentStatus,
                                         LocalDate orderDate, Map<String, AtomicInteger> monthSeq) {
        Order order = new Order();
        order.setCustomer(customer);
        order.setOrderType(Order.OrderType.RETAIL);
        order.setOrderStatus(Order.OrderStatus.DELIVERED);
        order.setOrderSource(Order.OrderSource.ADMIN);
        order.setPaymentType(paymentType);
        order.setPaymentStatus(paymentStatus);
        order.setNotes("HISTORICAL_IMPORT");

        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> items = new ArrayList<>();
        for (LineQty line : lines) {
            BigDecimal unit = line.product.getPrice();
            BigDecimal lineTotal = unit.multiply(BigDecimal.valueOf(line.qty));
            subtotal = subtotal.add(lineTotal);
            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setProduct(line.product);
            oi.setQuantity(line.qty);
            oi.setUnitPrice(unit);
            oi.setSubtotal(lineTotal);
            items.add(oi);
        }

        BigDecimal discount = subtotal.subtract(sheetAmount);
        if (discount.compareTo(BigDecimal.ZERO) < 0) {
            // Sheet total higher than current catalog: keep sheet as total, zero discount
            discount = BigDecimal.ZERO;
        }
        BigDecimal total = sheetAmount.setScale(2, RoundingMode.HALF_UP);

        order.setItems(items);
        order.setSubtotalAmount(subtotal.setScale(2, RoundingMode.HALF_UP));
        order.setDiscountAmount(discount.setScale(2, RoundingMode.HALF_UP));
        order.setTotalAmount(total);
        if (paymentStatus == Order.PaymentStatus.PAID) {
            order.setAmountPaid(total);
        } else if (paymentStatus == Order.PaymentStatus.DUE) {
            order.setAmountPaid(BigDecimal.ZERO);
        } else {
            order.setAmountPaid(total);
        }

        LocalDateTime createdAt = orderDate.atTime(12, 0);
        order.setCreatedAt(createdAt);
        order.setOrderNumber(nextOrderNumber(orderDate, monthSeq));

        orderRepository.save(order);
    }

    /** YYMMDD-NN with sequence resetting each calendar month (same as live orders). */
    private String nextOrderNumber(LocalDate date, Map<String, AtomicInteger> monthSeq) {
        String datePart = String.format("%02d%02d%02d", date.getYear() % 100, date.getMonthValue(), date.getDayOfMonth());
        String monthKey = String.format("%02d%02d", date.getYear() % 100, date.getMonthValue());
        AtomicInteger seq = monthSeq.computeIfAbsent(monthKey, k -> new AtomicInteger(maxSequenceForYearMonth(k)));
        int n = seq.incrementAndGet();
        return datePart + "-" + String.format("%02d", n);
    }

    private int maxSequenceForYearMonth(String ymPrefix) {
        int max = 0;
        for (String num : orderRepository.findOrderNumbersByYearMonthPrefix(ymPrefix)) {
            if (num == null) continue;
            int dash = num.lastIndexOf('-');
            if (dash < 0 || dash == num.length() - 1) continue;
            String head = num.substring(0, dash);
            if (head.length() != 6 || !head.startsWith(ymPrefix)) continue;
            try {
                max = Math.max(max, Integer.parseInt(num.substring(dash + 1)));
            } catch (NumberFormatException ignored) {
            }
        }
        return max;
    }

    private ResolveCustomer resolveCustomer(String name, String phoneRaw, Apartment apartment, boolean walkIn) {
        String phone = normalizePhone(phoneRaw);
        String flat = extractFlat(name);

        if (phone != null) {
            Optional<Customer> byPhone = customerRepository.findFirstByPhoneNumberAndActiveTrueOrderByIdAsc(phone);
            if (byPhone.isPresent()) {
                Customer c = byPhone.get();
                boolean changed = false;
                if (!c.getName().equalsIgnoreCase(name) && !walkIn) {
                    // keep existing name; only fill missing apartment/flat
                }
                if (apartment != null && c.getApartment() == null) {
                    c.setApartment(apartment);
                    changed = true;
                }
                if (flat != null && (c.getFlatNumber() == null || c.getFlatNumber().isBlank())) {
                    c.setFlatNumber(flat);
                    changed = true;
                }
                if (changed) {
                    customerRepository.save(c);
                    return new ResolveCustomer(c, false, true);
                }
                return new ResolveCustomer(c, false, false);
            }
        }

        Optional<Customer> byName;
        if (apartment != null) {
            byName = customerRepository.findFirstByNameIgnoreCaseAndApartmentIdAndActiveTrueOrderByIdAsc(name, apartment.getId());
        } else {
            byName = customerRepository.findFirstByNameIgnoreCaseAndApartmentIsNullAndActiveTrueOrderByIdAsc(name);
        }
        if (byName.isPresent()) {
            Customer c = byName.get();
            boolean changed = false;
            if (phone != null && (c.getPhoneNumber() == null || c.getPhoneNumber().isBlank())) {
                c.setPhoneNumber(phone);
                changed = true;
            }
            if (flat != null && (c.getFlatNumber() == null || c.getFlatNumber().isBlank())) {
                c.setFlatNumber(flat);
                changed = true;
            }
            if (changed) {
                customerRepository.save(c);
                return new ResolveCustomer(c, false, true);
            }
            return new ResolveCustomer(c, false, false);
        }

        Customer c = new Customer();
        c.setName(walkIn ? "Walk-In" : name);
        c.setPhoneNumber(phone);
        c.setApartment(apartment);
        c.setFlatNumber(flat);
        c.setActive(true);
        c.setSubscribedMonthly(false);
        customerRepository.save(c);
        return new ResolveCustomer(c, true, false);
    }

    private Map<String, Product> loadProductMap() {
        Map<String, Product> map = new HashMap<>();
        for (Product p : productRepository.findByActiveTrueOrderByDisplayNameAsc()) {
            if (p.getOilType() != null && p.getSize() != null) {
                map.putIfAbsent(key(p.getOilType(), p.getSize()), p);
            }
        }
        return map;
    }

    private static String key(Product.OilType oilType, Product.Size size) {
        return oilType.name() + "|" + size.name();
    }

    private List<Map<String, String>> parseCsv(InputStream in) throws Exception {
        List<Map<String, String>> rows = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String headerLine = br.readLine();
            if (headerLine == null) return rows;
            // strip BOM
            if (!headerLine.isEmpty() && headerLine.charAt(0) == '\uFEFF') {
                headerLine = headerLine.substring(1);
            }
            String[] headers = splitCsvLine(headerLine);
            for (int h = 0; h < headers.length; h++) {
                headers[h] = normalizeHeader(headers[h]);
            }
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] cols = splitCsvLine(line);
                Map<String, String> map = new LinkedHashMap<>();
                for (int i = 0; i < headers.length; i++) {
                    String key = headers[i];
                    if (key.isEmpty()) continue;
                    String val = i < cols.length ? cols[i].trim() : "";
                    map.put(key, val);
                }
                rows.add(map);
            }
        }
        return rows;
    }

    private List<Map<String, String>> parseExcel(InputStream in) throws Exception {
        List<Map<String, String>> rows = new ArrayList<>();
        try (Workbook wb = new XSSFWorkbook(in)) {
            Sheet sheet = wb.getNumberOfSheets() > 0 ? wb.getSheetAt(0) : null;
            if (sheet == null) return rows;
            Iterator<Row> it = sheet.iterator();
            if (!it.hasNext()) return rows;
            Row headerRow = it.next();
            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(normalizeHeader(cellToString(cell)));
            }
            DataFormatter formatter = new DataFormatter(Locale.ENGLISH);
            while (it.hasNext()) {
                Row r = it.next();
                if (r == null) continue;
                Map<String, String> map = new LinkedHashMap<>();
                boolean any = false;
                for (int i = 0; i < headers.size(); i++) {
                    String key = headers.get(i);
                    if (key.isEmpty()) continue;
                    Cell cell = r.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    String val = cell == null ? "" : formatter.formatCellValue(cell).trim();
                    if (!val.isEmpty()) any = true;
                    map.put(key, val);
                }
                if (any) rows.add(map);
            }
        }
        return rows;
    }

    private static String[] splitCsvLine(String line) {
        List<String> parts = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                parts.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        parts.add(cur.toString());
        return parts.toArray(new String[0]);
    }

    private static String normalizeHeader(String h) {
        if (h == null) return "";
        return h.trim().replace('\u00A0', ' ').replaceAll("\\s+", " ");
    }

    private static String cell(Map<String, String> row, String key) {
        String v = row.get(key);
        if (v != null) return v.trim();
        // case-insensitive fallback
        for (Map.Entry<String, String> e : row.entrySet()) {
            if (e.getKey() != null && e.getKey().equalsIgnoreCase(key)) {
                return e.getValue() != null ? e.getValue().trim() : "";
            }
        }
        return "";
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String cellToString(Cell cell) {
        if (cell == null) return "";
        return new DataFormatter(Locale.ENGLISH).formatCellValue(cell).trim();
    }

    static LocalDate parseDate(String raw) {
        String s = raw.trim().replaceAll("\\s+", " ");
        // Fix typo years like 0206 → 2026
        Matcher m = Pattern.compile("^(\\d{1,2}-[A-Za-z]+-)0(\\d{3})$").matcher(s);
        if (m.matches()) {
            s = m.group(1) + "2" + m.group(2);
        }
        for (DateTimeFormatter f : DATE_FORMATS) {
            try {
                return LocalDate.parse(s, f);
            } catch (DateTimeParseException ignored) {
            }
        }
        throw new RuntimeException("Unrecognized date: " + raw);
    }

    private static int parseQty(String raw) {
        if (isBlank(raw)) return 0;
        try {
            BigDecimal d = new BigDecimal(raw.trim().replace(",", ""));
            return d.setScale(0, RoundingMode.HALF_UP).intValueExact();
        } catch (Exception e) {
            throw new RuntimeException("Invalid quantity: " + raw);
        }
    }

    private static BigDecimal parseDecimal(String raw) {
        try {
            return new BigDecimal(raw.trim().replace(",", "")).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            throw new RuntimeException("Invalid amount: " + raw);
        }
    }

    private static String normalizePhone(String raw) {
        if (isBlank(raw)) return null;
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.isEmpty() || digits.matches("0+")) return null;
        if (digits.length() > 15) digits = digits.substring(digits.length() - 15);
        return digits;
    }

    private static String extractFlat(String name) {
        Matcher m = FLAT_IN_NAME.matcher(name);
        if (m.find()) {
            String f = m.group(1).trim();
            return f.isEmpty() ? null : f;
        }
        return null;
    }

    private static Order.PaymentType parsePaymentType(String raw) {
        if (isBlank(raw)) return Order.PaymentType.CASH;
        String s = raw.trim().toUpperCase(Locale.ROOT);
        if (s.contains("UPI") || s.contains("GPAY") || s.contains("G-PAY") || s.contains("PHONEPE") || s.contains("PAYTM")) {
            return Order.PaymentType.UPI;
        }
        if (s.contains("CASH")) return Order.PaymentType.CASH;
        return Order.PaymentType.CASH;
    }

    private static Order.PaymentStatus parsePaymentStatus(String raw) {
        if (isBlank(raw)) return Order.PaymentStatus.PAID;
        String s = raw.trim().toUpperCase(Locale.ROOT);
        if (s.contains("DUE") || s.contains("UNPAID") || s.contains("PENDING")) return Order.PaymentStatus.DUE;
        if (s.contains("PARTIAL")) return Order.PaymentStatus.PARTIAL_PAID;
        return Order.PaymentStatus.PAID;
    }

    private record ColumnQty(String header, Product.OilType oilType, Product.Size size) {}
    private record LineQty(Product product, int qty) {}
    private record ResolveCustomer(Customer customer, boolean created, boolean updated) {}

    private static final class RowOutcome {
        final int customersCreated;
        final int customersUpdated;
        final boolean skipped;

        private RowOutcome(boolean skipped, int created, int updated) {
            this.skipped = skipped;
            this.customersCreated = created;
            this.customersUpdated = updated;
        }

        static RowOutcome success(int created, int updated) {
            return new RowOutcome(false, created, updated);
        }

        static RowOutcome skipped() {
            return new RowOutcome(true, 0, 0);
        }
    }
}
