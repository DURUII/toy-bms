-- 1. 电池类型（可随时扩展）
CREATE TABLE battery_type (
                              battery_type_id   INT          AUTO_INCREMENT PRIMARY KEY,
                              code              VARCHAR(32)  NOT NULL UNIQUE,      -- 例：TERNARY, LFP
                              name              VARCHAR(64)  NOT NULL,             -- 中文名
                              created_at        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
                              updated_at        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                              is_delete         TINYINT(1)   DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO battery_type(code,name)
VALUES ('TERNARY','三元电池'),
       ('LFP','铁锂电池');

----------------------------------------------------------------
-- 2. 车辆
CREATE TABLE vehicle (
                         vid               CHAR(16)     PRIMARY KEY,
                         car_id            INT          NOT NULL UNIQUE,      -- 车架编号
                         battery_type_id   INT          NOT NULL,
                         mileage_km        BIGINT       NOT NULL,
                         health_pct        TINYINT      NOT NULL,
                         created_at        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
                         updated_at        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                         is_delete         TINYINT(1)   DEFAULT 0,
                         CONSTRAINT fk_vehicle_btype FOREIGN KEY (battery_type_id)
                             REFERENCES battery_type(battery_type_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 随机生成 16 位 VID（仅示例，真实可用程序批量插）
INSERT INTO vehicle(vid,car_id,battery_type_id,mileage_km,health_pct)
VALUES
    (SUBSTRING(REPLACE(UUID(),'-',''),1,16),1, (SELECT battery_type_id FROM battery_type WHERE code='TERNARY'),100,100),
    (SUBSTRING(REPLACE(UUID(),'-',''),1,16),2, (SELECT battery_type_id FROM battery_type WHERE code='LFP'),     600, 95),
    (SUBSTRING(REPLACE(UUID(),'-',''),1,16),3, (SELECT battery_type_id FROM battery_type WHERE code='TERNARY'),300, 98);

----------------------------------------------------------------
-- 3. 预警规则（主表）
CREATE TABLE warn_rule (
                           rule_id           BIGINT       AUTO_INCREMENT PRIMARY KEY,
                           rule_no           INT          NOT NULL,             -- 1: 电压差  2: 电流差
                           name              VARCHAR(64)  NOT NULL,
                           expr              VARCHAR(32)  NOT NULL,             -- 计算表达式: MX_MI / IX_II
                           battery_type_id   INT          NOT NULL,
                           created_at        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
                           updated_at        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                           is_delete         TINYINT(1)   DEFAULT 0,
                           CONSTRAINT fk_rule_btype FOREIGN KEY (battery_type_id)
                               REFERENCES battery_type(battery_type_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. 规则区间（子表）
CREATE TABLE warn_rule_item (
                                item_id           BIGINT       AUTO_INCREMENT PRIMARY KEY,
                                rule_id           BIGINT       NOT NULL,
                                min_val           DECIMAL(6,2) DEFAULT NULL,         -- null = -∞
                                max_val           DECIMAL(6,2) DEFAULT NULL,         -- null = +∞
                                warn_level        TINYINT      NOT NULL,             -- 0~4
                                created_at        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
                                updated_at        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                is_delete         TINYINT(1)   DEFAULT 0,
                                CONSTRAINT fk_item_rule FOREIGN KEY (rule_id)
                                    REFERENCES warn_rule(rule_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入四条规则（主表）
INSERT INTO warn_rule(rule_no,name,expr,battery_type_id)
SELECT 1,'电压差报警','MX_MI',battery_type_id FROM battery_type WHERE code='TERNARY' UNION ALL
SELECT 1,'电压差报警','MX_MI',battery_type_id FROM battery_type WHERE code='LFP'     UNION ALL
SELECT 2,'电流差报警','IX_II',battery_type_id FROM battery_type WHERE code='TERNARY' UNION ALL
SELECT 2,'电流差报警','IX_II',battery_type_id FROM battery_type WHERE code='LFP';

-- 插入区间（子表）——三元电池电压差
INSERT INTO warn_rule_item(rule_id,min_val,max_val,warn_level)
SELECT rule_id,5,NULL,0 FROM warn_rule WHERE rule_no=1 AND expr='MX_MI' AND battery_type_id=(SELECT battery_type_id FROM battery_type WHERE code='TERNARY')
UNION ALL SELECT rule_id,3,5,1   FROM warn_rule WHERE rule_no=1 AND expr='MX_MI' AND battery_type_id=(SELECT battery_type_id FROM battery_type WHERE code='TERNARY')
UNION ALL SELECT rule_id,1,3,2   FROM warn_rule WHERE rule_no=1 AND expr='MX_MI' AND battery_type_id=(SELECT battery_type_id FROM battery_type WHERE code='TERNARY')
UNION ALL SELECT rule_id,0.6,1,3 FROM warn_rule WHERE rule_no=1 AND expr='MX_MI' AND battery_type_id=(SELECT battery_type_id FROM battery_type WHERE code='TERNARY')
UNION ALL SELECT rule_id,0.2,0.6,4 FROM warn_rule WHERE rule_no=1 AND expr='MX_MI' AND battery_type_id=(SELECT battery_type_id FROM battery_type WHERE code='TERNARY');

-- 插入区间（子表）——铁锂电池电压差
INSERT INTO warn_rule_item(rule_id,min_val,max_val,warn_level)
SELECT rule_id,2,NULL,0 FROM warn_rule WHERE rule_no=1 AND expr='MX_MI' AND battery_type_id=(SELECT battery_type_id FROM battery_type WHERE code='LFP')
UNION ALL SELECT rule_id,1,2,1   FROM warn_rule WHERE rule_no=1 AND expr='MX_MI' AND battery_type_id=(SELECT battery_type_id FROM battery_type WHERE code='LFP')
UNION ALL SELECT rule_id,0.7,1,2 FROM warn_rule WHERE rule_no=1 AND expr='MX_MI' AND battery_type_id=(SELECT battery_type_id FROM battery_type WHERE code='LFP')
UNION ALL SELECT rule_id,0.4,0.7,3 FROM warn_rule WHERE rule_no=1 AND expr='MX_MI' AND battery_type_id=(SELECT battery_type_id FROM battery_type WHERE code='LFP')
UNION ALL SELECT rule_id,0.2,0.4,4 FROM warn_rule WHERE rule_no=1 AND expr='MX_MI' AND battery_type_id=(SELECT battery_type_id FROM battery_type WHERE code='LFP');

-- 插入区间（子表）——三元电池电流差
INSERT INTO warn_rule_item(rule_id,min_val,max_val,warn_level)
SELECT rule_id,3,NULL,0 FROM warn_rule WHERE rule_no=2 AND expr='IX_II' AND battery_type_id=(SELECT battery_type_id FROM battery_type WHERE code='TERNARY')
UNION ALL SELECT rule_id,1,3,1   FROM warn_rule WHERE rule_no=2 AND expr='IX_II' AND battery_type_id=(SELECT battery_type_id FROM battery_type WHERE code='TERNARY')
UNION ALL SELECT rule_id,0.2,1,2 FROM warn_rule WHERE rule_no=2 AND expr='IX_II' AND battery_type_id=(SELECT battery_type_id FROM battery_type WHERE code='TERNARY');

-- 插入区间（子表）——铁锂电池电流差
INSERT INTO warn_rule_item(rule_id,min_val,max_val,warn_level)
SELECT rule_id,1,NULL,0 FROM warn_rule WHERE rule_no=2 AND expr='IX_II' AND battery_type_id=(SELECT battery_type_id FROM battery_type WHERE code='LFP')
UNION ALL SELECT rule_id,0.5,1,1 FROM warn_rule WHERE rule_no=2 AND expr='IX_II' AND battery_type_id=(SELECT battery_type_id FROM battery_type WHERE code='LFP')
UNION ALL SELECT rule_id,0.2,0.5,2 FROM warn_rule WHERE rule_no=2 AND expr='IX_II' AND battery_type_id=(SELECT battery_type_id FROM battery_type WHERE code='LFP');

----------------------------------------------------------------
-- 5. 信号上报
CREATE TABLE signal (
                        signal_id         BIGINT       AUTO_INCREMENT PRIMARY KEY,
                        vid               CHAR(16)     NOT NULL,
                        mx                DECIMAL(6,2) DEFAULT NULL,
                        mi                DECIMAL(6,2) DEFAULT NULL,
                        ix                DECIMAL(6,2) DEFAULT NULL,
                        ii                DECIMAL(6,2) DEFAULT NULL,
                        ts                TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
                        processed         TINYINT(1)   DEFAULT 0,
                        created_at        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
                        updated_at        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        is_delete         TINYINT(1)   DEFAULT 0,
                        CONSTRAINT fk_signal_vehicle FOREIGN KEY (vid)
                            REFERENCES vehicle(vid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
    PARTITION BY RANGE (UNIX_TIMESTAMP(ts)) (             -- 简易按月分区
        PARTITION p202505 VALUES LESS THAN (UNIX_TIMESTAMP('2025-06-01')),
        PARTITION pmax    VALUES LESS THAN MAXVALUE
        );

----------------------------------------------------------------
-- 6. 预警记录
CREATE TABLE warning (
                         warning_id        BIGINT       AUTO_INCREMENT PRIMARY KEY,
                         signal_id         BIGINT       NOT NULL,
                         rule_id           BIGINT       NOT NULL,
                         warn_level        TINYINT      NOT NULL,
                         created_at        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
                         updated_at        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                         is_delete         TINYINT(1)   DEFAULT 0,
                         CONSTRAINT fk_warning_signal FOREIGN KEY (signal_id)
                             REFERENCES signal(signal_id),
                         CONSTRAINT fk_warning_rule   FOREIGN KEY (rule_id)
                             REFERENCES warn_rule(rule_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
