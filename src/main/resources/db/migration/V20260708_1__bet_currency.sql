-- Add currency column for imported bet stakes (Epic A: OCR stake/currency normalization)
ALTER TABLE bet ADD COLUMN currency VARCHAR(3) NULL;
UPDATE bet SET currency = 'PLN' WHERE currency IS NULL AND stake IS NOT NULL;
