package com.poko.moon;

import javax.inject.Inject;
import lombok.Getter;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;
import java.awt.image.BufferedImage;

@PluginDescriptor(
        name = "Moon Overlay",
        description = "Renders a custom sky object with sidebar control customization features",
        tags = {"overlay", "sky", "moon", "skybox", "night", "orbit"}
)
public class MoonPlugin extends Plugin
{
    @Inject
    private OverlayManager overlayManager;

    @Inject
    private MoonOverlay overlay;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private ColorPickerManager colorPickerManager;

    @Inject
    private ConfigManager configManager;

    @Getter
    private MoonPluginPanel panel;

    private NavigationButton navButton;

    @Override
    protected void startUp() throws Exception
    {
        // Pass both managers to handle color selections and disk profile reading/writing
        panel = new MoonPluginPanel(colorPickerManager, configManager);

        BufferedImage icon;
        try
        {
            icon = ImageUtil.loadImageResource(getClass(), "/moonicon.png");
            if (icon == null) throw new java.io.IOException();

            if (icon.getWidth() != 16 || icon.getHeight() != 16)
            {
                java.awt.Image scaledImg = icon.getScaledInstance(16, 16, java.awt.Image.SCALE_SMOOTH);
                BufferedImage fixedIcon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
                java.awt.Graphics2D g2d = fixedIcon.createGraphics();
                g2d.drawImage(scaledImg, 0, 0, null);
                g2d.dispose();
                icon = fixedIcon;
            }
        }
        catch (Exception e)
        {
            icon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g2d = icon.createGraphics();
            g2d.setColor(java.awt.Color.GRAY);
            g2d.fillRect(2, 2, 12, 12);
            g2d.dispose();
        }

        navButton = NavigationButton.builder()
                .tooltip("Moon Plugin Configuration")
                .icon(icon)
                .priority(20)
                .panel(panel)
                .build();

        clientToolbar.addNavigation(navButton);
        overlayManager.add(overlay);


        panel.initializeActiveProfileSession();
    }

    @Override
    protected void shutDown() throws Exception
    {
        clientToolbar.removeNavigation(navButton);
        overlayManager.remove(overlay);
        panel = null;
    }
}