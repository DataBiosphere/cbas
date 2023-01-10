CREATE ROLE cbas_user WITH LOGIN ENCRYPTED PASSWORD 'cbas_password';
CREATE DATABASE cbas_db OWNER cbas_user;

CREATE TABLE test_run_table (
  run_id uuid NOT NULL DEFAULT uuid_generate_v1(),
  engine_id uuid DEFAULT uuid_generate_v1(),
  run_set_id uuid NOT NULL DEFAULT uuid_generate_v1(),
  record_id varchar,
  submission_timestamp timestamp with time zone,
  status varchar,
  last_modified_timestamp timestamp with time zone,
  last_polled_timestamp timestamp with time zone,
  error_messages varchar
);

CREATE TABLE test_run_set_table (
  run_set_id uuid NOT NULL DEFAULT uuid_generate_v1(),
  is_template boolean,
  run_set_name varchar,
  run_set_description varchar,
  status varchar,
  submission_timestamp timestamp with time zone,
  last_modified_timestamp timestamp with time zone,
  last_polled_timestamp timestamp with time zone,
  run_count integer,
  error_count integer,
  input_definition text,
  output_definition text,
  record_type text,
  method_version_id uuid DEFAULT uuid_generate_v1()
);

CREATE TABLE test_method_table (
  method_id uuid NOT NULL DEFAULT uuid_generate_v1(),
  name text,
  description text,
  created timestamp with time zone,
  method_source text,
  last_run_set_id uuid DEFAULT uuid_generate_v1()
);

CREATE TABLE test_method_version_table (
  method_version_id uuid NOT NULL DEFAULT uuid_generate_v1(),
  method_id uuid NOT NULL DEFAULT uuid_generate_v1(),
  method_version_name varchar,
  method_version_description varchar,
  method_version_created timestamp with time zone,
  method_version_last_run_set_id uuid DEFAULT uuid_generate_v1(),
  method_version_url text
);

INSERT INTO test_run_table VALUES (
  '37a83113-5984-4f79-b553-c981fc027f3f',
  'aae4da11-f7b6-47ea-ae2c-bb1c02b63d1a',
  '1679acab-680e-4651-97ad-bbada3ef7585',
  'record1',
  '2023-01-05 16:32:51.707868-05',
  'COMPLETE',
  '2023-01-05 16:35:35.86435-05',
  '2023-01-05 16:35:35.86435-05',
  ''
);

INSERT INTO test_run_set_table VALUES (
  'f249124c-45c1-4e4a-8be1-232106ac4b67',
  'f',
  'db run set',
  'run set for the database',
  'COMPLETE',
  '2023-01-05 16:32:51.707868-05',
  '2023-01-05 16:35:35.86435-05',
  '2023-01-05 16:35:35.86435-05',
  '1',
  '0',
  '[' ||
  '{"input_name":"target_workflow_4.mock_task.input_string_1","input_type":{"type":"primitive","primitive_type":"String"},"source":{"type":"record_lookup","record_attribute":"target_workflow_4_mock_task_input_string_1"}},' ||
  '{"input_name":"target_workflow_4.mock_task.optional_int_1","input_type":{"type":"optional","optional_type":{"type":"primitive","primitive_type":"Int"}},"source":{"type":"record_lookup","record_attribute":"target_workflow_4_mock_task_optional_int_1"}},' ||
  '{"input_name":"target_workflow_4.mock_task.docker","input_type":{"type":"optional","optional_type":{"type":"primitive","primitive_type":"String"}},"source":{"type":"record_lookup","record_attribute":"target_workflow_4_mock_task_docker"}}' ||
  ']',
  '[' ||
  '{"output_name":"target_workflow_4.output_string_9","output_type":{"type":"primitive","primitive_type":"String"},"record_attribute":"target_workflow_4_output_string_9"},{"output_name":"target_workflow_4.output_file_1","output_type":{"type":"primitive","primitive_type":"File"},' ||
  '"record_attribute":"target_workflow_4_output_file_1"},{"output_name":"target_workflow_4.output_string_5","output_type":{"type":"primitive","primitive_type":"String"},"record_attribute":"target_workflow_4_output_string_5"},{"output_name":"target_workflow_4.output_string_4","output_type":{"type":"primitive","primitive_type":"String"},' ||
  '"record_attribute":"target_workflow_4_output_string_4"},{"output_name":"target_workflow_4.output_string_3","output_type":{"type":"primitive","primitive_type":"String"},"record_attribute":"target_workflow_4_output_string_3"},{"output_name":"target_workflow_4.output_file_2",' ||
  '"output_type":{"type":"primitive","primitive_type":"File"},"record_attribute":"target_workflow_4_output_file_2"},{"output_name":"target_workflow_4.output_string_6","output_type":{"type":"primitive","primitive_type":"String"},"record_attribute":"target_workflow_4_output_string_6"},' ||
  '{"output_name":"target_workflow_4.output_string_2","output_type":{"type":"primitive","primitive_type":"String"},"record_attribute":"target_workflow_4_output_string_2"},{"output_name":"target_workflow_4.output_string_10","output_type":{"type":"primitive","primitive_type":"String"},' ||
  '"record_attribute":"target_workflow_4_output_string_10"},{"output_name":"target_workflow_4.output_string_7","output_type":{"type":"primitive","primitive_type":"String"},"record_attribute":"target_workflow_4_output_string_7"},{"output_name":"target_workflow_4.output_string_1",' ||
  '"output_type":{"type":"primitive","primitive_type":"String"},"record_attribute":"target_workflow_4_output_string_1"},{"output_name":"target_workflow_4.output_string_8","output_type":{"type":"primitive","primitive_type":"String"},"record_attribute":"target_workflow_4_output_string_8"}' ||
  ']',
  'FOO',
  '10000000-0000-0000-0000-000000000001'
);

INSERT INTO test_method_table VALUES (
  '00000000-0000-0000-0000-000000000001',
  'DB method',
  'description for test db method',
  '2023-01-05 16:32:19.630612-05',
  'Github',
);

INSERT INTO test_method_version_table VALUES (
  '10000000-0000-0000-0000-000000000001',
  '00000000-0000-0000-0000-000000000001',
  '1.0',
  'description of test method version table',
  '2023-01-05 16:32:19.639412-05',
  'https://helloworld.wdl',
);
