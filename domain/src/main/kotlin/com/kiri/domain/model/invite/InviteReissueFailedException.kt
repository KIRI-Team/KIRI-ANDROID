package com.kiri.domain.model.invite

// 리셋하면서 지금 초대는 지웠지만 새 초대를 받지 못했다. 이전 초대 코드·링크는 더 이상 쓸 수 없다
class InviteReissueFailedException(cause: Throwable) : Exception("초대 코드를 재발급하지 못했습니다.", cause)
