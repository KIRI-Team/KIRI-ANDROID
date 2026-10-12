package com.kiri.app.navigation

import androidx.test.platform.app.InstrumentationRegistry
import com.kiri.data.home.LocalHomeTutorialRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class HomeTutorialPersistenceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val user = "tutorial-test-${UUID.randomUUID()}"
    @After fun cleanUp() {
        context.getSharedPreferences("home_tutorial", 0).edit().remove("visited:$user").remove("visited:$user-other").commit()
    }
    @Test fun firstVisitPersistsAcrossRepositoryRecreationAndIsScopedToUser() = runBlocking {
        assertTrue(LocalHomeTutorialRepository(context).claimFirstVisit(user))
        assertFalse(LocalHomeTutorialRepository(context).claimFirstVisit(user))
        assertTrue(LocalHomeTutorialRepository(context).claimFirstVisit("$user-other"))
    }
}
