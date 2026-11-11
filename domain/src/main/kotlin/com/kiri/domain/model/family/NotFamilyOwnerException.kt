package com.kiri.domain.model.family

// 방장이 아니라서 가족명을 바꿀 수 없다 (서버는 FAMILY403_1 로 거절한다)
class NotFamilyOwnerException : Exception("방장만 가족명을 바꿀 수 있습니다.")
