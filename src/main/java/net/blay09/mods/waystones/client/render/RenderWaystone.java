package net.blay09.mods.waystones.client.render;

import static org.lwjgl.opengl.GL11.*;

import net.blay09.mods.waystones.PlayerWaystoneData;
import net.blay09.mods.waystones.WaystoneConfig;
import net.blay09.mods.waystones.WaystoneManager;
import net.blay09.mods.waystones.Waystones;
import net.blay09.mods.waystones.block.TileWaystone;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.BufferUtils;
import java.nio.FloatBuffer;

public class RenderWaystone extends TileEntitySpecialRenderer {

    private static final ResourceLocation texture = new ResourceLocation(
        Waystones.MODID,
        "textures/entity/waystone.png");
    private static final ResourceLocation textureActive = new ResourceLocation(
        Waystones.MODID,
        "textures/entity/waystone_active.png");

    private static final ResourceLocation[] activeTextures = {
        new ResourceLocation(Waystones.MODID, "textures/entity/waystone_active_1.png"),
        new ResourceLocation(Waystones.MODID, "textures/entity/waystone_active_2.png"),
        new ResourceLocation(Waystones.MODID, "textures/entity/waystone_active_3.png"),
        new ResourceLocation(Waystones.MODID, "textures/entity/waystone_active_4.png"),
        new ResourceLocation(Waystones.MODID, "textures/entity/waystone_active_5.png"),
        new ResourceLocation(Waystones.MODID, "textures/entity/waystone_active_6.png") };

    private static final ResourceLocation textureNonActive = new ResourceLocation(
        Waystones.MODID,
        "textures/entity/waystone_nonactive.png");

    private final ModelWaystone model = new ModelWaystone();


private static final FloatBuffer LIGHT0_POS = BufferUtils.createFloatBuffer(4);
private static final FloatBuffer LIGHT1_POS = BufferUtils.createFloatBuffer(4);
private static final FloatBuffer AMBIENT = BufferUtils.createFloatBuffer(4);
private static final FloatBuffer DIFFUSE = BufferUtils.createFloatBuffer(4);
private static final FloatBuffer SPECULAR = BufferUtils.createFloatBuffer(4);

private static void setupWorldFixedItemLighting() {
    AMBIENT.clear(); AMBIENT.put(new float[] {0.15f, 0.15f, 0.15f, 1.0f}).flip();
    DIFFUSE.clear(); DIFFUSE.put(new float[] {0.85f, 0.85f, 0.85f, 1.0f}).flip();
    SPECULAR.clear(); SPECULAR.put(new float[] {0f, 0f, 0f, 1.0f}).flip();

    float[][] worldDirs = new float[][] {
        { 0.2f, 1.0f, 0.7f },   // key
        { -0.6f, 0.8f, -0.2f }  // fill
    };

    float yaw = (float) Math.toRadians(RenderManager.instance.playerViewY);
    float pitch = (float) Math.toRadians(RenderManager.instance.playerViewX);

    for (int i = 0; i < worldDirs.length; i++) {
        float x = worldDirs[i][0];
        float y = worldDirs[i][1];
        float z = worldDirs[i][2];

        float len = (float) Math.sqrt(x*x + y*y + z*z);
        if (len != 0f) { x /= len; y /= len; z /= len; }

        float cosY = (float) Math.cos(yaw);
        float sinY = (float) Math.sin(yaw);
        float x1 = x * cosY + z * sinY;
        float z1 = -x * sinY + z * cosY;

        float cosP = (float) Math.cos(pitch);
        float sinP = (float) Math.sin(pitch);
        float y2 = y * cosP - z1 * sinP;
        float z2 = y * sinP + z1 * cosP;

        FloatBuffer pos = (i == 0) ? LIGHT0_POS : LIGHT1_POS;
        pos.clear();
        pos.put(new float[] { x1, y2, z2, 0.0f }).flip();

        int light = (i == 0) ? GL11.GL_LIGHT0 : GL11.GL_LIGHT1;
        GL11.glLight(light, GL11.GL_POSITION, pos);
        GL11.glLight(light, GL11.GL_AMBIENT, AMBIENT);
        GL11.glLight(light, GL11.GL_DIFFUSE, DIFFUSE);
        GL11.glLight(light, GL11.GL_SPECULAR, SPECULAR);
    }

    GL11.glEnable(GL11.GL_LIGHT0);
    GL11.glEnable(GL11.GL_LIGHT1);
}

    float getCooldownProgress(TileWaystone tileWaystone) {
        if (!tileWaystone.hasWorldObj()) return 1f; // fully charged if not in world

        long lastUse = PlayerWaystoneData.getLastWarpStoneUse(Minecraft.getMinecraft().thePlayer);
        long cooldown = Waystones.getConfig().warpStoneCooldown * 1000L;
        long timeSince = System.currentTimeMillis() - lastUse;
        return Math.min(1f, Math.max(0f, (float) timeSince / cooldown));
    }

    public static int normalizeToFive(float x) {
        return Math.round(x * 5);
    }

