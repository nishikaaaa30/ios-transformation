package com.example.iostransformation

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.graphics.Bitmap
import android.content.Context
import android.content.SharedPreferences
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Blue = Color(0xFF4F7DFF)
private val Ink = Color(0xFF172033)
private val Pale = Color(0xFFF2F5FC)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TransformationApp() }
    }
}

data class LaunchApp(val label: String, val packageName: String, val icon: android.graphics.drawable.Drawable)

@Composable
fun TransformationApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("look_settings", Context.MODE_PRIVATE) }
    var dark by remember { mutableStateOf(prefs.getBoolean("dark", false)) }
    var page by remember { mutableStateOf("home") }
    var search by remember { mutableStateOf("") }
    var showControl by remember { mutableStateOf(false) }
    var selectedAccent by remember { mutableIntStateOf(prefs.getInt("accent", 0)) }
    var wallpaperPreview by remember { mutableStateOf<Bitmap?>(null) }
    val accents = listOf(Blue, Color(0xFF9A72FF), Color(0xFFFF7597), Color(0xFF25A98A), Color(0xFFFFA63D))
    val accent = accents[selectedAccent.coerceIn(accents.indices)]
    val wallpaperPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    if (bitmap != null) wallpaperPreview = bitmap
                }
            } catch (_: Exception) { }
        }
    }
    val apps = remember { loadLaunchableApps(context) }

    MaterialTheme(colorScheme = if (dark) darkColorScheme(primary = accent) else lightColorScheme(primary = accent)) {
        Box(Modifier.fillMaxSize().background(if (dark) Color(0xFF10131C) else Pale)) {
            AnimatedContent(targetState = page, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "pages") { current ->
                when (current) {
                    "home" -> HomeScreen(
                        apps = apps,
                        accent = accent,
                        dark = dark,
                        wallpaper = wallpaperPreview,
                        onOpenApp = { launchApp(context, it.packageName) },
                        onOpenLibrary = { page = "library" },
                        onOpenSettings = { page = "dashboard" },
                        onOpenControl = { showControl = true }
                    )
                    "library" -> AppLibrary(apps, search, { search = it }, { launchApp(context, it.packageName) }, { page = "home" }, dark)
                    else -> Dashboard(
                        dark = dark, accentIndex = selectedAccent,
                        onDark = { dark = it; prefs.edit().putBoolean("dark", it).apply() },
                        onAccent = { selectedAccent = it; prefs.edit().putInt("accent", it).apply() },
                        onWallpaper = { wallpaperPicker.launch("image/*") },
                        onApplyWallpaper = {
                            wallpaperPreview?.let { bitmap ->
                                try { WallpaperManager.getInstance(context).setBitmap(bitmap) } catch (_: Exception) { }
                            }
                        },
                        hasWallpaper = wallpaperPreview != null,
                        onHomeSettings = { context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) },
                        onSystemSettings = { context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) },
                        onBack = { page = "home" }
                    )
                }
            }
            if (showControl) ControlCenter(dark, { showControl = false }, {
                val intent = when (it) {
                    "Wi-Fi" -> Intent(Settings.ACTION_WIFI_SETTINGS)
                    "Bluetooth" -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                    "Airplane mode" -> Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
                    "Do Not Disturb" -> Intent(Settings.ACTION_ZEN_MODE_SETTINGS)
                    "Brightness" -> Intent(Settings.ACTION_DISPLAY_SETTINGS)
                    "Sound" -> Intent(Settings.ACTION_SOUND_SETTINGS)
                    else -> Intent(Settings.ACTION_SETTINGS)
                }
                try { context.startActivity(intent) } catch (_: Exception) { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
                showControl = false
            })
        }
    }
}

private fun loadLaunchableApps(context: Context): List<LaunchApp> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return try {
        @Suppress("DEPRECATION")
        pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .map { LaunchApp(it.loadLabel(pm).toString(), it.activityInfo.packageName, it.loadIcon(pm)) }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase(Locale.getDefault()) }
    } catch (_: Exception) { emptyList() }
}

private fun launchApp(context: Context, packageName: String) {
    try {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) context.startActivity(intent)
    } catch (_: Exception) { }
}

