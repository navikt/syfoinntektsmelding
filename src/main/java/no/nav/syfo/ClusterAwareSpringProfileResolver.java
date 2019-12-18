package no.nav.syfo;

import static java.lang.System.getenv;

public final class ClusterAwareSpringProfileResolver {

    private static final String NAIS_CLUSTER_NAME = "NAIS_CLUSTER_NAME";
    public static final String DEFAULT = "default";
    public static final String LOCAL = "local";

    public static final String PROD_FSS = "prod-fss";
    public static final String DEV_FSS = "dev-fss";
    public static final String REMOTE = "remote";

    private ClusterAwareSpringProfileResolver() {
    }

    public static String[] profiles() {
        return clusterFra(getenv(NAIS_CLUSTER_NAME));
    }

    public static String[] clusterFra(String cluster) {
        if (cluster == null) {
            return new String[]{LOCAL};
        }
        if (cluster.equals(DEV_FSS)) {
            return new String[]{DEV_FSS, REMOTE};
        }
        if (cluster.equals(REMOTE)) {
            return new String[]{PROD_FSS, REMOTE};
        }
        return new String[]{DEFAULT};
    }
}
