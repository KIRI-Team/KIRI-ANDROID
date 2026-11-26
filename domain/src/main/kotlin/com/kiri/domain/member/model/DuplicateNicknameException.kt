package com.kiri.domain.member.model

// 다른 회원이 이미 쓰고 있는 닉네임이다
class DuplicateNicknameException : Exception("이미 사용 중인 닉네임입니다.")
