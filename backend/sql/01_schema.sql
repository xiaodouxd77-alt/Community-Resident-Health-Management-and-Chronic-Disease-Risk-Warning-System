-- =============================================================
-- 社区居民健康管理与慢病风险预警系统 - 数据库结构（增量、可重复执行）
-- MySQL 8.0+
-- 说明：
--   1. 全部使用 CREATE TABLE IF NOT EXISTS，可重复执行；
--   2. sys_user 的 role 列通过 information_schema 判断后增量添加，兼容已有库；
--   3. 字段命名沿用现有项目约定：snake_case、create_time/update_time、deleted 逻辑删除；
--   4. 不删除、不重建已有数据，不覆盖用户数据。
-- =============================================================

CREATE DATABASE IF NOT EXISTS user_management
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;

USE user_management;

-- -------------------------------------------------------------
-- 系统用户表（与现有实体 com.cdvtc.demo.entity.User 一致）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    username    VARCHAR(50)  NOT NULL COMMENT '登录用户名',
    password    VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码哈希',
    email       VARCHAR(100)          DEFAULT NULL COMMENT '邮箱',
    phone       VARCHAR(20)           DEFAULT NULL COMMENT '手机号',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '0-禁用 1-启用',
    role        VARCHAR(20)  NOT NULL DEFAULT 'RESIDENT' COMMENT '角色 RESIDENT-居民 DOCTOR-社区医生 ADMIN-管理员',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统用户表';

