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

package eu.the5zig.mod.mixin;

import eu.the5zig.mod.The5zigMod;
import eu.the5zig.mod.gui.ingame.ItemStack;
import eu.the5zig.mod.util.ChatComponentBuilder;
import eu.the5zig.mod.util.TabList;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.*;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayClient.class)
public abstract class MixinNetHandlerPlayClient {

    @Inject(method = "handleServerDifficulty", at = @At(value = "TAIL"))
    public void handleServerDifficulty(SPacketServerDifficulty _pkt, CallbackInfo _ci) {
        The5zigMod.getListener().handleServerDifficulty();
    }

    @Inject(method = "handleCustomPayload", at = @At(value = "RETURN", ordinal = 0))
    public void handleCustomPayload(SPacketCustomPayload packet, CallbackInfo _ci) {
        The5zigMod.getListener().handlePluginMessage(packet.getChannelName(),
                packet.getBufferData());
    }

    @Inject(method = "handlePlayerListHeaderFooter", at = @At("TAIL"))
    public void handlePlayerListHeaderFooter(SPacketPlayerListHeaderFooter packet, CallbackInfo _ci) {
        TabList tabList = new TabList(packet.getHeader().getFormattedText(), packet.getFooter().getFormattedText());
        The5zigMod.getListener().onPlayerListHeaderFooter(tabList);
    }

    @Inject(method = "handleChat", at = @At(value = "INVOKE",
            target = "net/minecraft/client/gui/GuiIngame.addChatMessage(Lnet/minecraft/util/text/ChatType;Lnet/minecraft/util/text/ITextComponent;)V",
            shift = At.Shift.BEFORE), cancellable = true)
    public void handleChat(SPacketChat packet, CallbackInfo ci) {
        byte type = packet.getType().getId();
        ITextComponent comp = packet.getChatComponent();
        String formatted = comp.getFormattedText().replace("§r", "");
        boolean b;
        if(type == 2) {
            b = The5zigMod.getListener().onActionBar(formatted);
        }
        else {
            b = The5zigMod.getListener().onServerChat(formatted, ChatComponentBuilder.toInterface(comp), comp);
        }

        if(b)
            ci.cancel();
    }

    @Inject(method = "handleSetSlot", at = @At("HEAD"))
    public void handleSetSlot(SPacketSetSlot packet, CallbackInfo _ci) {
        try {
            ItemStack stack = (ItemStack) Class.forName("WrappedItemStack").getConstructor(net.minecraft.item.ItemStack.class)
                    .newInstance(packet.getStack());
            The5zigMod.getListener().onInventorySetSlot(packet.getSlot(), stack);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Inject(method = "handleTitle", at = @At(value = "RETURN", ordinal = 0))
    public void titleBar(SPacketTitle packet, CallbackInfo _ci) {
        The5zigMod.getListener().onActionBar(packet.getMessage().getFormattedText());
    }


    @Inject(method = "handleTitle", at = @At(value = "RETURN", ordinal = 1))
    public void titleClear(SPacketTitle packet, CallbackInfo _ci) {
        The5zigMod.getListener().onTitle(null, null);
    }

    @Inject(method = "handleTitle", at = @At(value = "RETURN", ordinal = 2))
    public void title(SPacketTitle packet, CallbackInfo _ci) {
        String text = packet.getMessage().getFormattedText();

        switch(packet.getType()) {
            case TITLE:
                The5zigMod.getListener().onTitle(text, null);
                break;
            case SUBTITLE:
                The5zigMod.getListener().onTitle(null, text);
                break;
        }
    }
}
