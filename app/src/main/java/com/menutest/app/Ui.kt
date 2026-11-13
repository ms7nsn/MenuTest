package com.menutest.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuTestApp(onRecreate: () -> Unit) {
    val ctx = LocalContext.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val lang = remember { Prefs.effectiveLang(ctx) }
    val snack = remember { SnackbarHostState() }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0, onClick = { tab = 0 },
                    icon = { Icon(Icons.Filled.Home, null) }, label = { Text(stringResource(R.string.tab_menus)) },
                )
                NavigationBarItem(
                    selected = tab == 1, onClick = { tab = 1 },
                    icon = { Icon(Icons.Filled.Search, null) }, label = { Text(stringResource(R.string.tab_discover)) },
                )
                NavigationBarItem(
                    selected = tab == 2, onClick = { tab = 2 },
                    icon = { Icon(Icons.Filled.Settings, null) }, label = { Text(stringResource(R.string.tab_settings)) },
                )
            }
        },
    ) { pad ->
        val m = Modifier.padding(pad)
        when (tab) {
            0 -> MenusScreen(m, lang, snack)
            1 -> DiscoverScreen(m, snack)
            else -> SettingsScreen(m, onRecreate)
        }
    }
}

// ───────────────────────── Menus ─────────────────────────

@Composable
fun MenusScreen(modifier: Modifier, lang: String, snack: SnackbarHostState) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("mine") }
    var onlyAvail by rememberSaveable { mutableStateOf(false) }
    val avail = remember { mutableStateMapOf<String, Boolean>() }
    var dialog by remember { mutableStateOf<Entry?>(null) }

    LaunchedEffect(Unit) {
        val res = withContext(Dispatchers.Default) {
            Catalog.all.associate { it.id to Launcher.isAvailable(ctx, it) }
        }
        avail.putAll(res)
    }

    val mine = remember { Detect.mine() }
    val shown: List<Brand> = when (filter) {
        "mine" -> mine
        "all" -> mine + Brand.values().filter { it !in mine }
        else -> listOf(Brand.valueOf(filter))
    }
    val q = query.trim().lowercase()
    val groups = shown.map { b ->
        b to Catalog.all.filter { e ->
            e.brand == b &&
                (q.isEmpty() || e.title.en.lowercase().contains(q) || e.title.fr.lowercase().contains(q) ||
                    (e.code ?: "").lowercase().contains(q)) &&
                (!onlyAvail || avail[e.id] == true)
        }
    }.filter { it.second.isNotEmpty() }

    fun open(e: Entry) {
        Haptics.tick(ctx)
        when (Launcher.launch(ctx, e)) {
            Outcome.OPENED -> {}
            Outcome.SENT -> scope.launch { snack.showSnackbar(ctx.getString(R.string.sent_msg)) }
            Outcome.UNAVAILABLE -> dialog = e
        }
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.your_device),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        Detect.title(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        "Android ${Build.VERSION.RELEASE}" + (Detect.device?.let { " · ${it.label}" } ?: ""),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
            )
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = filter == "mine", onClick = { filter = "mine" },
                        label = { Text(stringResource(R.string.filter_mine)) },
                    )
                }
                item {
                    FilterChip(
                        selected = filter == "all", onClick = { filter = "all" },
                        label = { Text(stringResource(R.string.filter_all)) },
                    )
                }
                items(Brand.values().toList()) { b ->
                    FilterChip(selected = filter == b.name, onClick = { filter = b.name }, label = { Text(b.label) })
                }
            }
        }
        item {
            FilterChip(
                selected = onlyAvail, onClick = { onlyAvail = !onlyAvail },
                label = { Text(stringResource(R.string.only_available)) },
                leadingIcon = if (onlyAvail) {
                    { Icon(Icons.Filled.Check, null, Modifier.size(18.dp)) }
                } else null,
            )
        }
        if (groups.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.no_results),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        groups.forEach { (brand, list) ->
            item(key = "h_${brand.name}") {
                Text(
                    brand.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(list, key = { it.id }) { e ->
                EntryCard(e, lang, avail[e.id]) { open(e) }
            }
        }
    }

    dialog?.let { e ->
        val code = e.code
        AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text(stringResource(R.string.unavailable_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.unavailable_msg))
                    if (code != null) {
                        Text(code, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.titleMedium)
                        FilledTonalButton(onClick = {
                            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("code", code))
                            scope.launch { snack.showSnackbar(ctx.getString(R.string.code_copied)) }
                            dialog = null
                        }) { Text(stringResource(R.string.copy_code)) }
                        FilledTonalButton(onClick = {
                            try {
                                ctx.startActivity(Launcher.dialerIntent(code))
                            } catch (ex: Exception) {
                            }
                            dialog = null
                        }) { Text(stringResource(R.string.open_dialer)) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.close)) } },
        )
    }
}

