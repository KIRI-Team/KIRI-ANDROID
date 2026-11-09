package com.kiri.data.auth

import android.content.Context
import androidx.datastore.preferences.core.*
import com.kiri.data.family.FamilyApi
import com.kiri.data.family.FamilyIdStore
import com.kiri.data.term.TermApi
import com.kiri.data.network.*
import com.kiri.domain.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import retrofit2.Retrofit
import javax.inject.Singleton

internal class SessionRepositoryImpl(private val terms: TermApi, private val families: FamilyApi,
    private val tokens: TokenStore, private val familyIds: FamilyIdStore, private val json: Json,
    private val store: androidx.datastore.core.DataStore<Preferences>) : SessionRepository {
    override suspend fun restore(): Result<SessionDestination> {
        if (tokens.get() == null) return Result.success(SessionDestination.LOGIN)
        val agreed = apiResult(json) { terms.agreements() }.getOrElse { error ->
            return if (tokens.get() == null || (error as? KiriApiException)?.code in setOf("COMMON401_1", "AUTH401_1", "AUTH401_2", "MEMBER404_1")) {
                tokens.clear(); Result.success(SessionDestination.LOGIN)
            } else Result.failure(error)
        }
        if (!agreed.termsAgreed) return Result.success(SessionDestination.TERMS)
        val prefs = store.data.first()
        val family = apiResult(json) { families.getMyFamilyId() }
        family.onSuccess { familyIds.save(it.familyId) }
        if (family.isFailure && (tokens.get() == null || (family.exceptionOrNull() as? KiriApiException)?.code in setOf("COMMON401_1", "AUTH401_1", "AUTH401_2", "MEMBER404_1"))) {
            tokens.clear(); return Result.success(SessionDestination.LOGIN)
        }
        if (family.isFailure && (family.exceptionOrNull() as? KiriApiException)?.code != "FAMILY404_1")
            return Result.failure(requireNotNull(family.exceptionOrNull()))
        // 이 기기에서 중단한 필수 프로필 입력은 가족 소속 여부보다 먼저 복구한다.
        val partial = prefs[NICKNAME] != null || prefs[ROLE] != null
        val incomplete = partial && (prefs[NICKNAME] == null || prefs[ROLE] == null)
        return Result.success(if (incomplete) SessionDestination.ONBOARDING
            else if (family.isSuccess || prefs[COMPLETE] == true) SessionDestination.MAIN else SessionDestination.ONBOARDING)
    }
    override suspend fun profile(): ProfileProgress {
        val prefs = store.data.first(); return ProfileProgress(prefs[NICKNAME], prefs[ROLE])
    }
    override suspend fun saveNickname(value: String) { store.edit { it[NICKNAME] = value } }
    override suspend fun saveRole(value: String) { store.edit { it[ROLE] = value } }
    override suspend fun completeOnboarding() { store.edit { it[COMPLETE] = true } }
    private companion object {
        val NICKNAME = stringPreferencesKey("onboarding_nickname")
        val ROLE = stringPreferencesKey("onboarding_role")
        val COMPLETE = booleanPreferencesKey("onboarding_complete")
    }
}
@Module @InstallIn(SingletonComponent::class)
object SessionModule {
    @Provides @Singleton internal fun session(@ApplicationContext context: Context, retrofit: Retrofit,
        tokens: TokenStore, ids: FamilyIdStore, json: Json): SessionRepository =
        SessionRepositoryImpl(retrofit.create(TermApi::class.java), retrofit.create(FamilyApi::class.java), tokens, ids, json, context.authDataStore)
}
