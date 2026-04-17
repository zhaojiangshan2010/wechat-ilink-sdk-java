package com.github.wechat.ilink.sdk.core.model;

public class ImageItem {
  private CDNMedia media;
  private String aeskey;
  private Long mid_size;

  public CDNMedia getMedia() {
    return media;
  }

  public void setMedia(CDNMedia v) {
    media = v;
  }

  public String getAeskey() {
    return aeskey;
  }

  public void setAeskey(String v) {
    aeskey = v;
  }

  public Long getMid_size() {
    return mid_size;
  }

  public void setMid_size(Long v) {
    mid_size = v;
  }
}
