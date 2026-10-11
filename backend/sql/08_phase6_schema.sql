-- =============================================================
-- 第六阶段数据库结构（增量、可重复执行）
--   在线咨询 consultation + 咨询消息 consultation_message
--   健康知识 health_article
--   社区公告 system_notice
-- 说明：
--   1. 全部 CREATE TABLE IF NOT EXISTS，可重复执行，不删除、不重建已有表；
--   2. 命名沿用项目约定：snake_case、create_time/update_time、deleted 逻辑删除、id-type=auto；
--      因此状态字段使用 create_time/update_time，而非规格里的 created_at/updated_at；
--   3. consultation_message 追加式保存，一次咨询多条消息，新消息不覆盖历史对话；
--   4. 不覆盖第一至第五阶段数据。
-- =============================================================

USE user_management;

-- -------------------------------------------------------------
-- 在线健康咨询表
--   居民发起；doctor_id 为空表示尚未受理，由有权限的医生主动受理后写入。
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS consultation (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    resident_id  BIGINT        NOT NULL COMMENT '居民 health_profile.id',
    doctor_id    BIGINT                 DEFAULT NULL COMMENT '受理医生 sys_user.id，可为空（未受理）',
    title        VARCHAR(100)  NOT NULL COMMENT '咨询标题',
    category     VARCHAR(30)   NOT NULL COMMENT 'HEALTH_INDICATOR-健康指标咨询 CHRONIC_MANAGEMENT-慢病管理 LIFESTYLE-生活方式指导 OTHER-其他',
    description  VARCHAR(2000) NOT NULL COMMENT '问题描述',
    status       VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING-待受理 PROCESSING-处理中 RESOLVED-已解决 CLOSED-已关闭',
    accepted_at  DATETIME               DEFAULT NULL COMMENT '受理时间',
    resolved_at  DATETIME               DEFAULT NULL COMMENT '解决时间',
    closed_at    DATETIME               DEFAULT NULL COMMENT '关闭/取消时间',
    deleted      TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    create_time  DATETIME               DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME               DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_consultation_resident (resident_id),
    KEY idx_consultation_doctor_status (doctor_id, status),
    KEY idx_consultation_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '在线健康咨询';

-- -------------------------------------------------------------
-- 咨询消息表
--   一次咨询可有多条消息，追加式保存；sender_role 由后端按登录身份写入，不信任前端。
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS consultation_message (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    consultation_id BIGINT        NOT NULL COMMENT '咨询 consultation.id',
    sender_id       BIGINT        NOT NULL COMMENT '发送者 sys_user.id',
    sender_role     VARCHAR(20)   NOT NULL COMMENT 'RESIDENT-居民 DOCTOR-医生',
    content         VARCHAR(1000) NOT NULL COMMENT '消息内容',
    deleted         TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    create_time     DATETIME               DEFAULT NULL COMMENT '创建时间',
    update_time     DATETIME               DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_message_consultation (consultation_id),
    KEY idx_message_sender (sender_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '咨询消息';

-- -------------------------------------------------------------
-- 健康知识文章表
--   管理员维护；居民只能读取 PUBLISHED 的文章。
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS health_article (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    title          VARCHAR(100) NOT NULL COMMENT '标题',
    summary        VARCHAR(500)          DEFAULT NULL COMMENT '摘要',
    content        TEXT                  DEFAULT NULL COMMENT '正文（纯文本/受控 Markdown）',
    category       VARCHAR(30)  NOT NULL DEFAULT 'OTHER' COMMENT 'CHRONIC-慢病科普 DIET-饮食营养 EXERCISE-运动健康 LIFESTYLE-生活方式 ELDERLY-老年保健 OTHER-其他',
    cover_url      VARCHAR(255)          DEFAULT NULL COMMENT '封面地址，可为空',
    author_id      BIGINT       NOT NULL COMMENT '作者（管理员）sys_user.id',
    publish_status VARCHAR(20)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT-草稿 PUBLISHED-已发布 OFFLINE-已下架',
    published_at   DATETIME              DEFAULT NULL COMMENT '发布时间，可为空',
    deleted        TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    create_time    DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time    DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_article_status (publish_status),
    KEY idx_article_category (category),
    KEY idx_article_published (published_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '健康知识文章';

-- -------------------------------------------------------------
-- 社区公告表
--   管理员维护；居民只能读取 PUBLISHED 且处于有效期内的公告。
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS system_notice (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    title           VARCHAR(100) NOT NULL COMMENT '公告标题',
    content         TEXT                  DEFAULT NULL COMMENT '公告正文',
    publisher_id    BIGINT       NOT NULL COMMENT '发布人（管理员）sys_user.id',
    publish_status  VARCHAR(20)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT-草稿 PUBLISHED-已发布 OFFLINE-已下架',
    published_at    DATETIME              DEFAULT NULL COMMENT '发布时间，可为空',
    effective_from  DATETIME              DEFAULT NULL COMMENT '生效开始时间，可为空（空表示不限制）',
    effective_until DATETIME              DEFAULT NULL COMMENT '生效结束时间，可为空（空表示不限制）',
    is_top          TINYINT      NOT NULL DEFAULT 0 COMMENT '是否置顶 0-否 1-是',
    deleted         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    create_time     DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time     DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_notice_status (publish_status),
    KEY idx_notice_top_published (is_top, published_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '社区公告';
