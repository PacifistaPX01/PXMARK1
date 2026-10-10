package com.josue.systeme30

import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews

class QuestWidget : AppWidgetProvider() {

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { update(ctx, mgr, it) }
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        super.onReceive(ctx, intent)
        if (intent.action == ACTION_TOGGLE) {
            val i = intent.getIntExtra("i", -1)
            val q = Store.quests(ctx).getOrNull(i)
            val ti = Store.dayIndex(ctx)
            if (q != null && ti in 0..29) Store.toggle(ctx, Store.today(), q.id)
            refresh(ctx)
        }
    }

    companion object {
        const val ACTION_TOGGLE = "com.josue.systeme30.TOGGLE"
        private val ROWS = intArrayOf(R.id.r0, R.id.r1, R.id.r2, R.id.r3, R.id.r4)

        fun refresh(ctx: Context) {
            val (da, dt) = Store.count(ctx, Store.today())
            if (dt > 0 && da >= dt) {
                (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(1)
            }
            val m = AppWidgetManager.getInstance(ctx)
            val ids = m.getAppWidgetIds(ComponentName(ctx, QuestWidget::class.java))
            ids.forEach { update(ctx, m, it) }
        }

        fun update(ctx: Context, m: AppWidgetManager, id: Int) {
            val v = RemoteViews(ctx.packageName, R.layout.widget)
            val quests = Store.quests(ctx)
            val ti = Store.dayIndex(ctx)
            val today = Store.today()
            val (a, t) = Store.count(ctx, today)

            val title = if (ti > 29) "[ MOIS TERMINÉ ]" else "[ JOUR ${maxOf(ti + 1, 1)}/30 ]  $a/$t  ·  série ${Store.streak(ctx)}"
            v.setTextViewText(R.id.title, title)
            val h = Theme.hue(ctx)
            v.setTextColor(R.id.title, Theme.c(h, 0.7f, 1f))

            val open = PendingIntent.getActivity(
                ctx, 100, Intent(ctx, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            v.setOnClickPendingIntent(R.id.title, open)

            for (k in ROWS.indices) {
                val q = quests.getOrNull(k)
                if (q == null) {
                    v.setViewVisibility(ROWS[k], View.GONE)
                } else {
                    val on = Store.isDone(ctx, today, q.id)
                    v.setViewVisibility(ROWS[k], View.VISIBLE)
                    v.setTextViewText(ROWS[k], (if (on) "☑  " else "☐  ") + q.name)
                    v.setTextColor(ROWS[k], if (on) Theme.c(h, 0.55f, 1f) else Theme.c(h, 0.12f, 1f))
                    val i = Intent(ctx, QuestWidget::class.java).setAction(ACTION_TOGGLE).putExtra("i", k)
                    val pi = PendingIntent.getBroadcast(
                        ctx, 200 + k, i,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    v.setOnClickPendingIntent(ROWS[k], pi)
                }
            }
            if (quests.size > ROWS.size) {
                v.setViewVisibility(R.id.more, View.VISIBLE)
                v.setTextViewText(R.id.more, "+${quests.size - ROWS.size} autres dans l'appli")
            } else {
                v.setViewVisibility(R.id.more, View.GONE)
            }
            m.updateAppWidget(id, v)
        }
    }
}
