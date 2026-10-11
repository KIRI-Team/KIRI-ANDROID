package com.kiri.feature.mypage

import com.kiri.feature.mypage.model.common.MypageProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

// TODO: 사용자 정보 저장소가 생기면 실제 프로필의 닉네임으로 교체
@HiltViewModel
class NicknameEditViewModel @Inject constructor() : NameEditViewModel(MypageProfile.Sample.nickname)
