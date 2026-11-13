package com.menutest.app

import android.os.Build

enum class Brand(val label: String) {
    SAMSUNG("Samsung"),
    XIAOMI("Xiaomi · Redmi · POCO"),
    HUAWEI("Huawei · Honor"),
    ONEPLUS("OnePlus · Oppo · Realme"),
    VIVO("Vivo · iQOO"),
    MOTOROLA("Motorola · Lenovo"),
    SONY("Sony"),
    GOOGLE("Google Pixel"),
    NOKIA("Nokia · HMD"),
    LG("LG"),
    ASUS("ASUS"),
    HTC("HTC"),
    TRANSSION("Infinix · Tecno · itel"),
    MEDIATEK("MediaTek chipset"),
    QUALCOMM("Qualcomm chipset"),
    ANDROID("Android"),
    CLASSIC("Classic codes"),
    SYSTEM("System screens");
}

/** One way of reaching a menu without the dialer. Tried in order. */
sealed class Method {
    /** Explicit activity (optionally with string extras). */
    data class Comp(val pkg: String, val cls: String, val extras: Map<String, String> = emptyMap()) : Method()

    /** android.provider.Telephony.SECRET_CODE broadcast, e.g. code "4636" for *#*#4636#*#*. */
    data class Secret(val code: String) : Method()

    /** Public system action (Settings screens). */
    data class Act(val action: String) : Method()

    /** A test app: launcher intent, or its exported test/engineer activity. */
    data class App(val pkg: String) : Method()
}

data class Txt(val en: String, val fr: String) {
    fun get(lang: String) = if (lang == "fr") fr else en
}

data class Entry(
    val id: String,
    val brand: Brand,
    val code: String?,
    val title: Txt,
    val desc: Txt,
    val methods: List<Method>,
)

object Detect {
    private val m = (Build.MANUFACTURER ?: "").lowercase()
    private val b = (Build.BRAND ?: "").lowercase()
    private val hw = ((Build.HARDWARE ?: "") + " " + (Build.BOARD ?: "")).lowercase()

    private fun any(vararg k: String) = k.any { m.contains(it) || b.contains(it) }

    /** Brand family of this phone, or null when unknown. */
    val device: Brand? = when {
        any("samsung") -> Brand.SAMSUNG
        any("xiaomi", "redmi", "poco", "blackshark") -> Brand.XIAOMI
        any("huawei", "honor") -> Brand.HUAWEI
        any("oneplus", "oppo", "realme") -> Brand.ONEPLUS
        any("vivo", "iqoo") -> Brand.VIVO
        any("motorola", "lenovo") -> Brand.MOTOROLA
        any("sony") -> Brand.SONY
        any("google") -> Brand.GOOGLE
        any("hmd", "nokia") -> Brand.NOKIA
        any("lge", "lg") -> Brand.LG
        any("asus") -> Brand.ASUS
        any("htc") -> Brand.HTC
        any("infinix", "tecno", "itel", "transsion") -> Brand.TRANSSION
        else -> null
    }

    val chipset: Brand? = when {
        hw.contains("mt") && Regex("mt\\d").containsMatchIn(hw) -> Brand.MEDIATEK
        hw.contains("qcom") || hw.contains("msm") || hw.contains("sm8") || hw.contains("kona") -> Brand.QUALCOMM
        else -> null
    }

    /** Brands shown first / in "My device" mode, in display order. */
    fun mine(): List<Brand> =
        listOfNotNull(device, chipset) + listOf(Brand.ANDROID, Brand.CLASSIC, Brand.SYSTEM)

    fun title(): String = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
}

object Catalog {
    private fun sc(d: String) = "*#*#$d#*#*"

    private fun e(
        brand: Brand, code: String?, en: String, fr: String, vararg m: Method,
        dEn: String = "", dFr: String = "",
    ) = Entry("${brand.name}:${code ?: en}", brand, code, Txt(en, fr), Txt(dEn, dFr), m.toList())

    private fun secret(d: String) = Method.Secret(d)
    private fun app(p: String) = Method.App(p)
    private fun comp(p: String, c: String) = Method.Comp(p, c)
    private fun act(a: String) = Method.Act(a)

