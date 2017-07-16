package com.nathanrassi.notifique;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity
public class Notification {

  @PrimaryKey
  private long uid;

  @ColumnInfo(name = "title")
  private String title;

  @ColumnInfo(name = "content")
  private String content;


  //getters & setters
  public long getUid() {
    return uid;
  }

  public void setUid(long id) {
    this.uid = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String ttl) {
    this.title = ttl;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String cntnt) {
    this.content = cntnt;
  }

}

