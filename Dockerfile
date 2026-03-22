FROM eclipse-temurin:17-jdk-jammy

# Install JavaFX dependencies and virtual display (Xvfb) for headless GUI
RUN apt-get update && apt-get install -y \
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

WORKDIR /app

# Copy the fat JAR (built by GitHub Actions before deploy)
COPY build/libs/BuildMat-Billing-1.0.0-all.jar app.jar

# Download JavaFX SDK
ADD https://download2.gluonhq.com/openjfx/21.0.2/openjfx-21.0.2_linux-x64_bin-sdk.zip /tmp/javafx.zip
RUN apt-get update && apt-get install -y unzip && rm -rf /var/lib/apt/lists/* \
    && unzip -q /tmp/javafx.zip -d /opt/ \
    && mv /opt/javafx-sdk-21.0.2 /opt/javafx-sdk \
    && rm /tmp/javafx.zip

ENV FX_LIB=/opt/javafx-sdk/lib

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
