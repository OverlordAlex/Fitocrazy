package com.itsabugnotafeature.fitocrazy.common

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.itsabugnotafeature.fitocrazy.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class AddSetNotificationManager(
    private val context: Context,
    private val intent: Intent,
) {
    private lateinit var lastNotification: LastNotification

    private data class LastNotification(
        val totalExercises: Int,
        val date: LocalDate,
        val chronometerBase: Long,
        var chronometerRunning: Boolean,
        val exerciseId: Long?,
        val displayName: String?,
        val set: Set?,
        val numPrevSets: Int?,
        val totalSets: Int?,
    )

    fun showNotificationAgain(chronometerBase: Long, chronometerRunning: Boolean = true) {
        if (::lastNotification.isInitialized)
            showNotification(
                lastNotification.totalExercises,
                lastNotification.date,
                chronometerBase,
                chronometerRunning,
                lastNotification.exerciseId,
                lastNotification.displayName,
                lastNotification.set,
                lastNotification.numPrevSets,
                lastNotification.totalSets,
            )
    }

    fun showNotificationAgainWithSetInc(chronometerBase: Long, chronometerRunning: Boolean = true) {
        if (::lastNotification.isInitialized)
            showNotification(
                lastNotification.totalExercises,
                lastNotification.date,
                chronometerBase,
                chronometerRunning,
                lastNotification.exerciseId,
                lastNotification.displayName,
                lastNotification.set,
                lastNotification.numPrevSets?.plus(1),
                lastNotification.totalSets?.plus(1),
            )
    }

    fun showNotification(
        totalExercises: Int,
        date: LocalDate,
        chronometerBase: Long,
        chronometerRunning: Boolean,
        exerciseId: Long?,
        displayName: String?,
        set: Set?,
        numPrevSets: Int?,
        totalSets: Int?,
    ) {
        if (date != LocalDate.now()) return
        lastNotification = LastNotification(
            totalExercises,
            date,
            chronometerBase,
            chronometerRunning,
            exerciseId,
            displayName,
            set,
            numPrevSets,
            totalSets
        )

        val title = "${date.format(DateTimeFormatter.ofPattern("dd LLLL yyyy"))} [$totalExercises]"

        val notificationLayout = RemoteViews("com.itsabugnotafeature.fitocrazy", R.layout.notification_small)
        notificationLayout.setTextViewText(R.id.label_notificationTitle, title)
        notificationLayout.setChronometer(R.id.timer_notificationSetTime, chronometerBase, null, chronometerRunning)

        val notificationLayoutExpanded = RemoteViews("com.itsabugnotafeature.fitocrazy", R.layout.notification_large)
        notificationLayoutExpanded.setTextViewText(R.id.label_notificationTitle, title)
        notificationLayoutExpanded.setChronometer(
            /* viewId = */ R.id.timer_notificationSetTime,
            /* base = */ chronometerBase,
            /* format = */ null,
            /* started = */ chronometerRunning
        )
        notificationLayoutExpanded.setTextViewText(
            R.id.label_notificationContent,
            if (numPrevSets != null) "[$numPrevSets/$totalSets] $displayName" else (displayName ?: "")
        )
        notificationLayoutExpanded.setTextViewText(
            R.id.label_notificationLastSet,
            if (set != null) "${Converters.formatDoubleWeight(set.weight)}kg x ${set.reps}" else ""
        )

        val contentIntent: PendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE //or PendingIntent.FLAG_UPDATE_CURRENT
            )

        val customNotification = NotificationCompat.Builder(context, CHANNEL_ID).setContentTitle(title)
            .setSmallIcon(R.drawable.fitocrazy_logo) // TODO get this to work
            .setStyle(NotificationCompat.DecoratedCustomViewStyle()).setCustomContentView(notificationLayout)
            .setCustomBigContentView(notificationLayoutExpanded).setContentIntent(contentIntent)
            .setCategory(NotificationCompat.CATEGORY_STATUS).setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (set != null) {
            val addAnotherSetIntent = Intent().apply {
                action = NOTIFICATION_ACTION_COMPLETE_SET
                putExtra("exerciseId", exerciseId)
            }
            val addAnotherSetPendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                addAnotherSetIntent,
                PendingIntent.FLAG_IMMUTABLE //or PendingIntent.FLAG_UPDATE_CURRENT
            )

            customNotification.addAction(
                android.R.drawable.ic_menu_add, NOTIFICATION_ACTION_COMPLETE_SET, addAnotherSetPendingIntent
            )
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, customNotification.build())
    }

    companion object {
        const val NOTIFICATION_ID = 123456
        const val NOTIFICATION_ACTION_COMPLETE_SET = "same again"
        const val CHANNEL_ID = "FitocrazyCurrentExerciseChannel"
    }
}