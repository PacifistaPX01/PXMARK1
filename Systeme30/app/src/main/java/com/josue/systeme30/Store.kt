package com.josue.systeme30

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class Quest(val id: String, val name: String)

object Store {
    private const val PREFS = "systeme30"

    private fun fmt() = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private fun p(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun today(): String = fmt().format(Date())

    fun addDays(day: String, n: Int): String {
        val c = Calendar.getInstance()
        c.time = fmt().parse(day)!!
        c.add(Calendar.DAY_OF_YEAR, n)
        return fmt().format(c.time)
    }

    private fun noon(day: String): Long {
        val c = Calendar.getInstance()
        c.time = fmt().parse(day)!!
        c.set(Calendar.HOUR_OF_DAY, 12)
        return c.timeInMillis
    }

    fun start(ctx: Context): String {
        val s = p(ctx).getString("start", null)
        if (s != null) return s
        val t = today()
        p(ctx).edit().putString("start", t).apply()
        return t
    }

    fun dayIndex(ctx: Context): Int =
        Math.round((noon(today()) - noon(start(ctx))) / 86400000.0).toInt()

    fun dayIndexOf(ctx: Context, day: String): Int =
        Math.round((noon(day) - noon(start(ctx))) / 86400000.0).toInt()

    private fun newId(): String =
        "q" + System.currentTimeMillis().toString(36) + (0..999).random()

    fun quests(ctx: Context): List<Quest> {
        val raw = p(ctx).getString("quests", null)
        if (raw == null) {
            val d = listOf(
                Quest(newId(), "Étudier 2 h"),
                Quest(newId(), "Exercice physique"),
                Quest(newId(), "Lire 20 pages")
            )
            saveQuests(ctx, d)
            return d
        }
        val a = JSONArray(raw)
        return (0 until a.length()).map {
            val o = a.getJSONObject(it)
            Quest(o.getString("id"), o.getString("name"))
        }
    }

    private fun saveQuests(ctx: Context, l: List<Quest>) {
        val a = JSONArray()
        l.forEach { a.put(JSONObject().put("id", it.id).put("name", it.name)) }
        p(ctx).edit().putString("quests", a.toString()).apply()
    }

    fun addQuest(ctx: Context, name: String) = saveQuests(ctx, quests(ctx) + Quest(newId(), name))

    fun removeQuest(ctx: Context, id: String) =
        saveQuests(ctx, quests(ctx).filter { it.id != id })

    private fun doneObj(ctx: Context): JSONObject =
        JSONObject(p(ctx).getString("done", "{}") ?: "{}")

    fun doneIds(ctx: Context, day: String): Set<String> {
        val a = doneObj(ctx).optJSONArray(day) ?: return emptySet()
        return (0 until a.length()).map { a.getString(it) }.toSet()
    }

    fun isDone(ctx: Context, day: String, id: String): Boolean = doneIds(ctx, day).contains(id)

    fun toggle(ctx: Context, day: String, id: String) {
        val cur = doneIds(ctx, day).toMutableSet()
        if (!cur.add(id)) cur.remove(id)
        val o = doneObj(ctx)
        o.put(day, JSONArray(cur.toList()))
        p(ctx).edit().putString("done", o.toString()).apply()
    }

    fun count(ctx: Context, day: String): Pair<Int, Int> {
        val q = quests(ctx).map { it.id }
        val d = doneIds(ctx, day)
        return Pair(q.count { it in d }, q.size)
    }

    fun isFull(ctx: Context, day: String): Boolean {
        val (a, t) = count(ctx, day)
        return t > 0 && a == t
    }

    fun xp(ctx: Context): Int {
        val s = start(ctx)
        var x = 0
        for (i in 0 until 30) x += count(ctx, addDays(s, i)).first * 10
        return x
    }

    fun streak(ctx: Context): Int {
        val s = start(ctx)
        var i = minOf(dayIndex(ctx), 29)
        if (i >= 0 && !isFull(ctx, addDays(s, i))) i--
        var n = 0
        while (i >= 0 && isFull(ctx, addDays(s, i))) {
            n++
            i--
        }
        return n
    }

    fun rate(ctx: Context): Int {
        val t = quests(ctx).size
        val el = maxOf(0, minOf(dayIndex(ctx) + 1, 30))
        if (t == 0 || el == 0) return 0
        return minOf(100, xp(ctx) / 10 * 100 / (el * t))
    }

    fun reset(ctx: Context) {
        p(ctx).edit().putString("start", today()).putString("done", "{}").apply()
    }
}
