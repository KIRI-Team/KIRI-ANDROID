package com.kiri.app

import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint

// UI 테스트에서 원하는 Composable을 Hilt가 적용된 상태로 띄우기 위한 빈 Activity (debug 빌드에만 포함)
@AndroidEntryPoint
class HiltTestActivity : ComponentActivity()
