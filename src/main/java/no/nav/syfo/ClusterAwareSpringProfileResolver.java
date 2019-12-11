package no.nav.syfo;

import static java.lang.System.getenv;

import java.util.Optional;

public final class ClusterAwareSpringProfileResolver {

    private static final String NAIS_CLUSTER_NAME = "NAIS_CLUSTER_NAME";
    public static final String DEFAULT = "default";
    public static final String LOCAL = "local";
    public static final String INCLUSTER = "!" + LOCAL;

    public static final String PROD_FSS = "prod-fss";
    public static final String DEV_FSS = "dev-fss";

    private ClusterAwareSpringProfileResolver() {
    }

    public static String[] profiles() {
        return Optional.ofNullable(clusterFra(getenv(NAIS_CLUSTER_NAME)))
            .map(c -> new String[] { c })
            .orElse(new String[0]);
    }

    private static String clusterFra(String cluster) {
        if (cluster == null) {
            return LOCAL;
        }
        if (cluster.equals(DEV_FSS)) {
            return DEV_FSS;
        }
        return DEFAULT;
    }
}
