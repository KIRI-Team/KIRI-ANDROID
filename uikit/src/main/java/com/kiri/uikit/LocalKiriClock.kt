package com.kiri.uikit

import androidx.compose.runtime.staticCompositionLocalOf
import java.time.Clock

/** The application provides the same injected clock used by its repositories and ViewModels. */
val LocalKiriClock = staticCompositionLocalOf<Clock> { Clock.systemUTC() }
