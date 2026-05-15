package com.poko.moon;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import lombok.Getter;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.components.ColorJButton;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;
import net.runelite.client.ui.components.colorpicker.RuneliteColorPicker;

public class MoonPluginPanel extends PluginPanel
{
    private final ConfigManager configManager;
    private static final String CONFIG_GROUP = "moonoverlay";

    // --- PERSISTENT GETTERS USED BY OVERLAY CANVAS LOOPS ---
    @Getter
    private boolean hideUnderground = true;
    @Getter
    private int modelId = 10589;
    @Getter
    private int moonAltitude = -1700;
    @Getter
    private int skyOffsetX = 0;
    @Getter
    private int skyOffsetY = 0;
    @Getter
    private boolean enableOrbit = false;
    @Getter
    private int orbitSpeed = 5;
    @Getter
    private double moonScale = 5.0;
    @Getter
    private int moonRotation = 0;
    @Getter
    private Color moonColor = new Color(240, 240, 220);
    @Getter
    private boolean enableHaze = true;
    @Getter
    private int hazeSize = 95;
    @Getter
    private boolean enablePhaseAutomation = true;
    @Getter
    private int manualPhaseValue = 0;
    @Getter
    private int phaseAutomationSpeed = 5;
    @Getter
    private boolean enableHazeFade = false;
    @Getter
    private boolean invertGazeDirection = false;

    // UI Interactive Component Hooks for Live Updates
    private final JComboBox<String> presetDropdown;
    private final JTextField modelField;
    private final ColorJButton colorPickerBtn;
    private final JCheckBox undergroundBox;
    private final JCheckBox orbitBox;
    private final JCheckBox hazeBox;
    private final JCheckBox phaseAutomationBox;
    private final JCheckBox hazeFadeBox;
    private final JCheckBox invertGazeBox;

    // Dual UI Structural Slider & Input Controls
    private final JSlider scaleSlider;
    private final JTextField scaleInput;
    private final JSlider altitudeSlider;
    private final JTextField altitudeInput;
    private final JSlider offsetSliderX;
    private final JTextField offsetInputX;
    private final JSlider offsetSliderY;
    private final JTextField offsetInputY;
    private final JSlider rotationSlider;
    private final JTextField rotationInput;
    private final JSlider speedSlider;
    private final JTextField speedInput;
    private final JSlider hazeSizeSlider;
    private final JTextField hazeSizeInput;
    private final JSlider phaseSlider;
    private final JTextField phaseInput;
    private final JSlider phaseSpeedSlider;
    private final JTextField phaseSpeedInput;

    // Intercept flag to prevent infinite event feedback execution loops
    private boolean isUpdatingControls = false;

