CREATE TABLE IF NOT EXISTS event
(
    id                 UUID PRIMARY KEY,
    title              VARCHAR(255) NOT NULL,
    description        TEXT,
    date_time          TIMESTAMP WITH TIME ZONE,
    duration           INTERVAL,
    location           VARCHAR(255),
    capacity           INTEGER,
    image_key          VARCHAR(255),
    organizer_id       UUID,
    created_date       TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    last_modified_date TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS event_categories
(
    id                 UUID PRIMARY KEY,
    activity_type      VARCHAR(255) NOT NULL,
    event_id           UUID         NOT NULL,
    created_date       TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    last_modified_date TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT fk_event
        FOREIGN KEY (event_id)
            REFERENCES event (id)
            ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_event_organizer_id
    ON event (organizer_id);

CREATE INDEX IF NOT EXISTS idx_event_categories_event_id
    ON event_categories (event_id);
