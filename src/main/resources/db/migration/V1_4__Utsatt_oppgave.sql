create table UTSATT_OPPGAVE (
    OPPGAVE_ID serial not null primary key,
    INNTEKTSMELDING_ID VARCHAR(100) not null,
    ARKIVREFERANSE VARCHAR(100) not null,
    FNR VARCHAR(50) not null,
    AKTOR_ID VARCHAR(50) not null,
    SAK_ID VARCHAR(50) not null,
    JOURNALPOST_ID VARCHAR(100) not null,
    TIMEOUT date not null,
    TILSTAND VARCHAR(100) not null
)
