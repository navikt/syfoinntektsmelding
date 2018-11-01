package no.nav.syfo.util;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.security.SecureRandom;

/**
 * Utility-klasse for kommunikasjon med MDC.
 */
@Slf4j
public final class MDCOperations {
    public static final String MDC_CALL_ID = "callId";
    public static final String MDC_USER_ID = "userId";
    public static final String MDC_CONSUMER_ID = "consumerId";

    private static final SecureRandom RANDOM = new SecureRandom();

    private MDCOperations() {
    }

    public static String generateCallId() {
        int randomNr = getRandomNumber();
        long systemTime = getSystemTime();

        return "CallId_" + systemTime + "_" + randomNr;
    }

    public static String getFromMDC(String key) {
        String value = MDC.get(key);
        log.debug("Getting key: " + key + " from MDC with value: " + value);
        return value;
    }

    public static void putToMDC(String key, String value) {
        log.debug("Putting value: " + value + " on MDC with key: " + key);
        MDC.put(key, value);
    }

    public static void remove(String key) {
        log.debug("Removing key: " + key);
        MDC.remove(key);
    }

    private static int getRandomNumber() {
        return RANDOM.nextInt(Integer.MAX_VALUE);
    }

    private static long getSystemTime() {
        return System.currentTimeMillis();
    }
}
