package com.nathanrassi.notifique;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.Query;
import java.util.List;

@Entity final class Notifique {
  @android.arch.persistence.room.Dao public interface Dao {
    @Query("SELECT * FROM notifique") List<Notifique> getAll();

    @Insert void insert(Notifique notifique);
  }

  @PrimaryKey(autoGenerate = true) final long id;
  final String message;
  final String title;
  final String notifPackage;
  final long timestamp;

  @Ignore Notifique(String message, String title, String notifPackage, Long timestamp) {
    this(0, message, title, notifPackage, timestamp);
  }

  Notifique(long id, String message, String title, String notifPackage, Long timestamp) {
    this.id = id;
    this.message = message;
    this.title = title;
    this.notifPackage = notifPackage;
    this.timestamp = timestamp;
  }
}
