package com.kiri.data.home

import android.content.Context
import com.kiri.domain.home.HomeTutorialRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalHomeTutorialRepository @Inject constructor(@ApplicationContext context: Context) : HomeTutorialRepository {
    private val preferences = context.getSharedPreferences("home_tutorial", Context.MODE_PRIVATE)
    private val mutex = Mutex()

    override suspend fun claimFirstVisit(userId: String): Boolean = withContext(Dispatchers.IO) {
        require(userId.isNotBlank())
        mutex.withLock {
            val key = "visited:$userId"
            if (preferences.getBoolean(key, false)) false
            else preferences.edit().putBoolean(key, true).commit()
        }
    }
}
