package com.kiri.domain.repository

import com.kiri.domain.model.invite.Invite

// 온보딩(가족 공간 생성)과 마이페이지(공용 초대 링크)가 함께 쓰는 가족 초대 저장소
interface InviteRepository {
    // 가족 초대 코드와 링크를 새로 발급한다. 발급하면 이전 초대 대신 이 초대가 지금 초대가 된다
    suspend fun createInvite(): Result<Invite>

    // 가장 최근에 발급한 초대 (아직 발급하지 않았으면 null)
    suspend fun getCurrentInvite(): Invite?
}