-- -------------------------------------------------------------
-- 增量：为已存在的 sys_user 补充 role 列（可重复执行）
-- -------------------------------------------------------------
SET @role_col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'role'
);
SET @ddl = IF(@role_col_exists = 0,
    'ALTER TABLE sys_user ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT ''RESIDENT'' COMMENT ''角色 RESIDENT-居民 DOCTOR-社区医生 ADMIN-管理员'' AFTER status',
    'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -------------------------------------------------------------
-- 居民健康档案表（仅保存相对稳定的基础资料）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS health_profile (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    user_id      BIGINT      NOT NULL COMMENT '关联 sys_user.id',
    resident_no  VARCHAR(32) NOT NULL COMMENT '居民编号',
    real_name    VARCHAR(50)          DEFAULT NULL COMMENT '居民姓名',
    gender       VARCHAR(10)          DEFAULT 'UNKNOWN' COMMENT 'MALE/FEMALE/UNKNOWN',
    birth_date   DATE                 DEFAULT NULL COMMENT '出生日期',
    community_id BIGINT               DEFAULT NULL COMMENT '社区ID（当前阶段允许为空）',
    deleted      TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    create_time  DATETIME             DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_profile_user (user_id),
    UNIQUE KEY uk_profile_no (resident_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '居民健康档案';

-- -------------------------------------------------------------
-- 健康指标记录表（可重复变化的测量数据）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS health_indicator_record (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    resident_id         BIGINT        NOT NULL COMMENT 'health_profile.id',
    indicator_type      VARCHAR(30)   NOT NULL COMMENT 'BLOOD_PRESSURE/BLOOD_GLUCOSE/WEIGHT/HEART_RATE',
    numeric_value       DECIMAL(10,2) NOT NULL COMMENT '主数值：收缩压/血糖/体重/心率',
    secondary_value     DECIMAL(10,2)          DEFAULT NULL COMMENT '第二数值：舒张压（仅血压使用）',
    unit                VARCHAR(20)   NOT NULL COMMENT '单位',
    measured_at         DATETIME      NOT NULL COMMENT '测量时间',
    measurement_context VARCHAR(20)            DEFAULT NULL COMMENT '测量场景 FASTING/POSTPRANDIAL/RANDOM',
    source              VARCHAR(20)   NOT NULL DEFAULT 'MANUAL' COMMENT '数据来源 MANUAL-居民手动录入',
    remark              VARCHAR(255)           DEFAULT NULL COMMENT '备注',
    request_id          VARCHAR(64)   NOT NULL COMMENT '客户端幂等键（防重复提交）',
    deleted             TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    create_time         DATETIME               DEFAULT NULL COMMENT '创建时间',
    update_time         DATETIME               DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_record_request (request_id),
    KEY idx_record_resident_type_time (resident_id, indicator_type, measured_at),
    KEY idx_record_resident_time (resident_id, measured_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '健康指标记录';

-- -------------------------------------------------------------
-- 风险规则表（简单、可解释的阈值规则）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS risk_rule (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    rule_code           VARCHAR(50)   NOT NULL COMMENT '规则编码',
    rule_name           VARCHAR(100)  NOT NULL COMMENT '规则名称',
    indicator_type      VARCHAR(30)   NOT NULL COMMENT '关联指标类型',
    measurement_context VARCHAR(20)            DEFAULT NULL COMMENT '适用测量场景，NULL 表示不限',
    value_field         VARCHAR(20)   NOT NULL DEFAULT 'PRIMARY' COMMENT 'PRIMARY-主数值 SECONDARY-第二数值（血压舒张压）',
    operator            VARCHAR(5)    NOT NULL COMMENT '比较操作符 >= > <= <',
    threshold_value     DECIMAL(10,2) NOT NULL COMMENT '阈值',
    risk_level          VARCHAR(20)   NOT NULL COMMENT 'LOW/MEDIUM/HIGH/CRITICAL',
    alert_type          VARCHAR(50)   NOT NULL COMMENT '预警类型编码',
    description         VARCHAR(255)           DEFAULT NULL COMMENT '规则说明/医学依据',
    rule_version        VARCHAR(20)   NOT NULL DEFAULT 'v1' COMMENT '规则版本',
    enabled             TINYINT       NOT NULL DEFAULT 1 COMMENT '是否启用 0-否 1-是',
    effective_from      DATETIME               DEFAULT NULL COMMENT '生效时间',
    effective_to        DATETIME               DEFAULT NULL COMMENT '失效时间',
    deleted             TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    create_time         DATETIME               DEFAULT NULL COMMENT '创建时间',
    update_time         DATETIME               DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_rule_code_version (rule_code, rule_version),
    KEY idx_rule_type_enabled (indicator_type, enabled)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '慢病风险阈值规则';

-- -------------------------------------------------------------
-- 风险评估记录表
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS risk_assessment (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    resident_id         BIGINT       NOT NULL COMMENT 'health_profile.id',
    indicator_record_id BIGINT       NOT NULL COMMENT '本次触发评估的指标记录 id',
    assessment_code     VARCHAR(64)  NOT NULL COMMENT '评估编号',
    risk_level          VARCHAR(20)  NOT NULL COMMENT 'LOW/MEDIUM/HIGH/CRITICAL',
    assessment_summary  VARCHAR(500)          DEFAULT NULL COMMENT '评估摘要',
    trigger_details     TEXT                  DEFAULT NULL COMMENT '触发规则与原因的结构化快照(JSON)',
    rule_version        VARCHAR(20)  NOT NULL COMMENT '本次评估使用的规则版本',
    assessed_at         DATETIME     NOT NULL COMMENT '评估时间',
    deleted             TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    create_time         DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time         DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_assessment_indicator (indicator_record_id),
    KEY idx_assessment_resident (resident_id, assessed_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '风险评估记录';

-- -------------------------------------------------------------
-- 异常预警表
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS alert_record (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    alert_no            VARCHAR(64)  NOT NULL COMMENT '预警编号',
    resident_id         BIGINT       NOT NULL COMMENT 'health_profile.id',
    assessment_id       BIGINT       NOT NULL COMMENT 'risk_assessment.id',
    indicator_record_id BIGINT       NOT NULL COMMENT 'health_indicator_record.id',
    alert_type          VARCHAR(50)  NOT NULL COMMENT '预警类型',
    risk_level          VARCHAR(20)  NOT NULL COMMENT '风险等级',
    alert_title         VARCHAR(100) NOT NULL COMMENT '预警标题',
    alert_reason        VARCHAR(500) NOT NULL COMMENT '触发原因',
    status              VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PROCESSING/RESOLVED',
    assigned_doctor_id  BIGINT                DEFAULT NULL COMMENT '负责医生 sys_user.id，NULL 表示待分配',
    deleted             TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    create_time         DATETIME              DEFAULT NULL COMMENT '预警产生时间',
    update_time         DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_alert_no (alert_no),
    UNIQUE KEY uk_alert_assessment (assessment_id),
    KEY idx_alert_resident_status (resident_id, status),
    KEY idx_alert_doctor_status (assigned_doctor_id, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '异常预警记录';

-- -------------------------------------------------------------
-- 预警处理记录表（追加式，不覆盖历史）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS alert_handling_record (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    alert_id           BIGINT       NOT NULL COMMENT 'alert_record.id',
    doctor_id          BIGINT       NOT NULL COMMENT '医生 sys_user.id',
    action_type        VARCHAR(20)  NOT NULL COMMENT 'START-开始处理 PROGRESS-处理中 RESOLVE-完成处理',
    handling_comment   VARCHAR(500) NOT NULL COMMENT '处理意见',
    followup_required  TINYINT      NOT NULL DEFAULT 0 COMMENT '是否需要随访 0-否 1-是',
    next_followup_at   DATETIME              DEFAULT NULL COMMENT '下次随访时间',
    deleted            TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    create_time        DATETIME              DEFAULT NULL COMMENT '处理时间',
    update_time        DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_handling_alert (alert_id, create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '预警处理记录';

-- -------------------------------------------------------------
-- 医生-居民授权关系表（最小可行的数据范围控制）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS doctor_resident (
    id          BIGINT   NOT NULL AUTO_INCREMENT,
    doctor_id   BIGINT   NOT NULL COMMENT '医生 sys_user.id',
    resident_id BIGINT   NOT NULL COMMENT '居民 health_profile.id',
    deleted     TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    create_time DATETIME          DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME          DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_dr_resident (resident_id),
    KEY idx_dr_doctor (doctor_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '医生-居民授权关系（一名居民至多归属一名责任医生）';
