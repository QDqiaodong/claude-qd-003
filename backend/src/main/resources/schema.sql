-- 花卉苗圃：建表 + 种子数据
-- 表结构由 Hibernate 兜底（ddl-auto=update），这里只保证首次启动就有数据

CREATE TABLE IF NOT EXISTS greenhouse (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(32) NOT NULL,
  name VARCHAR(64) NOT NULL,
  kind VARCHAR(16) NOT NULL DEFAULT '育苗棚',
  status VARCHAR(16) NOT NULL DEFAULT '在用',
  PRIMARY KEY (id),
  UNIQUE KEY uk_greenhouse_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS seedbed (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(32) NOT NULL,
  name VARCHAR(64) NOT NULL,
  greenhouse_id BIGINT NULL,
  capacity INT NOT NULL DEFAULT 0,
  status VARCHAR(16) NOT NULL DEFAULT '在用',
  PRIMARY KEY (id),
  UNIQUE KEY uk_seedbed_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS variety (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(32) NOT NULL,
  name VARCHAR(64) NOT NULL,
  category VARCHAR(16) NOT NULL DEFAULT '草本',
  status VARCHAR(16) NOT NULL DEFAULT '在售',
  PRIMARY KEY (id),
  UNIQUE KEY uk_variety_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS nursery_batch (
  id BIGINT NOT NULL AUTO_INCREMENT,
  batch_no VARCHAR(32) NOT NULL,
  variety_id BIGINT NOT NULL,
  seedbed_id BIGINT NULL,
  sow_date DATE NOT NULL,
  expect_out_date DATE NULL,
  plan_qty INT NOT NULL DEFAULT 0,
  actual_qty INT NOT NULL DEFAULT 0,
  grower VARCHAR(32) NULL,
  status VARCHAR(16) NOT NULL DEFAULT '育苗中',
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_batch_no (batch_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS shipment (
  id BIGINT NOT NULL AUTO_INCREMENT,
  shipment_no VARCHAR(32) NOT NULL,
  batch_id BIGINT NOT NULL,
  customer VARCHAR(64) NOT NULL,
  qty INT NOT NULL DEFAULT 0,
  carrier VARCHAR(32) NULL,
  ship_date DATE NULL,
  status VARCHAR(16) NOT NULL DEFAULT '待发货',
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_shipment_no (shipment_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 床位账：一批苗在一张床上占的一段日期；to_date 为空表示还占着
CREATE TABLE IF NOT EXISTS bed_occupancy (
  id BIGINT NOT NULL AUTO_INCREMENT,
  batch_id BIGINT NOT NULL,
  seedbed_id BIGINT NOT NULL,
  from_date DATE NOT NULL,
  to_date DATE NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  KEY idx_occ_seedbed (seedbed_id),
  KEY idx_occ_batch (batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 批次账：转棚调拨单，落账即把占用从旧床切到新床（只有「已落账」）
CREATE TABLE IF NOT EXISTS bed_transfer (
  id BIGINT NOT NULL AUTO_INCREMENT,
  transfer_no VARCHAR(32) NOT NULL,
  batch_id BIGINT NOT NULL,
  from_seedbed_id BIGINT NOT NULL,
  to_seedbed_id BIGINT NOT NULL,
  plan_qty INT NOT NULL DEFAULT 0,
  operator VARCHAR(32) NOT NULL,
  plan_start_date DATE NOT NULL,
  plan_end_date DATE NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT '已落账',
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_transfer_no (transfer_no),
  KEY idx_transfer_batch (batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO greenhouse (id, code, name, kind, status) VALUES
  (1, 'GH-01', '一号育苗温室', '育苗棚', '在用'),
  (2, 'GH-02', '二号成苗温室', '成苗棚', '在用'),
  (3, 'GH-03', '三号炼苗棚',   '炼苗棚', '在用'),
  (4, 'GH-04', '四号育苗温室', '育苗棚', '停用');

INSERT IGNORE INTO seedbed (id, code, name, greenhouse_id, capacity, status) VALUES
  (1, 'SB-101', '一号温室东床', 1, 600,  '在用'),
  (2, 'SB-102', '一号温室西床', 1, 600,  '在用'),
  (3, 'SB-201', '二号温区北床', 2, 800,  '在用'),
  (4, 'SB-202', '二号温区南床', 2, 400,  '维修'),
  (5, 'SB-301', '炼苗区一床',   3, 1000, '在用'),
  (6, 'SB-401', '四号温室单床', 4, 500,  '在用');

INSERT IGNORE INTO variety (id, code, name, category, status) VALUES
  (1, 'V-1001', '矮牵牛',     '草本', '在售'),
  (2, 'V-1002', '万寿菊',     '草本', '在售'),
  (3, 'V-1003', '月季',       '木本', '在售'),
  (4, 'V-1004', '胧月',       '多肉', '在售'),
  (5, 'V-1005', '小番茄',     '蔬果', '在售'),
  (6, 'V-1006', '旧年一串红', '草本', '停用');

INSERT IGNORE INTO nursery_batch (id, batch_no, variety_id, seedbed_id, sow_date, expect_out_date, plan_qty, actual_qty, grower, status, created_at, updated_at) VALUES
  (1, 'NB-0001', 1, 1, DATE_SUB(CURDATE(), INTERVAL 27 DAY), DATE_ADD(CURDATE(), INTERVAL 4 DAY),  500, 0,   '老周', '育苗中', NOW(), NOW()),
  (2, 'NB-0002', 2, 2, DATE_SUB(CURDATE(), INTERVAL 22 DAY), DATE_ADD(CURDATE(), INTERVAL 9 DAY),  520, 0,   '小王', '育苗中', NOW(), NOW()),
  (3, 'NB-0003', 3, 3, DATE_SUB(CURDATE(), INTERVAL 68 DAY), DATE_SUB(CURDATE(), INTERVAL 6 DAY),  600, 560, '老李', '待出圃', NOW(), NOW()),
  (4, 'NB-0004', 4, 5, DATE_SUB(CURDATE(), INTERVAL 107 DAY), DATE_SUB(CURDATE(), INTERVAL 17 DAY), 800, 760, '小陈', '已出圃', NOW(), NOW()),
  (5, 'NB-0005', 5, 1, DATE_ADD(CURDATE(), INTERVAL 9 DAY),  DATE_ADD(CURDATE(), INTERVAL 55 DAY), 400, 0,   '老周', '育苗中', NOW(), NOW()),
  (6, 'NB-0006', 2, 5, DATE_SUB(CURDATE(), INTERVAL 138 DAY), DATE_SUB(CURDATE(), INTERVAL 88 DAY), 800, 0,   '小陈', '已报废', NOW(), NOW()),
  (7, 'NB-0007', 4, 3, DATE_ADD(CURDATE(), INTERVAL 9 DAY),  DATE_ADD(CURDATE(), INTERVAL 65 DAY), 350, 0,   '小吴', '育苗中', NOW(), NOW());

INSERT IGNORE INTO shipment (id, shipment_no, batch_id, customer, qty, carrier, ship_date, status, created_at, updated_at) VALUES
  (1, 'SH-0001', 4, '城西花市',       300, '张师傅', DATE_SUB(CURDATE(), INTERVAL 12 DAY), '已签收', NOW(), NOW()),
  (2, 'SH-0002', 4, '滨河公园管理处', 260, '张师傅', DATE_SUB(CURDATE(), INTERVAL 5 DAY),  '已发货', NOW(), NOW()),
  (3, 'SH-0003', 4, '向阳小学',       200, '李师傅', NULL, '待发货', NOW(), NOW()),
  (4, 'SH-0004', 3, '城南绿化队',     150, '王师傅', DATE_SUB(CURDATE(), INTERVAL 2 DAY),  '已退回', NOW(), NOW()),
  (5, 'SH-0005', 3, '城南绿化队',     100, '王师傅', NULL, '待发货', NOW(), NOW());

-- 床位账初始分段：只给还占着床的批次（育苗中/待出圃）补开放占用段
INSERT INTO bed_occupancy (batch_id, seedbed_id, from_date, to_date, created_at, updated_at)
SELECT b.id, b.seedbed_id, b.sow_date, b.expect_out_date, NOW(), NOW()
FROM nursery_batch b
WHERE b.status IN ('育苗中', '待出圃')
  AND NOT EXISTS (SELECT 1 FROM bed_occupancy o WHERE o.batch_id = b.id);
