package com.cdvtc.demo.service;

import com.cdvtc.demo.enums.AlertStatus;
import com.cdvtc.demo.enums.DiseaseType;
import com.cdvtc.demo.enums.RiskLevel;
import com.cdvtc.demo.mapper.HealthStatisticsMapper;
import com.cdvtc.demo.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 慢病与预警统计分析。全部指标由 SQL 聚合得出，不做内存全量加载。
 * ids 为数据范围（医生=管理范围内居民；管理员传 null 表示全库）。
 */
@Service
@RequiredArgsConstructor
public class HealthStatisticsService {

    private final HealthStatisticsMapper statisticsMapper;

    // ============================ 概览 ============================

    public HealthOverviewVO overview(List<Long> ids) {
        if (isEmptyScope(ids)) {
            return zeroOverview();
        }
        Map<String, Object> row = statisticsMapper.overview(ids);
        HealthOverviewVO vo = new HealthOverviewVO();
        if (row == null) {
            return vo;
        }
        vo.setResidentCount(toLong(row.get("resident_count")));
        vo.setVerifiedProfileCount(toLong(row.get("verified_profile_count")));
        vo.setChronicConfirmedResidentCount(toLong(row.get("chronic_confirmed_resident_count")));
        vo.setChronicConfirmedRecordCount(toLong(row.get("chronic_confirmed_record_count")));
        vo.setChronicPendingReviewRecordCount(toLong(row.get("chronic_pending_review_record_count")));
        vo.setUnhandledAlertResidentCount(toLong(row.get("unhandled_alert_resident_count")));
        vo.setUnhandledAlertCount(toLong(row.get("unhandled_alert_count")));
        vo.setHighRiskResidentCount(toLong(row.get("high_risk_resident_count")));
        vo.setTotalAssessmentCount(toLong(row.get("total_assessment_count")));
        vo.setTotalAlertCount(toLong(row.get("total_alert_count")));
        vo.setResolvedAlertCount(toLong(row.get("resolved_alert_count")));
        return vo;
    }

    // ============================ 慢病类型分布 ============================

    public ChronicStatisticsVO chronicStatistics(List<Long> ids) {
        if (isEmptyScope(ids)) {
            ChronicStatisticsVO empty = new ChronicStatisticsVO();
            empty.setConfirmedRecordCount(0L);
            empty.setConfirmedResidentCount(0L);
            empty.setItems(new ArrayList<>());
            return empty;
        }
        List<Map<String, Object>> rows = statisticsMapper.chronicByType(ids);

        long confirmedRecordTotal = 0L;
        List<ChronicStatItemVO> items = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            ChronicStatItemVO item = new ChronicStatItemVO();
            String type = (String) row.get("disease_type");
            item.setDiseaseType(type);
            item.setDiseaseTypeLabel(DiseaseType.labelOf(type));
            item.setRecordCount(toLong(row.get("cnt")));
            item.setResidentCount(toLong(row.get("resident_cnt")));
            confirmedRecordTotal += item.getRecordCount() == null ? 0L : item.getRecordCount();
            items.add(item);
        }

        for (ChronicStatItemVO item : items) {
            long rc = item.getRecordCount() == null ? 0L : item.getRecordCount();
            item.setRecordRatio(confirmedRecordTotal == 0 ? 0.0
                    : round4((double) rc / confirmedRecordTotal));
        }
        // 按记录数降序，稳定展示
        items.sort(Comparator.comparingLong((ChronicStatItemVO i) ->
                i.getRecordCount() == null ? 0L : i.getRecordCount()).reversed());

        Map<String, Object> residentRow = statisticsMapper.chronicConfirmedResidentCount(ids);

