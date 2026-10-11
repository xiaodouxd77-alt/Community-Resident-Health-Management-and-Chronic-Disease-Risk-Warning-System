package com.cdvtc.demo.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdvtc.demo.dto.HighRiskResidentQueryRequest;
import com.cdvtc.demo.vo.HighRiskResidentVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 高风险居民筛查与健康统计的聚合查询。
 * 所有统计都下推到 SQL 聚合，不在内存里加载全表再统计。
 * ids 为数据范围（医生=授权范围居民 id；管理员传 null 表示全库）。
 */
@Mapper
public interface HealthStatisticsMapper {

    /** 高风险/关注居民分页筛查，按最近风险等级与评估时间排序。 */
    @Select("""
            <script>
            SELECT t.* FROM (
              SELECT
                p.id AS resident_id,
                p.resident_no AS resident_no,
                p.real_name AS real_name,
                p.gender AS gender,
                p.birth_date AS birth_date,
                c.community_name AS community_name,
                dr.doctor_id AS doctor_id,
                COALESCE(du.real_name, du.username) AS doctor_name,
                ra.risk_level AS latest_risk_level,
                ra.assessed_at AS latest_risk_assessed_at,
                (SELECT COUNT(*) FROM alert_record a WHERE a.resident_id = p.id AND a.deleted = 0
                    AND a.status IN ('PENDING','PROCESSING')) AS unhandled_alert_count,
                (SELECT COUNT(*) FROM alert_record a WHERE a.resident_id = p.id AND a.deleted = 0) AS total_alert_count,
                (SELECT MAX(a.create_time) FROM alert_record a WHERE a.resident_id = p.id AND a.deleted = 0) AS latest_alert_at,
                (SELECT COUNT(*) FROM chronic_disease_record cd WHERE cd.resident_id = p.id AND cd.deleted = 0
                    AND cd.diagnosis_status = 'CONFIRMED') AS confirmed_chronic_count,
                (SELECT MAX(hi.measured_at) FROM health_indicator_record hi WHERE hi.resident_id = p.id AND hi.deleted = 0) AS last_measured_at,
                (SELECT MAX(fr.followed_up_at) FROM followup_record fr WHERE fr.resident_id = p.id AND fr.deleted = 0) AS last_followup_at,
                (SELECT MAX(ir.executed_at) FROM intervention_record ir WHERE ir.resident_id = p.id AND ir.deleted = 0) AS last_intervention_at
              FROM health_profile p
              LEFT JOIN community c ON c.id = p.community_id
              LEFT JOIN doctor_resident dr ON dr.resident_id = p.id AND dr.deleted = 0
              LEFT JOIN sys_user du ON du.id = dr.doctor_id
              LEFT JOIN (
                  SELECT r.resident_id, r.risk_level, r.assessed_at
                  FROM risk_assessment r
                  INNER JOIN (
                      SELECT resident_id, MAX(assessed_at) AS max_at
                      FROM risk_assessment WHERE deleted = 0 GROUP BY resident_id
                  ) m ON m.resident_id = r.resident_id AND m.max_at = r.assessed_at
                  WHERE r.deleted = 0
              ) ra ON ra.resident_id = p.id
              WHERE p.deleted = 0
              <if test="ids != null">
                AND p.id IN <foreach collection="ids" item="rid" open="(" separator="," close=")">#{rid}</foreach>
              </if>
            ) t
            WHERE 1 = 1
            <if test="q.riskLevel != null and q.riskLevel != ''">
              AND t.latest_risk_level = #{q.riskLevel}
            </if>
            <if test="q.hasUnhandledAlert != null and q.hasUnhandledAlert == true">
              AND t.unhandled_alert_count &gt; 0
            </if>
            <if test="q.hasUnhandledAlert != null and q.hasUnhandledAlert == false">
              AND t.unhandled_alert_count = 0
            </if>
            <if test="q.alertStatus != null and q.alertStatus != ''">
              AND EXISTS (SELECT 1 FROM alert_record a WHERE a.resident_id = t.resident_id AND a.deleted = 0
                  AND a.status = #{q.alertStatus})
            </if>
            <if test="q.diseaseType != null and q.diseaseType != ''">
              AND EXISTS (SELECT 1 FROM chronic_disease_record cd WHERE cd.resident_id = t.resident_id AND cd.deleted = 0
                  AND cd.diagnosis_status = 'CONFIRMED' AND cd.disease_type = #{q.diseaseType})
            </if>
            <if test="q.hasChronic != null and q.hasChronic == true">
              AND EXISTS (SELECT 1 FROM chronic_disease_record cd WHERE cd.resident_id = t.resident_id AND cd.deleted = 0
                  AND cd.diagnosis_status = 'CONFIRMED')
            </if>
            <if test="q.hasChronic != null and q.hasChronic == false">
              AND NOT EXISTS (SELECT 1 FROM chronic_disease_record cd WHERE cd.resident_id = t.resident_id AND cd.deleted = 0
                  AND cd.diagnosis_status = 'CONFIRMED')
            </if>
            <if test="q.keyword != null and q.keyword != ''">
              AND (t.real_name LIKE CONCAT('%', #{q.keyword}, '%') OR t.resident_no LIKE CONCAT('%', #{q.keyword}, '%'))
            </if>
            <if test="q.groupId != null">
              AND EXISTS (SELECT 1 FROM resident_group_member gm WHERE gm.resident_id = t.resident_id AND gm.deleted = 0
                  AND gm.group_id = #{q.groupId})
            </if>
            <if test="q.lastMeasuredFrom != null">
              AND t.last_measured_at &gt;= #{q.lastMeasuredFrom}
            </if>
            <if test="q.lastMeasuredTo != null">
              AND t.last_measured_at &lt;= #{q.lastMeasuredTo}
            </if>
            ORDER BY FIELD(t.latest_risk_level, 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL') DESC, t.latest_risk_assessed_at DESC, t.resident_id DESC
            </script>
            """)
    IPage<HighRiskResidentVO> pageHighRiskResidents(Page<HighRiskResidentVO> page,
                                                     @Param("ids") List<Long> ids,
                                                     @Param("q") HighRiskResidentQueryRequest q);

