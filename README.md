# tengu-travels

Java 8 + Spring Boot 2.0.0.M3 (Undertow embedded) for highloadcup

build executable jar:

./gradlew clean bootJar

to run server:

java -jar <path_to_jar> --data-path=<path_to_data.zip> -Xmx3700mg -Xms3700mg -server -XX:+UseConcMarkSweepGC
