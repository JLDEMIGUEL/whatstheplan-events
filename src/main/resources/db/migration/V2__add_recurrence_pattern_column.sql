ALTER TABLE IF EXISTS event
    ADD COLUMN recurrence TEXT;

CREATE INDEX idx_event_datetime ON event (date_time);
CREATE INDEX idx_event_recurrence ON event (recurrence) WHERE recurrence IS NOT NULL;