    public MoonPluginPanel(ColorPickerManager colorPickerManager, ConfigManager configManager)
    {
        super();
        this.configManager = configManager;

        setBorder(new EmptyBorder(10, 10, 10, 10));
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(0, 0, 8, 0);

        // --- TITLE ---
        JLabel titleLabel = new JLabel("Moon Plugin Settings");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(titleLabel.getFont().deriveFont(16.0f));
        add(titleLabel, c);
        c.gridy++;

        // --- PROFILE CONTROL SECTION ---
        add(new JLabel("Active Profile Preset:"), c);
        c.gridy++;
        presetDropdown = new JComboBox<>(new String[]{"Default", "New Profile..."});
        presetDropdown.addActionListener(e -> handleDropdownSelection());
        add(presetDropdown, c);
        c.gridy++;

        // Action Buttons Grid (Beneath Dropdown Menu)
        JPanel buttonPanel = new JPanel(new GridBagLayout());
        buttonPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        GridBagConstraints bc = new GridBagConstraints();
        bc.fill = GridBagConstraints.HORIZONTAL;
        bc.weightx = 0.5;
        bc.gridx = 0;
        bc.gridy = 0;

        JButton saveBtn = new JButton("Save");
        saveBtn.addActionListener(e -> saveCurrentSettingsToProfile());
        buttonPanel.add(saveBtn, bc);

        bc.gridx = 1;
        bc.insets = new Insets(0, 4, 0, 0);
        JButton deleteBtn = new JButton("Delete");
        deleteBtn.addActionListener(e -> deleteSelectedProfile());
        buttonPanel.add(deleteBtn, bc);

        add(buttonPanel, c);
        c.gridy++;

        // Divider
        add(new javax.swing.JSeparator(), c);
        c.gridy++;

        // --- MODEL ID FIELD ---
        add(new JLabel("Model ID:"), c);
        c.gridy++;
        modelField = new JTextField(String.valueOf(modelId));
        modelField.addActionListener(e -> {
            try {
                this.modelId = Integer.parseInt(modelField.getText());
                autoSaveSessionValue("modelId", this.modelId);
            }
            catch (NumberFormatException ex) { modelField.setText(String.valueOf(modelId)); }
        });
        add(modelField, c);
        c.gridy++;

        // --- COLOR SELECTOR ---
        add(new JLabel("Moon Color Rendering:"), c);
        c.gridy++;
        String initialHexText = "#" + Integer.toHexString(this.moonColor.getRGB()).substring(2).toUpperCase();
        colorPickerBtn = new ColorJButton(initialHexText, this.moonColor);
        colorPickerBtn.addActionListener(e -> {
            RuneliteColorPicker picker = colorPickerManager.create(
                    SwingUtilities.getWindowAncestor(this), this.moonColor, "Choose Moon Color", true
            );
            picker.setLocation(colorPickerBtn.getLocationOnScreen());
            picker.setOnColorChange(newColor -> {
                this.moonColor = newColor;
                String newHexText = "#" + Integer.toHexString(newColor.getRGB()).substring(2).toUpperCase();
                colorPickerBtn.setText(newHexText);
                colorPickerBtn.setColor(newColor);
                autoSaveSessionValue("color", newColor.getRGB());
            });
            picker.setVisible(true);
        });
        add(colorPickerBtn, c);
        c.gridy++;

        // --- HIDE UNDERGROUND CHECKBOX ---
        undergroundBox = new JCheckBox("Hide Underground", hideUnderground);
        undergroundBox.setBackground(ColorScheme.DARK_GRAY_COLOR);
        undergroundBox.setForeground(Color.WHITE);
        undergroundBox.addActionListener(e -> {
            this.hideUnderground = undergroundBox.isSelected();
            autoSaveSessionValue("underground", this.hideUnderground);
        });
        add(undergroundBox, c);
        c.gridy++;

        // --- SYNCHRONIZED DUAL SLIDER & INPUT CONTROLS ---
        scaleSlider = new JSlider(1, 20, (int) moonScale);
        scaleInput = new JTextField(String.valueOf((int) moonScale));
        setupDualControls(c, "Moon Scale", scaleSlider, scaleInput, 1, 20, "scale", val -> this.moonScale = val);

        altitudeSlider = new JSlider(-10000, 0, moonAltitude);
        altitudeInput = new JTextField(String.valueOf(moonAltitude));
        setupDualControls(c, "Moon Altitude", altitudeSlider, altitudeInput, -10000, 0, "altitude", val -> this.moonAltitude = val);

        offsetSliderX = new JSlider(-10000, 10000, skyOffsetX);
        offsetInputX = new JTextField(String.valueOf(skyOffsetX));
        setupDualControls(c, "Sky Offset X", offsetSliderX, offsetInputX, -10000, 10000, "offsetX", val -> this.skyOffsetX = val);

        offsetSliderY = new JSlider(-10000, 10000, skyOffsetY);
        offsetInputY = new JTextField(String.valueOf(skyOffsetY));
        setupDualControls(c, "Sky Offset Y", offsetSliderY, offsetInputY, -10000, 10000, "offsetY", val -> this.skyOffsetY = val);

        rotationSlider = new JSlider(0, 360, moonRotation);
        rotationInput = new JTextField(String.valueOf(moonRotation));
        setupDualControls(c, "Moon Rotation", rotationSlider, rotationInput, 0, 360, "rotation", val -> this.moonRotation = val);

        // --- ENABLE ORBIT CHECKBOX ---
        orbitBox = new JCheckBox("Enable Automated Orbit", enableOrbit);
        orbitBox.setBackground(ColorScheme.DARK_GRAY_COLOR);
        orbitBox.setForeground(Color.WHITE);
        orbitBox.addActionListener(e -> {
            this.enableOrbit = orbitBox.isSelected();
            autoSaveSessionValue("orbit", this.enableOrbit);
        });
        add(orbitBox, c);
        c.gridy++;

        speedSlider = new JSlider(0, 20, orbitSpeed);
        speedInput = new JTextField(String.valueOf(orbitSpeed));
        setupDualControls(c, "Orbit Speed", speedSlider, speedInput, 0, 20, "orbitSpeed", val -> this.orbitSpeed = val);

        // --- ENABLE HAZE CHECKBOX ---
        hazeBox = new JCheckBox("Enable Atmospheric Glow", enableHaze);
        hazeBox.setBackground(ColorScheme.DARK_GRAY_COLOR);
        hazeBox.setForeground(Color.WHITE);
        hazeBox.addActionListener(e -> {
            this.enableHaze = hazeBox.isSelected();
            autoSaveSessionValue("haze", this.enableHaze);
        });
        add(hazeBox, c);
        c.gridy++;

        hazeSizeSlider = new JSlider(10, 200, hazeSize);
        hazeSizeInput = new JTextField(String.valueOf(hazeSize));
        setupDualControls(c, "Glow Haze Size", hazeSizeSlider, hazeSizeInput, 1, 200, "hazeSize", val -> this.hazeSize = val);

        // --- HAZE FADE CHECKBOX ---
        hazeFadeBox = new JCheckBox("Fade Haze with Phase", enableHazeFade);
        hazeFadeBox.setBackground(ColorScheme.DARK_GRAY_COLOR);
        hazeFadeBox.setForeground(Color.WHITE);
        hazeFadeBox.addActionListener(e -> {
            this.enableHazeFade = hazeFadeBox.isSelected();
            autoSaveSessionValue("hazeFade", this.enableHazeFade);
        });
        add(hazeFadeBox, c);
        c.gridy++;

        // --- INVERT GAZE DIRECTION CHECKBOX ---
        invertGazeBox = new JCheckBox("Invert Phase Direction", invertGazeDirection);
        invertGazeBox.setBackground(ColorScheme.DARK_GRAY_COLOR);
        invertGazeBox.setForeground(Color.WHITE);
        invertGazeBox.addActionListener(e -> {
            this.invertGazeDirection = invertGazeBox.isSelected();
            autoSaveSessionValue("invertGaze", this.invertGazeDirection);
        });
        add(invertGazeBox, c);
        c.gridy++;

        // --- PHASE AUTOMATION CONTROLS ---
        phaseSlider = new JSlider(0, 360, manualPhaseValue);
        phaseInput = new JTextField(String.valueOf(manualPhaseValue));
        phaseSlider.setEnabled(!enablePhaseAutomation);
        phaseInput.setEnabled(!enablePhaseAutomation);
        setupDualControls(c, "Manual Moon Phase (°)", phaseSlider, phaseInput, 0, 360, "manualPhase", val -> this.manualPhaseValue = val);

        phaseSpeedSlider = new JSlider(1, 50, phaseAutomationSpeed);
        phaseSpeedInput = new JTextField(String.valueOf(phaseAutomationSpeed));
        phaseSpeedSlider.setEnabled(enablePhaseAutomation);
        phaseSpeedInput.setEnabled(enablePhaseAutomation);
        setupDualControls(c, "Phase Automation Speed", phaseSpeedSlider, phaseSpeedInput, 1, 50, "phaseSpeed", val -> this.phaseAutomationSpeed = val);

        phaseAutomationBox = new JCheckBox("Automate Moon Phases", enablePhaseAutomation);
        phaseAutomationBox.setBackground(ColorScheme.DARK_GRAY_COLOR);
        phaseAutomationBox.setForeground(Color.WHITE);
        phaseAutomationBox.addActionListener(e -> {
            this.enablePhaseAutomation = phaseAutomationBox.isSelected();
            autoSaveSessionValue("phaseAuto", this.enablePhaseAutomation);

            phaseSlider.setEnabled(!this.enablePhaseAutomation);
            phaseInput.setEnabled(!this.enablePhaseAutomation);
            phaseSpeedSlider.setEnabled(this.enablePhaseAutomation);
            phaseSpeedInput.setEnabled(this.enablePhaseAutomation);
        });
        add(phaseAutomationBox, c);
        c.gridy++;

        c.weighty = 1.0;
        add(new JPanel(), c);
    }

