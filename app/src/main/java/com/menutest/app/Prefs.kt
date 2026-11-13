package com.menutest.app

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import java.util.Locale

/** Settings stored in SharedPreferences. */
object Prefs {
    private fun sp(c: Context) = c.applicationContext.getSharedPreferences("menutest", Context.MODE_PRIVATE)

    /** "system", "en" or "fr" */
    fun language(c: Context): String = sp(c).getString("lang", "system") ?: "system"
    fun setLanguage(c: Context, v: String) = sp(c).edit().putString("lang", v).apply()

    fun vibration(c: Context): Boolean = sp(c).getBoolean("vibration", true)
    fun setVibration(c: Context, v: Boolean) = sp(c).edit().putBoolean("vibration", v).apply()

    /** Language actually shown: "fr" or "en". */
    fun effectiveLang(c: Context): String {
        val l = language(c)
        if (l == "fr" || l == "en") return l
        return if (Locale.getDefault().language == "fr") "fr" else "en"
    }
}

object LocaleHelper {
    fun wrap(base: Context): Context {
        val l = Prefs.language(base)
        if (l == "system") return base
        val locale = Locale(l)
        Locale.setDefault(locale)
        val cfg = Configuration(base.resources.configuration)
        cfg.setLocale(locale)
        return base.createConfigurationContext(cfg)
    }
}

object Haptics {
    fun tick(c: Context) {
        if (!Prefs.vibration(c)) return
        try {
            val v: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
                (c.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                c.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (v == null || !v.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= 29) {
                v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                v.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } catch (ex: Exception) {
        }
    }
}
