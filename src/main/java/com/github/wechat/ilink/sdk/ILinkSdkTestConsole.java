package com.github.wechat.ilink.sdk;

import com.github.wechat.ilink.sdk.ILinkClient;
import com.github.wechat.ilink.sdk.core.context.ContextPoolManager;
import com.github.wechat.ilink.sdk.core.context.ConversationContext;
import com.github.wechat.ilink.sdk.core.login.LoginContext;
import com.github.wechat.ilink.sdk.core.model.FileItem;
import com.github.wechat.ilink.sdk.core.model.ImageItem;
import com.github.wechat.ilink.sdk.core.model.MessageItem;
import com.github.wechat.ilink.sdk.core.model.VideoItem;
import com.github.wechat.ilink.sdk.core.model.VoiceItem;
import com.github.wechat.ilink.sdk.core.model.WeixinMessage;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;

public class ILinkSdkTestConsole {

    private final Scanner scanner = new Scanner(System.in);

    // 按你当前 ILinkClient 的真实形态，优先走 builder()
    private final ILinkClient client = ILinkClient.builder().build();

    /**
     * 当前默认测试用户
     */
    private String currentUserId;

    public static void main(String[] args) {
        new ILinkSdkTestConsole().start();
    }

    public void start() {
        printBanner();

        boolean running = true;
        while (running) {
            try {
                printMenu();
                String cmd = readLine("请选择功能");
                switch (cmd) {
                    case "1":
                        login();
                        break;
                    case "2":
                        printLoginInfo();
                        break;
                    case "3":
                        setCurrentUser();
                        break;
                    case "4":
                        pollOnce();
                        break;
                    case "5":
                        sendText();
                        break;
                    case "6":
                        sendImage();
                        break;
                    case "7":
                        startTyping();
                        break;
                    case "8":
                        stopTyping();
                        break;
                    case "9":
                        sendTextWithTyping();
                        break;
                    case "10":
                        printContextInfo();
                        break;
                    case "11":
                        clearCurrentUserContext();
                        break;
                    case "0":
                        running = false;
                        break;
                    default:
                        System.out.println("无效选项，请重新输入。");
                }
            } catch (Throwable e) {
                System.err.println("[ERROR] " + e.getClass().getSimpleName() + ": " + e.getMessage());
                e.printStackTrace(System.err);
            }
        }

        try {
            client.close();
        } catch (Exception ignored) {
        }
        System.out.println("测试结束。");
    }

    private void printBanner() {
        System.out.println("==========================================");
        System.out.println("      ILink SDK Console Integration Test");
        System.out.println("==========================================");
        System.out.println("当前时间: " + LocalDateTime.now());
    }

    private void printMenu() {
        System.out.println();
        System.out.println("--------------- 菜单 ---------------");
        System.out.println("1. 登录");
        System.out.println("2. 查看登录信息");
        System.out.println("3. 设置当前测试用户");
        System.out.println("4. 拉取一次消息");
        System.out.println("5. 发送文本");
        System.out.println("6. 发送图片");
        System.out.println("7. 开始输入态");
        System.out.println("8. 停止输入态");
        System.out.println("9. 发送文本（自动带输入态）");
        System.out.println("10. 查看当前用户上下文");
        System.out.println("11. 清理当前用户上下文");
        System.out.println("0. 退出");
        System.out.println("------------------------------------");
    }

    private void login() throws Exception {
        if (client.getLoginContext() != null) {
            System.out.println("当前已经登录。");
            return;
        }

        System.out.println("开始登录...");
        String qrContent = client.executeLogin();

        System.out.println();
        System.out.println("请将下面内容渲染成二维码并扫码：");
        System.out.println(qrContent);
        System.out.println();

        LoginContext context = client.getLoginFuture().get();

        System.out.println("future.get() = " + context);
        System.out.println("client.getLoginContext() = " + client.getLoginContext());
        System.out.println("client.isLoggedIn() = " + client.isLoggedIn());
        System.out.println("loginStatus = " + client.getLoginStatus().getStatus());
        System.out.println("connectionStatus = " + client.getConnectionStatus());

        if (context == null || !client.isLoggedIn()) {
            throw new IllegalStateException("登录失败，未拿到有效 LoginContext");
        }

        System.out.println("登录成功。");
        printLoginInfo();
    }

    private void printLoginInfo() {
        LoginContext ctx = client.getLoginContext();
        if (ctx == null) {
            System.out.println("当前未登录。");
            return;
        }

        System.out.println("登录状态: " + client.getLoginStatus().getStatus());
        System.out.println("连接状态: " + client.getConnectionStatus());
        System.out.println("botId: " + ctx.getBotId());
        System.out.println("userId: " + ctx.getUserId());
        System.out.println("baseUrl: " + ctx.getBaseUrl());
        System.out.println("botToken(masked): " + mask(ctx.getBotToken()));
        System.out.println("当前默认测试用户: " + safe(currentUserId));
    }

    private void setCurrentUser() {
        ensureLogin();
        String userId = readLine("请输入 userId");
        if (userId == null || userId.trim().isEmpty()) {
            System.out.println("userId 不能为空。");
            return;
        }
        currentUserId = userId.trim();
        System.out.println("当前测试用户已设置为: " + currentUserId);
    }

    private void pollOnce() throws Exception {
        ensureLogin();

        List<WeixinMessage> messages = client.getUpdates();
        if (messages == null || messages.isEmpty()) {
            System.out.println("本次没有收到新消息。");
            return;
        }

        System.out.println("本次收到消息数: " + messages.size());
        for (WeixinMessage msg : messages) {
            printMessage(msg);
        }
    }

