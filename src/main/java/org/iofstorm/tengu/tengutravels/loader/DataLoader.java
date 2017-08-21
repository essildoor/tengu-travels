package org.iofstorm.tengu.tengutravels.loader;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
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
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
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

    private static final AtomicInteger userCount = new AtomicInteger(0);
    private static final AtomicInteger locationCount = new AtomicInteger(0);
    private static final AtomicInteger visitCount = new AtomicInteger(0);

    public static LocalDateTime NOW_TS;

    private final ObjectMapper objectMapper;

    @Value("${tengu.data.path}")
    private final String zipFilePath;

    @Autowired
    private UserService userService;

    @Autowired
    private LocationService locationService;

    @Autowired
    private VisitService visitService;

    private ExecutorService executor;


    @Autowired
    public DataLoader(@Value("${tengu.data.path}") String zipFilePath) throws IOException, InterruptedException {
        this.zipFilePath = Objects.requireNonNull(zipFilePath);
        objectMapper = new ObjectMapper();
        objectMapper.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false);

        executor = Executors.newFixedThreadPool(4);

        if (!new File(zipFilePath).exists()) {
            log.warn("data file {} not found", zipFilePath);
        } else {
            try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath))) {
                ZipEntry entry = zipIn.getNextEntry();
                while (entry != null) {
                    if (entry.getName().contains("txt")) log.info("found: {}", entry.getName());
                    if (entry.getName().contains("options")) {
                        Long ts = Long.valueOf(IOUtils.readLines(zipIn, Charset.defaultCharset()).get(0));

                        log.info("read timestamp: {}", ts);

                        NOW_TS = LocalDateTime.ofInstant(Instant.ofEpochSecond(ts), ZoneId.systemDefault());

                        log.info("set NOW_TS to {}", NOW_TS);

                        break;
                    }
                    zipIn.closeEntry();
                    entry = zipIn.getNextEntry();
                }
            }
        }

        if (NOW_TS == null) NOW_TS = LocalDateTime.ofInstant(Instant.ofEpochSecond(1502881955L), ZoneId.systemDefault());
    }

    @PostConstruct
    public void loadData() throws IOException {
        if (!new File(zipFilePath).exists()) {
            log.warn("data file {} not found", zipFilePath);
            return;
        }

        //log.info("start loading data from {}", zipFilePath);

        long startTs = System.currentTimeMillis();

        List<CompletableFuture<Void>> usersAndLocationsFutures = new ArrayList<>();
        Map<String, Visits> visitsList = new HashMap<>();

        try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry = zipIn.getNextEntry();
            // iterates over entries in the zip file
            while (entry != null) {
                if (entry.getName().startsWith("users_")) {
                    Users users = objectMapper.readValue(zipIn, Users.class);
                    usersAndLocationsFutures.add(CompletableFuture.supplyAsync(() -> userService.load(users.getUsers()), executor)
                            .thenAccept(userCount::addAndGet));
                } else if (entry.getName().startsWith("locations_")) {
                    Locations locations = objectMapper.readValue(zipIn, Locations.class);
                    usersAndLocationsFutures.add(CompletableFuture.supplyAsync(() -> locationService.load(locations.getLocations()), executor)
                            .thenAccept(locationCount::addAndGet));
                } else if (entry.getName().startsWith("visits_")) {
                    visitsList.put(entry.getName(), objectMapper.readValue(zipIn, Visits.class));
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
        }
        // wait until all of the users and locations are loaded and then start loading visits
        List<CompletableFuture<Void>> visitsFutures = new ArrayList<>(visitsList.size());
        CompletableFuture.allOf(usersAndLocationsFutures.toArray(new CompletableFuture[usersAndLocationsFutures.size()]))
                .thenRun(() -> visitsList.forEach((fileName, visits) ->
                        visitsFutures.add(CompletableFuture.supplyAsync(() -> visitService.load(visits.getVisits()), executor)
                                        .thenAccept(count -> visitCount.addAndGet(count)))));

        CompletableFuture.allOf(visitsFutures.toArray(new CompletableFuture[visitsFutures.size()]))
                .thenRun(() -> {
                    log.info("{} users were loaded", userCount.get());
                    log.info("{} locations were loaded", locationCount.get());
                    log.info("{} visits were loaded", visitCount.get());
                    log.info("data was loaded in {} sec", String.format("%.3f", (System.currentTimeMillis() - startTs) / 1000f));
                });
    }
}
