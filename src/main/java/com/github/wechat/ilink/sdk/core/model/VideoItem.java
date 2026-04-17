package com.github.wechat.ilink.sdk.core.model;

public class VideoItem {
  private CDNMedia media;
  private Long video_size;
  private Integer play_length;
  private String video_md5;

  public CDNMedia getMedia() {
    return media;
  }

  public void setMedia(CDNMedia v) {
    media = v;
  }

  public Long getVideo_size() {
    return video_size;
  }

  public void setVideo_size(Long v) {
    video_size = v;
  }

  public Integer getPlay_length() {
    return play_length;
  }

  public void setPlay_length(Integer v) {
    play_length = v;
  }

  public String getVideo_md5() {
    return video_md5;
  }

  public void setVideo_md5(String v) {
    video_md5 = v;
  }
}