    private void setupDualControls(GridBagConstraints masterConstraints, String labelText,
                                   JSlider slider, JTextField inputField, int min, int max, String saveSuffix, java.util.function.Consumer<Integer> valueUpdater)
    {
        add(new JLabel(labelText + ":"), masterConstraints);
        masterConstraints.gridy++;

        JPanel rowPanel = new JPanel(new GridBagLayout());
        rowPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        GridBagConstraints rc = new GridBagConstraints();

        rc.fill = GridBagConstraints.HORIZONTAL;
        rc.weightx = 0.75;
        rc.gridx = 0;
        rc.gridy = 0;
        slider.setBackground(ColorScheme.DARK_GRAY_COLOR);
        rowPanel.add(slider, rc);

        rc.weightx = 0.25;
        rc.gridx = 1;
        rc.insets = new Insets(0, 6, 0, 0);
        inputField.setPreferredSize(new Dimension(50, 20));
        rowPanel.add(inputField, rc);

        add(rowPanel, masterConstraints);
        masterConstraints.gridy++;

        slider.addChangeListener(e -> {
            if (isUpdatingControls) return;
            isUpdatingControls = true;
            int currentVal = slider.getValue();
            inputField.setText(String.valueOf(currentVal));
            valueUpdater.accept(currentVal);
            autoSaveSessionValue(saveSuffix, currentVal);
            isUpdatingControls = false;
        });

        inputField.getDocument().addDocumentListener(new DocumentListener() {
            private void update() {
                if (isUpdatingControls) return;
                SwingUtilities.invokeLater(() -> {
                    try {
                        int parsedVal = Integer.parseInt(inputField.getText().trim());
                        if (parsedVal >= min && parsedVal <= max) {
                            isUpdatingControls = true;
                            slider.setValue(parsedVal);
                            valueUpdater.accept(parsedVal);
                            autoSaveSessionValue(saveSuffix, parsedVal);
                            isUpdatingControls = false;
                        }
                    } catch (NumberFormatException ignored) {}
                });
            }
            @Override public void insertUpdate(DocumentEvent e) { update(); }
            @Override public void removeUpdate(DocumentEvent e) { update(); }
            @Override public void changedUpdate(DocumentEvent e) { update(); }
        });
    }

