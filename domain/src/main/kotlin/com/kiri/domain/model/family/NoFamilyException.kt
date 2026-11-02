package com.kiri.domain.model.family

// 이 기기에 내 가족 공간 정보(familyId)가 없다 (예: 코드로 참여했거나 앱을 다시 설치한 경우)
class NoFamilyException : Exception("가족 공간 정보가 없습니다.")
