package com.nathanrassi.notifique;

import android.arch.persistence.room.RoomDatabase;

@android.arch.persistence.room.Database(entities = {
    Notifique.class
}, version = 1, exportSchema = false) abstract class Database extends RoomDatabase {
  abstract Notifique.Dao notifiqueDao();
}