    private void autoSaveSessionValue(String keySuffix, Object value)
    {
        if (isUpdatingControls) return;

        String currentProfile = (String) presetDropdown.getSelectedItem();
        if (currentProfile == null || "New Profile...".equals(currentProfile)) return;

        if ("Default".equals(currentProfile))
        {
            if (value instanceof Integer) configManager.setConfiguration(CONFIG_GROUP, "session_" + keySuffix, (int) value);
            else if (value instanceof Boolean) configManager.setConfiguration(CONFIG_GROUP, "session_" + keySuffix, (boolean) value);
        }
        else
        {
            if (value instanceof Integer) configManager.setConfiguration(CONFIG_GROUP, currentProfile + "_" + keySuffix, (int) value);
            else if (value instanceof Boolean) configManager.setConfiguration(CONFIG_GROUP, currentProfile + "_" + keySuffix, (boolean) value);
        }
    }

    private void handleDropdownSelection()
    {
        if (isUpdatingControls) return;

        String selection = (String) presetDropdown.getSelectedItem();
        if (selection == null) return;

        if ("New Profile...".equals(selection))
        {
            String name = JOptionPane.showInputDialog(
                    SwingUtilities.getWindowAncestor(this),
                    "Enter profile name:",
                    "New Profile",
                    JOptionPane.PLAIN_MESSAGE
            );

            if (name != null)
            {
                name = name.replaceAll("[^a-zA-Z0-9 ]", "").trim();
            }

            if (name == null || name.isEmpty() || "Default".equalsIgnoreCase(name) || "New Profile...".equalsIgnoreCase(name))
            {
                isUpdatingControls = true;
                String previous = configManager.getConfiguration(CONFIG_GROUP, "activePresetName");
                presetDropdown.setSelectedItem(previous != null ? previous : "Default");
                isUpdatingControls = false;
                return;
            }

            createNewProfileRecord(name);
        }
        else
        {
            loadSelectedPresetData();
        }
    }

