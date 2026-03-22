# ── Stage 1: Build the fat JAR ───────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /build

# Copy Gradle wrapper and config first (layer cache — only re-downloads deps on change)
COPY gradlew .
COPY gradle/ gradle/
COPY settings.gradle .
COPY build.gradle .
RUN chmod +x ./gradlew && ./gradlew dependencies --no-daemon 2>/dev/null || true

# Copy source and build
COPY src/ src/
RUN ./gradlew fatJar --no-daemon

# ── Stage 2: Runtime image ───────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-jammy

# Install JavaFX native libs + Xvfb virtual display
RUN apt-get update && apt-get install -y \
    unzip \
    xvfb \
    libgtk-3-0 \
    libglib2.0-0 \
    libgl1-mesa-glx \
    libgl1-mesa-dri \
    libx11-6 \
    libxtst6 \
    libxrender1 \
    libxxf86vm1 \
    libxrandr2 \
    libxi6 \
    fonts-dejavu-core \
    && rm -rf /var/lib/apt/lists/*

# Download and install JavaFX SDK
RUN curl -fL "https://download2.gluonhq.com/openjfx/21.0.2/openjfx-21.0.2_linux-x64_bin-sdk.zip" \
      -o /tmp/javafx.zip \
    && unzip -q /tmp/javafx.zip -d /opt/ \
    && mv /opt/javafx-sdk-21.0.2 /opt/javafx-sdk \
    && rm /tmp/javafx.zip

ENV FX_LIB=/opt/javafx-sdk/lib

WORKDIR /app

# Copy only the built JAR from the builder stage
COPY --from=builder /build/build/libs/BuildMat-Billing-1.0.0-all.jar app.jar

# Start Xvfb virtual display, then launch the app
CMD Xvfb :99 -screen 0 1280x800x24 & \
    export DISPLAY=:99 && \
    sleep 1 && \
    java \
      --module-path "$FX_LIB" \
      --add-modules javafx.controls,javafx.graphics,javafx.fxml,javafx.web \
      --add-opens javafx.graphics/com.sun.javafx.sg.prism=ALL-UNNAMED \
      --add-opens javafx.graphics/com.sun.javafx.scene=ALL-UNNAMED \
      --add-opens javafx.graphics/com.sun.javafx.scene.text=ALL-UNNAMED \
      --add-opens javafx.graphics/com.sun.javafx.css=ALL-UNNAMED \
      --add-opens javafx.graphics/com.sun.javafx.tk=ALL-UNNAMED \
      --add-opens javafx.graphics/com.sun.glass.ui=ALL-UNNAMED \
      --add-opens javafx.base/com.sun.javafx.reflect=ALL-UNNAMED \
      --add-opens javafx.base/com.sun.javafx.beans=ALL-UNNAMED \
      --add-opens javafx.controls/com.sun.javafx.scene.control=ALL-UNNAMED \
      --add-opens javafx.web/com.sun.javafx.webkit=ALL-UNNAMED \
      -jar app.jar
