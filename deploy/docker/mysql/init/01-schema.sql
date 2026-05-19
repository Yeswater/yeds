SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

create table if not exists bids_datasource (
  id varchar(36) primary key comment '主键ID',
  code varchar(64) not null unique comment '数据源编码',
  name varchar(128) not null comment '数据源名称',
  jdbc_url varchar(512) not null comment 'JDBC连接URL',
  username varchar(128) not null comment '数据库用户名',
  password varchar(512) not null comment '数据库密码',
  driver_class_name varchar(128) not null comment 'JDBC驱动类名',
  sql_dialect varchar(32) not null default 'MYSQL' comment 'SQL方言：MYSQL,POSTGRESQL等',
  max_pool_size int not null comment '连接池最大连接数',
  active boolean not null default true comment '是否启用',
  created_at timestamp not null default current_timestamp comment '创建时间',
  updated_at timestamp not null default current_timestamp on update current_timestamp comment '更新时间'
) engine=InnoDB default charset=utf8mb4 comment='BIDS业务数据源配置';

create table if not exists bids_sql_model (
  id varchar(36) primary key comment '主键ID',
  code varchar(64) not null unique comment '模型编码',
  name varchar(128) not null comment '模型名称',
  datasource_code varchar(64) not null comment '关联数据源编码',
  sql_template text not null comment 'SQL模板',
  max_rows int not null comment '最大返回行数',
  status varchar(32) not null comment '状态：DRAFT,PUBLISHED,DISABLED',
  created_at timestamp not null default current_timestamp comment '创建时间',
  updated_at timestamp not null default current_timestamp on update current_timestamp comment '更新时间'
) engine=InnoDB default charset=utf8mb4 comment='SQL查询模型定义';

create table if not exists bids_form_field (
  id varchar(36) primary key comment '主键ID',
  model_id varchar(36) not null comment '所属模型ID',
  field_name varchar(64) not null comment '参数字段名',
  label varchar(128) not null comment '表单显示标签',
  field_type varchar(32) not null comment '字段类型：TEXT,NUMBER,DATE,SELECT等',
  required boolean not null default false comment '是否必填',
  default_value varchar(512) comment '默认值',
  options_json text comment '下拉选项JSON',
  sort_order int not null default 0 comment '排序序号'
) engine=InnoDB default charset=utf8mb4 comment='模型查询表单字段';

create table if not exists bids_result_column (
  id varchar(36) primary key comment '主键ID',
  model_id varchar(36) not null comment '所属模型ID',
  column_name varchar(128) not null comment '结果列名',
  label varchar(128) not null comment '列显示标签',
  value_type varchar(32) not null default 'TEXT' comment '值类型：TEXT,NUMBER,DATE等',
  visible boolean not null default true comment '是否可见',
  mask_type varchar(32) comment '脱敏类型',
  sort_order int not null default 0 comment '排序序号'
) engine=InnoDB default charset=utf8mb4 comment='模型查询结果列配置';

create table if not exists bids_model_permission (
  id varchar(36) primary key comment '主键ID',
  model_id varchar(36) not null comment '所属模型ID',
  username varchar(128) comment '授权用户名',
  role_code varchar(128) comment '授权角色编码'
) engine=InnoDB default charset=utf8mb4 comment='模型访问权限';

create table if not exists bids_execute_log (
  id varchar(36) primary key comment '主键ID',
  execute_id varchar(64) not null unique comment '执行流水号',
  model_code varchar(64) not null comment '模型编码',
  username varchar(128) not null comment '执行用户',
  final_sql text not null comment '最终执行SQL',
  parameters_json text not null comment '请求参数JSON',
  success boolean not null comment '是否成功',
  error_message text comment '错误信息',
  duration_ms bigint not null comment '耗时毫秒',
  row_count int not null comment '返回行数',
  created_at timestamp not null default current_timestamp comment '创建时间'
) engine=InnoDB default charset=utf8mb4 comment='模型执行审计日志';

create table if not exists t_order (
  order_id varchar(32) primary key comment '订单号',
  user_name varchar(64) not null comment '用户名',
  amount decimal(12,2) not null comment '订单金额',
  create_time timestamp not null comment '下单时间'
) engine=InnoDB default charset=utf8mb4 comment='演示订单表';

SET FOREIGN_KEY_CHECKS = 1;
