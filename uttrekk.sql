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
	a.data ->> 'førsteFraværsdag' != b.data ->> 'førsteFraværsdag'

-- - som har forskjellig refusjonsbeløp
 select count(*) from gruppert_admin2 a inner join gruppert_admin2 b on a.gruppering = b.gruppering where
	a.data ->> 'førsteFraværsdag' != b.data ->> 'førsteFraværsdag' and
	a.data -> 'refusjon' ->> 'beloepPrMnd' != b.data -> 'refusjon' ->> 'beloepPrMnd';

-- - som har forskjellig beregnet inntekt
 select count(*) from gruppert_admin2 a inner join gruppert_admin2 b on a.gruppering = b.gruppering where
	a.data ->> 'førsteFraværsdag' != b.data ->> 'førsteFraværsdag' and
	a.data ->> 'beregnetInntekt' != b.data ->> 'beregnetInntekt';

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




