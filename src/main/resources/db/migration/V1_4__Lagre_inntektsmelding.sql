alter table ARBEIDSGIVERPERIODE
drop constraint PERIODE_SOKNAD_ID_FK,
add constraint PERIODE_SOKNAD_ID_FK
   foreign key (INNTEKTSMELDING_UUID)
   references INNTEKTSMELDING(INNTEKTSMELDING_UUID)
   on delete cascade;

create index idx_arbeidsgiverperiode_im_uuid
on ARBEIDSGIVERPERIODE(INNTEKTSMELDING_UUID);
