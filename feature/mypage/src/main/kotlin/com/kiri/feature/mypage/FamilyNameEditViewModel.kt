package com.kiri.feature.mypage

import com.kiri.feature.mypage.model.common.MypageProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

// TODO: 가족 정보 저장소가 생기면 실제 가족명으로 교체
@HiltViewModel
class FamilyNameEditViewModel @Inject constructor() : NameEditViewModel(MypageProfile.Sample.familyName)
