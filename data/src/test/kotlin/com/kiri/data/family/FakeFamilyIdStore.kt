package com.kiri.data.family

// 메모리에만 가족 ID를 두는 보관소
internal class FakeFamilyIdStore(var saved: Long? = null) : FamilyIdStore {
    override suspend fun save(familyId: Long) {
        saved = familyId
    }

    override suspend fun get(): Long? = saved
}
