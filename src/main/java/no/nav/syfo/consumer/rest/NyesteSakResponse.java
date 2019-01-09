package no.nav.syfo.consumer.rest;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;


@Value
@Getter
@Builder
class NyesteSakResponse {
    String nyesteSak;
}
