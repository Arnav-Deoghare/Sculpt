#!/usr/bin/env bash
# Sculpt setup for macOS: installs what's missing (JDK 17, Android SDK, Gradle), connects your phone,
# builds the app, installs it and launches it. Everything it downloads goes into ~/.sculpt-tools,
# so it needs no Homebrew, no sudo and no Android Studio. Safe to re-run any time.
#   Run:  bash setup.sh      (or ./setup.sh)

cd "$(dirname "$0")" || exit 1
ROOT="$(pwd)"

PKG="com.arnav.sculpt"
GRADLE_VER="8.9"
CMDLINE_TOOLS_ZIP="commandlinetools-mac-11076708_latest.zip"
TOOLS="$HOME/.sculpt-tools"
DEVFILE="$ROOT/.last-device.txt"

say()  { printf '\n%s\n' "$*"; }
step() { printf '\n[%s] %s\n' "$1" "$2"; }
fail() { printf '\nERROR: %s\n\nSetup stopped. Fix the issue above and run setup.sh again.\n' "$*"; exit 1; }

[ "$(uname -s)" = "Darwin" ] || fail "This script is for macOS. On Windows use setup.bat."
command -v curl >/dev/null 2>&1 || fail "curl not found."
mkdir -p "$TOOLS"

case "$(uname -m)" in
  arm64) JDK_ARCH="aarch64" ;;
  *)     JDK_ARCH="x64" ;;
esac

echo "==================== Sculpt setup (macOS) ===================="

# ---------------------------------------------------------------- 1. Java
step "1/6" "Java (JDK 17 or newer) ..."
JAVA_OK=""
if JH="$(/usr/libexec/java_home -v 17+ 2>/dev/null)" && [ -n "$JH" ]; then
  export JAVA_HOME="$JH"; JAVA_OK=1
