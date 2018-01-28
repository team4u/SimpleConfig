DROP TABLE IF EXISTS system_config;

# -----------------------------------------------------------------------
# system_config
# -----------------------------------------------------------------------

CREATE TABLE system_config
(
  id          BIGINT UNSIGNED AUTO_INCREMENT
  COMMENT '自增长标识',
  is_enabled  SMALLINT DEFAULT 1                                                 NOT NULL
  COMMENT '是否开启',
  type        VARCHAR(32) DEFAULT ''                                             NOT NULL
  COMMENT '类型',
  name        VARCHAR(255) DEFAULT ''                                            NOT NULL
  COMMENT '键',
  value       VARCHAR(4000) DEFAULT ''                                           NOT NULL
  COMMENT '值',
  sequence_no BIGINT DEFAULT 0                                                   NOT NULL
  COMMENT '顺序号',
  description VARCHAR(255) DEFAULT ''                                            NOT NULL
  COMMENT '描述',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP                                 NOT NULL
  COMMENT '创建时间',
  update_time DATETIME ON UPDATE CURRENT_TIMESTAMP DEFAULT CURRENT_TIMESTAMP     NOT NULL
  COMMENT '更新时间',
  PRIMARY KEY (id)
)
  COMMENT '系统配置';

CREATE INDEX idx_type_name
  ON system_config (type, name);