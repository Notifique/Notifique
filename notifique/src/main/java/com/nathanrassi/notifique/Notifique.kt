package com.nathanrassi.notifique


import androidx.paging.DataSource
import androidx.room.*


@Entity
internal data class Notifique(
  @field:PrimaryKey(autoGenerate = true) val id: Long,
  val message: String,
  val notifAppName: String,
  val title: String,
  val notifPackage: String,
  val timestamp: Long
) {
  @Ignore constructor(
    message: String,
    appName: String,
    title: String,
    notifPackage: String,
    timestamp: Long
  ) : this(0, message, appName, title, notifPackage, timestamp)

  @androidx.room.Dao
  interface Dao {
    @Query("SELECT * FROM notifique ORDER BY timestamp DESC, id DESC")
    fun sourceFactory(): DataSource.Factory<Int, Notifique>

    @Insert fun insert(notifique: Notifique)
  }
}