    /** 健康概览：一次返回多个聚合计数（人数与记录数口径分开）。 */
    @Select("""
            <script>
            SELECT
              (SELECT COUNT(*) FROM health_profile p WHERE p.deleted = 0
                  <if test="ids != null">AND p.id IN <foreach collection="ids" item="rid" open="(" separator="," close=")">#{rid}</foreach></if>
              ) AS resident_count,
              (SELECT COUNT(*) FROM health_profile p WHERE p.deleted = 0 AND p.verified = 1
                  <if test="ids != null">AND p.id IN <foreach collection="ids" item="rid" open="(" separator="," close=")">#{rid}</foreach></if>
              ) AS verified_profile_count,
              (SELECT COUNT(DISTINCT cd.resident_id) FROM chronic_disease_record cd
                  WHERE cd.deleted = 0 AND cd.diagnosis_status = 'CONFIRMED'
                  <if test="ids != null">AND cd.resident_id IN <foreach collection="ids" item="rid" open="(" separator="," close=")">#{rid}</foreach></if>
              ) AS chronic_confirmed_resident_count,
              (SELECT COUNT(*) FROM chronic_disease_record cd
                  WHERE cd.deleted = 0 AND cd.diagnosis_status = 'CONFIRMED'
                  <if test="ids != null">AND cd.resident_id IN <foreach collection="ids" item="rid" open="(" separator="," close=")">#{rid}</foreach></if>
              ) AS chronic_confirmed_record_count,
              (SELECT COUNT(*) FROM chronic_disease_record cd
                  WHERE cd.deleted = 0 AND cd.diagnosis_status = 'PENDING_REVIEW'
                  <if test="ids != null">AND cd.resident_id IN <foreach collection="ids" item="rid" open="(" separator="," close=")">#{rid}</foreach></if>
              ) AS chronic_pending_review_record_count,
              (SELECT COUNT(DISTINCT a.resident_id) FROM alert_record a
                  WHERE a.deleted = 0 AND a.status IN ('PENDING','PROCESSING')
                  <if test="ids != null">AND a.resident_id IN <foreach collection="ids" item="rid" open="(" separator="," close=")">#{rid}</foreach></if>
              ) AS unhandled_alert_resident_count,
              (SELECT COUNT(*) FROM alert_record a
                  WHERE a.deleted = 0 AND a.status IN ('PENDING','PROCESSING')
                  <if test="ids != null">AND a.resident_id IN <foreach collection="ids" item="rid" open="(" separator="," close=")">#{rid}</foreach></if>
              ) AS unhandled_alert_count,
              (SELECT COUNT(*) FROM (
                  SELECT r.resident_id, r.risk_level
                  FROM risk_assessment r
                  INNER JOIN (SELECT resident_id, MAX(assessed_at) AS max_at FROM risk_assessment WHERE deleted = 0 GROUP BY resident_id) m
                      ON m.resident_id = r.resident_id AND m.max_at = r.assessed_at
                  WHERE r.deleted = 0
                  <if test="ids != null">AND r.resident_id IN <foreach collection="ids" item="rid" open="(" separator="," close=")">#{rid}</foreach></if>
              ) hr WHERE hr.risk_level IN ('HIGH','CRITICAL')
              ) AS high_risk_resident_count,
              (SELECT COUNT(*) FROM risk_assessment r WHERE r.deleted = 0
                  <if test="ids != null">AND r.resident_id IN <foreach collection="ids" item="rid" open="(" separator="," close=")">#{rid}</foreach></if>
              ) AS total_assessment_count,
              (SELECT COUNT(*) FROM alert_record a WHERE a.deleted = 0
                  <if test="ids != null">AND a.resident_id IN <foreach collection="ids" item="rid" open="(" separator="," close=")">#{rid}</foreach></if>
              ) AS total_alert_count,
              (SELECT COUNT(*) FROM alert_record a WHERE a.deleted = 0 AND a.status = 'RESOLVED'
                  <if test="ids != null">AND a.resident_id IN <foreach collection="ids" item="rid" open="(" separator="," close=")">#{rid}</foreach></if>
              ) AS resolved_alert_count
            </script>
            """)
    Map<String, Object> overview(@Param("ids") List<Long> ids);

