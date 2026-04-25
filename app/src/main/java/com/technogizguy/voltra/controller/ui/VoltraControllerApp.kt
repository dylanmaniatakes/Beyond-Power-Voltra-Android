package com.technogizguy.voltra.controller.ui

import android.graphics.Bitmap
import android.util.Log
import android.widget.NumberPicker
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Shapes
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.technogizguy.voltra.controller.R
import com.technogizguy.voltra.controller.AccentColor
import com.technogizguy.voltra.controller.ControlModeUi
import com.technogizguy.voltra.controller.CustomCurvePreset
import com.technogizguy.voltra.controller.HttpGatewayPreferences
import com.technogizguy.voltra.controller.IsometricForceSample
import com.technogizguy.voltra.controller.LocalPreferences
import com.technogizguy.voltra.controller.MqttPreferences
import com.technogizguy.voltra.controller.PreparedStartupImage
import com.technogizguy.voltra.controller.RuntimePermissionState
import com.technogizguy.voltra.controller.StartupImageCropTransform
import com.technogizguy.voltra.controller.loadStartupImageBitmap
import com.technogizguy.voltra.controller.prepareStartupImage
import com.technogizguy.voltra.controller.VoltraViewModel
import com.technogizguy.voltra.controller.WeightPreset
import com.technogizguy.voltra.controller.WeightPresetScope
import com.technogizguy.voltra.controller.WorkoutHistoryEntry
import com.technogizguy.voltra.controller.WEIGHT_INCREMENT_OPTIONS
import com.technogizguy.voltra.controller.computeIsometricMetrics
import com.technogizguy.voltra.controller.http.HttpGatewayConnectionState
import com.technogizguy.voltra.controller.http.HttpGatewayState
import com.technogizguy.voltra.controller.mqtt.MqttPublisherConnectionState
import com.technogizguy.voltra.controller.mqtt.MqttPublisherState
import com.technogizguy.voltra.controller.model.VoltraControlCommand
import com.technogizguy.voltra.controller.model.RawVoltraFrame
import com.technogizguy.voltra.controller.model.VoltraCommandResult
import com.technogizguy.voltra.controller.model.VoltraCommandStatus
import com.technogizguy.voltra.controller.model.VoltraConnectionState
import com.technogizguy.voltra.controller.model.VoltraGattSnapshot
import com.technogizguy.voltra.controller.model.VoltraProtocolStatus
import com.technogizguy.voltra.controller.model.VoltraReading
import com.technogizguy.voltra.controller.model.VoltraScanResult
import com.technogizguy.voltra.controller.model.VoltraSessionState
import com.technogizguy.voltra.controller.model.WeightUnit
import com.technogizguy.voltra.controller.protocol.VoltraControlFrames
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private fun voltraColorScheme(
    primary: Color,
    onPrimary: Color,
    primaryContainer: Color,
    onPrimaryContainer: Color,
) = darkColorScheme(
    primary = primary,
    onPrimary = onPrimary,
    primaryContainer = primaryContainer,
    onPrimaryContainer = onPrimaryContainer,
    secondary = Color(0xFF65D6C4),
    onSecondary = Color(0xFF00201B),
    secondaryContainer = Color(0xFF123B35),
    onSecondaryContainer = Color(0xFFA4F2E5),
    tertiary = Color(0xFFFFC857),
    onTertiary = Color(0xFF271900),
    background = Color(0xFF0B0D0C),
    onBackground = Color(0xFFE7EDE8),
    surface = Color(0xFF171A18),
    onSurface = Color(0xFFE7EDE8),
    surfaceVariant = Color(0xFF232824),
    onSurfaceVariant = Color(0xFFC3CAC2),
    outline = Color(0xFF454D45),
    error = Color(0xFFFF5449),
    onError = Color(0xFF330000),
)

private fun colorSchemeFor(accent: AccentColor) = when (accent) {
    AccentColor.WHITE         -> voltraColorScheme(Color(0xFFF5F7FA), Color(0xFF101417), Color(0xFF3A4349), Color(0xFFF3F6F8))
    AccentColor.DARK_BLUE     -> voltraColorScheme(Color(0xFF3E63DD), Color.White, Color(0xFF12306E), Color(0xFFDCE6FF))
    AccentColor.ORANGE        -> voltraColorScheme(Color(0xFFFFA24C), Color(0xFF3B2200), Color(0xFF6A3B00), Color(0xFFFFDDBB))
    AccentColor.RAVENS_PURPLE -> voltraColorScheme(Color(0xFF6B4ACB), Color.White, Color(0xFF241773), Color(0xFFE9DDFF))
    AccentColor.LIME_GREEN    -> voltraColorScheme(Color(0xFFC6FF2E), Color(0xFF132000), Color(0xFF31460A), Color(0xFFE5FF9F))
    AccentColor.ELECTRIC_BLUE -> voltraColorScheme(Color(0xFF82B4FF), Color(0xFF003063), Color(0xFF004792), Color(0xFFD7E3FF))
    AccentColor.EMBER_RED     -> voltraColorScheme(Color(0xFFFFB4AB), Color(0xFF690005), Color(0xFF93000A), Color(0xFFFFDAD6))
    AccentColor.GOLD          -> voltraColorScheme(Color(0xFFFFCC02), Color(0xFF3D2F00), Color(0xFF594400), Color(0xFFFFE878))
    AccentColor.RED           -> voltraColorScheme(Color(0xFFFF6E6E), Color(0xFF640000), Color(0xFF920000), Color(0xFFFFDAD4))
}

private fun accentPrimaryColor(accent: AccentColor): Color = when (accent) {
    AccentColor.WHITE         -> Color(0xFFF5F7FA)
    AccentColor.DARK_BLUE     -> Color(0xFF3E63DD)
    AccentColor.ORANGE        -> Color(0xFFFFA24C)
    AccentColor.RAVENS_PURPLE -> Color(0xFF6B4ACB)
    AccentColor.LIME_GREEN    -> Color(0xFFC6FF2E)
    AccentColor.ELECTRIC_BLUE -> Color(0xFF82B4FF)
    AccentColor.EMBER_RED     -> Color(0xFFFFB4AB)
    AccentColor.GOLD          -> Color(0xFFFFCC02)
    AccentColor.RED           -> Color(0xFFFF6E6E)
}

private data class ControlAccentPalette(
    val accent: Color,
    val onAccent: Color,
    val accentContainer: Color,
    val onAccentContainer: Color,
)

private data class PowerWorkoutHistoryEntry(
    val index: Int,
    val setCount: Int?,
    val repCount: Int?,
    val peakForceLb: Double?,
    val peakPowerWatts: Int?,
    val timeToPeakMillis: Long?,
)

private fun controlAccentFor(mode: ControlModeUi): ControlAccentPalette = when (mode) {
    ControlModeUi.WEIGHT_TRAINING -> ControlAccentPalette(
        accent = Color(0xFFC6FF2E),
        onAccent = Color(0xFF122000),
        accentContainer = Color(0xFF31460A),
        onAccentContainer = Color(0xFFE5FF9F),
    )
    ControlModeUi.RESISTANCE_BAND -> ControlAccentPalette(
        accent = Color(0xFF65D6C4),
        onAccent = Color(0xFF00201B),
        accentContainer = Color(0xFF123B35),
        onAccentContainer = Color(0xFFA4F2E5),
    )
    ControlModeUi.DAMPER -> ControlAccentPalette(
        accent = Color(0xFFD5FF3F),
        onAccent = Color(0xFF172300),
        accentContainer = Color(0xFF334800),
        onAccentContainer = Color(0xFFF0FFAE),
    )
    ControlModeUi.ISOKINETIC -> ControlAccentPalette(
        accent = Color(0xFFFFCC02),
        onAccent = Color(0xFF3D2F00),
        accentContainer = Color(0xFF594400),
        onAccentContainer = Color(0xFFFFE878),
    )
    ControlModeUi.ISOMETRIC_TEST -> ControlAccentPalette(
        accent = Color(0xFFFFA94D),
        onAccent = Color(0xFF3C2300),
        accentContainer = Color(0xFF5A3700),
        onAccentContainer = Color(0xFFFFDCB1),
    )
    ControlModeUi.CUSTOM_CURVE -> ControlAccentPalette(
        accent = Color(0xFF7FE36C),
        onAccent = Color(0xFF0F2107),
        accentContainer = Color(0xFF1F4315),
        onAccentContainer = Color(0xFFCFF8B9),
    )
    ControlModeUi.ROWING -> ControlAccentPalette(
        accent = Color(0xFF6ED3FF),
        onAccent = Color(0xFF002432),
        accentContainer = Color(0xFF123E4E),
        onAccentContainer = Color(0xFFBFEFFF),
    )
}

private val LocalControlAccent = staticCompositionLocalOf { controlAccentFor(ControlModeUi.WEIGHT_TRAINING) }

private fun snapWeight(value: Double, minLoad: Double, maxLoad: Double, step: Double): Double {
    val bounded = value.coerceIn(minLoad, maxLoad)
    if (bounded <= minLoad + step / 2.0) return minLoad
    if (bounded >= maxLoad - step / 2.0) return maxLoad
    val intervals = ((bounded - minLoad) / step).roundToInt()
    return (minLoad + intervals * step).coerceIn(minLoad, maxLoad)
}

private fun sliderSteps(minLoad: Double, maxLoad: Double, step: Double): Int {
    val intervals = ((maxLoad - minLoad) / step).roundToInt()
    return (intervals - 1).coerceAtLeast(0)
}

private fun formatWeightValue(value: Double): String {
    val roundedTenths = (value * 10.0).roundToInt() / 10.0
    return if (roundedTenths % 1.0 == 0.0) {
        roundedTenths.roundToInt().toString()
    } else {
        roundedTenths.toString()
    }
}

private fun poundsToUnit(pounds: Double, unit: WeightUnit): Double = when (unit) {
    WeightUnit.LB -> pounds
    WeightUnit.KG -> pounds / UI_LB_PER_KG
}

private fun convertWeightValue(value: Double, from: WeightUnit, to: WeightUnit): Double {
    if (from == to) return value
    return when {
        from == WeightUnit.LB && to == WeightUnit.KG -> value / UI_LB_PER_KG
        from == WeightUnit.KG && to == WeightUnit.LB -> value * UI_LB_PER_KG
        else -> value
    }
}

private fun formatElapsedClock(elapsedMillis: Long): String {
    val totalSeconds = ((elapsedMillis + 500L) / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

private fun formatPowerWorkoutSummary(
    peakForceLb: Double?,
    peakPowerWatts: Int?,
    timeToPeakMillis: Long?,
): String? {
    val parts = listOfNotNull(
        peakPowerWatts?.let { "Peak Power ${it} W" },
        peakForceLb?.let { "Peak ${formatWeightValue(it)} lb" },
        timeToPeakMillis?.let { "TTP ${formatSecondsClock(it)}" },
    )
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" - ")
}

private const val UI_LB_PER_KG = 2.2046226218
private const val STARTUP_UI_TAG = "VoltraStartupDebug"
private const val STARTUP_IMAGE_UPLOAD_TIMEOUT_MILLIS = 120_000L
private val StartupImageChunkProgressRegex = Regex("""startup image chunk\s+(\d+)/(\d+)""", RegexOption.IGNORE_CASE)

private data class StartupImageTransferUiState(
    val title: String,
    val detail: String,
    val progress: Float?,
)

private fun damperFactorForLevel(level: Int): Int {
    return when (level.coerceIn(1, 10)) {
        1 -> 5
        2 -> 8
        3 -> 11
        4 -> 14
        5 -> 17
        6 -> 21
        7 -> 30
        8 -> 33
        9 -> 41
        else -> 50
    }
}

private fun formatDamperFactor(level: Double): String {
    return damperFactorForLevel(level.roundToInt()).toString()
}

private fun displayedSetCount(sets: Int?, reps: Int?): Int? {
    val safeSets = sets ?: return null
    val safeReps = reps ?: 0
    return if (safeReps == 0) {
        (safeSets - 1).coerceAtLeast(0)
    } else {
        safeSets.coerceAtLeast(0)
    }
}

private fun nextWeightIncrement(current: Int): Int {
    val currentIndex = WEIGHT_INCREMENT_OPTIONS.indexOf(current)
    return WEIGHT_INCREMENT_OPTIONS[(currentIndex + 1).coerceAtLeast(0) % WEIGHT_INCREMENT_OPTIONS.size]
}

private val VoltraShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(8.dp),
)

private enum class Route(val path: String, val label: String) {
    HOME("home", "Home"),
    CONTROL("control", "Control"),
    DIAGNOSTICS("diagnostics", "Logs"),
    CONNECT("connect", "Connect"),
    MORE("more", "More"),
}

private enum class ResistanceExperienceOption(val label: String) {
    STANDARD("Standard"),
    INTENSE("Intense"),
}

private enum class ResistanceModeOption(val label: String) {
    STANDARD("Standard"),
    INVERSE("Inverse"),
}

private enum class ResistanceCurveOption(val label: String) {
    POWER_LAW("Power Law"),
    LOGARITHM("Logarithm"),
}

private enum class ProgressiveLengthOption(val label: String) {
    BAND_LENGTH("Band Length"),
    ROM("ROM"),
}

private enum class ResistanceBandSettingsSection(val label: String) {
    EXPERIENCE("Experience"),
    MODE("Mode"),
    CURVE("Curve"),
    BAND_LENGTH("Length"),
}

private enum class IsokineticMenuOption(val label: String) {
    ISOKINETIC("Isokinetic"),
    CONSTANT_RESISTANCE("Constant Resistance"),
}

private fun IsokineticMenuOption.paramValue(): Int = when (this) {
    IsokineticMenuOption.ISOKINETIC -> VoltraControlFrames.ISOKINETIC_MENU_ISOKINETIC
    IsokineticMenuOption.CONSTANT_RESISTANCE -> VoltraControlFrames.ISOKINETIC_MENU_CONSTANT_RESISTANCE
}

private fun isokineticMenuFromParam(mode: Int?): IsokineticMenuOption? = when (mode) {
    VoltraControlFrames.ISOKINETIC_MENU_ISOKINETIC -> IsokineticMenuOption.ISOKINETIC
    VoltraControlFrames.ISOKINETIC_MENU_CONSTANT_RESISTANCE -> IsokineticMenuOption.CONSTANT_RESISTANCE
    else -> null
}

private fun buildIsokineticSpeedOptions(): List<Double> {
    val values = mutableListOf(0.0)
    var current = 0.10
    while (current <= 2.0001) {
        values += ((current * 100.0).roundToInt() / 100.0)
        current += 0.05
    }
    return values
}

private fun formatSpeedValue(value: Double): String {
    return if (value <= 0.0) "Auto" else "${formatWeightValue(value)} m/s"
}

private fun formatSpeedOptionLabel(value: Double): String {
    if (value <= 0.0) return "Auto"
    val hundredths = (value * 100.0).roundToInt()
    return if (hundredths % 10 == 0) {
        String.format(Locale.US, "%.1f", value)
    } else {
        String.format(Locale.US, "%.2f", value)
    }
}

private fun isokineticSpeedMmSToOption(speedMmS: Int?): Double? = when {
    speedMmS == null -> null
    speedMmS <= 0 -> 0.0
    else -> speedMmS / 1000.0
}

private fun closestIsokineticSpeedIndex(options: List<Double>, speedMmS: Int?): Int? {
    val target = isokineticSpeedMmSToOption(speedMmS) ?: return null
    return options.indices.minByOrNull { index ->
        kotlin.math.abs(options[index] - target)
    }
}

private fun isokineticSpeedMmSForOption(speedValue: Double): Int {
    return if (speedValue <= 0.0) {
        VoltraControlFrames.AUTO_ISOKINETIC_SPEED_MM_S
    } else {
        (speedValue * 1000.0).roundToInt()
            .coerceIn(
                VoltraControlFrames.MIN_ISOKINETIC_SPEED_MM_S,
                VoltraControlFrames.MAX_ISOKINETIC_SPEED_MM_S,
            )
    }
}

private fun formatInchesValue(value: Double): String = "${formatWeightValue(value)} in"

private data class AdaptiveLayoutInfo(
    val isTablet: Boolean,
    val isLandscape: Boolean,
    val useWidePanels: Boolean,
)

@Composable
private fun AdaptiveScreenScaffold(
    maxContentWidth: Dp = 1160.dp,
    content: @Composable (layout: AdaptiveLayoutInfo) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 840.dp
        val isLandscape = maxWidth > maxHeight
        val useWidePanels = maxWidth >= 900.dp && isLandscape
        val layout = AdaptiveLayoutInfo(
            isTablet = isTablet,
            isLandscape = isLandscape,
            useWidePanels = useWidePanels,
        )
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = maxContentWidth),
            ) {
                content(layout)
            }
        }
    }
}

