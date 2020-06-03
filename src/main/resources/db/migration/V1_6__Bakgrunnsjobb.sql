create table BAKGRUNNSJOBB (
    JOBB_ID VARCHAR(100) unique not null,
    TYPE VARCHAR(100) not null,
    BEHANDLET timestamp,
    OPPRETTET timestamp not null,

    STATUS VARCHAR(50) not null,
    KJOERETID timestamp not null,

    FORSOEK int not null default 0,
    MAKS_FORSOEK int not null,
    DATA jsonb
)
