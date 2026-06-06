-- M2.1 RBAC 增量补齐：仪表盘权限、后台角色授权、admin 种子密码。
INSERT IGNORE INTO `permission` (`name`, `code`, `resource_type`, `url`, `parent_id`, `sort`, `status`) VALUES
('管理仪表盘', 'admin:dashboard', 'api', '/api/admin/dashboard', 20, 0, 0),
('角色管理', 'role:manage', 'api', '/api/admin/roles', 20, 1, 0),
('权限管理', 'permission:manage', 'api', '/api/admin/permissions', 20, 2, 0);

-- 超级管理员始终拥有全部启用权限。
INSERT IGNORE INTO `role_permission` (`role_id`, `permission_id`)
SELECT r.`id`, p.`id`
FROM `role` r
JOIN `permission` p ON p.`status` = 0
WHERE r.`code` = 'SUPER_ADMIN';

-- 普通管理员拥有后台入口、用户基础管理、内容/分类/板块管理权限，不包含角色/权限管理。
INSERT IGNORE INTO `role_permission` (`role_id`, `permission_id`)
SELECT r.`id`, p.`id`
FROM `role` r
JOIN `permission` p ON p.`status` = 0
WHERE r.`code` = 'ADMIN'
  AND (
      p.`code` IN ('admin:dashboard', 'user:manage', 'user:list', 'user:detail', 'user:edit')
      OR p.`code` LIKE 'content:%'
      OR p.`code` LIKE 'post:%'
      OR p.`code` LIKE 'category:%'
      OR p.`code` LIKE 'board:%'
  );

-- 使用确定可用的 BCrypt(admin123) 修正早期种子密码。
UPDATE `user`
SET `password` = '$2a$10$k7.irS3tWreE.D0XozyhqOHk5WLReT2EGyZE49rj6ZkWeLuTsTxHK'
WHERE `username` = 'admin';
