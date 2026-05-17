create table if not exists bids_export_task (
  id varchar(64) primary key,
  model_code varchar(64) not null,
  username varchar(128) not null,
  parameters_json text not null,
  final_sql text,
  status varchar(32) not null,
  mode varchar(16) not null,
  file_format varchar(16) not null,
  estimated_rows bigint,
  actual_rows bigint,
  truncated tinyint(1) not null default 0,
  progress_pct int not null default 0,
  error_message varchar(1024),
  rustfs_bucket varchar(128),
  rustfs_object_key varchar(512),
  file_size_bytes bigint,
  download_expires_at timestamp null,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  finished_at timestamp null
);

create index idx_export_task_user_created on bids_export_task(username, created_at desc);
create index idx_export_task_status on bids_export_task(status);
