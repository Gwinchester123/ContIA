BEGIN;

ALTER TABLE estabelecimentos
    ADD COLUMN IF NOT EXISTS plano VARCHAR(20) NOT NULL DEFAULT 'BASICO';

UPDATE estabelecimentos
SET plano = 'BASICO'
WHERE plano IS NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_estabelecimentos_plano'
          AND conrelid = 'estabelecimentos'::regclass
    ) THEN
        ALTER TABLE estabelecimentos
            ADD CONSTRAINT ck_estabelecimentos_plano
            CHECK (plano IN ('BASICO', 'PREMIUM'));
    END IF;
END $$;

COMMIT;
