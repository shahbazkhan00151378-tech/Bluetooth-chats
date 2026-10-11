package com.example

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.example.ble.BleConstants
import com.example.ble.BleHardwareState
import com.example.ble.DiscoveredPeerDevice
import com.example.viewmodel.MeshViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("MeshLink", appName)
  }

  @Test
  fun `create MeshViewModel via AndroidViewModelFactory`() {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val factory = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
    val viewModel = factory.create(MeshViewModel::class.java)
    assertNotNull(viewModel)
    assertNotNull(viewModel.bleManager)
  }

  @Test
  fun `bleConstants verify service UUID and scan timeout`() {
    assertEquals("0000fd42-0000-1000-8000-00805f9b34fb", BleConstants.SERVICE_UUID.toString().lowercase())
    assertEquals("0000fd43-0000-1000-8000-00805f9b34fb", BleConstants.CHAR_MESSAGE_UUID.toString().lowercase())
    assertEquals(20_000L, BleConstants.SCAN_TIMEOUT_MS)
  }

  @Test
  fun `bleManager initial state and safe methods`() {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = MeshViewModel(application)
    assertNotNull(viewModel.bleHardwareState.value)
    assertFalse(viewModel.isBleAdvertising.value)
    assertFalse(viewModel.isBleScanning.value)
    assertTrue(viewModel.discoveredBlePeers.value.isEmpty())
  }
}