private fun stepValues(min: Int, max: Int, step: Int): List<Int> {
    val values = mutableListOf<Int>()
    var current = min
    while (current <= max) {
        values += current
        current += step
    }
    return values
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoltraControllerApp(
    viewModel: VoltraViewModel,
    permissionState: RuntimePermissionState,
    onRequestPermissions: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val mqttState by viewModel.mqttState.collectAsStateWithLifecycle()
    val httpGatewayState by viewModel.httpGatewayState.collectAsStateWithLifecycle()
    val scanResults by viewModel.scanResults.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val weightPresets by viewModel.weightPresets.collectAsStateWithLifecycle()
    val customCurvePresets by viewModel.customCurvePresets.collectAsStateWithLifecycle()
    val workoutHistory by viewModel.workoutHistory.collectAsStateWithLifecycle()
    val showAllDevices by viewModel.showAllDevices.collectAsStateWithLifecycle()
    val selectedControlMode by viewModel.selectedControlMode.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val context = LocalContext.current
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentRoute = currentDestination?.route
    var showOverflowMenu by remember { mutableStateOf(false) }

    LaunchedEffect(currentRoute, state.connectionState) {
        if (currentRoute == Route.CONNECT.path && state.connectionState == VoltraConnectionState.CONNECTED) {
            navController.navigate(Route.HOME.path) {
                launchSingleTop = true
            }
        }
    }

    MaterialTheme(colorScheme = colorSchemeFor(preferences.accentColor), shapes = VoltraShapes) {
        val versionLabel = stringResource(id = R.string.app_version_label)
        val canNavigateBack = currentRoute == Route.MORE.path || currentRoute == Route.DIAGNOSTICS.path
        val showOverflow = !canNavigateBack
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Voltra Controller", fontWeight = FontWeight.Bold)
                            Text(
                                versionLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                    navigationIcon = if (canNavigateBack) {
                        {
                            IconButton(
                                onClick = {
                                    if (!navController.popBackStack()) {
                                        navController.navigate(Route.HOME.path) {
                                            launchSingleTop = true
                                        }
                                    }
                                },
                            ) {
                                Text("<", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        {}
                    },
                    actions = {
                        if (showOverflow) {
                            Box {
                                IconButton(onClick = { showOverflowMenu = true }) {
                                    Text("...", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                                DropdownMenu(
                                    expanded = showOverflowMenu,
                                    onDismissRequest = { showOverflowMenu = false },
                                ) {
                                    if (currentRoute != Route.CONNECT.path) {
                                        DropdownMenuItem(
                                            text = { Text(Route.CONNECT.label) },
                                            onClick = {
                                                showOverflowMenu = false
                                                navController.navigate(Route.CONNECT.path) {
                                                    launchSingleTop = true
                                                }
                                            },
                                        )
                                    }
                                    if (currentRoute != Route.MORE.path) {
                                        DropdownMenuItem(
                                            text = { Text(Route.MORE.label) },
                                            onClick = {
                                                showOverflowMenu = false
                                                navController.navigate(Route.MORE.path) {
                                                    launchSingleTop = true
                                                }
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    },
                )
            },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Route.CONNECT.path,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(padding),
            ) {
                composable(Route.CONNECT.path) {
                    ConnectScreen(
                        permissionState = permissionState,
                        showAllDevices = showAllDevices,
                        scanResults = scanResults,
                        state = state,
                        preferences = preferences,
                        onRequestPermissions = onRequestPermissions,
                        onStartScan = viewModel::startScan,
                        onStopScan = viewModel::stopScan,
                        onShowAllDevices = viewModel::setShowAllDevices,
                        onConnect = viewModel::connect,
                        onConnectLastDevice = viewModel::connectLastDevice,
                        onDisconnect = viewModel::disconnect,
                    )
                }
                composable(Route.HOME.path) {
                    HomeScreen(
                        state = state,
                        developerModeEnabled = preferences.developerModeEnabled,
                        onWeightTraining = {
                            viewModel.selectControlMode(ControlModeUi.WEIGHT_TRAINING)
                            viewModel.setStrengthMode()
                            navController.navigate(Route.CONTROL.path) {
                                launchSingleTop = true
                            }
                        },
                        onResistanceBand = {
                            viewModel.selectControlMode(ControlModeUi.RESISTANCE_BAND)
                            viewModel.enterResistanceBandMode()
                            navController.navigate(Route.CONTROL.path) {
                                launchSingleTop = true
                            }
                        },
                        onDamper = {
                            viewModel.selectControlMode(ControlModeUi.DAMPER)
                            viewModel.enterDamperMode()
                            navController.navigate(Route.CONTROL.path) {
                                launchSingleTop = true
                            }
                        },
                        onIsokinetic = {
                            viewModel.selectControlMode(ControlModeUi.ISOKINETIC)
                            viewModel.enterIsokineticMode()
                            navController.navigate(Route.CONTROL.path) {
                                launchSingleTop = true
                            }
                        },
                        onIsometric = {
                            viewModel.selectControlMode(ControlModeUi.ISOMETRIC_TEST)
                            viewModel.enterIsometricMode()
                            navController.navigate(Route.CONTROL.path) {
                                launchSingleTop = true
                            }
                        },
                        onCustomCurve = {
                            viewModel.selectControlMode(ControlModeUi.CUSTOM_CURVE)
                            navController.navigate(Route.CONTROL.path) {
                                launchSingleTop = true
                            }
                        },
                        onRowing = {
                            viewModel.selectControlMode(ControlModeUi.ROWING)
                            viewModel.enterRowMode()
                            navController.navigate(Route.CONTROL.path) {
                                launchSingleTop = true
                            }
                        },
                        onDisconnect = viewModel::disconnect,
                    )
                }
                composable(Route.CONTROL.path) {
                    ControlScreen(
                        state = state,
                        preferences = preferences,
                        weightPresets = weightPresets,
                        customCurvePresets = customCurvePresets,
                        selectedMode = selectedControlMode,
                        onEnterWeightTraining = viewModel::setStrengthMode,
                        onEnterDamper = viewModel::enterDamperMode,
                        onEnterIsokinetic = viewModel::enterIsokineticMode,
                        onEnterIsometric = viewModel::enterIsometricMode,
                        onEnterCustomCurve = viewModel::enterCustomCurveMode,
                        onEnterRowing = viewModel::enterRowMode,
                        onStartRowing = viewModel::startRow,
                        onApplyCustomCurve = viewModel::applyCustomCurve,
                        onRefreshModeFeatureStatus = viewModel::refreshModeFeatureStatus,
                        onSetTarget = viewModel::setTargetLoad,
                        onSetDamperLevel = viewModel::setDamperLevel,
                        onSetAssistMode = viewModel::setAssistMode,
                        onSetChainsWeight = viewModel::setChainsWeight,
                        onSetEccentricWeight = viewModel::setEccentricWeight,
                        onSetInverseChains = viewModel::setInverseChainsEnabled,
                        onEnterResistanceBand = viewModel::enterResistanceBandMode,
                        onSetResistanceExperience = viewModel::setResistanceExperience,
                        onSetResistanceMode = viewModel::setResistanceBandInverse,
                        onSetResistanceCurve = viewModel::setResistanceBandCurveLogarithm,
                        onSetResistanceBandForce = viewModel::setResistanceBandMaxForce,
                        onSetResistanceBandByRangeOfMotion = viewModel::setResistanceBandByRangeOfMotion,
                        onSetResistanceBandLength = viewModel::setResistanceBandLengthInches,
                        onSetIsokineticMenu = viewModel::setIsokineticMenu,
                        onSetIsokineticTargetSpeed = viewModel::setIsokineticTargetSpeedMmS,
                        onSetIsokineticSpeedLimit = viewModel::setIsokineticSpeedLimitMmS,
                        onSetIsokineticConstantResistance = viewModel::setIsokineticConstantResistance,
                        onSetIsokineticMaxEccentricLoad = viewModel::setIsokineticMaxEccentricLoad,
                        onSetRowingResistanceLevel = viewModel::setRowingResistanceLevel,
                        onSetRowingSimulatedWearLevel = viewModel::setRowingSimulatedWearLevel,
                        onLoadResistanceBand = viewModel::loadResistanceBand,
                        onTriggerCableLength = viewModel::triggerCableLengthMode,
                        onSetUnit = viewModel::setUnit,
                        onSetWeightIncrement = viewModel::setWeightIncrement,
                        onSaveWeightPreset = viewModel::saveWeightPreset,
                        onApplyWeightPreset = viewModel::applyWeightPreset,
                        onSaveCustomCurvePreset = viewModel::saveCustomCurvePreset,
                        onDeleteCustomCurvePreset = viewModel::deleteCustomCurvePreset,
                        onLoad = viewModel::load,
                        onUnload = viewModel::unload,
                        onReturnHome = {
                            navController.navigate(Route.HOME.path) {
                                launchSingleTop = true
                            }
                        },
                        onExitWorkout = {
                            viewModel.exitWorkout()
                            navController.navigate(Route.HOME.path) {
                                launchSingleTop = true
                            }
                        },
                    )
                }
                composable(Route.DIAGNOSTICS.path) {
                    DiagnosticsScreen(
                        state = state,
                        onEnableNotifications = viewModel::enableCandidateNotifications,
                        onReadVoltraCharacteristics = viewModel::readVoltraCharacteristics,
                        onReadOnlyHandshakeProbe = viewModel::runReadOnlyHandshakeProbe,
                    )
                }
                composable(Route.MORE.path) {
                    MoreFeaturesScreen(
                        state = state,
                        preferences = preferences,
                        weightPresets = weightPresets,
                        workoutHistory = workoutHistory,
                        mqttState = mqttState,
                        httpGatewayState = httpGatewayState,
                        onSetAccentColor = viewModel::setAccentColor,
                        onSetInstantWeightApplyDefault = viewModel::setInstantWeightApplyDefault,
                        onSetDeveloperModeEnabled = viewModel::setDeveloperModeEnabled,
                        onSetMqttEnabled = viewModel::setMqttEnabled,
                        onSaveMqttSettings = viewModel::saveMqttSettings,
                        onSetHttpGatewayEnabled = viewModel::setHttpGatewayEnabled,
                        onSaveHttpGatewaySettings = viewModel::saveHttpGatewaySettings,
                        onRotateHttpGatewayAccessKey = viewModel::rotateHttpGatewayAccessKey,
                        onPublishMqttNow = viewModel::publishMqttNow,
                        onSetDeviceName = viewModel::setDeviceName,
                        onUploadStartupImage = viewModel::uploadStartupImage,
                        onDeleteWeightPreset = viewModel::deleteWeightPreset,
                        onShareWorkoutHistory = { viewModel.shareWorkoutHistoryCsv(context) },
                        onClearWorkoutHistory = viewModel::clearWorkoutHistory,
                        onOpenLogs = {
                            navController.navigate(Route.DIAGNOSTICS.path) {
                                launchSingleTop = true
                            }
                        },
                        onShareDiagnostics = { viewModel.shareDiagnostics(context) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectScreen(
    permissionState: RuntimePermissionState,
    showAllDevices: Boolean,
    scanResults: List<VoltraScanResult>,
    state: VoltraSessionState,
    preferences: LocalPreferences,
    onRequestPermissions: () -> Unit,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onShowAllDevices: (Boolean) -> Unit,
    onConnect: (VoltraScanResult) -> Unit,
    onConnectLastDevice: () -> Unit,
    onDisconnect: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = screenPadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            DeviceStatusCard(
                state = state,
                onDisconnect = onDisconnect,
            )
        }
        item {
            PermissionCard(
                permissionState = permissionState,
                onRequestPermissions = onRequestPermissions,
            )
        }
        item {
            SectionHeader(
                title = "Nearby Devices",
                subtitle = "Scan with the VOLTRA awake and close by.",
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        if (permissionState.requiredGranted) {
                            onStartScan()
                        } else {
                            onRequestPermissions()
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (state.connectionState == VoltraConnectionState.SCANNING) "Rescan" else "Scan")
                }
                OutlinedButton(
                    onClick = onStopScan,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Stop")
                }
            }
        }
        item {
            ToggleRow(
                checked = showAllDevices,
                label = "Show diagnostic BLE devices",
                detail = "Keep this on if VOLTRA advertising changes.",
                onCheckedChange = onShowAllDevices,
            )
        }
        if (preferences.lastDeviceId != null) {
            item {
                MetricCard {
                    Text("Last Device", style = MaterialTheme.typography.labelLarge)
                    DetailRow("Name", preferences.lastDeviceName ?: preferences.lastDeviceId)
                    DetailRow("Address", preferences.lastDeviceId)
                    OutlinedButton(
                        onClick = {
                            if (permissionState.requiredGranted) {
                                onConnectLastDevice()
                            } else {
                                onRequestPermissions()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Connect Saved Device")
                    }
                }
            }
        }
        if (scanResults.isEmpty()) {
            item {
                EmptyState("No devices found yet.")
            }
        } else {
            items(scanResults, key = { it.device.id }) { result ->
                DeviceRow(result = result, onConnect = { onConnect(result) })
            }
        }
    }
}

@Composable
private fun HomeScreen(
    state: VoltraSessionState,
    developerModeEnabled: Boolean,
    onWeightTraining: () -> Unit,
    onResistanceBand: () -> Unit,
    onDamper: () -> Unit,
    onIsokinetic: () -> Unit,
    onIsometric: () -> Unit,
    onCustomCurve: () -> Unit,
    onRowing: () -> Unit,
    onDisconnect: () -> Unit,
) {
    AdaptiveScreenScaffold { layout ->
        val sectionSpacing = if (layout.isTablet) 14.dp else 12.dp
        if (layout.useWidePanels) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(screenPadding()),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(0.42f),
                    verticalArrangement = Arrangement.spacedBy(sectionSpacing),
                ) {
                    DeviceStatusCard(
                        state = state,
                        onDisconnect = onDisconnect,
                    )
                    SectionHeader(
                        title = "Mode",
                        subtitle = "Pick the VOLTRA profile you want to drive.",
                    )
                }
                Column(
                    modifier = Modifier.weight(0.58f),
                    verticalArrangement = Arrangement.spacedBy(sectionSpacing),
                ) {
                    ModeGrid(
                        controlReady = state.controlCommandsEnabled,
                        isWideLayout = true,
                        onWeightTraining = onWeightTraining,
                        onResistanceBand = onResistanceBand,
                        onDamper = onDamper,
                        onIsokinetic = onIsokinetic,
                        onIsometric = onIsometric,
                        onCustomCurve = onCustomCurve,
                        onRowing = onRowing,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = if (layout.isTablet) screenPadding() else screenPadding(top = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(sectionSpacing),
            ) {
                item {
                    DeviceStatusCard(
                        state = state,
                        onDisconnect = onDisconnect,
                    )
                }
                item {
                    SectionHeader(
                        title = "Mode",
                        subtitle = "Pick the VOLTRA profile you want to drive.",
                    )
                }
                item {
                    ModeGrid(
                        controlReady = state.controlCommandsEnabled,
                        isWideLayout = layout.isTablet,
                        onWeightTraining = onWeightTraining,
                        onResistanceBand = onResistanceBand,
                        onDamper = onDamper,
                        onIsokinetic = onIsokinetic,
                        onIsometric = onIsometric,
                        onCustomCurve = onCustomCurve,
                        onRowing = onRowing,
                    )
                }
            }
        }
    }
}

@Composable
private fun ControlScreen(
    state: VoltraSessionState,
    preferences: LocalPreferences,
    weightPresets: List<WeightPreset>,
    customCurvePresets: List<CustomCurvePreset>,
    selectedMode: ControlModeUi,
    onEnterWeightTraining: () -> Unit,
    onEnterDamper: () -> Unit,
    onEnterIsokinetic: () -> Unit,
    onEnterIsometric: () -> Unit,
    onEnterCustomCurve: () -> Unit,
    onEnterRowing: () -> Unit,
    onStartRowing: (Int?) -> Unit,
    onApplyCustomCurve: (List<Float>, Int, Int, Int) -> Unit,
    onRefreshModeFeatureStatus: () -> Unit,
    onSetTarget: (Double) -> Unit,
    onSetDamperLevel: (Int) -> Unit,
    onSetAssistMode: (Boolean) -> Unit,
    onSetChainsWeight: (Double) -> Unit,
    onSetEccentricWeight: (Double) -> Unit,
    onSetInverseChains: (Boolean) -> Unit,
    onEnterResistanceBand: () -> Unit,
    onSetResistanceExperience: (Boolean) -> Unit,
    onSetResistanceMode: (Boolean) -> Unit,
    onSetResistanceCurve: (Boolean) -> Unit,
    onSetResistanceBandForce: (Double) -> Unit,
    onSetResistanceBandByRangeOfMotion: (Boolean) -> Unit,
    onSetResistanceBandLength: (Double) -> Unit,
    onSetIsokineticMenu: (Int) -> Unit,
    onSetIsokineticTargetSpeed: (Int) -> Unit,
    onSetIsokineticSpeedLimit: (Int) -> Unit,
    onSetIsokineticConstantResistance: (Double) -> Unit,
    onSetIsokineticMaxEccentricLoad: (Double) -> Unit,
    onSetRowingResistanceLevel: (Int) -> Unit,
    onSetRowingSimulatedWearLevel: (Int) -> Unit,
    onLoadResistanceBand: () -> Unit,
    onTriggerCableLength: () -> Unit,
    onSetUnit: (WeightUnit) -> Unit,
    onSetWeightIncrement: (Int) -> Unit,
    onSaveWeightPreset: (String, WeightPresetScope, Double) -> Unit,
    onApplyWeightPreset: (WeightPreset) -> Unit,
    onSaveCustomCurvePreset: (String, List<Float>, Int, Int, Int) -> Unit,
    onDeleteCustomCurvePreset: (String) -> Unit,
    onLoad: () -> Unit,
    onUnload: () -> Unit,
    onReturnHome: () -> Unit,
    onExitWorkout: () -> Unit,
) {
    val inIsokineticFamily = VoltraControlFrames.isIsokineticWorkoutState(state.safety.workoutState)
    val reportedProfile = when {
        state.safety.workoutState == VoltraControlFrames.WORKOUT_STATE_RESISTANCE_BAND -> ControlModeUi.RESISTANCE_BAND
        state.safety.workoutState == VoltraControlFrames.WORKOUT_STATE_DAMPER -> ControlModeUi.DAMPER
        state.safety.workoutState == VoltraControlFrames.WORKOUT_STATE_CUSTOM_CURVE -> ControlModeUi.CUSTOM_CURVE
        state.safety.workoutState == VoltraControlFrames.WORKOUT_STATE_ROWING -> ControlModeUi.ROWING
        inIsokineticFamily -> ControlModeUi.ISOKINETIC
        state.safety.workoutState == VoltraControlFrames.WORKOUT_STATE_ISOMETRIC -> ControlModeUi.ISOMETRIC_TEST
        else -> ControlModeUi.WEIGHT_TRAINING
    }
    val activeProfile = when {
        selectedMode != ControlModeUi.WEIGHT_TRAINING && selectedMode != reportedProfile -> selectedMode
        else -> reportedProfile
    }
    val inResistanceBand = activeProfile == ControlModeUi.RESISTANCE_BAND
    val sessionActive = state.safety.workoutState != null &&
        state.safety.workoutState != VoltraControlFrames.WORKOUT_STATE_INACTIVE
    val modeSessionMatched = when (activeProfile) {
        ControlModeUi.WEIGHT_TRAINING ->
            state.safety.workoutState == null ||
                state.safety.workoutState == VoltraControlFrames.WORKOUT_STATE_INACTIVE ||
                state.safety.workoutState == VoltraControlFrames.WORKOUT_STATE_ACTIVE
        ControlModeUi.RESISTANCE_BAND ->
            state.safety.workoutState == VoltraControlFrames.WORKOUT_STATE_RESISTANCE_BAND
        ControlModeUi.DAMPER ->
            state.safety.workoutState == VoltraControlFrames.WORKOUT_STATE_DAMPER
        ControlModeUi.ISOKINETIC ->
            inIsokineticFamily
        ControlModeUi.ISOMETRIC_TEST ->
            state.safety.workoutState == VoltraControlFrames.WORKOUT_STATE_ISOMETRIC
        ControlModeUi.CUSTOM_CURVE ->
            state.safety.workoutState == VoltraControlFrames.WORKOUT_STATE_CUSTOM_CURVE
        ControlModeUi.ROWING ->
            state.safety.workoutState == VoltraControlFrames.WORKOUT_STATE_ROWING ||
                state.reading.workoutMode?.startsWith("Rowing") == true
    }
    val modeTitle = when (activeProfile) {
        ControlModeUi.WEIGHT_TRAINING -> "Weight Training"
        ControlModeUi.RESISTANCE_BAND -> "Resistance Band"
        ControlModeUi.DAMPER -> "Damper"
        ControlModeUi.ISOKINETIC -> "Isokinetic"
        ControlModeUi.ISOMETRIC_TEST -> "Isometric Test"
        ControlModeUi.CUSTOM_CURVE -> "Custom Curve"
        ControlModeUi.ROWING -> "Rowing"
    }
    val activeProfileStatus = when {
        activeProfile == ControlModeUi.ROWING && state.reading.workoutMode?.startsWith("Rowing") == true ->
            state.reading.workoutMode ?: "Rowing is starting."
        activeProfile == ControlModeUi.ROWING && modeSessionMatched ->
            state.reading.workoutMode ?: "Rowing is ready."
        activeProfile == ControlModeUi.ROWING ->
            "Switching VOLTRA to Rowing..."
        activeProfile == ControlModeUi.CUSTOM_CURVE && modeSessionMatched ->
            state.reading.workoutMode ?: "Custom Curve is ready."
        activeProfile == ControlModeUi.CUSTOM_CURVE ->
            "Switching VOLTRA to Custom Curve..."
        modeSessionMatched -> state.reading.workoutMode ?: state.protocolStatus.displayText()
        else -> "Switching VOLTRA to $modeTitle..."
    }
    val controlAccent = controlAccentFor(activeProfile)
    val activeUnit = preferences.unit
    val minLoad = when {
        inResistanceBand -> poundsToUnit(
            VoltraControlFrames.MIN_RESISTANCE_BAND_FORCE_LB.toDouble(),
            activeUnit,
        )
        activeUnit == WeightUnit.LB -> VoltraControlFrames.MIN_TARGET_LB.toDouble()
        else -> 2.5
    }
    val maxLoad = when {
        inResistanceBand -> poundsToUnit(
            VoltraControlFrames.MAX_RESISTANCE_BAND_FORCE_LB.toDouble(),
            activeUnit,
        )
        activeUnit == WeightUnit.LB -> VoltraControlFrames.MAX_TARGET_LB.toDouble()
        else -> 90.7
    }
    val weightStep = if (inResistanceBand) 1.0 else preferences.weightIncrement.toDouble()
    val displayedTarget = if (inResistanceBand) {
        state.reading.resistanceBandMaxForceLb?.let { poundsToUnit(it, activeUnit) }
            ?: convertWeightValue(
                value = state.targetLoad.value,
                from = state.targetLoad.unit,
                to = activeUnit,
            )
    } else {
        convertWeightValue(
            value = state.targetLoad.value,
            from = state.targetLoad.unit,
            to = activeUnit,
        )
    }
    var pendingTarget by remember(activeUnit, preferences.weightIncrement, displayedTarget, inResistanceBand) {
        mutableDoubleStateOf(snapWeight(displayedTarget, minLoad, maxLoad, weightStep))
    }
    var instantApply by remember(preferences.instantWeightApplyDefault) {
        mutableStateOf(preferences.instantWeightApplyDefault)
    }
    var wasLoaded by remember {
        mutableStateOf(
            VoltraControlFrames.isLoadEngagedForWorkoutState(
                mode = state.safety.fitnessMode,
                workoutState = state.safety.workoutState,
            ),
        )
    }
    var lastInstantApplied by remember { mutableStateOf<Double?>(null) }
    val observedIsLoaded = VoltraControlFrames.isLoadEngagedForWorkoutState(
        mode = state.safety.fitnessMode,
        workoutState = state.safety.workoutState,
    )
    val observedRowingLoaded =
        state.safety.workoutState == VoltraControlFrames.WORKOUT_STATE_ROWING &&
            VoltraControlFrames.isLoadEngagedForWorkoutState(
                mode = state.safety.fitnessMode,
                workoutState = VoltraControlFrames.WORKOUT_STATE_ROWING,
            )
    var optimisticIsometricLoaded by remember { mutableStateOf<Boolean?>(null) }
    var pendingIsometricLoad by remember { mutableStateOf(false) }
    val isLoaded = when (activeProfile) {
        ControlModeUi.ISOMETRIC_TEST -> optimisticIsometricLoaded ?: observedIsLoaded
        ControlModeUi.ROWING -> observedRowingLoaded
        else -> observedIsLoaded
    }
    val canLoadShared = state.controlCommandsEnabled &&
        state.connectionState == VoltraConnectionState.CONNECTED &&
        state.safety.lowBattery != true &&
        state.safety.locked != true &&
        state.safety.childLocked != true &&
        state.safety.activeOta != true &&
        !VoltraControlFrames.isLoadedFitnessMode(state.safety.fitnessMode)
    val canLoadActiveProfile = when (activeProfile) {
        ControlModeUi.WEIGHT_TRAINING -> state.safety.canLoad
        ControlModeUi.RESISTANCE_BAND -> canLoadShared &&
            state.safety.workoutState == VoltraControlFrames.WORKOUT_STATE_RESISTANCE_BAND
        ControlModeUi.DAMPER -> canLoadShared &&
            state.safety.workoutState == VoltraControlFrames.WORKOUT_STATE_DAMPER
        ControlModeUi.ISOKINETIC -> canLoadShared &&
            inIsokineticFamily
        ControlModeUi.ISOMETRIC_TEST -> state.safety.canLoad &&
            state.safety.workoutState == VoltraControlFrames.WORKOUT_STATE_ISOMETRIC
        ControlModeUi.CUSTOM_CURVE ->
            canLoadShared &&
                state.safety.workoutState == VoltraControlFrames.WORKOUT_STATE_CUSTOM_CURVE
        ControlModeUi.ROWING ->
            state.controlCommandsEnabled &&
                state.connectionState == VoltraConnectionState.CONNECTED
    }
    val setTargetForMode: (Double) -> Unit = if (inResistanceBand) {
        onSetResistanceBandForce
    } else {
        onSetTarget
    }
    var suppressIsometricAutoEnterUntilMillis by remember { mutableStateOf(0L) }
    val loadForMode: () -> Unit = if (inResistanceBand) onLoadResistanceBand else onLoad
    val onIsometricLoad: () -> Unit = {
        suppressIsometricAutoEnterUntilMillis = 0L
        val readyToArm =
            state.controlCommandsEnabled &&
                state.connectionState == VoltraConnectionState.CONNECTED &&
                state.safety.workoutState == VoltraControlFrames.WORKOUT_STATE_ISOMETRIC &&
                state.safety.canLoad
        if (readyToArm) {
            pendingIsometricLoad = false
            optimisticIsometricLoaded = true
            onLoad()
        } else {
            pendingIsometricLoad = true
            if (state.safety.workoutState != VoltraControlFrames.WORKOUT_STATE_ISOMETRIC) {
                onEnterIsometric()
            }
        }
    }
    val onIsometricUnload: () -> Unit = {
        pendingIsometricLoad = false
        optimisticIsometricLoaded = false
        onUnload()
    }
    val onIsometricExitWorkout: () -> Unit = {
        pendingIsometricLoad = false
        optimisticIsometricLoaded = false
        suppressIsometricAutoEnterUntilMillis =
            System.currentTimeMillis() + ISOMETRIC_EXIT_REENTRY_GRACE_MILLIS
        onExitWorkout()
    }
    val observedResistanceExperience = when (state.reading.resistanceExperienceIntense) {
        true -> ResistanceExperienceOption.INTENSE
        false -> ResistanceExperienceOption.STANDARD
        null -> null
    }
    val observedAssistEnabled = state.reading.assistModeEnabled
    var pendingResistanceExperience by remember { mutableStateOf<ResistanceExperienceOption?>(null) }
    var pendingAssistEnabled by remember { mutableStateOf<Boolean?>(null) }
    val resistanceExperience = observedResistanceExperience ?: pendingResistanceExperience ?: ResistanceExperienceOption.STANDARD
    val assistEnabled = observedAssistEnabled ?: pendingAssistEnabled ?: false
    val observedResistanceMode = when (state.reading.resistanceBandInverse) {
        true -> ResistanceModeOption.INVERSE
        false -> ResistanceModeOption.STANDARD
        null -> null
    }
    val observedResistanceCurve = when (state.reading.resistanceBandCurveLogarithm) {
        true -> ResistanceCurveOption.LOGARITHM
        false -> ResistanceCurveOption.POWER_LAW
        null -> null
    }
    val observedProgressiveLengthMode = when (state.reading.resistanceBandByRangeOfMotion) {
        true -> ProgressiveLengthOption.ROM
        false -> ProgressiveLengthOption.BAND_LENGTH
        null -> null
    }
    var pendingResistanceMode by remember { mutableStateOf<ResistanceModeOption?>(null) }
    var pendingResistanceCurve by remember { mutableStateOf<ResistanceCurveOption?>(null) }
    var pendingProgressiveLengthMode by remember { mutableStateOf<ProgressiveLengthOption?>(null) }
    val resistanceMode = observedResistanceMode ?: pendingResistanceMode ?: ResistanceModeOption.STANDARD
    val resistanceCurve = observedResistanceCurve ?: pendingResistanceCurve ?: ResistanceCurveOption.POWER_LAW
    val progressiveLengthMode = observedProgressiveLengthMode ?: pendingProgressiveLengthMode ?: ProgressiveLengthOption.BAND_LENGTH
    var bandLengthInches by remember(state.reading.resistanceBandLengthCm) {
        mutableDoubleStateOf(
            (((state.reading.resistanceBandLengthCm ?: VoltraControlFrames.MAX_RESISTANCE_BAND_LENGTH_CM.toDouble()) / 2.54)
                .coerceIn(20.0, 102.0)),
        )
    }
    var damperLevel by remember {
        mutableDoubleStateOf(
            ((state.reading.damperLevelIndex ?: 4) + 1).coerceIn(1, 10).toDouble(),
        )
    }
    var rowingResistanceLevel by remember {
        mutableDoubleStateOf(
            (state.reading.rowingResistanceLevel ?: VoltraControlFrames.DEFAULT_ROWING_RESISTANCE_LEVEL)
                .coerceIn(
                    VoltraControlFrames.MIN_ROWING_SELECTOR_LEVEL,
                    VoltraControlFrames.MAX_ROWING_SELECTOR_LEVEL,
                )
                .toDouble(),
        )
    }
    var rowingSimulatedWearLevel by remember {
        mutableDoubleStateOf(
            (state.reading.rowingSimulatedWearLevel ?: VoltraControlFrames.DEFAULT_ROWING_SIMULATED_WEAR_LEVEL)
                .coerceIn(
                    VoltraControlFrames.MIN_ROWING_SELECTOR_LEVEL,
                    VoltraControlFrames.MAX_ROWING_SELECTOR_LEVEL,
                )
                .toDouble(),
        )
    }
    val isokineticSpeedOptions = remember { buildIsokineticSpeedOptions() }
    val observedIsokineticMenu = isokineticMenuFromParam(state.reading.isokineticMode)
    val observedIsokineticTargetSpeedIndex = closestIsokineticSpeedIndex(
        options = isokineticSpeedOptions,
        speedMmS = state.reading.isokineticTargetSpeedMmS ?: state.reading.isokineticSpeedLimitMmS,
    )
    val observedIsokineticSpeedLimitIndex = closestIsokineticSpeedIndex(
        options = isokineticSpeedOptions,
        speedMmS = state.reading.isokineticSpeedLimitMmS,
    )
    var isokineticMenu by remember { mutableStateOf(observedIsokineticMenu ?: IsokineticMenuOption.ISOKINETIC) }
    var isokineticTargetSpeedIndex by remember { mutableStateOf((observedIsokineticTargetSpeedIndex ?: 1).coerceAtLeast(1)) }
    var isokineticSpeedLimitIndex by remember { mutableStateOf(observedIsokineticSpeedLimitIndex ?: 0) }
    var isokineticMaxEccentricLoad by remember {
        mutableDoubleStateOf(
            state.reading.isokineticMaxEccentricLoadLb ?: VoltraControlFrames.MIN_ISOKINETIC_MAX_ECCENTRIC_LOAD_LB.toDouble(),
        )
    }
    var isokineticConstantResistance by remember {
        mutableDoubleStateOf(
            state.reading.isokineticConstantResistanceLb ?: VoltraControlFrames.MIN_ISOKINETIC_CONSTANT_RESISTANCE_LB.toDouble(),
        )
    }
    var showIsokineticSettings by remember { mutableStateOf(false) }
    var showWeightTrainingSettings by remember { mutableStateOf(false) }
    var showResistanceSettings by remember { mutableStateOf(false) }
    var showDamperSettings by remember { mutableStateOf(false) }
    var resistanceBandLaunchRequested by remember { mutableStateOf(false) }
    var damperLaunchRequested by remember { mutableStateOf(false) }
    var isokineticLaunchRequested by remember { mutableStateOf(false) }
    var isometricLaunchRequested by remember { mutableStateOf(false) }
    var customCurveLaunchRequested by remember { mutableStateOf(false) }
    var rowingLaunchRequested by remember { mutableStateOf(false) }
    val trackedSets = displayedSetCount(state.reading.setCount, state.reading.repCount)
    val damperPowerHistory = remember { mutableStateListOf<PowerWorkoutHistoryEntry>() }
    val isokineticPowerHistory = remember { mutableStateListOf<PowerWorkoutHistoryEntry>() }
    var lastDamperPowerHistoryKey by remember { mutableStateOf<String?>(null) }
    var lastIsokineticPowerHistoryKey by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(isLoaded) {
        if (wasLoaded && !isLoaded) {
            instantApply = false
            lastInstantApplied = null
        }
        wasLoaded = isLoaded
    }

    LaunchedEffect(observedResistanceExperience) {
        if (observedResistanceExperience != null) {
            pendingResistanceExperience = null
        }
    }

    LaunchedEffect(observedAssistEnabled) {
        if (observedAssistEnabled != null) {
            pendingAssistEnabled = null
        }
    }

    LaunchedEffect(state.connectionState) {
        if (state.connectionState != VoltraConnectionState.CONNECTED) {
            pendingResistanceExperience = null
            pendingAssistEnabled = null
            pendingResistanceMode = null
            pendingResistanceCurve = null
            pendingProgressiveLengthMode = null
            optimisticIsometricLoaded = null
            pendingIsometricLoad = false
            damperPowerHistory.clear()
            isokineticPowerHistory.clear()
            lastDamperPowerHistoryKey = null
            lastIsokineticPowerHistoryKey = null
        }
    }

    LaunchedEffect(state.safety.workoutState) {
        if (state.safety.workoutState != VoltraControlFrames.WORKOUT_STATE_DAMPER) {
            damperPowerHistory.clear()
            lastDamperPowerHistoryKey = null
        }
        if (state.safety.workoutState != VoltraControlFrames.WORKOUT_STATE_ISOKINETIC) {
            isokineticPowerHistory.clear()
            lastIsokineticPowerHistoryKey = null
        }
    }

    LaunchedEffect(
        activeProfile,
        trackedSets,
        state.reading.repCount,
        state.reading.workoutPeakForceLb,
        state.reading.workoutPeakPowerWatts,
        state.reading.workoutTimeToPeakMillis,
    ) {
        val peakPowerWatts = state.reading.workoutPeakPowerWatts ?: return@LaunchedEffect
        val entry = PowerWorkoutHistoryEntry(
            index = when (activeProfile) {
                ControlModeUi.DAMPER -> damperPowerHistory.size + 1
                ControlModeUi.ISOKINETIC -> isokineticPowerHistory.size + 1
                else -> return@LaunchedEffect
            },
            setCount = trackedSets,
            repCount = state.reading.repCount,
            peakForceLb = state.reading.workoutPeakForceLb,
            peakPowerWatts = peakPowerWatts,
            timeToPeakMillis = state.reading.workoutTimeToPeakMillis,
        )
        val key = listOf(
            activeProfile.name,
            entry.setCount ?: -1,
            entry.repCount ?: -1,
            entry.peakForceLb?.let { (it * 10.0).roundToInt() } ?: -1,
            entry.peakPowerWatts ?: -1,
            entry.timeToPeakMillis ?: -1L,
        ).joinToString(":")
        when (activeProfile) {
            ControlModeUi.DAMPER -> {
                if (key != lastDamperPowerHistoryKey) {
                    damperPowerHistory += entry
                    lastDamperPowerHistoryKey = key
                }
            }
            ControlModeUi.ISOKINETIC -> {
                if (key != lastIsokineticPowerHistoryKey) {
                    isokineticPowerHistory += entry
                    lastIsokineticPowerHistoryKey = key
                }
            }
        }
    }

    LaunchedEffect(observedResistanceMode) {
        if (observedResistanceMode != null) {
            pendingResistanceMode = null
        }
    }

    LaunchedEffect(observedResistanceCurve) {
        if (observedResistanceCurve != null) {
            pendingResistanceCurve = null
        }
    }

    LaunchedEffect(observedProgressiveLengthMode) {
        if (observedProgressiveLengthMode != null) {
            pendingProgressiveLengthMode = null
        }
    }

    LaunchedEffect(observedIsLoaded, activeProfile) {
        if (activeProfile != ControlModeUi.ISOMETRIC_TEST) {
            optimisticIsometricLoaded = null
            pendingIsometricLoad = false
        } else if (optimisticIsometricLoaded != null && optimisticIsometricLoaded == observedIsLoaded) {
            optimisticIsometricLoaded = null
            if (observedIsLoaded) {
                pendingIsometricLoad = false
            }
        }
    }

    LaunchedEffect(
        pendingIsometricLoad,
        activeProfile,
        state.connectionState,
        state.controlCommandsEnabled,
        state.safety.workoutState,
        state.safety.canLoad,
    ) {
        if (activeProfile != ControlModeUi.ISOMETRIC_TEST) return@LaunchedEffect
        if (
            pendingIsometricLoad &&
            state.connectionState == VoltraConnectionState.CONNECTED &&
            state.controlCommandsEnabled &&
            state.safety.workoutState == VoltraControlFrames.WORKOUT_STATE_ISOMETRIC &&
            state.safety.canLoad
        ) {
            pendingIsometricLoad = false
            optimisticIsometricLoaded = true
            onLoad()
        }
    }

    LaunchedEffect(state.reading.damperLevelIndex) {
        state.reading.damperLevelIndex?.let {
            damperLevel = (it + 1).coerceIn(1, 10).toDouble()
        }
    }

    LaunchedEffect(state.reading.rowingResistanceLevel) {
        state.reading.rowingResistanceLevel?.let {
            rowingResistanceLevel = it.coerceIn(
                VoltraControlFrames.MIN_ROWING_SELECTOR_LEVEL,
                VoltraControlFrames.MAX_ROWING_SELECTOR_LEVEL,
            ).toDouble()
        }
    }

    LaunchedEffect(state.reading.rowingSimulatedWearLevel) {
        state.reading.rowingSimulatedWearLevel?.let {
            rowingSimulatedWearLevel = it.coerceIn(
                VoltraControlFrames.MIN_ROWING_SELECTOR_LEVEL,
                VoltraControlFrames.MAX_ROWING_SELECTOR_LEVEL,
            ).toDouble()
        }
    }

    LaunchedEffect(observedIsokineticMenu) {
        observedIsokineticMenu?.let { isokineticMenu = it }
    }

    LaunchedEffect(observedIsokineticTargetSpeedIndex) {
        observedIsokineticTargetSpeedIndex?.let { isokineticTargetSpeedIndex = it.coerceAtLeast(1) }
    }

    LaunchedEffect(observedIsokineticSpeedLimitIndex) {
        observedIsokineticSpeedLimitIndex?.let { isokineticSpeedLimitIndex = it }
    }

    LaunchedEffect(state.reading.isokineticMaxEccentricLoadLb) {
        state.reading.isokineticMaxEccentricLoadLb?.let {
            isokineticMaxEccentricLoad = snapWeight(
                value = it,
                minLoad = VoltraControlFrames.MIN_ISOKINETIC_MAX_ECCENTRIC_LOAD_LB.toDouble(),
                maxLoad = VoltraControlFrames.MAX_ISOKINETIC_MAX_ECCENTRIC_LOAD_LB.toDouble(),
                step = 5.0,
            )
        }
    }

    LaunchedEffect(state.reading.isokineticConstantResistanceLb) {
        state.reading.isokineticConstantResistanceLb?.let {
            isokineticConstantResistance = snapWeight(
                value = it,
                minLoad = VoltraControlFrames.MIN_ISOKINETIC_CONSTANT_RESISTANCE_LB.toDouble(),
                maxLoad = VoltraControlFrames.MAX_ISOKINETIC_CONSTANT_RESISTANCE_LB.toDouble(),
                step = 5.0,
            )
        }
    }

    LaunchedEffect(activeProfile) {
        if (activeProfile != ControlModeUi.ISOKINETIC) {
            showIsokineticSettings = false
        }
    }

    LaunchedEffect(activeProfile, state.connectionState, state.controlCommandsEnabled) {
        if (state.connectionState == VoltraConnectionState.CONNECTED && state.controlCommandsEnabled) {
            onRefreshModeFeatureStatus()
        }
    }

    LaunchedEffect(selectedMode, state.connectionState, state.controlCommandsEnabled, state.safety.workoutState) {
        if (
            selectedMode == ControlModeUi.RESISTANCE_BAND &&
            state.connectionState == VoltraConnectionState.CONNECTED &&
            state.controlCommandsEnabled &&
            state.safety.workoutState != VoltraControlFrames.WORKOUT_STATE_RESISTANCE_BAND &&
            !resistanceBandLaunchRequested
        ) {
            resistanceBandLaunchRequested = true
            onEnterResistanceBand()
        } else if (
            selectedMode != ControlModeUi.RESISTANCE_BAND ||
            state.connectionState != VoltraConnectionState.CONNECTED ||
            state.safety.workoutState == VoltraControlFrames.WORKOUT_STATE_RESISTANCE_BAND
        ) {
            resistanceBandLaunchRequested = false
        }
    }

    LaunchedEffect(selectedMode, state.connectionState, state.controlCommandsEnabled, state.safety.workoutState) {
        if (
            selectedMode == ControlModeUi.DAMPER &&
            state.connectionState == VoltraConnectionState.CONNECTED &&
            state.controlCommandsEnabled &&
            state.safety.workoutState != VoltraControlFrames.WORKOUT_STATE_DAMPER &&
            !damperLaunchRequested
        ) {
            damperLaunchRequested = true
            onEnterDamper()
        } else if (
            selectedMode != ControlModeUi.DAMPER ||
            state.connectionState != VoltraConnectionState.CONNECTED ||
            state.safety.workoutState == VoltraControlFrames.WORKOUT_STATE_DAMPER
        ) {
            damperLaunchRequested = false
        }
    }

    LaunchedEffect(selectedMode, state.connectionState, state.controlCommandsEnabled, inIsokineticFamily) {
        if (
            selectedMode == ControlModeUi.ISOKINETIC &&
            state.connectionState == VoltraConnectionState.CONNECTED &&
            state.controlCommandsEnabled &&
            !inIsokineticFamily &&
            !isokineticLaunchRequested
        ) {
            isokineticLaunchRequested = true
            onEnterIsokinetic()
        } else if (
            selectedMode != ControlModeUi.ISOKINETIC ||
            state.connectionState != VoltraConnectionState.CONNECTED ||
            inIsokineticFamily
        ) {
            isokineticLaunchRequested = false
        }
    }

    LaunchedEffect(selectedMode, state.connectionState, state.controlCommandsEnabled, state.safety.workoutState) {
        if (
            selectedMode == ControlModeUi.ISOMETRIC_TEST &&
            state.connectionState == VoltraConnectionState.CONNECTED &&
            state.controlCommandsEnabled &&
            state.safety.workoutState != VoltraControlFrames.WORKOUT_STATE_ISOMETRIC &&
            System.currentTimeMillis() >= suppressIsometricAutoEnterUntilMillis &&
            !isometricLaunchRequested
        ) {
            isometricLaunchRequested = true
            pendingIsometricLoad = true
            onEnterIsometric()
        } else if (
            selectedMode != ControlModeUi.ISOMETRIC_TEST ||
            state.connectionState != VoltraConnectionState.CONNECTED ||
            state.safety.workoutState == VoltraControlFrames.WORKOUT_STATE_ISOMETRIC
        ) {
            isometricLaunchRequested = false
        }
    }

    LaunchedEffect(selectedMode, state.connectionState, state.controlCommandsEnabled, state.safety.workoutState) {
        if (
            selectedMode == ControlModeUi.CUSTOM_CURVE &&
            state.connectionState == VoltraConnectionState.CONNECTED &&
            state.controlCommandsEnabled &&
            state.safety.workoutState != VoltraControlFrames.WORKOUT_STATE_CUSTOM_CURVE &&
            !customCurveLaunchRequested
        ) {
            customCurveLaunchRequested = true
            onEnterCustomCurve()
        } else if (
            selectedMode != ControlModeUi.CUSTOM_CURVE ||
            state.connectionState != VoltraConnectionState.CONNECTED ||
            state.safety.workoutState == VoltraControlFrames.WORKOUT_STATE_CUSTOM_CURVE
        ) {
            customCurveLaunchRequested = false
        }
    }

    LaunchedEffect(selectedMode, state.connectionState, state.controlCommandsEnabled, state.safety.workoutState) {
        if (
            selectedMode == ControlModeUi.ROWING &&
            state.connectionState == VoltraConnectionState.CONNECTED &&
            state.controlCommandsEnabled &&
            state.safety.workoutState != VoltraControlFrames.WORKOUT_STATE_ROWING &&
            state.reading.workoutMode?.startsWith("Rowing") != true &&
            !rowingLaunchRequested
        ) {
            rowingLaunchRequested = true
            onEnterRowing()
        } else if (
            selectedMode != ControlModeUi.ROWING ||
            state.connectionState != VoltraConnectionState.CONNECTED ||
            state.safety.workoutState == VoltraControlFrames.WORKOUT_STATE_ROWING ||
            state.reading.workoutMode?.startsWith("Rowing") == true
        ) {
            rowingLaunchRequested = false
        }
    }

    fun setPendingTarget(target: Double) {
        pendingTarget = snapWeight(target, minLoad, maxLoad, weightStep)
    }

    fun applyPendingTarget(target: Double) {
        val snapped = snapWeight(target, minLoad, maxLoad, weightStep)
        pendingTarget = snapped
        if (lastInstantApplied != snapped) {
            lastInstantApplied = snapped
            setTargetForMode(snapped)
        }
    }
    val activePresetScope = if (inResistanceBand) WeightPresetScope.RESISTANCE_BAND else WeightPresetScope.WEIGHT_TRAINING
    val activePresets = weightPresets.filter { it.scope == activePresetScope }
    val mainControlCard: @Composable (Boolean) -> Unit = { wideLayout ->
        when (activeProfile) {
            ControlModeUi.WEIGHT_TRAINING,
            ControlModeUi.RESISTANCE_BAND ->
                WeightTrainingCard(
                    modeTitle = modeTitle,
                    targetLabel = if (inResistanceBand) "Band Force" else "Weight",
                    unit = activeUnit,
                    pendingTarget = pendingTarget,
                    minLoad = minLoad,
                    maxLoad = maxLoad,
                    weightStep = weightStep,
                    isLoaded = isLoaded,
                    controlReady = state.controlCommandsEnabled && modeSessionMatched,
                    canLoad = canLoadActiveProfile,
                    instantApply = instantApply,
                    status = state.reading.workoutMode ?: state.protocolStatus.displayText(),
                    onTargetChange = ::setPendingTarget,
                    onTargetSettled = { target ->
                        if (instantApply) {
                            applyPendingTarget(target)
                        }
                    },
                    onSetTarget = {
                        if (instantApply) {
                            instantApply = false
                            lastInstantApplied = null
                        } else {
                            setTargetForMode(pendingTarget)
                        }
                    },
                    onTargetCommit = { target ->
                        pendingTarget = target
                        instantApply = false
                        lastInstantApplied = null
                        setTargetForMode(target)
                    },
                    onToggleInstantApply = {
                        instantApply = !instantApply
                        lastInstantApplied = null
                    },
                    settingsLabel = if (activeProfile == ControlModeUi.WEIGHT_TRAINING) "Workout Settings" else "Resistance Settings",
                    onOpenSettings = {
                        if (activeProfile == ControlModeUi.WEIGHT_TRAINING) {
                            showWeightTrainingSettings = true
                        } else {
                            showResistanceSettings = true
                        }
                    },
                    sets = trackedSets,
                    reps = state.reading.repCount,
                    phase = state.reading.repPhase,
                    onCycleWeightIncrement = {
                        onSetWeightIncrement(nextWeightIncrement(preferences.weightIncrement))
                    },
                    presets = activePresets,
                    onSavePreset = { name, value ->
                        onSaveWeightPreset(name, activePresetScope, value)
                    },
                    onApplyPreset = { preset ->
                        val converted = com.technogizguy.voltra.controller.model.Weight(preset.value, preset.unit)
                            .toUnit(activeUnit)
                            .cappedForV1()
                        pendingTarget = snapWeight(converted.value, minLoad, maxLoad, weightStep)
                        onApplyWeightPreset(preset)
                    },
                    wideLayout = wideLayout,
                    onSetUnit = onSetUnit,
                    unitSwitchEnabled = true,
                    onLoad = loadForMode,
                    onUnload = {
                        instantApply = false
                        lastInstantApplied = null
                        onUnload()
                    },
                    cableLengthEnabled = state.controlCommandsEnabled && sessionActive,
                    onTriggerCableLength = onTriggerCableLength,
                    onExitWorkout = onExitWorkout,
                )
            ControlModeUi.DAMPER ->
                DamperModeCard(
                    level = damperLevel,
                    isLoaded = isLoaded,
                    controlReady = state.controlCommandsEnabled && modeSessionMatched,
                    canLoad = canLoadActiveProfile,
                    status = activeProfileStatus,
                    sets = trackedSets,
                    reps = state.reading.repCount,
                    phase = state.reading.repPhase,
                    peakForceLb = state.reading.workoutPeakForceLb,
                    peakPowerWatts = state.reading.workoutPeakPowerWatts,
                    timeToPeakMillis = state.reading.workoutTimeToPeakMillis,
                    history = damperPowerHistory,
                    wideLayout = wideLayout,
                    onOpenSettings = { showDamperSettings = true },
                    onLevelChange = {
                        val snapped = snapWeight(it, 1.0, 10.0, 1.0)
                        if (snapped != damperLevel) {
                            damperLevel = snapped
                            onSetDamperLevel((snapped.roundToInt() - 1).coerceIn(0, 9))
                        }
                    },
                    onLoad = onLoad,
                    onUnload = onUnload,
                    onTriggerCableLength = onTriggerCableLength,
                    cableLengthEnabled = state.controlCommandsEnabled && sessionActive,
                    onExitWorkout = onExitWorkout,
                )
            ControlModeUi.ISOKINETIC ->
                IsokineticModeCard(
                    speedOptions = isokineticSpeedOptions,
                    targetSpeedIndex = isokineticTargetSpeedIndex,
                    maxEccentricLoad = isokineticMaxEccentricLoad,
                    constantResistance = isokineticConstantResistance,
                    isLoaded = isLoaded,
                    controlReady = state.controlCommandsEnabled && modeSessionMatched,
                    canLoad = canLoadActiveProfile,
                    status = activeProfileStatus,
                    sets = trackedSets,
                    reps = state.reading.repCount,
                    phase = state.reading.repPhase,
                    peakForceLb = state.reading.workoutPeakForceLb,
                    peakPowerWatts = state.reading.workoutPeakPowerWatts,
                    timeToPeakMillis = state.reading.workoutTimeToPeakMillis,
                    history = isokineticPowerHistory,
                    wideLayout = wideLayout,
                    onSelectTargetSpeedIndex = {
                        val nextIndex = it.coerceIn(1, isokineticSpeedOptions.lastIndex)
                        if (nextIndex != isokineticTargetSpeedIndex) {
                            isokineticTargetSpeedIndex = nextIndex
                            onSetIsokineticTargetSpeed(isokineticSpeedMmSForOption(isokineticSpeedOptions[nextIndex]))
                        }
                    },
                    onLoad = onLoad,
                    onUnload = onUnload,
                    onTriggerCableLength = onTriggerCableLength,
                    cableLengthEnabled = state.controlCommandsEnabled && sessionActive,
                    onExitWorkout = onExitWorkout,
                    onOpenSettings = { showIsokineticSettings = true },
                )
            ControlModeUi.ISOMETRIC_TEST ->
                IsometricTestCard(
                    status = activeProfileStatus,
                    currentForceN = state.reading.isometricCurrentForceN,
                    peakForceN = state.reading.isometricPeakForceN,
                    peakRelativeForcePercent = state.reading.isometricPeakRelativeForcePercent,
                    elapsedMillis = state.reading.isometricElapsedMillis,
                    waveformSamplesN = state.reading.isometricWaveformSamplesN,
                    maxForceLb = state.reading.isometricMaxForceLb,
                    maxDurationSeconds = state.reading.isometricMaxDurationSeconds,
                    isLoaded = isLoaded,
                    controlReady = state.controlCommandsEnabled &&
                        state.connectionState == VoltraConnectionState.CONNECTED,
                    canLoad = canLoadActiveProfile,
                    canRequestLoad = state.controlCommandsEnabled &&
                        state.connectionState == VoltraConnectionState.CONNECTED &&
                        activeProfile == ControlModeUi.ISOMETRIC_TEST,
                    sets = trackedSets,
                    reps = state.reading.repCount,
                    phase = state.reading.repPhase,
                    wideLayout = wideLayout,
                    onLoad = onIsometricLoad,
                    onUnload = onIsometricUnload,
                    onExitWorkout = onIsometricExitWorkout,
                )
            ControlModeUi.CUSTOM_CURVE ->
                CustomCurveCard(
                    status = activeProfileStatus,
                    presets = customCurvePresets,
                    currentForceLb = state.reading.forceLb,
                    reps = state.reading.repCount,
                    phase = state.reading.repPhase,
                    isLoaded = isLoaded,
                    controlReady = state.controlCommandsEnabled && state.connectionState == VoltraConnectionState.CONNECTED,
                    canLoad = canLoadActiveProfile,
                    onApplyCurve = onApplyCustomCurve,
                    onSavePreset = onSaveCustomCurvePreset,
                    onDeletePreset = onDeleteCustomCurvePreset,
                    onLoad = onLoad,
                    onUnload = onUnload,
                    onExitWorkout = onExitWorkout,
                )
            ControlModeUi.ROWING ->
                RowingModeCard(
                    status = activeProfileStatus,
                    distanceMeters = state.reading.rowingDistanceMeters,
                    elapsedMillis = state.reading.rowingElapsedMillis,
                    pace500Millis = state.reading.rowingPace500Millis,
                    averagePace500Millis = state.reading.rowingAveragePace500Millis,
                    strokeRateSpm = state.reading.rowingStrokeRateSpm,
                    driveForceLb = state.reading.rowingDriveForceLb,
                    distanceSamplesMeters = state.reading.rowingDistanceSamplesMeters,
                    forceSamplesLb = state.reading.rowingForceSamplesLb,
                    resistanceLevel = rowingResistanceLevel,
                    simulatedWearLevel = rowingSimulatedWearLevel,
                    strokes = state.reading.repCount,
                    phase = state.reading.repPhase,
                    isLoaded = isLoaded,
                    controlReady = state.controlCommandsEnabled && state.connectionState == VoltraConnectionState.CONNECTED,
                    canLoad = canLoadActiveProfile,
                    onResistanceLevelChange = {
                        val snapped = it.roundToInt().coerceIn(
                            VoltraControlFrames.MIN_ROWING_SELECTOR_LEVEL,
                            VoltraControlFrames.MAX_ROWING_SELECTOR_LEVEL,
                        )
                        if (snapped.toDouble() != rowingResistanceLevel) {
                            rowingResistanceLevel = snapped.toDouble()
                            onSetRowingResistanceLevel(snapped)
                        }
                    },
                    onSimulatedWearLevelChange = {
                        val snapped = it.roundToInt().coerceIn(
                            VoltraControlFrames.MIN_ROWING_SELECTOR_LEVEL,
                            VoltraControlFrames.MAX_ROWING_SELECTOR_LEVEL,
                        )
                        if (snapped.toDouble() != rowingSimulatedWearLevel) {
                            rowingSimulatedWearLevel = snapped.toDouble()
                            onSetRowingSimulatedWearLevel(snapped)
                        }
                    },
                    onLoad = onStartRowing,
                    onUnload = onUnload,
                    onExitWorkout = onExitWorkout,
                )
        }
    }

    CompositionLocalProvider(LocalControlAccent provides controlAccent) {
        AdaptiveScreenScaffold(maxContentWidth = 1320.dp) { layout ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = screenPadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item { mainControlCard(layout.useWidePanels) }
            }
        }
    }

    if (activeProfile == ControlModeUi.WEIGHT_TRAINING && showWeightTrainingSettings) {
        WeightTrainingSettingsHost(
            reading = state.reading,
            unit = preferences.unit,
            baseWeightLb = state.safety.targetLoadLb
                ?: state.reading.weightLb
                ?: state.targetLoad.takeIf { it.unit == WeightUnit.LB }?.value,
            controlReady = state.controlCommandsEnabled,
            developerModeEnabled = preferences.developerModeEnabled,
            experience = resistanceExperience,
            assistEnabled = assistEnabled,
            onDismiss = { showWeightTrainingSettings = false },
            onRefreshModeFeatureStatus = onRefreshModeFeatureStatus,
            onSetExperience = {
                pendingResistanceExperience = it
                onSetResistanceExperience(it == ResistanceExperienceOption.INTENSE)
            },
            onSetAssist = {
                pendingAssistEnabled = it
                onSetAssistMode(it)
            },
            onSetChainsWeight = onSetChainsWeight,
            onSetEccentricWeight = onSetEccentricWeight,
            onSetInverseChains = onSetInverseChains,
        )
    }

    if (activeProfile == ControlModeUi.RESISTANCE_BAND && showResistanceSettings) {
        ResistanceBandSettingsSheet(
            experience = resistanceExperience,
            resistanceMode = resistanceMode,
            resistanceCurve = resistanceCurve,
            progressiveLengthMode = progressiveLengthMode,
            bandLengthInches = bandLengthInches,
            observedBandLengthInches = state.reading.resistanceBandLengthCm?.div(2.54),
            onDismiss = { showResistanceSettings = false },
            onSetExperience = {
                pendingResistanceExperience = it
                onSetResistanceExperience(it == ResistanceExperienceOption.INTENSE)
            },
            onSetResistanceMode = {
                pendingResistanceMode = it
                onSetResistanceMode(it == ResistanceModeOption.INVERSE)
            },
            onSetResistanceCurve = {
                pendingResistanceCurve = it
                onSetResistanceCurve(it == ResistanceCurveOption.LOGARITHM)
            },
            onSetProgressiveLengthMode = {
                pendingProgressiveLengthMode = it
                onSetResistanceBandByRangeOfMotion(it == ProgressiveLengthOption.ROM)
            },
            onSetBandLengthInches = {
                val snapped = snapWeight(it, 20.0, 102.0, 1.0)
                bandLengthInches = snapped
                onSetResistanceBandLength(snapped)
            },
        )
    }

    if (activeProfile == ControlModeUi.DAMPER && showDamperSettings) {
        DamperSettingsSheet(
            experience = resistanceExperience,
            onDismiss = { showDamperSettings = false },
            onSetExperience = {
                pendingResistanceExperience = it
                onSetResistanceExperience(it == ResistanceExperienceOption.INTENSE)
            },
        )
    }

    if (activeProfile == ControlModeUi.ISOKINETIC && showIsokineticSettings) {
        IsokineticSettingsSheet(
            menu = isokineticMenu,
            speedOptions = isokineticSpeedOptions,
            speedIndex = isokineticSpeedLimitIndex,
            maxEccentricLoad = isokineticMaxEccentricLoad,
            constantResistance = isokineticConstantResistance,
            onDismiss = { showIsokineticSettings = false },
            onSelectMenu = {
                isokineticMenu = it
                onSetIsokineticMenu(it.paramValue())
            },
            onSelectSpeedIndex = {
                val nextIndex = it.coerceIn(0, isokineticSpeedOptions.lastIndex)
                if (isokineticMenu != IsokineticMenuOption.ISOKINETIC) {
                    isokineticMenu = IsokineticMenuOption.ISOKINETIC
                    onSetIsokineticMenu(IsokineticMenuOption.ISOKINETIC.paramValue())
                }
                isokineticSpeedLimitIndex = nextIndex
                onSetIsokineticSpeedLimit(isokineticSpeedMmSForOption(isokineticSpeedOptions[nextIndex]))
            },
            onSetMaxEccentricLoad = {
                val snapped = snapWeight(it, 5.0, 200.0, 5.0)
                if (isokineticMenu != IsokineticMenuOption.ISOKINETIC) {
                    isokineticMenu = IsokineticMenuOption.ISOKINETIC
                    onSetIsokineticMenu(IsokineticMenuOption.ISOKINETIC.paramValue())
                }
                isokineticMaxEccentricLoad = snapped
                onSetIsokineticMaxEccentricLoad(snapped)
            },
            onSetConstantResistance = {
                val snapped = snapWeight(it, 5.0, 100.0, 5.0)
                if (isokineticMenu != IsokineticMenuOption.CONSTANT_RESISTANCE) {
                    isokineticMenu = IsokineticMenuOption.CONSTANT_RESISTANCE
                    onSetIsokineticMenu(IsokineticMenuOption.CONSTANT_RESISTANCE.paramValue())
                }
                isokineticConstantResistance = snapped
                onSetIsokineticConstantResistance(snapped)
            },
        )
    }
}

@Composable
private fun MoreFeaturesScreen(
    state: VoltraSessionState,
    preferences: LocalPreferences,
    weightPresets: List<WeightPreset>,
    workoutHistory: List<WorkoutHistoryEntry>,
    mqttState: MqttPublisherState,
    httpGatewayState: HttpGatewayState,
    onSetAccentColor: (AccentColor) -> Unit,
    onSetInstantWeightApplyDefault: (Boolean) -> Unit,
    onSetDeveloperModeEnabled: (Boolean) -> Unit,
    onSetMqttEnabled: (Boolean) -> Unit,
    onSaveMqttSettings: (MqttPreferences) -> Unit,
    onSetHttpGatewayEnabled: (Boolean) -> Unit,
    onSaveHttpGatewaySettings: (HttpGatewayPreferences) -> Unit,
    onRotateHttpGatewayAccessKey: () -> Unit,
    onPublishMqttNow: () -> Unit,
    onSetDeviceName: (String) -> Unit,
    onUploadStartupImage: (ByteArray) -> Unit,
    onDeleteWeightPreset: (String) -> Unit,
    onShareWorkoutHistory: () -> Unit,
    onClearWorkoutHistory: () -> Unit,
    onOpenLogs: () -> Unit,
    onShareDiagnostics: () -> Unit,
) {
    AdaptiveScreenScaffold { layout ->
        var versionTapCount by remember(preferences.developerModeEnabled) { mutableStateOf(0) }
        val versionLabel = stringResource(id = R.string.app_version_label)
        val leftColumn: @Composable ColumnScope.() -> Unit = {
            DevicePersonalizationCard(
                state = state,
                onSetDeviceName = onSetDeviceName,
                onUploadStartupImage = onUploadStartupImage,
            )
            ThemePickerRow(accentColor = preferences.accentColor, onSetAccentColor = onSetAccentColor)
            ControlPreferencesCard(
                preferences = preferences,
                onSetInstantWeightApplyDefault = onSetInstantWeightApplyDefault,
            )
            WeightPresetLibraryCard(
                presets = weightPresets,
                onDeletePreset = onDeleteWeightPreset,
            )
            WorkoutHistoryCard(
                history = workoutHistory,
                onShareWorkoutHistory = onShareWorkoutHistory,
                onClearWorkoutHistory = onClearWorkoutHistory,
            )
            DiagnosticsToolsCard(
                onOpenLogs = onOpenLogs,
                onShareDiagnostics = onShareDiagnostics,
            )
        }
        val rightColumn: @Composable ColumnScope.() -> Unit = {
            MqttSensorCard(
                preferences = preferences,
                mqttState = mqttState,
                onSetMqttEnabled = onSetMqttEnabled,
                onSaveMqttSettings = onSaveMqttSettings,
                onPublishMqttNow = onPublishMqttNow,
            )
            HttpGatewayCard(
                preferences = preferences,
                gatewayState = httpGatewayState,
                onSetHttpGatewayEnabled = onSetHttpGatewayEnabled,
                onSaveHttpGatewaySettings = onSaveHttpGatewaySettings,
                onRotateHttpGatewayAccessKey = onRotateHttpGatewayAccessKey,
            )
            VersionCard(
                versionLabel = versionLabel,
                developerModeEnabled = preferences.developerModeEnabled,
                onTapVersion = {
                    if (preferences.developerModeEnabled) return@VersionCard
                    versionTapCount += 1
                    if (versionTapCount >= 5) {
                        onSetDeveloperModeEnabled(true)
                    }
                },
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = screenPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                SectionHeader(
                    title = "More",
                    subtitle = "Preferences, exports, and advanced features.",
                )
            }
            if (layout.useWidePanels) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            content = leftColumn,
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            content = rightColumn,
                        )
                    }
                }
            } else {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp), content = leftColumn)
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp), content = rightColumn)
                }
            }
        }
    }
}

@Composable
private fun DevicePersonalizationCard(
    state: VoltraSessionState,
    onSetDeviceName: (String) -> Unit,
    onUploadStartupImage: (ByteArray) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isConnected = state.connectionState == VoltraConnectionState.CONNECTED
    val currentDeviceName = state.currentDevice?.name.orEmpty()
    var deviceName by remember(currentDeviceName) { mutableStateOf(currentDeviceName) }
    var localMessage by remember { mutableStateOf<String?>(null) }
    var startupImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var startupImagePreparing by remember { mutableStateOf(false) }
    var startupImageUploadStartedAtMillis by remember { mutableStateOf<Long?>(null) }
    val latestPersonalizationCommand = state.commandLog.lastOrNull { command ->
        command.command == VoltraControlCommand.SET_DEVICE_NAME ||
            command.command == VoltraControlCommand.UPLOAD_STARTUP_IMAGE
    }
    val latestStartupImageCommand = state.commandLog.lastOrNull { command ->
        command.command == VoltraControlCommand.UPLOAD_STARTUP_IMAGE &&
            startupImageUploadStartedAtMillis?.let { command.timestampMillis >= it } != false
    }
    val startupImageUploadFinished = latestStartupImageCommand?.status in setOf(
        VoltraCommandStatus.CONFIRMED,
        VoltraCommandStatus.BLOCKED,
        VoltraCommandStatus.TIMED_OUT,
        VoltraCommandStatus.FAILED,
        VoltraCommandStatus.CANCELLED,
    )
    val startupImageUploadInProgress = startupImageUploadStartedAtMillis != null && !startupImageUploadFinished
    val startupImageBusy = startupImagePreparing || startupImageUploadInProgress
    val startupImageTransferUiState = startupImageTransferUiState(
        statusMessage = state.statusMessage,
        command = latestStartupImageCommand,
    )
    val trimmedName = deviceName.trim()
    val nameError = remember(trimmedName) {
        when {
            trimmedName.isEmpty() -> "Device name must not be blank."
            trimmedName.length > 20 -> "Device name must be 20 characters or fewer."
            !trimmedName.first().isLetter() -> "Device name must start with a letter."
            trimmedName.any { it.code !in 0x20..0x7E || it == ':' || it == '\\' || it == '|' } ->
                "Use plain ASCII only. :, \\, and | are not allowed."
            else -> null
        }
    }
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        Log.d(STARTUP_UI_TAG, "photo picker returned startup image uri=$uri")
        localMessage = "Loading startup image..."
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    loadStartupImageBitmap(context, uri)
                }
            }.onSuccess { bitmap ->
                Log.d(
                    STARTUP_UI_TAG,
                    "loaded startup image bitmap ${bitmap.width}x${bitmap.height} config=${bitmap.config}",
                )
                startupImageBitmap?.recycle()
                startupImageBitmap = bitmap
                localMessage = "Adjust the square crop, then send the startup image when it looks right."
            }.onFailure { error ->
                Log.w(STARTUP_UI_TAG, "failed to load startup image", error)
                localMessage = error.message ?: "Could not load the selected image."
            }
        }
    }

    LaunchedEffect(latestStartupImageCommand?.timestampMillis, latestStartupImageCommand?.status) {
        if (startupImageUploadStartedAtMillis != null && startupImageUploadFinished) {
            localMessage = latestStartupImageCommand?.message
            startupImageUploadStartedAtMillis = null
        }
    }

    LaunchedEffect(startupImageUploadStartedAtMillis) {
        val uploadStartedAt = startupImageUploadStartedAtMillis ?: return@LaunchedEffect
        delay(STARTUP_IMAGE_UPLOAD_TIMEOUT_MILLIS)
        if (startupImageUploadStartedAtMillis == uploadStartedAt) {
            localMessage = "Startup image transfer is taking longer than expected. Check the VOLTRA screen before trying again."
            startupImageUploadStartedAtMillis = null
        }
    }

    startupImageBitmap?.let { bitmap ->
        StartupImageCropDialog(
            bitmap = bitmap,
            isProcessing = startupImagePreparing,
            onDismiss = {
                if (startupImagePreparing) return@StartupImageCropDialog
                bitmap.recycle()
                startupImageBitmap = null
            },
            onConfirm = { cropTransform ->
                if (startupImagePreparing) return@StartupImageCropDialog
                startupImagePreparing = true
                Log.d(
                    STARTUP_UI_TAG,
                    "preparing startup image crop zoom=${cropTransform.zoom} x=${cropTransform.offsetXFraction} y=${cropTransform.offsetYFraction}",
                )
                scope.launch {
                    runCatching {
                        localMessage = "Preparing startup image..."
                        val prepared = withContext(Dispatchers.IO) {
                            prepareStartupImage(bitmap, cropTransform)
                        }
                        Log.d(
                            STARTUP_UI_TAG,
                            "prepared startup image bytes=${prepared.jpegBytes.size} size=${prepared.width}x${prepared.height}",
                        )
                        startupImageUploadStartedAtMillis = System.currentTimeMillis()
                        localMessage = "Sending startup image to the VOLTRA..."
                        onUploadStartupImage(prepared.jpegBytes)
                    }.onFailure { error ->
                        Log.w(STARTUP_UI_TAG, "failed to prepare startup image", error)
                        localMessage = error.message ?: "Could not prepare the selected image."
                    }
                    startupImagePreparing = false
                    bitmap.recycle()
                    startupImageBitmap = null
                }
            },
        )
    }

    MetricCard {
        Text("Device Personalization", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(
            "Set the VOLTRA name from your phone, then choose a square crop for the startup image.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
        Text(
            if (currentDeviceName.isNotBlank()) "Current device name: $currentDeviceName" else "Connect to a VOLTRA to personalize it.",
            style = MaterialTheme.typography.bodyMedium,
            color = if (state.currentDevice != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = deviceName,
            onValueChange = { deviceName = it.take(20) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Device Name") },
            supportingText = {
                Text("1-20 ASCII characters. Start with a letter.")
            },
            singleLine = true,
            enabled = state.currentDevice != null,
            isError = nameError != null,
        )
        nameError?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = {
                    onSetDeviceName(trimmedName)
                    localMessage = "Queued device rename to \"$trimmedName\"."
                },
                enabled = isConnected && nameError == null && trimmedName.isNotEmpty() && trimmedName != currentDeviceName,
                modifier = Modifier.weight(1f),
            ) {
                Text("Save Name")
            }
            OutlinedButton(
                onClick = {
                    photoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                enabled = isConnected && !startupImageBusy,
                modifier = Modifier.weight(1f),
            ) {
                Text(if (startupImageBusy) "Sending..." else "Startup Image")
            }
        }
        Text(
            "Choose a photo, adjust the square crop, and send it to the VOLTRA as the startup image.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (startupImageUploadInProgress) {
            StartupImageTransferProgress(startupImageTransferUiState)
        }
        localMessage?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        latestPersonalizationCommand?.message?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun startupImageTransferUiState(
    statusMessage: String,
    command: VoltraCommandResult?,
): StartupImageTransferUiState {
    val chunkMatch = StartupImageChunkProgressRegex.find(statusMessage)
    if (chunkMatch != null) {
        val chunkIndex = chunkMatch.groupValues.getOrNull(1)?.toIntOrNull()
        val chunkCount = chunkMatch.groupValues.getOrNull(2)?.toIntOrNull()
        if (chunkIndex != null && chunkCount != null && chunkCount > 0) {
            return StartupImageTransferUiState(
                title = "Sending startup image",
                detail = "Chunk $chunkIndex of $chunkCount",
                progress = (chunkIndex.toFloat() / chunkCount.toFloat()).coerceIn(0.02f, 1f),
            )
        }
    }
    return when {
        statusMessage.contains("startup image header", ignoreCase = true) ->
            StartupImageTransferUiState("Starting transfer", "Preparing the VOLTRA for the image.", null)
        statusMessage.contains("startup image finalize", ignoreCase = true) ->
            StartupImageTransferUiState("Finalizing image", "The VOLTRA is checking the upload.", null)
        statusMessage.contains("startup image apply", ignoreCase = true) ->
            StartupImageTransferUiState("Applying image", "Waiting for the VOLTRA to accept it.", null)
        command?.status == VoltraCommandStatus.SENT ->
            StartupImageTransferUiState("Waiting for VOLTRA", "Transfer sent. Watching for confirmation.", null)
        command?.status == VoltraCommandStatus.QUEUED ->
            StartupImageTransferUiState("Queued startup image", "Transfer will begin in a moment.", null)
        else ->
            StartupImageTransferUiState("Sending startup image", "Keep the phone close to the VOLTRA.", null)
    }
}

@Composable
private fun StartupImageTransferProgress(state: StartupImageTransferUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val progress = state.progress
        if (progress == null) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                strokeWidth = 3.dp,
            )
        } else {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(28.dp),
                strokeWidth = 3.dp,
                trackColor = MaterialTheme.colorScheme.surface,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                state.title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                state.detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StartupImageCropDialog(
    bitmap: Bitmap,
    isProcessing: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (StartupImageCropTransform) -> Unit,
) {
    var zoom by remember(bitmap) { mutableFloatStateOf(1f) }
    var offsetX by remember(bitmap) { mutableFloatStateOf(0f) }
    var offsetY by remember(bitmap) { mutableFloatStateOf(0f) }
    var viewportSizePx by remember(bitmap) { mutableFloatStateOf(0f) }

    fun clampOffsets(nextOffsetX: Float, nextOffsetY: Float, nextZoom: Float): Pair<Float, Float> {
        if (viewportSizePx <= 0f) return nextOffsetX to nextOffsetY
        val baseScale = max(
            viewportSizePx / bitmap.width.toFloat(),
            viewportSizePx / bitmap.height.toFloat(),
        )
        val maxOffsetX = (((bitmap.width * baseScale * nextZoom) - viewportSizePx) / 2f).coerceAtLeast(0f)
        val maxOffsetY = (((bitmap.height * baseScale * nextZoom) - viewportSizePx) / 2f).coerceAtLeast(0f)
        return nextOffsetX.coerceIn(-maxOffsetX, maxOffsetX) to nextOffsetY.coerceIn(-maxOffsetY, maxOffsetY)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    if (isProcessing) return@Button
                    onConfirm(
                        StartupImageCropTransform(
                            zoom = zoom,
                            offsetXFraction = if (viewportSizePx > 0f) offsetX / viewportSizePx else 0f,
                            offsetYFraction = if (viewportSizePx > 0f) offsetY / viewportSizePx else 0f,
                        ),
                    )
                },
                enabled = !isProcessing,
            ) {
                Text(if (isProcessing) "Preparing..." else "Send")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isProcessing,
            ) {
                Text("Cancel")
            }
        },
        title = {
            Text("Crop Startup Image")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Drag and zoom until the square looks right. Android will upload exactly this square crop.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black)
                        .border(
                            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                            RoundedCornerShape(8.dp),
                        )
                        .pointerInput(bitmap, viewportSizePx, zoom, offsetX, offsetY) {
                            detectTransformGestures { _, pan, gestureZoom, _ ->
                                val nextZoom = (zoom * gestureZoom).coerceIn(1f, 4f)
                                val (clampedX, clampedY) = clampOffsets(
                                    nextOffsetX = offsetX + pan.x,
                                    nextOffsetY = offsetY + pan.y,
                                    nextZoom = nextZoom,
                                )
                                zoom = nextZoom
                                offsetX = clampedX
                                offsetY = clampedY
                            }
                        },
                ) {
                    val density = androidx.compose.ui.platform.LocalDensity.current
                    val viewport = with(density) { maxWidth.toPx() }
                    viewportSizePx = viewport
                    val baseScale = max(
                        viewport / bitmap.width.toFloat(),
                        viewport / bitmap.height.toFloat(),
                    )
                    val baseWidthDp = with(density) { (bitmap.width.toFloat() * baseScale).toDp() }
                    val baseHeightDp = with(density) { (bitmap.height.toFloat() * baseScale).toDp() }
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(width = baseWidthDp, height = baseHeightDp)
                            .graphicsLayer {
                                scaleX = zoom
                                scaleY = zoom
                                translationX = offsetX
                                translationY = offsetY
                            },
                    )
                }
                Slider(
                    value = zoom,
                    onValueChange = { nextZoom ->
                        val (clampedX, clampedY) = clampOffsets(offsetX, offsetY, nextZoom)
                        zoom = nextZoom
                        offsetX = clampedX
                        offsetY = clampedY
                    },
                    valueRange = 1f..4f,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Zoom ${(zoom * 100f).roundToInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(
                        onClick = {
                            zoom = 1f
                            offsetX = 0f
                            offsetY = 0f
                        },
                    ) {
                        Text("Reset")
                    }
                }
            }
        },
    )
}

