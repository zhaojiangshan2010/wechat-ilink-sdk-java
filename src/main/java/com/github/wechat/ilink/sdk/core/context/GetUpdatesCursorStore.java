package com.github.wechat.ilink.sdk.core.context;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class GetUpdatesCursorStore {
  private final Map<String, String> map = new ConcurrentHashMap<String, String>();

  public String get(String botId) {
    return map.get(botId);
  }

  public void put(String botId, String cursor) {
    if (botId != null) map.put(botId, cursor == null ? "" : cursor);
  }

  public void remove(String botId) {
    if (botId != null) map.remove(botId);
  }

  public void clear() {
    map.clear();
  }
}
