package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.database.DatabaseProvider
import com.example.database.WordEntity
import kotlinx.coroutines.runBlocking
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
    assertEquals("Keyboard Hemat", appName)
  }

  @Test
  fun `test room user dictionary insertion and prediction`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val dao = DatabaseProvider.getDatabase(context).wordDao()

    // Insert custom name
    dao.insertWord(
      WordEntity(
        word = "rizky",
        frequency = 15,
        isUserCustom = true,
        timestamp = System.currentTimeMillis()
      )
    )

    // Verify prediction
    val predictions = dao.getPredictions("riz", 3)
    assertTrue(predictions.contains("rizky"))
  }
}

