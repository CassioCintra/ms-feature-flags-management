CREATE TABLE flag_event_outbox (
    id          BIGSERIAL    PRIMARY KEY,
    message_key VARCHAR(511) NOT NULL,
    payload     TEXT         NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);
