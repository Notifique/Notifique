package com.nathanrassi.notifique

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class NotificationStoreTest {
  @Test fun returnsFalseForAddingDuplicateNotification() {
    val store = NotificationStore()
    assertThat(store.addNotificationId("com.example.package", 1)).isTrue()
    assertThat(store.addNotificationId("com.example.package", 1)).isFalse()
  }

  @Test fun removeStopsTrackingNotifiaction() {
    val store = NotificationStore()
    store.addNotificationId("com.example.package", 1)
    store.removeNotificationId("com.example.package", 2)
    assertThat(store.addNotificationId("com.example.package", 1)).isFalse()
    store.removeNotificationId("com.example.package", 1)
    assertThat(store.addNotificationId("com.example.package", 1)).isTrue()
  }
}
