package com.kiri.feature.mypage

import com.kiri.domain.model.taste.MyTaste
import com.kiri.domain.model.taste.Taste
import com.kiri.domain.repository.TasteRepository

// 불러오기·저장 결과를 정해두고 저장한 취향을 모은다
class TestTasteRepository(
    var loadResult: Result<MyTaste> = Result.success(MyTaste(nickname = "쥬123", taste = null)),
    // 정하면 저장 대신 이 실패를 돌려준다
    var saveError: Throwable? = null,
) : TasteRepository {
    val savedTastes = mutableListOf<Taste>()

    override suspend fun getMyTaste(): Result<MyTaste> = loadResult

    override suspend fun saveMyTaste(taste: Taste): Result<MyTaste> {
        savedTastes += taste
        saveError?.let { return Result.failure(it) }
        return Result.success(MyTaste(nickname = "쥬123", taste = taste))
    }
}