    /** 慢病类型分布（仅已核实档案）。 */
    @Select("""
            <script>
            SELECT cd.disease_type AS disease_type,
                   COUNT(*) AS cnt,
                   COUNT(DISTINCT cd.resident_id) AS resident_cnt
            FROM chronic_disease_record cd
            WHERE cd.deleted = 0 AND cd.diagnosis_status = 'CONFIRMED'
            <if test="ids != null">AND cd.resident_id IN <foreach collection="ids" item="rid" open="(" separator="," close=")">#{rid}</foreach></if>
            GROUP BY cd.disease_type
            </script>
            """)
    List<Map<String, Object>> chronicByType(@Param("ids") List<Long> ids);

    /** 已核实慢病居民去重人数（一人多病只计一次）。 */
    @Select("""
            <script>
            SELECT COUNT(DISTINCT cd.resident_id) AS resident_cnt
            FROM chronic_disease_record cd
            WHERE cd.deleted = 0 AND cd.diagnosis_status = 'CONFIRMED'
            <if test="ids != null">AND cd.resident_id IN <foreach collection="ids" item="rid" open="(" separator="," close=")">#{rid}</foreach></if>
            </script>
            """)
    Map<String, Object> chronicConfirmedResidentCount(@Param("ids") List<Long> ids);

    /** 风险评估按等级分布。 */
    @Select("""
            <script>
            SELECT r.risk_level AS risk_level, COUNT(*) AS cnt
            FROM risk_assessment r
            WHERE r.deleted = 0
            <if test="ids != null">AND r.resident_id IN <foreach collection="ids" item="rid" open="(" separator="," close=")">#{rid}</foreach></if>
            GROUP BY r.risk_level
            </script>
            """)
    List<Map<String, Object>> riskDistribution(@Param("ids") List<Long> ids);

    /** 预警按状态计数。 */
    @Select("""
            <script>
            SELECT a.status AS status, COUNT(*) AS cnt
            FROM alert_record a
            WHERE a.deleted = 0
            <if test="ids != null">AND a.resident_id IN <foreach collection="ids" item="rid" open="(" separator="," close=")">#{rid}</foreach></if>
            GROUP BY a.status
            </script>
            """)
    List<Map<String, Object>> alertByStatus(@Param("ids") List<Long> ids);

    /** 预警按风险等级计数。 */
    @Select("""
            <script>
            SELECT a.risk_level AS risk_level, COUNT(*) AS cnt
            FROM alert_record a
            WHERE a.deleted = 0
            <if test="ids != null">AND a.resident_id IN <foreach collection="ids" item="rid" open="(" separator="," close=")">#{rid}</foreach></if>
            GROUP BY a.risk_level
            </script>
            """)
    List<Map<String, Object>> alertByRiskLevel(@Param("ids") List<Long> ids);

    /** 预警趋势：按天/月分桶，各状态分别计数。 */
    @Select("""
            <script>
            SELECT DATE_FORMAT(a.create_time, #{fmt}) AS bucket,
                   SUM(CASE WHEN a.status = 'PENDING' THEN 1 ELSE 0 END) AS pending_cnt,
                   SUM(CASE WHEN a.status = 'PROCESSING' THEN 1 ELSE 0 END) AS processing_cnt,
                   SUM(CASE WHEN a.status = 'RESOLVED' THEN 1 ELSE 0 END) AS resolved_cnt,
                   COUNT(*) AS total_cnt
            FROM alert_record a
            WHERE a.deleted = 0
            <if test="from != null">AND a.create_time &gt;= #{from}</if>
            <if test="to != null">AND a.create_time &lt;= #{to}</if>
            <if test="ids != null">AND a.resident_id IN <foreach collection="ids" item="rid" open="(" separator="," close=")">#{rid}</foreach></if>
            GROUP BY bucket
            ORDER BY bucket
            </script>
            """)
    List<Map<String, Object>> alertTrend(@Param("ids") List<Long> ids,
                                         @Param("from") LocalDateTime from,
                                         @Param("to") LocalDateTime to,
                                         @Param("fmt") String fmt);
}
