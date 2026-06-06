MERGE INTO role (id, name, code, description, status) KEY(id)
VALUES
    (1, '超级管理员', 'SUPER_ADMIN', '超级管理员，拥有所有权限', 0),
    (2, '管理员', 'ADMIN', '管理员，拥有管理权限', 0),
    (3, '普通用户', 'USER', '普通用户', 0),
    (4, 'VIP用户', 'VIP', 'VIP用户', 0);

MERGE INTO permission (id, name, code, resource_type, url, parent_id, sort, status) KEY(id)
VALUES
    (1, '用户管理', 'user:manage', 'menu', '/admin/users', 0, 1, 0),
    (2, '用户列表', 'user:list', 'api', '/api/admin/users', 1, 1, 0),
    (3, '用户详情', 'user:detail', 'api', '/api/admin/users/*', 1, 2, 0),
    (4, '编辑用户', 'user:edit', 'api', '/api/admin/users/*', 1, 3, 0),
    (5, '内容管理', 'content:manage', 'menu', '/admin/posts', 0, 2, 0),
    (6, '帖子列表', 'post:list', 'api', '/api/admin/posts', 5, 1, 0),
    (7, '审核帖子', 'post:review', 'api', '/api/admin/posts/*/review', 5, 2, 0),
    (8, '删除帖子', 'post:delete', 'api', '/api/admin/posts/*', 5, 3, 0),
    (9, '分类管理', 'category:manage', 'menu', '/admin/categories', 0, 3, 0),
    (10, '分类列表', 'category:list', 'api', '/api/admin/categories', 9, 1, 0),
    (11, '添加分类', 'category:add', 'api', '/api/admin/categories', 9, 2, 0),
    (12, '编辑分类', 'category:edit', 'api', '/api/admin/categories/*', 9, 3, 0),
    (13, '删除分类', 'category:delete', 'api', '/api/admin/categories/*', 9, 4, 0),
    (14, '板块管理', 'board:manage', 'menu', '/admin/boards', 0, 4, 0),
    (15, '板块列表', 'board:list', 'api', '/api/admin/boards', 14, 1, 0),
    (16, '添加板块', 'board:add', 'api', '/api/admin/boards', 14, 2, 0),
    (17, '编辑板块', 'board:edit', 'api', '/api/admin/boards/*', 14, 3, 0),
    (18, '删除板块', 'board:delete', 'api', '/api/admin/boards/*', 14, 4, 0),
    (19, '系统管理', 'system:manage', 'menu', '/admin/settings', 0, 5, 0),
    (20, '管理仪表盘', 'admin:dashboard', 'api', '/api/admin/dashboard', 19, 0, 0),
    (21, '角色管理', 'role:manage', 'api', '/api/admin/roles', 19, 1, 0),
    (22, '权限管理', 'permission:manage', 'api', '/api/admin/permissions', 19, 2, 0);

MERGE INTO role_permission (id, role_id, permission_id) KEY(id)
VALUES
    (1, 1, 1), (2, 1, 2), (3, 1, 3), (4, 1, 4), (5, 1, 5), (6, 1, 6),
    (7, 1, 7), (8, 1, 8), (9, 1, 9), (10, 1, 10), (11, 1, 11), (12, 1, 12),
    (13, 1, 13), (14, 1, 14), (15, 1, 15), (16, 1, 16), (17, 1, 17), (18, 1, 18),
    (19, 1, 19), (20, 1, 20), (21, 1, 21), (22, 1, 22),
    (23, 2, 1), (24, 2, 2), (25, 2, 3), (26, 2, 4), (27, 2, 5), (28, 2, 6),
    (29, 2, 7), (30, 2, 8), (31, 2, 9), (32, 2, 10), (33, 2, 11), (34, 2, 12),
    (35, 2, 13), (36, 2, 14), (37, 2, 15), (38, 2, 16), (39, 2, 17), (40, 2, 18),
    (41, 2, 20);

MERGE INTO board (id, name, code, description, sort, status, post_count) KEY(id)
VALUES (1, '默认板块', 'default', '默认板块', 1, 0, 0);

MERGE INTO category (id, name, code, description, sort, status) KEY(id)
VALUES (1, '技术讨论', 'tech', '技术相关讨论', 1, 0);
