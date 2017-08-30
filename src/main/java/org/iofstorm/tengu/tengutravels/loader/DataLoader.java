package org.iofstorm.tengu.tengutravels.loader;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
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
import java.io.InputStreamReader;
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

    public static LocalDateTime NOW_TS;

    @Autowired
    private Gson gson;

    @Value("${tengu.data.path}")
    private String zipFilePath;

    @Autowired
    private UserService userService;

    @Autowired
    private LocationService locationService;

    @Autowired
    private VisitService visitService;

    @Autowired
    public DataLoader(@Value("${tengu.data.path}") String zipFilePath) throws IOException, InterruptedException {
        this.zipFilePath = Objects.requireNonNull(zipFilePath);

        if (NOW_TS == null) {
            log.warn("options.txt was not found!");
            NOW_TS = LocalDateTime.ofInstant(Instant.ofEpochSecond(1503695452), ZoneId.systemDefault());
        }
    }

    @PostConstruct
    public void loadData() throws IOException, InterruptedException {
        if (!new File(zipFilePath).exists()) {
            log.warn("data file {} not found", zipFilePath);
            return;
        }

        log.info("loading users and locations...");

        long startTs = System.currentTimeMillis();

        int usersCount = 0;
        int locationsCount = 0;

        try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry = zipIn.getNextEntry();
            // iterates over entries in the zip file
            while (entry != null) {
                if (entry.getName().startsWith("users_")) {
                    JsonReader jsonReader = new JsonReader(new InputStreamReader(zipIn));
                    Users users = gson.fromJson(jsonReader, Users.class);
                    userService.load(users.getUsers());
                    usersCount += users.getUsers().size();
                } else if (entry.getName().startsWith("locations_")) {
                    JsonReader jsonReader = new JsonReader(new InputStreamReader(zipIn));
                    Locations locations = gson.fromJson(jsonReader, Locations.class);
                    locationService.load(locations.getLocations());
                    locationsCount += locations.getLocations().size();
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
        }

        log.info("{} users and {} locations were loaded in {} sec", usersCount, locationsCount, String.format("%.3f", (System.currentTimeMillis() - startTs) / 1000f) );

        System.gc();

        log.info("loading visits...");

        startTs = System.currentTimeMillis();

        int visitsCount = 0;

        try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry = zipIn.getNextEntry();
            // iterates over entries in the zip file
            while (entry != null) {
                if (entry.getName().startsWith("visits_")) {
                    JsonReader jsonReader = new JsonReader(new InputStreamReader(zipIn));
                    Visits visits = gson.fromJson(jsonReader, Visits.class);
                    visitService.load(visits);
                    visitsCount += visits.getVisits().size();
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
        }

        log.info("{} visits were loaded in {} sec", visitsCount, String.format("%.3f", (System.currentTimeMillis() - startTs) / 1000f));

        System.gc();
    }
}
