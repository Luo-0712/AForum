MERGE INTO role (id, name, code, description, status) KEY(id)
VALUES (1, '普通用户', 'USER', '普通用户', 0);

MERGE INTO board (id, name, code, description, sort, status, post_count) KEY(id)
VALUES (1, '默认板块', 'default', '默认板块', 1, 0, 0);

MERGE INTO category (id, name, code, description, sort, status) KEY(id)
VALUES (1, '技术讨论', 'tech', '技术相关讨论', 1, 0);