    /** Samsung's service-mode app takes the dial string as "keyString". */
    private fun ssm(key: String): Array<Method> = arrayOf(
        Method.Comp("com.sec.android.RilServiceModeApp", ".ServiceModeApp", mapOf("keyString" to key)),
        Method.Comp("com.samsung.android.app.servicemodeapp", ".ServiceModeApp", mapOf("keyString" to key)),
        Method.Comp("com.sec.android.app.servicemodeapp", ".ServiceModeApp", mapOf("keyString" to key)),
        Method.Comp("com.sec.android.RilServiceModeApp", "com.sec.android.RilServiceModeApp.ServiceModeApp", mapOf("keyString" to key)),
    )

    private val sam = Brand.SAMSUNG
    private val hwTest = arrayOf<Method>(
        comp("com.sec.android.app.hwmoduletest", ".HwModuleTest"),
        comp("com.sec.android.app.hwmoduletest", "com.sec.android.app.hwmoduletest.HwModuleTest"),
        app("com.sec.android.app.hwmoduletest"),
    )

    val all: List<Entry> = listOf(
        // ───────── Samsung ─────────
        e(sam, "*#0*#", "General test mode", "Mode test général", *hwTest,
            dEn = "LCD, touch, sensors, camera, speaker, vibration…", dFr = "Écran, tactile, capteurs, caméra, haut-parleur, vibration…"),
        e(sam, "*#0011#", "Service mode (network)", "Mode service (réseau)", *ssm("0011"),
            dEn = "Network status and signal details", dFr = "État du réseau et détails du signal"),
        e(sam, "*#2263#", "Band selection", "Sélection des bandes", *ssm("2263"),
            dEn = "RF band lock", dFr = "Verrouillage des bandes RF"),
        e(sam, "*#0228#", "Battery status", "État de la batterie", *ssm("0228")),
        e(sam, "*#1234#", "Firmware version", "Version du firmware", *ssm("1234"),
            dEn = "PDA, CSC and phone versions", dFr = "Versions PDA, CSC et modem"),
        e(sam, "*#12580*369#", "Software & hardware info", "Infos logiciel et matériel", *ssm("12580*369")),
        e(sam, "*#7353#", "Quick test menu", "Menu de test rapide", *ssm("7353"), *hwTest),
        e(sam, "*#9090#", "Diagnostic configuration", "Configuration diagnostic", *ssm("9090")),
        e(sam, "*#9900#", "SysDump", "SysDump", *ssm("9900"), dEn = "Debug and log tools", dFr = "Outils de débogage et journaux"),
        e(sam, "*#0283#", "Audio loopback test", "Test de boucle audio", *ssm("0283")),
        e(sam, "*#0588#", "Proximity sensor test", "Test du capteur de proximité", *ssm("0588")),
        e(sam, "*#0589#", "Light sensor test", "Test du capteur de luminosité", *ssm("0589")),
        e(sam, "*#0673#", "Melody / audio test", "Test mélodie / audio", *ssm("0673")),
        e(sam, "*#0782#", "Real-time clock test", "Test de l'horloge temps réel", *ssm("0782")),
        e(sam, "*#0842#", "Vibration & backlight test", "Test vibration et rétroéclairage", *ssm("0842")),
        e(sam, "*#1111#", "FTA software version", "Version logicielle FTA", *ssm("1111")),
        e(sam, "*#2222#", "FTA hardware version", "Version matérielle FTA", *ssm("2222")),
        e(sam, "*#2663#", "Touch screen version", "Version de l'écran tactile", *ssm("2663")),
        e(sam, "*#7465625#", "Network lock status", "État du verrouillage réseau", *ssm("7465625")),
        e(sam, "*#232331#", "Bluetooth test", "Test Bluetooth", *ssm("232331")),
        e(sam, "*#232337#", "Bluetooth address", "Adresse Bluetooth", *ssm("232337")),
        e(sam, "*#232339#", "Wi-Fi test", "Test Wi-Fi", *ssm("232339")),
        e(sam, "*#1575#", "GPS test", "Test GPS", *ssm("1575")),
        e(sam, "*#34971539#", "Camera firmware", "Firmware de l'appareil photo", *ssm("34971539")),

        // ───────── Xiaomi / Redmi / POCO ─────────
        e(Brand.XIAOMI, sc("6484"), "Hardware test (CIT)", "Test matériel (CIT)",
            secret("6484"), comp("com.miui.cit", ".CitLauncherActivity"), app("com.miui.cit"),
            dEn = "Display, touch, sensors, camera, audio, charging", dFr = "Écran, tactile, capteurs, caméra, audio, charge"),
        e(Brand.XIAOMI, sc("64663"), "CIT test (alternate)", "Test CIT (alternatif)", secret("64663"), app("com.miui.cit")),
        e(Brand.XIAOMI, sc("284"), "Bug report capture", "Capture de rapport de bug", secret("284")),
        e(Brand.XIAOMI, null, "About phone (tap Kernel version)", "À propos (toucher Version du noyau)",
            act("android.settings.DEVICE_INFO_SETTINGS"),
            dEn = "On MIUI/HyperOS, tapping Kernel version several times opens the hardware test",
            dFr = "Sur MIUI/HyperOS, toucher plusieurs fois Version du noyau ouvre le test matériel"),

        // ───────── Huawei / Honor ─────────
        e(Brand.HUAWEI, sc("2846579"), "Project menu", "Menu projet", secret("2846579"), app("com.huawei.android.projectmenu")),
        e(Brand.HUAWEI, sc("2846579159"), "Project menu (new)", "Menu projet (récent)", secret("2846579159")),
        e(Brand.HUAWEI, sc("14789632"), "Hardware test", "Test matériel", secret("14789632")),
        e(Brand.HUAWEI, sc("0000"), "Software version", "Version logicielle", secret("0000")),

        // ───────── OnePlus / Oppo / Realme ─────────
        e(Brand.ONEPLUS, "*#800#", "Engineer mode", "Mode ingénieur",
            app("com.oplus.engineermode"), app("com.oppo.engineermode"), app("com.oem.engineeringmode.main"), app("com.oneplus.factorymode")),
        e(Brand.ONEPLUS, "*#899#", "Hardware test", "Test matériel",
            app("com.oplus.engineermode"), app("com.oppo.engineermode"), app("com.oem.engineeringmode.main")),
        e(Brand.ONEPLUS, "*#36446337#", "Engineer mode (alternate)", "Mode ingénieur (alternatif)",
            app("com.oplus.engineermode"), app("com.oppo.engineermode")),
        e(Brand.ONEPLUS, "*#808#", "Hardware info", "Infos matérielles", app("com.oem.engineeringmode.main")),

        // ───────── Vivo / iQOO ─────────
        e(Brand.VIVO, "*#558#", "Device info / test", "Infos et test de l'appareil",
            app("com.vivo.factorytest"), app("com.vivo.engineermode"), app("com.vivo.engineeringmode")),

        // ───────── Motorola / Lenovo ─────────
        e(Brand.MOTOROLA, sc("2486"), "CQA test", "Test CQA", secret("2486"), app("com.motorola.motocit"),
            dEn = "Motorola hardware quality test", dFr = "Test qualité matériel Motorola"),

        // ───────── Sony ─────────
        e(Brand.SONY, sc("7378423"), "Service tests", "Tests de service",
            secret("7378423"), comp("com.sonyericsson.android.servicemenu", ".ServiceMenu"), app("com.sonyericsson.android.servicemenu")),
        e(Brand.SONY, sc("3424"), "Diagnostics", "Diagnostics", secret("3424"), app("com.sonyericsson.diagnostics")),
        e(Brand.SONY, sc("2664"), "Touch screen test", "Test de l'écran tactile", secret("2664")),

        // ───────── Google Pixel ─────────
        e(Brand.GOOGLE, sc("7287"), "Pixel diagnostics", "Diagnostic Pixel", secret("7287"),
            dEn = "Available on some Pixel models", dFr = "Disponible sur certains modèles Pixel"),

        // ───────── Nokia / HMD ─────────
        e(Brand.NOKIA, sc("372733"), "Hardware test", "Test matériel", secret("372733")),

        // ───────── LG ─────────
        e(Brand.LG, "3845#*xxx#", "Hidden menu (needs model code)", "Menu caché (code modèle requis)",
            app("com.lge.hiddenmenu"), app("com.lge.sizechangable.hiddenmenu")),

        // ───────── ASUS ─────────
        e(Brand.ASUS, null, "ATD SMMI test", "Test ATD SMMI", app("com.asus.atd.smmitest"), app("com.asus.factorytest")),

        // ───────── HTC ─────────
        e(Brand.HTC, sc("3424"), "Diagnostics", "Diagnostics", secret("3424")),
        e(Brand.HTC, sc("7262626"), "Field test", "Test terrain", secret("7262626")),

        // ───────── Infinix / Tecno / itel ─────────
        e(Brand.TRANSSION, sc("3646633"), "Engineer mode (MediaTek)", "Mode ingénieur (MediaTek)",
            secret("3646633"), comp("com.mediatek.engineermode", ".EngineerMode")),
        e(Brand.TRANSSION, null, "Factory test", "Test usine", app("com.transsion.cit"), app("com.mediatek.factorymode")),

        // ───────── MediaTek ─────────
        e(Brand.MEDIATEK, sc("3646633"), "Engineer mode", "Mode ingénieur",
            secret("3646633"), comp("com.mediatek.engineermode", ".EngineerMode"), app("com.mediatek.engineermode"),
            dEn = "Network, camera, audio, sensors, battery", dFr = "Réseau, caméra, audio, capteurs, batterie"),
        e(Brand.MEDIATEK, sc("2237332846633"), "Engineer mode (new)", "Mode ingénieur (récent)", secret("2237332846633")),
        e(Brand.MEDIATEK, null, "Factory mode", "Mode usine", app("com.mediatek.factorymode")),
        e(Brand.MEDIATEK, null, "MTK Logger", "Journaux MTK", app("com.mediatek.mtklogger")),

        // ───────── Qualcomm ─────────
        e(Brand.QUALCOMM, null, "QMMI factory test", "Test usine QMMI", app("com.qualcomm.qti.qmmi")),

        // ───────── Android (AOSP) ─────────
        e(Brand.ANDROID, sc("4636"), "Phone information", "Informations du téléphone",
            secret("4636"),
            comp("com.android.phone", "com.android.phone.settings.RadioInfo"),
            comp("com.android.settings", "com.android.settings.RadioInfo"),
            dEn = "Radio info, battery info, usage statistics, Wi-Fi", dFr = "Infos radio, batterie, statistiques d'utilisation, Wi-Fi"),
        e(Brand.ANDROID, "*#06#", "IMEI & device status", "IMEI et état de l'appareil",
            act("android.settings.DEVICE_INFO_SETTINGS"),
            dEn = "Opens About phone (IMEI, serial, addresses)", dFr = "Ouvre À propos du téléphone (IMEI, série, adresses)"),
        e(Brand.ANDROID, sc("197328640"), "Service mode", "Mode service", secret("197328640")),

        // ───────── Classic codes ─────────
        e(Brand.CLASSIC, sc("34971539"), "Camera information", "Informations de l'appareil photo", secret("34971539")),
        e(Brand.CLASSIC, sc("1472365"), "GPS test", "Test GPS", secret("1472365")),
        e(Brand.CLASSIC, sc("1575"), "GPS test (alternate)", "Test GPS (alternatif)", secret("1575")),
        e(Brand.CLASSIC, sc("232338"), "Wi-Fi MAC address", "Adresse MAC Wi-Fi", secret("232338")),
        e(Brand.CLASSIC, sc("232339"), "Wi-Fi test", "Test Wi-Fi", secret("232339")),
        e(Brand.CLASSIC, sc("232331"), "Bluetooth test", "Test Bluetooth", secret("232331")),
        e(Brand.CLASSIC, sc("232337"), "Bluetooth address", "Adresse Bluetooth", secret("232337")),
        e(Brand.CLASSIC, sc("0842"), "Backlight & vibration test", "Test rétroéclairage et vibration", secret("0842")),
        e(Brand.CLASSIC, sc("0588"), "Proximity sensor test", "Test du capteur de proximité", secret("0588")),
        e(Brand.CLASSIC, sc("0283"), "Loopback test", "Test de boucle", secret("0283")),
        e(Brand.CLASSIC, sc("0289"), "Melody test", "Test mélodie", secret("0289")),
        e(Brand.CLASSIC, sc("0673"), "Audio test", "Test audio", secret("0673")),
        e(Brand.CLASSIC, sc("2663"), "Touch screen version", "Version de l'écran tactile", secret("2663")),
        e(Brand.CLASSIC, sc("2664"), "Touch screen test", "Test de l'écran tactile", secret("2664")),
        e(Brand.CLASSIC, sc("1111"), "FTA software version", "Version logicielle FTA", secret("1111")),
        e(Brand.CLASSIC, sc("2222"), "FTA hardware version", "Version matérielle FTA", secret("2222")),
        e(Brand.CLASSIC, sc("3264"), "RAM version", "Version de la RAM", secret("3264")),
        e(Brand.CLASSIC, sc("1234"), "PDA & phone firmware", "Firmware PDA et téléphone", secret("1234")),
        e(Brand.CLASSIC, sc("4986*2650468"), "PDA / phone / HW / RF dates", "Dates PDA / téléphone / matériel / RF", secret("4986*2650468")),
        e(Brand.CLASSIC, sc("7594"), "Power button behavior", "Comportement du bouton marche", secret("7594")),
        e(Brand.CLASSIC, sc("8255"), "GTalk service monitor", "Moniteur du service GTalk", secret("8255")),
        e(Brand.CLASSIC, sc("426"), "Google push (FCM) diagnostics", "Diagnostic Google push (FCM)", secret("426")),
        e(Brand.CLASSIC, sc("759"), "Google partner setup", "Configuration partenaire Google", secret("759")),
        e(Brand.CLASSIC, sc("2432546"), "Google check-in", "Enregistrement Google", secret("2432546")),

        // ───────── System screens (always work) ─────────
        e(Brand.SYSTEM, null, "Developer options", "Options pour les développeurs", act("android.settings.APPLICATION_DEVELOPMENT_SETTINGS")),
        e(Brand.SYSTEM, null, "About phone", "À propos du téléphone", act("android.settings.DEVICE_INFO_SETTINGS")),
        e(Brand.SYSTEM, null, "Mobile network", "Réseau mobile", act("android.settings.DATA_ROAMING_SETTINGS")),
        e(Brand.SYSTEM, null, "Wi-Fi", "Wi-Fi", act("android.settings.WIFI_SETTINGS")),
        e(Brand.SYSTEM, null, "Bluetooth", "Bluetooth", act("android.settings.BLUETOOTH_SETTINGS")),
        e(Brand.SYSTEM, null, "NFC", "NFC", act("android.settings.NFC_SETTINGS")),
        e(Brand.SYSTEM, null, "Display", "Affichage", act("android.settings.DISPLAY_SETTINGS")),
        e(Brand.SYSTEM, null, "Sound", "Son", act("android.settings.SOUND_SETTINGS")),
        e(Brand.SYSTEM, null, "Location", "Localisation", act("android.settings.LOCATION_SOURCE_SETTINGS")),
        e(Brand.SYSTEM, null, "Storage", "Stockage", act("android.settings.INTERNAL_STORAGE_SETTINGS")),
        e(Brand.SYSTEM, null, "Battery saver", "Économiseur de batterie", act("android.settings.BATTERY_SAVER_SETTINGS")),
        e(Brand.SYSTEM, null, "Accessibility", "Accessibilité", act("android.settings.ACCESSIBILITY_SETTINGS")),
        e(Brand.SYSTEM, null, "Date & time", "Date et heure", act("android.settings.DATE_SETTINGS")),
        e(Brand.SYSTEM, null, "Security", "Sécurité", act("android.settings.SECURITY_SETTINGS")),
    )
}
