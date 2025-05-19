CREATE TABLE IF NOT EXISTS vehicle (
    vid              CHAR(16)     PRIMARY KEY,
    car_id           INT          NOT NULL UNIQUE,      
    battery_type_id  INT          NOT NULL,
    mileage_km       BIGINT       NOT NULL,
    health_pct       TINYINT      NOT NULL,
    created_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete        TINYINT(1)   DEFAULT 0,
    CONSTRAINT fk_vehicle_btype FOREIGN KEY (battery_type_id)
        REFERENCES battery_type(battery_type_id)
);

CREATE TABLE IF NOT EXISTS warn_rule (
    rule_id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    rule_no          INT          NOT NULL,             
    name             VARCHAR(64)  NOT NULL,
    expr             VARCHAR(32)  NOT NULL,             
    battery_type_id  INT          NOT NULL,
    created_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete        TINYINT(1)   DEFAULT 0,
    CONSTRAINT fk_rule_btype FOREIGN KEY (battery_type_id)
        REFERENCES battery_type(battery_type_id)
);

CREATE TABLE IF NOT EXISTS warn_rule_item (
    item_id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    rule_id          BIGINT       NOT NULL,
    min_val          DECIMAL(6,2) DEFAULT NULL,         
    max_val          DECIMAL(6,2) DEFAULT NULL,         
    warn_level       TINYINT      NOT NULL,             
    created_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete        TINYINT(1)   DEFAULT 0,
    CONSTRAINT fk_item_rule FOREIGN KEY (rule_id)
        REFERENCES warn_rule(rule_id)
);

CREATE TABLE IF NOT EXISTS `signal` (
    id               BIGINT       AUTO_INCREMENT PRIMARY KEY,
    car_id           INT          NOT NULL,
    battery_type_id  INT          NOT NULL,
    signal_data      TEXT         NOT NULL,
    processed        TINYINT(1)   NOT NULL DEFAULT 0,
    is_delete        TINYINT(1)   NOT NULL DEFAULT 0,
    created_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS warning (
    id               BIGINT       AUTO_INCREMENT PRIMARY KEY,
    car_id           INT          NOT NULL,
    battery_type_id  INT          NOT NULL,
    rule_no          INT          NOT NULL,
    rule_name        VARCHAR(64)  NOT NULL,
    warn_level       INT          NOT NULL,
    signal_data      TEXT         NOT NULL,
    is_delete        TINYINT(1)   NOT NULL DEFAULT 0,
    created_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
); 