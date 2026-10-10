-- =============================================================
-- 演示数据（可重复执行，非破坏性）
--   - 使用 INSERT IGNORE：账号/档案已存在时不会被覆盖或重置；
--   - 演示账号密码均为 123456（BCrypt 哈希，仅用于本地演示，生产环境请勿使用）；
--   - 演示数据与居民真实录入的数据通过 sys_user.username 区分（demo 账号命名以 doctor/resident 开头）。
-- =============================================================

USE user_management;

-- 医生账号
INSERT IGNORE INTO sys_user (username, password, real_name, email, phone, status, role, deleted, create_time, update_time)
VALUES ('doctor', '$2a$10$K3Fsg24LmZzQE.2D9uufDOL8Bo2VUmbNC6xuq64A4RpYaf3/XuROG', '陈国栋',
        'cgt@demo.local', '13800000001', 1, 'DOCTOR', 0, NOW(), NOW());

-- 居民账号
INSERT IGNORE INTO sys_user (username, password, real_name, email, phone, status, role, deleted, create_time, update_time)
VALUES ('resident', '$2a$10$K3Fsg24LmZzQE.2D9uufDOL8Bo2VUmbNC6xuq64A4RpYaf3/XuROG', '王建军',
        'wjj@demo.local', '13800000002', 1, 'RESIDENT', 0, NOW(), NOW());

-- 管理员账号（用户管理功能仅管理员可访问）
INSERT IGNORE INTO sys_user (username, password, real_name, email, phone, status, role, deleted, create_time, update_time)
VALUES ('admin', '$2a$10$K3Fsg24LmZzQE.2D9uufDOL8Bo2VUmbNC6xuq64A4RpYaf3/XuROG', '刘志远',
        'lzy@demo.local', '13800000003', 1, 'ADMIN', 0, NOW(), NOW());

-- 居民健康档案
INSERT IGNORE INTO health_profile (user_id, resident_no, real_name, gender, birth_date, community_id, deleted, create_time, update_time)
SELECT u.id, 'R2026000001', '王建军', 'MALE', '1975-06-01', NULL, 0, NOW(), NOW()
FROM sys_user u
WHERE u.username = 'resident';

-- 演示医生-居民授权关系（居民归演示医生负责）
INSERT IGNORE INTO doctor_resident (doctor_id, resident_id, deleted, create_time, update_time)
SELECT d.id, p.id, 0, NOW(), NOW()
FROM sys_user d
JOIN health_profile p ON p.user_id = (SELECT id FROM sys_user WHERE username = 'resident')
WHERE d.username = 'doctor';
