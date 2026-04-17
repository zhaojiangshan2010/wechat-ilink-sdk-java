package com.github.wechat.ilink.sdk;

import com.github.wechat.ilink.sdk.core.config.ILinkConfig;
import com.github.wechat.ilink.sdk.core.exception.NotLoginException;
import com.github.wechat.ilink.sdk.core.listener.*;
import com.github.wechat.ilink.sdk.core.login.LoginContext;
import com.github.wechat.ilink.sdk.core.login.LoginStatus;
import com.github.wechat.ilink.sdk.core.state.ConnectionStatus;
import com.github.wechat.ilink.sdk.core.model.WeixinMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试ILinkClient的所有功能
 */
public class ILinkClientTest {

    private static final Logger log = LoggerFactory.getLogger(ILinkClientTest.class);
    private ILinkClient client;
    private CountDownLatch loginLatch;
    private boolean loginSuccess;
    private String loginErrorMessage;

    @BeforeEach
    public void setUp() {
        // 初始化登录信号量
        loginLatch = new CountDownLatch(1);
        loginSuccess = false;
        loginErrorMessage = null;

        // 构建客户端
        client = ILinkClient.builder()
                .onLogin(new OnLoginListener() {
                    @Override
                    public void onLoginSuccess(LoginContext context) {
                        log.info("Login successful: {}", context);
                        loginSuccess = true;
                        loginLatch.countDown();
                    }

                    @Override
                    public void onLoginFailure(Throwable throwable) {
                        log.error("Login failed: {}", throwable.getMessage());
                        loginErrorMessage = throwable.getMessage();
                        loginLatch.countDown();
                    }
                })
                .onMessage(new OnMessageListener() {
                    @Override
                    public void onMessages(List<WeixinMessage> messages) {
                        log.info("Received messages: {}", messages.size());
                    }
                })
                .onHeartbeat(new OnHeartbeatListener() {
                    @Override
                    public void onHeartbeatSuccess() {
                        log.info("Heartbeat successful");
                    }

                    @Override
                    public void onHeartbeatFailure(Throwable throwable) {
                        log.error("Heartbeat failed: {}", throwable.getMessage());
                    }
                })
                .onDisconnect(new OnDisconnectListener() {
                    @Override
                    public void onDisconnect(Throwable cause) {
                        log.info("Disconnected: {}", cause != null ? cause.getMessage() : "unknown");
                    }

                    @Override
                    public void onReconnectStart(int attempt) {
                        log.info("Reconnect start, attempt: {}", attempt);
                    }

                    @Override
                    public void onReconnectSuccess() {
                        log.info("Reconnect successful");
                    }

                    @Override
                    public void onReconnectFailed(Throwable cause) {
                        log.error("Reconnect failed: {}", cause.getMessage());
                    }
                })
                .build();
    }

    @AfterEach
    public void tearDown() {
        if (client != null) {
            client.close();
        }
    }

    /**
     * 测试客户端构建
     */
    @Test
    public void testClientBuild() {
        assertNotNull(client, "Client should be built successfully");
        assertNotNull(client.getConfig(), "Config should be loaded");
        assertEquals(ConnectionStatus.DISCONNECTED, client.getConnectionStatus(), "Initial status should be DISCONNECTED");
        assertFalse(client.isLoggedIn(), "Client should not be logged in initially");
    }

    /**
     * 测试登录功能
     */
    @Test
    public void testLogin() throws Exception {
        // 执行登录
        String qrCodeImageContent = client.executeLogin();
        assertNotNull(qrCodeImageContent, "QR code image content should not be null");
        assertNotNull(client.getQrcode(), "QR code should not be null");
        assertEquals(ConnectionStatus.CONNECTING, client.getConnectionStatus(), "Status should be CONNECTING");
        assertEquals(LoginStatus.Status.WAITING, client.getLoginStatus().getStatus(), "Login status should be WAITING");

        // 等待登录完成（实际测试中需要手动扫描二维码）
        // 这里我们设置一个超时，实际使用时需要根据情况调整
        boolean loginCompleted = loginLatch.await(60, TimeUnit.SECONDS);
        if (loginCompleted) {
            if (loginSuccess) {
                assertEquals(ConnectionStatus.LOGGED_IN, client.getConnectionStatus(), "Status should be LOGGED_IN");
                assertTrue(client.isLoggedIn(), "Client should be logged in");
                assertNotNull(client.getLoginContext(), "Login context should not be null");
            } else {
                fail("Login failed: " + loginErrorMessage);
            }
        } else {
            fail("Login timed out");
        }
    }

