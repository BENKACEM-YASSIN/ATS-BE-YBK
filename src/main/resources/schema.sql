-- Ensure cv_history table exists
CREATE TABLE IF NOT EXISTS cv_history (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) UNIQUE,
    cv_data_json TEXT,
    pinned BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Ensure pinned column exists for older databases
ALTER TABLE cv_history
ADD COLUMN IF NOT EXISTS pinned BOOLEAN DEFAULT FALSE;
