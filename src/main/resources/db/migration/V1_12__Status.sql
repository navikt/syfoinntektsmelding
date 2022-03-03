UPDATE inntektsmelding
SET data = jsonb_set(personb, '{journalStatus}', '"MOTTATT"', false)
WHERE data ->> 'journalStatus' = 'MIDLERTIDIG'
