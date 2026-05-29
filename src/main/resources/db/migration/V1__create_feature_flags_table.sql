CREATE TABLE feature_flags (
    id         BIGSERIAL    PRIMARY KEY,
    flag_name  VARCHAR(255) NOT NULL UNIQUE,
    service_name VARCHAR(255) NOT NULL,
    type       VARCHAR(20)  NOT NULL DEFAULT 'BOOLEAN',
    rollout    INTEGER      DEFAULT NULL,
    owner      VARCHAR(255) DEFAULT NULL,
    expires_at DATE         DEFAULT NULL,
    enabled    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE flag_environments (
    flag_id  BIGINT       NOT NULL REFERENCES feature_flags(id) ON DELETE CASCADE,
    env_name VARCHAR(255) NOT NULL,
    PRIMARY KEY (flag_id, env_name)
);

CREATE TABLE flag_tags (
    flag_id  BIGINT       NOT NULL REFERENCES feature_flags(id) ON DELETE CASCADE,
    tag_name VARCHAR(255) NOT NULL,
    PRIMARY KEY (flag_id, tag_name)
);
