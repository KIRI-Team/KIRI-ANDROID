package com.kiri.domain.repository

// 초대 링크로 앱이 열렸을 때 받은 코드를 수락 전까지 보관한다
interface PendingInviteRepository {
    fun save(code: String)

    // 보관한 코드를 꺼내고 비운다
    fun consume(): String?
}
