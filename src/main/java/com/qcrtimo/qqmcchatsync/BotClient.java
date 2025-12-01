package com.qcrtimo.qqmcchatsync;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.Map;

public class BotClient extends WebSocketClient {

    private final QQChatSync plugin;

    public BotClient(URI serverUri, QQChatSync plugin) {
        //如果有Token，需要在Header中添加
        super(serverUri, getHeaders(plugin));
        this.plugin = plugin;
    }

    private static Map<String, String> getHeaders(QQChatSync plugin) {
        String token = plugin.getConfig().getString("access_token");
        if (token != null && !token.isEmpty()) {
            return Map.of("Authorization", "Bearer " + token);
        }
        return Map.of();
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        plugin.getLogger().info("已连接到 Onebot 机器人！");
    }

    @Override
    public void onMessage(String message) {
        try {
            JsonObject json = JsonParser.parseString(message).getAsJsonObject();
            //过滤非消息事件
            if (!json.has("post_type") || !json.get("post_type").getAsString().equals("message")) {
                return;
            }
            //过滤非群消息
            if (!json.has("message_type") || !json.get("message_type").getAsString().equals("group")) {
                return;
            }
            //检查群号
            long groupId = json.get("group_id").getAsLong();
            if (groupId != plugin.getConfig().getLong("target_group_id")) {
                return;
            }
            //获取发送者信息
            JsonObject sender = json.getAsJsonObject("sender");
            String nickname = sender.has("card") && !sender.get("card").getAsString().isEmpty()
                    ? sender.get("card").getAsString()
                    : sender.get("nickname").getAsString();
            //解析消息链 (OneBot 11)
            Component content = Component.empty();
            if (json.has("message") && json.get("message").isJsonArray()) {
                JsonArray messageChain = json.getAsJsonArray("message");
                for (JsonElement element : messageChain) {
                    JsonObject segment = element.getAsJsonObject();
                    String type = segment.get("type").getAsString();
                    JsonObject data = segment.getAsJsonObject("data");

                    if ("text".equals(type)) {
                        content = content.append(Component.text(data.get("text").getAsString()));
                    }
                    else if ("image".equals(type)) {
                        String url = data.get("url").getAsString();
                        //创建可点击的[图片]文本
                        Component imageComp = Component.text("[图片]")
                                .color(NamedTextColor.GREEN)
                                .hoverEvent(HoverEvent.showText(Component.text("点击打开图片: " + url)))
                                .clickEvent(ClickEvent.openUrl(url));
                        content = content.append(imageComp).append(Component.space());
                    }
                    else if ("face".equals(type)) {
                        content = content.append(Component.text("[表情]").color(NamedTextColor.YELLOW));
                    }
                }
            }
            else {
                //兼容raw_message
                content = Component.text(json.get("raw_message").getAsString());
            }
            //广播到服务器
            plugin.broadcastToGame(nickname, content);

        }
        catch (Exception e) {
            plugin.getLogger().warning("处理消息出错: " + e.getMessage());
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        for (int i = 0; i < 10; i++) {
            plugin.getLogger().warning("[" + "第" + (i+1) + "次重连" + "]" + "与机器人的连接断开，5秒后重试...");
            //简单的重连机制
            plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                plugin.connectBot();
            }, 100L);
        }

    }

    @Override
    public void onError(Exception ex) {
        plugin.getLogger().severe("WebSocket 错误: " + ex.getMessage());
    }
    //发送群消息
    public void sendGroupMessage(long groupId, String text) {
        if (isOpen()) {
            JsonObject params = new JsonObject();
            params.addProperty("group_id", groupId);
            params.addProperty("message", text);

            JsonObject packet = new JsonObject();
            packet.addProperty("action", "send_group_msg");
            packet.add("params", params);

            send(packet.toString());
        }
    }
}