-- M2.4 搜索功能权限：管理员可查看搜索健康状态并重建帖子索引。
INSERT IGNORE INTO `permission` (`name`, `code`, `resource_type`, `url`, `parent_id`, `sort`, `status`) VALUES
('搜索索引管理', 'search:reindex', 'api', '/api/admin/search/posts/reindex', 19, 3, 0);

INSERT IGNORE INTO `role_permission` (`role_id`, `permission_id`)
SELECT r.`id`, p.`id`
FROM `role` r
JOIN `permission` p ON p.`code` = 'search:reindex'
WHERE r.`code` IN ('SUPER_ADMIN', 'ADMIN');
