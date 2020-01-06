INSERT INTO ARBEIDSGIVERPERIODE (PERIODE_UUID, INNTEKTSMELDING_UUID, FOM, TOM)
  SELECT
    SYS_GUID(),
    INNTEKTSMELDING_UUID,
    ARBEIDSGIVERPERIODE_FOM,
    ARBEIDSGIVERPERIODE_TOM
  FROM INNTEKTSMELDING
  WHERE ARBEIDSGIVERPERIODE_FOM IS NOT NULL
        AND ARBEIDSGIVERPERIODE_TOM IS NOT NULL;