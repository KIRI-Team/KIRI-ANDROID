package com.kiri.domain.push.repository

// 푸시 알림을 받을 이 기기를 로그인한 회원에게 등록하거나 지운다
interface PushDeviceRepository {
    // 이 기기의 FCM 토큰을 등록한다. 이미 등록한 기기면 서버가 토큰만 바꾼다
    suspend fun register(fcmToken: String): Result<Unit>

    // 이 기기 등록을 지운다. 로그인 토큰이 있어야 해서 로그아웃·탈퇴 전에 부른다
    suspend fun unregister(): Result<Unit>
}