    private void createNewProfileRecord(String name)
    {
        String currentList = configManager.getConfiguration(CONFIG_GROUP, "moonPresets");
        if (currentList == null || currentList.isEmpty()) currentList = "Default";

        List<String> list = new ArrayList<>(Arrays.asList(currentList.split(",")));
        if (!list.contains(name))
        {
            list.add(name);
            configManager.setConfiguration(CONFIG_GROUP, "moonPresets", String.join(",", list));
        }

        configManager.setConfiguration(CONFIG_GROUP, "activePresetName", name);
        configManager.setConfiguration(CONFIG_GROUP, name + "_modelId", modelId);
        configManager.setConfiguration(CONFIG_GROUP, name + "_altitude", moonAltitude);
        configManager.setConfiguration(CONFIG_GROUP, name + "_scale", (int) moonScale);
        configManager.setConfiguration(CONFIG_GROUP, name + "_color", moonColor.getRGB());
        configManager.setConfiguration(CONFIG_GROUP, name + "_offsetX", skyOffsetX);
        configManager.setConfiguration(CONFIG_GROUP, name + "_offsetY", skyOffsetY);
        configManager.setConfiguration(CONFIG_GROUP, name + "_rotation", moonRotation);
        configManager.setConfiguration(CONFIG_GROUP, name + "_orbit", enableOrbit);
        configManager.setConfiguration(CONFIG_GROUP, name + "_orbitSpeed", orbitSpeed);
        configManager.setConfiguration(CONFIG_GROUP, name + "_haze", enableHaze);
        configManager.setConfiguration(CONFIG_GROUP, name + "_hazeSize", hazeSize);
        configManager.setConfiguration(CONFIG_GROUP, name + "_underground", hideUnderground);
        configManager.setConfiguration(CONFIG_GROUP, name + "_phaseAuto", enablePhaseAutomation);
        configManager.setConfiguration(CONFIG_GROUP, name + "_manualPhase", manualPhaseValue);
        configManager.setConfiguration(CONFIG_GROUP, name + "_phaseSpeed", phaseAutomationSpeed);
        configManager.setConfiguration(CONFIG_GROUP, name + "_hazeFade", enableHazeFade);
        configManager.setConfiguration(CONFIG_GROUP, name + "_invertGaze", invertGazeDirection);

        loadSavedPresetsList();

        isUpdatingControls = true;
        presetDropdown.setSelectedItem(name);
        isUpdatingControls = false;
    }

    public void loadSavedPresetsList()
    {
        isUpdatingControls = true;

        String rawPresetsString = configManager.getConfiguration(CONFIG_GROUP, "moonPresets");
        presetDropdown.removeAllItems();
        presetDropdown.addItem("Default");

        if (rawPresetsString != null && !rawPresetsString.trim().isEmpty())
        {
            String[] list = rawPresetsString.split(",");
            for (String profile : list)
            {
                if (!profile.equals("Default") && !profile.equals("New Profile...")) presetDropdown.addItem(profile);
            }
        }

        presetDropdown.addItem("New Profile...");

        String active = configManager.getConfiguration(CONFIG_GROUP, "activePresetName");
        if (active != null && !active.equals("New Profile...")) presetDropdown.setSelectedItem(active);

        isUpdatingControls = false;
    }

