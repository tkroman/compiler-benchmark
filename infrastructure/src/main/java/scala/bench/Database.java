package scala.bench;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;

import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

public class Database {

    public static InfluxDB connectDb() {
        Config conf = ConfigFactory.load();
        String influxUrl = conf.getString("influx.url");
        String influxUser = conf.getString("influx.user");
        String influxPassword = getPassword(conf, influxUrl, influxUser);

        OkHttpClient.Builder client = new OkHttpClient.Builder();

        client.authenticator((route, response) -> {
            String credential = Credentials.basic(influxUser, influxPassword);
            return response.request().newBuilder()
                    .header("Authorization", credential)
                    .build();
        });
        InfluxDB influxDB = InfluxDBFactory.connect(influxUrl, influxUser, influxPassword, client);
        // influxDB.setLogLevel(InfluxDB.LogLevel.FULL);
        return influxDB;
    }

    private static String getPassword(Config conf, String influxUrl, String influxUser) {
        if (!conf.hasPath("influx.password") || conf.getIsNull("influx.password")) {
            // Lookup password in .netrc
            try {
                String host = new URI(influxUrl).getHost();
                Stream<String> netrc = Files.readAllLines(Paths.get(System.getProperty("user.home"), ".netrc")).stream();
                String netrcFilter = "machine " + host + " login " + influxUser + " ";
                return netrc.filter(s -> s.contains(netrcFilter))
                        .map(s -> s.replaceFirst(".* password ", "")).findFirst().orElse("");
            } catch (IOException | URISyntaxException e) {
                return "";
            }
        } else {
            return conf.getString("influx.password");
        }
    }
}
