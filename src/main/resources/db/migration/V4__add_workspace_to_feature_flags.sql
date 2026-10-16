ALTER TABLE feature_flags ADD COLUMN workspace_id UUID NOT NULL;

ALTER TABLE feature_flags DROP CONSTRAINT feature_flags_flag_name_key;
ALTER TABLE feature_flags ADD CONSTRAINT uk_feature_flags_workspace_flag UNIQUE (workspace_id, flag_name);
