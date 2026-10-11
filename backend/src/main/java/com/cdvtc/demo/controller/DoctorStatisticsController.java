package com.cdvtc.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cdvtc.demo.common.Result;
import com.cdvtc.demo.dto.HighRiskResidentQueryRequest;
import com.cdvtc.demo.entity.User;
import com.cdvtc.demo.service.CurrentUserProvider;
import com.cdvtc.demo.service.DoctorResidentService;
import com.cdvtc.demo.service.HealthStatisticsService;
import com.cdvtc.demo.service.HighRiskResidentService;
import com.cdvtc.demo.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 医生端高风险居民筛查与统计分析。
 * 所有统计均以「当前医生管理范围内的居民」为数据范围，不返回超出范围的数据。
 */
@RestController
@RequestMapping("/api/doctor")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DOCTOR')")
public class DoctorStatisticsController {

    private final CurrentUserProvider currentUserProvider;
    private final DoctorResidentService doctorResidentService;
    private final HighRiskResidentService highRiskResidentService;
    private final HealthStatisticsService healthStatisticsService;

    // ============================ 高风险居民筛查 ============================

    @GetMapping("/high-risk-residents")
    public Result<IPage<HighRiskResidentVO>> pageHighRiskResidents(HighRiskResidentQueryRequest query) {
        User doctor = currentUserProvider.requireCurrentUser();
        return Result.success(highRiskResidentService.page(doctor.getId(), query));
    }

    @GetMapping("/high-risk-residents/{residentId}")
    public Result<HighRiskResidentDetailVO> getHighRiskResident(@PathVariable Long residentId) {
        User doctor = currentUserProvider.requireCurrentUser();
        return Result.success(highRiskResidentService.detail(doctor.getId(), residentId));
    }

    // ============================ 健康统计 ============================

    @GetMapping("/health-statistics/overview")
    public Result<HealthOverviewVO> overview() {
        User doctor = currentUserProvider.requireCurrentUser();
        return Result.success(healthStatisticsService.overview(scope(doctor.getId())));
    }

    @GetMapping("/health-statistics/chronic-diseases")
    public Result<ChronicStatisticsVO> chronicStatistics() {
        User doctor = currentUserProvider.requireCurrentUser();
        return Result.success(healthStatisticsService.chronicStatistics(scope(doctor.getId())));
    }

    @GetMapping("/health-statistics/risk-distribution")
    public Result<List<RiskDistributionItemVO>> riskDistribution() {
        User doctor = currentUserProvider.requireCurrentUser();
        return Result.success(healthStatisticsService.riskDistribution(scope(doctor.getId())));
    }

    @GetMapping("/health-statistics/alerts")
    public Result<AlertStatsVO> alertStatistics() {
        User doctor = currentUserProvider.requireCurrentUser();
        return Result.success(healthStatisticsService.alertStats(scope(doctor.getId())));
    }

    @GetMapping("/health-statistics/trend")
    public Result<List<AlertTrendPointVO>> alertTrend(
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime to,
            @RequestParam(defaultValue = "day") String granularity) {
        User doctor = currentUserProvider.requireCurrentUser();
        return Result.success(healthStatisticsService.alertTrend(scope(doctor.getId()), from, to, granularity));
    }

    /** 医生管理范围内的居民 id；作为所有统计的强边界。 */
    private List<Long> scope(Long doctorId) {
        return doctorResidentService.listResidentIdsOfDoctor(doctorId);
    }
}
