package com.qcrtimo.qqmcchatsync;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ChatListener implements Listener {

    private final QQChatSync plugin;

    public ChatListener(QQChatSync plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        BotClient client = plugin.getBotClient();
        if (client == null || !client.isOpen()) {
            return;
        }

        // 获取纯文本消息
        String plainMessage = PlainTextComponentSerializer.plainText().serialize(event.message());
        String playerName = event.getPlayer().getName();
        long targetGroup = plugin.getConfig().getLong("target_group_id");

        // 格式化消息
        String format = plugin.getConfig().getString("format.mc_to_qq", "[%player%]: %message%");
        String finalMessage = format
                .replace("%player%", playerName)
                .replace("%message%", plainMessage);

        // 异步发送给 QQ
        client.sendGroupMessage(targetGroup, finalMessage);
    }
}