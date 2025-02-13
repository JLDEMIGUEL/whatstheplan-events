CREATE TABLE IF NOT EXISTS registration
(
    id                 UUID PRIMARY KEY,
    event_id           UUID NOT NULL,
    user_id            UUID NOT NULL,
    created_date       TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    last_modified_date TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT fk_event_registration_event
        FOREIGN KEY (event_id)
            REFERENCES event (id)
            ON DELETE CASCADE,
    CONSTRAINT unique_event_registration UNIQUE (event_id, user_id)
);

CREATE INDEX idx_event_registration_event ON registration (event_id);
CREATE INDEX idx_event_registration_user ON registration (user_id);
