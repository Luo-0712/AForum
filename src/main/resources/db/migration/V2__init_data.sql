-- 初始化角色
INSERT INTO `role` (`name`, `code`, `description`, `status`) VALUES
('超级管理员', 'SUPER_ADMIN', '超级管理员，拥有所有权限', 0),
('管理员', 'ADMIN', '管理员，拥有大部分管理权限', 0),
('普通用户', 'USER', '普通用户，拥有基础权限', 0),
('VIP用户', 'VIP', 'VIP用户，拥有更多权限', 0);

-- 初始化权限
INSERT INTO `permission` (`name`, `code`, `resource_type`, `url`, `parent_id`, `sort`, `status`) VALUES
-- 用户管理
('用户管理', 'user:manage', 'menu', '/admin/user', 0, 1, 0),
('用户列表', 'user:list', 'api', '/api/admin/users', 1, 1, 0),
('用户详情', 'user:detail', 'api', '/api/admin/users/*', 1, 2, 0),
('编辑用户', 'user:edit', 'api', '/api/admin/users/*', 1, 3, 0),
('删除用户', 'user:delete', 'api', '/api/admin/users/*', 1, 4, 0),

-- 内容管理
('内容管理', 'content:manage', 'menu', '/admin/content', 0, 2, 0),
('帖子列表', 'post:list', 'api', '/api/admin/posts', 6, 1, 0),
('审核帖子', 'post:review', 'api', '/api/admin/posts/*/review', 6, 2, 0),
('删除帖子', 'post:delete', 'api', '/api/admin/posts/*', 6, 3, 0),

-- 分类管理
('分类管理', 'category:manage', 'menu', '/admin/category', 0, 3, 0),
('分类列表', 'category:list', 'api', '/api/admin/categories', 10, 1, 0),
('添加分类', 'category:add', 'api', '/api/admin/categories', 10, 2, 0),
('编辑分类', 'category:edit', 'api', '/api/admin/categories/*', 10, 3, 0),
('删除分类', 'category:delete', 'api', '/api/admin/categories/*', 10, 4, 0),

-- 板块管理
('板块管理', 'board:manage', 'menu', '/admin/board', 0, 4, 0),
('板块列表', 'board:list', 'api', '/api/admin/boards', 15, 1, 0),
('添加板块', 'board:add', 'api', '/api/admin/boards', 15, 2, 0),
('编辑板块', 'board:edit', 'api', '/api/admin/boards/*', 15, 3, 0),
('删除板块', 'board:delete', 'api', '/api/admin/boards/*', 15, 4, 0),

-- 系统管理
('系统管理', 'system:manage', 'menu', '/admin/system', 0, 5, 0),
('角色管理', 'role:manage', 'api', '/api/admin/roles', 20, 1, 0),
('权限管理', 'permission:manage', 'api', '/api/admin/permissions', 20, 2, 0);

-- 超级管理员拥有所有权限
INSERT INTO `role_permission` (`role_id`, `permission_id`)
SELECT 1, `id` FROM `permission`;

-- 管理员拥有内容管理权限
INSERT INTO `role_permission` (`role_id`, `permission_id`)
SELECT 2, `id` FROM `permission` WHERE `code` LIKE 'content:%' OR `code` LIKE 'post:%' OR `code` LIKE 'category:%' OR `code` LIKE 'board:%';

-- 初始化超级管理员用户（密码：admin123）
INSERT INTO `user` (`username`, `password`, `email`, `nickname`, `user_type`, `status`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'admin@aforum.com', '超级管理员', 3, 0);

-- 超级管理员角色关联
INSERT INTO `user_role` (`user_id`, `role_id`) VALUES (1, 1);

-- 初始化板块
INSERT INTO `board` (`name`, `code`, `description`, `sort`, `status`) VALUES
('默认板块', 'default', '默认板块', 1, 0);

-- 初始化分类
INSERT INTO `category` (`name`, `code`, `description`, `sort`, `status`) VALUES
('技术讨论', 'tech', '技术相关讨论', 1, 0),
('问题求助', 'help', '问题求助', 2, 0),
('经验分享', 'share', '经验分享', 3, 0),
('闲聊灌水', 'chat', '闲聊灌水', 4, 0);