    /**
     * 测试获取更新
     */
    @Test
    public void testGetUpdates() throws Exception {
        // 先登录
        testLogin();

        // 获取更新
        List<WeixinMessage> messages = client.getUpdates();
        assertNotNull(messages, "Messages should not be null");
    }

    /**
     * 测试发送文本消息
     */
    @Test
    public void testSendText() throws Exception {
        // 先登录
        testLogin();

        // 发送文本消息（需要替换为实际的用户ID）
        String testUserId = "test_user_id"; // 实际测试时需要替换
        String testText = "Hello from ILink SDK test"; 
        assertDoesNotThrow(() -> client.sendText(testUserId, testText), "Send text should not throw exception");
    }

    /**
     * 测试发送带输入状态的文本消息
     */
    @Test
    public void testSendTextWithTyping() throws Exception {
        // 先登录
        testLogin();

        // 发送带输入状态的文本消息
        String testUserId = "test_user_id"; // 实际测试时需要替换
        String testText = "Hello from ILink SDK test with typing";
        long typingMillis = 1000;
        assertDoesNotThrow(() -> client.sendTextWithTyping(testUserId, testText, typingMillis), "Send text with typing should not throw exception");
    }

    /**
     * 测试发送图片消息
     */
    @Test
    public void testSendImage() throws Exception {
        // 先登录
        testLogin();

        // 发送图片消息（需要替换为实际的用户ID和图片数据）
        String testUserId = "test_user_id"; // 实际测试时需要替换
        byte[] testImage = new byte[]{0x00, 0x01, 0x02}; // 实际测试时需要替换为真实图片数据
        String fileName = "test_image.jpg";
        String caption = "Test image";
        assertDoesNotThrow(() -> client.sendImage(testUserId, testImage, fileName, caption), "Send image should not throw exception");
    }

    /**
     * 测试输入状态管理
     */
    @Test
    public void testTyping() throws Exception {
        // 先登录
        testLogin();

        // 测试输入状态
        String testUserId = "test_user_id"; // 实际测试时需要替换
        assertDoesNotThrow(() -> client.startTyping(testUserId), "Start typing should not throw exception");
        Thread.sleep(500); // 模拟输入时间
        assertDoesNotThrow(() -> client.stopTyping(testUserId), "Stop typing should not throw exception");
    }

    /**
     * 测试上下文管理
     */
    @Test
    public void testContextManagement() throws Exception {
        // 先登录
        testLogin();

        // 测试上下文管理
        String testUserId = "test_user_id"; // 实际测试时需要替换
        assertDoesNotThrow(() -> client.clearContext(testUserId), "Clear context should not throw exception");
        assertDoesNotThrow(() -> client.clearAllContexts(), "Clear all contexts should not throw exception");
    }

    /**
     * 测试未登录状态下的操作
     */
    @Test
    public void testNotLoggedInOperations() {
        // 测试未登录状态下的操作
        String testUserId = "test_user_id";
        assertThrows(NotLoginException.class, () -> client.getUpdates(), "Should throw NotLoginException");
        assertThrows(NotLoginException.class, () -> client.sendText(testUserId, "test"), "Should throw NotLoginException");
        assertThrows(NotLoginException.class, () -> client.sendImage(testUserId, new byte[0], "test.jpg", "test"), "Should throw NotLoginException");
        assertThrows(NotLoginException.class, () -> client.startTyping(testUserId), "Should throw NotLoginException");
        assertThrows(NotLoginException.class, () -> client.stopTyping(testUserId), "Should throw NotLoginException");
    }

    /**
     * 测试取消登录
     */
    @Test
    public void testCancelLogin() {
        // 执行登录
        client.executeLogin();
        // 取消登录
        assertDoesNotThrow(() -> client.cancelLogin(), "Cancel login should not throw exception");
    }

    /**
     * 测试客户端关闭
     */
    @Test
    public void testClose() {
        // 测试关闭客户端
        assertDoesNotThrow(() -> client.close(), "Close should not throw exception");
        assertEquals(ConnectionStatus.CLOSED, client.getConnectionStatus(), "Status should be CLOSED");
    }
}