elif [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
  JAVA_OK=1
fi
if [ -z "$JAVA_OK" ]; then
  LOCAL_JDK="$(find "$TOOLS/jdk17" -maxdepth 4 -type d -name Home 2>/dev/null | head -n1)"
  if [ -z "$LOCAL_JDK" ]; then
    echo "No JDK found. Downloading JDK 17 (one time, about 190 MB) ..."
    rm -rf "$TOOLS/jdk17"; mkdir -p "$TOOLS/jdk17"
    curl -fL --progress-bar -o "$TOOLS/jdk17.tar.gz" \
      "https://api.adoptium.net/v3/binary/latest/17/ga/mac/$JDK_ARCH/jdk/hotspot/normal/eclipse" \
      || fail "Could not download the JDK. Check your internet, VPN or proxy."
    tar -xzf "$TOOLS/jdk17.tar.gz" -C "$TOOLS/jdk17" || fail "Could not unpack the JDK."
    rm -f "$TOOLS/jdk17.tar.gz"
    LOCAL_JDK="$(find "$TOOLS/jdk17" -maxdepth 4 -type d -name Home | head -n1)"
  fi
  [ -n "$LOCAL_JDK" ] && [ -x "$LOCAL_JDK/bin/java" ] || fail "JDK install looks broken. Delete $TOOLS/jdk17 and re-run."
  export JAVA_HOME="$LOCAL_JDK"
fi
export PATH="$JAVA_HOME/bin:$PATH"
echo "JAVA_HOME: $JAVA_HOME"
java -version 2>&1 | head -n1

# ---------------------------------------------------------------- 2. Android SDK
step "2/6" "Android SDK ..."
SDK=""
for c in "${ANDROID_HOME:-}" "${ANDROID_SDK_ROOT:-}" "$HOME/Library/Android/sdk" "$TOOLS/android-sdk"; do
  if [ -n "$c" ] && [ -x "$c/platform-tools/adb" ]; then SDK="$c"; break; fi
done
if [ -z "$SDK" ]; then
  SDK="$TOOLS/android-sdk"
  echo "No Android SDK found. Installing a minimal one into $SDK (one time) ..."
  if [ ! -x "$SDK/cmdline-tools/latest/bin/sdkmanager" ]; then
    mkdir -p "$SDK/cmdline-tools"
    curl -fL --progress-bar -o "$TOOLS/cmdline-tools.zip" \
      "https://dl.google.com/android/repository/$CMDLINE_TOOLS_ZIP" \
      || fail "Could not download the Android command-line tools."
    rm -rf "$TOOLS/cmdline-extract"; mkdir -p "$TOOLS/cmdline-extract"
    unzip -q "$TOOLS/cmdline-tools.zip" -d "$TOOLS/cmdline-extract" || fail "Could not unpack the command-line tools."
    rm -rf "$SDK/cmdline-tools/latest"
    mv "$TOOLS/cmdline-extract/cmdline-tools" "$SDK/cmdline-tools/latest"
    rm -rf "$TOOLS/cmdline-extract" "$TOOLS/cmdline-tools.zip"
  fi
fi
export ANDROID_HOME="$SDK"
export ANDROID_SDK_ROOT="$SDK"

SDKM="$SDK/cmdline-tools/latest/bin/sdkmanager"
if [ -x "$SDKM" ]; then
  echo "Accepting licenses and making sure platform 35 and platform-tools are installed ..."
  yes 2>/dev/null | "$SDKM" --sdk_root="$SDK" --licenses >/dev/null 2>&1
  yes 2>/dev/null | "$SDKM" --sdk_root="$SDK" "platform-tools" "platforms;android-35" "build-tools;35.0.0" >/dev/null 2>&1
else
  echo "NOTE: cmdline-tools not found in this SDK, skipping the license and package check."
fi
[ -x "$SDK/platform-tools/adb" ] || fail "adb not found in $SDK/platform-tools."
export PATH="$SDK/platform-tools:$PATH"
echo "SDK: $SDK"
printf 'sdk.dir=%s\n' "$SDK" > "$ROOT/local.properties"

# ---------------------------------------------------------------- 3. Gradle
step "3/6" "Gradle ..."
# Slow or flaky connections: give every Gradle download much more time than the 10 s default.
export GRADLE_OPTS="${GRADLE_OPTS:-} -Dorg.gradle.internal.http.connectionTimeout=60000 -Dorg.gradle.internal.http.socketTimeout=180000"
GRADLE_CMD=""
BOOTGRADLE="$TOOLS/gradle-$GRADLE_VER/bin/gradle"

# Download Gradle with curl: resumes after a drop and retries, so a slow link still finishes.
fetch_gradle() {
  [ -x "$BOOTGRADLE" ] && return 0
  echo "Downloading Gradle $GRADLE_VER once (kept in $TOOLS). Resumes automatically if the connection drops ..."
  local url ok=""
  for url in \
    "https://services.gradle.org/distributions/gradle-$GRADLE_VER-bin.zip" \
    "https://downloads.gradle.org/distributions/gradle-$GRADLE_VER-bin.zip"; do
    for attempt in 1 2 3 4 5 6; do
      if curl -fL -C - --connect-timeout 30 --retry 5 --retry-delay 3 --progress-bar \
           -o "$TOOLS/gradle.zip" "$url"; then ok=1; break 2; fi
      echo "Download interrupted (attempt $attempt), retrying ..."
      sleep 3
    done
  done
  [ -n "$ok" ] || { echo "Could not download Gradle. Check your internet, VPN or proxy."; return 1; }
  unzip -q -o "$TOOLS/gradle.zip" -d "$TOOLS" || { rm -f "$TOOLS/gradle.zip"; echo "Gradle zip was corrupt; deleted. Re-run."; return 1; }
  rm -f "$TOOLS/gradle.zip"
  [ -x "$BOOTGRADLE" ]
}

if [ -f gradle/wrapper/gradle-wrapper.jar ] && [ -f gradlew ]; then
  chmod +x gradlew
  # The wrapper's own download gives up after 10 s of silence; allow 3 minutes.
  WP="gradle/wrapper/gradle-wrapper.properties"
  if [ -f "$WP" ]; then
    if grep -q '^networkTimeout=' "$WP"; then
      sed 's/^networkTimeout=.*/networkTimeout=180000/' "$WP" > "$WP.tmp" && mv "$WP.tmp" "$WP"
    else
      printf 'networkTimeout=180000\n' >> "$WP"
    fi
  fi
  GRADLE_CMD="./gradlew"
else
  fetch_gradle || fail "Could not get Gradle."
  echo "Generating Gradle wrapper ..."
  if "$BOOTGRADLE" wrapper --gradle-version "$GRADLE_VER" --no-validate-url --no-daemon && [ -f gradlew ]; then
    chmod +x gradlew
    GRADLE_CMD="./gradlew"
  else
    echo "Wrapper generation failed. Using the downloaded Gradle directly instead."
    GRADLE_CMD="$BOOTGRADLE"
  fi
fi
echo "Using: $GRADLE_CMD"

# ---------------------------------------------------------------- 4. Phone
step "4/6" "Connecting to your phone ..."
adb start-server >/dev/null 2>&1
has_device() { adb devices | awk 'NR>1 && $2=="device" {f=1} END {exit !f}'; }

connect_phone() {
  if has_device; then return 0; fi
  if [ -f "$DEVFILE" ]; then
    LAST="$(cat "$DEVFILE")"
    echo "Trying last phone at $LAST ..."
    adb connect "$LAST" >/dev/null 2>&1
    has_device && return 0
  fi
  return 1
}

if ! connect_phone; then
  while true; do
    cat <<'MSG'

No phone connected. Easiest: plug it in with USB (USB debugging on) and press Enter.
Or use wireless debugging:
  1. Phone: Developer options > Wireless debugging > turn ON.
  2. Tap "Pair device with pairing code" and note the IP:port and 6-digit code.
  3. Phone and Mac must be on the same Wi-Fi.
MSG
    printf '\nPairing IP:port (blank = skip, e.g. USB or already paired): '
    read -r PAIRADDR
    if [ -n "$PAIRADDR" ]; then
      printf 'Pairing code: '
      read -r PAIRCODE
      adb pair "$PAIRADDR" "$PAIRCODE"
      echo
      echo "Close the pairing dialog. On the main Wireless debugging screen, note the IP address and port shown there."
      printf 'Connect IP:port: '
      read -r CONNADDR
      if [ -n "$CONNADDR" ]; then
        adb connect "$CONNADDR"
        if has_device; then printf '%s\n' "$CONNADDR" > "$DEVFILE"; break; fi
      fi
    else
      sleep 1
      if has_device; then break; fi
      printf 'Could not connect with USB either. Tap "Allow USB debugging" on the phone if it asks.\n'
    fi
    cat <<'MSG'

Could not connect. Common causes:
  - The connect port changes whenever Wireless debugging is toggled, so re-read it on the phone.
  - A router or guest Wi-Fi may block device-to-device traffic. Try the phone hotspot.
  - USB: unlock the phone, allow USB debugging, and use a data cable (not charge-only).
MSG
    printf '\nPress Enter to try again (Ctrl+C to quit) '
    read -r _
  done
fi
echo "Phone connected."
adb devices

# ---------------------------------------------------------------- 5. Build
step "5/6" "Building and installing (first run downloads dependencies, be patient) ..."
if ! "$GRADLE_CMD" installDebug; then
  if [ "$GRADLE_CMD" != "$BOOTGRADLE" ]; then
    echo; echo "Build via wrapper failed. Retrying once with a separately downloaded Gradle ..."
    fetch_gradle || fail "Could not get Gradle. Check your internet, VPN or proxy and re-run."
    "$BOOTGRADLE" installDebug || fail "Build failed. Scroll up for the first error."
  else
    fail "Build failed. Scroll up for the first error."
  fi
fi

# ---------------------------------------------------------------- 6. Launch
step "6/6" "Launching Sculpt ..."
adb shell am start -n "$PKG/.MainActivity" >/dev/null
if command -v git >/dev/null 2>&1 && [ ! -d .git ]; then
  git init -q && echo "Initialised a git repository."
fi

say "Done. Sculpt is installed on your phone."
echo "Re-run setup.sh any time to rebuild and reinstall."