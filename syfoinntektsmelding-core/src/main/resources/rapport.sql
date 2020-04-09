select data -> 'avsenderSystem' ->> 'navn' as AvsenderSystem, data -> 'avsenderSystem' ->> 'versjon' as AvsenderVersjon
from inntektsmelding
where data ->> 'mottattDato' >= '2020-01' and data ->> 'mottattDato' <= '2020-12'
group by AvsenderSystem, AvsenderVersjon;

select *
from inntektsmelding
where data ->> 'mottattDato' >= '2020-01' and data ->> 'mottattDato' <= '2020-12'
and data ->> 'førsteFraværsdag' IS NULL;
