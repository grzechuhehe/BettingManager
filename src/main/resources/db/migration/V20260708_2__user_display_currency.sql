ALTER TABLE user ADD COLUMN display_currency VARCHAR(3) NULL;
UPDATE user SET display_currency = 'PLN' WHERE display_currency IS NULL;