@Composable
fun HomeScreen(apps: List<LaunchApp>, accent: Color, dark: Boolean, wallpaper: Bitmap?, onOpenApp: (LaunchApp) -> Unit, onOpenLibrary: () -> Unit, onOpenSettings: () -> Unit, onOpenControl: () -> Unit) {
    val foreground = if (dark) Color.White else Ink
    val now = remember { SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date()) }
    val dockNames = listOf("Phone", "Messages", "Chrome", "Camera")
    val dock = dockNames.mapNotNull { name -> apps.firstOrNull { it.label.equals(name, true) || it.label.contains(name, true) } }.distinctBy { it.packageName }.take(4)
    Box(Modifier.fillMaxSize()) {
        if (wallpaper != null) {
            androidx.compose.foundation.Image(bitmap = wallpaper.asImageBitmap(), contentDescription = "Wallpaper preview", modifier = Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.15f)))
        } else {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFB9D8FF), Color(0xFFE4E4FF), Color(0xFFF7D9E8), Color(0xFFB8E5E5)))))
        }
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${SimpleDateFormat("h:mm", Locale.getDefault()).format(Date())}", color = foreground, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SignalCellularAlt, null, tint = foreground, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp)); Icon(Icons.Default.Wifi, null, tint = foreground, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp)); Icon(Icons.Default.BatteryFull, null, tint = foreground, modifier = Modifier.size(19.dp))
                }
            }
            Spacer(Modifier.height(30.dp))
            Text(now, color = foreground.copy(alpha = .9f), fontSize = 15.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Text(SimpleDateFormat("d", Locale.getDefault()).format(Date()), color = foreground, fontSize = 68.sp, fontWeight = FontWeight.Light, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(24.dp))
            val featured = apps.filterNot { it.label.equals("Settings", true) }.take(8)
            LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(18.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                items(featured) { app -> AppIcon(app, foreground) { onOpenApp(app) } }
                item { UtilityIcon("Library", Icons.Default.Apps, accent, foreground, onOpenLibrary) }
                item { UtilityIcon("Settings", Icons.Default.Tune, accent, foreground, onOpenSettings) }
                item { UtilityIcon("Control", Icons.Default.Tune, accent, foreground, onOpenControl) }
            }
            Surface(color = Color.White.copy(alpha = if (dark) .13f else .48f), shape = RoundedCornerShape(30.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    dock.forEach { app -> AppIcon(app, foreground, showLabel = false) { onOpenApp(app) } }
                    if (dock.isEmpty()) {
                        UtilityIcon("Apps", Icons.Default.Apps, accent, foreground, onOpenLibrary)
                        UtilityIcon("Theme", Icons.Default.Palette, accent, foreground, onOpenSettings)
                    }
                }
            }
            Text("Swipe up for App Library", color = foreground.copy(alpha = .75f), fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().clickable { onOpenLibrary() }.padding(4.dp))
        }
    }
}

@Composable
fun AppIcon(app: LaunchApp, foreground: Color, showLabel: Boolean = true, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        androidx.compose.foundation.Image(bitmap = remember(app.packageName) { drawableToBitmap(app.icon).asImageBitmap() }, contentDescription = app.label, modifier = Modifier.size(54.dp).clip(RoundedCornerShape(16.dp)))
        if (showLabel) Text(app.label, color = foreground, fontSize = 10.sp, maxLines = 1, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 5.dp))
    }
}

private fun drawableToBitmap(drawable: android.graphics.drawable.Drawable): Bitmap {
    val w = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
    val h = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
    val bitmap = Bitmap.createBitmap(w.coerceAtMost(256), h.coerceAtMost(256), Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height); drawable.draw(canvas)
    return bitmap
}

@Composable
fun UtilityIcon(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, accent: Color, foreground: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Box(Modifier.size(54.dp).clip(RoundedCornerShape(16.dp)).background(accent.copy(alpha = .9f)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(28.dp)) }
        Text(label, color = foreground, fontSize = 10.sp, modifier = Modifier.padding(top = 5.dp))
    }
}

@Composable
fun AppLibrary(apps: List<LaunchApp>, search: String, onSearch: (String) -> Unit, onOpen: (LaunchApp) -> Unit, onBack: () -> Unit, dark: Boolean) {
    val fg = if (dark) Color.White else Ink
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = fg) }; Text("App Library", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = fg) }
        OutlinedTextField(value = search, onValueChange = onSearch, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Search apps") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true, shape = RoundedCornerShape(18.dp))
        Spacer(Modifier.height(14.dp))
        val filtered = apps.filter { it.label.contains(search, true) }
        LazyVerticalGrid(columns = GridCells.Adaptive(76.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) { items(filtered) { AppIcon(it, fg) { onOpen(it) } } }
    }
}

