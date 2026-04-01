-- V4: Add preferred_engine column to investigations
-- Stores the engine preference chosen at investigation creation time.
-- Values: 'python' (Standard) | 'agent-zero' (Deep Investigation)
-- NULL means use global osint.engine config (backward compatible)

ALTER TABLE investigations ADD COLUMN preferred_engine TEXT DEFAULT NULL;
