-- brukes for å få tilgang til views
set role = 'syfoinntektsmelding-admin';

---------Samme person/virksomhet/AGP med forskjellig første fraværsdag--------------

-- hent inntektsmeldinger på samme aktørid, orgnummer og arbeidsgiverperiode
create or replace view gruppert_admin2 as
    (select * , aktor_id || orgnummer || (data ->> 'arbeidsgiverperioder') as gruppering
    from inntektsmelding
    where jsonb_array_length(data -> 'arbeidsgiverperioder') = 1);

-- men har forskjellig første fraværsdag
 select count(*) from gruppert_admin2 a inner join gruppert_admin2 b on a.gruppering = b.gruppering where
	a.data ->> 'førsteFraværsdag' != b.data ->> 'førsteFraværsdag';

-- - som har forskjellig refusjonsbeløp
 select count(*) from gruppert_admin2 a inner join gruppert_admin2 b on a.gruppering = b.gruppering where
	a.data ->> 'førsteFraværsdag' != b.data ->> 'førsteFraværsdag' and
	a.data -> 'refusjon' ->> 'beloepPrMnd' != b.data -> 'refusjon' ->> 'beloepPrMnd';

-- - som har forskjellig beregnet inntekt
 select count(*) from gruppert_admin2 a inner join gruppert_admin2 b on a.gruppering = b.gruppering where
	a.data ->> 'førsteFraværsdag' != b.data ->> 'førsteFraværsdag' and
	a.data ->> 'beregnetInntekt' != b.data ->> 'beregnetInntekt';

	-- - som grupperer forskjellig refusjonsbeløp på differanse

select count(*) as antall_refusjon,
       case when (ABS((a.data -> 'refusjon' ->> 'beloepPrMnd')::float -  (b.data -> 'refusjon' ->> 'beloepPrMnd')::float) between 0 and 1) then '0-1'
            when (ABS((a.data -> 'refusjon' ->> 'beloepPrMnd')::float -  (b.data -> 'refusjon' ->> 'beloepPrMnd')::float) between 1 and 10) then '1-10'
            when (ABS((a.data -> 'refusjon' ->> 'beloepPrMnd')::float -  (b.data -> 'refusjon' ->> 'beloepPrMnd')::float) between 10 and 100) then '10-100'
            when (ABS((a.data -> 'refusjon' ->> 'beloepPrMnd')::float -  (b.data -> 'refusjon' ->> 'beloepPrMnd')::float) between 100 and 1000) then '100-1000'
            when (ABS((a.data -> 'refusjon' ->> 'beloepPrMnd')::float -  (b.data -> 'refusjon' ->> 'beloepPrMnd')::float) between 1000 and 10000) then '1000-10000'
            when (ABS((a.data -> 'refusjon' ->> 'beloepPrMnd')::float -  (b.data -> 'refusjon' ->> 'beloepPrMnd')::float) > 10000) then '>10000' end
           as differanse
from gruppert_admin2 a inner join gruppert_admin2 b on a.gruppering = b.gruppering where
            a.data ->> 'førsteFraværsdag' != b.data ->> 'førsteFraværsdag' and
                a.data -> 'refusjon' ->> 'beloepPrMnd' != b.data -> 'refusjon' ->> 'beloepPrMnd'
group by differanse;

-- - som grupperer forskjellig beregnet inntekt på differanse

select count(*) as antall_beregnetInntekt,
       case when (ABS((a.data ->> 'beregnetInntekt')::float -  (b.data ->> 'beregnetInntekt')::float) between 0 and 1) then '0-1'
            when (ABS((a.data ->> 'beregnetInntekt')::float -  (b.data ->> 'beregnetInntekt')::float) between 1 and 10) then '1-10'
            when (ABS((a.data ->> 'beregnetInntekt')::float -  (b.data ->> 'beregnetInntekt')::float) between 10 and 100) then '10-100'
            when (ABS((a.data ->> 'beregnetInntekt')::float -  (b.data ->> 'beregnetInntekt')::float) between 100 and 1000) then '100-1000'
            when (ABS((a.data ->> 'beregnetInntekt')::float -  (b.data ->> 'beregnetInntekt')::float) between 1000 and 10000) then '1000-10000'
            when (ABS((a.data ->> 'beregnetInntekt')::float -  (b.data ->> 'beregnetInntekt')::float) > 10000) then '>10000' end
           as differanse
from gruppert_admin2 a inner join gruppert_admin2 b on a.gruppering = b.gruppering where
            a.data ->> 'førsteFraværsdag' != b.data ->> 'førsteFraværsdag' and
                a.data ->> 'beregnetInntekt' != b.data ->> 'beregnetInntekt'
group by differanse;

-- - som har både forskjellig refusjonsbeløp og beregnet inntekt

select count(*) from gruppert_admin2 a inner join gruppert_admin2 b on a.gruppering = b.gruppering where
    a.data ->> 'førsteFraværsdag' != b.data ->> 'førsteFraværsdag' and
    (a.data -> 'refusjon' ->> 'beloepPrMnd' != b.data -> 'refusjon' ->> 'beloepPrMnd' or
        a.data ->> 'beregnetInntekt' != b.data ->> 'beregnetInntekt');

-----------------------------Andel IM sendt fra LPS---------------------------------------------------------

-- IM sendt fra LPS gruppert på systemnavn, legger sammen SAP og Lessor
select count(data) as antall,
       case when data -> 'avsenderSystem' ->> 'navn' LIKE '%SAP%' THEN 'SAP'
            when (data -> 'avsenderSystem' ->> 'navn' LIKE '%LESSOR-Payroll%'
                or data -> 'avsenderSystem' ->> 'navn' LIKE '%Lessor Payroll%') THEN 'Lessor'
           else data -> 'avsenderSystem' ->> 'navn'
        end
