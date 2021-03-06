/*
 * Copyright (c) 2019-2020 5zig Reborn
 *
 * This file is part of The 5zig Mod
 * The 5zig Mod is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The 5zig Mod is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with The 5zig Mod.  If not, see <http://www.gnu.org/licenses/>.
 */

package eu.the5zig.mod.util;

import com.mojang.blaze3d.platform.GlStateManager;
import eu.the5zig.mod.The5zigMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.PlayerRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Team;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public class RewardTagUtils {

    private static final float SCALE_FACTOR = 0.5F;

    public static boolean shouldRender(PlayerEntity player) {
        if(player == Minecraft.getInstance().player && !The5zigMod.getConfig().getBool("showOwnNameTag")) return false;
        if(player.isInvisibleToPlayer(Minecraft.getInstance().player)) return false;
        if(player.isSneaking()) return false;
        if(player.isBeingRidden()) return false;

        int renderDist = 4096; // 64^2
        if(player.getDistanceSq(Minecraft.getInstance().player) > renderDist) return false;

        return shouldRenderTeam(player);

    }

    public static void render(PlayerRenderer renderer, String str, PlayerEntity pl, double x, double y, double z) {
        FontRenderer fontRenderer = renderer.getFontRendererFromRenderManager();
        float f = 1.6F;
        float f1 = 0.016666668F * f * SCALE_FACTOR;
        GlStateManager.pushMatrix();
        GlStateManager.translatef((float) x + 0.0F, (float) y + pl.getHeight() + 0.35F, (float) z);
        GL11.glNormal3f(0.0F, 1.0F, 0.0F);
        GlStateManager.rotatef(-renderer.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotatef(renderer.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.scalef(-f1, -f1, f1);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.disableDepthTest();
        GlStateManager.enableBlend();
        GlStateManager.blendFuncSeparate(770, 771, 1, 0);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder worldrenderer = tessellator.getBuffer();
        int i = 0;

        int j = fontRenderer.getStringWidth(str) / 2;
        GlStateManager.disableTexture();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        worldrenderer.pos((double) (-j - 1), (double) (-1 + i), 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        worldrenderer.pos((double) (-j - 1), (double) (8 + i), 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        worldrenderer.pos((double) (j + 1), (double) (8 + i), 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        worldrenderer.pos((double) (j + 1), (double) (-1 + i), 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        tessellator.draw();
        GlStateManager.enableTexture();

        makeStr(fontRenderer, str);

        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    private static void makeStr(FontRenderer renderer, String str) {

        int x = -renderer.getStringWidth(str) / 2;
        renderTxt(renderer, str, x);

    }

    private static void renderTxt(FontRenderer renderer, String toRender, int x) {
        GlStateManager.disableDepthTest();
        GlStateManager.depthMask(true);
        GlStateManager.disableDepthTest();
        GlStateManager.depthMask(false);

        int y = 0;
        GlStateManager.color4f(255, 255, 255, .5F);
        renderer.drawString(toRender, x, y, Color.WHITE.darker().darker().darker().darker().darker().getRGB() * 255);


        GlStateManager.enableDepthTest();
        GlStateManager.depthMask(true);

        GlStateManager.color3f(1.0F, 1.0F, 1.0F);
        renderer.drawString(toRender, x, y, Color.WHITE.darker().getRGB());

    }

    private static boolean shouldRenderTeam(PlayerEntity player) {
        Team team = player.getTeam();
        Team team1 = Minecraft.getInstance().player.getTeam();

        if (team != null) {
            Team.Visible enumVisible = team.getNameTagVisibility();
            switch (enumVisible) {
                case ALWAYS:
                    return true;
                case NEVER:
                    return false;
                case HIDE_FOR_OTHER_TEAMS:
                    return team1 == null || team.isSameTeam(team1);
                case HIDE_FOR_OWN_TEAM:
                    return team1 == null || !team.isSameTeam(team1);
                default:
                    return true;
            }
        }
        return true;
    }

}
