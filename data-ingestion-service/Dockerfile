# Build context is the PremierLeague/ root (so pl-core-data is reachable).
# docker build -f data-ingestion-service/Dockerfile .
#
# NOTE: this service uses Playwright for scraping, which needs a real
# browser + its OS-level dependencies at runtime. Rather than fight a
# Spring Boot fat-jar classpath to run `playwright install` at the very
# end (fragile, and I have no Docker here to verify it), this image
# just keeps the full Maven/JDK build image as the runtime too, and
# installs Chromium + its deps in that same environment. Bigger image,
# much more likely to actually work first try.

FROM maven:3.9-eclipse-temurin-21
WORKDIR /workspace

COPY pl-core-data ./pl-core-data
RUN mvn -f pl-core-data/pom.xml -DskipTests install

COPY data-ingestion-service ./data-ingestion-service
RUN mvn -f data-ingestion-service/pom.xml -DskipTests package

# Install Chromium + required OS packages for Playwright.
# Uses the fully-qualified plugin coordinate so it works without
# adding exec-maven-plugin to the pom.
RUN mvn -f data-ingestion-service/pom.xml org.codehaus.mojo:exec-maven-plugin:3.1.0:java \
    -Dexec.mainClass="com.microsoft.playwright.CLI" \
    -Dexec.args="install --with-deps chromium" \
    -Dexec.classpathScope="compile" || \
    (echo "Playwright browser install failed — see README troubleshooting notes for data-ingestion-service." && exit 1)

WORKDIR /workspace/data-ingestion-service
ENTRYPOINT ["java", "-jar", "target/data-ingestion-service-0.0.1-SNAPSHOT.jar"]
