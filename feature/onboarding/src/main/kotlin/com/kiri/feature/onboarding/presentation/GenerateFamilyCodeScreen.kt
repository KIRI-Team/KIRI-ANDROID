package com.kiri.feature.onboarding.presentation

import android.content.ClipData
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kiri.uikit.component.KiriButtonTone
import com.kiri.feature.onboarding.presentation.component.OnboardingInviteCode
import com.kiri.uikit.component.KiriButton
import com.kiri.uikit.component.KiriTitle
import com.kiri.uikit.component.KiriTopBar
import com.kiri.uikit.color.KiriColor
import kotlinx.coroutines.launch

@Composable
fun GenerateFamilyCodeScreen(
    familyCode: String,
    onCancelClick: () -> Unit,
    onInviteClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val onCopyClick: () -> Unit = {
        scope.launch {
            clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("가족코드", familyCode)))
        }
        // Android 13 이상은 시스템이 복사 알림을 직접 보여준다
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(context, "가족코드를 복사했어요", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier
            .fillMaxSize()
            .background(KiriColor.Bg.Base)
            .systemBarsPadding()
            .testTag("generate_family_code_screen"),
    ) {
        KiriTopBar(onBackClick = onBackClick)
        Column(modifier = Modifier.weight(1f)) {
            Spacer(Modifier.height(40.dp))
            KiriTitle(
                title = "가족코드\n생성이 완료되었어요!",
                subtitle = "복사해서 가족에게 보내주세요",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(59.dp))
            OnboardingInviteCode(
                code = familyCode,
                onCopyClick = onCopyClick,
                modifier = Modifier.padding(horizontal = 39.5.dp),
            )
        }
        Row(
            modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            KiriButton(
                onClick = onCancelClick,
                text = "취소",
                tone = KiriButtonTone.Neutral,
                modifier = Modifier.weight(1f),
            )
            KiriButton(
                onClick = onInviteClick,
                text = "초대하기",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun GenerateFamilyCodeScreenPreview() {
    GenerateFamilyCodeScreen(familyCode = "NWTKYJ", onCancelClick = {}, onInviteClick = {}, onBackClick = {})
}
