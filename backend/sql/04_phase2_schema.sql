-- =============================================================
-- 第二阶段数据库结构（增量、可重复执行）
--   模块一：居民健康档案扩展（health_profile 增补字段 + community 参照表）
--   模块二：慢病患者建档（chronic_disease_record + chronic_disease_history）
--   模块三：居民分组（resident_group + resident_group_member）
-- 说明：
--   1. 全部 CREATE TABLE IF NOT EXISTS，可重复执行；
--   2. health_profile 新字段通过 information_schema 判断后增量添加，兼容已有库，不动旧数据；
--   3. 命名沿用项目约定：snake_case、create_time/update_time、deleted 逻辑删除、id-type=auto；
--   4. 不删除、不重建已有表，不覆盖第一阶段数据。
-- =============================================================

USE user_management;

-- -------------------------------------------------------------
-- 社区参照表（健康档案与分组均引用，避免社区名称在多表重复保存）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS community (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    community_code VARCHAR(32) NOT NULL COMMENT '社区编码',
    community_name VARCHAR(100) NOT NULL COMMENT '社区名称',
    deleted        TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    create_time    DATETIME             DEFAULT NULL COMMENT '创建时间',
    update_time    DATETIME             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_community_code (community_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '社区参照表';

-- -------------------------------------------------------------
-- 增量：居民健康档案补充基础资料字段（居民自述）
--   既往史/家族史/过敏史/身高/备注属于居民可自行维护的自述信息；
--   verified 系列字段用于区分"居民自述"与"医生核实"，不把自述当作已确认医学事实。
-- -------------------------------------------------------------
SET @hp_col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'health_profile' AND COLUMN_NAME = 'height'
);
SET @ddl = IF(@hp_col_exists = 0,
    'ALTER TABLE health_profile
        ADD COLUMN height DECIMAL(5,1) DEFAULT NULL COMMENT ''身高(cm)'' AFTER birth_date,
        ADD COLUMN past_history VARCHAR(500) DEFAULT NULL COMMENT ''既往病史(居民自述)'' AFTER height,
        ADD COLUMN family_history VARCHAR(500) DEFAULT NULL COMMENT ''家族病史(居民自述)'' AFTER past_history,
        ADD COLUMN allergy_history VARCHAR(500) DEFAULT NULL COMMENT ''过敏史(居民自述)'' AFTER family_history,
        ADD COLUMN health_remark VARCHAR(500) DEFAULT NULL COMMENT ''健康备注(居民自述)'' AFTER allergy_history,
        ADD COLUMN verified TINYINT NOT NULL DEFAULT 0 COMMENT ''资料是否经医生核实 0-否 1-是'' AFTER health_remark,
        ADD COLUMN verified_doctor_id BIGINT DEFAULT NULL COMMENT ''核实医生 sys_user.id'' AFTER verified,
        ADD COLUMN verified_at DATETIME DEFAULT NULL COMMENT ''核实时间'' AFTER verified_doctor_id',
    'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -------------------------------------------------------------
-- 增量：用户展示姓名（医生/管理员的列表展示名；为空时前端回退登录名）
-- -------------------------------------------------------------
SET @user_realname_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'real_name'
);
SET @ddl = IF(@user_realname_exists = 0,
    'ALTER TABLE sys_user ADD COLUMN real_name VARCHAR(50) DEFAULT NULL COMMENT ''展示姓名'' AFTER password',
    'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -------------------------------------------------------------
-- 慢病患者建档表
--   建档是医生依据已有诊断资料的独立管理业务，不等于风险评估结论。
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS chronic_disease_record (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    resident_id        BIGINT       NOT NULL COMMENT 'health_profile.id',
    disease_type       VARCHAR(30)  NOT NULL COMMENT 'HYPERTENSION/DIABETES/OTHER',
    diagnosis_status   VARCHAR(20)  NOT NULL DEFAULT 'PENDING_REVIEW' COMMENT 'PENDING_REVIEW-待核实 CONFIRMED-已核实 REJECTED-未通过',
    diagnosis_date     DATE                  DEFAULT NULL COMMENT '诊断日期',
    diagnosis_basis    VARCHAR(500)          DEFAULT NULL COMMENT '诊断依据/资料说明',
    management_status  VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE-管理中 SUSPENDED-暂停 CLOSED-终止',
    managing_doctor_id BIGINT                DEFAULT NULL COMMENT '负责医生 sys_user.id',
    reviewed_by        BIGINT                DEFAULT NULL COMMENT '审核医生 sys_user.id',
    reviewed_at        DATETIME              DEFAULT NULL COMMENT '审核时间',
    remark             VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    deleted            TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    create_time        DATETIME              DEFAULT NULL COMMENT '建档时间',
    update_time        DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_chronic_resident (resident_id, disease_type),
    KEY idx_chronic_doctor (managing_doctor_id, management_status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '慢病患者建档记录';

-- -------------------------------------------------------------
-- 慢病档案变更历史（追加式，不覆盖）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS chronic_disease_history (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    chronic_id  BIGINT       NOT NULL COMMENT 'chronic_disease_record.id',
    resident_id BIGINT       NOT NULL COMMENT 'health_profile.id',
    action      VARCHAR(20)  NOT NULL COMMENT 'CREATE/UPDATE/REVIEW/STATUS_CHANGE',
    detail      VARCHAR(500)          DEFAULT NULL COMMENT '变更内容说明',
    operator_id BIGINT                DEFAULT NULL COMMENT '操作人 sys_user.id',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    create_time DATETIME              DEFAULT NULL COMMENT '操作时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_chronic_history (chronic_id, create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '慢病档案变更历史';

-- -------------------------------------------------------------
-- 居民分组表（管理标签，非诊断结论）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS resident_group (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    group_name        VARCHAR(100) NOT NULL COMMENT '分组名称',
    group_code        VARCHAR(32)  NOT NULL COMMENT '分组编码',
    description       VARCHAR(255)          DEFAULT NULL COMMENT '分组说明',
    community_id      BIGINT                DEFAULT NULL COMMENT '所属社区 community.id',
    manager_doctor_id BIGINT                DEFAULT NULL COMMENT '负责医生 sys_user.id',
    created_by        BIGINT                DEFAULT NULL COMMENT '创建人 sys_user.id',
    status            VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE-启用 DISABLED-停用',
    deleted           TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    create_time       DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time       DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_group_code (group_code),
    KEY idx_group_community (community_id),
    KEY idx_group_manager (manager_doctor_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '居民分组';

-- -------------------------------------------------------------
-- 居民分组成员关联表（独立关联表，支持多对多，同一居民不可重复加入同一分组）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS resident_group_member (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    group_id    BIGINT       NOT NULL COMMENT 'resident_group.id',
    resident_id BIGINT       NOT NULL COMMENT 'health_profile.id',
    added_by    BIGINT                DEFAULT NULL COMMENT '添加人 sys_user.id',
    remark      VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    create_time DATETIME              DEFAULT NULL COMMENT '加入时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_group_member (group_id, resident_id),
    KEY idx_member_resident (resident_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '居民分组成员关联';
