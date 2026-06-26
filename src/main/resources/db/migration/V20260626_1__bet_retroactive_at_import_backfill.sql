-- Backfill retroactive / pre-match flags (kolumna tworzona przez Hibernate: retroactive_at_import).
-- Uruchom ręcznie na istniejącej bazie, jeśli nie używasz RetroactiveImportBackfillRunner.

UPDATE bet
SET retroactive_at_import = 1
WHERE is_ai_extracted = 1
  AND status IN ('WON', 'LOST', 'VOID', 'HALF_WON', 'HALF_LOST', 'CASHED_OUT')
  AND (resolution_source IS NULL OR resolution_source != 'APIFY_SOFASCORE');

UPDATE bet
SET is_pre_match = 1
WHERE resolution_source = 'APIFY_SOFASCORE';

UPDATE bet
SET retroactive_at_import = 0
WHERE resolution_source = 'APIFY_SOFASCORE' OR status = 'PENDING';
