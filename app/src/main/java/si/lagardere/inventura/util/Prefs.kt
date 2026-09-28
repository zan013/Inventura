package si.lagardere.inventura.util

import android.content.Context

/** Small persistent settings: last popisovalec, last skladišče, "only EAN" mode. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("inventura", Context.MODE_PRIVATE)

    var popisovalec: String
        get() = sp.getString("popisovalec", "") ?: ""
        set(v) = sp.edit().putString("popisovalec", v).apply()

    var skladisce: String
        get() = sp.getString("skladisce", "") ?: ""
        set(v) = sp.edit().putString("skladisce", v).apply()

    var onlyEan: Boolean
        get() = sp.getBoolean("only_ean", false)
        set(v) = sp.edit().putBoolean("only_ean", v).apply()

    var dwConfigured: Boolean
        get() = sp.getBoolean("dw_configured", false)
        set(v) = sp.edit().putBoolean("dw_configured", v).apply()
}
