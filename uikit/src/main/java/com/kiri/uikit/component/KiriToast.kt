package com.kiri.uikit.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kiri.uikit.KiriTypography
import com.kiri.uikit.R
import com.kiri.uikit.color.KiriColor

enum class KiriToastIcon { Send, Poke, Check }

/** Figma 8793:9376. 노출 시간과 메시지는 호출 화면이 관리한다. */
@Composable
fun KiriToast(
    message: String,
    modifier: Modifier = Modifier,
    icon: KiriToastIcon? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Surface(modifier.semantics { liveRegion = LiveRegionMode.Polite },
        color = KiriColor.Bg.Secondary, shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.heightIn(min = 59.dp).padding(start = 28.dp, end = 28.dp, top = 18.dp, bottom = 17.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            icon?.let {
                Image(painterResource(when (it) {
                    KiriToastIcon.Send -> R.drawable.kiri_toast_send
                    KiriToastIcon.Poke -> R.drawable.kiri_toast_tap
                    KiriToastIcon.Check -> R.drawable.kiri_toast_check
                }), null, Modifier.size(24.dp))
            }
            Text(message, Modifier.weight(1f), style = KiriTypography.TitleLarge, color = KiriColor.Text.Strong)
            if (actionLabel != null && onAction != null) {
                Text(actionLabel, Modifier.clickable(role = Role.Button, onClick = onAction),
                    style = KiriTypography.BodyMedium, color = KiriColor.Text.Accent)
            }
        }
    }
}
