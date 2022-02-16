package com.terriblefriends.chatlinks.mixin;

import net.minecraft.client.option.ChatVisibility;
import net.minecraft.network.MessageType;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.filter.TextStream;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {
    @Shadow
    public ServerPlayerEntity player;
    @Shadow public void disconnect(Text textComponent){}
    @Shadow public void sendPacket(Packet<?> packet){}
    @Shadow private void executeCommand(String input){}
    @Shadow @Final private MinecraftServer server;
    @Shadow private int messageCooldown;

    @Inject(method="handleMessage",at=@At("HEAD"),cancellable = true)
    private void handleMessageMixin(TextStream.Message message, CallbackInfo ci) {
        if (this.player.getClientChatVisibility() == ChatVisibility.HIDDEN) {
            this.sendPacket(new GameMessageS2CPacket((new TranslatableText("chat.disabled.options")).formatted(Formatting.RED), MessageType.SYSTEM, Util.NIL_UUID));
        } else {
            this.player.updateLastActionTime();
            String string = message.getRaw();
            if (string.startsWith("/")) {
                this.executeCommand(string);
            } else {
                String string2 = message.getFiltered();
                String[] httpcheck = string2.split(" ");
                String link = null;
                for (int check=0;check<httpcheck.length;check++){
                    if (httpcheck[check].contains("http")) {
                        link = httpcheck[check];
                    }
                }
                Text text = string2.isEmpty() ? null : new TranslatableText("chat.type.text", this.player.getDisplayName(), string2);
                Text text2 = new TranslatableText("chat.type.text", this.player.getDisplayName(), string);
                if (link != null) {
                    text = text.copy().setStyle(text.getStyle().withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, link)));
                    text2 = text2.copy().setStyle(text.getStyle().withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL,link)));
                }
                Text finalText = text;
                Text finalText1 = text2;
                this.server.getPlayerManager().broadcast(text2, (player) -> {
                    return this.player.shouldFilterMessagesSentTo(player) ? finalText : finalText1;
                }, MessageType.CHAT, this.player.getUuid());
            }

            this.messageCooldown += 20;
            if (this.messageCooldown > 200 && !this.server.getPlayerManager().isOperator(this.player.getGameProfile())) {
                this.disconnect(new TranslatableText("disconnect.spam"));
            }

        }
        if (ci.isCancellable()) {ci.cancel();}
    }
}
