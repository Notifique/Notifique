package com.nathanrassi.notifique

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.fail
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

  @Test fun ignoresRemovingFromUntrackedPackages() {
    val store = NotificationStore()
    store.removeNotificationId("com.example.package", 1)
  }

  @Test fun disallowsRemovingWithoutFirstAddingIfPackageIsAlreadyTracked() {
    val store = NotificationStore()
    store.addNotificationId("com.example.package", 1) // Tracking the package.
    try {
      store.removeNotificationId("com.example.package", 2)
      fail()
    } catch (expected: IllegalStateException) {
      assertThat(expected).hasMessageThat().isEqualTo(
          "No notification from package com.example.package with id 2 was added.")
    }
  }
}
