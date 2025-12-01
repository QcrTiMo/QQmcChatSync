package com.qcrtimo.qqmcchatsync;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;

public class QQChatSync extends JavaPlugin {

    private BotClient botClient;
    private static QQChatSync instance;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        connectBot();
        //注册游戏内聊天监听
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getLogger().info("QQmcChatSync启动完毕");
    }

    @Override
    public void onDisable() {
        if (botClient != null) {
            botClient.close();
        }
    }

    public void connectBot() {
        if (botClient != null && botClient.isOpen()) {
            botClient.close();
        }
        String url = getConfig().getString("socket_url");
        try {
            botClient = new BotClient(new URI(url), this);
            botClient.connect();
        }
        catch (Exception e) {
            getLogger().severe("无法连接到 Onebot WebSocket: " + e.getMessage());
        }
    }

    public BotClient getBotClient() {
        return botClient;
    }
    public static QQChatSync getInstance() {
        return instance;
    }
    //广播消息到服务器
    public void broadcastToGame(String nickname, Component messageContent) {
        String format = getConfig().getString("format.qq_to_mc", "<gray>Q群: <white>[<aqua>%nickname%<white>]: ");
        //使用MiniMessage解析格式配置
        Component prefix = MiniMessage.miniMessage().deserialize(format.replace("%nickname%", nickname));
        //拼接前缀和消息内容
        getServer().broadcast(prefix.append(messageContent));
    }
}