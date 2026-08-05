-- ShedLock's lock table, used to coordinate the cron job scheduler across
-- multiple app instances. Kept as a plain SQL script (not a JPA entity) so it
-- is not affected by hibernate.ddl-auto, and IF NOT EXISTS keeps it safe to
-- run on every boot instead of introducing a migration tool.
CREATE TABLE IF NOT EXISTS shedlock (
    name       VARCHAR(64)    NOT NULL,
    lock_until TIMESTAMP(3)   NOT NULL,
    locked_at  TIMESTAMP(3)   NOT NULL,
    locked_by  VARCHAR(255)   NOT NULL,
    PRIMARY KEY (name)
);
