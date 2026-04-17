package com.github.wechat.ilink.sdk.core.model;

public class GetUploadUrlRequest {
  private String filekey;
  private Integer media_type;
  private String to_user_id;
  private Long rawsize;
  private String rawfilemd5;
  private Long filesize;
  private Boolean no_need_thumb;
  private String aeskey;
  private BaseInfo base_info;

  public GetUploadUrlRequest(
      String filekey,
      Integer mediaType,
      String toUserId,
      Long rawsize,
      String rawfilemd5,
      Long filesize,
      Boolean noNeedThumb,
      String aeskey,
      BaseInfo info) {
    this.filekey = filekey;
    this.media_type = mediaType;
    this.to_user_id = toUserId;
    this.rawsize = rawsize;
    this.rawfilemd5 = rawfilemd5;
    this.filesize = filesize;
    this.no_need_thumb = noNeedThumb;
    this.aeskey = aeskey;
    this.base_info = info;
  }

  public String getFilekey() {
    return filekey;
  }

  public Integer getMedia_type() {
    return media_type;
  }

  public String getTo_user_id() {
    return to_user_id;
  }

  public Long getRawsize() {
    return rawsize;
  }

  public String getRawfilemd5() {
    return rawfilemd5;
  }

  public Long getFilesize() {
    return filesize;
  }

  public Boolean getNo_need_thumb() {
    return no_need_thumb;
  }

  public String getAeskey() {
    return aeskey;
  }

  public BaseInfo getBase_info() {
    return base_info;
  }
}
