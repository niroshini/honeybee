package tnefern.honeybeeframework.cloud;

import java.util.Locale;

public class CloudServer {

    private String ipAddress;
    private int port;

    public CloudServer(String ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }

    public String getUrl() {
        return String.format(Locale.ENGLISH, "http://%s:%d", ipAddress, port);
    }
}
