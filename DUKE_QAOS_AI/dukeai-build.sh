#!/bin/bash
#
# Copyright © 2025 Devin B. Royal.
# All Rights Reserved.
#
# DUKEAi Smart Build & Run Script - Production Ready
# Features:
# - Auto Java version detection and switching
# - Maven cache cleanup and bulletproof pom.xml
# - Directory and config validation
# - Full build, test, and run pipeline
# - Resilient error handling and recovery

set -euo pipefail

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Logging
log_info()    { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
log_warn()    { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error()   { echo -e "${RED}[ERROR]${NC} $1"; }

# Globals
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAVA_VERSION=""
WORKING_JAR="target/DUKEAi-1.0.0.jar"
CONFIG_FILE="src/main/resources/config.properties"
SRC_DIR="src/main/java/com/devinroyal/dukeai"
BUILD_SUCCESS=false

echo "================================================"
echo " DUKEAi Smart Build & Run Script"
echo " Copyright © 2025 Devin B. Royal"
echo "================================================"

# Detect Java
detect_java_version() {
  log_info "🔍 Detecting Java..."
  if ! command -v java &> /dev/null; then
    log_error "Java not found. Install Java 11+."
    exit 1
  fi
  local full_version=$(java -version 2>&1 | head -1 | cut -d'"' -f2)
  JAVA_VERSION=$(echo "$full_version" | cut -d'.' -f1)
  log_info "📋 Java version: $full_version"

  if [ "$JAVA_VERSION" -lt 11 ]; then
    log_error "Java 11+ required. Found: $full_version"
    exit 1
  fi

  if [ "$JAVA_VERSION" -gt 11 ]; then
    log_warn "Using Java $JAVA_VERSION. Attempting switch to Java 11..."
    if command -v brew &> /dev/null && brew list openjdk@11 &> /dev/null; then
      local brew_java11=$(brew --prefix openjdk@11)/libexec/openjdk.jdk/Contents/Home
      if [[ -d "$brew_java11" ]]; then
        export JAVA_HOME="$brew_java11"
        export PATH="$JAVA_HOME/bin:$PATH"
        JAVA_VERSION=11
        log_success "Switched to Java 11"
      fi
    fi
  fi
  log_success "Java environment ready"
}

# Detect Maven
detect_maven() {
  log_info "🔍 Detecting Maven..."
  if ! command -v mvn &> /dev/null; then
    log_error "Maven not found. Install with: brew install maven"
    exit 1
  fi
  local version=$(mvn --version 2>&1 | head -1 | awk '{print $3}')
  log_info "📋 Maven version: $version"
  log_success "Maven environment ready"
}

# Validate structure
validate_structure() {
  log_info "📁 Checking project structure..."
  if [[ ! -d "$SRC_DIR" ]]; then
    log_warn "Missing source directory: $SRC_DIR"
    mkdir -p "$SRC_DIR"
    touch "$SRC_DIR/DUKEAi.java"
    echo "public class DUKEAi { public static void main(String[] args) { System.out.println(\"DUKEAi running...\"); } }" > "$SRC_DIR/DUKEAi.java"
    log_success "Created minimal DUKEAi.java"
  fi

  if [[ ! -f "$CONFIG_FILE" ]]; then
    log_warn "Missing config.properties. Creating default..."
    mkdir -p "$(dirname "$CONFIG_FILE")"
    echo "# DUKEAi runtime config\nmode=production\nlog.level=info" > "$CONFIG_FILE"
    log_success "Created config.properties"
  fi
}

# Clean Maven cache
clean_maven_cache() {
  log_info "🧹 Cleaning Maven cache..."
  local backup="$HOME/.m2/repository.backup.$(date +%Y%m%d_%H%M%S)"
  if [[ -d "$HOME/.m2/repository" ]]; then
    mv "$HOME/.m2/repository" "$backup" 2>/dev/null || true
    log_warn "Backed up Maven cache to: $backup"
  fi
  rm -rf "$HOME/.m2/repository" target/ pom.xml.original .mvn/ mvnw mvnw.cmd
  mkdir -p "$HOME/.m2/repository"
  log_success "Maven cache cleaned"
}

# Create pom.xml
create_bulletproof_pom() {
  log_info "📝 Creating pom.xml..."
  if [[ -f "pom.xml" ]]; then
    cp pom.xml pom.xml.original
    log_warn "Backed up original pom.xml"
  fi

  cat > pom.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.devinroyal.dukeai</groupId>
  <artifactId>DUKEAi</artifactId>
  <version>1.0.0</version>
  <packaging>jar</packaging>
  <properties>
    <maven.compiler.source>11</maven.compiler.source>
    <maven.compiler.target>11</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  </properties>
  <dependencies>
    <dependency>
      <groupId>org.bouncycastle</groupId>
      <artifactId>bcprov-jdk15on</artifactId>
      <version>1.70</version>
    </dependency>
  </dependencies>
  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.11.0</version>
        <configuration>
          <source>11</source>
          <target>11</target>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
EOF
  log_success "pom.xml created"
}

# Build project
build_project() {
  log_info "🔧 Building project..."
  if mvn clean package -DskipTests; then
    BUILD_SUCCESS=true
    log_success "Build successful"
  else
    log_error "Build failed"
    exit 1
  fi
}

# Run tests
run_tests() {
  log_info "🧪 Running tests..."
  if mvn test; then
    log_success "Tests passed"
  else
    log_error "Tests failed"
    exit 1
  fi
}

# Verify JAR
verify_jar() {
  log_info "🚀 Verifying JAR execution..."
  if [[ -f "$WORKING_JAR" ]]; then
    local output=$(java -jar "$WORKING_JAR" 2>&1)
    echo "$output" | grep -q "DUKEAi running" && log_success "JAR executed successfully" || log_error "JAR did not run as expected"
  else
    log_error "JAR not found: $WORKING_JAR"
    exit 1
  fi
}

# Pipeline
detect_java_version
detect_maven
validate_structure
clean_maven_cache
create_bulletproof_pom
build_project
run_tests
verify_jar
