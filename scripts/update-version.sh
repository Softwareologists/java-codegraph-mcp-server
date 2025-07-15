#!/bin/bash
set -euo pipefail
# Remove leading 'v' if present so tags like v1.0.3 become 1.0.3
NEW_VERSION="${1#v}"

# Update ManifestGenerator.java
sed -i "s/1\.0\.1/${NEW_VERSION}/" core/src/main/java/tech/softwareologists/core/ManifestGenerator.java

# Update manifest.json.tpl
sed -i -E "s/(\"version\": \")[0-9]+\.[0-9]+\.[0-9]+(\")/\1${NEW_VERSION}\2/" core/manifest.json.tpl

# Update IntelliJ plugin.xml
sed -i -E "s/(<version>)[0-9]+\.[0-9]+\.[0-9]+(<\/version>)/\1${NEW_VERSION}\2/" intellij/resources/META-INF/plugin.xml

# Update distribution file reference in docs
sed -i -E "s/(CodeGraphMcp-)[0-9]+\.[0-9]+\.[0-9]+(\.zip)/\1${NEW_VERSION}\2/" examples/idea-launch.md

echo "Updated version to ${NEW_VERSION}"
