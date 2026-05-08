#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# ── Colors ──────────────────────────────────────────────
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BOLD='\033[1m'
RESET='\033[0m'

step()   { echo "${GREEN}▸${RESET} $1"; }
warn()   { echo "${YELLOW}⚠${RESET} $1"; }
fail()   { echo "${RED}✗${RESET} $1"; exit 1; }

# ── Pre-flight checks ──────────────────────────────────
step "Checking prerequisites..."

command -v node >/dev/null 2>&1  || fail "Node.js not found. Install from https://nodejs.org/"
command -v java >/dev/null 2>&1  || fail "Java 17+ not found. Install from https://adoptium.net/"
command -v npm  >/dev/null 2>&1  || fail "npm not found. It comes with Node.js."

java -version 2>&1 | grep -q '"17\|"18\|"19\|"20\|"21' || warn "Java 17+ recommended. Your version: $(java -version 2>&1 | head -1)"

# ── Parse arguments ────────────────────────────────────
MODE="jar"          # default: build fat JAR only
CLEAN=false

for arg in "$@"; do
  case "$arg" in
    --native)   MODE="native" ;;   # also create native app with jpackage
    --clean)    CLEAN=true ;;
    --help|-h)
      echo "Usage: ./build.sh [--native] [--clean]"
      echo ""
      echo "  (default)   Build fat JAR → backend/target/interview-assistant-0.1.0.jar"
      echo "  --native    Also create native app (.app on macOS, .deb on Linux)"
      echo "  --clean     Clean before build"
      exit 0
      ;;
    *) warn "Unknown argument: $arg" ;;
  esac
done

if [ "$CLEAN" = true ]; then
  step "Cleaning previous builds..."
  rm -rf frontend/dist backend/src/main/resources/static/assets backend/src/main/resources/static/index.html
  cd backend && ./mvnw clean -q && cd "$SCRIPT_DIR"
fi

# ── 1. Build frontend ──────────────────────────────────
step "Building frontend..."
cd frontend
npm install --silent 2>/dev/null
npm run build
cd "$SCRIPT_DIR"

# Verify frontend output was placed into backend resources
if [ ! -f "backend/src/main/resources/static/index.html" ]; then
  fail "Frontend build failed: static/index.html not found in backend resources"
fi

# ── 2. Build backend (fat JAR) ─────────────────────────
step "Building backend..."
cd backend
./mvnw package -DskipTests -q
cd "$SCRIPT_DIR"

JAR="$SCRIPT_DIR/backend/target/interview-assistant-0.1.0.jar"
if [ ! -f "$JAR" ]; then
  fail "Backend build failed: JAR not found"
fi

SIZE=$(du -h "$JAR" | cut -f1 | tr -d ' ')
echo ""
echo "${BOLD}Build complete!${RESET}"
echo ""
echo "  JAR:  backend/target/interview-assistant-0.1.0.jar  ($SIZE)"
echo "  Run:  java -jar backend/target/interview-assistant-0.1.0.jar"
echo ""

if [ "$MODE" != "native" ]; then
  exit 0
fi

# ── 3. Native package (optional, --native) ─────────────
step "Creating native package..."

if [ -z "${JAVA_HOME:-}" ]; then
  JAVA_HOME="$(dirname "$(dirname "$(readlink -f "$(which java)")")")" 2>/dev/null || true
fi

JPACKAGE="${JAVA_HOME:-}/bin/jpackage"
if [ ! -f "$JPACKAGE" ]; then
  fail "jpackage not found (requires JDK, not JRE). Expected: $JPACKAGE"
fi

OS="$(uname -s)"
APP_NAME="InterviewAssistant"
MAIN_CLASS="com.interviewassistant.InterviewAssistantApplication"
OUTPUT_DIR="$SCRIPT_DIR/dist"

rm -rf "$OUTPUT_DIR"
mkdir -p "$OUTPUT_DIR"

if [ "$OS" = "Darwin" ]; then
  "$JPACKAGE" \
    --type app-image \
    --name "$APP_NAME" \
    --input "$SCRIPT_DIR/backend/target" \
    --main-jar interview-assistant-0.1.0.jar \
    --main-class "$MAIN_CLASS" \
    --dest "$OUTPUT_DIR" \
    --app-version "0.1.0" \
    --vendor "InterviewAssistant" \
    --java-options "--add-opens java.base/java.lang=ALL-UNNAMED" \
    --icon "$SCRIPT_DIR/frontend/public/favicon.ico" 2>/dev/null || \
  "$JPACKAGE" \
    --type app-image \
    --name "$APP_NAME" \
    --input "$SCRIPT_DIR/backend/target" \
    --main-jar interview-assistant-0.1.0.jar \
    --main-class "$MAIN_CLASS" \
    --dest "$OUTPUT_DIR" \
    --app-version "0.1.0" \
    --vendor "InterviewAssistant" \
    --java-options "--add-opens java.base/java.lang=ALL-UNNAMED"

  echo ""
  echo "  App:  dist/$APP_NAME.app"
  echo ""

elif [ "$OS" = "Linux" ]; then
  "$JPACKAGE" \
    --type deb \
    --name "$APP_NAME" \
    --input "$SCRIPT_DIR/backend/target" \
    --main-jar interview-assistant-0.1.0.jar \
    --main-class "$MAIN_CLASS" \
    --dest "$OUTPUT_DIR" \
    --app-version "0.1.0" \
    --vendor "InterviewAssistant" \
    --java-options "--add-opens java.base/java.lang=ALL-UNNAMED"

  echo ""
  echo "  Package: dist/"
  echo ""

else
  fail "Unsupported OS: $OS"
fi
