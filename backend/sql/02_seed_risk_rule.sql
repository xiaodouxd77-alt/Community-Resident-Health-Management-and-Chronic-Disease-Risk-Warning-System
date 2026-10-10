-- =============================================================
-- 风险阈值规则种子数据（可重复执行）
-- 唯一键：(rule_code, rule_version)，重复执行时更新规则内容
--
-- 【医学依据说明】以下阈值均引用公开的临床指南切点，仅用于本系统的
-- 健康风险提示，不构成疾病诊断。规则版本 v1。
-- effective_from 固定为 2020-01-01，保证规则对历史测量时间同样生效
-- （规则有效期按"评估时刻"判断，见 RiskAssessmentService）。
--
-- 血压（mmHg）：参考《中国高血压防治指南(2018年修订版)》及 ACC/AHA 2017
--   - 收缩压/舒张压 >=180 / >=110 mmHg 为高血压急症警戒值（CRITICAL）
--   - >=160 / >=100 mmHg 为 2 级高血压范围（HIGH）
--   - >=140 / >=90  mmHg 为 1 级高血压诊断切点（MEDIUM）
--   - >=130 / >=85  mmHg 为正常高值，需关注（LOW）
--
-- 血糖（mmol/L）：参考《中国2型糖尿病防治指南(2020年版)》
--   - 空腹(FASTING)：>=7.0 达糖尿病诊断切点(HIGH)；6.1~6.9 空腹血糖受损(MEDIUM)；<3.9 低血糖(HIGH)
--   - 餐后2h(POSTPRANDIAL)：>=11.1 达诊断切点(HIGH)；7.8~11.0 糖耐量减低(MEDIUM)；<3.9 低血糖(HIGH)
--   - 随机(RANDOM)：>=11.1 偏高需复查(MEDIUM)；<3.9 低血糖(HIGH)
--
-- 心率（次/分）：参考 AHA 静息心率正常范围 60~100
--   - >=120 明显心动过速(HIGH)；>=100 心动过速(MEDIUM)
--   - <40 明显心动过缓(HIGH)；<50 心动过缓(MEDIUM)
--
-- 体重（kg）：不配置阈值规则。单次体重缺少身高/BMI 上下文，
--   不足以下结论，评估服务会返回"需结合身高进一步判断"，不产生预警。
-- =============================================================

USE user_management;

INSERT INTO risk_rule
(rule_code, rule_name, indicator_type, measurement_context, value_field, operator,
 threshold_value, risk_level, alert_type, description, rule_version, enabled, effective_from)
VALUES
-- ---------------- 血压：收缩压（PRIMARY） ----------------
('BP_SBP_CRITICAL', '收缩压重度升高', 'BLOOD_PRESSURE', NULL, 'PRIMARY', '>=', 180.00, 'CRITICAL', 'BLOOD_PRESSURE_CRISIS',
 '收缩压≥180mmHg，处于高血压急症警戒范围，建议立即就医（《中国高血压防治指南2018》）', 'v1', 1, '2020-01-01 00:00:00'),
('BP_SBP_HIGH', '收缩压明显升高', 'BLOOD_PRESSURE', NULL, 'PRIMARY', '>=', 160.00, 'HIGH', 'BLOOD_PRESSURE_HIGH',
 '收缩压≥160mmHg，处于2级高血压范围，建议尽快就医评估（《中国高血压防治指南2018》）', 'v1', 1, '2020-01-01 00:00:00'),
('BP_SBP_MEDIUM', '收缩压升高', 'BLOOD_PRESSURE', NULL, 'PRIMARY', '>=', 140.00, 'MEDIUM', 'BLOOD_PRESSURE_ELEVATED',
 '收缩压≥140mmHg，达到1级高血压诊断切点，建议复测并咨询医生（《中国高血压防治指南2018》）', 'v1', 1, '2020-01-01 00:00:00'),
