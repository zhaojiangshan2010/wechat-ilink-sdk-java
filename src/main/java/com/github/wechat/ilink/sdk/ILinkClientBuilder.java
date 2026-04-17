package com.github.wechat.ilink.sdk;

import com.github.wechat.ilink.sdk.core.config.ConfigLoader;
import com.github.wechat.ilink.sdk.core.config.ILinkConfig;
import com.github.wechat.ilink.sdk.core.listener.*;

public class ILinkClientBuilder {
  private ILinkConfig config = ConfigLoader.loadDefault();
  private final ListenerRegistry listenerRegistry = new ListenerRegistry();

  public ILinkClientBuilder config(ILinkConfig config) {
    this.config = config;
    return this;
  }

  public ILinkClientBuilder onLogin(OnLoginListener l) {
    listenerRegistry.addOnLoginListener(l);
    return this;
  }

  public ILinkClientBuilder onDisconnect(OnDisconnectListener l) {
    listenerRegistry.addOnDisconnectListener(l);
    return this;
  }

  public ILinkClientBuilder onHeartbeat(OnHeartbeatListener l) {
    listenerRegistry.addOnHeartbeatListener(l);
    return this;
  }

  public ILinkClientBuilder onMessage(OnMessageListener l) {
    listenerRegistry.addOnMessageListener(l);
    return this;
  }

  public ILinkClient build() {
    return new ILinkClient(config, listenerRegistry);
  }
}
