UPDATE inntektsmelding
SET data ->> 'journalStatus' = 'MOTTATT'
WHERE data ->> 'journalStatus' = 'MIDLERTIDIG'
