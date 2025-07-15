package tech.softwareologists.ij.settings;

import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

import tech.softwareologists.ij.McpServerStatus;

/**
 * Settings panel allowing configuration of the MCP HTTP port and package filters.
 */
public class McpSettingsConfigurable implements Configurable {
    private JTextField portField;
    private JTextField filterField;
    private JLabel statusLabel;
    private JPanel panel;

    @Override
    public @Nls String getDisplayName() {
        return "CodeGraph MCP";
    }

    @Override
    public @Nullable JComponent createComponent() {
        panel = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4,4,4,4);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridx = 0;
        gc.gridy = 0;
        panel.add(new JLabel("HTTP Port:"), gc);
        gc.gridx = 1;
        portField = new JTextField(String.valueOf(McpSettings.getInstance().getPort()), 10);
        panel.add(portField, gc);

        gc.gridx = 0;
        gc.gridy = 1;
        panel.add(new JLabel("Package Filters (comma-separated):"), gc);
        gc.gridx = 1;
        filterField = new JTextField(McpSettings.getInstance().getPackageFilters(), 20);
        panel.add(filterField, gc);

        gc.gridx = 0;
        gc.gridy = 2;
        panel.add(new JLabel("Server Status:"), gc);
        gc.gridx = 1;
        statusLabel = new JLabel(McpServerStatus.getStatus());
        panel.add(statusLabel, gc);
        return panel;
    }

    @Override
    public boolean isModified() {
        McpSettings settings = McpSettings.getInstance();
        String portText = portField.getText().trim();
        String filterText = filterField.getText().trim();
        return settings.getPort() != parseInt(portText, settings.getPort()) ||
                !filterText.equals(settings.getPackageFilters());
    }

    @Override
    public void apply() {
        McpSettings settings = McpSettings.getInstance();
        settings.setPort(parseInt(portField.getText().trim(), settings.getPort()));
        settings.setPackageFilters(filterField.getText().trim());
    }

    private static int parseInt(String text, int def) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    @Override
    public void reset() {
        McpSettings settings = McpSettings.getInstance();
        portField.setText(String.valueOf(settings.getPort()));
        filterField.setText(settings.getPackageFilters());
        if (statusLabel != null) {
            statusLabel.setText(McpServerStatus.getStatus());
        }
    }

    @Override
    public void disposeUIResources() {
        panel = null;
        portField = null;
        filterField = null;
        statusLabel = null;
    }
}