        ChronicStatisticsVO vo = new ChronicStatisticsVO();
        vo.setConfirmedRecordCount(confirmedRecordTotal);
        vo.setConfirmedResidentCount(residentRow == null ? 0L : toLong(residentRow.get("resident_cnt")));
        vo.setItems(items);
        return vo;
    }

    // ============================ 风险等级分布 ============================

    public List<RiskDistributionItemVO> riskDistribution(List<Long> ids) {
        if (isEmptyScope(ids)) {
            return zeroRiskDistribution();
        }
        List<Map<String, Object>> rows = statisticsMapper.riskDistribution(ids);
        Map<String, Long> counts = new HashMap<>();
        for (Map<String, Object> row : rows) {
            counts.put((String) row.get("risk_level"), toLong(row.get("cnt")));
        }
        List<RiskDistributionItemVO> result = new ArrayList<>();
        for (RiskLevel level : RiskLevel.values()) {
            RiskDistributionItemVO item = new RiskDistributionItemVO();
            item.setRiskLevel(level.name());
            item.setRiskLevelLabel(level.getLabel());
            item.setCount(counts.getOrDefault(level.name(), 0L));
            result.add(item);
        }
        // 风险从高到低展示
        result.sort(Comparator.comparingInt((RiskDistributionItemVO i) ->
                RiskLevel.fromCode(i.getRiskLevel()).map(RiskLevel::getPriority).orElse(0)).reversed());
        return result;
    }

    // ============================ 预警统计 ============================

    public AlertStatsVO alertStats(List<Long> ids) {
        if (isEmptyScope(ids)) {
            AlertStatsVO empty = new AlertStatsVO();
            empty.setTotalCount(0L);
            empty.setPendingCount(0L);
            empty.setProcessingCount(0L);
            empty.setResolvedCount(0L);
            empty.setUnhandledCount(0L);
            empty.setHandlingRate(0.0);
            empty.setByRiskLevel(zeroRiskDistribution());
            return empty;
        }
        Map<String, Long> statusCounts = new HashMap<>();
        for (Map<String, Object> row : statisticsMapper.alertByStatus(ids)) {
            statusCounts.put((String) row.get("status"), toLong(row.get("cnt")));
        }
        long pending = statusCounts.getOrDefault(AlertStatus.PENDING.name(), 0L);
        long processing = statusCounts.getOrDefault(AlertStatus.PROCESSING.name(), 0L);
        long resolved = statusCounts.getOrDefault(AlertStatus.RESOLVED.name(), 0L);
        long total = pending + processing + resolved;

        AlertStatsVO vo = new AlertStatsVO();
        vo.setTotalCount(total);
        vo.setPendingCount(pending);
        vo.setProcessingCount(processing);
        vo.setResolvedCount(resolved);
        vo.setUnhandledCount(pending + processing);
        vo.setHandlingRate(total == 0 ? 0.0 : round4((double) resolved / total));

        Map<String, Long> riskCounts = new HashMap<>();
        for (Map<String, Object> row : statisticsMapper.alertByRiskLevel(ids)) {
            riskCounts.put((String) row.get("risk_level"), toLong(row.get("cnt")));
        }
        List<RiskDistributionItemVO> byRisk = new ArrayList<>();
        for (RiskLevel level : RiskLevel.values()) {
            RiskDistributionItemVO item = new RiskDistributionItemVO();
            item.setRiskLevel(level.name());
            item.setRiskLevelLabel(level.getLabel());
            item.setCount(riskCounts.getOrDefault(level.name(), 0L));
            byRisk.add(item);
        }
        byRisk.sort(Comparator.comparingInt((RiskDistributionItemVO i) ->
                RiskLevel.fromCode(i.getRiskLevel()).map(RiskLevel::getPriority).orElse(0)).reversed());
        vo.setByRiskLevel(byRisk);
        return vo;
    }

    // ============================ 预警趋势 ============================

    public List<AlertTrendPointVO> alertTrend(List<Long> ids, LocalDateTime from, LocalDateTime to, String granularity) {
        if (isEmptyScope(ids)) {
            return new ArrayList<>();
        }
        String fmt = "month".equalsIgnoreCase(granularity) ? "%Y-%m" : "%Y-%m-%d";
        List<AlertTrendPointVO> points = new ArrayList<>();
        for (Map<String, Object> row : statisticsMapper.alertTrend(ids, from, to, fmt)) {
            AlertTrendPointVO p = new AlertTrendPointVO();
            p.setBucket((String) row.get("bucket"));
            p.setPendingCount(toLong(row.get("pending_cnt")));
            p.setProcessingCount(toLong(row.get("processing_cnt")));
            p.setResolvedCount(toLong(row.get("resolved_cnt")));
            p.setTotalCount(toLong(row.get("total_cnt")));
            points.add(p);
        }
        return points;
    }

    // ============================ 工具 ============================

    /** ids == null 表示全库（管理员）；非 null 但为空表示无数据范围，直接返回零值，避免 IN () 语法错误。 */
    private boolean isEmptyScope(List<Long> ids) {
        return ids != null && ids.isEmpty();
    }

    private HealthOverviewVO zeroOverview() {
        HealthOverviewVO vo = new HealthOverviewVO();
        vo.setResidentCount(0L);
        vo.setVerifiedProfileCount(0L);
        vo.setChronicConfirmedResidentCount(0L);
        vo.setChronicConfirmedRecordCount(0L);
        vo.setChronicPendingReviewRecordCount(0L);
        vo.setUnhandledAlertResidentCount(0L);
        vo.setUnhandledAlertCount(0L);
        vo.setHighRiskResidentCount(0L);
        vo.setTotalAssessmentCount(0L);
        vo.setTotalAlertCount(0L);
        vo.setResolvedAlertCount(0L);
        return vo;
    }

    private List<RiskDistributionItemVO> zeroRiskDistribution() {
        List<RiskDistributionItemVO> result = new ArrayList<>();
        for (RiskLevel level : RiskLevel.values()) {
            RiskDistributionItemVO item = new RiskDistributionItemVO();
            item.setRiskLevel(level.name());
            item.setRiskLevelLabel(level.getLabel());
            item.setCount(0L);
            result.add(item);
        }
        result.sort(Comparator.comparingInt((RiskDistributionItemVO i) ->
                RiskLevel.fromCode(i.getRiskLevel()).map(RiskLevel::getPriority).orElse(0)).reversed());
        return result;
    }

    private Long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(value.toString());
    }

    private double round4(double value) {
        return Math.round(value * 10000d) / 10000d;
    }
}
