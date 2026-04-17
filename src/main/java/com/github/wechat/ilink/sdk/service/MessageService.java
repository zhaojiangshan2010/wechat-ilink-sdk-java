package com.github.wechat.ilink.sdk.service;

import com.github.wechat.ilink.sdk.core.config.ILinkConfig;
import com.github.wechat.ilink.sdk.core.context.ContextPoolManager;
import com.github.wechat.ilink.sdk.core.context.ConversationContext;
import com.github.wechat.ilink.sdk.core.exception.ILinkException;
import com.github.wechat.ilink.sdk.core.http.BusinessApiClient;
import com.github.wechat.ilink.sdk.core.login.LoginContext;
import com.github.wechat.ilink.sdk.core.model.ApiResponse;
import com.github.wechat.ilink.sdk.core.model.BaseInfo;
import com.github.wechat.ilink.sdk.core.model.ImageItem;
import com.github.wechat.ilink.sdk.core.model.MessageItem;
import com.github.wechat.ilink.sdk.core.model.SendMessageRequest;
import com.github.wechat.ilink.sdk.core.model.UploadedMedia;
import com.github.wechat.ilink.sdk.core.utils.RandomUtils;
import java.io.IOException;
import java.util.Arrays;

public class MessageService {

  private final ILinkConfig config;
  private final BusinessApiClient apiClient;
  private final MediaService mediaService;
  private final ContextPoolManager contextPoolManager = ContextPoolManager.getInstance();

  public MessageService(
      ILinkConfig config, BusinessApiClient apiClient, MediaService mediaService) {
    this.config = config;
    this.apiClient = apiClient;
    this.mediaService = mediaService;
  }

  public void sendText(LoginContext loginContext, String toUserId, String text) throws IOException {
    ConversationContext ctx = contextPoolManager.get(loginContext.getBotId(), toUserId);
    if (ctx == null || !ctx.hasContextToken()) {
      throw new ILinkException("missing latest context token for userId=" + toUserId);
    }

    SendMessageRequest.Msg msg =
        new SendMessageRequest.Msg(
            toUserId,
            RandomUtils.clientId("ilink-sdk"),
            ctx.getLatestContextToken(),
            Arrays.asList(MessageItem.text(text)));

    apiClient.post(
        loginContext,
        "/ilink/bot/sendmessage",
        new SendMessageRequest(msg, new BaseInfo(config.getChannelVersion())),
        ApiResponse.class);
  }

  public void sendImage(
      LoginContext loginContext,
      String toUserId,
      byte[] imageBytes,
      String fileName,
      String caption)
      throws IOException {
    if (caption != null && !caption.isEmpty()) {
      sendText(loginContext, toUserId, caption);
    }

    ConversationContext ctx = contextPoolManager.get(loginContext.getBotId(), toUserId);
    if (ctx == null || !ctx.hasContextToken()) {
      throw new ILinkException("missing latest context token for userId=" + toUserId);
    }

    UploadedMedia uploaded = mediaService.uploadImage(loginContext, toUserId, imageBytes, fileName);

    ImageItem imageItem = new ImageItem();
    imageItem.setMedia(uploaded.getMedia());
    imageItem.setAeskey(uploaded.getAesKeyHex());
    imageItem.setMid_size(uploaded.getEncryptedSize());

    MessageItem item = new MessageItem();
    item.setType(2);
    item.setImage_item(imageItem);

    SendMessageRequest.Msg msg =
        new SendMessageRequest.Msg(
            toUserId,
            RandomUtils.clientId("ilink-sdk"),
            ctx.getLatestContextToken(),
            Arrays.asList(item));

    apiClient.post(
        loginContext,
        "/ilink/bot/sendmessage",
        new SendMessageRequest(msg, new BaseInfo(config.getChannelVersion())),
        ApiResponse.class);
  }
}
