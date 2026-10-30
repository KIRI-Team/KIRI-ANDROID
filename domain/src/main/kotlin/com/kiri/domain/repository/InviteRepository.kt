package com.kiri.domain.repository

import com.kiri.domain.model.invite.Invite

// 마이페이지(공용 초대 링크)가 쓰는 내 가족 공간의 초대 저장소
// 온보딩에서 가족 공간을 만들 때 받은 초대는 FamilyRepository.createFamily 가 함께 돌려준다
interface InviteRepository {
    // 가족 초대 코드와 링크를 새로 발급한다. 발급하면 이전 초대 대신 이 초대가 지금 초대가 된다
    suspend fun createInvite(): Result<Invite>

    // 내 가족 공간의 지금 초대를 서버에서 불러온다
    // 이 기기에 가족 공간 정보가 없으면 NoFamilyException 으로 실패한다
    suspend fun getCurrentInvite(): Result<Invite>
}
