package com.kiri.feature.mypage.presentation.license

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiri.feature.mypage.OpenLicenseViewModel
import com.kiri.feature.mypage.R
import com.kiri.feature.mypage.model.license.OpenLicense
import com.kiri.feature.mypage.model.license.OpenLicenseUiEffect
import com.kiri.feature.mypage.model.license.OpenLicenseUiEvent
import com.kiri.feature.mypage.model.license.OpenLicenseUiState
import com.kiri.feature.mypage.presentation.common.MypageDialog
import com.kiri.feature.mypage.presentation.common.MypageDialogButton
import com.kiri.feature.mypage.presentation.common.MypageDialogButtonTone
import com.kiri.feature.mypage.presentation.common.lastUpdatedStyle
import com.kiri.uikit.color.KiriColor
import com.kiri.uikit.component.KiriTopBar

@Composable
fun OpenLicenseRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OpenLicenseViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnNavigateBack by rememberUpdatedState(onNavigateBack)

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                OpenLicenseUiEffect.NavigateBack -> currentOnNavigateBack()
            }
        }
    }

    OpenLicenseScreen(modifier = modifier, state = state, onEvent = viewModel::onEvent)
}

// 오픈라이선스. 라이브러리를 누르면 라이선스 전문을 팝업으로 보여준다
@Composable
fun OpenLicenseScreen(
    state: OpenLicenseUiState,
    onEvent: (OpenLicenseUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(KiriColor.Bg.Base)
            .testTag("open_license_screen"),
    ) {
        KiriTopBar(onBackClick = { onEvent(OpenLicenseUiEvent.BackClicked) }, title = "오픈라이선스")
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "HALO 는 오픈소스 라이브러리를 사용합니다.\n각 라이브러리의 라이선스 전문은 아래에서 확인하실 수 있습니다.",
                textAlign = TextAlign.Center,
                style = TextStyle(
                    color = KiriColor.Text.Tertiary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 17.sp,
                    letterSpacing = (-0.04).em,
                ),
            )
            Spacer(Modifier.height(18.dp))
            state.licenses.forEachIndexed { index, license ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier
                            .padding(vertical = 5.dp, horizontal = 21.dp),
                        thickness = 1.dp,
                        color = KiriColor.Divider,
                    )
                }
                LicenseRow(
                    license = license,
                    onClick = { onEvent(OpenLicenseUiEvent.LicenseClicked(license)) },
                    testTag = "open_license_item_$index",
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = state.lastUpdatedText,
                modifier = Modifier.align(Alignment.End),
                style = lastUpdatedStyle,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
    state.viewingLicense?.let { license ->
        LicenseDialog(license = license, onClose = { onEvent(OpenLicenseUiEvent.LicenseDialogClosed) })
    }
}

@Composable
private fun LicenseRow(
    license: OpenLicense,
    onClick: () -> Unit,
    testTag: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 18.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Figma 의 비어 있는 36dp 자리 (라이브러리 아이콘 자리)
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(KiriColor.Bg.Primary, RoundedCornerShape(8.dp)),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = license.name,
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        color = KiriColor.Text.Primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 1.45.em,
                        letterSpacing = (-0.01).em,
                    ),
                )
                Text(text = license.version, maxLines = 1, style = captionStyle)
            }
            Text(text = license.description, maxLines = 1, overflow = TextOverflow.Ellipsis, style = captionStyle)
        }
        // Figma 원본 그대로 왼쪽을 향하는 아이콘을 좌우 반전해서 오른쪽 화살표로 보여준다
        Image(
            painter = painterResource(R.drawable.ic_mypage_chevron_24),
            contentDescription = null,
            modifier = Modifier.scale(scaleX = -1f, scaleY = 1f),
        )
    }
}

@Composable
private fun LicenseDialog(
    license: OpenLicense,
    onClose: () -> Unit,
) {
    MypageDialog(
        title = license.name,
        subtitle = "${license.version} · ${license.licenseName}",
        onDismiss = onClose,
        testTag = "open_license_dialog",
        buttons = {
            MypageDialogButton(
                text = "닫기",
                onClick = onClose,
                tone = MypageDialogButtonTone.Accent,
                testTag = "open_license_dialog_close",
            )
        },
    ) {
        // 라이선스 전문이 길면 팝업이 화면을 넘지 않도록 본문만 스크롤한다
        Text(
            text = license.notice,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 320.dp)
                .verticalScroll(rememberScrollState()),
            style = TextStyle(
                color = KiriColor.Text.Secondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 18.sp,
                letterSpacing = (-0.02).em,
            ),
        )
    }
}

private val captionStyle = TextStyle(
    color = KiriColor.Text.Tertiary,
    fontSize = 12.sp,
    fontWeight = FontWeight.Normal,
    lineHeight = 1.45.em,
    letterSpacing = (-0.01).em,
)

@Preview(showSystemUi = true)
@Composable
private fun OpenLicenseScreenPreview() {
    OpenLicenseScreen(state = OpenLicenseUiState(), onEvent = {})
}
