package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.action.OfflineMayraBrain
import com.example.data.model.LanguagePreference
import com.example.data.model.PersonalityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Mayra AI", appName)
  }

  @Test
  fun `offline brain handles time query`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val reply = OfflineMayraBrain.processInput(
        query = "samay kya hua hai",
        context = context,
        userName = "Rahul",
        personality = PersonalityType.CHULBULI_WITTY,
        language = LanguagePreference.HINGLISH
    )
    assertTrue(reply.spokenText.contains("Rahul"))
  }
}
