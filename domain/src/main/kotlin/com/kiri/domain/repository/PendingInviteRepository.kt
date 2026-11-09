package com.kiri.domain.repository

// 초대 링크로 앱이 열렸을 때 받은 초대 토큰을 가족 참여가 끝날 때까지 기기에 보관한다.
// 링크를 누른 뒤 로그인 -> 온보딩을 거쳐 홈에 들어가는 동안 앱이 종료돼도 남아 있어야 한다
interface PendingInviteRepository {
    // 새 링크를 누르면 이전 토큰을 덮어쓴다
    suspend fun save(token: String)

    // 보관한 토큰 (없으면 null). 꺼내도 지우지 않는다
    suspend fun get(): String?

    // 가족 참여에 성공했거나 토큰을 더 쓸 수 없을 때(만료·이미 가족 있음 등) 지운다
    suspend fun clear()
}
