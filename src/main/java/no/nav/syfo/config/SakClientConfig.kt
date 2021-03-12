package no.nav.syfo.config;

public class SakClientConfig {

    private String url;
    private String tokenUrl;
    private String username;
    private String password;

    public SakClientConfig(String url, String tokenUrl, String username, String password) {
        this.url = url;
        this.tokenUrl = tokenUrl;
        this.username = username;
        this.password = password;
    }

    public String getUrl() {
        return url;
    }

    public String getTokenUrl() {
        return tokenUrl;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
