INSERT INTO `permission` (`name`, `code`, `resource_type`, `url`, `parent_id`, `sort`, `status`)
SELECT '内容审核列表', 'post:audit:list', 'api', '/api/admin/audit/pending',
       COALESCE((SELECT p.`id` FROM `permission` p WHERE p.`code` = 'content:manage' LIMIT 1), 0), 4, 0
WHERE NOT EXISTS (SELECT 1 FROM `permission` p WHERE p.`code` = 'post:audit:list');

INSERT INTO `permission` (`name`, `code`, `resource_type`, `url`, `parent_id`, `sort`, `status`)
SELECT '后台统计趋势', 'admin:stats:trends', 'api', '/api/admin/stats/trends',
       COALESCE((SELECT p.`id` FROM `permission` p WHERE p.`code` = 'system:manage' LIMIT 1), 0), 4, 0
WHERE NOT EXISTS (SELECT 1 FROM `permission` p WHERE p.`code` = 'admin:stats:trends');

INSERT INTO `role_permission` (`role_id`, `permission_id`)
SELECT r.`id`, p.`id`
FROM `role` r
JOIN `permission` p ON p.`code` IN ('post:audit:list', 'admin:stats:trends')
WHERE r.`code` = 'SUPER_ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM `role_permission` rp
      WHERE rp.`role_id` = r.`id` AND rp.`permission_id` = p.`id`
  );

INSERT INTO `role_permission` (`role_id`, `permission_id`)
SELECT r.`id`, p.`id`
FROM `role` r
JOIN `permission` p ON p.`code` IN ('post:audit:list')
WHERE r.`code` = 'ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM `role_permission` rp
      WHERE rp.`role_id` = r.`id` AND rp.`permission_id` = p.`id`
  );
