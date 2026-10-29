package com.verdantdays.controller;

import com.verdantdays.model.dto.ApiResponse;
import com.verdantdays.model.dto.SalesImportResult;
import com.verdantdays.service.SalesImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/import")
@Tag(name = "Sales Import", description = "One-shot historical sales upload from Google Sheet / Excel")
public class SalesImportController {

    @Autowired
    private SalesImportService salesImportService;

    @Operation(summary = "Import historical sales CSV/XLSX",
            description = """
                    Upload the sheet export (CSV or XLSX).
                    Apartments must already exist with the same names as the Apartment column.
                    Use dryRun=true first to validate, then dryRun=false to create customers + orders.
                    Does not deduct stock. Do not run the real import more than once.
                    """)
    @PostMapping(value = "/sales", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> importSales(
            @Parameter(description = "CSV or XLSX file", required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                            schema = @Schema(type = "string", format = "binary")))
            @RequestPart("file") MultipartFile file,
            @Parameter(description = "Validate only when true")
            @RequestParam(value = "dryRun", defaultValue = "false") boolean dryRun) {
        try {
            SalesImportResult result = salesImportService.importFile(file, dryRun);
            String msg = dryRun
                    ? "Dry run complete"
                    : "Import complete: " + result.getSuccessCount() + " orders, "
                      + result.getErrorCount() + " errors";
            int code = result.getErrorCount() > 0 && result.getSuccessCount() == 0 ? 400 : 200;
            return ResponseEntity.status(code).body(new ApiResponse<>(msg, result, code));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(e.getMessage(), null, 400));
        }
    }
}