    public void initializeActiveProfileSession()
    {
        loadSavedPresetsList();

        String lastActiveProfile = configManager.getConfiguration(CONFIG_GROUP, "activePresetName");
        if (lastActiveProfile == null || lastActiveProfile.trim().isEmpty() || "New Profile...".equals(lastActiveProfile))
        {
            lastActiveProfile = "Default";
        }

        isUpdatingControls = true;
        presetDropdown.setSelectedItem(lastActiveProfile);
        isUpdatingControls = false;

        loadSelectedPresetData();
    }

    private void saveCurrentSettingsToProfile()
    {
        String currentProfile = (String) presetDropdown.getSelectedItem();
        if (currentProfile == null || "Default".equals(currentProfile) || "New Profile...".equals(currentProfile)) return;

        configManager.setConfiguration(CONFIG_GROUP, currentProfile + "_modelId", modelId);
        configManager.setConfiguration(CONFIG_GROUP, currentProfile + "_altitude", moonAltitude);
        configManager.setConfiguration(CONFIG_GROUP, currentProfile + "_scale", (int) moonScale);
        configManager.setConfiguration(CONFIG_GROUP, currentProfile + "_color", moonColor.getRGB());
        configManager.setConfiguration(CONFIG_GROUP, currentProfile + "_offsetX", skyOffsetX);
        configManager.setConfiguration(CONFIG_GROUP, currentProfile + "_offsetY", skyOffsetY);
        configManager.setConfiguration(CONFIG_GROUP, currentProfile + "_rotation", moonRotation);
        configManager.setConfiguration(CONFIG_GROUP, currentProfile + "_orbit", enableOrbit);
        configManager.setConfiguration(CONFIG_GROUP, currentProfile + "_orbitSpeed", orbitSpeed);
        configManager.setConfiguration(CONFIG_GROUP, currentProfile + "_haze", enableHaze);
        configManager.setConfiguration(CONFIG_GROUP, currentProfile + "_hazeSize", hazeSize);
        configManager.setConfiguration(CONFIG_GROUP, currentProfile + "_underground", hideUnderground);
        configManager.setConfiguration(CONFIG_GROUP, currentProfile + "_phaseAuto", enablePhaseAutomation);
        configManager.setConfiguration(CONFIG_GROUP, currentProfile + "_manualPhase", manualPhaseValue);
        configManager.setConfiguration(CONFIG_GROUP, currentProfile + "_phaseSpeed", phaseAutomationSpeed);
        configManager.setConfiguration(CONFIG_GROUP, currentProfile + "_hazeFade", enableHazeFade);
        configManager.setConfiguration(CONFIG_GROUP, currentProfile + "_invertGaze", invertGazeDirection);
    }