    @Override
    public void renderTileEntityAt(TileEntity tileEntity, double x, double y, double z, float partialTicks) {
        TileWaystone tileWaystone = (TileWaystone) tileEntity;
        boolean stoneIsKnown = WaystoneManager.getKnownWaystone(tileWaystone.getWaystoneName()) != null
            || WaystoneManager.getServerWaystone(tileWaystone.getWaystoneName()) != null;
        bindTexture(texture);

        float angle = tileEntity.hasWorldObj()
            ? WaystoneManager.getRotationYaw(ForgeDirection.getOrientation(tileEntity.getBlockMetadata()))
            : 0f;

// Snapshot render state that affects lighting (prevents permanent brightness/texture-unit drift)
final int prevActiveTex = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
final float prevBrightX = OpenGlHelper.lastBrightnessX;
final float prevBrightY = OpenGlHelper.lastBrightnessY;

GL11.glPushMatrix();
try {
    // Apply world lighting for this tile (so it doesn't become too bright/dim incorrectly)
    if (tileWaystone.hasWorldObj()) {
        int packed = tileWaystone.getWorldObj().getLightBrightnessForSkyBlocks(
                tileWaystone.xCoord, tileWaystone.yCoord, tileWaystone.zCoord, 0);
        int lx = packed & 0xFFFF;
        int ly = (packed >> 16) & 0xFFFF;

        OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) lx, (float) ly);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }

    // Stable shaded look: TESRs in 1.7.10 can't use real world AO, so we approximate it
    // with fixed per-part shading (static, no camera-dependent lighting).
    RenderHelper.disableStandardItemLighting();
    GL11.glDisable(GL11.GL_LIGHTING);
    GL11.glDisable(GL11.GL_COLOR_MATERIAL);
    GL11.glEnable(GL11.GL_NORMALIZE);

    GL11.glColor4f(1f, 1f, 1f, 1f);
    GL11.glTranslated(x + 0.5, y, z + 0.5);
    GL11.glRotatef(angle, 0f, 1f, 0f);
    GL11.glRotatef(-180f, 1f, 0f, 0f);
    GL11.glScalef(0.5f, 0.5f, 0.5f);

    GL11.glEnable(GL_BLEND);
    GL11.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

    model.renderAllWithStaticAo();
    if (tileWaystone.hasWorldObj() && stoneIsKnown) {
        GL11.glScalef(1.05f, 1.05f, 1.05f);

        GL11.glDisable(GL11.GL_CULL_FACE); // render all faces

        // Render nonactive pillar (static shading)
        bindTexture(textureNonActive);
            model.renderPillarWithStaticAo(0.78f);

        // Render active pillar with glow (lighting off)
        boolean glowPass = false;
        if (!WaystoneConfig.disableTextGlow) {
            glowPass = true;
            GL11.glDisable(GL11.GL_LIGHTING);
            Minecraft.getMinecraft().entityRenderer.disableLightmap(0);
        }
        bindTexture(activeTextures[normalizeToFive(getCooldownProgress(tileWaystone))]);
        model.renderPillarWithStaticAo(0.78f);
        if (!WaystoneConfig.disableTextGlow) {
            // Re-enable lightmap after glow pass to prevent state leaking into world rendering
            Minecraft.getMinecraft().entityRenderer.enableLightmap(0);
        }

        GL11.glEnable(GL11.GL_CULL_FACE); // restore culling
    }

    GL11.glDisable(GL_BLEND);
} finally {
    // Restore GL state that can affect subsequent renders
    GL11.glDisable(GL11.GL_BLEND);
    GL11.glEnable(GL11.GL_CULL_FACE);
    GL11.glEnable(GL11.GL_LIGHTING);
    RenderHelper.enableStandardItemLighting();
    GL11.glColor4f(1f, 1f, 1f, 1f);

    // Always re-enable lightmap (TESR glow paths may disable it)
    Minecraft.getMinecraft().entityRenderer.enableLightmap(0);

    // Restore lightmap coords and active texture unit
    OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
    GL11.glEnable(GL11.GL_TEXTURE_2D);
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, prevBrightX, prevBrightY);
    OpenGlHelper.setActiveTexture(prevActiveTex);
    GL11.glPopMatrix();
}

        if (WaystoneConfig.showNametag && tileWaystone.hasWorldObj() && stoneIsKnown) {
            renderWaystoneName(tileWaystone, x + 0.5, y + 2.5, z + 0.5);
        }
    }

    private void renderWaystoneName(TileWaystone tile, double x, double y, double z) {
        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
        String name = tile.getWaystoneName();

        // Prevent state leaks into world rendering
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glPushMatrix();
        try {
        GL11.glTranslated(x, y, z);

        // Face the player
        GL11.glRotatef(-RenderManager.instance.playerViewY, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(RenderManager.instance.playerViewX, 1.0F, 0.0F, 0.0F);

        float scale = 0.01666667F * 1.6F; // adjust size
        GL11.glScalef(-scale, -scale, scale);

        // Draw background
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        int width = fontRenderer.getStringWidth(name) / 2;
        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        tess.setColorRGBA_F(0f, 0f, 0f, 0.5f); // semi-transparent black
        tess.addVertex(-width - 1, -1, 0.01);
        tess.addVertex(-width - 1, 8, 0.01);
        tess.addVertex(width + 1, 8, 0.01);
        tess.addVertex(width + 1, -1, 0.01);
        tess.draw();

        // Draw text
        fontRenderer.drawString(name, -width, 0, 0xFFFFFF);
        GL11.glDepthMask(true);
// white
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_BLEND);

        } finally {
            GL11.glPopMatrix();
            GL11.glColor4f(1f, 1f, 1f, 1f);
            GL11.glPopAttrib();
        }
    }

}
