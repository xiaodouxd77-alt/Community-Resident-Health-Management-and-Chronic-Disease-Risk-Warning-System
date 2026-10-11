-- =============================================================
-- 第四阶段数据库结构（增量、可重复执行）
--   健康干预方案 intervention_plan + 干预执行记录 intervention_record
-- 说明：
--   1. 全部 CREATE TABLE IF NOT EXISTS，可重复执行，不删除、不重建已有表；
--   2. 命名沿用项目约定：snake_case、create_time/update_time、deleted 逻辑删除、id-type=auto；
--      因此沿用 create_time/update_time，而非 created_at/updated_at；
--   3. intervention_record 采用「同一张表 + operator_role 区分填写者」，
--      医生执行记录与居民反馈各占独立行，追加式保存，互不覆盖；
--   4. 不覆盖第一、二、三阶段数据。
-- =============================================================

USE user_management;

-- -------------------------------------------------------------
-- 健康干预方案表
--   医生为居民制定的健康干预方案。可关联慢病档案 / 异常预警 / 随访计划，均可为空。
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS intervention_plan (
    id                        BIGINT        NOT NULL AUTO_INCREMENT,
    resident_id               BIGINT        NOT NULL COMMENT '居民 health_profile.id',
    doctor_id                 BIGINT        NOT NULL COMMENT '负责医生 sys_user.id',
    chronic_disease_record_id BIGINT                 DEFAULT NULL COMMENT '关联慢病档案 chronic_disease_record.id，可为空',
    alert_id                  BIGINT                 DEFAULT NULL COMMENT '关联预警 alert_record.id，可为空',
    followup_plan_id          BIGINT                 DEFAULT NULL COMMENT '关联随访计划 followup_plan.id，可为空',
    plan_name                 VARCHAR(100)  NOT NULL COMMENT '干预方案名称',
    intervention_type         VARCHAR(20)   NOT NULL COMMENT 'LIFESTYLE-生活方式 DIET-饮食 EXERCISE-运动 MONITORING-指标监测 FOLLOWUP-随访管理 OTHER-其他',
    intervention_goal         VARCHAR(500)           DEFAULT NULL COMMENT '干预目标',
    intervention_content      VARCHAR(1000)          DEFAULT NULL COMMENT '干预措施',
    start_date                DATE                   DEFAULT NULL COMMENT '开始日期',
    end_date                  DATE                   DEFAULT NULL COMMENT '结束日期',
    plan_status               VARCHAR(20)   NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT-草稿 ACTIVE-执行中 COMPLETED-已完成 SUSPENDED-已暂停 CANCELLED-已取消',
    resident_visible          TINYINT       NOT NULL DEFAULT 1 COMMENT '是否允许居民查看 0-否 1-是',
    remark                    VARCHAR(255)           DEFAULT NULL COMMENT '医生内部备注',
    deleted                   TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    create_time               DATETIME               DEFAULT NULL COMMENT '创建时间',
    update_time               DATETIME               DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_iplan_resident (resident_id),
    KEY idx_iplan_doctor_status (doctor_id, plan_status),
    KEY idx_iplan_status (plan_status),
    KEY idx_iplan_start (start_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '健康干预方案';

-- -------------------------------------------------------------
-- 干预执行记录表
--   同一方案多条记录追加保存；operator_role 区分「医生执行记录」与「居民反馈」，
--   居民只能新增自己角色的记录，不能覆盖医生的执行记录。
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS intervention_record (
    id                   BIGINT        NOT NULL AUTO_INCREMENT,
    intervention_plan_id BIGINT        NOT NULL COMMENT '干预方案 intervention_plan.id',
    resident_id          BIGINT        NOT NULL COMMENT '居民 health_profile.id',
    operator_id          BIGINT        NOT NULL COMMENT '填写者 sys_user.id',
    operator_role        VARCHAR(20)   NOT NULL COMMENT 'DOCTOR-医生执行记录 RESIDENT-居民反馈',
    executed_at          DATETIME      NOT NULL COMMENT '实际执行/反馈时间',
    execution_content    VARCHAR(1000)          DEFAULT NULL COMMENT '实际执行内容（医生填写）',
    resident_feedback    VARCHAR(500)           DEFAULT NULL COMMENT '居民反馈',
    execution_status     VARCHAR(20)            DEFAULT NULL COMMENT 'COMPLETED-已执行 PARTIAL-部分执行 NOT_COMPLETED-未执行 NEEDS_ADJUSTMENT-需要调整',
    effect_summary       VARCHAR(500)           DEFAULT NULL COMMENT '阶段性效果摘要（医生填写）',
    next_action          VARCHAR(500)           DEFAULT NULL COMMENT '后续计划（医生填写）',
    resident_visible     TINYINT       NOT NULL DEFAULT 1 COMMENT '是否允许居民查看 0-否 1-是',
    remark               VARCHAR(255)           DEFAULT NULL COMMENT '医生内部备注',
    deleted              TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    create_time          DATETIME               DEFAULT NULL COMMENT '创建时间',
    update_time          DATETIME               DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_irecord_plan (intervention_plan_id),
    KEY idx_irecord_resident (resident_id),
    KEY idx_irecord_operator (operator_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '健康干预执行记录';
