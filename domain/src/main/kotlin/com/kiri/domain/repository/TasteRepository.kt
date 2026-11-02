package com.kiri.domain.repository

import com.kiri.domain.model.taste.MyTaste
import com.kiri.domain.model.taste.Taste

interface TasteRepository {
    // 내 닉네임과 저장된 취향을 불러온다. 취향을 정한 적이 없으면 MyTaste.taste 가 null
    suspend fun getMyTaste(): Result<MyTaste>

    // 내 취향을 저장하고 저장된 결과를 돌려준다
    suspend fun saveMyTaste(taste: Taste): Result<MyTaste>
}
