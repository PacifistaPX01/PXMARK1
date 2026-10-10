package com.josue.systeme30

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : Activity() {

    private var BG = 0
    private var PANEL = 0
    private var CELL = 0
    private var LINE = 0
    private var LINE2 = 0
    private var GLOW = 0
    private var CYAN = 0
    private var TEXT = 0
    private var DIM = 0
    private var DONE_FILL = 0
    private var SEL_FILL = 0
    private var ALERT_BG = 0
    private val RED = Color.parseColor("#FF3B5C")
    private val GOLD = Color.parseColor("#FFD25A")

    private fun applyTheme() {
        val h = Theme.hue(this)
        BG = Theme.c(h, 0.8f, 0.07f)
        PANEL = Theme.c(h, 0.75f, 0.19f)
        CELL = Theme.c(h, 0.75f, 0.17f)
        LINE = Theme.c(h, 0.82f, 0.94f)
        LINE2 = Theme.c(h, 0.75f, 0.43f)
        GLOW = Theme.c(h, 0.7f, 1f)
        CYAN = Theme.c(h, 0.55f, 1f)
        TEXT = Theme.c(h, 0.12f, 1f)
        DIM = Theme.c(h, 0.35f, 0.70f)
        DONE_FILL = Theme.c(h, 0.8f, 0.43f)
        SEL_FILL = Theme.c(h, 0.8f, 0.63f)
        ALERT_BG = Theme.c(h, 0.8f, 0.29f)
        window.statusBarColor = BG
        window.navigationBarColor = BG
        sv.setBackgroundColor(BG)
    }

    private lateinit var sv: ScrollView
    private lateinit var root: LinearLayout
    private var sel: String? = null
    private var editing = false
    private var resetArm = false

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        sv = ScrollView(this)
        root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(14), dp(14), dp(14), dp(40))
        sv.addView(root)
        setContentView(sv)
        applyTheme()

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        Reminder.schedule(this)
    }

    override fun onResume() {
        super.onResume()
        render()
        QuestWidget.refresh(this)
    }

    // ---------- helpers ----------
    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun box(stroke: Int, fill: Int, w: Int = 1): GradientDrawable {
        val g = GradientDrawable()
        g.setColor(fill)
        g.setStroke(dp(w), stroke)
        return g
    }

    private fun tv(s: String, size: Float, color: Int, bold: Boolean = false): TextView {
        val t = TextView(this)
        t.text = s
        t.textSize = size
        t.setTextColor(color)
        if (bold) t.typeface = Typeface.DEFAULT_BOLD
        return t
    }

    private fun lp(top: Int = 0): LinearLayout.LayoutParams {
        val p = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        p.topMargin = dp(top)
        return p
    }

    private fun panel(): LinearLayout {
        val l = LinearLayout(this)
        l.orientation = LinearLayout.VERTICAL
        l.background = box(LINE, PANEL)
        l.setPadding(dp(14), dp(14), dp(14), dp(14))
        return l
    }

    private fun btn(label: String, color: Int, onClick: () -> Unit): Button {
        val b = Button(this)
        b.text = label
        b.isAllCaps = false
        b.setTextColor(color)
        b.background = box(color, Color.TRANSPARENT)
        b.setOnClickListener { onClick() }
        return b
    }

    private fun alert(title: String, msg: String, color: Int, action: String? = null, onClick: (() -> Unit)? = null) {
        val a = LinearLayout(this)
        a.orientation = LinearLayout.HORIZONTAL
        a.gravity = Gravity.CENTER_VERTICAL
        a.background = box(color, ALERT_BG)
        a.setPadding(dp(14), dp(12), dp(14), dp(12))
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.addView(tv(title, 12f, color, true))
        col.addView(tv(msg, 15f, TEXT))
        a.addView(col, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        if (action != null && onClick != null) a.addView(btn(action, color, onClick))
        root.addView(a, lp(12))
    }

    private fun changed() {
        QuestWidget.refresh(this)
        render()
    }

    // ---------- écran ----------
    private fun render() {
        applyTheme()
        root.removeAllViews()
        val start = Store.start(this)
        val today = Store.today()
        val ti = Store.dayIndex(this)
        if (sel == null) sel = if (ti > 29) Store.addDays(start, 29) else today
        val day = sel!!
        val quests = Store.quests(this)
        val t = quests.size
        val xp = Store.xp(this)
        val level = xp / 100 + 1
        val thr = intArrayOf(1, 3, 6, 10, 14, 18)
        var r = 0
        for (k in thr.indices) if (level >= thr[k]) r = k

        // Héros
        val hero = panel()
        hero.orientation = LinearLayout.HORIZONTAL
        hero.gravity = Gravity.CENTER_VERTICAL
        val rank = tv("EDCBAS"[r].toString(), 34f, CYAN, true)
        rank.gravity = Gravity.CENTER
        rank.background = box(CYAN, Color.TRANSPARENT, 2)
        hero.addView(rank, LinearLayout.LayoutParams(dp(64), dp(64)))
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.addView(tv("Joueur", 17f, TEXT, true))
        col.addView(tv("Niveau $level", 14f, DIM))
        val pb = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
        pb.max = 100
        pb.progress = xp % 100
        pb.progressTintList = ColorStateList.valueOf(CYAN)
        pb.progressBackgroundTintList = ColorStateList.valueOf(LINE2)
        val pbp = LinearLayout.LayoutParams(MATCH_PARENT, dp(10))
        pbp.topMargin = dp(6)
        col.addView(pb, pbp)
        col.addView(tv("${xp % 100} / 100 XP", 12f, DIM))
        val cp = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        cp.leftMargin = dp(14)
        hero.addView(col, cp)
        root.addView(hero, lp(0))

        // Alertes
        var missedFirst: String? = null
        var missedQ = 0
        var missedD = 0
        for (i in 0 until minOf(ti, 30)) {
            val d = Store.addDays(start, i)
            val (a, tt) = Store.count(this, d)
            if (tt > 0 && a < tt) {
                if (missedFirst == null) missedFirst = d
                missedQ += tt - a
                missedD++
            }
        }
        val (ta, tt) = Store.count(this, today)
        if (t == 0) {
            alert("[ SYSTÈME ]", "Ajoute tes quêtes pour commencer.", GLOW)
            editing = true
        } else if (ti > 29) {
            alert("[ MOIS TERMINÉ ]", "Résultat final : ${Store.rate(this)}% de quêtes accomplies. Rang ${"EDCBAS"[r]}.", CYAN)
        } else {
            if (missedQ > 0) {
                val f = missedFirst!!
                alert("[ PÉNALITÉ ]", "$missedQ quête(s) manquée(s) sur $missedD jour(s). Rattrape-les.", RED, "Voir") {
                    sel = f
                    render()
                }
            }
            if (ta < tt) {
                alert("[ RAPPEL ]", "${tt - ta} quête(s) à accomplir aujourd'hui.", GLOW, "Aller") {
                    sel = today
                    render()
                }
            } else {
                alert("[ QUÊTES DU JOUR ACCOMPLIES ]", "+${tt * 10} XP. Reviens demain.", CYAN)
            }
        }

        // Calendrier
        val cal = panel()
        cal.addView(tv("[ CALENDRIER · 30 JOURS ]", 13f, GLOW, true))
        for (row in 0 until 5) {
            val rl = LinearLayout(this)
            rl.orientation = LinearLayout.HORIZONTAL
            for (col2 in 0 until 6) {
                val i = row * 6 + col2
                val d = Store.addDays(start, i)
                val fut = i > ti
                val (a, tot) = Store.count(this, d)
                var stroke = LINE2
                var fill = CELL
                var color = DIM
                if (!fut) {
                    if (tot > 0 && a == tot) { stroke = CYAN; fill = DONE_FILL; color = CYAN }
                    else if (i < ti && tot > 0) { stroke = RED; fill = Color.parseColor("#2A0A16"); color = RED }
                    else if (a > 0) { stroke = GOLD; color = GOLD }
                }
                if (d == day) fill = SEL_FILL
                if (i == ti) stroke = GLOW
                val c = tv("${i + 1}", 15f, color, true)
                c.gravity = Gravity.CENTER
                c.background = box(stroke, fill, if (i == ti) 2 else 1)
                c.alpha = if (fut) 0.35f else 1f
                if (!fut) c.setOnClickListener { sel = d; render() }
                val cpar = LinearLayout.LayoutParams(0, dp(46), 1f)
                cpar.setMargins(dp(3), dp(3), dp(3), dp(3))
                rl.addView(c, cpar)
            }
            cal.addView(rl, lp(if (row == 0) 8 else 0))
        }
        root.addView(cal, lp(12))

        // Quêtes
        val qp = panel()
        qp.addView(tv("[ QUÊTES QUOTIDIENNES ]", 13f, GLOW, true))
        val label = SimpleDateFormat("EEEE d MMMM", Locale.FRANCE)
            .format(SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(day)!!)
        qp.addView(tv("Jour ${Store.dayIndexOf(this, day) + 1} · $label", 14f, DIM), lp(6))
        if (quests.isEmpty()) qp.addView(tv("Aucune quête. Ajoute-en une ci-dessous.", 15f, DIM), lp(8))
        for (q in quests) {
            val on = Store.isDone(this, day, q.id)
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            row.background = box(if (on) CYAN else LINE2, CELL)
            row.setPadding(dp(12), dp(12), dp(12), dp(12))
            row.addView(tv(if (on) "☑" else "☐", 22f, if (on) CYAN else GLOW))
            val name = tv(q.name, 17f, if (on) DIM else TEXT)
            if (on) name.paintFlags = name.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            val np = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
            np.leftMargin = dp(12)
            row.addView(name, np)
            if (editing) {
                row.addView(btn("Supprimer", RED) { Store.removeQuest(this, q.id); changed() })
            } else {
                row.setOnClickListener { Store.toggle(this, day, q.id); changed() }
            }
            qp.addView(row, lp(8))
        }
        if (editing) {
            val add = LinearLayout(this)
            add.orientation = LinearLayout.HORIZONTAL
            val et = EditText(this)
            et.hint = "Nouvelle quête (ex. 100 pompes)"
            et.setHintTextColor(DIM)
            et.setTextColor(TEXT)
            et.setSingleLine(true)
            add.addView(et, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            add.addView(btn("Ajouter", GLOW) {
                val v = et.text.toString().trim()
                if (v.isNotEmpty()) { Store.addQuest(this, v); changed() }
            })
            qp.addView(add, lp(10))
        }
        root.addView(qp, lp(12))

        // Stats
        val st = LinearLayout(this)
        st.orientation = LinearLayout.HORIZONTAL
        val vals = listOf(
            Pair("${Store.streak(this)}", "Série"),
            Pair("${minOf(maxOf(ti + 1, 0), 30)}/30", "Jour"),
            Pair("${Store.rate(this)}%", "Réussite")
        )
        for (v in vals) {
            val c = LinearLayout(this)
            c.orientation = LinearLayout.VERTICAL
            c.gravity = Gravity.CENTER
            c.background = box(LINE2, CELL)
            c.setPadding(dp(4), dp(10), dp(4), dp(10))
            c.addView(tv(v.first, 20f, CYAN, true))
            c.addView(tv(v.second, 13f, DIM))
            val sp = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
            sp.setMargins(dp(4), 0, dp(4), 0)
            st.addView(c, sp)
        }
        root.addView(st, lp(12))

        // Boutons
        root.addView(btn(if (editing) "Terminer la modification" else "Modifier les quêtes", GLOW) {
            editing = !editing
            render()
        }, lp(16))
        root.addView(btn(if (resetArm) "Confirmer : tout effacer" else "Recommencer le mois", if (resetArm) RED else DIM) {
            if (!resetArm) {
                resetArm = true
                render()
                root.postDelayed({ resetArm = false; render() }, 4000)
            } else {
                Store.reset(this)
                sel = null
                resetArm = false
                changed()
            }
        }, lp(8))
    }
}