    private void sendText() throws Exception {
        ensureLogin();
        String toUserId = requireCurrentUser();
        String text = readLine("请输入要发送的文本");
        client.sendText(toUserId, text);
        System.out.println("文本发送完成。");
    }

    private void sendImage() throws Exception {
        ensureLogin();
        String toUserId = requireCurrentUser();
        String path = readLine("请输入图片路径");
        String caption = readLineAllowEmpty("请输入图片说明(可留空)");

        byte[] imageBytes = Files.readAllBytes(Paths.get(path));
        client.sendImage(
            toUserId,
            imageBytes,
            Paths.get(path).getFileName().toString(),
            emptyToNull(caption)
        );
        System.out.println("图片发送完成。");
    }

    private void startTyping() throws Exception {
        ensureLogin();
        String toUserId = requireCurrentUser();
        client.startTyping(toUserId);
        System.out.println("已发送输入中状态。");
    }

    private void stopTyping() throws Exception {
        ensureLogin();
        String toUserId = requireCurrentUser();
        client.stopTyping(toUserId);
        System.out.println("已发送停止输入状态。");
    }

    private void sendTextWithTyping() throws Exception {
        ensureLogin();
        String toUserId = requireCurrentUser();
        String text = readLine("请输入要发送的文本");
        String msText = readLine("请输入输入态停留时间(ms)，例如 1500");
        long typingMillis = Long.parseLong(msText);

        client.sendTextWithTyping(toUserId, text, typingMillis);
        System.out.println("已完成带输入态的文本发送。");
    }

    private void printContextInfo() {
        ensureLogin();
        String toUserId = requireCurrentUser();
        LoginContext loginContext = client.getLoginContext();

        ConversationContext ctx = ContextPoolManager.getInstance().get(loginContext.getBotId(), toUserId);
        if (ctx == null) {
            System.out.println("当前用户没有上下文。");
            return;
        }

        System.out.println("botId: " + loginContext.getBotId());
        System.out.println("userId: " + toUserId);
        System.out.println("latestContextToken: " + safe(ctx.getLatestContextToken()));
        System.out.println("typingTicket: " + safe(ctx.getTypingTicket()));
        System.out.println("sourceMessageId: " + ctx.getSourceMessageId());
        System.out.println("sourceMessageTime: " + ctx.getSourceMessageTime());
        System.out.println("lastUpdatedAt: " + ctx.getLastUpdatedAt());
        System.out.println("hasContextToken: " + ctx.hasContextToken());
    }

    private void clearCurrentUserContext() {
        ensureLogin();
        String toUserId = requireCurrentUser();
        client.clearContext(toUserId);
        System.out.println("当前用户上下文已清理。");
    }

    private void printMessage(WeixinMessage msg) {
        System.out.println("======================================");
        System.out.println("messageId  : " + msg.getMessage_id());
        System.out.println("fromUserId : " + safe(msg.getFrom_user_id()));
        System.out.println("toUserId   : " + safe(msg.getTo_user_id()));
        System.out.println("contextTok : " + mask(msg.getContext_token()));
        System.out.println("createTime : " + safeLong(msg.getCreate_time_ms()));

        if (msg.getItem_list() != null) {
            for (MessageItem item : msg.getItem_list()) {
                System.out.println("item.type  : " + item.getType());

                if (item.getText_item() != null) {
                    System.out.println("text       : " + item.getText_item().getText());
                }

                ImageItem imageItem = item.getImage_item();
                if (imageItem != null) {
                    System.out.println("image      : media=" + (imageItem.getMedia() != null));
                }

                FileItem fileItem = item.getFile_item();
                if (fileItem != null) {
                    System.out.println("file       : fileName=" + safe(fileItem.getFile_name())
                        + ", len=" + safe(fileItem.getLen())
                        + ", md5=" + safe(fileItem.getMd5()));
                }

                VoiceItem voiceItem = item.getVoice_item();
                if (voiceItem != null) {
                    System.out.println("voice      : media=" + (voiceItem.getMedia() != null)
                        + ", playtime=" + voiceItem.getPlaytime()
                        + ", sampleRate=" + voiceItem.getSample_rate());
                }

                VideoItem videoItem = item.getVideo_item();
                if (videoItem != null) {
                    System.out.println("video      : media=" + (videoItem.getMedia() != null)
                        + ", playLength=" + videoItem.getPlay_length()
                        + ", videoMd5=" + safe(videoItem.getVideo_md5()));
                }
            }
        }

        System.out.println("======================================");
    }

    private void ensureLogin() {
        if (client.getLoginContext() == null) {
            throw new IllegalStateException("当前未登录，请先执行登录。");
        }
    }

    private String requireCurrentUser() {
        if (currentUserId == null || currentUserId.trim().isEmpty()) {
            throw new IllegalStateException("当前未设置测试用户，请先执行“设置当前测试用户”。");
        }
        return currentUserId;
    }

    private String readLine(String prompt) {
        System.out.print(prompt + ": ");
        return scanner.nextLine();
    }

    private String readLineAllowEmpty(String prompt) {
        System.out.print(prompt + ": ");
        return scanner.nextLine();
    }

    private static String emptyToNull(String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }

    private static String safe(String v) {
        return v == null ? "null" : v;
    }

    private static String safeLong(Long v) {
        return v == null ? "null" : String.valueOf(v);
    }

    private static String mask(String token) {
        if (token == null || token.length() <= 8) {
            return "****";
        }
        return token.substring(0, 4) + "****" + token.substring(token.length() - 4);
    }
}
