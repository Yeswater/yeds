create table if not exists bids_export_task (
  id varchar(64) primary key comment '任务ID',
  model_code varchar(64) not null comment '模型编码',
  username varchar(128) not null comment '发起用户',
  parameters_json text not null comment '导出参数JSON',
  final_sql text comment '最终SQL',
  status varchar(32) not null comment 'PENDING,RUNNING,SUCCESS,FAILED,CANCELLED',
  mode varchar(16) not null comment 'SYNC,ASYNC',
  file_format varchar(16) not null comment 'xlsx,zip',
  estimated_rows bigint comment '预估行数',
  actual_rows bigint comment '实际行数',
  truncated tinyint(1) not null default 0 comment '是否截断',
  progress_pct int not null default 0 comment '进度百分比',
  error_message varchar(1024) comment '错误信息',
  rustfs_bucket varchar(128) comment '对象存储桶',
  rustfs_object_key varchar(512) comment '对象存储键',
  file_size_bytes bigint comment '文件大小字节',
  download_expires_at timestamp null comment '下载链接过期时间',
  created_at timestamp not null default current_timestamp comment '创建时间',
  updated_at timestamp not null default current_timestamp comment '更新时间',
  finished_at timestamp null comment '完成时间'
) engine=InnoDB default charset=utf8mb4 comment='异步导出任务';

create index idx_export_task_user_created on bids_export_task(username, created_at desc);
create index idx_export_task_status on bids_export_task(status);
