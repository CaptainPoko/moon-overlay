package poko.moon;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Model;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public class MoonOverlay extends Overlay
{
    private final Client client;
    private final MoonPlugin plugin;

    @Inject
    public MoonOverlay(Client client, MoonPlugin plugin)
    {
        this.client = client;
        this.plugin = plugin;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPriority(Overlay.PRIORITY_HIGH);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (plugin.getPanel() == null)
        {
            return null;
        }

        MoonPluginPanel ui = plugin.getPanel();

        // [BLOCK 1: UNIVERSAL AUTOMATIC UNDERGROUND CHECK]
        if (ui.isHideUnderground() && client.getLocalPlayer() != null)
        {
            net.runelite.api.coords.WorldPoint wp = client.getLocalPlayer().getWorldLocation();
            if (wp != null)
            {
                if (wp.getY() >= 9000 || wp.getRegionID() == 9808 || wp.getRegionID() == 10064)
                {
                    return null;
                }
            }

            if (client.isInInstancedRegion() && client.getPlane() == 0)
            {
                return null;
            }
        }

        int currentModelId = ui.getModelId();
        int currentAltitude = ui.getMoonAltitude();
        int offsetX = ui.getSkyOffsetX();
        int offsetY = ui.getSkyOffsetY();

        // [BLOCK 2: AUTOMATED ORBIT ROTATION VECTORS]
        if (ui.isEnableOrbit())
        {
            double timeMultiplier = (client.getGameCycle() * (ui.getOrbitSpeed() / 5.0)) / 1000.0;
            int orbitRadius = 2500;
            offsetX += (int) (Math.cos(timeMultiplier) * orbitRadius);
            offsetY += (int) (Math.sin(timeMultiplier) * orbitRadius);
        }

        float currentScale = (float) ui.getMoonScale();

        double radians = Math.toRadians(ui.getMoonRotation());
        float cosTheta = (float) Math.cos(radians);
        float sinTheta = (float) Math.sin(radians);

        Model finalizedSkyModel = client.loadModel(currentModelId);
        if (finalizedSkyModel == null) return null;

        // [BLOCK 3: SAFE HIGH-DISTANCE SCENE BOUNDS ANCHOR]
        int camYaw = client.getCameraYaw();
        double angleRad = (camYaw / 2048.0) * 2.0 * Math.PI;

        int distantX = (int) (client.getCameraX() + Math.sin(angleRad) * 2500) + offsetX;
        int distantY = (int) (client.getCameraY() + Math.cos(angleRad) * 2500) + offsetY;

        float[] verticesX = finalizedSkyModel.getVerticesX();
        float[] verticesY = finalizedSkyModel.getVerticesY();
        float[] verticesZ = finalizedSkyModel.getVerticesZ();

        // [BLOCK 4: PERSPECTIVE VERTEX PROJECTION WITH INLINE ROTATION & SCALE]
        Point[] screenPoints = new Point[verticesX.length];
        for (int i = 0; i < verticesX.length; ++i)
        {
            float scaledX = verticesX[i] * currentScale;
            float scaledY = verticesY[i] * currentScale;
            float scaledZ = verticesZ[i] * currentScale;

            double rx = (scaledX * cosTheta) - (scaledY * sinTheta);
            double ry = (scaledX * sinTheta) + (scaledY * cosTheta);
            double rz = scaledZ;

            screenPoints[i] = Perspective.localToCanvas(client,
                    distantX + (int) rx,
                    distantY + (int) ry,
                    currentAltitude + (int) rz);
        }

        Color baseColor = ui.getMoonColor();
        if (baseColor == null) baseColor = new Color(240, 240, 220);

        // --- PHASE TIMING CALCULATION SYNC ---
        double phaseCycleTime;
        if (ui.isEnablePhaseAutomation())
        {
            phaseCycleTime = (client.getGameCycle() * (ui.getPhaseAutomationSpeed() / 5.0) % 30000) / 30000.0 * 2.0 * Math.PI;
        }
        else
        {
            phaseCycleTime = Math.toRadians(ui.getManualPhaseValue());
        }

        // Handle Invert Direction checkbox behavior
        if (ui.isInvertGazeDirection())
        {
            phaseCycleTime = (2.0 * Math.PI) - phaseCycleTime;
        }

        float lightDirX = (float) Math.cos(phaseCycleTime);
        float lightDirY = (float) Math.sin(phaseCycleTime);

        // [BLOCK 5: PHASE-MASKED LAYERED CONCENTRIC ATMOSPHERIC HAZE GLOW]
        if (ui.isEnableHaze())
        {
            Point centerScreenPos = Perspective.localToCanvas(client, distantX, distantY, currentAltitude);
            if (centerScreenPos != null)
            {
                int baselineGlowRadius = (int) (currentScale * ui.getHazeSize());

                // Calculate fading multiplier based on current visibility (how much face is lit)
                double phaseLitRatio = (lightDirX + 1.0) / 2.0; // Normalizes horizontal lighting vector to 0.0 - 1.0 range
                float hazeFadeMultiplier = ui.isEnableHazeFade() ? (float) phaseLitRatio : 1.0f;

                for (int ringIndex = 3; ringIndex > 0; ringIndex--)
                {
                    int expandingRadius = baselineGlowRadius + (ringIndex * 22);
                    int alphaTransparency = 12 / ringIndex;

                    // Apply the fading multiplier factor straight to the transparency calculation metrics
                    alphaTransparency = Math.max(0, (int) (alphaTransparency * hazeFadeMultiplier));

                    // If the haze is completely faded out, skip rendering this ring to optimize performance
                    if (alphaTransparency <= 0) continue;

                    Color ringColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), alphaTransparency);
                    graphics.setColor(ringColor);

                    // CRITICAL FIX: Changed from fillArc to fillOval to restore a smooth, full-circle glow ring
                    graphics.fillOval(
                            centerScreenPos.getX() - expandingRadius,
                            centerScreenPos.getY() - expandingRadius,
                            expandingRadius * 2,
                            expandingRadius * 2
                    );
                }
            }
        }

        // [BLOCK 6: DYNAMIC TERMINATOR PHASE SHADER LOOP]
        int[] face1 = finalizedSkyModel.getFaceIndices1();
        int[] face2 = finalizedSkyModel.getFaceIndices2();
        int[] face3 = finalizedSkyModel.getFaceIndices3();

        for (int i = 0; i < face1.length; i++)
        {
            Point p1 = screenPoints[face1[i]];
            Point p2 = screenPoints[face2[i]];
            Point p3 = screenPoints[face3[i]];

            if (p1 != null && p2 != null && p3 != null)
            {
                int faceCenterX = (p1.getX() + p2.getX() + p3.getX()) / 3;
                int faceCenterY = (p1.getY() + p2.getY() + p3.getY()) / 3;

                Point centerScreenPos = Perspective.localToCanvas(client, distantX, distantY, currentAltitude);
                if (centerScreenPos == null) continue;

                float normalX = faceCenterX - centerScreenPos.getX();
                float normalY = faceCenterY - centerScreenPos.getY();
                float length = (float) Math.sqrt(normalX * normalX + normalY * normalY);

                if (length > 0) { normalX /= length; normalY /= length; }

                float illumination = (normalX * lightDirX) + (normalY * lightDirY);

                // Skip unlit geometry backfaces to cleanly mask out shadowed curvature curves
                if (illumination < -0.1f)
                {
                    continue;
                }

                Polygon poly = new Polygon();
                poly.addPoint(p1.getX(), p1.getY());
                poly.addPoint(p2.getX(), p2.getY());
                poly.addPoint(p3.getX(), p3.getY());

                int v1Hash = Math.abs(face1[i] * 13) % 100;
                int v2Hash = Math.abs(face2[i] * 19) % 100;
                int v3Hash = Math.abs(face3[i] * 29) % 100;
                int smoothNoise = (v1Hash + v2Hash + v3Hash) / 3;

                int r = baseColor.getRed();
                int g = baseColor.getGreen();
                int b = baseColor.getBlue();

                if (smoothNoise > 75)
                {
                    r = Math.max(0, r - 55); g = Math.max(0, g - 55); b = Math.max(0, b - 45);
                }
                else if (smoothNoise > 55)
                {
                    r = Math.max(0, r - 30); g = Math.max(0, g - 30); b = Math.max(0, b - 25);
                }
                else if (smoothNoise < 15)
                {
                    r = Math.min(255, r + 15); g = Math.min(255, g + 15); b = Math.min(255, b + 25);
                }

                // Smooth edge brightness dropoff scaling near shadow boundaries
                if (illumination < 0.2f)
                {
                    float fadeFactor = (illumination + 0.1f) / 0.3f;
                    r = (int) (r * fadeFactor);
                    g = (int) (g * fadeFactor);
                    b = (int) (b * fadeFactor);
                }

                Color dynamicFaceColor = new Color(r, g, b);
                graphics.setColor(dynamicFaceColor);
                graphics.fill(poly);
                graphics.draw(poly);
            }
        }
        return null;
    }
}