MERGE INTO role (id, name, code, description, status) KEY(id)
VALUES
    (1, 'Super Admin', 'SUPER_ADMIN', 'Full platform administrator', 0),
    (2, 'Admin', 'ADMIN', 'Forum administrator', 0),
    (3, 'User', 'USER', 'Regular user', 0),
    (4, 'VIP', 'VIP', 'VIP user', 0);

MERGE INTO permission (id, name, code, resource_type, url, parent_id, sort, status) KEY(id)
VALUES
    (1, 'User Management', 'user:manage', 'menu', '/admin/users', 0, 1, 0),
    (2, 'User List', 'user:list', 'api', '/api/admin/users', 1, 1, 0),
    (3, 'User Detail', 'user:detail', 'api', '/api/admin/users/*', 1, 2, 0),
    (4, 'Edit User', 'user:edit', 'api', '/api/admin/users/*', 1, 3, 0),
    (5, 'Content Management', 'content:manage', 'menu', '/admin/posts', 0, 2, 0),
    (6, 'Post List', 'post:list', 'api', '/api/admin/posts', 5, 1, 0),
    (7, 'Review Post', 'post:review', 'api', '/api/admin/posts/*/review', 5, 2, 0),
    (8, 'Delete Post', 'post:delete', 'api', '/api/admin/posts/*', 5, 3, 0),
    (9, 'Category Management', 'category:manage', 'menu', '/admin/categories', 0, 3, 0),
    (10, 'Category List', 'category:list', 'api', '/api/admin/categories', 9, 1, 0),
    (11, 'Add Category', 'category:add', 'api', '/api/admin/categories', 9, 2, 0),
    (12, 'Edit Category', 'category:edit', 'api', '/api/admin/categories/*', 9, 3, 0),
    (13, 'Delete Category', 'category:delete', 'api', '/api/admin/categories/*', 9, 4, 0),
    (14, 'Board Management', 'board:manage', 'menu', '/admin/boards', 0, 4, 0),
    (15, 'Board List', 'board:list', 'api', '/api/admin/boards', 14, 1, 0),
    (16, 'Add Board', 'board:add', 'api', '/api/admin/boards', 14, 2, 0),
    (17, 'Edit Board', 'board:edit', 'api', '/api/admin/boards/*', 14, 3, 0),
    (18, 'Delete Board', 'board:delete', 'api', '/api/admin/boards/*', 14, 4, 0),
    (19, 'System Management', 'system:manage', 'menu', '/admin/settings', 0, 5, 0),
    (20, 'Admin Dashboard', 'admin:dashboard', 'api', '/api/admin/dashboard', 19, 0, 0),
    (21, 'Role Management', 'role:manage', 'api', '/api/admin/roles', 19, 1, 0),
    (22, 'Permission Management', 'permission:manage', 'api', '/api/admin/permissions', 19, 2, 0),
    (23, 'Search Index Management', 'search:reindex', 'api', '/api/admin/search/posts/reindex', 19, 3, 0);

MERGE INTO role_permission (id, role_id, permission_id) KEY(id)
VALUES
    (1, 1, 1), (2, 1, 2), (3, 1, 3), (4, 1, 4), (5, 1, 5), (6, 1, 6),
    (7, 1, 7), (8, 1, 8), (9, 1, 9), (10, 1, 10), (11, 1, 11), (12, 1, 12),
    (13, 1, 13), (14, 1, 14), (15, 1, 15), (16, 1, 16), (17, 1, 17), (18, 1, 18),
    (19, 1, 19), (20, 1, 20), (21, 1, 21), (22, 1, 22), (43, 1, 23),
    (23, 2, 1), (24, 2, 2), (25, 2, 3), (26, 2, 4), (27, 2, 5), (28, 2, 6),
    (29, 2, 7), (30, 2, 8), (31, 2, 9), (32, 2, 10), (33, 2, 11), (34, 2, 12),
    (35, 2, 13), (36, 2, 14), (37, 2, 15), (38, 2, 16), (39, 2, 17), (40, 2, 18),
    (41, 2, 20), (42, 2, 23);

MERGE INTO board (id, name, code, description, sort, status, post_count) KEY(id)
VALUES (1, 'Default Board', 'default', 'Default board', 1, 0, 0);

MERGE INTO category (id, name, code, description, sort, status) KEY(id)
VALUES (1, 'Tech', 'tech', 'Technology discussions', 1, 0);
