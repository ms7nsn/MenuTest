package com.menutest.app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

enum class Outcome { OPENED, SENT, UNAVAILABLE }

/** Keywords that identify engineering / factory / test screens. */
val TEST_REGEX = Regex(
    "engineer|factory|hwmodule|servicemode|servicemenu|projectmenu|hiddenmenu|diagnos|" +
        "(^|[./])cit([./]|\$)|qmmi|smmi|selftest|hardwaretest|testmode|testmenu|rftest|mmitest|" +
        "sysdump|motocit|radioinfo|hwtest|engineering",
    RegexOption.IGNORE_CASE,
)

object Launcher {
    private const val SECRET_ACTION = "android.provider.Telephony.SECRET_CODE"

    private fun secretIntent(code: String) =
        Intent(SECRET_ACTION, Uri.parse("android_secret_code://$code"))

    private fun secretReceivers(ctx: Context, code: String): List<ComponentName> = try {
        ctx.packageManager.queryBroadcastReceivers(secretIntent(code), 0)
            .filter { it.activityInfo != null && it.activityInfo.exported }
            .map { ComponentName(it.activityInfo.packageName, it.activityInfo.name) }
    } catch (ex: Exception) {
        emptyList()
    }

    private fun main(): Intent = Intent(Intent.ACTION_MAIN).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** The first exported, enabled test-looking activity of a package, if any. */
    @Suppress("DEPRECATION")
    fun findTestActivity(pm: PackageManager, pkg: String): ComponentName? = try {
        pm.getPackageInfo(pkg, PackageManager.GET_ACTIVITIES).activities
            ?.firstOrNull { it.exported && it.enabled && it.permission == null && TEST_REGEX.containsMatchIn(it.name) }
            ?.let { ComponentName(it.packageName, it.name) }
    } catch (ex: Exception) {
        null
    }

    private fun activityIntent(ctx: Context, m: Method): Intent? {
        val pm = ctx.packageManager
        return when (m) {
            is Method.Comp -> {
                val cls = if (m.cls.startsWith(".")) m.pkg + m.cls else m.cls
                val i = main().setClassName(m.pkg, cls)
                m.extras.forEach { (k, v) -> i.putExtra(k, v) }
                if (pm.resolveActivity(i, 0) != null) i else null
            }
            is Method.Act -> {
                val i = Intent(m.action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (pm.resolveActivity(i, 0) != null) i else null
            }
            is Method.App -> {
                val launch = pm.getLaunchIntentForPackage(m.pkg)
                if (launch != null) {
                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                } else {
                    findTestActivity(pm, m.pkg)?.let { main().setComponent(it) }
                }
            }
            is Method.Secret -> null
        }
    }

    /** True when at least one way of opening this entry exists on this device. */
    fun isAvailable(ctx: Context, e: Entry): Boolean = e.methods.any { m ->
        when (m) {
            is Method.Secret -> secretReceivers(ctx, m.code).isNotEmpty()
            else -> activityIntent(ctx, m) != null
        }
    }

    fun launch(ctx: Context, e: Entry): Outcome {
        for (m in e.methods) {
            if (m is Method.Secret) {
                val targets = secretReceivers(ctx, m.code)
                if (targets.isNotEmpty()) {
                    var sent = false
                    for (t in targets) {
                        try {
                            ctx.sendBroadcast(secretIntent(m.code).setComponent(t))
                            sent = true
                        } catch (ex: Exception) {
                        }
                    }
                    if (sent) return Outcome.SENT
                }
            } else {
                val i = activityIntent(ctx, m) ?: continue
                try {
                    ctx.startActivity(i)
                    return Outcome.OPENED
                } catch (ex: Exception) {
                    // not exported / permission denied: try the next method
                }
            }
        }
        return Outcome.UNAVAILABLE
    }

    fun launchComponent(ctx: Context, pkg: String, cls: String): Boolean = try {
        ctx.startActivity(main().setClassName(pkg, cls))
        true
    } catch (ex: Exception) {
        false
    }

    fun dialerIntent(code: String): Intent =
        Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(code))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

data class Found(
    val pkg: String,
    val app: String,
    val cls: String,
    val exported: Boolean,
    val permission: String?,
)

object Discover {
    /** Scans every installed package for engineering / test activities. */
    @Suppress("DEPRECATION")
    fun scan(ctx: Context, onProgress: (Int, Int) -> Unit): List<Found> {
        val pm = ctx.packageManager
        val apps = pm.getInstalledApplications(0)
        val out = ArrayList<Found>()
        apps.forEachIndexed { idx, ai ->
            if (idx % 20 == 0) onProgress(idx, apps.size)
            if (ai.packageName == ctx.packageName) return@forEachIndexed
            try {
                val acts = pm.getPackageInfo(ai.packageName, PackageManager.GET_ACTIVITIES).activities ?: return@forEachIndexed
                val label = pm.getApplicationLabel(ai).toString()
                for (a in acts) {
                    if (!a.enabled) continue
                    if (TEST_REGEX.containsMatchIn(a.packageName + "/" + a.name)) {
                        out.add(Found(a.packageName, label, a.name, a.exported, a.permission))
                    }
                }
            } catch (ex: Exception) {
            }
        }
        onProgress(apps.size, apps.size)
        return out.sortedWith(compareBy<Found>({ !it.exported || it.permission != null }, { it.app.lowercase() }, { it.cls }))
    }
}