@Composable
fun EntryCard(e: Entry, lang: String, available: Boolean?, onOpen: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(e.title.get(lang), style = MaterialTheme.typography.titleMedium)
                e.code?.let {
                    Text(
                        it,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                val d = e.desc.get(lang)
                if (d.isNotBlank()) {
                    Text(d, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                when (available) {
                    true -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Check, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            stringResource(R.string.found_here),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    false -> Text(
                        stringResource(R.string.not_found_here),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    null -> {}
                }
            }
            Spacer(Modifier.width(12.dp))
            FilledTonalButton(onClick = onOpen) {
                Icon(Icons.Filled.PlayArrow, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.open))
            }
        }
    }
}

// ───────────────────────── Discover ─────────────────────────

@Composable
fun DiscoverScreen(modifier: Modifier, snack: SnackbarHostState) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var scanning by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var results by remember { mutableStateOf<List<Found>?>(null) }

    LazyColumn(
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                stringResource(R.string.scan_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Button(
                onClick = {
                    Haptics.tick(ctx)
                    scanning = true
                    progress = 0f
                    scope.launch {
                        val r = withContext(Dispatchers.Default) {
                            Discover.scan(ctx) { i, n -> progress = if (n == 0) 1f else i / n.toFloat() }
                        }
                        results = r
                        scanning = false
                        snack.showSnackbar(ctx.getString(R.string.scan_done, r.size))
                    }
                },
                enabled = !scanning,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(if (scanning) R.string.scanning else R.string.scan)) }
        }
        if (scanning) {
            item { LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth()) }
        }
        results?.let { list ->
            items(list, key = { it.pkg + "/" + it.cls }) { f ->
                val canOpen = f.exported && f.permission == null
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(f.app, style = MaterialTheme.typography.titleMedium)
                            Text(
                                f.cls.substringAfterLast('.'),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                f.pkg,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (canOpen) Icons.Filled.Check else Icons.Filled.Warning, null,
                                    Modifier.size(14.dp),
                                    tint = if (canOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    stringResource(
                                        when {
                                            !f.exported -> R.string.not_exported
                                            f.permission != null -> R.string.needs_permission
                                            else -> R.string.exported
                                        },
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        FilledTonalButton(
                            enabled = canOpen,
                            onClick = {
                                Haptics.tick(ctx)
                                if (!Launcher.launchComponent(ctx, f.pkg, f.cls)) {
                                    scope.launch { snack.showSnackbar(ctx.getString(R.string.blocked_msg)) }
                                }
                            },
                        ) { Text(stringResource(R.string.open)) }
                    }
                }
            }
        }
    }
}

// ───────────────────────── Settings ─────────────────────────

@Composable
fun SettingsScreen(modifier: Modifier, onRecreate: () -> Unit) {
    val ctx = LocalContext.current
    var lang by remember { mutableStateOf(Prefs.language(ctx)) }
    var vib by remember { mutableStateOf(Prefs.vibration(ctx)) }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.language), style = MaterialTheme.typography.titleMedium)
        listOf(
            "system" to R.string.lang_system,
            "en" to R.string.lang_en,
            "fr" to R.string.lang_fr,
        ).forEach { (code, label) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(selected = lang == code, role = Role.RadioButton, onClick = {
                        if (lang != code) {
                            lang = code
                            Prefs.setLanguage(ctx, code)
                            onRecreate()
                        }
                    }),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = lang == code, onClick = null)
                Spacer(Modifier.width(12.dp))
                Text(stringResource(label), style = MaterialTheme.typography.bodyLarge)
            }
        }

        HorizontalDivider()

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.vibration), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.vibration_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = vib, onCheckedChange = {
                vib = it
                Prefs.setVibration(ctx, it)
                if (it) Haptics.tick(ctx)
            })
        }

        HorizontalDivider()

        Text(stringResource(R.string.about), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.about_text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            stringResource(R.string.version, "1.0"),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
