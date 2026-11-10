package com.kiri.domain.model.family

// 초대 코드로 가입한 가족 공간
data class JoinedFamily(val id: Long, val name: String)

// 초대 코드가 비었거나 형식이 틀렸거나, 그 코드의 가족을 찾을 수 없다
class InvalidInviteCodeException : Exception("올바르지 않은 초대 코드입니다.")

// 초대 코드가 만료됐다 (FAMILY410_1)
class ExpiredInviteCodeException : Exception("초대 코드가 만료되었습니다.")

// 이미 다른 가족에 가입되어 있다 (FAMILY409_2)
class AlreadyInFamilyException : Exception("이미 다른 가족에 가입되어 있습니다.")
