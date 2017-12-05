package com.nathanrassi.notifique;

import android.arch.paging.TiledDataSource;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.Query;

@Entity final class Notifique {
  @android.arch.persistence.room.Dao public interface Dao {
    @Query("SELECT * FROM notifique ORDER BY timestamp DESC, id DESC")
    TiledDataSource<Notifique> source();

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

  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Notifique)) return false;
    Notifique notifique = (Notifique) o;
    return id == notifique.id
        && timestamp == notifique.timestamp
        && message.equals(notifique.message)
        && title.equals(notifique.title)
        && notifPackage.equals(notifique.notifPackage);
  }

  @Override public int hashCode() {
    int result = (int) (id ^ (id >>> 32));
    result = 31 * result + message.hashCode();
    result = 31 * result + title.hashCode();
    result = 31 * result + notifPackage.hashCode();
    result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
    return result;
  }
}
