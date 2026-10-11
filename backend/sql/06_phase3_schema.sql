-- =============================================================
-- 第三阶段数据库结构（增量、可重复执行）
--   随访计划 followup_plan + 随访记录 followup_record
-- 说明：
--   1. 全部 CREATE TABLE IF NOT EXISTS，可重复执行，不删除、不重建已有表；
--   2. 命名沿用项目约定：snake_case、create_time/update_time、deleted 逻辑删除、id-type=auto；
--      因此沿用 create_time/update_time，而非 created_at/updated_at；
--   3. 待执行的计划是否逾期（scheduled_at 已过且未完成/未取消）由后端按当前时间动态计算，
--      不在表中冗余存储，避免时间流逝造成状态不一致；
--   4. 不覆盖第一、第二阶段数据。
-- =============================================================

USE user_management;

-- -------------------------------------------------------------
-- 随访计划表
--   医生为居民制定的一次随访安排。可关联慢病档案、异常预警，也可都是空（普通健康随访）。
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS followup_plan (
    id                        BIGINT       NOT NULL AUTO_INCREMENT,
    resident_id               BIGINT       NOT NULL COMMENT '居民 health_profile.id',
    doctor_id                 BIGINT       NOT NULL COMMENT '负责医生 sys_user.id',
    chronic_disease_record_id BIGINT                DEFAULT NULL COMMENT '关联慢病档案 chronic_disease_record.id，可为空',
    alert_id                  BIGINT                DEFAULT NULL COMMENT '关联预警 alert_record.id，可为空',
    plan_title                VARCHAR(100) NOT NULL COMMENT '计划标题',
    followup_purpose          VARCHAR(500)          DEFAULT NULL COMMENT '随访目的',
    scheduled_at              DATETIME     NOT NULL COMMENT '计划随访时间',
    followup_method           VARCHAR(20)  NOT NULL COMMENT 'PHONE-电话 OFFLINE-线下 ONLINE-线上 OTHER-其他',
    plan_status               VARCHAR(20)  NOT NULL DEFAULT 'PLANNED' COMMENT 'PLANNED-待执行 IN_PROGRESS-进行中 COMPLETED-已完成 CANCELLED-已取消',
    resident_visible          TINYINT      NOT NULL DEFAULT 1 COMMENT '是否允许居民查看 0-否 1-是',
    remark                    VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    deleted                   TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    create_time               DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time               DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_plan_resident (resident_id),
    KEY idx_plan_doctor_status (doctor_id, plan_status),
    KEY idx_plan_status (plan_status),
    KEY idx_plan_scheduled (scheduled_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '随访计划';

-- -------------------------------------------------------------
-- 随访记录表
--   同一计划可有多条记录，追加式保留每次随访历史；新增记录不覆盖历史记录。
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS followup_record (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    plan_id            BIGINT       NOT NULL COMMENT '随访计划 followup_plan.id',
    resident_id        BIGINT       NOT NULL COMMENT '居民 health_profile.id',
    doctor_id          BIGINT       NOT NULL COMMENT '实际执行随访的医生 sys_user.id',
    followed_up_at     DATETIME     NOT NULL COMMENT '实际随访时间',
    followup_method    VARCHAR(20)  NOT NULL COMMENT 'PHONE-电话 OFFLINE-线下 ONLINE-线上 OTHER-其他',
    resident_feedback  VARCHAR(500)          DEFAULT NULL COMMENT '居民反馈',
    health_summary     VARCHAR(500)          DEFAULT NULL COMMENT '近期健康情况摘要',
    indicator_summary  VARCHAR(500)          DEFAULT NULL COMMENT '健康指标变化摘要',
    intervention_advice VARCHAR(500)         DEFAULT NULL COMMENT '健康干预建议',
    followup_result    VARCHAR(500)          DEFAULT NULL COMMENT '本次随访结果',
    next_followup_at   DATETIME              DEFAULT NULL COMMENT '建议下次随访时间，可为空',
    resident_visible   TINYINT      NOT NULL DEFAULT 1 COMMENT '居民是否可查看本条记录 0-否 1-是',
    remark             VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    deleted            TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    create_time        DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time        DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_record_plan (plan_id),
    KEY idx_record_resident (resident_id),
    KEY idx_record_doctor (doctor_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '随访记录';
