package com.android.sample

import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MainActivityTest {

  @Test
  fun activityInitializesWithoutCrashing() {
    val controller = Robolectric.buildActivity(MainActivity::class.java)
    val activity = controller.create().start().resume().get()
    assertNotNull(activity)
    controller.pause().stop().destroy()
  }
}