@Composable
private fun HttpGatewayCard(
    preferences: LocalPreferences,
    gatewayState: HttpGatewayState,
    onSetHttpGatewayEnabled: (Boolean) -> Unit,
    onSaveHttpGatewaySettings: (HttpGatewayPreferences) -> Unit,
    onRotateHttpGatewayAccessKey: () -> Unit,
) {
    var portText by remember(preferences.httpGateway.port) { mutableStateOf(preferences.httpGateway.port.toString()) }
    val parsedPort = portText.toIntOrNull()
    val canSave = parsedPort != null && parsedPort in 1..65535
    val statusColor = when (gatewayState.connectionState) {
        HttpGatewayConnectionState.RUNNING -> MaterialTheme.colorScheme.primary
        HttpGatewayConnectionState.STARTING -> MaterialTheme.colorScheme.tertiary
        HttpGatewayConnectionState.ERROR -> MaterialTheme.colorScheme.error
        HttpGatewayConnectionState.DISABLED -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val statusText = when (gatewayState.connectionState) {
        HttpGatewayConnectionState.DISABLED -> "Disabled"
        HttpGatewayConnectionState.STARTING -> "Starting on port ${gatewayState.port}"
        HttpGatewayConnectionState.RUNNING -> "Listening on port ${gatewayState.port}"
        HttpGatewayConnectionState.ERROR -> gatewayState.lastError ?: "HTTP gateway error"
    }
    val curlExampleUrl = gatewayState.urls.firstOrNull { it.startsWith("http://192.") || it.startsWith("http://10.") || it.startsWith("http://172.") }
        ?: gatewayState.urls.firstOrNull()
        ?: "http://127.0.0.1:${preferences.httpGateway.port}"

    MetricCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("HTTP Gateway", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Lets your phone relay state reads and control commands over simple HTTP for curl, dashboards, or local automation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Checkbox(
                checked = preferences.httpGateway.enabled,
                onCheckedChange = onSetHttpGatewayEnabled,
            )
        }
        Text(
            statusText,
            style = MaterialTheme.typography.bodyMedium,
            color = statusColor,
        )
        if (preferences.httpGateway.enabled) {
            gatewayState.urls.firstOrNull()?.let {
                Text(
                    "Primary URL: $it",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (gatewayState.urls.size > 1) {
                Text(
                    gatewayState.urls.drop(1).joinToString(separator = "\n"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            OutlinedTextField(
                value = portText,
                onValueChange = { portText = it.filter(Char::isDigit).take(5) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Port") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            OutlinedTextField(
                value = preferences.httpGateway.accessKey.ifBlank { "Saved automatically when enabled" },
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Access Key") },
                readOnly = true,
                singleLine = true,
            )
            if (parsedPort == null && portText.isNotBlank()) {
                Text(
                    "Port must be a number between 1 and 65535.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        onSaveHttpGatewaySettings(
                            HttpGatewayPreferences(
                                enabled = preferences.httpGateway.enabled,
                                port = parsedPort ?: preferences.httpGateway.port,
                                accessKey = preferences.httpGateway.accessKey,
                            ),
                        )
                    },
                    enabled = canSave,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Save Gateway")
                }
                OutlinedButton(
                    onClick = onRotateHttpGatewayAccessKey,
                    enabled = preferences.httpGateway.enabled,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Rotate Key")
                }
            }
            Text(
                "Example:\ncurl -H \"X-Voltra-Key: ${preferences.httpGateway.accessKey}\" $curlExampleUrl/v1/state",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                "Enable HTTP Gateway to expose local state and command endpoints on your own network.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MqttSensorCard(
    preferences: LocalPreferences,
    mqttState: MqttPublisherState,
    onSetMqttEnabled: (Boolean) -> Unit,
    onSaveMqttSettings: (MqttPreferences) -> Unit,
    onPublishMqttNow: () -> Unit,
) {
    var host by remember(preferences.mqtt.host) { mutableStateOf(preferences.mqtt.host) }
    var portText by remember(preferences.mqtt.port) { mutableStateOf(preferences.mqtt.port.toString()) }
    var username by remember(preferences.mqtt.username) { mutableStateOf(preferences.mqtt.username) }
    var password by remember(preferences.mqtt.password) { mutableStateOf(preferences.mqtt.password) }
    var topicPrefix by remember(preferences.mqtt.topicPrefix) { mutableStateOf(preferences.mqtt.topicPrefix) }
    var discoveryEnabled by remember(preferences.mqtt.discoveryEnabled) { mutableStateOf(preferences.mqtt.discoveryEnabled) }
    var discoveryPrefix by remember(preferences.mqtt.discoveryPrefix) { mutableStateOf(preferences.mqtt.discoveryPrefix) }

    val parsedPort = portText.toIntOrNull()
    val canSave = host.isNotBlank() && parsedPort != null && parsedPort in 1..65535 && topicPrefix.isNotBlank() &&
        (!discoveryEnabled || discoveryPrefix.isNotBlank())
    val statusColor = when (mqttState.connectionState) {
        MqttPublisherConnectionState.CONNECTED -> MaterialTheme.colorScheme.primary
        MqttPublisherConnectionState.CONNECTING -> MaterialTheme.colorScheme.tertiary
        MqttPublisherConnectionState.ERROR -> MaterialTheme.colorScheme.error
        MqttPublisherConnectionState.DISABLED -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val statusText = when (mqttState.connectionState) {
        MqttPublisherConnectionState.DISABLED -> "Disabled"
        MqttPublisherConnectionState.CONNECTING -> "Connecting to ${mqttState.brokerEndpoint ?: host}"
        MqttPublisherConnectionState.CONNECTED -> "Connected to ${mqttState.brokerEndpoint ?: host}"
        MqttPublisherConnectionState.ERROR -> mqttState.lastError ?: "MQTT error"
    }

    MetricCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("MQTT Sensor", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Publishes retained VOLTRA sensor topics and optional Home Assistant discovery over your local broker.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Checkbox(
                checked = preferences.mqtt.enabled,
                onCheckedChange = onSetMqttEnabled,
            )
        }
        Text(
            statusText,
            style = MaterialTheme.typography.bodyMedium,
            color = statusColor,
        )
        if (preferences.mqtt.enabled) {
            mqttState.topicPrefix?.let { prefix ->
                Text(
                    "Publishing under $prefix",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Broker Host") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = portText,
                    onValueChange = { portText = it.filter(Char::isDigit).take(5) },
                    modifier = Modifier.weight(0.55f),
                    label = { Text("Port") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
            OutlinedTextField(
                value = topicPrefix,
                onValueChange = { topicPrefix = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Topic Prefix") },
                supportingText = { Text("Example: voltra_control") },
                singleLine = true,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Username") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = discoveryEnabled,
                    onCheckedChange = { discoveryEnabled = it },
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text("Home Assistant discovery", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Publishes retained discovery configs so sensors appear automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (discoveryEnabled) {
                OutlinedTextField(
                    value = discoveryPrefix,
                    onValueChange = { discoveryPrefix = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Discovery Prefix") },
                    supportingText = { Text("Usually homeassistant") },
                    singleLine = true,
                )
            }
            if (parsedPort == null && portText.isNotBlank()) {
                Text(
                    "Port must be a number between 1 and 65535.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        onSaveMqttSettings(
                            MqttPreferences(
                                enabled = preferences.mqtt.enabled,
                                host = host.trim(),
                                port = parsedPort ?: preferences.mqtt.port,
                                username = username,
                                password = password,
                                topicPrefix = topicPrefix.trim(),
                                discoveryEnabled = discoveryEnabled,
                                discoveryPrefix = discoveryPrefix.trim(),
                            ),
                        )
                    },
                    enabled = canSave,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Save MQTT")
                }
                OutlinedButton(
                    onClick = onPublishMqttNow,
                    enabled = preferences.mqtt.enabled,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Publish Now")
                }
            }
            Text(
                "Local MQTT publishing stays on your own broker and network.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                "Enable MQTT Sensor to show broker settings and publish local workout data.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ControlPreferencesCard(
    preferences: LocalPreferences,
    onSetInstantWeightApplyDefault: (Boolean) -> Unit,
) {
    MetricCard {
        Text("Control Defaults", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(
            "Long-press either +/- button on Control to cycle the current +/-${preferences.weightIncrement} step.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = preferences.instantWeightApplyDefault,
                onCheckedChange = onSetInstantWeightApplyDefault,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text("Default to instant apply", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "Hold Set during a workout to toggle instantly; unloading still drops back to Set mode.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun WeightPresetLibraryCard(
    presets: List<WeightPreset>,
    onDeletePreset: (String) -> Unit,
) {
    MetricCard {
        Text("Weight Presets", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(
            "Presets saved from the control screens appear here. Tap them on-device to apply quickly, or remove old ones here.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (presets.isEmpty()) {
            Text(
                "No presets saved yet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            presets.forEach { preset ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(preset.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${preset.scope.label} · ${formatWeightValue(preset.value)} ${preset.unit.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    OutlinedButton(onClick = { onDeletePreset(preset.id) }) {
                        Text("Remove")
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutHistoryCard(
    history: List<WorkoutHistoryEntry>,
    onShareWorkoutHistory: () -> Unit,
    onClearWorkoutHistory: () -> Unit,
) {
    val formatter = remember { SimpleDateFormat("MMM d, h:mm a", Locale.US) }
    MetricCard {
        Text("Workout History", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(
            "Local session summaries are saved on-device so you can start building history before the deeper analytics work lands.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = onShareWorkoutHistory,
                enabled = history.isNotEmpty(),
                modifier = Modifier.weight(1f),
            ) {
                Text("Share CSV")
            }
            OutlinedButton(
                onClick = onClearWorkoutHistory,
                enabled = history.isNotEmpty(),
                modifier = Modifier.weight(1f),
            ) {
                Text("Clear")
            }
        }
        if (history.isEmpty()) {
            Text(
                "No completed workouts saved yet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            history.take(6).forEach { entry ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        "${entry.modeLabel} · ${entry.reps} reps · ${entry.sets} sets",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        buildString {
                            append(formatter.format(Date(entry.startedAtMillis)))
                            entry.primarySetting?.takeIf { it.isNotBlank() }?.let {
                                append(" · ")
                                append(it)
                            }
                            entry.peakForceN?.let {
                                append(" · Peak ")
                                append("%.1f N".format(Locale.US, it))
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun DiagnosticsToolsCard(
    onOpenLogs: () -> Unit,
    onShareDiagnostics: () -> Unit,
) {
    MetricCard {
        Text("Logs", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(
            "Connection logs are recorded automatically each time the app connects.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = onOpenLogs,
                modifier = Modifier.weight(1f),
            ) {
                Text("Open Logs")
            }
            OutlinedButton(
                onClick = onShareDiagnostics,
                modifier = Modifier.weight(1f),
            ) {
                Text("Share Logs")
            }
        }
    }
}

@Composable
private fun VersionCard(
    versionLabel: String,
    developerModeEnabled: Boolean,
    onTapVersion: () -> Unit,
) {
    MetricCard {
        Text("App Version", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(
            versionLabel,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable(onClick = onTapVersion),
        )
        if (developerModeEnabled) {
            Text(
                "Advanced mode is enabled on this device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DiagnosticsScreen(
    state: VoltraSessionState,
    onEnableNotifications: () -> Unit,
    onReadVoltraCharacteristics: () -> Unit,
    onReadOnlyHandshakeProbe: () -> Unit,
) {
    val latestSessionFrames = state.rawFrames
        .filter { state.isLatestSessionTimestamp(it.timestampMillis) }
        .takeLast(80)
        .reversed()
    val earlierFrames = state.rawFrames
        .filterNot { state.isLatestSessionTimestamp(it.timestampMillis) }
        .takeLast(80)
        .reversed()
    val latestSessionCommands = state.commandLog
        .filter { state.isLatestSessionTimestamp(it.timestampMillis) }
        .takeLast(80)
        .reversed()
    val earlierCommands = state.commandLog
        .filterNot { state.isLatestSessionTimestamp(it.timestampMillis) }
        .takeLast(80)
        .reversed()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = screenPadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SectionHeader(
                title = "Logs",
                subtitle = "Session details recorded automatically while you connect and train.",
            )
        }
        item {
            ConnectionTraceCard(state)
        }
        item {
            ReadingCard(state.reading)
        }
        item {
            SafetyCard(state)
        }
        item {
            GattSnapshotCard(state.gattSnapshot)
        }
        item {
            SectionHeader("Latest Session Frames")
        }
        if (latestSessionFrames.isEmpty()) {
            item { EmptyState("No session frames recorded in this connection yet.") }
        } else {
            items(latestSessionFrames) { frame ->
                FrameRow(frame)
            }
        }
        if (earlierFrames.isNotEmpty()) {
            item {
                SectionHeader("Earlier Frames")
            }
            items(earlierFrames) { frame ->
                FrameRow(frame)
            }
        }
        item {
            SectionHeader("Latest Session Commands")
        }
        if (latestSessionCommands.isEmpty()) {
            item { EmptyState("No command attempts in this connection.") }
        } else {
            items(latestSessionCommands) { command ->
                CommandRow(command)
            }
        }
        if (earlierCommands.isNotEmpty()) {
            item {
                SectionHeader("Earlier Commands")
            }
            items(earlierCommands) { command ->
                CommandRow(command)
            }
        }
    }
}

@Composable
private fun DeviceStatusCard(
    state: VoltraSessionState,
    onDisconnect: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    state.currentDevice?.name ?: "No VOLTRA Connected",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    state.connectionState.displayText(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = state.connectionState.statusColor(),
                )
                Text(
                    "Battery ${state.reading.batteryPercent?.let { "$it%" } ?: "unknown"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (state.connectionState.hasActiveGattSession()) {
                    OutlinedButton(onClick = onDisconnect) {
                        Text("Disconnect")
                    }
                }
            }
            BatteryGauge(state.reading.batteryPercent)
        }
    }
}

@Composable
private fun BatteryGauge(percent: Int?) {
    val progress = ((percent ?: 0).coerceIn(0, 100) / 100f)
    Box(
        modifier = Modifier.size(62.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            progress = { if (percent == null) 0f else progress },
            modifier = Modifier.fillMaxSize(),
            color = if (percent != null && percent < 15) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            },
            trackColor = MaterialTheme.colorScheme.surface,
        )
        Text(
            percent?.let { "$it%" } ?: "--",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ModeGrid(
    controlReady: Boolean,
    isWideLayout: Boolean,
    onWeightTraining: () -> Unit,
    onResistanceBand: () -> Unit,
    onDamper: () -> Unit,
    onIsokinetic: () -> Unit,
    onIsometric: () -> Unit,
    onCustomCurve: () -> Unit,
    onRowing: () -> Unit,
) {
    val columns = if (isWideLayout) 3 else 2
    val mainModes = listOf(
        ModeTileSpec("Weight Training", ModeIconKind.WEIGHT_TRAINING, if (controlReady) "" else "Connect to enable", controlReady, onWeightTraining),
        ModeTileSpec("Resistance Band", ModeIconKind.RESISTANCE_BAND, if (controlReady) "" else "Connect to enable", controlReady, onResistanceBand),
        ModeTileSpec("Damper", ModeIconKind.DAMPER, if (controlReady) "" else "Connect to enable", controlReady, onDamper),
        ModeTileSpec("Isokinetic", ModeIconKind.ISOKINETIC, if (controlReady) "" else "Connect to enable", controlReady, onIsokinetic),
        ModeTileSpec("Isometric Test", ModeIconKind.ISOMETRIC_TEST, if (controlReady) "" else "Connect to enable", controlReady, onIsometric),
        ModeTileSpec("Custom Curve", ModeIconKind.CUSTOM_CURVE, if (controlReady) "" else "Connect to enable", controlReady, onCustomCurve),
        ModeTileSpec("Rowing", ModeIconKind.ROWING, if (controlReady) "" else "Connect to enable", controlReady, onRowing),
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        mainModes.chunked(columns).forEach { rowModes ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowModes.forEach { mode ->
                    ModeTile(
                        title = mode.title,
                        icon = mode.icon,
                        detail = mode.detail,
                        enabled = mode.enabled,
                        onClick = mode.onClick,
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(columns - rowModes.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private data class ModeTileSpec(
    val title: String,
    val icon: ModeIconKind,
    val detail: String,
    val enabled: Boolean,
    val onClick: () -> Unit,
)

private enum class ModeIconKind {
    WEIGHT_TRAINING,
    RESISTANCE_BAND,
    DAMPER,
    ISOKINETIC,
    ISOMETRIC_TEST,
    CUSTOM_CURVE,
    ROWING,
}

@Composable
private fun ModeTile(
    title: String,
    icon: ModeIconKind,
    detail: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val compactTile = maxWidth < 170.dp
        val tileHeight = if (compactTile) 114.dp else 126.dp
        val tilePadding = if (compactTile) 10.dp else 12.dp
        val iconSize = if (compactTile) 38.dp else 46.dp
        val titleStyle = if (compactTile) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge
        val detailStyle = if (compactTile) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium
        val border = if (enabled) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.65f))
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
        }
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(tileHeight)
                .clickable(enabled = enabled, onClick = onClick),
            colors = CardDefaults.outlinedCardColors(
                containerColor = if (enabled) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    MaterialTheme.colorScheme.surface
                },
            ),
            border = border,
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(tilePadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ModeGlyph(
                    kind = icon,
                    enabled = enabled,
                    modifier = Modifier.size(iconSize),
                )
                Spacer(modifier = Modifier.height(if (compactTile) 10.dp else 14.dp))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        title,
                        style = titleStyle,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (detail.isNotBlank()) {
                        Text(
                            detail,
                            style = detailStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.heightIn(min = if (compactTile) 28.dp else 18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeGlyph(
    kind: ModeIconKind,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier = modifier) {
        val stroke = size.minDimension * 0.08f
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        when (kind) {
            ModeIconKind.WEIGHT_TRAINING -> {
                drawLine(color, Offset(size.width * 0.2f, centerY), Offset(size.width * 0.8f, centerY), strokeWidth = stroke, cap = StrokeCap.Round)
                listOf(0.22f, 0.34f, 0.66f, 0.78f).forEach { x ->
                    drawLine(
                        color,
                        Offset(size.width * x, centerY - size.height * 0.16f),
                        Offset(size.width * x, centerY + size.height * 0.16f),
                        strokeWidth = stroke * 1.1f,
                        cap = StrokeCap.Round,
                    )
                }
            }
            ModeIconKind.RESISTANCE_BAND -> {
                val path = Path().apply {
                    moveTo(size.width * 0.18f, centerY)
                    cubicTo(size.width * 0.28f, size.height * 0.18f, size.width * 0.44f, size.height * 0.18f, centerX, centerY)
                    cubicTo(size.width * 0.56f, size.height * 0.82f, size.width * 0.72f, size.height * 0.82f, size.width * 0.82f, centerY)
                    cubicTo(size.width * 0.72f, size.height * 0.18f, size.width * 0.56f, size.height * 0.18f, centerX, centerY)
                    cubicTo(size.width * 0.44f, size.height * 0.82f, size.width * 0.28f, size.height * 0.82f, size.width * 0.18f, centerY)
                }
                drawPath(path, color = color, style = Stroke(width = stroke, cap = StrokeCap.Round))
            }
            ModeIconKind.DAMPER -> {
                val canopy = Path().apply {
                    moveTo(size.width * 0.2f, size.height * 0.44f)
                    quadraticTo(centerX, size.height * 0.14f, size.width * 0.8f, size.height * 0.44f)
                    quadraticTo(size.width * 0.68f, size.height * 0.54f, centerX, size.height * 0.56f)
                    quadraticTo(size.width * 0.32f, size.height * 0.54f, size.width * 0.2f, size.height * 0.44f)
                }
                drawPath(canopy, color = color, style = Stroke(width = stroke, cap = StrokeCap.Round))
                listOf(0.32f, 0.5f, 0.68f).forEach { x ->
                    drawLine(
                        color,
                        Offset(size.width * x, size.height * 0.5f),
                        Offset(centerX, size.height * 0.78f),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round,
                    )
                }
                drawLine(
                    color,
                    Offset(centerX, size.height * 0.78f),
                    Offset(centerX, size.height * 0.88f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }
            ModeIconKind.ISOKINETIC -> {
                listOf(0.28f, 0.5f, 0.72f).forEach { y ->
                    drawLine(
                        color,
                        Offset(size.width * 0.28f, centerY),
                        Offset(size.width * 0.82f, size.height * y),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round,
                    )
                }
            }
            ModeIconKind.ISOMETRIC_TEST -> {
                drawArc(
                    color = color,
                    startAngle = 205f,
                    sweepAngle = 130f,
                    useCenter = false,
                    topLeft = Offset(size.width * 0.18f, size.height * 0.26f),
                    size = Size(size.width * 0.64f, size.height * 0.5f),
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                drawLine(
                    color,
                    Offset(centerX, size.height * 0.56f),
                    Offset(size.width * 0.72f, size.height * 0.32f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }
            ModeIconKind.CUSTOM_CURVE -> {
                drawLine(
                    color,
                    Offset(size.width * 0.18f, size.height * 0.78f),
                    Offset(size.width * 0.82f, size.height * 0.78f),
                    strokeWidth = stroke * 0.82f,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color,
                    Offset(size.width * 0.18f, size.height * 0.78f),
                    Offset(size.width * 0.18f, size.height * 0.24f),
                    strokeWidth = stroke * 0.82f,
                    cap = StrokeCap.Round,
                )
                val path = Path().apply {
                    moveTo(size.width * 0.24f, size.height * 0.72f)
                    cubicTo(size.width * 0.36f, size.height * 0.68f, size.width * 0.48f, size.height * 0.56f, size.width * 0.58f, size.height * 0.48f)
                    cubicTo(size.width * 0.66f, size.height * 0.4f, size.width * 0.72f, size.height * 0.3f, size.width * 0.8f, size.height * 0.26f)
                }
                drawPath(path, color = color, style = Stroke(width = stroke, cap = StrokeCap.Round))
                drawCircle(color = color, radius = stroke * 0.62f, center = Offset(size.width * 0.24f, size.height * 0.72f))
                drawCircle(color = color, radius = stroke * 0.62f, center = Offset(size.width * 0.56f, size.height * 0.5f))
                drawCircle(color = color, radius = stroke * 0.62f, center = Offset(size.width * 0.8f, size.height * 0.26f))
            }
            ModeIconKind.ROWING -> {
                drawLine(color, Offset(size.width * 0.22f, size.height * 0.76f), Offset(size.width * 0.82f, size.height * 0.76f), strokeWidth = stroke, cap = StrokeCap.Round)
                drawCircle(color = color, radius = stroke * 0.65f, center = Offset(size.width * 0.38f, size.height * 0.34f))
                drawLine(color, Offset(size.width * 0.38f, size.height * 0.42f), Offset(size.width * 0.5f, size.height * 0.56f), strokeWidth = stroke, cap = StrokeCap.Round)
                drawLine(color, Offset(size.width * 0.5f, size.height * 0.56f), Offset(size.width * 0.66f, size.height * 0.52f), strokeWidth = stroke, cap = StrokeCap.Round)
                drawLine(color, Offset(size.width * 0.47f, size.height * 0.52f), Offset(size.width * 0.36f, size.height * 0.66f), strokeWidth = stroke, cap = StrokeCap.Round)
                drawLine(color, Offset(size.width * 0.56f, size.height * 0.56f), Offset(size.width * 0.7f, size.height * 0.74f), strokeWidth = stroke, cap = StrokeCap.Round)
                drawLine(color, Offset(size.width * 0.58f, size.height * 0.46f), Offset(size.width * 0.8f, size.height * 0.28f), strokeWidth = stroke, cap = StrokeCap.Round)
            }
        }
    }
}

@Composable
private fun ControlModeSelector(
    selectedMode: ControlModeUi,
    onSelectMode: (ControlModeUi) -> Unit,
) {
    MetricCard {
        Text("Mode", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(
            "Switch between the VOLTRA control profiles here.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = selectedMode == ControlModeUi.WEIGHT_TRAINING,
                onClick = { onSelectMode(ControlModeUi.WEIGHT_TRAINING) },
                label = { Text("Weight") },
            )
            FilterChip(
                selected = selectedMode == ControlModeUi.RESISTANCE_BAND,
                onClick = { onSelectMode(ControlModeUi.RESISTANCE_BAND) },
                label = { Text("Resistance") },
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = selectedMode == ControlModeUi.DAMPER,
                onClick = { onSelectMode(ControlModeUi.DAMPER) },
                label = { Text("Damper") },
            )
            FilterChip(
                selected = selectedMode == ControlModeUi.ISOKINETIC,
                onClick = { onSelectMode(ControlModeUi.ISOKINETIC) },
                label = { Text("Isokinetic") },
            )
        }
    }
}

@Composable
private fun WeightTrainingCard(
    modeTitle: String,
    targetLabel: String,
    unit: WeightUnit,
    pendingTarget: Double,
    minLoad: Double,
    maxLoad: Double,
    weightStep: Double,
    isLoaded: Boolean,
    controlReady: Boolean,
    canLoad: Boolean,
    instantApply: Boolean,
    status: String,
    settingsLabel: String,
    onOpenSettings: () -> Unit,
    sets: Int?,
    reps: Int?,
    phase: String?,
    onTargetChange: (Double) -> Unit,
    onTargetSettled: (Double) -> Unit,
    onSetTarget: () -> Unit,
    onTargetCommit: (Double) -> Unit,
    onToggleInstantApply: () -> Unit,
    onCycleWeightIncrement: () -> Unit,
    presets: List<WeightPreset>,
    onSavePreset: (String, Double) -> Unit,
    onApplyPreset: (WeightPreset) -> Unit,
    wideLayout: Boolean,
    onSetUnit: (WeightUnit) -> Unit,
    unitSwitchEnabled: Boolean,
    onLoad: () -> Unit,
    onUnload: () -> Unit,
    cableLengthEnabled: Boolean,
    onTriggerCableLength: () -> Unit,
    onExitWorkout: () -> Unit,
) {
    val accent = LocalControlAccent.current
    var showWeightDialog by remember { mutableStateOf(false) }
    var dialogInput by remember { mutableStateOf("") }
    var showSavePresetDialog by remember { mutableStateOf(false) }
    var presetNameInput by remember { mutableStateOf("") }
    val displayValueLabel = "${formatWeightValue(pendingTarget)} ${unit.label}"
    val cardPadding = if (wideLayout) 16.dp else 18.dp
    val sectionSpacing = if (wideLayout) 14.dp else 16.dp

    if (showWeightDialog) {
        AlertDialog(
            onDismissRequest = { showWeightDialog = false },
            title = { Text("Set $targetLabel") },
            text = {
                OutlinedTextField(
                    value = dialogInput,
                    onValueChange = { dialogInput = it },
                    label = { Text(unit.label) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val parsed = dialogInput.toDoubleOrNull()
                    if (parsed != null) {
                        val target = snapWeight(parsed, minLoad, maxLoad, weightStep)
                        onTargetChange(target)
                        onTargetCommit(target)
                    }
                    showWeightDialog = false
                }) { Text("Set") }
            },
            dismissButton = {
                TextButton(onClick = { showWeightDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (showSavePresetDialog) {
        AlertDialog(
            onDismissRequest = { showSavePresetDialog = false },
            title = { Text("Save Preset") },
            text = {
                OutlinedTextField(
                    value = presetNameInput,
                    onValueChange = { presetNameInput = it.take(24) },
                    label = { Text("Preset Name") },
                    supportingText = { Text(displayValueLabel) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSavePreset(presetNameInput, pendingTarget)
                        presetNameInput = ""
                        showSavePresetDialog = false
                    },
                    enabled = presetNameInput.isNotBlank(),
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSavePresetDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(cardPadding),
            verticalArrangement = Arrangement.spacedBy(sectionSpacing),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (wideLayout) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(modeTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            status,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AssistChip(
                            onClick = {
                                presetNameInput = ""
                                showSavePresetDialog = true
                            },
                            enabled = controlReady,
                            label = { Text("Save Preset") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = accent.accent,
                            ),
                        )
                        AssistChip(
                            onClick = onOpenSettings,
                            enabled = controlReady,
                            label = { Text(settingsLabel) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = accent.accentContainer,
                                labelColor = accent.onAccentContainer,
                                disabledContainerColor = accent.accentContainer.copy(alpha = 0.45f),
                                disabledLabelColor = accent.onAccentContainer.copy(alpha = 0.7f),
                            ),
                        )
                        AssistChip(
                            onClick = {
                                if (isLoaded) {
                                    onUnload()
                                } else {
                                    onLoad()
                                }
                            },
                            enabled = controlReady && (isLoaded || canLoad),
                            label = { Text(if (isLoaded) "Weight Off" else "Load") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = accent.accentContainer,
                                labelColor = accent.onAccentContainer,
                                disabledContainerColor = accent.accentContainer.copy(alpha = 0.45f),
                                disabledLabelColor = accent.onAccentContainer.copy(alpha = 0.7f),
                            ),
                        )
                    }
                }
                if (presets.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        presets.forEach { preset ->
                            AssistChip(
                                onClick = { onApplyPreset(preset) },
                                label = {
                                    Text(
                                        "${preset.name} · ${formatWeightValue(convertWeightValue(preset.value, preset.unit, unit))} ${unit.label}",
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = accent.accentContainer.copy(alpha = 0.88f),
                                    labelColor = accent.onAccentContainer,
                                ),
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(modeTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            status,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    AssistChip(
                        onClick = {
                            if (isLoaded) {
                                onUnload()
                            } else {
                                onLoad()
                            }
                        },
                        enabled = controlReady && (isLoaded || canLoad),
                        label = { Text(if (isLoaded) "Weight Off" else "Load") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = accent.accentContainer,
                            labelColor = accent.onAccentContainer,
                            disabledContainerColor = accent.accentContainer.copy(alpha = 0.45f),
                            disabledLabelColor = accent.onAccentContainer.copy(alpha = 0.7f),
                        ),
                    )
                }
                AssistChip(
                    onClick = onOpenSettings,
                    enabled = controlReady,
                    label = { Text(settingsLabel) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = accent.accentContainer,
                        labelColor = accent.onAccentContainer,
                        disabledContainerColor = accent.accentContainer.copy(alpha = 0.45f),
                        disabledLabelColor = accent.onAccentContainer.copy(alpha = 0.7f),
                    ),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AssistChip(
                        onClick = {
                            presetNameInput = ""
                            showSavePresetDialog = true
                        },
                        enabled = controlReady,
                        label = { Text("Save Preset") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = accent.accent,
                        ),
                    )
                    presets.forEach { preset ->
                        AssistChip(
                            onClick = { onApplyPreset(preset) },
                            label = {
                                Text(
                                    "${preset.name} · ${formatWeightValue(convertWeightValue(preset.value, preset.unit, unit))} ${unit.label}",
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = accent.accentContainer.copy(alpha = 0.88f),
                                labelColor = accent.onAccentContainer,
                            ),
                        )
                    }
                }
            }
            if (wideLayout) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier.weight(1.16f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        DigitalWeightDial(
                            label = displayValueLabel,
                            unit = unit,
                            value = pendingTarget,
                            minLoad = minLoad,
                            maxLoad = maxLoad,
                            step = weightStep,
                            instantApply = instantApply,
                            onTargetChange = onTargetChange,
                            onTargetSettled = onTargetSettled,
                            onCycleStep = onCycleWeightIncrement,
                            onOpenDial = {
                                dialogInput = formatWeightValue(pendingTarget)
                                showWeightDialog = true
                            },
                            denseLayout = true,
                        )
                        HoldToLoadButton(
                            enabled = controlReady && canLoad,
                            onLoad = onLoad,
                        )
                    }
                    Column(
                        modifier = Modifier.weight(0.84f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            FilterChip(
                                selected = unit == WeightUnit.LB,
                                onClick = { onSetUnit(WeightUnit.LB) },
                                enabled = unitSwitchEnabled,
                                label = { Text("lb") },
                            )
                            FilterChip(
                                selected = unit == WeightUnit.KG,
                                onClick = { onSetUnit(WeightUnit.KG) },
                                enabled = unitSwitchEnabled,
                                label = { Text("kg") },
                            )
                            Spacer(Modifier.weight(1f))
                            SetModeButton(
                                instantApply = instantApply,
                                onClick = onSetTarget,
                                onLongClick = onToggleInstantApply,
                                enabled = controlReady,
                            )
                        }
                        WorkoutTelemetryStrip(
                            sets = sets,
                            reps = reps,
                            phase = phase,
                            status = status,
                            isLoaded = isLoaded,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(
                                onClick = onExitWorkout,
                                enabled = controlReady,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Exit Mode")
                            }
                            FilledTonalButton(
                                onClick = {
                                    if (isLoaded) onUnload() else onLoad()
                                },
                                enabled = controlReady && (isLoaded || canLoad),
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = accent.accentContainer,
                                    contentColor = accent.onAccentContainer,
                                ),
                            ) {
                                Text("Unload")
                            }
                        }
                        OutlinedButton(
                            onClick = onTriggerCableLength,
                            enabled = cableLengthEnabled,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Cable Length")
                        }
                    }
                }
            } else {
                DigitalWeightDial(
                    label = displayValueLabel,
                    unit = unit,
                    value = pendingTarget,
                    minLoad = minLoad,
                    maxLoad = maxLoad,
                    step = weightStep,
                    instantApply = instantApply,
                    onTargetChange = onTargetChange,
                    onTargetSettled = onTargetSettled,
                    onCycleStep = onCycleWeightIncrement,
                    onOpenDial = {
                        dialogInput = formatWeightValue(pendingTarget)
                        showWeightDialog = true
                    },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilterChip(
                        selected = unit == WeightUnit.LB,
                        onClick = { onSetUnit(WeightUnit.LB) },
                        enabled = unitSwitchEnabled,
                        label = { Text("lb") },
                    )
                    FilterChip(
                        selected = unit == WeightUnit.KG,
                        onClick = { onSetUnit(WeightUnit.KG) },
                        enabled = unitSwitchEnabled,
                        label = { Text("kg") },
                    )
                    Spacer(Modifier.weight(1f))
                    SetModeButton(
                        instantApply = instantApply,
                        onClick = onSetTarget,
                        onLongClick = onToggleInstantApply,
                        enabled = controlReady,
                    )
                }
                WorkoutTelemetryStrip(
                    sets = sets,
                    reps = reps,
                    phase = phase,
                    status = status,
                    isLoaded = isLoaded,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onExitWorkout,
                        enabled = controlReady,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Exit Mode")
                    }
                    FilledTonalButton(
                        onClick = {
                            if (isLoaded) onUnload() else onLoad()
                        },
                        enabled = controlReady && (isLoaded || canLoad),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = accent.accentContainer,
                            contentColor = accent.onAccentContainer,
                        ),
                    ) {
                        Text("Unload")
                    }
                }
                OutlinedButton(
                    onClick = onTriggerCableLength,
                    enabled = cableLengthEnabled,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Cable Length")
                }
                HoldToLoadButton(
                    enabled = controlReady && canLoad,
                    onLoad = onLoad,
                )
            }
        }
    }
}

@Composable
private fun DamperModeCard(
    level: Double,
    isLoaded: Boolean,
    controlReady: Boolean,
    canLoad: Boolean,
    status: String,
    sets: Int?,
    reps: Int?,
    phase: String?,
    peakForceLb: Double?,
    peakPowerWatts: Int?,
    timeToPeakMillis: Long?,
    history: List<PowerWorkoutHistoryEntry>,
    wideLayout: Boolean,
    onOpenSettings: () -> Unit,
    onLevelChange: (Double) -> Unit,
    onLoad: () -> Unit,
    onUnload: () -> Unit,
    onTriggerCableLength: () -> Unit,
    cableLengthEnabled: Boolean,
    onExitWorkout: () -> Unit,
) {
    val accent = LocalControlAccent.current
    var dragLevel by remember { mutableDoubleStateOf(level) }
    val powerSummaryText = formatPowerWorkoutSummary(peakForceLb, peakPowerWatts, timeToPeakMillis)

    LaunchedEffect(level) {
        dragLevel = level
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Damper", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        status,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                AssistChip(
                    onClick = {
                        if (isLoaded) onUnload() else onLoad()
                    },
                    enabled = controlReady && (isLoaded || canLoad),
                    label = { Text(if (isLoaded) "Weight Off" else "Load") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = accent.accentContainer,
                        labelColor = accent.onAccentContainer,
                        disabledContainerColor = accent.accentContainer.copy(alpha = 0.45f),
                        disabledLabelColor = accent.onAccentContainer.copy(alpha = 0.7f),
                    ),
                )
            }
            AssistChip(
                onClick = onOpenSettings,
                enabled = controlReady,
                label = { Text("Damper Settings") },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = accent.accentContainer,
                    labelColor = accent.onAccentContainer,
                    disabledContainerColor = accent.accentContainer.copy(alpha = 0.45f),
                    disabledLabelColor = accent.onAccentContainer.copy(alpha = 0.7f),
                ),
            )
            if (wideLayout) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            formatDamperFactor(dragLevel),
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = accent.accent,
                        )
                        Text(
                            "Factor",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "Level ${dragLevel.roundToInt()}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Slider(
                            value = dragLevel.toFloat(),
                            onValueChange = {
                                dragLevel = snapWeight(it.toDouble(), 1.0, 10.0, 1.0)
                            },
                            onValueChangeFinished = {
                                onLevelChange(dragLevel)
                            },
                            valueRange = 1f..10f,
                            steps = 8,
                            colors = SliderDefaults.colors(
                                thumbColor = accent.accent,
                                activeTrackColor = accent.accent,
                            ),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("5 factor", style = MaterialTheme.typography.labelMedium)
                            Text("50 factor", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Column(
                        modifier = Modifier.weight(0.92f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        WorkoutTelemetryStrip(
                            sets = sets,
                            reps = reps,
                            phase = phase,
                            status = status,
                            isLoaded = isLoaded,
                            summaryText = powerSummaryText,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(
                                onClick = onExitWorkout,
                                enabled = controlReady,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Exit Mode")
                            }
                            FilledTonalButton(
                                onClick = onUnload,
                                enabled = controlReady,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = accent.accentContainer,
                                    contentColor = accent.onAccentContainer,
                                ),
                            ) {
                                Text("Unload")
                            }
                        }
                        OutlinedButton(
                            onClick = onTriggerCableLength,
                            enabled = cableLengthEnabled,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Cable Length")
                        }
                        HoldToLoadButton(
                            enabled = controlReady && canLoad,
                            onLoad = onLoad,
                        )
                    }
                }
            } else {
                Text(
                    formatDamperFactor(dragLevel),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = accent.accent,
                )
                Text(
                    "Factor",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Level ${dragLevel.roundToInt()}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = dragLevel.toFloat(),
                    onValueChange = {
                        dragLevel = snapWeight(it.toDouble(), 1.0, 10.0, 1.0)
                    },
                    onValueChangeFinished = {
                        onLevelChange(dragLevel)
                    },
                    valueRange = 1f..10f,
                    steps = 8,
                    colors = SliderDefaults.colors(
                        thumbColor = accent.accent,
                        activeTrackColor = accent.accent,
                    ),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("5 factor", style = MaterialTheme.typography.labelMedium)
                    Text("50 factor", style = MaterialTheme.typography.labelMedium)
                }
                WorkoutTelemetryStrip(
                    sets = sets,
                    reps = reps,
                    phase = phase,
                    status = status,
                    isLoaded = isLoaded,
                    summaryText = powerSummaryText,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onExitWorkout,
                        enabled = controlReady,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Exit Mode")
                    }
                    FilledTonalButton(
                        onClick = onUnload,
                        enabled = controlReady,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = accent.accentContainer,
                            contentColor = accent.onAccentContainer,
                        ),
                    ) {
                        Text("Unload")
                    }
                }
                OutlinedButton(
                    onClick = onTriggerCableLength,
                    enabled = cableLengthEnabled,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Cable Length")
                }
                HoldToLoadButton(
                    enabled = controlReady && canLoad,
                    onLoad = onLoad,
                )
            }
            PowerWorkoutHistoryList(history)
        }
    }
}

@Composable
private fun IsokineticModeCard(
    speedOptions: List<Double>,
    targetSpeedIndex: Int,
    maxEccentricLoad: Double,
    constantResistance: Double,
    isLoaded: Boolean,
    controlReady: Boolean,
    canLoad: Boolean,
    status: String,
    sets: Int?,
    reps: Int?,
    phase: String?,
    peakForceLb: Double?,
    peakPowerWatts: Int?,
    timeToPeakMillis: Long?,
    history: List<PowerWorkoutHistoryEntry>,
    wideLayout: Boolean,
    onSelectTargetSpeedIndex: (Int) -> Unit,
    onLoad: () -> Unit,
    onUnload: () -> Unit,
    onTriggerCableLength: () -> Unit,
    cableLengthEnabled: Boolean,
    onExitWorkout: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val accent = LocalControlAccent.current
    var sliderSpeedIndex by remember { mutableFloatStateOf(targetSpeedIndex.toFloat()) }
    var isDraggingSpeed by remember { mutableStateOf(false) }
    val powerSummaryText = formatPowerWorkoutSummary(peakForceLb, peakPowerWatts, timeToPeakMillis)

    LaunchedEffect(targetSpeedIndex) {
        if (!isDraggingSpeed) {
            sliderSpeedIndex = targetSpeedIndex.toFloat()
        }
    }

    val displayedSpeedIndex = sliderSpeedIndex
        .roundToInt()
        .coerceIn(1, speedOptions.lastIndex)
    val displayedSpeedValue = speedOptions[displayedSpeedIndex]

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Isokinetic", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        status,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                AssistChip(
                    onClick = {
                        if (isLoaded) onUnload() else onLoad()
                    },
                    enabled = controlReady && (isLoaded || canLoad),
                    label = { Text(if (isLoaded) "Weight Off" else "Load") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = accent.accentContainer,
                        labelColor = accent.onAccentContainer,
                        disabledContainerColor = accent.accentContainer.copy(alpha = 0.45f),
                        disabledLabelColor = accent.onAccentContainer.copy(alpha = 0.7f),
                    ),
                )
            }
            AssistChip(
                onClick = onOpenSettings,
                enabled = controlReady,
                label = { Text("Eccentric Settings") },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = accent.accentContainer,
                    labelColor = accent.onAccentContainer,
                    disabledContainerColor = accent.accentContainer.copy(alpha = 0.45f),
                    disabledLabelColor = accent.onAccentContainer.copy(alpha = 0.7f),
                ),
            )
            if (wideLayout) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier.weight(1.02f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            formatSpeedOptionLabel(displayedSpeedValue),
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = accent.accent,
                        )
                        Text(
                            "m/s",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Slider(
                            value = sliderSpeedIndex,
                            onValueChange = {
                                isDraggingSpeed = true
                                sliderSpeedIndex = it
                            },
                            onValueChangeFinished = {
                                val nextIndex = sliderSpeedIndex
                                    .roundToInt()
                                    .coerceIn(1, speedOptions.lastIndex)
                                sliderSpeedIndex = nextIndex.toFloat()
                                isDraggingSpeed = false
                                if (nextIndex != targetSpeedIndex) {
                                    onSelectTargetSpeedIndex(nextIndex)
                                }
                            },
                            valueRange = 1f..speedOptions.lastIndex.toFloat(),
                            steps = (speedOptions.lastIndex - 2).coerceAtLeast(0),
                            enabled = controlReady,
                            colors = SliderDefaults.colors(
                                thumbColor = accent.accent,
                                activeTrackColor = accent.accent,
                            ),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("0.1", style = MaterialTheme.typography.labelMedium)
                            Text(
                                formatSpeedOptionLabel(displayedSpeedValue),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text("2.0", style = MaterialTheme.typography.labelMedium)
                        }
                        Text(
                            "Eccentric settings: ${formatWeightValue(maxEccentricLoad)} lb max load, ${formatWeightValue(constantResistance)} lb constant resistance",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Column(
                        modifier = Modifier.weight(0.98f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        WorkoutTelemetryStrip(
                            sets = sets,
                            reps = reps,
                            phase = phase,
                            status = status,
                            isLoaded = isLoaded,
                            summaryText = powerSummaryText,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(
                                onClick = onExitWorkout,
                                enabled = controlReady,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Exit Mode")
                            }
                            FilledTonalButton(
                                onClick = onUnload,
                                enabled = controlReady,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = accent.accentContainer,
                                    contentColor = accent.onAccentContainer,
                                ),
                            ) {
                                Text("Unload")
                            }
                        }
                        OutlinedButton(
                            onClick = onTriggerCableLength,
                            enabled = cableLengthEnabled,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Cable Length")
                        }
                        HoldToLoadButton(
                            enabled = controlReady && canLoad,
                            onLoad = onLoad,
                        )
                    }
                }
            } else {
                Text(
                    formatSpeedOptionLabel(displayedSpeedValue),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = accent.accent,
                )
                Text(
                    "m/s",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = sliderSpeedIndex,
                    onValueChange = {
                        isDraggingSpeed = true
                        sliderSpeedIndex = it
                    },
                    onValueChangeFinished = {
                        val nextIndex = sliderSpeedIndex
                            .roundToInt()
                            .coerceIn(1, speedOptions.lastIndex)
                        sliderSpeedIndex = nextIndex.toFloat()
                        isDraggingSpeed = false
                        if (nextIndex != targetSpeedIndex) {
                            onSelectTargetSpeedIndex(nextIndex)
                        }
                    },
                    valueRange = 1f..speedOptions.lastIndex.toFloat(),
                    steps = (speedOptions.lastIndex - 2).coerceAtLeast(0),
                    enabled = controlReady,
                    colors = SliderDefaults.colors(
                        thumbColor = accent.accent,
                        activeTrackColor = accent.accent,
                    ),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("0.1", style = MaterialTheme.typography.labelMedium)
                    Text(
                        formatSpeedOptionLabel(displayedSpeedValue),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text("2.0", style = MaterialTheme.typography.labelMedium)
                }
                Text(
                    "Eccentric settings: ${formatWeightValue(maxEccentricLoad)} lb max load, ${formatWeightValue(constantResistance)} lb constant resistance",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                WorkoutTelemetryStrip(
                    sets = sets,
                    reps = reps,
                    phase = phase,
                    status = status,
                    isLoaded = isLoaded,
                    summaryText = powerSummaryText,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onExitWorkout,
                        enabled = controlReady,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Exit Mode")
                    }
                    FilledTonalButton(
                        onClick = onUnload,
                        enabled = controlReady,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = accent.accentContainer,
                            contentColor = accent.onAccentContainer,
                        ),
                    ) {
                        Text("Unload")
                    }
                }
                OutlinedButton(
                    onClick = onTriggerCableLength,
                    enabled = cableLengthEnabled,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Cable Length")
                }
                HoldToLoadButton(
                    enabled = controlReady && canLoad,
                    onLoad = onLoad,
                )
            }
            PowerWorkoutHistoryList(history)
        }
    }
}

@Composable
private fun IsometricTestCard(
    status: String,
    currentForceN: Double?,
    peakForceN: Double?,
    peakRelativeForcePercent: Double?,
    elapsedMillis: Long?,
    waveformSamplesN: List<Double>,
    maxForceLb: Double?,
    maxDurationSeconds: Int?,
    isLoaded: Boolean,
    controlReady: Boolean,
    canLoad: Boolean,
    canRequestLoad: Boolean,
    sets: Int?,
    reps: Int?,
    phase: String?,
    wideLayout: Boolean,
    onLoad: () -> Unit,
    onUnload: () -> Unit,
    onExitWorkout: () -> Unit,
) {
    val accent = LocalControlAccent.current
    val samples = remember { mutableStateListOf<IsometricForceSample>() }
    var previouslyLoaded by remember { mutableStateOf(isLoaded) }

    LaunchedEffect(isLoaded) {
        if (isLoaded && !previouslyLoaded) {
            samples.clear()
        }
        previouslyLoaded = isLoaded
    }

    LaunchedEffect(waveformSamplesN, elapsedMillis, isLoaded, currentForceN) {
        if (waveformSamplesN.isEmpty()) {
            if (!isLoaded || currentForceN == null || elapsedMillis == null) {
                samples.clear()
            }
            return@LaunchedEffect
        }
        val effectiveDurationMillis = elapsedMillis?.takeIf { it > 0L }
        val stepMillis = when {
            waveformSamplesN.size > 1 && effectiveDurationMillis != null ->
                effectiveDurationMillis.toDouble() / (waveformSamplesN.size - 1).toDouble()

            else -> 4.0
        }
        samples.clear()
        waveformSamplesN.forEachIndexed { index, forceN ->
            samples += IsometricForceSample(
                elapsedMillis = (index * stepMillis).roundToLong(),
                forceN = forceN.coerceAtLeast(0.0),
            )
        }
    }

    LaunchedEffect(currentForceN, elapsedMillis, isLoaded) {
        if (!isLoaded || currentForceN == null || elapsedMillis == null) return@LaunchedEffect
        val nextSample = IsometricForceSample(
            elapsedMillis = elapsedMillis,
            forceN = currentForceN.coerceAtLeast(0.0),
        )
        when {
            samples.isEmpty() -> samples += nextSample
            nextSample.elapsedMillis < samples.last().elapsedMillis -> {
                samples.clear()
                samples += nextSample
            }

            nextSample.elapsedMillis == samples.last().elapsedMillis -> {
                samples[samples.lastIndex] = nextSample
            }

            else -> {
                samples += nextSample
                if (samples.size > 240) {
                    repeat(samples.size - 240) { samples.removeAt(0) }
                }
            }
        }
    }

    val computedMetrics = computeIsometricMetrics(samples)
    val hasTraceMetrics = samples.size >= 2
    val averageSampleStepMillis = if (samples.size > 1) {
        samples.zipWithNext { previous, next ->
            (next.elapsedMillis - previous.elapsedMillis).coerceAtLeast(0L)
        }.average()
    } else {
        null
    }
    val hasDenseWaveform = averageSampleStepMillis != null && averageSampleStepMillis <= 25.0
    val hasWaveformTrace = samples.isNotEmpty()
    val preferCompletedSummary =
        currentForceN == null &&
            peakRelativeForcePercent != null &&
            peakForceN != null &&
            elapsedMillis != null &&
            !hasTraceMetrics
    val displayedCurrentForce = when {
        isLoaded && hasTraceMetrics -> computedMetrics.currentForceN ?: currentForceN
        isLoaded -> currentForceN
        else -> null
    }
    val displayedPeakForce = when {
        preferCompletedSummary -> peakForceN
        hasTraceMetrics -> computedMetrics.peakForceN ?: peakForceN
        hasWaveformTrace -> computedMetrics.peakForceN ?: peakForceN
        else -> peakForceN ?: computedMetrics.peakForceN
    }
    val displayedElapsedMillis = when {
        preferCompletedSummary -> elapsedMillis
        hasTraceMetrics -> computedMetrics.durationMillis ?: elapsedMillis
        hasWaveformTrace -> computedMetrics.durationMillis ?: elapsedMillis
        isLoaded -> elapsedMillis ?: computedMetrics.durationMillis
        else -> computedMetrics.durationMillis
    }
    val displayedPeakRelativeForce = peakRelativeForcePercent?.takeIf { currentForceN == null }
    val leadingMetricTitle = if (displayedPeakRelativeForce != null) "Peak Relative Force" else "Current Force"
    val leadingMetricValue = if (displayedPeakRelativeForce != null) {
        "${formatPercentage(displayedPeakRelativeForce)}%"
    } else {
        displayedCurrentForce?.roundToInt()?.let { "$it N" } ?: "--"
    }
    val hasCompletedSummary = samples.isNotEmpty() ||
        displayedPeakForce != null ||
        displayedElapsedMillis != null ||
        displayedPeakRelativeForce != null
    val helperStatus = when {
        isLoaded -> "Pull against the VOLTRA to capture live force."
        hasCompletedSummary -> "Last completed test is shown below. Load again to start a new attempt."
        canLoad || canRequestLoad -> "The VOLTRA is already in Isometric ready. Pull to start, or tap Refresh if the live stream misses."
        else -> "Waiting for the VOLTRA to enter Isometric ready."
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Isometric", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        status,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                AssistChip(
                    onClick = {
                        if (isLoaded) onUnload() else onLoad()
                    },
                    enabled = if (isLoaded) {
                        controlReady
                    } else {
                        controlReady && (canLoad || canRequestLoad)
                    },
                    label = { Text(if (isLoaded) "Weight Off" else "Refresh") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = accent.accentContainer,
                        labelColor = accent.onAccentContainer,
                        disabledContainerColor = accent.accentContainer.copy(alpha = 0.45f),
                        disabledLabelColor = accent.onAccentContainer.copy(alpha = 0.7f),
                    ),
                )
            }
            if (wideLayout) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier.weight(1.08f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Realtime Data", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                IsometricRealtimeChart(
                                    samples = samples,
                                    maxForceN = computedMetrics.graphMaxForceN,
                                    accentColor = accent.accent,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(280.dp),
                                )
                            }
                        }
                    }
                    Column(
                        modifier = Modifier.weight(0.92f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    IsometricMetricCell(
                                        title = leadingMetricTitle,
                                        value = leadingMetricValue,
                                        valueColor = accent.accent,
                                        modifier = Modifier.weight(1f),
                                    )
                                    IsometricMetricCell(
                                        title = "Time",
                                        value = displayedElapsedMillis?.let(::formatElapsedClock) ?: "--:--",
                                        valueColor = Color(0xFFFFE45C),
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    IsometricMetricCell(
                                        title = "Peak Force",
                                        value = displayedPeakForce?.roundToInt()?.let { "$it N" } ?: "--",
                                        valueColor = accent.accent,
                                        modifier = Modifier.weight(1f),
                                    )
                                    IsometricMetricCell(
                                        title = "RFD 0-100ms",
                                        value = if (hasDenseWaveform) {
                                            computedMetrics.rfd100Ns?.let(::formatTwoDecimals)?.let { "$it N/s" }
                                        } else {
                                            null
                                        } ?: "--",
                                        valueColor = Color(0xFF8EE86D),
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    IsometricMetricCell(
                                        title = "Time To Peak",
                                        value = if (hasDenseWaveform) {
                                            computedMetrics.timeToPeakMillis?.let(::formatSecondsClock)
                                        } else {
                                            null
                                        } ?: "--",
                                        valueColor = Color(0xFF67A9FF),
                                        modifier = Modifier.weight(1f),
                                    )
                                    IsometricMetricCell(
                                        title = "Impulse 0-100ms",
                                        value = if (hasDenseWaveform) {
                                            computedMetrics.impulse100Ns?.let(::formatTwoDecimals)?.let { "$it N*s" }
                                        } else {
                                            null
                                        } ?: "--",
                                        valueColor = Color(0xFFFF5C8A),
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                                Text(
                                    buildList {
                                        maxForceLb?.roundToInt()?.let { add("Force limit $it lb") }
                                        maxDurationSeconds?.let { add("Timer limit ${it}s") }
                                        add(helperStatus)
                                    }.joinToString(" • "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(
                                onClick = onExitWorkout,
                                enabled = controlReady,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Exit Mode")
                            }
                            FilledTonalButton(
                                onClick = {
                                    if (isLoaded) onUnload() else onLoad()
                                },
                                enabled = if (isLoaded) {
                                    controlReady
                                } else {
                                    controlReady && (canLoad || canRequestLoad)
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = accent.accentContainer,
                                    contentColor = accent.onAccentContainer,
                                ),
                            ) {
                                Text(if (isLoaded) "Finish" else "Refresh")
                            }
                        }
                        WorkoutTelemetryStrip(
                            sets = sets,
                            reps = reps,
                            phase = phase,
                            status = helperStatus,
                            isLoaded = isLoaded,
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Realtime Data", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            IsometricRealtimeChart(
                                samples = samples,
                                maxForceN = computedMetrics.graphMaxForceN,
                                accentColor = accent.accent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp),
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                IsometricMetricCell(
                                    title = leadingMetricTitle,
                                    value = leadingMetricValue,
                                    valueColor = accent.accent,
                                    modifier = Modifier.weight(1f),
                                )
                                IsometricMetricCell(
                                    title = "Time",
                                    value = displayedElapsedMillis?.let(::formatElapsedClock) ?: "--:--",
                                    valueColor = Color(0xFFFFE45C),
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                IsometricMetricCell(
                                    title = "Peak Force",
                                    value = displayedPeakForce?.roundToInt()?.let { "$it N" } ?: "--",
                                    valueColor = accent.accent,
                                    modifier = Modifier.weight(1f),
                                )
                                IsometricMetricCell(
                                    title = "RFD 0-100ms",
                        value = if (hasDenseWaveform) {
                            computedMetrics.rfd100Ns?.let(::formatTwoDecimals)?.let { "$it N/s" }
                        } else {
                            null
                        } ?: "--",
                                    valueColor = Color(0xFF8EE86D),
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                IsometricMetricCell(
                                    title = "Time To Peak",
                        value = if (hasDenseWaveform) {
                            computedMetrics.timeToPeakMillis?.let(::formatSecondsClock)
                        } else {
                            null
                        } ?: "--",
                                    valueColor = Color(0xFF67A9FF),
                                    modifier = Modifier.weight(1f),
                                )
                                IsometricMetricCell(
                                    title = "Impulse 0-100ms",
                        value = if (hasDenseWaveform) {
                            computedMetrics.impulse100Ns?.let(::formatTwoDecimals)?.let { "$it N*s" }
                        } else {
                            null
                        } ?: "--",
                                    valueColor = Color(0xFFFF5C8A),
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                    Text(
                        buildList {
                            maxForceLb?.roundToInt()?.let { add("Force limit $it lb") }
                            maxDurationSeconds?.let { add("Timer limit ${it}s") }
                            add(helperStatus)
                        }.joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onExitWorkout,
                    enabled = controlReady,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Exit Mode")
                }
                FilledTonalButton(
                    onClick = {
                        if (isLoaded) onUnload() else onLoad()
                    },
                    enabled = if (isLoaded) {
                        controlReady
                    } else {
                        controlReady && (canLoad || canRequestLoad)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = accent.accentContainer,
                        contentColor = accent.onAccentContainer,
                    ),
                ) {
                    Text(if (isLoaded) "Finish" else "Refresh")
                }
            }
            WorkoutTelemetryStrip(
                sets = sets,
                reps = reps,
                phase = phase,
                status = helperStatus,
                isLoaded = isLoaded,
            )
        }
    }
}

@Composable
private fun IsometricRealtimeChart(
    samples: List<IsometricForceSample>,
    maxForceN: Double,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val tickLabels = listOf(maxForceN, maxForceN * 0.75, maxForceN * 0.5, maxForceN * 0.25, 0.0)
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(
            modifier = Modifier
                .width(40.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            tickLabels.forEach { value ->
                Text(
                    "${value.roundToInt()}N",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                )
            }
        }
        Canvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
        ) {
            val strokeWidth = 2.dp.toPx()
            repeat(5) { index ->
                val y = size.height * (index / 4f)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            repeat(5) { index ->
                val x = size.width * (index / 4f)
                drawLine(
                    color = gridColor,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            }

            if (samples.isEmpty()) return@Canvas

            val firstElapsed = samples.first().elapsedMillis
            val normalized = samples.map {
                IsometricForceSample(
                    elapsedMillis = (it.elapsedMillis - firstElapsed).coerceAtLeast(0L),
                    forceN = it.forceN.coerceAtLeast(0.0),
                )
            }
            val durationMillis = normalized.last().elapsedMillis.coerceAtLeast(1000L)
            val path = Path()
            normalized.forEachIndexed { index, sample ->
                val x = (sample.elapsedMillis.toFloat() / durationMillis.toFloat()) * size.width
                val yRatio = (sample.forceN / maxForceN).coerceIn(0.0, 1.0)
                val y = size.height - (yRatio.toFloat() * size.height)
                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }
            drawPath(
                path = path,
                color = accentColor,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
    }
}

@Composable
private fun IsometricMetricCell(
    title: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = valueColor,
        )
    }
}

private fun formatTwoDecimals(value: Double): String = String.format(Locale.US, "%.2f", value)

private fun formatPercentage(value: Double): String {
    val roundedTenth = (value * 10.0).roundToInt() / 10.0
    return if (roundedTenth % 1.0 == 0.0) {
        roundedTenth.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", roundedTenth)
    }
}

private fun formatSecondsClock(valueMillis: Long): String = String.format(Locale.US, "%.2fs", valueMillis / 1000.0)

private fun formatRowPace(valueMillis: Long): String {
    val totalSeconds = ((valueMillis + 500L) / 1000L).coerceAtLeast(0L)
    return String.format(Locale.US, "%d:%02d", totalSeconds / 60L, totalSeconds % 60L)
}

private data class RowDistanceOption(
    val targetMeters: Int?,
    val label: String,
) {
    val splitMeters: Int?
        get() = when (targetMeters) {
            50 -> 10
            100 -> 20
            500 -> 100
            1000 -> 200
            2000 -> 500
            5000 -> 1000
            else -> null
        }

    val splitCount: Int?
        get() = when (targetMeters) {
            2000 -> 4
            50, 100, 500, 1000, 5000 -> 5
            else -> null
        }

    val splitLabel: String?
        get() {
            val meters = splitMeters ?: return null
            val count = splitCount ?: return null
            return "$count x $meters m splits"
        }
}

private const val ISOMETRIC_EXIT_REENTRY_GRACE_MILLIS = 4_000L

@Composable
private fun RowingModeCard(
    status: String,
    distanceMeters: Double?,
    elapsedMillis: Long?,
    pace500Millis: Long?,
    averagePace500Millis: Long?,
    strokeRateSpm: Int?,
    driveForceLb: Double?,
    distanceSamplesMeters: List<Double>,
    forceSamplesLb: List<Double>,
    resistanceLevel: Double,
    simulatedWearLevel: Double,
    strokes: Int?,
    phase: String?,
    isLoaded: Boolean,
    controlReady: Boolean,
    canLoad: Boolean,
    onResistanceLevelChange: (Double) -> Unit,
    onSimulatedWearLevelChange: (Double) -> Unit,
    onLoad: (Int?) -> Unit,
    onUnload: () -> Unit,
    onExitWorkout: () -> Unit,
) {
    val accent = LocalControlAccent.current
    val rowDistanceOptions = remember {
        listOf(
            RowDistanceOption(null, "Just Row"),
            RowDistanceOption(50, "50 m"),
            RowDistanceOption(100, "100 m"),
            RowDistanceOption(500, "500 m"),
            RowDistanceOption(1000, "1000 m"),
            RowDistanceOption(2000, "2000 m"),
            RowDistanceOption(5000, "5000 m"),
        )
    }
    var selectedTargetMeters by remember { mutableStateOf<Int?>(null) }
    val targetReached = selectedTargetMeters?.let { targetMeters ->
        distanceMeters?.let { distance -> distance >= targetMeters } == true
    } == true
    val selectedTargetLabel = selectedTargetMeters?.let { "$it m" }
    val selectedDistanceOption = rowDistanceOptions.firstOrNull { it.targetMeters == selectedTargetMeters }
    val selectedSplitLabel = selectedDistanceOption?.splitLabel
    val splitProgressText = selectedDistanceOption?.let { option ->
        val splitMeters = option.splitMeters ?: return@let null
        val splitCount = option.splitCount ?: return@let null
        val distance = (distanceMeters ?: 0.0).coerceAtLeast(0.0)
        val completedSplits = (distance / splitMeters).toInt().coerceIn(0, splitCount)
        val currentSplit = (completedSplits + 1).coerceAtMost(splitCount)
        val distanceIntoSplit = if (targetReached) {
            splitMeters.toDouble()
        } else {
            (distance % splitMeters).coerceIn(0.0, splitMeters.toDouble())
        }
        "Split $currentSplit/$splitCount - ${formatWeightValue(distanceIntoSplit)} / $splitMeters m"
    }
    val hasTelemetry = distanceMeters != null ||
        elapsedMillis != null ||
        driveForceLb != null ||
        distanceSamplesMeters.isNotEmpty() ||
        forceSamplesLb.isNotEmpty()
    val hasForceTelemetry = driveForceLb != null || forceSamplesLb.isNotEmpty()
    val helperText = when {
        targetReached -> "Target reached. Finish on the VOLTRA or app before starting another row."
        isLoaded && selectedTargetLabel != null && selectedSplitLabel != null ->
            "$selectedTargetLabel row is live with $selectedSplitLabel."
        isLoaded && selectedTargetLabel != null -> "$selectedTargetLabel row is live from the VOLTRA stream."
        isLoaded -> "Just Row is live. Pull the cable and watch distance, pace, and drive force settle in."
        hasTelemetry && selectedTargetLabel != null -> "Last row is shown below against the $selectedTargetLabel target."
        hasTelemetry -> "Last row is shown below. Start Row again before another row."
        selectedTargetLabel != null && selectedSplitLabel != null ->
            "Start $selectedTargetLabel with $selectedSplitLabel."
        selectedTargetLabel != null -> "Start $selectedTargetLabel from Android."
        canLoad -> "Start Just Row from Android. If the VOLTRA is already rowing, this reconnects the live stream."
        else -> "Start Just Row from Android. If the VOLTRA is already rowing, this reconnects the live stream."
    }
    val primaryActionLabel = when {
        isLoaded -> "Finish"
        hasTelemetry -> "Start Row"
        else -> "Start Row"
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Rowing", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        status,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                AssistChip(
                    onClick = { if (isLoaded) onUnload() else onLoad(selectedTargetMeters) },
                    enabled = controlReady,
                    label = { Text(if (isLoaded) "Finish" else "Start Row") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = accent.accentContainer,
                        labelColor = accent.onAccentContainer,
                        disabledContainerColor = accent.accentContainer.copy(alpha = 0.45f),
                        disabledLabelColor = accent.onAccentContainer.copy(alpha = 0.7f),
                    ),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ModeSectionTitle("Row Setup")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowDistanceOptions.forEach { option ->
                        val selected = option.targetMeters == selectedTargetMeters
                        AssistChip(
                            onClick = { selectedTargetMeters = option.targetMeters },
                            enabled = controlReady,
                            label = { Text(option.label) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (selected) accent.accentContainer else MaterialTheme.colorScheme.surface,
                                labelColor = if (selected) accent.onAccentContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledContainerColor = if (selected) {
                                    accent.accentContainer.copy(alpha = 0.45f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                disabledLabelColor = if (selected) {
                                    accent.onAccentContainer.copy(alpha = 0.7f)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                },
                            ),
                        )
                    }
                }
                if (selectedSplitLabel != null) {
                    Text(
                        selectedSplitLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            ModeSectionTitle("Row Settings")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RowingLevelControl(
                    label = "Resistance",
                    value = resistanceLevel,
                    onValueChange = onResistanceLevelChange,
                    enabled = controlReady,
                    modifier = Modifier.weight(1f),
                )
                RowingLevelControl(
                    label = "Simulated Wear",
                    value = simulatedWearLevel,
                    onValueChange = onSimulatedWearLevelChange,
                    enabled = controlReady,
                    modifier = Modifier.weight(1f),
                )
            }

            ModeSectionTitle("Live Data")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RepStat(
                    "Distance",
                    when {
                        distanceMeters != null && selectedTargetMeters != null ->
                            "${formatWeightValue(distanceMeters)} / $selectedTargetMeters m"
                        distanceMeters != null -> "${formatWeightValue(distanceMeters)} m"
                        selectedTargetMeters != null -> "0 / $selectedTargetMeters m"
                        else -> "--"
                    },
                    Modifier.weight(1f),
                )
                RepStat(
                    "Time",
                    elapsedMillis?.let(::formatElapsedClock) ?: "--:--",
                    Modifier.weight(1f),
                )
                RepStat(
                    "Strokes",
                    strokes?.toString() ?: "--",
                    Modifier.weight(1f),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RepStat(
                    "Pace",
                    pace500Millis?.let { "${formatRowPace(it)} /500m" } ?: "--",
                    Modifier.weight(1f),
                )
                RepStat(
                    "Avg Pace",
                    averagePace500Millis?.let { "${formatRowPace(it)} /500m" } ?: "--",
                    Modifier.weight(1f),
                )
                RepStat(
                    "SPM",
                    strokeRateSpm?.toString() ?: "--",
                    Modifier.weight(1f),
                )
            }

            if (splitProgressText != null) {
                Text(
                    splitProgressText,
                    style = MaterialTheme.typography.labelMedium,
                    color = accent.accent,
                    fontWeight = FontWeight.Bold,
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Column {
                            Text(
                                if (hasForceTelemetry) "Drive Force" else "Distance Trend",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                if (hasForceTelemetry) {
                                    driveForceLb?.let { "${formatWeightValue(it)} lb" } ?: "--"
                                } else {
                                    distanceMeters?.let { "${formatWeightValue(it)} m" } ?: "--"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = accent.accent,
                            )
                        }
                        Text(
                            phase ?: if (isLoaded) "Loaded" else "Ready",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    RowingMetricChart(
                        samples = if (hasForceTelemetry) forceSamplesLb else distanceSamplesMeters,
                        unitLabel = if (hasForceTelemetry) "lb" else "m",
                        minMaxValue = if (hasForceTelemetry) 20.0 else 10.0,
                        accentColor = accent.accent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                    )
                }
            }

            Text(
                helperText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onExitWorkout,
                    enabled = controlReady,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Exit Mode")
                }
                FilledTonalButton(
                    onClick = { if (isLoaded) onUnload() else onLoad(selectedTargetMeters) },
                    enabled = controlReady,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = accent.accentContainer,
                        contentColor = accent.onAccentContainer,
                    ),
                ) {
                    Text(primaryActionLabel)
                }
            }
        }
    }
}

@Composable
private fun RowingLevelControl(
    label: String,
    value: Double,
    onValueChange: (Double) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val accent = LocalControlAccent.current
    var pendingValue by remember(value) {
        mutableDoubleStateOf(value.roundToInt().coerceIn(1, 10).toDouble())
    }
    val displayedValue = pendingValue.roundToInt().coerceIn(1, 10)
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                displayedValue.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = accent.accent,
            )
        }
        Slider(
            value = pendingValue.toFloat().coerceIn(1f, 10f),
            onValueChange = {
                pendingValue = it.roundToInt().coerceIn(1, 10).toDouble()
            },
            onValueChangeFinished = {
                onValueChange(pendingValue.roundToInt().coerceIn(1, 10).toDouble())
            },
            valueRange = 1f..10f,
            steps = 8,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = accent.accent,
                activeTrackColor = accent.accent,
                inactiveTrackColor = accent.accentContainer.copy(alpha = 0.55f),
            ),
        )
    }
}

@Composable
private fun RowingMetricChart(
    samples: List<Double>,
    unitLabel: String,
    minMaxValue: Double,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val maxValue = maxOf(minMaxValue, samples.maxOrNull()?.let { kotlin.math.ceil(it / 10.0) * 10.0 } ?: minMaxValue)
    val tickLabels = listOf(maxValue, maxValue * 0.75, maxValue * 0.5, maxValue * 0.25, 0.0)
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(
            modifier = Modifier
                .width(42.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            tickLabels.forEach { value ->
                Text(
                    "${value.roundToInt()}$unitLabel",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                )
            }
        }
        Canvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
        ) {
            repeat(5) { index ->
                val y = size.height * (index / 4f)
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                val x = size.width * (index / 4f)
                drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.dp.toPx())
            }
            if (samples.isEmpty()) return@Canvas
            val visible = samples.takeLast(240)
            val path = Path()
            visible.forEachIndexed { index, value ->
                val x = if (visible.lastIndex <= 0) 0f else (index.toFloat() / visible.lastIndex.toFloat()) * size.width
                val yRatio = (value / maxValue).coerceIn(0.0, 1.0)
                val y = size.height - (yRatio.toFloat() * size.height)
                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }
            drawPath(path, color = accentColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}

@Composable
private fun CustomCurveCard(
    status: String,
    presets: List<CustomCurvePreset>,
    currentForceLb: Double?,
    reps: Int?,
    phase: String?,
    isLoaded: Boolean,
    controlReady: Boolean,
    canLoad: Boolean,
    onApplyCurve: (List<Float>, Int, Int, Int) -> Unit,
    onSavePreset: (String, List<Float>, Int, Int, Int) -> Unit,
    onDeletePreset: (String) -> Unit,
    onLoad: () -> Unit,
    onUnload: () -> Unit,
    onExitWorkout: () -> Unit,
) {
    val accent = LocalControlAccent.current
    var showSavePresetDialog by remember { mutableStateOf(false) }
    var presetNameInput by remember { mutableStateOf("") }
    val curvePoints = remember {
        mutableStateListOf<Float>().apply {
            addAll(VoltraControlFrames.DEFAULT_CUSTOM_CURVE_POINTS)
        }
    }
    var showPointSliders by remember { mutableStateOf(false) }
    var resistanceMinLb by remember { mutableStateOf(VoltraControlFrames.DEFAULT_CUSTOM_CURVE_RESISTANCE_MIN_LB) }
    var resistanceLimitLb by remember { mutableStateOf(VoltraControlFrames.DEFAULT_CUSTOM_CURVE_RESISTANCE_LIMIT_LB) }
    var rangeOfMotionIn by remember { mutableStateOf(VoltraControlFrames.DEFAULT_CUSTOM_CURVE_RANGE_OF_MOTION_IN) }
    if (showSavePresetDialog) {
        AlertDialog(
            onDismissRequest = { showSavePresetDialog = false },
            title = { Text("Save Curve") },
            text = {
                OutlinedTextField(
                    value = presetNameInput,
                    onValueChange = { presetNameInput = it },
                    label = { Text("Preset Name") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSavePreset(presetNameInput, curvePoints.toList(), resistanceMinLb, resistanceLimitLb, rangeOfMotionIn)
                        presetNameInput = ""
                        showSavePresetDialog = false
                    },
                    enabled = presetNameInput.isNotBlank(),
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSavePresetDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Custom Curve", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        status,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                AssistChip(
                    onClick = { if (isLoaded) onUnload() else onLoad() },
                    enabled = controlReady && (isLoaded || canLoad),
                    label = { Text(if (isLoaded) "Weight Off" else "Load") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = accent.accentContainer,
                        labelColor = accent.onAccentContainer,
                        disabledContainerColor = accent.accentContainer.copy(alpha = 0.45f),
                        disabledLabelColor = accent.onAccentContainer.copy(alpha = 0.7f),
                    ),
                )
            }

            ModeSectionTitle("Live Data")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RepStat(
                    "Force",
                    currentForceLb?.let { "${formatWeightValue(it)} lb" } ?: "--",
                    Modifier.weight(1f),
                )
                RepStat("Reps", reps?.toString() ?: "--", Modifier.weight(1f))
                RepStat("Phase", phase ?: if (isLoaded) "Loaded" else "Ready", Modifier.weight(1f))
            }

            ModeSectionTitle("Curve")
            CustomCurveGraph(
                points = curvePoints,
                enabled = controlReady,
                onPointChange = { index, value -> curvePoints[index] = value },
                modifier = Modifier.fillMaxWidth().height(150.dp),
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ModeSectionTitle("Limits")
                CustomCurveRangeSlider(
                    label = "Resistance Range",
                    minValue = resistanceMinLb,
                    maxValue = resistanceLimitLb,
                    suffix = "lb",
                    valueRange = VoltraControlFrames.MIN_CUSTOM_CURVE_RESISTANCE_LIMIT_LB..VoltraControlFrames.MAX_CUSTOM_CURVE_RESISTANCE_LIMIT_LB,
                    minimumSpan = VoltraControlFrames.MIN_CUSTOM_CURVE_RESISTANCE_SPAN_LB,
                    enabled = controlReady,
                    accent = accent,
                    onValueChange = { min, max ->
                        resistanceMinLb = min
                        resistanceLimitLb = max
                    },
                )
                CustomCurveIntSlider(
                    label = "Range of Motion",
                    value = rangeOfMotionIn,
                    suffix = "in",
                    valueRange = VoltraControlFrames.MIN_CUSTOM_CURVE_RANGE_OF_MOTION_IN..VoltraControlFrames.MAX_CUSTOM_CURVE_RANGE_OF_MOTION_IN,
                    enabled = controlReady,
                    accent = accent,
                    onValueChange = { rangeOfMotionIn = it },
                )
            }

            HorizontalDivider()

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ModeSectionTitle("Curve Points")
                    TextButton(onClick = { showPointSliders = !showPointSliders }) {
                        Text(if (showPointSliders) "Hide Sliders" else "Fine Tune")
                    }
                }
                Text(
                    curvePoints.joinToString(separator = "  ") { "${(it * 100).roundToInt()}%" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (showPointSliders) {
                    curvePoints.forEachIndexed { index, value ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("Point ${index + 1}", style = MaterialTheme.typography.labelMedium)
                                Text("${(value * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium)
                            }
                            Slider(
                                value = value,
                                onValueChange = { curvePoints[index] = it.coerceIn(0f, 1f) },
                                valueRange = 0f..1f,
                                enabled = controlReady,
                                colors = SliderDefaults.colors(
                                    thumbColor = accent.accent,
                                    activeTrackColor = accent.accent,
                                ),
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FilledTonalButton(
                    onClick = { onApplyCurve(curvePoints.toList(), resistanceMinLb, resistanceLimitLb, rangeOfMotionIn) },
                    enabled = controlReady,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = accent.accentContainer,
                        contentColor = accent.onAccentContainer,
                        disabledContainerColor = accent.accentContainer.copy(alpha = 0.5f),
                        disabledContentColor = accent.onAccentContainer.copy(alpha = 0.75f),
                    ),
                ) {
                    Text("Apply Curve")
                }
                OutlinedButton(
                    onClick = {
                        presetNameInput = ""
                        showSavePresetDialog = true
                    },
                    modifier = Modifier.weight(1f),
                    enabled = controlReady,
                ) {
                    Text("Save")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onExitWorkout,
                    enabled = controlReady,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Exit Mode")
                }
                OutlinedButton(
                    onClick = {
                        curvePoints.clear()
                        curvePoints.addAll(VoltraControlFrames.DEFAULT_CUSTOM_CURVE_POINTS)
                        resistanceMinLb = VoltraControlFrames.DEFAULT_CUSTOM_CURVE_RESISTANCE_MIN_LB
                        resistanceLimitLb = VoltraControlFrames.DEFAULT_CUSTOM_CURVE_RESISTANCE_LIMIT_LB
                        rangeOfMotionIn = VoltraControlFrames.DEFAULT_CUSTOM_CURVE_RANGE_OF_MOTION_IN
                    },
                    enabled = controlReady,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Reset")
                }
            }

            if (presets.isNotEmpty()) {
                HorizontalDivider()
                ModeSectionTitle("Saved Curves")
                presets.forEach { preset ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            preset.name,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        TextButton(
                            onClick = {
                                curvePoints.clear()
                                curvePoints.addAll(preset.points)
                                resistanceMinLb = preset.resistanceMinLb
                                resistanceLimitLb = preset.resistanceLimitLb
                                rangeOfMotionIn = preset.rangeOfMotionIn
                                if (controlReady) {
                                    onApplyCurve(
                                        preset.points,
                                        preset.resistanceMinLb,
                                        preset.resistanceLimitLb,
                                        preset.rangeOfMotionIn,
                                    )
                                }
                            },
                        ) {
                            Text("Load")
                        }
                        TextButton(onClick = { onDeletePreset(preset.id) }) {
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomCurveRangeSlider(
    label: String,
    minValue: Int,
    maxValue: Int,
    suffix: String,
    valueRange: IntRange,
    minimumSpan: Int,
    enabled: Boolean,
    accent: ControlAccentPalette,
    onValueChange: (Int, Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text("$minValue-$maxValue $suffix", style = MaterialTheme.typography.labelLarge)
        }
        RangeSlider(
            value = minValue.toFloat()..maxValue.toFloat(),
            onValueChange = { range ->
                var nextMin = range.start.roundToInt()
                    .coerceIn(valueRange.first, valueRange.last - minimumSpan)
                var nextMax = range.endInclusive.roundToInt()
                    .coerceIn(valueRange.first + minimumSpan, valueRange.last)
                if (nextMax - nextMin < minimumSpan) {
                    if (nextMin != minValue) {
                        nextMax = (nextMin + minimumSpan).coerceAtMost(valueRange.last)
                    } else {
                        nextMin = (nextMax - minimumSpan).coerceAtLeast(valueRange.first)
                    }
                }
                onValueChange(nextMin, nextMax)
            },
            valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = accent.accent,
                activeTrackColor = accent.accent,
            ),
        )
    }
}

@Composable
private fun CustomCurveIntSlider(
    label: String,
    value: Int,
    suffix: String,
    valueRange: IntRange,
    enabled: Boolean,
    accent: ControlAccentPalette,
    onValueChange: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text("$value $suffix", style = MaterialTheme.typography.labelLarge)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = {
                onValueChange(it.roundToInt().coerceIn(valueRange.first, valueRange.last))
            },
            valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = accent.accent,
                activeTrackColor = accent.accent,
            ),
        )
    }
}

@Composable
private fun CustomCurveGraph(
    points: List<Float>,
    enabled: Boolean,
    onPointChange: (Int, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = LocalControlAccent.current
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
    val lineColor = accent.accent
    val handleColor = accent.onAccent
    val plotPadding = 12.dp
    Canvas(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.small)
            .pointerInput(enabled, points.size) {
                if (!enabled || points.isEmpty()) return@pointerInput

                fun updatePoint(position: Offset) {
                    val paddingPx = plotPadding.toPx()
                    val plotWidth = (size.width - paddingPx * 2f).coerceAtLeast(1f)
                    val plotHeight = (size.height - paddingPx * 2f).coerceAtLeast(1f)
                    val index = if (points.lastIndex <= 0) {
                        0
                    } else {
                        (((position.x - paddingPx) / plotWidth) * points.lastIndex)
                            .roundToInt()
                            .coerceIn(0, points.lastIndex)
                    }
                    val value = (1f - ((position.y - paddingPx) / plotHeight))
                        .coerceIn(0f, 1f)
                    onPointChange(index, value)
                }

                detectDragGestures(
                    onDragStart = { offset -> updatePoint(offset) },
                    onDrag = { change, _ ->
                        updatePoint(change.position)
                        change.consume()
                    },
                )
            },
    ) {
        if (points.isEmpty()) return@Canvas
        val paddingPx = plotPadding.toPx()
        val plotLeft = paddingPx
        val plotTop = paddingPx
        val plotWidth = (size.width - paddingPx * 2f).coerceAtLeast(1f)
        val plotHeight = (size.height - paddingPx * 2f).coerceAtLeast(1f)
        repeat(4) { index ->
            val x = plotLeft + plotWidth * index / 3f
            drawLine(gridColor, Offset(x, plotTop), Offset(x, plotTop + plotHeight), strokeWidth = 1f)
            val y = plotTop + plotHeight * index / 3f
            drawLine(gridColor, Offset(plotLeft, y), Offset(plotLeft + plotWidth, y), strokeWidth = 1f)
        }
        val path = Path()
        val pointOffsets = points.mapIndexed { index, value ->
            val x = if (points.lastIndex == 0) {
                plotLeft
            } else {
                plotLeft + plotWidth * index / points.lastIndex
            }
            val y = plotTop + plotHeight - (plotHeight * value.coerceIn(0f, 1f))
            Offset(x, y)
        }
        pointOffsets.forEachIndexed { index, offset ->
            if (index == 0) {
                path.moveTo(offset.x, offset.y)
            } else {
                path.lineTo(offset.x, offset.y)
            }
        }
        drawPath(path, color = lineColor, style = Stroke(width = 4f, cap = StrokeCap.Round))
        pointOffsets.forEach { offset ->
            drawCircle(color = lineColor, radius = 8.dp.toPx(), center = offset)
            drawCircle(color = handleColor, radius = 3.dp.toPx(), center = offset)
        }
    }
}

@Composable
private fun WeightTrainingSettingsHost(
    reading: VoltraReading,
    unit: WeightUnit,
    baseWeightLb: Double?,
    controlReady: Boolean,
    developerModeEnabled: Boolean,
    experience: ResistanceExperienceOption,
    assistEnabled: Boolean,
    onDismiss: () -> Unit,
    onRefreshModeFeatureStatus: () -> Unit,
    onSetExperience: (ResistanceExperienceOption) -> Unit,
    onSetAssist: (Boolean) -> Unit,
    onSetChainsWeight: (Double) -> Unit,
    onSetEccentricWeight: (Double) -> Unit,
    onSetInverseChains: (Boolean) -> Unit,
) {
    var editingFeature by remember { mutableStateOf<StrengthFeatureDialogConfig?>(null) }
    var editingEccentric by remember { mutableStateOf(false) }
    var selectedSection by remember { mutableStateOf(StrengthSettingsSection.GENERAL) }
    var chainsSyncPending by remember { mutableStateOf(false) }
    var inverseChainsSyncPending by remember { mutableStateOf(false) }
    var inverseChainsBorrowingSharedLoad by remember { mutableStateOf(false) }
    var lastChainsAmount by remember { mutableDoubleStateOf(5.0) }
    var lastInverseChainsAmount by remember { mutableDoubleStateOf(5.0) }
    val base = baseWeightLb
        ?.coerceIn(VoltraControlFrames.MIN_TARGET_LB.toDouble(), VoltraControlFrames.MAX_TARGET_LB.toDouble())
    val chainsMax = base?.let {
        minOf(it, VoltraControlFrames.MAX_TARGET_LB.toDouble() - it)
            .coerceIn(VoltraControlFrames.MIN_EXTRA_WEIGHT_LB.toDouble(), VoltraControlFrames.MAX_EXTRA_WEIGHT_LB.toDouble())
    } ?: 0.0
    val eccentricMin = -(base ?: 0.0)
    val eccentricMax = base ?: 0.0
    val settingEnabled = controlReady && unit == WeightUnit.LB && base != null
    val inverseSettingEnabled = controlReady
    val disabledReason = when {
        unit != WeightUnit.LB -> "Switch to lb before editing these load profiles."
        base == null -> "Set a base weight first."
        !controlReady -> "Connect to enable these controls."
        else -> "Feature edits follow the active base load."
    }
    val inverseChains = reading.inverseChains
    val chainsCurrent = reading.chainsWeightLb?.coerceIn(0.0, chainsMax) ?: 0.0
    val eccentricCurrent = (reading.eccentricWeightLb ?: 0.0).coerceIn(eccentricMin, eccentricMax)
    val chainsActive = chainsCurrent.roundToInt() > 0
    val inverseChainsActive = inverseChains == true
    val inverseChainsAmount = when {
        inverseChainsActive && chainsCurrent > 0.0 -> chainsCurrent
        else -> lastInverseChainsAmount.coerceIn(0.0, chainsMax)
    }
    val eccentricActive = kotlin.math.abs(eccentricCurrent) >= 0.5

    LaunchedEffect(controlReady) {
        if (controlReady) {
            onRefreshModeFeatureStatus()
        } else {
            chainsSyncPending = false
            inverseChainsSyncPending = false
            inverseChainsBorrowingSharedLoad = false
        }
    }
    LaunchedEffect(chainsCurrent, chainsMax) {
        if (chainsCurrent > 0.0) {
            lastChainsAmount = chainsCurrent.coerceIn(0.0, chainsMax)
        }
        chainsSyncPending = false
    }
    LaunchedEffect(chainsCurrent, chainsMax, inverseChainsActive) {
        if (inverseChainsActive && chainsCurrent > 0.0) {
            lastInverseChainsAmount = chainsCurrent.coerceIn(0.0, chainsMax)
        }
    }
    LaunchedEffect(reading.inverseChains) {
        if (reading.inverseChains != null) {
            inverseChainsSyncPending = false
            if (reading.inverseChains == false && chainsCurrent <= 0.0) {
                inverseChainsBorrowingSharedLoad = false
            }
        }
    }
    LaunchedEffect(chainsSyncPending) {
        if (chainsSyncPending) {
            delay(1200)
            chainsSyncPending = false
        }
    }
    LaunchedEffect(inverseChainsSyncPending) {
        if (inverseChainsSyncPending) {
            delay(1200)
            inverseChainsSyncPending = false
        }
    }

    fun requestChainsWeight(amount: Double) {
        val clamped = amount.coerceIn(0.0, chainsMax)
        if (clamped > 0.0) {
            lastChainsAmount = clamped
        }
        chainsSyncPending = true
        onSetChainsWeight(clamped)
    }

    fun applyInverseChains(
        enabled: Boolean,
        amount: Double? = null,
        borrowSharedChains: Boolean = false,
    ) {
        val desiredAmount = amount?.coerceIn(0.0, chainsMax)
        if (enabled) {
            desiredAmount
                ?.takeIf { it > 0.0 }
                ?.let {
                    lastInverseChainsAmount = it
                    if (chainsCurrent.roundToInt() != it.roundToInt()) {
                        requestChainsWeight(it)
                    }
                }
            inverseChainsBorrowingSharedLoad = borrowSharedChains
        } else {
            val shouldClearChains = inverseChainsBorrowingSharedLoad && chainsCurrent > 0.0
            inverseChainsBorrowingSharedLoad = false
            if (shouldClearChains) {
                requestChainsWeight(0.0)
            }
        }
        inverseChainsSyncPending = true
        onSetInverseChains(enabled)
    }

    editingFeature?.let { config ->
        StrengthFeatureDialog(
            config = config,
            onDismiss = { editingFeature = null },
        )
    }
    if (editingEccentric) {
        EccentricFeatureDialog(
            current = eccentricCurrent,
            maxMagnitude = eccentricMax,
            enabled = settingEnabled,
            onApply = onSetEccentricWeight,
            onDismiss = { editingEccentric = false },
        )
    }

    WeightTrainingSettingsSheet(
        selectedSection = selectedSection,
        developerModeEnabled = developerModeEnabled,
        onDismiss = onDismiss,
        onSelectSection = { selectedSection = it },
        content = {
            when (selectedSection) {
                StrengthSettingsSection.GENERAL -> {
                    BinaryChoiceRow(
                        title = "Resistance Experience",
                        detail = "Choose the feel you want during Weight Training.",
                        firstLabel = ResistanceExperienceOption.STANDARD.label,
                        firstSelected = experience == ResistanceExperienceOption.STANDARD,
                        onFirst = { onSetExperience(ResistanceExperienceOption.STANDARD) },
                        secondLabel = ResistanceExperienceOption.INTENSE.label,
                        secondSelected = experience == ResistanceExperienceOption.INTENSE,
                        onSecond = { onSetExperience(ResistanceExperienceOption.INTENSE) },
                    )
                    BinaryChoiceRow(
                        title = "Assist",
                        detail = "Toggle Assist directly on the unit.",
                        firstLabel = "Off",
                        firstSelected = !assistEnabled,
                        onFirst = { onSetAssist(false) },
                        secondLabel = "On",
                        secondSelected = assistEnabled,
                        onSecond = { onSetAssist(true) },
                    )
                }
                StrengthSettingsSection.CHAINS -> {
                    StrengthFeatureButton(
                        title = "Chains",
                        current = "${chainsCurrent.roundToInt()} lb, ${if (chainsActive) "On" else "Off"}",
                        detail = "Range 0-${chainsMax.roundToInt()} lb; limited by base load and 200 lb total.",
                        enabled = settingEnabled && !chainsSyncPending,
                        actionLabel = if (chainsSyncPending) "Syncing" else if (chainsActive) "Off" else "On",
                        onClick = {
                            inverseChainsBorrowingSharedLoad = false
                            if (chainsActive) {
                                requestChainsWeight(0.0)
                            } else {
                                requestChainsWeight(lastChainsAmount.coerceIn(1.0, chainsMax.coerceAtLeast(1.0)))
                            }
                        },
                        secondaryLabel = "Edit",
                        onSecondaryClick = {
                            editingFeature = StrengthFeatureDialogConfig(
                                title = "Chains",
                                current = if (chainsActive) chainsCurrent else lastChainsAmount.coerceIn(0.0, chainsMax),
                                min = 0.0,
                                max = chainsMax,
                                unit = "lb",
                                supportingText = "Chains add resistance as you move through the rep.",
                                enabled = settingEnabled && !chainsSyncPending,
                                onApply = {
                                    inverseChainsBorrowingSharedLoad = false
                                    requestChainsWeight(it)
                                },
                            )
                        },
                    )
                }
                StrengthSettingsSection.INVERSE_CHAINS -> {
                    StrengthFeatureButton(
                        title = "Inverse Chains",
                        current = "${inverseChainsAmount.roundToInt()} lb, ${if (inverseChainsActive) "On" else "Off"}",
                        detail = "Uses the shared chain-load amount while dedicated inverse tuning is being finished.",
                        enabled = inverseSettingEnabled && !inverseChainsSyncPending && !chainsSyncPending,
                        actionLabel = if (inverseChainsSyncPending || chainsSyncPending) "Syncing" else if (inverseChainsActive) "Off" else "On",
                        onClick = {
                            val next = !inverseChainsActive
                            if (next) {
                                val amount = when {
                                    chainsCurrent > 0.0 -> chainsCurrent
                                    chainsMax >= 1.0 -> lastInverseChainsAmount.coerceIn(1.0, chainsMax)
                                    else -> 0.0
                                }
                                applyInverseChains(
                                    enabled = true,
                                    amount = amount,
                                    borrowSharedChains = chainsCurrent <= 0.0,
                                )
                            } else {
                                applyInverseChains(enabled = false)
                            }
                        },
                        secondaryLabel = "Edit",
                        onSecondaryClick = {
                            editingFeature = StrengthFeatureDialogConfig(
                                title = "Inverse Chains",
                                current = inverseChainsAmount,
                                min = 0.0,
                                max = chainsMax,
                                unit = "lb",
                                supportingText = "Inverse Chains currently uses the shared chain-load amount so the effect can start immediately.",
                                enabled = settingEnabled && !inverseChainsSyncPending && !chainsSyncPending,
                                onApply = { nextAmount ->
                                    val clamped = nextAmount.coerceIn(0.0, chainsMax)
                                    if (clamped <= 0.0) {
                                        lastInverseChainsAmount = 0.0
                                        applyInverseChains(enabled = false)
                                    } else {
                                        applyInverseChains(
                                            enabled = true,
                                            amount = clamped,
                                            borrowSharedChains = chainsCurrent <= 0.0,
                                        )
                                    }
                                },
                            )
                        },
                    )
                }
                StrengthSettingsSection.ECCENTRIC -> {
                    StrengthFeatureButton(
                        title = "Eccentric",
                        current = "${eccentricCurrent.roundToInt()} lb, ${if (eccentricActive) "On" else "Off"}",
                        detail = "Magnitude ${eccentricMin.roundToInt()} to +${eccentricMax.roundToInt()} lb.",
                        enabled = settingEnabled,
                        actionLabel = if (eccentricActive) "Off" else "On",
                        onClick = {
                            if (eccentricActive) {
                                onSetEccentricWeight(0.0)
                            } else {
                                editingEccentric = true
                            }
                        },
                        secondaryLabel = "Edit",
                        onSecondaryClick = { editingEccentric = true },
                    )
                }
                StrengthSettingsSection.CUSTOM_CURVE -> {
                    Text(
                        "Custom Curve is under development.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Slider(
                        value = 0.5f,
                        onValueChange = {},
                        enabled = false,
                        valueRange = 0f..1f,
                    )
                }
            }
            if (!settingEnabled && selectedSection in listOf(StrengthSettingsSection.CHAINS, StrengthSettingsSection.ECCENTRIC)) {
                Text(
                    disabledReason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResistanceBandSettingsSheet(
    experience: ResistanceExperienceOption,
    resistanceMode: ResistanceModeOption,
    resistanceCurve: ResistanceCurveOption,
    progressiveLengthMode: ProgressiveLengthOption,
    bandLengthInches: Double,
    observedBandLengthInches: Double?,
    onDismiss: () -> Unit,
    onSetExperience: (ResistanceExperienceOption) -> Unit,
    onSetResistanceMode: (ResistanceModeOption) -> Unit,
    onSetResistanceCurve: (ResistanceCurveOption) -> Unit,
    onSetProgressiveLengthMode: (ProgressiveLengthOption) -> Unit,
    onSetBandLengthInches: (Double) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedSection by remember { mutableStateOf(ResistanceBandSettingsSection.EXPERIENCE) }
    var pendingBandLengthInches by remember(bandLengthInches) { mutableDoubleStateOf(bandLengthInches) }
    var showBandLengthDialog by remember { mutableStateOf(false) }
    var bandLengthInput by remember { mutableStateOf("") }

    LaunchedEffect(bandLengthInches) {
        pendingBandLengthInches = bandLengthInches
    }

    fun setPendingBandLength(next: Double) {
        pendingBandLengthInches = snapWeight(next, 20.0, 102.0, 1.0)
    }

    fun commitBandLength(next: Double) {
        val snapped = snapWeight(next, 20.0, 102.0, 1.0)
        pendingBandLengthInches = snapped
        onSetBandLengthInches(snapped)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Resistance Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ResistanceBandSettingsSection.entries.take(2).forEach { section ->
                    FilterChip(
                        selected = selectedSection == section,
                        onClick = { selectedSection = section },
                        label = { Text(section.label) },
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ResistanceBandSettingsSection.entries.drop(2).forEach { section ->
                    FilterChip(
                        selected = selectedSection == section,
                        onClick = { selectedSection = section },
                        label = { Text(section.label) },
                    )
                }
            }
            when (selectedSection) {
                ResistanceBandSettingsSection.EXPERIENCE -> {
                    BinaryChoiceRow(
                        title = "Resistance Experience",
                        detail = "Choose Standard or Intense.",
                        firstLabel = ResistanceExperienceOption.STANDARD.label,
                        firstSelected = experience == ResistanceExperienceOption.STANDARD,
                        onFirst = { onSetExperience(ResistanceExperienceOption.STANDARD) },
                        secondLabel = ResistanceExperienceOption.INTENSE.label,
                        secondSelected = experience == ResistanceExperienceOption.INTENSE,
                        onSecond = { onSetExperience(ResistanceExperienceOption.INTENSE) },
                    )
                }
                ResistanceBandSettingsSection.MODE -> {
                    BinaryChoiceRow(
                        title = "Resistance Mode",
                        detail = "Switch between Standard and Inverse. The selected state mirrors the unit.",
                        firstLabel = ResistanceModeOption.STANDARD.label,
                        firstSelected = resistanceMode == ResistanceModeOption.STANDARD,
                        onFirst = { onSetResistanceMode(ResistanceModeOption.STANDARD) },
                        secondLabel = ResistanceModeOption.INVERSE.label,
                        secondSelected = resistanceMode == ResistanceModeOption.INVERSE,
                        onSecond = { onSetResistanceMode(ResistanceModeOption.INVERSE) },
                    )
                }
                ResistanceBandSettingsSection.CURVE -> {
                    BinaryChoiceRow(
                        title = "Resistance Curve",
                        detail = "Choose Power Law or Logarithm. The selection mirrors the unit.",
                        firstLabel = ResistanceCurveOption.POWER_LAW.label,
                        firstSelected = resistanceCurve == ResistanceCurveOption.POWER_LAW,
                        onFirst = { onSetResistanceCurve(ResistanceCurveOption.POWER_LAW) },
                        secondLabel = ResistanceCurveOption.LOGARITHM.label,
                        secondSelected = resistanceCurve == ResistanceCurveOption.LOGARITHM,
                        onSecond = { onSetResistanceCurve(ResistanceCurveOption.LOGARITHM) },
                    )
                }
                ResistanceBandSettingsSection.BAND_LENGTH -> {
                    if (showBandLengthDialog) {
                        AlertDialog(
                            onDismissRequest = { showBandLengthDialog = false },
                            title = { Text("Set Band Length") },
                            text = {
                                OutlinedTextField(
                                    value = bandLengthInput,
                                    onValueChange = { bandLengthInput = it },
                                    label = { Text("in") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                )
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        val parsed = bandLengthInput.toDoubleOrNull()
                                        if (parsed != null) {
                                            commitBandLength(parsed)
                                        }
                                        showBandLengthDialog = false
                                    },
                                ) {
                                    Text("Set")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showBandLengthDialog = false }) {
                                    Text("Cancel")
                                }
                            },
                        )
                    }
                    BinaryChoiceRow(
                        title = "Progressive Length",
                        detail = "Switch between Band Length and ROM. Band Length writes are only available while Band Length is selected on the unit.",
                        firstLabel = ProgressiveLengthOption.BAND_LENGTH.label,
                        firstSelected = progressiveLengthMode == ProgressiveLengthOption.BAND_LENGTH,
                        onFirst = { onSetProgressiveLengthMode(ProgressiveLengthOption.BAND_LENGTH) },
                        secondLabel = ProgressiveLengthOption.ROM.label,
                        secondSelected = progressiveLengthMode == ProgressiveLengthOption.ROM,
                        onSecond = { onSetProgressiveLengthMode(ProgressiveLengthOption.ROM) },
                    )
                    if (progressiveLengthMode == ProgressiveLengthOption.BAND_LENGTH) {
                        Text("Band Length", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text(
                            observedBandLengthInches?.let { "Current on unit: ${formatInchesValue(it)}" }
                                ?: "Use the slider or buttons to set the band length.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedButton(
                                onClick = { commitBandLength(pendingBandLengthInches - 5.0) },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("-5")
                            }
                            OutlinedButton(
                                onClick = { commitBandLength(pendingBandLengthInches - 1.0) },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("-1")
                            }
                            Surface(
                                modifier = Modifier
                                    .weight(1.35f)
                                    .clickable {
                                        bandLengthInput = formatWeightValue(pendingBandLengthInches)
                                        showBandLengthDialog = true
                                    },
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        formatInchesValue(pendingBandLengthInches),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = { commitBandLength(pendingBandLengthInches + 1.0) },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("+1")
                            }
                            OutlinedButton(
                                onClick = { commitBandLength(pendingBandLengthInches + 5.0) },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("+5")
                            }
                        }
                        Slider(
                            value = pendingBandLengthInches.toFloat(),
                            onValueChange = { setPendingBandLength(it.toDouble()) },
                            onValueChangeFinished = { commitBandLength(pendingBandLengthInches) },
                            valueRange = 20f..102f,
                            steps = 81,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("20 in", style = MaterialTheme.typography.labelMedium)
                            Text(formatInchesValue(pendingBandLengthInches), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Text("102 in", style = MaterialTheme.typography.labelMedium)
                        }
                        Text(
                            "Tap the value for direct entry, or use the +/- buttons for easier fine adjustment.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            "ROM is active on the unit. Band Length is hidden until you switch Progressive Length back to Band Length.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text("Close")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DamperSettingsSheet(
    experience: ResistanceExperienceOption,
    onDismiss: () -> Unit,
    onSetExperience: (ResistanceExperienceOption) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Damper Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            BinaryChoiceRow(
                title = "Resistance Experience",
                detail = "Choose Standard or Intense.",
                firstLabel = ResistanceExperienceOption.STANDARD.label,
                firstSelected = experience == ResistanceExperienceOption.STANDARD,
                onFirst = { onSetExperience(ResistanceExperienceOption.STANDARD) },
                secondLabel = ResistanceExperienceOption.INTENSE.label,
                secondSelected = experience == ResistanceExperienceOption.INTENSE,
                onSecond = { onSetExperience(ResistanceExperienceOption.INTENSE) },
            )
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text("Close")
            }
        }
    }
}

@Composable
private fun WeightTrainingOptionsCard(
    experience: ResistanceExperienceOption,
    assistEnabled: Boolean,
    onSetExperience: (ResistanceExperienceOption) -> Unit,
    onSetAssist: (Boolean) -> Unit,
) {
    MetricCard {
        Text("Weight Training Options", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(
            "Tune the feel of Weight Training here.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        BinaryChoiceRow(
            title = "Resistance Experience",
            detail = "Switch between Standard and Intense on the connected VOLTRA.",
            firstLabel = ResistanceExperienceOption.STANDARD.label,
            firstSelected = experience == ResistanceExperienceOption.STANDARD,
            onFirst = { onSetExperience(ResistanceExperienceOption.STANDARD) },
            secondLabel = ResistanceExperienceOption.INTENSE.label,
            secondSelected = experience == ResistanceExperienceOption.INTENSE,
            onSecond = { onSetExperience(ResistanceExperienceOption.INTENSE) },
        )
        BinaryChoiceRow(
            title = "Assist",
            detail = "Toggle Assist directly on the unit.",
            firstLabel = "Off",
            firstSelected = !assistEnabled,
            onFirst = { onSetAssist(false) },
            secondLabel = "On",
            secondSelected = assistEnabled,
            onSecond = { onSetAssist(true) },
        )
    }
}

@Composable
private fun ResistanceBandOptionsCard(
    experience: ResistanceExperienceOption,
    resistanceMode: ResistanceModeOption,
    resistanceCurve: ResistanceCurveOption,
    bandLengthInches: Double,
    observedBandLengthInches: Double?,
    onSetExperience: (ResistanceExperienceOption) -> Unit,
    onSetResistanceMode: (ResistanceModeOption) -> Unit,
    onSetResistanceCurve: (ResistanceCurveOption) -> Unit,
    onSetBandLengthInches: (Double) -> Unit,
) {
    MetricCard {
        Text("Resistance Options", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(
            "These Resistance Band controls are still being developed.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        BinaryChoiceRow(
            title = "Experience",
            detail = "Standard or Intense on the device.",
            firstLabel = ResistanceExperienceOption.STANDARD.label,
            firstSelected = experience == ResistanceExperienceOption.STANDARD,
            onFirst = { onSetExperience(ResistanceExperienceOption.STANDARD) },
            secondLabel = ResistanceExperienceOption.INTENSE.label,
            secondSelected = experience == ResistanceExperienceOption.INTENSE,
            onSecond = { onSetExperience(ResistanceExperienceOption.INTENSE) },
        )
        BinaryChoiceRow(
            title = "Resistance Mode",
            detail = "Standard or Inverse.",
            firstLabel = ResistanceModeOption.STANDARD.label,
            firstSelected = resistanceMode == ResistanceModeOption.STANDARD,
            onFirst = { onSetResistanceMode(ResistanceModeOption.STANDARD) },
            secondLabel = ResistanceModeOption.INVERSE.label,
            secondSelected = resistanceMode == ResistanceModeOption.INVERSE,
            onSecond = { onSetResistanceMode(ResistanceModeOption.INVERSE) },
        )
        BinaryChoiceRow(
            title = "Resistance Curve",
            detail = "Power Law or Logarithm.",
            firstLabel = ResistanceCurveOption.POWER_LAW.label,
            firstSelected = resistanceCurve == ResistanceCurveOption.POWER_LAW,
            onFirst = { onSetResistanceCurve(ResistanceCurveOption.POWER_LAW) },
            secondLabel = ResistanceCurveOption.LOGARITHM.label,
            secondSelected = resistanceCurve == ResistanceCurveOption.LOGARITHM,
            onSecond = { onSetResistanceCurve(ResistanceCurveOption.LOGARITHM) },
        )
        Text("Band Length", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        Text(
            observedBandLengthInches?.let { "Current on unit: ${formatInchesValue(it)}" }
                ?: "Open Cable Length on the unit to adjust this.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Slider(
            value = bandLengthInches.toFloat(),
            onValueChange = { onSetBandLengthInches(it.toDouble()) },
            valueRange = 20f..102f,
            steps = 81,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("20 in", style = MaterialTheme.typography.labelMedium)
            Text(formatInchesValue(bandLengthInches), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text("102 in", style = MaterialTheme.typography.labelMedium)
        }
        Text(
            "Band Length controls are still being developed. Use Cable Length on the unit for now.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DamperOptionsCard(
    experience: ResistanceExperienceOption,
    onSetExperience: (ResistanceExperienceOption) -> Unit,
) {
    MetricCard {
        Text("Damper Options", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(
            "Damper now shares the live Resistance Experience toggle with the other workout profiles.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        BinaryChoiceRow(
            title = "Resistance Experience",
            detail = "Standard or Intense.",
            firstLabel = ResistanceExperienceOption.STANDARD.label,
            firstSelected = experience == ResistanceExperienceOption.STANDARD,
            onFirst = { onSetExperience(ResistanceExperienceOption.STANDARD) },
            secondLabel = ResistanceExperienceOption.INTENSE.label,
            secondSelected = experience == ResistanceExperienceOption.INTENSE,
            onSecond = { onSetExperience(ResistanceExperienceOption.INTENSE) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IsokineticSettingsSheet(
    menu: IsokineticMenuOption,
    speedOptions: List<Double>,
    speedIndex: Int,
    maxEccentricLoad: Double,
    constantResistance: Double,
    onDismiss: () -> Unit,
    onSelectMenu: (IsokineticMenuOption) -> Unit,
    onSelectSpeedIndex: (Int) -> Unit,
    onSetMaxEccentricLoad: (Double) -> Unit,
    onSetConstantResistance: (Double) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val speedLabels = remember(speedOptions) { speedOptions.map(::formatSpeedOptionLabel) }
    val constantResistanceValues = remember { stepValues(5, 100, 5) }
    val maxEccentricValues = remember { stepValues(5, 200, 5) }
    val constantResistanceIndex = remember(constantResistance) {
        constantResistanceValues.indexOf(constantResistance.roundToInt()).coerceAtLeast(0)
    }
    val maxEccentricIndex = remember(maxEccentricLoad) {
        maxEccentricValues.indexOf(maxEccentricLoad.roundToInt()).coerceAtLeast(0)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Eccentric Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = menu == IsokineticMenuOption.ISOKINETIC,
                    onClick = { onSelectMenu(IsokineticMenuOption.ISOKINETIC) },
                    label = { Text(IsokineticMenuOption.ISOKINETIC.label) },
                )
                FilterChip(
                    selected = menu == IsokineticMenuOption.CONSTANT_RESISTANCE,
                    onClick = { onSelectMenu(IsokineticMenuOption.CONSTANT_RESISTANCE) },
                    label = { Text(IsokineticMenuOption.CONSTANT_RESISTANCE.label) },
                )
            }
            if (menu == IsokineticMenuOption.ISOKINETIC) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    NumberWheelPicker(
                        title = "Isokinetic",
                        items = speedLabels,
                        selectedIndex = speedIndex,
                        footer = if (speedOptions[speedIndex] <= 0.0) "Auto speed limit" else "${formatSpeedValue(speedOptions[speedIndex])} limit",
                        modifier = Modifier.weight(1f),
                        onSelectedIndexChange = onSelectSpeedIndex,
                    )
                    NumberWheelPicker(
                        title = "Max Eccentric Load",
                        items = maxEccentricValues.map { "$it lb" },
                        selectedIndex = maxEccentricIndex,
                        footer = "${formatWeightValue(maxEccentricLoad)} lb",
                        modifier = Modifier.weight(1f),
                        onSelectedIndexChange = { index -> onSetMaxEccentricLoad(maxEccentricValues[index].toDouble()) },
                    )
                }
            } else {
                NumberWheelPicker(
                    title = "Constant Resistance",
                    items = constantResistanceValues.map { "$it lb" },
                    selectedIndex = constantResistanceIndex,
                    footer = "${formatWeightValue(constantResistance)} lb",
                    modifier = Modifier.fillMaxWidth(),
                    onSelectedIndexChange = { index -> onSetConstantResistance(constantResistanceValues[index].toDouble()) },
                )
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text("Close")
            }
        }
    }
}

@Composable
private fun NumberWheelPicker(
    title: String,
    items: List<String>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    footer: String? = null,
    onSelectedIndexChange: (Int) -> Unit,
) {
    ElevatedCard(
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            AndroidView(
                factory = { context ->
                    NumberPicker(context).apply {
                        descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
                        wrapSelectorWheel = false
                        minValue = 0
                        maxValue = (items.lastIndex).coerceAtLeast(0)
                        displayedValues = items.toTypedArray()
                        value = selectedIndex.coerceIn(0, items.lastIndex.coerceAtLeast(0))
                        setOnValueChangedListener { _, _, newVal ->
                            onSelectedIndexChange(newVal)
                        }
                    }
                },
                update = { picker ->
                    val safeIndex = selectedIndex.coerceIn(0, items.lastIndex.coerceAtLeast(0))
                    picker.displayedValues = null
                    picker.minValue = 0
                    picker.maxValue = (items.lastIndex).coerceAtLeast(0)
                    picker.displayedValues = items.toTypedArray()
                    if (picker.value != safeIndex) {
                        picker.value = safeIndex
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
            )
            footer?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun BinaryChoiceRow(
    title: String,
    detail: String,
    firstLabel: String,
    firstSelected: Boolean,
    onFirst: () -> Unit,
    secondLabel: String,
    secondSelected: Boolean,
    onSecond: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        Text(
            detail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = firstSelected,
                onClick = onFirst,
                label = { Text(firstLabel) },
            )
            FilterChip(
                selected = secondSelected,
                onClick = onSecond,
                label = { Text(secondLabel) },
            )
        }
    }
}

@Composable
private fun PowerWorkoutHistoryList(history: List<PowerWorkoutHistoryEntry>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ModeSectionTitle("Session History")
            if (history.isEmpty()) {
                Text(
                    "Completed reps will appear here for this workout.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    HistoryHeader("Rep", Modifier.weight(0.75f))
                    HistoryHeader("Power", Modifier.weight(1f))
                    HistoryHeader("Peak", Modifier.weight(1f))
                    HistoryHeader("TTP", Modifier.weight(1f))
                }
                history.takeLast(12).forEach { entry ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val repLabel = entry.repCount
                            ?.takeIf { it > 0 }
                            ?.toString()
                            ?: entry.index.toString()
                        Text(repLabel, modifier = Modifier.weight(0.75f), style = MaterialTheme.typography.bodySmall)
                        Text(
                            entry.peakPowerWatts?.let { "$it W" } ?: "--",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            entry.peakForceLb?.let { "${formatWeightValue(it)} lb" } ?: "--",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            entry.timeToPeakMillis?.let(::formatSecondsClock) ?: "--",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeSectionTitle(title: String, detail: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        detail?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HistoryHeader(label: String, modifier: Modifier = Modifier) {
    Text(
        label,
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun WorkoutTelemetryStrip(
    sets: Int?,
    reps: Int?,
    phase: String?,
    status: String,
    isLoaded: Boolean,
    summaryText: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RepStat("Sets", sets?.toString() ?: "--", Modifier.weight(1f))
        RepStat("Reps", reps?.toString() ?: "--", Modifier.weight(1f))
        RepStat("Phase", phase ?: if (isLoaded) "Loaded" else "Ready", Modifier.weight(1f))
    }
    Text(
        summaryText ?: if (isLoaded) "Live from the unit." else status,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun WorkoutCounterCard(
    modeTitle: String,
    sets: Int?,
    reps: Int?,
    phase: String?,
    isLoaded: Boolean,
    status: String,
) {
    val accent = LocalControlAccent.current
    OutlinedCard(
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, accent.accent.copy(alpha = 0.32f)),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("$modeTitle Telemetry", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    if (isLoaded) "Live from the VOLTRA while loaded." else status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AssistChip(
                onClick = {},
                enabled = false,
                label = { Text(if (isLoaded) "Loaded" else "Ready") },
                colors = AssistChipDefaults.assistChipColors(
                    disabledContainerColor = accent.accentContainer,
                    disabledLabelColor = accent.onAccentContainer,
                ),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RepStat("Sets", sets?.toString() ?: "--", Modifier.weight(1f))
            RepStat("Reps", reps?.toString() ?: "--", Modifier.weight(1f))
            RepStat("Phase", phase ?: "--", Modifier.weight(1f))
        }
        }
    }
}

@Composable
private fun DigitalWeightDial(
    label: String,
    unit: WeightUnit,
    value: Double,
    minLoad: Double,
    maxLoad: Double,
    step: Double,
    instantApply: Boolean,
    onTargetChange: (Double) -> Unit,
    onTargetSettled: (Double) -> Unit,
    onCycleStep: () -> Unit,
    onOpenDial: () -> Unit,
    denseLayout: Boolean = false,
) {
    val accent = LocalControlAccent.current
    var dragTarget by remember { mutableDoubleStateOf(value) }

    LaunchedEffect(value, minLoad, maxLoad, step) {
        dragTarget = snapWeight(value, minLoad, maxLoad, step)
    }

    fun updateTarget(target: Double, settle: Boolean) {
        val next = snapWeight(target, minLoad, maxLoad, step)
        dragTarget = next
        onTargetChange(next)
        if (settle && instantApply) {
            onTargetSettled(next)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val compactLayout = maxWidth < 360.dp
        val reducedLayout = denseLayout && !compactLayout
        Surface(
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(
                    when {
                        compactLayout -> 12.dp
                        reducedLayout -> 12.dp
                        else -> 14.dp
                    },
                ),
                verticalArrangement = Arrangement.spacedBy(if (reducedLayout) 8.dp else 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        when {
                            compactLayout -> 8.dp
                            reducedLayout -> 8.dp
                            else -> 10.dp
                        },
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WeightStepButton(
                        label = "-${formatWeightValue(step)}",
                        onClick = {
                            updateTarget(dragTarget - step, settle = true)
                        },
                        onLongClick = onCycleStep,
                        modifier = Modifier.weight(if (compactLayout || reducedLayout) 0.95f else 0.8f),
                        compact = reducedLayout,
                    )
                    Column(
                        modifier = Modifier
                            .weight(if (compactLayout || reducedLayout) 1.1f else 1.4f)
                            .clip(MaterialTheme.shapes.small)
                            .clickable(onClick = onOpenDial)
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            label,
                            style = when {
                                compactLayout -> MaterialTheme.typography.headlineLarge
                                reducedLayout -> MaterialTheme.typography.headlineMedium
                                else -> MaterialTheme.typography.displaySmall
                            },
                            fontWeight = FontWeight.Bold,
                            color = accent.accent,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                        )
                        Text(
                            "Tap to dial",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    WeightStepButton(
                        label = "+${formatWeightValue(step)}",
                        onClick = {
                            updateTarget(dragTarget + step, settle = true)
                        },
                        onLongClick = onCycleStep,
                        modifier = Modifier.weight(if (compactLayout || reducedLayout) 0.95f else 0.8f),
                        compact = reducedLayout,
                    )
                }
                Slider(
                    value = dragTarget.toFloat(),
                    onValueChange = {
                        updateTarget(it.toDouble(), settle = false)
                    },
                    onValueChangeFinished = {
                        updateTarget(dragTarget, settle = true)
                    },
                    valueRange = minLoad.toFloat()..maxLoad.toFloat(),
                    steps = sliderSteps(minLoad, maxLoad, step),
                    colors = SliderDefaults.colors(
                        thumbColor = accent.accent,
                        activeTrackColor = accent.accent,
                    ),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("${formatWeightValue(minLoad)} ${unit.label}", style = MaterialTheme.typography.labelMedium)
                    Text("${formatWeightValue(maxLoad)} ${unit.label}", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WeightStepButton(
    label: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val accent = LocalControlAccent.current
    Surface(
        color = MaterialTheme.colorScheme.surface,
        contentColor = accent.accent,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
            .height(if (compact) 52.dp else 56.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(label, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SetModeButton(
    instantApply: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    enabled: Boolean,
) {
    val accent = LocalControlAccent.current
    val containerColor = when {
        !enabled -> MaterialTheme.colorScheme.surface
        instantApply -> Color(0xFF2F7DFF)
        else -> accent.accent
    }
    val contentColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant
        instantApply -> Color.White
        else -> accent.onAccent
    }
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .height(40.dp)
            .combinedClickable(
                enabled = enabled,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (instantApply) "Instant" else "Set", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RepStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val accent = LocalControlAccent.current
    Surface(
        modifier = modifier,
        color = accent.accentContainer.copy(alpha = 0.7f),
        contentColor = accent.onAccentContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = accent.onAccentContainer.copy(alpha = 0.82f))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun WeightTrainingFeaturesCard(
    reading: VoltraReading,
    unit: WeightUnit,
    baseWeightLb: Double?,
    controlReady: Boolean,
    isLoaded: Boolean,
    developerModeEnabled: Boolean,
    onSetChainsWeight: (Double) -> Unit,
    onSetEccentricWeight: (Double) -> Unit,
    onSetInverseChains: (Boolean) -> Unit,
) {
    var editingFeature by remember { mutableStateOf<StrengthFeatureDialogConfig?>(null) }
    var editingEccentric by remember { mutableStateOf(false) }
    var showStrengthSettings by remember { mutableStateOf(false) }
    var selectedStrengthSection by remember { mutableStateOf(StrengthSettingsSection.CHAINS) }
    var optimisticInverseChains by remember { mutableStateOf<Boolean?>(null) }
    var lastChainsAmount by remember { mutableDoubleStateOf(5.0) }
    val base = baseWeightLb
        ?.coerceIn(VoltraControlFrames.MIN_TARGET_LB.toDouble(), VoltraControlFrames.MAX_TARGET_LB.toDouble())
    val chainsMax = base?.let {
        minOf(it, VoltraControlFrames.MAX_TARGET_LB.toDouble() - it)
            .coerceIn(VoltraControlFrames.MIN_EXTRA_WEIGHT_LB.toDouble(), VoltraControlFrames.MAX_EXTRA_WEIGHT_LB.toDouble())
    } ?: 0.0
    val eccentricMin = -(base ?: 0.0)
    val eccentricMax = base ?: 0.0
    val settingEnabled = controlReady && unit == WeightUnit.LB && base != null
    val inverseSettingEnabled = controlReady
    val disabledReason = when {
        unit != WeightUnit.LB -> "Switch to lb before editing these load profiles."
        base == null -> "Set a base weight first."
        !controlReady -> "Connect to enable these controls."
        else -> "Uses the current base weight to keep total load in range. Feature edits can be sent loaded or unloaded."
    }
    val inverseChains = reading.inverseChains ?: optimisticInverseChains
    val chainsCurrent = reading.chainsWeightLb?.coerceIn(0.0, chainsMax) ?: 0.0
    val eccentricCurrent = (reading.eccentricWeightLb ?: 0.0)
        .coerceIn(eccentricMin, eccentricMax)
    val chainsActive = chainsCurrent.roundToInt() > 0
    val inverseChainsActive = inverseChains == true
    val eccentricActive = kotlin.math.abs(eccentricCurrent) >= 0.5

    LaunchedEffect(reading.inverseChains) {
        if (reading.inverseChains != null) {
            optimisticInverseChains = reading.inverseChains
        }
    }
    LaunchedEffect(chainsCurrent, chainsMax) {
        if (chainsCurrent > 0.0) {
            lastChainsAmount = chainsCurrent.coerceIn(0.0, chainsMax)
        }
    }
    LaunchedEffect(controlReady) {
        if (!controlReady) {
            optimisticInverseChains = null
        }
    }

    editingFeature?.let { config ->
        StrengthFeatureDialog(
            config = config,
            onDismiss = { editingFeature = null },
        )
    }
    if (editingEccentric) {
        EccentricFeatureDialog(
            current = eccentricCurrent,
            maxMagnitude = eccentricMax,
            enabled = settingEnabled,
            onApply = onSetEccentricWeight,
            onDismiss = { editingEccentric = false },
        )
    }
    if (showStrengthSettings) {
        WeightTrainingSettingsSheet(
            selectedSection = selectedStrengthSection,
            developerModeEnabled = developerModeEnabled,
            onDismiss = { showStrengthSettings = false },
            onSelectSection = { selectedStrengthSection = it },
            content = {
                when (selectedStrengthSection) {
                    StrengthSettingsSection.GENERAL -> {
                        Text(
                            "General workout settings now live in the main Weight Training settings menu.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    StrengthSettingsSection.CHAINS -> {
                        StrengthFeatureButton(
                            title = "Chains",
                            current = "${chainsCurrent.roundToInt()} lb, ${if (chainsActive) "On" else "Off"}",
                            detail = "Range 0-${chainsMax.roundToInt()} lb; limited by base load and 200 lb total.",
                            enabled = settingEnabled,
                            actionLabel = if (chainsActive) "Off" else "On",
                            onClick = {
                                if (chainsActive) {
                                    onSetChainsWeight(0.0)
                                } else {
                                    onSetChainsWeight(lastChainsAmount.coerceIn(1.0, chainsMax.coerceAtLeast(1.0)))
                                }
                            },
                            secondaryLabel = "Edit",
                            onSecondaryClick = {
                                editingFeature = StrengthFeatureDialogConfig(
                                    title = "Chains",
                                    current = if (chainsActive) chainsCurrent else lastChainsAmount.coerceIn(0.0, chainsMax),
                                    min = 0.0,
                                    max = chainsMax,
                                    unit = "lb",
                                    supportingText = "Chains add resistance as you move. The device clamps this to the smaller of base weight and remaining headroom to 200 lb.",
                                    enabled = settingEnabled,
                                    onApply = onSetChainsWeight,
                                )
                            },
                        )
                    }
                    StrengthSettingsSection.INVERSE_CHAINS -> {
                        StrengthFeatureButton(
                            title = "Inverse Chains",
                            current = inverseChains?.let { if (it) "On" else "Off" } ?: "Unknown",
                            detail = if (reading.inverseChains == null && optimisticInverseChains != null) {
                                "Sent ${if (optimisticInverseChains == true) "on" else "off"}; a read-back request is queued after writes."
                            } else {
                                "Independent On/Off control is live. Fine-tuning support is still being developed."
                            },
                            enabled = inverseSettingEnabled,
                            actionLabel = if (inverseChainsActive) "Off" else "On",
                            onClick = {
                                val next = !inverseChainsActive
                                optimisticInverseChains = next
                                onSetInverseChains(next)
                            },
                        )
                        Text(
                            "Inverse Chains fine-tuning is still being developed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    StrengthSettingsSection.ECCENTRIC -> {
                        StrengthFeatureButton(
                            title = "Eccentric",
                            current = "${eccentricCurrent.roundToInt()} lb, ${if (eccentricActive) "On" else "Off"}",
                            detail = "Magnitude 0-${eccentricMax.roundToInt()} lb; choose Add or Reduce inside the editor.",
                            enabled = settingEnabled,
                            actionLabel = if (eccentricActive) "Off" else "On",
                            onClick = {
                                if (eccentricActive) {
                                    onSetEccentricWeight(0.0)
                                } else {
                                    editingEccentric = true
                                }
                            },
                            secondaryLabel = "Edit",
                            onSecondaryClick = { editingEccentric = true },
                        )
                    }
                    StrengthSettingsSection.CUSTOM_CURVE -> {
                        Text(
                            "Custom Curve is under development.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Slider(
                            value = 0.5f,
                            onValueChange = {},
                            enabled = false,
                            valueRange = 0f..1f,
                        )
                    }
                }
                if (!settingEnabled && selectedStrengthSection != StrengthSettingsSection.INVERSE_CHAINS) {
                    Text(
                        disabledReason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
        )
    }

    MetricCard {
        Text("Resistance Profile", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(
            base?.let { "Base ${it.roundToInt()} lb. Chains can add up to ${chainsMax.roundToInt()} lb. Eccentric ranges ${eccentricMin.roundToInt()} to +${eccentricMax.roundToInt()} lb. Inverse Chains stays independent." }
                ?: "Set a base weight before editing Chains or Eccentric. Inverse Chains stays independent.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        DetailRow("Chains", "${chainsCurrent.roundToInt()} lb ${if (chainsActive) "on" else "off"}")
        DetailRow("Inverse Chains", inverseChains?.let { if (it) "On" else "Off" } ?: "Unknown")
        DetailRow("Eccentric", "${eccentricCurrent.roundToInt()} lb ${if (eccentricActive) "on" else "off"}")
        FilledTonalButton(
            onClick = { showStrengthSettings = true },
            enabled = controlReady,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = LocalControlAccent.current.accentContainer,
                contentColor = LocalControlAccent.current.onAccentContainer,
            ),
        ) {
            Text("Open Strength Settings")
        }
    }
}

private enum class StrengthSettingsSection(val label: String) {
    GENERAL("General"),
    CHAINS("Chains"),
    INVERSE_CHAINS("Inverse Chains"),
    ECCENTRIC("Eccentric"),
    CUSTOM_CURVE("Custom Curve"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeightTrainingSettingsSheet(
    selectedSection: StrengthSettingsSection,
    developerModeEnabled: Boolean,
    onDismiss: () -> Unit,
    onSelectSection: (StrengthSettingsSection) -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Strength Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    StrengthSettingsSection.GENERAL,
                    StrengthSettingsSection.CHAINS,
                ).forEach { section ->
                    FilterChip(
                        selected = selectedSection == section,
                        onClick = { onSelectSection(section) },
                        label = { Text(section.label) },
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    StrengthSettingsSection.INVERSE_CHAINS,
                    StrengthSettingsSection.ECCENTRIC,
                ).forEach { section ->
                    FilterChip(
                        selected = selectedSection == section,
                        onClick = { onSelectSection(section) },
                        label = { Text(section.label) },
                    )
                }
            }
            if (developerModeEnabled) {
                FilterChip(
                    selected = selectedSection == StrengthSettingsSection.CUSTOM_CURVE,
                    onClick = { onSelectSection(StrengthSettingsSection.CUSTOM_CURVE) },
                    label = { Text(StrengthSettingsSection.CUSTOM_CURVE.label) },
                )
            }
            content()
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text("Close")
            }
        }
    }
}

@Composable
private fun StrengthFeatureButton(
    title: String,
    current: String,
    detail: String,
    enabled: Boolean,
    actionLabel: String,
    onClick: () -> Unit,
    secondaryLabel: String? = null,
    onSecondaryClick: (() -> Unit)? = null,
) {
    val accent = LocalControlAccent.current
    Surface(
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = if (enabled) 0.6f else 0.25f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    "Current $current",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    detail,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (secondaryLabel != null && onSecondaryClick != null) {
                    OutlinedButton(onClick = onSecondaryClick, enabled = enabled) {
                        Text(secondaryLabel)
                    }
                }
                Button(
                    onClick = onClick,
                    enabled = enabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent.accent,
                        contentColor = accent.onAccent,
                    ),
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
private fun StrengthFeatureDialog(
    config: StrengthFeatureDialogConfig,
    onDismiss: () -> Unit,
) {
    var selectedValue by remember(config.title, config.current, config.min, config.max) {
        mutableDoubleStateOf(config.current.coerceIn(config.min, config.max))
    }
    var inputValue by remember(config.title, config.current) {
        mutableStateOf(formatWeightValue(selectedValue))
    }

    fun setSelected(value: Double) {
        selectedValue = value.coerceIn(config.min, config.max)
        inputValue = formatWeightValue(selectedValue)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(config.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    config.supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (config.min < config.max) {
                    Slider(
                        value = selectedValue.toFloat(),
                        onValueChange = { setSelected(it.toDouble()) },
                        valueRange = config.min.toFloat()..config.max.toFloat(),
                        steps = sliderSteps(config.min, config.max, 1.0),
                    )
                } else {
                    Text("Only ${formatWeightValue(config.min)} ${config.unit} is available at this base weight.")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("${formatWeightValue(config.min)} ${config.unit}", style = MaterialTheme.typography.labelMedium)
                    Text("${formatWeightValue(config.max)} ${config.unit}", style = MaterialTheme.typography.labelMedium)
                }
                OutlinedTextField(
                    value = inputValue,
                    onValueChange = { text ->
                        inputValue = text
                        text.toDoubleOrNull()?.let(::setSelected)
                    },
                    label = { Text(config.unit) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = inputValue.toDoubleOrNull() ?: selectedValue
                    config.onApply(parsed.coerceIn(config.min, config.max))
                    onDismiss()
                },
                enabled = config.enabled,
            ) {
                Text("Set")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun EccentricFeatureDialog(
    current: Double,
    maxMagnitude: Double,
    enabled: Boolean,
    onApply: (Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var addLoad by remember(current) { mutableStateOf(current >= 0.0) }
    var selectedMagnitude by remember(current, maxMagnitude) {
        mutableDoubleStateOf(kotlin.math.abs(current).coerceIn(0.0, maxMagnitude))
    }
    var inputValue by remember(current, maxMagnitude) {
        mutableStateOf(formatWeightValue(selectedMagnitude))
    }

    fun setMagnitude(value: Double) {
        selectedMagnitude = value.coerceIn(0.0, maxMagnitude)
        inputValue = formatWeightValue(selectedMagnitude)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eccentric") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Use the slider as a positive amount. Add sends a positive eccentric value; Reduce sends the same amount as a negative value.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = addLoad,
                        onClick = { addLoad = true },
                        label = { Text("Add") },
                    )
                    FilterChip(
                        selected = !addLoad,
                        onClick = { addLoad = false },
                        label = { Text("Reduce") },
                    )
                }
                if (maxMagnitude > 0.0) {
                    Slider(
                        value = selectedMagnitude.toFloat(),
                        onValueChange = { setMagnitude(it.toDouble()) },
                        valueRange = 0f..maxMagnitude.toFloat(),
                        steps = sliderSteps(0.0, maxMagnitude, 1.0),
                    )
                } else {
                    Text("Set a base weight before editing Eccentric.")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("0 lb", style = MaterialTheme.typography.labelMedium)
                    Text("${formatWeightValue(maxMagnitude)} lb", style = MaterialTheme.typography.labelMedium)
                }
                OutlinedTextField(
                    value = inputValue,
                    onValueChange = { text ->
                        inputValue = text
                        text.toDoubleOrNull()?.let(::setMagnitude)
                    },
                    label = { Text("lb") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                Text(
                    "Will send ${if (addLoad) "+" else "-"}${formatWeightValue(selectedMagnitude)} lb.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = inputValue.toDoubleOrNull() ?: selectedMagnitude
                    val magnitude = parsed.coerceIn(0.0, maxMagnitude)
                    onApply(if (addLoad) magnitude else -magnitude)
                    onDismiss()
                },
                enabled = enabled,
            ) {
                Text("Set")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

private data class StrengthFeatureDialogConfig(
    val title: String,
    val current: Double,
    val min: Double,
    val max: Double,
    val unit: String,
    val supportingText: String,
    val enabled: Boolean,
    val onApply: (Double) -> Unit,
)

@Composable
private fun FeaturePlaceholderRow(
    title: String,
    value: String,
    detail: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AssistChip(
            onClick = {},
            enabled = false,
            label = { Text(value) },
        )
    }
}

@Composable
private fun PermissionCard(
    permissionState: RuntimePermissionState,
    onRequestPermissions: () -> Unit,
) {
    val requiredText = if (permissionState.requiredGranted) {
        "Bluetooth is ready."
    } else {
        "Missing ${permissionState.missingRequiredPermissions.joinToString { it.shortPermissionName() }}."
    }
    MetricCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Permissions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(requiredText, style = MaterialTheme.typography.bodyMedium)
                if (permissionState.missingOptionalPermissions.isNotEmpty()) {
                    Text(
                        "Notifications are optional for the foreground connection notice.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            OutlinedButton(onClick = onRequestPermissions) {
                Text(if (permissionState.allGranted) "Granted" else "Grant")
            }
        }
    }
}

@Composable
private fun DeviceRow(
    result: VoltraScanResult,
    onConnect: () -> Unit,
) {
    MetricCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    result.device.name ?: "Unnamed BLE device",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(result.device.address, style = MaterialTheme.typography.labelMedium)
                Text(
                    listOfNotNull(
                        result.device.rssi?.let { "$it dBm" },
                        if (result.device.isLikelyVoltra) "VOLTRA candidate" else "diagnostic",
                        result.connectable?.let { if (it) "connectable" else "not connectable" },
                    ).joinToString(" - "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(12.dp))
            Button(onClick = onConnect) {
                Text(if (result.device.isLikelyVoltra) "Connect" else "Inspect")
            }
        }
    }
}

@Composable
private fun ReadingCard(reading: VoltraReading) {
    MetricCard {
        Text("Readings", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        DetailRow("Battery", reading.batteryPercent?.let { "$it%" } ?: "Unknown")
        DetailRow("Firmware", reading.firmwareVersion ?: "Unknown")
        DetailRow("Serial", reading.serialNumber ?: "Unknown")
        DetailRow("Activation", reading.activationState ?: "Unknown")
        DetailRow("Cable", reading.cableLengthCm?.let { "$it cm" } ?: "Unknown")
        DetailRow("Cable offset", reading.cableOffsetCm?.let { "$it cm" } ?: "Unknown")
        DetailRow("Force", reading.forceLb?.let { "$it lb" } ?: "Unknown")
        DetailRow("Target", reading.weightLb?.let { "$it lb" } ?: "Unknown")
        DetailRow("Resistance Band", reading.resistanceBandMaxForceLb?.let { "$it lb max" } ?: "Unknown")
        DetailRow("Band length", reading.resistanceBandLengthCm?.let { "$it cm" } ?: "Unknown")
        DetailRow("Band ROM length", reading.resistanceBandByRangeOfMotion?.let { if (it) "On" else "Off" } ?: "Unknown")
        DetailRow("Band inverse", reading.resistanceBandInverse?.let { if (it) "On" else "Off" } ?: "Unknown")
        DetailRow("Quick cable", reading.quickCableAdjustment?.let { if (it) "On" else "Off" } ?: "Unknown")
        DetailRow("Chains", reading.chainsWeightLb?.let { "$it lb" } ?: "Unknown")
        DetailRow("Eccentric", reading.eccentricWeightLb?.let { "$it lb" } ?: "Unknown")
        DetailRow("Inverse chains", reading.inverseChains?.let { if (it) "On" else "Off" } ?: "Unknown")
        DetailRow("Workout peak force", reading.workoutPeakForceLb?.let { "${formatWeightValue(it)} lb" } ?: "Unknown")
        DetailRow("Workout peak power", reading.workoutPeakPowerWatts?.let { "$it W" } ?: "Unknown")
        DetailRow("Workout time to peak", reading.workoutTimeToPeakMillis?.let(::formatSecondsClock) ?: "Unknown")
        DetailRow("Sets", reading.setCount?.toString() ?: "Unknown")
        DetailRow("Reps", reading.repCount?.toString() ?: "Unknown")
        DetailRow("Rep phase", reading.repPhase ?: "Unknown")
        DetailRow("Mode", reading.workoutMode ?: "Unknown")
    }
}

@Composable
private fun SafetyCard(state: VoltraSessionState) {
    MetricCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Safety", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    if (state.safety.canLoad) "Ready to load." else "Load is guarded.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.safety.canLoad) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AssistChip(
                onClick = {},
                label = { Text(if (state.controlCommandsEnabled) "Validated" else "Locked") },
            )
        }
        DetailRow("Fitness mode", state.safety.fitnessMode?.toString() ?: "Unknown")
        DetailRow("Workout state", state.safety.workoutState?.toString() ?: "Unknown")
        DetailRow("Target load", state.safety.targetLoadLb?.let { "${it.roundToInt()} lb" } ?: "Unknown")
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
        state.safety.reasons.take(4).forEach { reason ->
            Text(reason, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ConnectionTraceCard(state: VoltraSessionState) {
    MetricCard {
        Text("Connection Trace", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        DetailRow("Connection", state.connectionState.displayText())
        DetailRow("Protocol", state.protocolStatus.displayText())
        DetailRow("Connected at", state.connectedAtMillis?.toString() ?: "None")
        DetailRow("Disconnected at", state.lastDisconnectAtMillis?.toString() ?: "None")
        DetailRow("Last duration", state.lastConnectionDurationMillis?.let { "$it ms" } ?: "None")
        DetailRow("Last reason", state.lastDisconnectReason ?: "None")
    }
}

@Composable
private fun GattSnapshotCard(snapshot: VoltraGattSnapshot?) {
    MetricCard {
        Text("GATT Snapshot", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        if (snapshot == null) {
            Text("Connect to view services and characteristics.", style = MaterialTheme.typography.bodyMedium)
        } else {
            DetailRow("Services", snapshot.services.size.toString())
            DetailRow("Characteristics", snapshot.characteristicCount.toString())
            snapshot.services.forEach { service ->
                Spacer(Modifier.height(8.dp))
                Text(
                    service.uuid,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                service.characteristics.forEach { characteristic ->
                    Text(
                        "${characteristic.uuid} ${characteristic.properties} ${characteristic.candidateRole}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun FrameRow(frame: RawVoltraFrame) {
    MetricCard {
        Text("${frame.direction} ${frame.characteristicUuid}", style = MaterialTheme.typography.labelLarge)
        frame.parsedSummary?.let {
            Text(it, style = MaterialTheme.typography.bodySmall)
        }
        Text(frame.hex, style = MaterialTheme.typography.bodySmall)
        frame.asciiPreview?.let {
            Text(it, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun CommandRow(command: VoltraCommandResult) {
    MetricCard {
        Text("${command.command} - ${command.status}", fontWeight = FontWeight.SemiBold)
        Text(command.message, style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HoldToLoadButton(
    enabled: Boolean,
    onLoad: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = LocalControlAccent.current
    val color = if (enabled) accent.accent else MaterialTheme.colorScheme.surface
    val textColor = if (enabled) accent.onAccent else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        color = color,
        contentColor = textColor,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .combinedClickable(
                enabled = enabled,
                onClick = {},
                onLongClick = onLoad,
            ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(if (enabled) "Hold to Load" else "Load Locked", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ThemePickerRow(
    accentColor: AccentColor,
    onSetAccentColor: (AccentColor) -> Unit,
) {
    MetricCard {
        Text("Theme", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AccentColor.entries.chunked(5).forEach { rowOptions ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    rowOptions.forEach { option ->
                        val swatch = accentPrimaryColor(option)
                        val selected = option == accentColor
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(swatch)
                                .border(
                                    BorderStroke(
                                        2.dp,
                                        if (selected) Color(0xFFFFC857) else MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                                    ),
                                    CircleShape,
                                )
                                .clickable { onSetAccentColor(option) },
                        )
                    }
                    Spacer(Modifier.weight(1f))
                }
            }
            Text(
                accentColor.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ToggleRow(
    checked: Boolean,
    label: String,
    detail: String,
    onCheckedChange: (Boolean) -> Unit,
) {
    MetricCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = checked, onCheckedChange = onCheckedChange)
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        subtitle?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MetricCard(content: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.9f),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.1f),
        )
    }
}

@Composable
private fun EmptyState(text: String) {
    MetricCard {
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun NavMark(text: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun screenPadding(
    start: Dp = 16.dp,
    top: Dp = 14.dp,
    end: Dp = 16.dp,
    bottom: Dp = 18.dp,
) = PaddingValues(start = start, top = top, end = end, bottom = bottom)

private fun String.shortPermissionName(): String = substringAfterLast('.').replace('_', ' ')

private fun VoltraConnectionState.displayText(): String = when (this) {
    VoltraConnectionState.IDLE -> "Ready"
    VoltraConnectionState.SCANNING -> "Scanning"
    VoltraConnectionState.CONNECTING -> "Connecting"
    VoltraConnectionState.DISCOVERING_SERVICES -> "Discovering"
    VoltraConnectionState.SUBSCRIBING -> "Handshaking"
    VoltraConnectionState.CONNECTED -> "Connected"
    VoltraConnectionState.DISCONNECTING -> "Disconnecting"
    VoltraConnectionState.DISCONNECTED -> "Disconnected"
    VoltraConnectionState.FAILED -> "Needs attention"
}

@Composable
private fun VoltraConnectionState.statusColor(): Color = when (this) {
    VoltraConnectionState.CONNECTED -> MaterialTheme.colorScheme.primary
    VoltraConnectionState.FAILED,
    VoltraConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.error
    VoltraConnectionState.SCANNING,
    VoltraConnectionState.CONNECTING,
    VoltraConnectionState.DISCOVERING_SERVICES,
    VoltraConnectionState.SUBSCRIBING,
    VoltraConnectionState.DISCONNECTING -> MaterialTheme.colorScheme.tertiary
    VoltraConnectionState.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun VoltraConnectionState.hasActiveGattSession(): Boolean = when (this) {
    VoltraConnectionState.CONNECTING,
    VoltraConnectionState.DISCOVERING_SERVICES,
    VoltraConnectionState.SUBSCRIBING,
    VoltraConnectionState.CONNECTED -> true
    else -> false
}

private fun VoltraProtocolStatus.displayText(): String = when (this) {
    VoltraProtocolStatus.UNKNOWN -> "Protocol unknown"
    VoltraProtocolStatus.BLE_ONLY -> "BLE diagnostic mode"
    VoltraProtocolStatus.VOLTRA_GATT_MATCH -> "VOLTRA GATT matched"
    VoltraProtocolStatus.RAW_FRAMES_SEEN -> "VOLTRA frames seen"
    VoltraProtocolStatus.COMMAND_PROTOCOL_VALIDATED -> "Local controls validated"
}

private fun VoltraSessionState.isLatestSessionTimestamp(timestampMillis: Long): Boolean {
    val start = connectedAtMillis ?: return false
    val end = lastDisconnectAtMillis
    return timestampMillis >= start && (end == null || timestampMillis <= end)
}
