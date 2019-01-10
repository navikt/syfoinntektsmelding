package no.nav.syfo.consumer.rest.aktor;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;

import java.util.HashMap;

@Value
@Builder
@Getter
public class AktorResponse extends HashMap<String, Aktor> {
}
