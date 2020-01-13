CREATE TABLE task (
    id            bigint                                               NOT NULL PRIMARY KEY,
    payload       text                                                 NOT NULL,
    status        varchar(15)  DEFAULT 'UBEHANDLET'::character varying NOT NULL,
    versjon       bigint       DEFAULT 0,
    opprettet_tid timestamp(3) DEFAULT LOCALTIMESTAMP,
    type          varchar(100)                                         NOT NULL,
    metadata      varchar(4000),
    trigger_tid   timestamp    DEFAULT LOCALTIMESTAMP,
    avvikstype    varchar(50)
);


CREATE SEQUENCE task_seq INCREMENT BY 50 START WITH 1000000 NO CYCLE;

CREATE INDEX ON task (status);

CREATE TABLE task_logg (
    id            bigint       NOT NULL PRIMARY KEY,
    task_id       bigint       NOT NULL REFERENCES task,
    type          varchar(15)  NOT NULL,
    node          varchar(100) NOT NULL,
    opprettet_tid timestamp(3) DEFAULT LOCALTIMESTAMP,
    melding       text,
    endret_av     varchar(100) DEFAULT 'VL'::character varying
);

CREATE SEQUENCE task_logg_seq INCREMENT BY 50 START WITH 1000000 NO CYCLE;

CREATE INDEX ON task_logg (task_id);
