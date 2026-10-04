package com.kiri.feature.home.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kiri.feature.home.R

internal val HomeBackground = Color(0xFF1A1A1A)
internal val HomeCard = Color(0xFF2B2B2B)
internal val HomeAccent = Color(0xFF9BEA51)
internal val HomeStrong = Color(0xFFF7F7F7)
internal val HomeSecondary = Color(0xFFB3B3B3)

/** The host already provides system insets. IME padding keeps submission reachable. */
@Composable
internal fun AnswerEditor(
    question: String,
    draft: String,
    busy: Boolean,
    error: String?,
    onChange: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler { if (!busy) onBack() }
    Column(Modifier.fillMaxSize().background(HomeBackground).imePadding().padding(horizontal = 18.dp)) {
        IconButton(onClick = onBack, enabled = !busy, modifier = Modifier.size(52.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로", tint = HomeStrong)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(20.dp))
            Text("Q1.", color = HomeSecondary, fontSize = 14.sp)
            Spacer(Modifier.height(12.dp))
            Text(question, color = HomeStrong, fontSize = 28.sp, lineHeight = 39.sp)
            Spacer(Modifier.height(8.dp))
            Text("오늘의 질문에 답변해주세요!", color = HomeSecondary, fontSize = 16.sp)
            Spacer(Modifier.height(36.dp))
            TextField(
                value = draft, onValueChange = onChange, enabled = !busy,
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "오늘의 질문 답변" },
                placeholder = { Text("답변을 입력하세요.") },
                shape = RoundedCornerShape(18.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = HomeCard, unfocusedContainerColor = HomeCard,
                    disabledContainerColor = HomeCard,
                    focusedTextColor = HomeStrong, unfocusedTextColor = HomeStrong,
                    disabledTextColor = HomeSecondary, cursorColor = HomeAccent,
                    focusedPlaceholderColor = HomeSecondary, unfocusedPlaceholderColor = HomeSecondary,
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
            )
            if (error != null) Text(error, Modifier.padding(top = 12.dp), color = HomeStrong)
            Spacer(Modifier.height(24.dp))
        }
        Button(
            onClick = onSave, enabled = draft.isNotBlank() && !busy,
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).heightIn(min = 54.dp),
            shape = RoundedCornerShape(30.dp),
            colors = ButtonDefaults.buttonColors(containerColor = HomeAccent, contentColor = HomeBackground,
                disabledContainerColor = HomeCard, disabledContentColor = HomeSecondary),
        ) { Text(if (busy) "저장 중…" else "답변 제출", fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
internal fun AnswerSavedDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().background(Color(0xFF242424), RoundedCornerShape(30.dp)).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("답변이 완료됐어요!", color = HomeStrong, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text("다른 가족의 답변을 기다려볼까요?", color = HomeSecondary, fontSize = 16.sp)
            Spacer(Modifier.height(20.dp))
            Image(painterResource(R.drawable.home_answer_success), null, Modifier.size(131.dp))
        }
    }
}

@Preview(name = "답변 입력", widthDp = 390, heightDp = 700)
@Composable
private fun AnswerEditorPreview() {
    AnswerEditor("내가 가장 좋아하는 음식은?", "", false, null, {}, {}, {})
}

@Preview(name = "저장 실패 · 긴 답변", widthDp = 320, heightDp = 600)
@Composable
private fun AnswerErrorPreview() {
    AnswerEditor("내가 가장 좋아하는 음식은?", "가족과 함께 먹었던 음식이 생각나요. ".repeat(10), false,
        "저장하지 못했어요. 다시 시도해주세요.", {}, {}, {})
}