    private void loadSelectedPresetData()
    {
        String targetProfile = (String) presetDropdown.getSelectedItem();
        if (targetProfile == null || "New Profile...".equals(targetProfile)) return;

        configManager.setConfiguration(CONFIG_GROUP, "activePresetName", targetProfile);

        if (targetProfile.equals("Default"))
        {
            Integer mid = configManager.getConfiguration(CONFIG_GROUP, "session_modelId", Integer.class);
            modelId = (mid != null) ? mid : 10589;
            Integer alt = configManager.getConfiguration(CONFIG_GROUP, "session_altitude", Integer.class);
            moonAltitude = (alt != null) ? alt : -1700;
            Integer scl = configManager.getConfiguration(CONFIG_GROUP, "session_scale", Integer.class);
            moonScale = (scl != null) ? scl : 5.0;
            Integer clr = configManager.getConfiguration(CONFIG_GROUP, "session_color", Integer.class);
            moonColor = (clr != null) ? new Color(clr, true) : new Color(240, 240, 220);
            Integer ox = configManager.getConfiguration(CONFIG_GROUP, "session_offsetX", Integer.class);
            skyOffsetX = (ox != null) ? ox : 0;
            Integer oy = configManager.getConfiguration(CONFIG_GROUP, "session_offsetY", Integer.class);
            skyOffsetY = (oy != null) ? oy : 0;
            Integer rot = configManager.getConfiguration(CONFIG_GROUP, "session_rotation", Integer.class);
            moonRotation = (rot != null) ? rot : 0;
            Boolean orb = configManager.getConfiguration(CONFIG_GROUP, "session_orbit", Boolean.class);
            enableOrbit = (orb != null) ? orb : false;
            Integer spd = configManager.getConfiguration(CONFIG_GROUP, "session_orbitSpeed", Integer.class);
            orbitSpeed = (spd != null) ? spd : 5;
            Boolean hz = configManager.getConfiguration(CONFIG_GROUP, "session_haze", Boolean.class);
            enableHaze = (hz != null) ? hz : true;
            Integer hzs = configManager.getConfiguration(CONFIG_GROUP, "session_hazeSize", Integer.class);
            hazeSize = (hzs != null) ? hzs : 95;
            Boolean und = configManager.getConfiguration(CONFIG_GROUP, "session_underground", Boolean.class);
            hideUnderground = (und != null) ? und : true;
            Boolean pa = configManager.getConfiguration(CONFIG_GROUP, "session_phaseAuto", Boolean.class);
            enablePhaseAutomation = (pa != null) ? pa : true;
            Integer mp = configManager.getConfiguration(CONFIG_GROUP, "session_manualPhase", Integer.class);
            manualPhaseValue = (mp != null) ? mp : 0;
            Integer ps = configManager.getConfiguration(CONFIG_GROUP, "session_phaseSpeed", Integer.class);
            phaseAutomationSpeed = (ps != null) ? ps : 5;
            Boolean hf = configManager.getConfiguration(CONFIG_GROUP, "session_hazeFade", Boolean.class);
            enableHazeFade = (hf != null) ? hf : false;
            Boolean ig = configManager.getConfiguration(CONFIG_GROUP, "session_invertGaze", Boolean.class);
            invertGazeDirection = (ig != null) ? ig : false;
        }
        else
        {
            Integer mid = configManager.getConfiguration(CONFIG_GROUP, targetProfile + "_modelId", Integer.class);
            if (mid != null) modelId = mid;
            Integer alt = configManager.getConfiguration(CONFIG_GROUP, targetProfile + "_altitude", Integer.class);
            if (alt != null) moonAltitude = alt;
            Integer scl = configManager.getConfiguration(CONFIG_GROUP, targetProfile + "_scale", Integer.class);
            if (scl != null) moonScale = scl;
            Integer clr = configManager.getConfiguration(CONFIG_GROUP, targetProfile + "_color", Integer.class);
            if (clr != null) moonColor = new Color(clr, true);
            Integer ox = configManager.getConfiguration(CONFIG_GROUP, targetProfile + "_offsetX", Integer.class);
            if (ox != null) skyOffsetX = ox;
            Integer oy = configManager.getConfiguration(CONFIG_GROUP, targetProfile + "_offsetY", Integer.class);
            if (oy != null) skyOffsetY = oy;
            Integer rot = configManager.getConfiguration(CONFIG_GROUP, targetProfile + "_rotation", Integer.class);
            if (rot != null) moonRotation = rot;
            Boolean orb = configManager.getConfiguration(CONFIG_GROUP, targetProfile + "_orbit", Boolean.class);
            if (orb != null) enableOrbit = orb;
            Integer spd = configManager.getConfiguration(CONFIG_GROUP, targetProfile + "_orbitSpeed", Integer.class);
            if (spd != null) orbitSpeed = spd;
            Boolean hz = configManager.getConfiguration(CONFIG_GROUP, targetProfile + "_haze", Boolean.class);
            if (hz != null) enableHaze = hz;
            Integer hzs = configManager.getConfiguration(CONFIG_GROUP, targetProfile + "_hazeSize", Integer.class);
            if (hzs != null) hazeSize = hzs;
            Boolean und = configManager.getConfiguration(CONFIG_GROUP, targetProfile + "_underground", Boolean.class);
            if (und != null) hideUnderground = und;
            Boolean pa = configManager.getConfiguration(CONFIG_GROUP, targetProfile + "_phaseAuto", Boolean.class);
            if (pa != null) enablePhaseAutomation = pa;
            Integer mp = configManager.getConfiguration(CONFIG_GROUP, targetProfile + "_manualPhase", Integer.class);
            if (mp != null) manualPhaseValue = mp;
            Integer ps = configManager.getConfiguration(CONFIG_GROUP, targetProfile + "_phaseSpeed", Integer.class);
            if (ps != null) phaseAutomationSpeed = ps;
            Boolean hf = configManager.getConfiguration(CONFIG_GROUP, targetProfile + "_hazeFade", Boolean.class);
            if (hf != null) enableHazeFade = hf;
            Boolean ig = configManager.getConfiguration(CONFIG_GROUP, targetProfile + "_invertGaze", Boolean.class);
            if (ig != null) invertGazeDirection = ig;
        }

        isUpdatingControls = true;

        modelField.setText(String.valueOf(modelId));
        undergroundBox.setSelected(hideUnderground);

        scaleSlider.setValue((int) moonScale); scaleInput.setText(String.valueOf((int) moonScale));
        altitudeSlider.setValue(moonAltitude); altitudeInput.setText(String.valueOf(moonAltitude));
        offsetSliderX.setValue(skyOffsetX); offsetInputX.setText(String.valueOf(skyOffsetX));
        offsetSliderY.setValue(skyOffsetY); offsetInputY.setText(String.valueOf(skyOffsetY));
        rotationSlider.setValue(moonRotation); rotationInput.setText(String.valueOf(moonRotation));

        orbitBox.setSelected(enableOrbit);
        speedSlider.setValue(orbitSpeed); speedInput.setText(String.valueOf(orbitSpeed));

        hazeBox.setSelected(enableHaze);
        hazeSizeSlider.setValue(hazeSize); hazeSizeInput.setText(String.valueOf(hazeSize));
        hazeFadeBox.setSelected(enableHazeFade);
        invertGazeBox.setSelected(invertGazeDirection);

        phaseAutomationBox.setSelected(enablePhaseAutomation);
        phaseSlider.setValue(manualPhaseValue); phaseInput.setText(String.valueOf(manualPhaseValue));
        phaseSpeedSlider.setValue(phaseAutomationSpeed); phaseSpeedInput.setText(String.valueOf(phaseAutomationSpeed));

        phaseSlider.setEnabled(!enablePhaseAutomation);
        phaseInput.setEnabled(!enablePhaseAutomation);
        phaseSpeedSlider.setEnabled(enablePhaseAutomation);
        phaseSpeedInput.setEnabled(enablePhaseAutomation);

        String hex = "#" + Integer.toHexString(moonColor.getRGB()).substring(2).toUpperCase();
        colorPickerBtn.setText(hex);
        colorPickerBtn.setColor(moonColor);

        isUpdatingControls = false;
    }

    private void deleteSelectedProfile()
    {
        String active = (String) presetDropdown.getSelectedItem();
        if (active == null || active.equals("Default") || "New Profile...".equals(active)) return;

        String rawList = configManager.getConfiguration(CONFIG_GROUP, "moonPresets");
        if (rawList == null) return;

        List<String> list = new ArrayList<>(Arrays.asList(rawList.split(",")));
        list.remove(active);

        configManager.setConfiguration(CONFIG_GROUP, "moonPresets", String.join(",", list));
        configManager.setConfiguration(CONFIG_GROUP, "activePresetName", "Default");

        initializeActiveProfileSession();
    }
}