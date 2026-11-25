package io.github.xijordanwang.assetintel.export.controller;

import io.github.xijordanwang.assetintel.asset.entity.Asset;
import io.github.xijordanwang.assetintel.asset.entity.AssetCategory;
import io.github.xijordanwang.assetintel.asset.repository.AssetRepository;
import io.github.xijordanwang.assetintel.asset.entity.AssetStatus;
import io.github.xijordanwang.assetintel.asset.service.DepreciationCalculator;
import io.github.xijordanwang.assetintel.change.entity.AssetChange;
import io.github.xijordanwang.assetintel.change.repository.AssetChangeRepository;
import io.github.xijordanwang.assetintel.change.entity.AssetChangeStep;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/** Excel downloads of the asset register and the change forms, with the approval trail flattened per form. */
@RestController
@RequestMapping("/api/export")
@Transactional(readOnly = true)
public class ExcelExportController {

    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final AssetRepository assets;
    private final AssetChangeRepository changes;
    private final Clock clock;

    public ExcelExportController(AssetRepository assets, AssetChangeRepository changes, Clock clock) {
        this.assets = assets;
        this.changes = changes;
        this.clock = clock;
    }

    @GetMapping("/assets.xlsx")
    public void assets(@RequestParam(required = false) String site,
                       @RequestParam(required = false) AssetCategory category,
                       @RequestParam(required = false) AssetStatus status,
                       HttpServletResponse response) throws IOException {
        LocalDate today = LocalDate.now(clock);
        List<Asset> rows = assets.findAllWithSite().stream()
                .filter(a -> site == null || site.isBlank() || a.getSite().getCode().equals(site))
                .filter(a -> category == null || a.getCategory() == category)
                .filter(a -> status == null || a.getStatus() == status)
                .sorted(Comparator.comparing(Asset::getTag))
                .toList();
        write(response, "asset-register-" + today + ".xlsx", "Asset register", List.of(
                col("Tag", Asset::getTag),
                col("Name", Asset::getName),
                col("Class", a -> a.getCategory().name()),
                col("Status", a -> a.getStatus().name()),
                col("Plant", a -> a.getSite().getPlantCode()),
                col("Site", a -> a.getSite().getCode()),
                col("Custodian", Asset::getCustodian),
                col("Supplier", Asset::getSupplier),
                col("Original value (NZD)", Asset::getOriginalValue),
                col("Accumulated depreciation (NZD)", a -> DepreciationCalculator.accumulatedDepreciation(a, today)),
                col("Impairment (NZD)", Asset::getImpairment),
                col("Net book value (NZD)", a -> DepreciationCalculator.netBookValue(a, today)),
                col("In service", Asset::getInServiceDate),
                col("Last verified", Asset::getLastVerifiedDate),
                col("Disposed", Asset::getDisposedDate),
                col("Open form", Asset::getLockedByChange)), rows);
    }

    @GetMapping("/changes.xlsx")
    public void changes(HttpServletResponse response) throws IOException {
        List<AssetChange> rows = changes.findAllByOrderByCreatedAtDesc();
        write(response, "asset-changes-" + LocalDate.now(clock) + ".xlsx", "Transfers, disposals, changes", List.of(
                col("Reference", AssetChange::getReference),
                col("Type", c -> c.getType().name()),
                col("Status", c -> c.getStatus().name()),
                col("Asset", c -> c.getAsset().getTag()),
                col("Asset name", c -> c.getAsset().getName()),
                col("Target site", c -> c.getTargetSite() != null ? c.getTargetSite().getCode() : null),
                col("Disposal reason", c -> c.getDisposalReason() != null ? c.getDisposalReason().name() : null),
                col("NBV at submission (NZD)", AssetChange::getNetBookValue),
                col("Requested by", AssetChange::getRequestedBy),
                col("Created", c -> c.getCreatedAt().toString()),
                col("Approval trail", ExcelExportController::trail)), rows);
    }

    private static String trail(AssetChange c) {
        StringBuilder sb = new StringBuilder();
        for (AssetChangeStep s : c.getSteps()) {
            if (!sb.isEmpty()) sb.append(" → ");
            sb.append(s.getApproverRole()).append(": ").append(s.getDecision());
            if (s.getDecidedBy() != null) sb.append(" by ").append(s.getDecidedBy());
        }
        return sb.toString();
    }

    record Column<T>(String header, Function<T, Object> value) {
    }

    private static <T> Column<T> col(String header, Function<T, Object> value) {
        return new Column<>(header, value);
    }

    private static <T> void write(HttpServletResponse response, String filename, String sheetName,
                                  List<Column<T>> columns, List<T> rows) throws IOException {
        response.setContentType(XLSX);
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            SXSSFSheet sheet = wb.createSheet(sheetName);
            Font bold = wb.createFont();
            bold.setBold(true);
            CellStyle header = wb.createCellStyle();
            header.setFont(bold);
            CellStyle moneyStyle = wb.createCellStyle();
            moneyStyle.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
            CellStyle dateStyle = wb.createCellStyle();
            dateStyle.setDataFormat(wb.createDataFormat().getFormat("yyyy-mm-dd"));

            Row head = sheet.createRow(0);
            for (int i = 0; i < columns.size(); i++) {
                Cell cell = head.createCell(i);
                cell.setCellValue(columns.get(i).header());
                cell.setCellStyle(header);
                sheet.setColumnWidth(i, Math.max(12, columns.get(i).header().length() + 2) * 256);
            }
            sheet.createFreezePane(0, 1);
            int r = 1;
            for (T item : rows) {
                Row row = sheet.createRow(r++);
                for (int i = 0; i < columns.size(); i++) {
                    Object v = columns.get(i).value().apply(item);
                    Cell cell = row.createCell(i);
                    if (v instanceof BigDecimal d) {
                        cell.setCellValue(d.doubleValue());
                        cell.setCellStyle(moneyStyle);
                    } else if (v instanceof LocalDate d) {
                        cell.setCellValue(d);
                        cell.setCellStyle(dateStyle);
                    } else if (v != null) {
                        cell.setCellValue(v.toString());
                    }
                }
            }
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, Math.max(0, r - 1), 0, columns.size() - 1));
            wb.write(response.getOutputStream());
        }
    }
}