('BP_SBP_LOW', '收缩压正常高值', 'BLOOD_PRESSURE', NULL, 'PRIMARY', '>=', 130.00, 'LOW', 'BLOOD_PRESSURE_ATTENTION',
 '收缩压130~139mmHg，属正常高值，建议关注并定期复测（《中国高血压防治指南2018》）', 'v1', 1, '2020-01-01 00:00:00'),

-- ---------------- 血压：舒张压（SECONDARY） ----------------
('BP_DBP_CRITICAL', '舒张压重度升高', 'BLOOD_PRESSURE', NULL, 'SECONDARY', '>=', 110.00, 'CRITICAL', 'BLOOD_PRESSURE_CRISIS',
 '舒张压≥110mmHg，处于高血压急症警戒范围，建议立即就医（《中国高血压防治指南2018》）', 'v1', 1, '2020-01-01 00:00:00'),
('BP_DBP_HIGH', '舒张压明显升高', 'BLOOD_PRESSURE', NULL, 'SECONDARY', '>=', 100.00, 'HIGH', 'BLOOD_PRESSURE_HIGH',
 '舒张压≥100mmHg，处于2级高血压范围，建议尽快就医评估（《中国高血压防治指南2018》）', 'v1', 1, '2020-01-01 00:00:00'),
('BP_DBP_MEDIUM', '舒张压升高', 'BLOOD_PRESSURE', NULL, 'SECONDARY', '>=', 90.00, 'MEDIUM', 'BLOOD_PRESSURE_ELEVATED',
 '舒张压≥90mmHg，达到1级高血压诊断切点，建议复测并咨询医生（《中国高血压防治指南2018》）', 'v1', 1, '2020-01-01 00:00:00'),
('BP_DBP_LOW', '舒张压正常高值', 'BLOOD_PRESSURE', NULL, 'SECONDARY', '>=', 85.00, 'LOW', 'BLOOD_PRESSURE_ATTENTION',
 '舒张压85~89mmHg，属正常高值，建议关注并定期复测（《中国高血压防治指南2018》）', 'v1', 1, '2020-01-01 00:00:00'),

-- ---------------- 血糖：空腹（FASTING） ----------------
('GLU_FASTING_HIGH', '空腹血糖达诊断切点', 'BLOOD_GLUCOSE', 'FASTING', 'PRIMARY', '>=', 7.00, 'HIGH', 'BLOOD_GLUCOSE_HIGH',
 '空腹血糖≥7.0mmol/L，达到糖尿病诊断切点，需到医疗机构复核确诊，本提示不构成诊断（《中国2型糖尿病防治指南2020》）', 'v1', 1, '2020-01-01 00:00:00'),
('GLU_FASTING_IFG', '空腹血糖受损', 'BLOOD_GLUCOSE', 'FASTING', 'PRIMARY', '>=', 6.10, 'MEDIUM', 'BLOOD_GLUCOSE_IMPAIRED',
 '空腹血糖6.1~6.9mmol/L，属空腹血糖受损，建议生活方式干预并复查（《中国2型糖尿病防治指南2020》）', 'v1', 1, '2020-01-01 00:00:00'),
('GLU_FASTING_LOW', '空腹低血糖', 'BLOOD_GLUCOSE', 'FASTING', 'PRIMARY', '<', 3.90, 'HIGH', 'BLOOD_GLUCOSE_LOW',
 '空腹血糖<3.9mmol/L，属低血糖，建议及时补充糖分并就医排查原因', 'v1', 1, '2020-01-01 00:00:00'),

-- ---------------- 血糖：餐后2小时（POSTPRANDIAL） ----------------
('GLU_POST_HIGH', '餐后血糖达诊断切点', 'BLOOD_GLUCOSE', 'POSTPRANDIAL', 'PRIMARY', '>=', 11.10, 'HIGH', 'BLOOD_GLUCOSE_HIGH',
 '餐后2小时血糖≥11.0mmol/L，达到糖尿病诊断切点，需到医疗机构复核确诊，本提示不构成诊断（《中国2型糖尿病防治指南2020》）', 'v1', 1, '2020-01-01 00:00:00'),
