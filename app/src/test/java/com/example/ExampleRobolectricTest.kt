package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.audio.AsrEngineStatus
import com.example.data.audio.SherpaCaptureController
import org.junit.Assert.assertEquals
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
    assertEquals("双生思辨", appName)
  }

  @Test
  fun `sherpa streaming bilingual zh-en model assets are bundled`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val assetList = context.assets.list("sherpa-onnx-streaming-zipformer-small-bilingual-zh-en")?.toSet().orEmpty()
    assertTrue(assetList.contains("encoder-epoch-99-avg-1.int8.onnx"))
    assertTrue(assetList.contains("decoder-epoch-99-avg-1.onnx"))
    assertTrue(assetList.contains("joiner-epoch-99-avg-1.int8.onnx"))
    assertTrue(assetList.contains("tokens.txt"))

    val controller = SherpaCaptureController(context)
    assertEquals(AsrEngineStatus.IDLE, controller.state.value.engineStatus)
    controller.releaseOnDispose()
  }
}
