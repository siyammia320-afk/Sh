package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.TrafficManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Traffic Switch", appName)
    }

    @Test
    fun `traffic manager format speed`() {
        assertEquals("0 B/s", TrafficManager.formatSpeed(0))
        assertEquals("1.0 KB/s", TrafficManager.formatSpeed(1024))
    }
}
