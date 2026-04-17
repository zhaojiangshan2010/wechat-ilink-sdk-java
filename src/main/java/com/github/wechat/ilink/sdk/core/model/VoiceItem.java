package com.github.wechat.ilink.sdk.core.model;

public class VoiceItem {
  private CDNMedia media;
  private Integer encode_type;
  private Integer playtime;
  private Integer sample_rate;

  public CDNMedia getMedia() {
    return media;
  }

  public void setMedia(CDNMedia v) {
    media = v;
  }

  public Integer getEncode_type() {
    return encode_type;
  }

  public void setEncode_type(Integer v) {
    encode_type = v;
  }

  public Integer getPlaytime() {
    return playtime;
  }

  public void setPlaytime(Integer v) {
    playtime = v;
  }

  public Integer getSample_rate() {
    return sample_rate;
  }

  public void setSample_rate(Integer v) {
    sample_rate = v;
  }
}
