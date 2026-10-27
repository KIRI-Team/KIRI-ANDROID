package com.kiri.data.local

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.*
import okio.Path.Companion.toPath
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DataStoreLocalStateStoreTest {

    @Test
    fun snapshotsAreDurableAcrossDataStoreInstancesAndKeysRemainIsolated() = runBlocking {
        val file =
            File(
                InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
                "activity-${java.util.UUID.randomUUID()}.preferences_pb",
            )
        val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val first =
            DataStoreLocalStateStore(
                PreferenceDataStoreFactory.createWithPath(
                    scope = firstScope,
                    produceFile = { file.absolutePath.toPath() },
                )
            )
        first.write("buckets/account-a", "saved")
        first.write("buckets/account-b", "other")
        firstScope.coroutineContext[Job]!!.cancelAndJoin()
        val secondScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val second =
                DataStoreLocalStateStore(
                    PreferenceDataStoreFactory.createWithPath(
                        scope = secondScope,
                        produceFile = { file.absolutePath.toPath() },
                    )
                )
            assertEquals("saved", second.read("buckets/account-a"))
            assertEquals("other", second.read("buckets/account-b"))
        } finally {
            secondScope.coroutineContext[Job]!!.cancelAndJoin()
            file.delete()
        }
    }
}
