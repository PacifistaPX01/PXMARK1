package com.josue.systeme30

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.util.Calendar

class Reminder : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        QuestWidget.refresh(ctx)
        val ti = Store.dayIndex(ctx)
        if (ti !in 0..29) return
        val hr = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (hr < FIRST_HOUR || hr > LAST_HOUR) return
        val (a, t) = Store.count(ctx, Store.today())
        if (t == 0 || a >= t) return

        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Rappels des quêtes", NotificationManager.IMPORTANCE_HIGH)
        )
        val open = PendingIntent.getActivity(
            ctx, 101, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = Notification.Builder(ctx, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setColor(Theme.c(Theme.hue(ctx), 0.7f, 1f))
            .setContentTitle("[ PX MARK 1 ]")
            .setContentText("${t - a} quête(s) restante(s) aujourd'hui. Ne lâche pas.")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        nm.notify(1, n)
    }

    companion object {
        private const val CHANNEL = "rappels"
        // Rappel toutes les heures, entre 8 h et 22 h, tant que tout n'est pas coché
        private const val FIRST_HOUR = 8
        private const val LAST_HOUR = 22

        fun schedule(ctx: Context) {
            val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pi = PendingIntent.getBroadcast(
                ctx, 0, Intent(ctx, Reminder::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val c = Calendar.getInstance()
            c.add(Calendar.HOUR_OF_DAY, 1)
            c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 0)
            am.setInexactRepeating(AlarmManager.RTC_WAKEUP, c.timeInMillis, AlarmManager.INTERVAL_HOUR, pi)
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Reminder.schedule(ctx)
            QuestWidget.refresh(ctx)
        }
    }
}