@Composable
fun Dashboard(dark: Boolean, accentIndex: Int, onDark: (Boolean) -> Unit, onAccent: (Int) -> Unit, onWallpaper: () -> Unit, onApplyWallpaper: () -> Unit, hasWallpaper: Boolean, onHomeSettings: () -> Unit, onSystemSettings: () -> Unit, onBack: () -> Unit) {
    val fg = if (dark) Color.White else Ink
    val accents = listOf(Blue, Color(0xFF9A72FF), Color(0xFFFF7597), Color(0xFF25A98A), Color(0xFFFFA63D))
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp, bottom = 18.dp)) { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = fg) }; Column { Text("Personalize", fontSize = 27.sp, fontWeight = FontWeight.Bold, color = fg); Text("Make your Galaxy feel like yours", color = fg.copy(alpha = .65f), fontSize = 13.sp) } }
        androidx.compose.foundation.lazy.LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(bottom = 28.dp)) {
            item {
                Surface(shape = RoundedCornerShape(28.dp), color = accents[accentIndex].copy(alpha = .14f), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        Text("iOS-inspired look", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = fg)
                        Text("Preview your visual style", color = fg.copy(alpha = .7f), fontSize = 13.sp)
                        Spacer(Modifier.height(16.dp))
                        Box(Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(22.dp)).background(Brush.linearGradient(listOf(Color(0xFFB7D7FF), accents[accentIndex], Color(0xFFF6D6E8)))), contentAlignment = Alignment.Center) {
                            Row(horizontalArrangement = Arrangement.spacedBy(13.dp)) { repeat(4) { Box(Modifier.size(45.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = .8f)), contentAlignment = Alignment.Center) { Icon(listOf(Icons.Default.Call, Icons.Default.Chat, Icons.Default.MusicNote, Icons.Default.Photo)[it], null, tint = accents[accentIndex]) } } }
                        }
                    }
                }
            }
            item { SettingCard("Default home app", "Choose iOS Transformation in Android Home settings", Icons.Default.Home, fg, onHomeSettings) }
            item { SettingCard("Choose wallpaper", "Import a photo from your device", Icons.Default.Wallpaper, fg, onWallpaper) }
            if (hasWallpaper) item { SettingCard("Apply wallpaper", "Set the selected photo as system wallpaper", Icons.Default.CheckCircle, fg, onApplyWallpaper) }
            item {
                Surface(shape = RoundedCornerShape(22.dp), color = if (dark) Color(0xFF202635) else Color.White) {
                    Column(Modifier.fillMaxWidth().padding(17.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.DarkMode, null, tint = fg); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text("Dark appearance", color = fg, fontWeight = FontWeight.SemiBold); Text("Change the launcher interface", color = fg.copy(alpha = .6f), fontSize = 12.sp) }; Switch(checked = dark, onCheckedChange = onDark) }
                        Spacer(Modifier.height(18.dp)); Text("Accent colour", color = fg, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(13.dp)) { accents.forEachIndexed { index, color -> Box(Modifier.size(34.dp).clip(CircleShape).background(color).clickable { onAccent(index) }, contentAlignment = Alignment.Center) { if (accentIndex == index) Icon(Icons.Default.Check, null, tint = Color.White) } } }
                    }
                }
            }
            item { SettingCard("Android settings", "Manage permissions and system features", Icons.Default.Settings, fg, onSystemSettings) }
            item { Text("Privacy: settings are stored on this device. This starter version does not upload your photos or app list.", color = fg.copy(alpha = .65f), fontSize = 12.sp, modifier = Modifier.padding(horizontal = 4.dp)) }
        }
    }
}

@Composable
fun SettingCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, foreground: Color, onClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(22.dp), color = Color.White, modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(43.dp).clip(RoundedCornerShape(13.dp)).background(Blue.copy(alpha = .12f)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Blue) }; Spacer(Modifier.width(13.dp)); Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.SemiBold, color = foreground); Text(subtitle, fontSize = 12.sp, color = foreground.copy(alpha = .62f)) }; Icon(Icons.Default.ChevronRight, null, tint = foreground.copy(alpha = .45f)) }
    }
}

@Composable
fun ControlCenter(dark: Boolean, onDismiss: () -> Unit, onSetting: (String) -> Unit) {
    val fg = if (dark) Color.White else Ink
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = RoundedCornerShape(32.dp), color = if (dark) Color(0xF2222632) else Color(0xF2F7F8FC), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Text("Control Center", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = fg, modifier = Modifier.weight(1f)); IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = fg) } }
                Text("Shortcuts open Android's real settings where direct control is restricted.", color = fg.copy(alpha = .65f), fontSize = 12.sp)
                Spacer(Modifier.height(18.dp))
                val rows = listOf(listOf("Wi-Fi" to Icons.Default.Wifi, "Bluetooth" to Icons.Default.Bluetooth), listOf("Airplane mode" to Icons.Default.Flight, "Do Not Disturb" to Icons.Default.DoNotDisturbOn), listOf("Brightness" to Icons.Default.Brightness6, "Sound" to Icons.Default.VolumeUp))
                rows.forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) { row.forEach { (label, icon) -> Surface(shape = RoundedCornerShape(20.dp), color = if (dark) Color(0xFF343A4A) else Color.White, modifier = Modifier.weight(1f).height(94.dp).clickable { onSetting(label) }) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) { Icon(icon, null, tint = Blue, modifier = Modifier.size(25.dp)); Text(label, color = fg, fontWeight = FontWeight.Medium, fontSize = 13.sp) } } } } }
                Button(onClick = { onSetting("Flashlight") }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.FlashlightOn, null); Spacer(Modifier.width(8.dp)); Text("Open more settings") }
            }
        }
    }
}
