package org.iofstorm.tengu.tengutravels.loader;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.iofstorm.tengu.tengutravels.model.Locations;
import org.iofstorm.tengu.tengutravels.model.Users;
import org.iofstorm.tengu.tengutravels.model.Visits;
import org.iofstorm.tengu.tengutravels.service.LocationService;
import org.iofstorm.tengu.tengutravels.service.UserService;
import org.iofstorm.tengu.tengutravels.service.VisitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class DataLoader {
    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);
    private static final AtomicInteger THREAD_NUM = new AtomicInteger(1);

    private final ObjectMapper objectMapper;
    private final ExecutorService executor;

    @Value("${tengu.data.path}")
    private final String zipFilePath;

    @Autowired
    private UserService userService;

    @Autowired
    private LocationService locationService;

    @Autowired
    private VisitService visitService;

    @Autowired
    public DataLoader(@Value("${tengu.data.path}") String zipFilePath) {
        this.zipFilePath = Objects.requireNonNull(zipFilePath);
        objectMapper = new ObjectMapper();
        objectMapper.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false);
        executor = Executors.newFixedThreadPool(8, r -> new Thread(r, "loader-worker-" + THREAD_NUM.getAndDecrement()));
    }

    @PostConstruct
    public void loadData() throws IOException {
        if (!new File(zipFilePath).exists()) {
            log.warn("data file {} not found", zipFilePath);
            return;
        }

        log.debug("start loading data from {}", zipFilePath);
        long startTs = System.currentTimeMillis();

        List<CompletableFuture<Void>> usersAndLocationsFutures = new ArrayList<>();
        Map<String, Visits> visitsList = new HashMap<>();
        try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry = zipIn.getNextEntry();
            // iterates over entries in the zip file
            while (entry != null) {
                if (entry.getName().startsWith("users_")) {
                    String fileName = entry.getName();
                    Users users = objectMapper.readValue(zipIn, Users.class);
                    usersAndLocationsFutures.add(CompletableFuture.runAsync(() -> {
                        log.debug("begin loading data from {}", fileName);
                        userService.load(users.getUsers());
                        log.debug("complete loading data from {}", fileName);
                    }, executor));
                } else if (entry.getName().startsWith("locations_")) {
                    String fileName = entry.getName();
                    Locations locations = objectMapper.readValue(zipIn, Locations.class);
                    usersAndLocationsFutures.add(CompletableFuture.runAsync(() -> {
                        log.debug("begin loading data from {}", fileName);
                        locationService.load(locations.getLocations());
                        log.debug("complete loading data from {}", fileName);
                    }, executor));
                } else if (entry.getName().startsWith("visits_")) {
                    visitsList.put(entry.getName(), objectMapper.readValue(zipIn, Visits.class));
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
        }
        // wait until all of the users and locations are loaded and then start loading visits
        CompletableFuture.allOf(usersAndLocationsFutures.toArray(new CompletableFuture[usersAndLocationsFutures.size()]))
                .thenRun(() -> visitsList.forEach((fileName, visits) -> CompletableFuture.runAsync(() -> {
                    log.debug("begin loading data from {}", fileName);
                    visitService.load(visits.getVisits());
                    log.debug("complete loading data from {}", fileName);
                }, executor)))
                .thenRun(() -> log.debug("all data was loaded. eta: {}", String.format("%.2f s", (System.currentTimeMillis() - startTs) / 1000f)));

        executor.shutdownNow();
    }
}