('GLU_POST_IGT', '餐后糖耐量减低', 'BLOOD_GLUCOSE', 'POSTPRANDIAL', 'PRIMARY', '>=', 7.80, 'MEDIUM', 'BLOOD_GLUCOSE_IMPAIRED',
 '餐后2小时血糖7.8~11.0mmol/L，属糖耐量减低，建议复查（《中国2型糖尿病防治指南2020》）', 'v1', 1, '2020-01-01 00:00:00'),
('GLU_POST_LOW', '餐后低血糖', 'BLOOD_GLUCOSE', 'POSTPRANDIAL', 'PRIMARY', '<', 3.90, 'HIGH', 'BLOOD_GLUCOSE_LOW',
 '餐后血糖<3.9mmol/L，属低血糖，建议及时补充糖分并就医排查原因', 'v1', 1, '2020-01-01 00:00:00'),

-- ---------------- 血糖：随机（RANDOM） ----------------
('GLU_RANDOM_HIGH', '随机血糖偏高', 'BLOOD_GLUCOSE', 'RANDOM', 'PRIMARY', '>=', 11.10, 'MEDIUM', 'BLOOD_GLUCOSE_ELEVATED',
 '随机血糖≥11.1mmol/L，建议复查空腹及餐后血糖进一步评估（《中国2型糖尿病防治指南2020》）', 'v1', 1, '2020-01-01 00:00:00'),
('GLU_RANDOM_LOW', '随机血糖偏低', 'BLOOD_GLUCOSE', 'RANDOM', 'PRIMARY', '<', 3.90, 'HIGH', 'BLOOD_GLUCOSE_LOW',
 '随机血糖<3.9mmol/L，属低血糖，建议及时补充糖分并就医排查原因', 'v1', 1, '2020-01-01 00:00:00'),

-- ---------------- 心率（HEART_RATE） ----------------
('HR_TACHY_HIGH', '明显心动过速', 'HEART_RATE', NULL, 'PRIMARY', '>=', 120.00, 'HIGH', 'HEART_RATE_TACHYCARDIA',
 '静息心率≥120次/分，明显高于正常范围，建议就医评估（AHA 静息心率正常范围60~100）', 'v1', 1, '2020-01-01 00:00:00'),
('HR_TACHY', '心动过速', 'HEART_RATE', NULL, 'PRIMARY', '>=', 100.00, 'MEDIUM', 'HEART_RATE_TACHYCARDIA',
 '静息心率100~119次/分，高于正常范围，建议休息后复测（AHA 静息心率正常范围60~100）', 'v1', 1, '2020-01-01 00:00:00'),
('HR_BRADY_HIGH', '明显心动过缓', 'HEART_RATE', NULL, 'PRIMARY', '<', 40.00, 'HIGH', 'HEART_RATE_BRADYCARDIA',
 '静息心率<40次/分，明显低于正常范围，建议就医评估（AHA 静息心率正常范围60~100）', 'v1', 1, '2020-01-01 00:00:00'),
('HR_BRADY', '心动过缓', 'HEART_RATE', NULL, 'PRIMARY', '<', 50.00, 'MEDIUM', 'HEART_RATE_BRADYCARDIA',
 '静息心率40~59次/分，低于正常范围（长期运动人群可偏低），建议关注（AHA 静息心率正常范围60~100）', 'v1', 1, '2020-01-01 00:00:00')
ON DUPLICATE KEY UPDATE
    rule_name           = VALUES(rule_name),
    indicator_type      = VALUES(indicator_type),
    measurement_context = VALUES(measurement_context),
    value_field         = VALUES(value_field),
    operator            = VALUES(operator),
    threshold_value     = VALUES(threshold_value),
    risk_level          = VALUES(risk_level),
    alert_type          = VALUES(alert_type),
    description         = VALUES(description),
    enabled             = VALUES(enabled),
    effective_from      = VALUES(effective_from),
    update_time         = NOW();
