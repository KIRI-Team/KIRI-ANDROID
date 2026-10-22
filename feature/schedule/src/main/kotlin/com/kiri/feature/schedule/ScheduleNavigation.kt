package com.kiri.feature.schedule

import android.net.Uri
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.kiri.feature.record.presentation.RecordReviewRoute

fun scheduleRoute(id: String, namespace: String) = "$namespace/schedule/detail/${Uri.encode(id)}"
fun scheduleCreateRoute(bucketId: String, namespace: String) = "$namespace/schedule/create/${Uri.encode(bucketId)}"
fun NavGraphBuilder.scheduleGraph(navController: NavHostController, namespace: String) {
    composable("$namespace/schedule/create/{bucketId}") {
        ScheduleRoute({ navController.popBackStack() }, { navController.navigate("$namespace/schedule/review/${Uri.encode(it)}") })
    }
    composable("$namespace/schedule/detail/{scheduleId}") {
        ScheduleRoute({ navController.popBackStack() }, { navController.navigate("$namespace/schedule/review/${Uri.encode(it)}") })
    }
    composable("$namespace/schedule/review/{entryId}") { RecordReviewRoute({ navController.popBackStack() }) }
}
