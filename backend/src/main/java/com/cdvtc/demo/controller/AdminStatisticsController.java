package com.cdvtc.demo.controller;

import com.cdvtc.demo.common.Result;
import com.cdvtc.demo.service.HealthStatisticsService;
import com.cdvtc.demo.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端全局健康统计。数据范围为全库（不含医生范围限制），用于整体态势查看。
 */
@RestController
@RequestMapping("/api/admin/health-statistics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminStatisticsController {

    private final HealthStatisticsService healthStatisticsService;

    @GetMapping("/overview")
    public Result<HealthOverviewVO> overview() {
        return Result.success(healthStatisticsService.overview(null));
    }

    @GetMapping("/chronic-diseases")
    public Result<ChronicStatisticsVO> chronicStatistics() {
        return Result.success(healthStatisticsService.chronicStatistics(null));
    }

    @GetMapping("/risk-distribution")
    public Result<List<RiskDistributionItemVO>> riskDistribution() {
        return Result.success(healthStatisticsService.riskDistribution(null));
    }

    @GetMapping("/alerts")
    public Result<AlertStatsVO> alertStatistics() {
        return Result.success(healthStatisticsService.alertStats(null));
    }

    @GetMapping("/trend")
    public Result<List<AlertTrendPointVO>> alertTrend(
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime to,
            @RequestParam(defaultValue = "day") String granularity) {
        return Result.success(healthStatisticsService.alertTrend(null, from, to, granularity));
    }
}
