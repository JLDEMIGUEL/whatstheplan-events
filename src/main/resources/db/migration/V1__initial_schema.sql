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

CREATE INDEX idx_event_organizer_id ON event (organizer_id);
CREATE INDEX idx_event_date_time ON event (date_time);
CREATE INDEX idx_event_location ON event (location);

CREATE TABLE IF NOT EXISTS category
(
    id   UUID PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL
);
CREATE INDEX idx_categories_name ON category (name);

CREATE TABLE IF NOT EXISTS event_categories
(
    id          UUID PRIMARY KEY,
    event_id    UUID NOT NULL,
    category_id UUID NOT NULL,
    CONSTRAINT fk_event
        FOREIGN KEY (event_id)
            REFERENCES event (id)
            ON DELETE CASCADE,
    CONSTRAINT fk_category
        FOREIGN KEY (category_id)
            REFERENCES category (id)
            ON DELETE CASCADE,
    CONSTRAINT unique_event_category UNIQUE (event_id, category_id)
);
CREATE INDEX idx_mapping_category_id ON event_categories (category_id);