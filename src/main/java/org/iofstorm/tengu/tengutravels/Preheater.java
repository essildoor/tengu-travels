package org.iofstorm.tengu.tengutravels;

import org.apache.commons.lang3.RandomUtils;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Component
public class Preheater {
    private static final Logger log = LoggerFactory.getLogger(Preheater.class);

    private static final String host = "127.0.0.1";
    private static final String GET_USER_VISITS = "/users/%s/visits";
    private static final String GET_USER = "/users/%s";
    private static final String GET_LOCATION = "/locations/%s";
    private static final String GET_LOCATION_AVG = "/locations/%s/avg";
    private static final String GET_VISIT = "/visits/%s";

    private static final List<String> pathsSet = new ArrayList<>(Arrays.asList(GET_USER, GET_USER_VISITS, GET_LOCATION, GET_LOCATION_AVG, GET_VISIT));

    private static final int[] load = new int[]{10, 20, 30, 40, 50, 100, 200};
    private static final int concurrency = 4;

    private List<HttpGet> requests;

    private HttpClient httpClient;


    public Preheater(@Value("${port}") int port) {
        Set<Header> defaultHeaders = new HashSet<>();
        defaultHeaders.add(new BasicHeader(HttpHeaders.CONNECTION, "keep-alive"));
        defaultHeaders.add(new BasicHeader(HttpHeaders.ACCEPT, "*/*"));

        httpClient = HttpClientBuilder.create()
                .setDefaultHeaders(defaultHeaders)
                .disableAutomaticRetries()
                .build();

        requests = new ArrayList<>(10000);
        for (int i = 0; i < 10000; i++) {
            String uri = String.format("http://%s:%d%s", host, port, String.format(pathsSet.get(RandomUtils.nextInt(0, 5)), RandomUtils.nextInt(1, 10000)));
            requests.add(new HttpGet(uri));
        }
        System.out.println();
    }

    public void startPreheat() throws InterruptedException, IOException {
        long stopTs = TenguTravelsApplication.startTs + 25_000;

        log.info("start preheating...");

        for (int i = 0; i < load.length; i++) {
            shoot(i);
            Thread.sleep(1);
        }

        while (true) {

            if (System.currentTimeMillis() > stopTs) {
                log.info("stop preheating");
                break;
            }

            for (int i = 0; i < concurrency; i++) {
                CompletableFuture.runAsync(() -> {
                    try {
                        shoot(load[load.length - 1]);
                    } catch (IOException | InterruptedException e) {
                        log.error("got error on preheat: {}", e.getMessage());
                    }
                });
            }
        }
    }

    private void shoot(int num) throws IOException, InterruptedException {
        for (int i = 0; i > num; i++) {
            httpClient.execute(requests.get(RandomUtils.nextInt(0, 10000)));
            Thread.sleep(0, 100);
        }
    }
}
