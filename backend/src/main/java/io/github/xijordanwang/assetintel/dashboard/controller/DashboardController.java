package io.github.xijordanwang.assetintel.dashboard.controller;

import io.github.xijordanwang.assetintel.dashboard.dto.DashboardView;
import io.github.xijordanwang.assetintel.dashboard.dto.PlantView;
import io.github.xijordanwang.assetintel.dashboard.service.DashboardService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboard;
    private final Clock clock;

    public DashboardController(DashboardService dashboard, Clock clock) {
        this.dashboard = dashboard;
        this.clock = clock;
    }

    @GetMapping("/overview")
    public DashboardView overview(@RequestParam(required = false)
                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return dashboard.overview(asOf != null ? asOf : LocalDate.now(clock));
    }

    /** A plant at the end of a month, e.g. {@code /api/dashboard/plants/AKL?upTo=2026-08}. Defaults to today. */
    @GetMapping("/plants/{plantCode}")
    public PlantView plant(@PathVariable String plantCode, @RequestParam(required = false) YearMonth upTo) {
        LocalDate today = LocalDate.now(clock);
        LocalDate asOf = upTo == null || !upTo.atEndOfMonth().isBefore(today) ? today : upTo.atEndOfMonth();
        return dashboard.plant(plantCode.toUpperCase(), asOf);
    }
}
