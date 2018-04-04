package com.nathanrassi.notifique

import android.arch.paging.DataSource
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.Insert
import android.arch.persistence.room.PrimaryKey
import android.arch.persistence.room.Query

@Entity
internal class Notifique(
  @field:PrimaryKey(autoGenerate = true) val id: Long,
  val message: String,
  val title: String,
  val notifPackage: String,
  val timestamp: Long?
) {
  @Ignore constructor(
    message: String,
    title: String,
    notifPackage: String,
    timestamp: Long?
  ) : this(0, message, title, notifPackage, timestamp)

  @android.arch.persistence.room.Dao
  interface Dao {
    @Query("SELECT * FROM notifique ORDER BY timestamp DESC, id DESC")
    fun sourceFactory(): DataSource.Factory<Int, Notifique>

    @Insert fun insert(notifique: Notifique)
  }
}