as Systemnavn
from inntektsmelding
group by Systemnavn
order by antall desc;

--IM fra LPS vs altinnPortal
select
    sum(case when data -> 'avsenderSystem' ->> 'navn' LIKE 'AltinnPortal' then 1 else 0 end) as altinnPortal,
    sum(case when data -> 'avsenderSystem' ->> 'navn' NOT LIKE 'AltinnPortal' then 1 else 0 end) as lps
from inntektsmelding;


---------------Finne dager mellom AGP og ferie eller første fraværsdag-----------------
select
count(*) as antall
-- TO_DATE(data -> 'feriePerioder' -> 0  ->> 'fom','YYYY-MM-DD') - TO_DATE(data -> 'arbeidsgiverperioder' -> -1  ->> 'tom', 'YYYY-MM-DD') as dager_mellom_ferie_og_agp,
-- TO_DATE(data ->> 'førsteFraværsdag', 'YYYY-MM-DD') - TO_DATE(data -> 'arbeidsgiverperioder' -> -1  ->> 'tom', 'YYYY-MM-DD')  as dager_mellom_agp_og_ff
-- TO_DATE(data -> 'arbeidsgiverperioder' -> -1  ->> 'tom', 'YYYY-MM-DD') as tom,
-- TO_DATE(data -> 'feriePerioder' -> 0  ->> 'fom','YYYY-MM-DD') as fom
from inntektsmelding
where
data -> 'feriePerioder' -> 0  ->> 'fom' is not null
-- and TO_DATE(data -> 'arbeidsgiverperioder' -> -1  ->> 'tom','YYYY-MM-DD') - TO_DATE(data -> 'arbeidsgiverperioder' -> 0  ->> 'fom', 'YYYY-MM-DD') = 15
-- and TO_DATE(data -> 'feriePerioder' -> 0  ->> 'fom','YYYY-MM-DD') - TO_DATE(data -> 'arbeidsgiverperioder' -> -1  ->> 'tom', 'YYYY-MM-DD') > 0
-- and TO_DATE(data ->> 'førsteFraværsdag', 'YYYY-MM-DD') - TO_DATE(data -> 'feriePerioder' -> 0  ->> 'fom', 'YYYY-MM-DD')  > 15
--and TO_DATE(data ->> 'førsteFraværsdag', 'YYYY-MM-DD') - TO_DATE(data -> 'feriePerioder' -> -1  ->> 'tom', 'YYYY-MM-DD')  < 15--and TO_DATE(data -> 'feriePerioder' -> 0  ->> 'fom','YYYY-MM-DD') - TO_DATE(data -> 'arbeidsgiverperioder' -> -
-- 1  ->> 'tom', 'YYYY-MM-DD') < 4
--and TO_DATE(data ->> 'førsteFraværsdag', 'YYYY-MM-DD') - TO_DATE(data -> 'arbeidsgiverperioder' -> -1  ->> 'tom', 'YYYY-MM-DD')  < 21
--  TO_DATE(data -> 'arbeidsgiverperioder' -> -1  ->> 'tom', 'YYYY-MM-DD') + interval '3 days' = TO_DATE(data -> 'feriePerioder' -> 0  ->> 'fom','YYYY-MM-DD')
--group by
--    dager_mellom_ferie_og_agp,
--    dager_mellom_agp_og_ff


---------------Finne totalt antall utbetaling til bruker-----------------
SELECT COUNT(*)
FROM INNTEKTSMELDING i
         INNER JOIN utsatt_oppgave u ON (u.arkivreferanse = i.data ->> 'arkivRefereranse')
WHERE
  -- Må ha gosys_oppgave_id
    u.gosys_oppgave_id IS NOT NULL
  AND
  -- Ikke fra speil
        u.speil = false
  AND NOT
    -- Ikke SYKEPENGER_UTLAND
        u.enhet = '4474'
  AND
  -- refusjon.beloepPrMnd er over 0
        (i.data -> 'refusjon' ->> 'beloepPrMnd')::FLOAT > 0
  AND (
    -- Beregnet inntekt er større enn refusjon.belopPrMnd
            (i.data ->> 'beregnetInntekt')::FLOAT > (i.data -> 'refusjon' ->> 'beloepPrMnd')::FLOAT
        -- Eller opphørsdato er satt
        OR (i.data -> 'refusjon' ->> 'opphoersdato') IS NOT NULL
    )
;


---------------Dato for den eldste utbetaling til bruker-----------------
SELECT i.behandlet
FROM INNTEKTSMELDING i
         INNER JOIN utsatt_oppgave u ON (u.arkivreferanse = i.data ->> 'arkivRefereranse')
WHERE
  -- Må ha gosys_oppgave_id
    u.gosys_oppgave_id IS NOT NULL
  AND
  -- Ikke fra speil
    u.speil = false
  AND NOT
    -- Ikke SYKEPENGER_UTLAND
    u.enhet = '4474'
  AND
  -- refusjon.beloepPrMnd er over 0
    (i.data -> 'refusjon' ->> 'beloepPrMnd')::FLOAT > 0
  AND (
    -- Beregnet inntekt er større enn refusjon.belopPrMnd
            (i.data ->> 'beregnetInntekt')::FLOAT > (i.data -> 'refusjon' ->> 'beloepPrMnd')::FLOAT
        -- Eller opphørsdato er satt
        OR (i.data -> 'refusjon' ->> 'opphoersdato') IS NOT NULL
    )
ORDER BY i.behandlet ASC
LIMIT 1;
