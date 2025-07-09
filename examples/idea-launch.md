# Launching the IntelliJ Plugin

This document describes how to build and install the CodeGraph MCP plugin into IntelliJ IDEA.

1. **Build the plugin**

   From the project root run:
   ```bash
   gradle :intellij:buildPlugin
   ```
   The distribution ZIP will be created in `intellij/build/distributions/CodeGraphMcp-0.1.0.zip`.

2. **Install in IntelliJ**

   In the IDE go to *Settings > Plugins > ⚙️ > Install Plugin from Disk...* and select the generated ZIP file.

3. **Configure**

   After restarting IntelliJ open *Settings > Tools > CodeGraph MCP* to adjust port and other options.

4. **Run in Sandbox (optional)**

   You can start a sandbox IDE with the plugin already enabled by executing:
   ```bash
   gradle :intellij:runIde
   ```
