package com.nathanrassi.notifique

import androidx.room.RoomDatabase

@androidx.room.Database(
    entities = [Notifique::class],
    version = 1,
    exportSchema = false
)
internal abstract class Database : RoomDatabase() {
  abstract fun notifiqueDao(): Notifique.Dao
}
