package com.github.wechat.ilink.sdk;

import com.github.wechat.ilink.sdk.core.config.ILinkConfig;
import com.github.wechat.ilink.sdk.core.context.ContextPoolManager;
import com.github.wechat.ilink.sdk.core.context.GetUpdatesCursorStore;
import com.github.wechat.ilink.sdk.core.exception.NotLoginException;
import com.github.wechat.ilink.sdk.core.executor.ExecutorManager;
import com.github.wechat.ilink.sdk.core.http.BusinessApiClient;
import com.github.wechat.ilink.sdk.core.http.HttpClientFacade;
import com.github.wechat.ilink.sdk.core.lifecycle.HealthChecker;
import com.github.wechat.ilink.sdk.core.lifecycle.HeartbeatService;
import com.github.wechat.ilink.sdk.core.listener.*;
import com.github.wechat.ilink.sdk.core.login.*;
import com.github.wechat.ilink.sdk.core.retry.ExponentialBackoffStrategy;
import com.github.wechat.ilink.sdk.core.retry.RetryPolicy;
import com.github.wechat.ilink.sdk.core.serializer.JsonSerializer;
import com.github.wechat.ilink.sdk.core.serializer.Serializer;
import com.github.wechat.ilink.sdk.core.state.ClientStateManager;
import com.github.wechat.ilink.sdk.core.state.ConnectionStatus;
import com.github.wechat.ilink.sdk.service.*;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ILinkClient implements AutoCloseable {
  private static final Logger log = LoggerFactory.getLogger(ILinkClient.class);
  private final ILinkConfig config;
  private final ListenerRegistry listenerRegistry;
  private final ExecutorManager executorManager;
  private final ClientStateManager stateManager = new ClientStateManager();
  private final ContextPoolManager contextPoolManager = ContextPoolManager.getInstance();
  private final GetUpdatesCursorStore cursorStore = new GetUpdatesCursorStore();

  private final Serializer serializer;
  private final RetryPolicy retryPolicy;
  private final HttpClientFacade httpClientFacade;
  private final BusinessApiClient businessApiClient;

  private final LoginService loginService;
  private final LoginStatus loginStatus = new LoginStatus();
  private final AtomicReference<LoginContext> loginContext = new AtomicReference<LoginContext>();
  private final UpdateService updateService;
  private final MediaService mediaService;
  private final MessageService messageService;
  private final TypingService typingService;

  private volatile CompletableFuture<LoginContext> loginFuture;
  private volatile String qrcode;
  private HeartbeatService heartbeatService;

  public static ILinkClientBuilder builder() {
    return new ILinkClientBuilder();
  }

  public ILinkClient(ILinkConfig config, ListenerRegistry listenerRegistry) {
    this.config = config;
    this.listenerRegistry = listenerRegistry;
    this.executorManager = new ExecutorManager(config);
    this.serializer = new JsonSerializer();
    this.retryPolicy =
        new RetryPolicy(
            config.getHttpMaxRetries(),
            new ExponentialBackoffStrategy(
                config.getRetryBaseDelayMs(),
                config.getRetryMaxDelayMs(),
                config.isRetryJitterEnabled()));
    this.httpClientFacade = new HttpClientFacade(config, retryPolicy);
    this.businessApiClient = new BusinessApiClient(config, serializer, httpClientFacade);
    this.loginService =
        new LoginService(config, serializer, httpClientFacade, executorManager.ioExecutor());
    this.updateService = new UpdateService(config, businessApiClient, cursorStore);
    this.mediaService = new MediaService(config, businessApiClient, httpClientFacade);
    this.messageService = new MessageService(config, businessApiClient, mediaService);
    this.typingService = new TypingService(config, businessApiClient);
    initHeartbeat();
  }

  private void initHeartbeat() {
    if (!config.isHeartbeatEnabled()) return;
    this.heartbeatService =
        new HeartbeatService(
            executorManager.scheduler(),
            config.getHeartbeatIntervalMs(),
            new HealthChecker() {
              public void check() throws Exception {
                if (!stateManager.isLoggedIn()) return;
                LoginContext ctx = loginContext.get();
                if (ctx == null) return;
                updateService.poll(ctx);
              }
            },
            listenerRegistry);
  }

  public String executeLogin() {
    stateManager.set(ConnectionStatus.CONNECTING);
    try {
      QRCodeResponse response = loginService.getQRCode();
      this.qrcode = response.getQrcode();
      this.loginFuture = loginService.startLoginPolling(qrcode, loginStatus, loginContext);
      this.loginFuture.whenComplete(
          (ctx, ex) -> {
            if (ex != null) {
              stateManager.set(ConnectionStatus.DISCONNECTED);
              for (OnLoginListener l : listenerRegistry.getLoginListeners()) l.onLoginFailure(ex);
              return;
            }
            stateManager.set(ConnectionStatus.LOGGED_IN);
            for (OnLoginListener l : listenerRegistry.getLoginListeners()) l.onLoginSuccess(ctx);
            if (heartbeatService != null) heartbeatService.start();
          });
      return response.getQrcodeImgContent();
    } catch (RuntimeException | IOException e) {
      stateManager.set(ConnectionStatus.DISCONNECTED);
      for (OnLoginListener l : listenerRegistry.getLoginListeners()) l.onLoginFailure(e);
      throw new RuntimeException("start login failed", e);
    }
  }

  public List<com.github.wechat.ilink.sdk.core.model.WeixinMessage> getUpdates()
      throws IOException {
    List<com.github.wechat.ilink.sdk.core.model.WeixinMessage> messages =
        updateService.poll(requireLogin());
    if (messages != null && !messages.isEmpty())
      for (OnMessageListener l : listenerRegistry.getMessageListeners()) l.onMessages(messages);
    return messages;
  }

  public void sendText(String toUserId, String text) throws IOException {
    messageService.sendText(requireLogin(), toUserId, text);
  }

  public void sendTextWithTyping(String toUserId, String text, long typingMillis)
      throws IOException {
    typingService.startTyping(requireLogin(), toUserId);
    try {
      if (typingMillis > 0) {
        try {
          Thread.sleep(typingMillis);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
      messageService.sendText(requireLogin(), toUserId, text);
    } finally {
      typingService.stopTyping(requireLogin(), toUserId);
    }
  }

  public void sendImage(String toUserId, byte[] imageBytes, String fileName, String caption)
      throws IOException {
    messageService.sendImage(requireLogin(), toUserId, imageBytes, fileName, caption);
  }

  public void startTyping(String toUserId) throws IOException {
    typingService.startTyping(requireLogin(), toUserId);
  }

  public void stopTyping(String toUserId) throws IOException {
    typingService.stopTyping(requireLogin(), toUserId);
  }

  public CompletableFuture<LoginContext> getLoginFuture() {
    return loginFuture;
  }

  public LoginContext getLoginContext() {
    return loginContext.get();
  }

  public LoginStatus getLoginStatus() {
    return loginStatus;
  }

  public ConnectionStatus getConnectionStatus() {
    return stateManager.get();
  }

  public boolean isLoggedIn() {
    return stateManager.isLoggedIn();
  }

  public String getQrcode() {
    return qrcode;
  }

  public ILinkConfig getConfig() {
    return config;
  }

  public void clearContext(String userId) {
    LoginContext ctx = loginContext.get();
    if (ctx != null) contextPoolManager.remove(ctx.getBotId(), userId);
  }

  public void clearAllContexts() {
    contextPoolManager.clearAll();
  }

  public void cancelLogin() {
    loginService.cancelCurrentLogin();
  }

  private LoginContext requireLogin() {
    LoginContext ctx = loginContext.get();
    if (ctx == null) throw new NotLoginException("not logged in");
    return ctx;
  }

  public void close() {
    log.info("closing ILinkClient");
    stateManager.set(ConnectionStatus.DISCONNECTING);
    if (heartbeatService != null) heartbeatService.close();
    loginService.close();
    cursorStore.clear();
    contextPoolManager.clearAll();
    executorManager.close();
    stateManager.set(ConnectionStatus.CLOSED);
  }
}
