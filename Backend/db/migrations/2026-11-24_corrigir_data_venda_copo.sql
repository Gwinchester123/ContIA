BEGIN;

LOCK TABLE public.vendas, public.venda_itens IN SHARE ROW EXCLUSIVE MODE;

DO $$
DECLARE
    vendas_encontradas BIGINT;
    vendas_atualizadas BIGINT;
BEGIN
    SELECT COUNT(DISTINCT v.id)
    INTO vendas_encontradas
    FROM public.vendas v
    WHERE v.data_hora >= TIMESTAMP '2026-11-24 00:33:00'
      AND v.data_hora < TIMESTAMP '2026-11-24 00:34:00'
      AND (
          v.produto ILIKE '%copo%'
          OR EXISTS (
              SELECT 1
              FROM public.venda_itens i
              WHERE i.venda_id = v.id
                AND i.produto ILIKE '%copo%'
          )
      );

    IF vendas_encontradas <> 1 THEN
        RAISE EXCEPTION
            'Correção cancelada: esperado exatamente 1 venda de copo às 00:33 de 24/11/2026; encontradas %.',
            vendas_encontradas;
    END IF;

    UPDATE public.vendas v
    SET data_hora = TIMESTAMP '2026-11-23 21:30:00'
    WHERE v.data_hora >= TIMESTAMP '2026-11-24 00:33:00'
      AND v.data_hora < TIMESTAMP '2026-11-24 00:34:00'
      AND (
          v.produto ILIKE '%copo%'
          OR EXISTS (
              SELECT 1
              FROM public.venda_itens i
              WHERE i.venda_id = v.id
                AND i.produto ILIKE '%copo%'
          )
      );

    GET DIAGNOSTICS vendas_atualizadas = ROW_COUNT;
    IF vendas_atualizadas <> 1 THEN
        RAISE EXCEPTION
            'Correção cancelada: esperado atualizar 1 venda, atualizadas %.',
            vendas_atualizadas;
    END IF;
END $$;

COMMIT;
