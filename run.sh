#!/bin/bash

APP_DIR="./build/libs/"
APP_JAR="${APP_DIR}BuildMat-Billing-1.0.0-all.jar"
FX_DIR="${APP_DIR}javafx-sdk"
FX_LIB="${FX_DIR}/lib"
FX_ZIP="${APP_DIR}javafx-sdk.zip"
FX_TEMP="${APP_DIR}fx_temp"

# Detect OS and set download URL
if [[ "$OSTYPE" == "darwin"* ]]; then
    ARCH=$(uname -m)
    if [[ "$ARCH" == "arm64" ]]; then
        FX_URL="https://download2.gluonhq.com/openjfx/21.0.2/openjfx-21.0.2_osx-aarch64_bin-sdk.zip"
    else
        FX_URL="https://download2.gluonhq.com/openjfx/21.0.2/openjfx-21.0.2_osx-x64_bin-sdk.zip"
    fi
else
    FX_URL="https://download2.gluonhq.com/openjfx/21.0.2/openjfx-21.0.2_linux-x64_bin-sdk.zip"
fi

echo "================================================"
echo "  BuildMat Billing"
echo "================================================"
echo ""

# ── Check Java ───────────────────────────────────
if ! java -version > /dev/null 2>&1; then
    echo "ERROR: Java 17+ not found."
    echo "Download from: https://adoptium.net"
    echo ""
    exit 1
fi

# ── Check JAR exists ─────────────────────────────
if [ ! -f "$APP_JAR" ]; then
    echo "ERROR: JAR not found at:"
    echo "$APP_JAR"
    echo ""
    echo "Run the build script first to build the app."
    exit 1
fi

# ── Download JavaFX if not present ───────────────
if [ ! -d "$FX_LIB" ]; then
    echo "JavaFX SDK not found. Downloading automatically..."
    echo "This only happens once (~70 MB)."
    echo ""

    if command -v curl > /dev/null 2>&1; then
        curl -L "$FX_URL" -o "$FX_ZIP"
    elif command -v wget > /dev/null 2>&1; then
        wget "$FX_URL" -O "$FX_ZIP"
    else
        echo "Auto-download failed: neither curl nor wget found."
        echo ""
        echo "Please download manually:"
        echo "  1. Go to: https://gluonhq.com/products/javafx/"
        echo "  2. Select: Version 21, your OS, SDK"
        echo "  3. Extract the ZIP"
        echo "  4. Rename the extracted folder to: javafx-sdk"
        echo "  5. Place it here: $APP_DIR"
        echo ""
        exit 1
    fi

    if [ ! -f "$FX_ZIP" ]; then
        echo ""
        echo "Auto-download failed. Please download manually:"
        echo "  1. Go to: https://gluonhq.com/products/javafx/"
        echo "  2. Select: Version 21, your OS, SDK"
        echo "  3. Extract the ZIP"
        echo "  4. Rename the extracted folder to: javafx-sdk"
        echo "  5. Place it here: $APP_DIR"
        echo ""
        exit 1
    fi

    echo "Extracting..."
    mkdir -p "$FX_TEMP"
    unzip -q "$FX_ZIP" -d "$FX_TEMP"

    EXTRACTED=$(find "$FX_TEMP" -maxdepth 1 -type d -name "javafx-sdk-*" | head -1)
    if [ -n "$EXTRACTED" ]; then
        mv "$EXTRACTED" "$FX_DIR"
    fi

    rm -f "$FX_ZIP"
    rm -rf "$FX_TEMP"

    if [ ! -d "$FX_LIB" ]; then
        echo "Extraction failed. Please manually place the JavaFX SDK folder"
        echo "at: $FX_DIR"
        echo ""
        exit 1
    fi

    echo "JavaFX SDK ready!"
    echo ""
fi

# ── Launch app ───────────────────────────────────
echo "Starting BuildMat Billing..."
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
  -jar "$APP_JAR"

if [ $? -ne 0 ]; then
    echo ""
    echo "App closed with an error. See output above."
    exit 1
fi
