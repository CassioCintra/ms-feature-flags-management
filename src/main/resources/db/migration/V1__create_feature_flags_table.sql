CREATE TABLE feature_flags (
    id               BIGSERIAL    PRIMARY KEY,
    flag_name        VARCHAR(255) NOT NULL,
    service_name     VARCHAR(255) NOT NULL,
    environment_name VARCHAR(255) NOT NULL,
    enabled          BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (flag_name, service_name, environment_name)
);
