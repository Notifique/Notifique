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

  @Ignore Notifique(String message) {
    this(0, message);
  }

  Notifique(long id, String message) {
    this.id = id;
    this.message = message;
  }
}
