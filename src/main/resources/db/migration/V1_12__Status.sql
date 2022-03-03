UPDATE inntektsmelding
SET data = jsonb_set(data, '{journalStatus}', '"MOTTATT"', false)
WHERE data ->> 'journalStatus' = 'MIDLERTIDIG'
