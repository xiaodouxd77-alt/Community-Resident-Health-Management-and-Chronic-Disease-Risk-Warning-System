-- =============================================================
-- 第二阶段演示数据（可重复执行，非破坏性）
--   - 使用 INSERT IGNORE / 条件 UPDATE，不覆盖已有数据；
--   - 演示账号密码均为 123456（BCrypt 哈希，仅本地演示用）。
-- =============================================================

USE user_management;

-- 社区参照数据
INSERT IGNORE INTO community (community_code, community_name, deleted, create_time, update_time)
VALUES ('C001', '幸福社区', 0, NOW(), NOW()),
       ('C002', '康乐社区', 0, NOW(), NOW()),
       ('C003', '和平社区', 0, NOW(), NOW());

-- 为第一阶段演示居民补全归属社区（仅在为空时设置，不覆盖已有值）
UPDATE health_profile p
   JOIN community c ON c.community_code = 'C001'
   SET p.community_id = c.id
 WHERE p.resident_no = 'R2026000001'
   AND p.community_id IS NULL;

-- 补充两名居民，便于医生端居民列表、分组与慢病档案演示
INSERT IGNORE INTO sys_user (username, password, real_name, email, phone, status, role, deleted, create_time, update_time)
VALUES ('demo_resident_2', '$2a$10$K3Fsg24LmZzQE.2D9uufDOL8Bo2VUmbNC6xuq64A4RpYaf3/XuROG', '张明',
        'zm@demo.local', '13800000005', 1, 'RESIDENT', 0, NOW(), NOW()),
       ('demo_resident_3', '$2a$10$K3Fsg24LmZzQE.2D9uufDOL8Bo2VUmbNC6xuq64A4RpYaf3/XuROG', '李芳',
        'lf@demo.local', '13800000006', 1, 'RESIDENT', 0, NOW(), NOW());

INSERT IGNORE INTO health_profile (user_id, resident_no, real_name, gender, birth_date, community_id, deleted, create_time, update_time)
SELECT u.id, 'R2026000002', '张明', 'MALE', '1968-03-12',
       (SELECT id FROM community WHERE community_code = 'C002'), 0, NOW(), NOW()
FROM sys_user u WHERE u.username = 'demo_resident_2';

INSERT IGNORE INTO health_profile (user_id, resident_no, real_name, gender, birth_date, community_id, deleted, create_time, update_time)
SELECT u.id, 'R2026000003', '李芳', 'FEMALE', '1972-11-20',
       (SELECT id FROM community WHERE community_code = 'C001'), 0, NOW(), NOW()
FROM sys_user u WHERE u.username = 'demo_resident_3';

-- 修复早期占位展示名（仅在仍为占位值/空值时更新，不覆盖人工修改）
UPDATE sys_user SET real_name = '陈国栋' WHERE username = 'doctor' AND (real_name IS NULL OR real_name = '');
UPDATE sys_user SET real_name = '刘志远' WHERE username = 'admin' AND (real_name IS NULL OR real_name = '');
UPDATE sys_user SET real_name = '王建军' WHERE username = 'resident' AND (real_name IS NULL OR real_name = '');
UPDATE sys_user SET real_name = '张明' WHERE username = 'demo_resident_2' AND (real_name IS NULL OR real_name = '');
UPDATE sys_user SET real_name = '李芳' WHERE username = 'demo_resident_3' AND (real_name IS NULL OR real_name = '');
UPDATE health_profile SET real_name = '王建军' WHERE resident_no = 'R2026000001' AND real_name = '演示居民';

-- 演示医生负责以上居民（居民已有归属时 INSERT IGNORE 不会重复）
INSERT IGNORE INTO doctor_resident (doctor_id, resident_id, deleted, create_time, update_time)
SELECT d.id, p.id, 0, NOW(), NOW()
FROM sys_user d
JOIN health_profile p ON p.resident_no IN ('R2026000002', 'R2026000003')
WHERE d.username = 'doctor';
