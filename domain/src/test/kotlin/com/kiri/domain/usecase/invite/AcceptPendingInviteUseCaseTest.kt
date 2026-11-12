package com.kiri.domain.usecase.invite

import com.kiri.domain.model.family.AlreadyInFamilyException
import com.kiri.domain.model.family.ExpiredInviteCodeException
import com.kiri.domain.model.family.Family
import com.kiri.domain.model.family.InvalidInviteCodeException
import com.kiri.domain.model.family.JoinedFamily
import com.kiri.domain.repository.FamilyRepository
import com.kiri.domain.repository.PendingInviteRepository
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AcceptPendingInviteUseCaseTest {

    private class Pending(var token: String?) : PendingInviteRepository {
        override suspend fun save(token: String) { this.token = token }
        override suspend fun get(): String? = token
        override suspend fun clear() { token = null }
    }

    private class Families(var result: Result<JoinedFamily>) : FamilyRepository {
        val sentTokens = mutableListOf<String>()
        override suspend fun joinFamilyByToken(inviteToken: String): Result<JoinedFamily> {
            sentTokens += inviteToken
            return result
        }
        override suspend fun createFamily(name: String): Result<Family> = throw UnsupportedOperationException()
        override suspend fun renameFamily(name: String): Result<String> = throw UnsupportedOperationException()
    }

    private val joined = JoinedFamily(1, "끼리네가족")

    @Test
    fun 받아_둔_초대가_없으면_참여를_요청하지_않는다() = runBlocking {
        val families = Families(Result.success(joined))

        val result = AcceptPendingInviteUseCase(Pending(null), families)()

        assertEquals(InviteAcceptResult.NoInvite, result)
        assertNull(result.message)
        assertTrue(families.sentTokens.isEmpty())
    }

    @Test
    fun 토큰으로_참여하면_토큰을_지우고_참여한_가족을_알린다() = runBlocking {
        val pending = Pending("tok-123")
        val families = Families(Result.success(joined))

        val result = AcceptPendingInviteUseCase(pending, families)()

        assertEquals(InviteAcceptResult.Joined(joined), result)
        assertEquals(listOf("tok-123"), families.sentTokens)
        assertNull(pending.token)
        assertEquals("끼리네가족에 참여했어요.", result.message)
    }

    @Test
    fun 다시_써도_소용없는_토큰이면_지운다() = runBlocking {
        listOf(ExpiredInviteCodeException(), InvalidInviteCodeException(), AlreadyInFamilyException(), Exception("COMMON500_1"))
            .forEach { error ->
                val pending = Pending("tok-123")

                val result = AcceptPendingInviteUseCase(pending, Families(Result.failure(error)))()

                assertNull(error.toString(), pending.token)
                assertFalse((result as InviteAcceptResult.Failed).willRetry)
            }
    }

    @Test
    fun 연결_문제면_토큰을_남겨_다음에_다시_시도한다() = runBlocking {
        val pending = Pending("tok-123")

        val result = AcceptPendingInviteUseCase(pending, Families(Result.failure(IOException())))()

        assertEquals("tok-123", pending.token)
        assertTrue((result as InviteAcceptResult.Failed).willRetry)
    }

    @Test
    fun 실패_이유마다_안내가_다르다() {
        assertEquals("만료된 초대 링크예요. 가족에게 새 링크를 받아주세요.", InviteAcceptResult.Failed(ExpiredInviteCodeException()).message)
        assertEquals("이미 다른 가족에 가입되어 있어 초대 링크로 참여할 수 없어요.", InviteAcceptResult.Failed(AlreadyInFamilyException()).message)
    }
}
