package poko.moon;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("moonoverlay")
public interface MoonConfig extends Config
{
    enum Notice
    {
        OPEN_SIDEBAR
    }

    @ConfigItem(
            keyName = "sidebarNoticeText",
            name = "⚠️ Notice: Please use the Moon sidebar icon panel on the right edge of your screen to adjust settings!",
            description = "",
            position = 1
    )
    default Notice sidebarNoticeText()
    {
        return Notice.OPEN_SIDEBAR;
    }

    // Hidden config items remain here unaltered to maintain profile validation metrics
    @ConfigItem(keyName = "modelId", name = "Model ID", description = "", hidden = true)
    default int modelId() { return 10589; }

    @ConfigItem(keyName = "moonAltitude", name = "Altitude", description = "", hidden = true)
    default int moonAltitude() { return -1700; }

    @ConfigItem(keyName = "skyOffsetX", name = "Offset X", description = "", hidden = true)
    default int skyOffsetX() { return 300; }

    @ConfigItem(keyName = "skyOffsetY", name = "Offset Y", description = "", hidden = true)
    default int skyOffsetY() { return 500; }

    @ConfigItem(keyName = "moonScale", name = "Moon Scale", description = "", hidden = true)
    default int moonScale() { return 5; }

    @ConfigItem(keyName = "hazeSize", name = "Haze Size", description = "", hidden = true)
    default int hazeSize() { return 75; }

    @ConfigItem(keyName = "moonRotation", name = "Moon Rotation", description = "", hidden = true)
    default int moonRotation() { return 0; }

    @ConfigItem(keyName = "enableOrbit", name = "Enable Orbit", description = "", hidden = true)
    default boolean enableOrbit() { return true; }

    @ConfigItem(keyName = "orbitSpeed", name = "Orbit Speed", description = "", hidden = true)
    default int orbitSpeed() { return 5; }

    @ConfigItem(keyName = "enableHaze", name = "Enable Haze", description = "", hidden = true)
    default boolean enableHaze() { return true; }

    @ConfigItem(keyName = "hideUnderground", name = "Hide Underground", description = "", hidden = true)
    default boolean hideUnderground() { return true; }

    @ConfigItem(keyName = "moonColor", name = "Moon Color", description = "", hidden = true)
    default Color moonColor() { return new Color(240, 240, 220); }

    @ConfigItem(keyName = "moonPresets", name = "Presets", description = "", hidden = true)
    default String moonPresets() { return ""; }

    @ConfigItem(keyName = "activePresetName", name = "Active Preset", description = "", hidden = true)
    default String activePresetName() { return "Default"; }
}