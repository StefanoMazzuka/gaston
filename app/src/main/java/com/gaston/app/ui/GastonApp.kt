package com.gaston.app.ui

import android.content.Context
import android.media.MediaPlayer
import androidx.compose.foundation.BorderStroke
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import com.gaston.app.R
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*
import com.gaston.app.data.*
import com.gaston.app.domain.*
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

private val Spanish = Locale.forLanguageTag("es-ES")
private fun euros(cents: Long) = NumberFormat.getCurrencyInstance(Spanish).format(java.math.BigDecimal.valueOf(cents, 2))

private fun playUiSound(context: Context, resourceId: Int) {
    val player = MediaPlayer.create(context, resourceId) ?: return
    player.setOnCompletionListener { it.release() }
    player.setOnErrorListener { mediaPlayer, _, _ ->
        mediaPlayer.release()
        true
    }
    player.start()
}

private val dateFormat = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Spanish)
private data class ExpenseCategory(val name: String, val icon: String)
private val expenseCategories = listOf(
    ExpenseCategory("Compra", "🛒"),
    ExpenseCategory("Supermercado", "🛍️"),
    ExpenseCategory("Hogar", "🏠"),
    ExpenseCategory("Luz", "💡"),
    ExpenseCategory("Suscripciones", "📺"),
    ExpenseCategory("Salud", "❤️"),
    ExpenseCategory("Transporte", "🚌"),
    ExpenseCategory("Café", "☕"),
    ExpenseCategory("Regalos", "🎁"),
    ExpenseCategory("Comida", "🍕"),
    ExpenseCategory("Comida rápida", "🍔"),
    ExpenseCategory("Panadería", "🥖"),
    ExpenseCategory("Internet", "📶"),
    ExpenseCategory("Agua", "💧"),
    ExpenseCategory("Gasolina", "⛽"),
    ExpenseCategory("Coche", "🚗"),
    ExpenseCategory("Taxi", "🚕"),
    ExpenseCategory("Viajes", "✈️"),
    ExpenseCategory("Farmacia", "💊"),
    ExpenseCategory("Deporte", "🏋️"),
    ExpenseCategory("Peluquería", "✂️"),
    ExpenseCategory("Ropa", "👗"),
    ExpenseCategory("Ocio", "🎬"),
    ExpenseCategory("Videojuegos", "🎮"),
    ExpenseCategory("Mascotas", "🐶"),
    ExpenseCategory("Bebé", "👶"),
    ExpenseCategory("Trabajo", "💼"),
    ExpenseCategory("Estudios", "🎓"),
    ExpenseCategory("Banco", "🏦"),
    ExpenseCategory("Reparaciones", "🔧"),
    ExpenseCategory("Envíos", "📦"),
    ExpenseCategory("Otros", "❓")
)

private fun cycleProgress(remaining: Long, initial: Long): Float {
    if (initial <= 0) return 0f
    return (remaining.toFloat() / initial.toFloat()).coerceIn(0f, 1f)
}

@Composable
fun GastonApp(vm: GastonViewModel) {
    val nav = rememberNavController()
    val context = LocalContext.current
    val loadedBags by vm.bags.collectAsStateWithLifecycle()
    val bags = loadedBags.orEmpty()
    val favoriteBagId by vm.favoriteBagId.collectAsStateWithLifecycle()
    var startupHandled by rememberSaveable { mutableStateOf(false) }
    val error by vm.error.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    var today by remember { mutableStateOf(LocalDate.now()) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) { today = LocalDate.now(); vm.refresh() }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) {
        vm.refresh()
        while (true) { delay(30_000); val now = LocalDate.now(); if (now != today) { today = now; vm.refresh() } }
    }
    MaterialTheme(colorScheme = lightColorScheme(
        primary = Color(0xFF176B53), onPrimary = Color.White,
        primaryContainer = Color(0xFFD3F1DF), onPrimaryContainer = Color(0xFF103F31),
        secondary = Color(0xFFD96842), onSecondary = Color.White,
        secondaryContainer = Color(0xFFFFE1D5), onSecondaryContainer = Color(0xFF632B1B),
        tertiary = Color(0xFFE1A321), onTertiary = Color(0xFF392600),
        tertiaryContainer = Color(0xFFFFE8AE), onTertiaryContainer = Color(0xFF523800),
        background = Color(0xFFF3F7EE), onBackground = Color(0xFF1D392F),
        surface = Color(0xFFFFFEF8), onSurface = Color(0xFF1D392F),
        surfaceVariant = Color(0xFFE6EEE2), onSurfaceVariant = Color(0xFF52645A),
        surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF7FAF3),
        surfaceContainer = Color(0xFFEDF4E9), surfaceContainerHigh = Color(0xFFE6EEE2),
        surfaceContainerHighest = Color(0xFFDDE8DC), surfaceTint = Color(0xFF176B53),
        outline = Color(0xFF75877B), outlineVariant = Color(0xFFD3DED2),
        error = Color(0xFFB63D36), onError = Color.White,
        errorContainer = Color(0xFFFFDAD5), onErrorContainer = Color(0xFF6D1716)
    )) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            val backStackEntry by nav.currentBackStackEntryAsState()
            val selectedTab = backStackEntry?.destination?.route
            Scaffold(
                modifier = Modifier.safeDrawingPadding(),
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    if (selectedTab == "bags" || selectedTab == "treasury") {
                        NavigationBar {
                            NavigationBarItem(
                                selected = selectedTab == "bags",
                                onClick = { if (selectedTab != "bags") nav.popBackStack("bags", inclusive = false) },
                                icon = { Image(painterResource(R.drawable.bag), contentDescription = null, modifier = Modifier.size(24.dp)) },
                                label = { Text("Sacos") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == "treasury",
                                onClick = {
                                    if (selectedTab != "treasury") {
                                        playUiSound(context, R.raw.chest_open)
                                        nav.navigate("treasury") { launchSingleTop = true }
                                    }
                                },
                                icon = { Image(painterResource(if (selectedTab == "treasury") R.drawable.chest_open else R.drawable.chest_close), contentDescription = null, modifier = Modifier.size(24.dp)) },
                                label = { Text("Ahorro") }
                            )
                        }
                    }
                }
            ) { contentPadding ->
            NavHost(nav, "bags", Modifier.padding(contentPadding)) {
                composable("bags") {
                    LaunchedEffect(loadedBags) {
                        if (!startupHandled && loadedBags != null) {
                            startupHandled = true
                            bags.firstOrNull { it.bag.id == favoriteBagId }?.let {
                                nav.navigate("bag/${it.bag.id}") { launchSingleTop = true }
                            }
                        }
                    }
                    if (!startupHandled) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                        return@composable
                    }
                    var showHowItWorks by rememberSaveable { mutableStateOf(false) }
                    if (showHowItWorks) HowItWorksDialog { showHowItWorks = false }
                    val totalSaved = bags.sumOf { it.treasury(today) }
                    Page("Sacos", "Resumen de tus presupuestos", onInfo = { showHowItWorks = true }) {
                        if (bags.isEmpty()) Section {
                            Text("🏆", style = MaterialTheme.typography.displaySmall)
                            Text("¡Tu aventura empieza aquí!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("Crea tu primer saco y descubre cuántas monedas puedes guardar para cada día.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        bags.forEach { data ->
                            val budget = currentBudget(data, today)
                            val cycle = data.cycles.find { today.toString() >= it.start && today.toString() < it.end }
                            val progress = budget?.let { cycleProgress(it.remaining, cycle?.initial ?: 0L) } ?: 0f
                            OutlinedCard(onClick = {
                                playUiSound(context, R.raw.bag_open)
                                nav.navigate("bag/${data.bag.id}")
                            }, modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                                Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Image(
                                        painter = painterResource(R.drawable.bag),
                                        contentDescription = "Saco de presupuesto ${data.bag.name}",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.padding(end = 12.dp).size(76.dp)
                                    )
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                        Text("TU TESORO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                                        Text(data.bag.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                        budget?.let {
                                            Text(euros(it.remaining), style = MaterialTheme.typography.headlineSmall,
                                                color = if (it.remaining < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold)
                                            LinearProgressIndicator(
                                                progress = { progress },
                                                modifier = Modifier.fillMaxWidth().height(8.dp),
                                                color = if (it.remaining < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                                                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                                drawStopIndicator = {}
                                            )
                                            Text("${(progress * 100).toInt()}% de monedas disponibles", style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Column(
                                        modifier = Modifier.align(Alignment.Top),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        IconToggleButton(
                                            checked = favoriteBagId == data.bag.id,
                                            onCheckedChange = { vm.toggleFavorite(data.bag.id) },
                                            enabled = !busy
                                        ) {
                                            Icon(
                                                painterResource(if (favoriteBagId == data.bag.id) R.drawable.ic_star_filled else R.drawable.ic_star_outline),
                                                contentDescription = if (favoriteBagId == data.bag.id) "Quitar ${data.bag.name} de favorito" else "Abrir ${data.bag.name} al iniciar la app",
                                                tint = if (favoriteBagId == data.bag.id) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(
                                            onClick = { nav.navigate("edit/${data.bag.id}") },
                                            enabled = !busy,
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Icon(painterResource(R.drawable.ic_edit), contentDescription = "Editar ${data.bag.name}")
                                        }
                                    }
                                }
                            }
                        }
                        OutlinedCard(
                            onClick = {
                                playUiSound(context, R.raw.chest_open)
                                nav.navigate("treasury") { launchSingleTop = true }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary)
                        ) {
                            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Image(painterResource(R.drawable.chest_close), contentDescription = null,
                                    contentScale = ContentScale.Fit, modifier = Modifier.size(76.dp).padding(end = 12.dp))
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("TU COFRE DE AHORRO", style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                    Text(euros(totalSaved), style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                    Text("Ver ahorro acumulado", style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer)
                                }
                            }
                        }
                        Button(onClick = { nav.navigate("create") }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(16.dp)) {
                            Text("＋ Crear un saco")
                        }
                        CreatorCredit()
                    }
                }
                composable("create") { CreateBag(busy, { nav.popBackStack() }) { name, income, day, saving, percent, costs, opening ->
                    vm.create(name, income, day, saving, percent, costs, opening) { id -> nav.popBackStack(); nav.navigate("bag/$id") }
                } }
                composable("edit/{id}") { entry ->
                    val data = bags.find { it.bag.id == entry.arguments?.getString("id") }
                    if (data != null) CreateBag(
                        busy,
                        { nav.popBackStack() },
                        existing = data,
                        onDelete = { vm.delete(data.bag.id) { nav.popBackStack() } }
                    ) { name, income, day, saving, percent, costs, opening ->
                        vm.update(data.bag.id, name, income, day, saving, percent, costs, opening) { nav.popBackStack() }
                    }
                }
                composable("bag/{id}") { entry ->
                    val data = bags.find { it.bag.id == entry.arguments?.getString("id") }
                    if (data != null) BagScreen(data, today, busy, { nav.popBackStack() }, { nav.navigate("calendar/${data.bag.id}") }, vm)
                    else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
                composable("treasury") { TreasuryScreen(bags, today) }
                composable("calendar/{id}") { entry ->
                    val data = bags.find { it.bag.id == entry.arguments?.getString("id") }
                    if (data != null) CalendarScreen(data, today, { nav.popBackStack() }, busy, vm)
                }
            }
            if (error != null) AlertDialog(onDismissRequest = { vm.error.value = null }, title = { Text("No se ha podido completar") }, text = { Text(error!!) }, confirmButton = { TextButton(onClick = { vm.error.value = null }) { Text("Entendido") } })
            }
        }
    }
}

@Composable
private fun Page(
    title: String,
    subtitle: String,
    back: (() -> Unit)? = null,
    onInfo: (() -> Unit)? = null,
    infoDescription: String = "¿Cómo se calcula el presupuesto?",
    content: @Composable ColumnScope.() -> Unit
) {
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (back != null) TextButton(onClick = back) { Text("← Volver a la aventura") }
        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.primary) {
            Row(Modifier.padding(start = 20.dp, top = 18.dp, end = 12.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .82f))
                }
                if (onInfo != null) IconButton(onClick = onInfo) {
                    Icon(painterResource(R.drawable.ic_info), contentDescription = infoDescription, tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
        content()
        Spacer(Modifier.height(16.dp))
    }
}
@Composable
private fun Section(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

private fun currentBudget(data: BagData, today: LocalDate): Budget? {
    val cycle = data.cycles.find { today.toString() >= it.start && today.toString() < it.end } ?: return null
    return BudgetCalculator.calculate(cycle.initial, Window(LocalDate.parse(cycle.start), LocalDate.parse(cycle.end)), today,
        data.expenses.map { Spending(LocalDate.parse(it.date), it.cents) })
}

@Composable
private fun TreasuryScreen(bags: List<BagData>, today: LocalDate) {
    var showSavingsInfo by rememberSaveable { mutableStateOf(false) }
    val todayText = today.toString()
    val totalSaved = bags.sumOf { it.treasury(today) }
    if (showSavingsInfo) SavingsInfoDialog { showSavingsInfo = false }
    Page(
        "Tu cofre",
        "Ahorro real de tus ciclos cerrados",
        onInfo = { showSavingsInfo = true },
        infoDescription = "Cómo funciona la pestaña del cofre de ahorro"
    ) {
        Section {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.chest_open), contentDescription = "Cofre de ahorro abierto",
                    contentScale = ContentScale.Fit, modifier = Modifier.size(104.dp).padding(end = 12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("TOTAL ACUMULADO", style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                    Text(euros(totalSaved), style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Ahorro restante y dinero útil sobrante al cerrar cada ciclo", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Text("Ahorro por saco", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (bags.isEmpty()) {
            Text("Crea un saco para empezar a llenar tu cofre.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        bags.forEach { data ->
            val savedCycles = data.cycles.filter { it.end <= todayText }.sortedByDescending { it.end }
            val currentCycle = data.cycles.find { todayText >= it.start && todayText < it.end }
            val currentSaving = currentCycle?.let { data.balance(it, today).savingRemaining } ?: 0L
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.bag), contentDescription = "Saco de ${data.bag.name}",
                        contentScale = ContentScale.Fit, modifier = Modifier.size(44.dp).padding(end = 8.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(data.bag.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Ahorro estimado · ${euros(currentSaving)}", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (savedCycles.isEmpty()) Text("El primer depósito llegará al cerrar tu ciclo.",
                style = MaterialTheme.typography.bodySmall)
            savedCycles.forEach { closed ->
                val result = data.balance(closed, today)
                Section {
                    Text("${closed.start} → ${closed.end}", fontWeight = FontWeight.Bold)
                    Text("Objetivo de ahorro · ${euros(closed.saving)}")
                    Text("Ahorro conseguido · ${euros(result.saved)}")
                    Text(when {
                        result.differenceFromTarget > 0 -> "Superaste el objetivo en ${euros(result.differenceFromTarget)}: gastaste menos de lo previsto."
                        result.differenceFromTarget < 0 -> "Gastaste ${euros(-result.differenceFromTarget)} más de lo previsto."
                        else -> "Alcanzaste exactamente el ahorro deseado."
                    })
                    Text("Aportación al cofre · ${euros(result.saved)}", color = MaterialTheme.colorScheme.primary)
                    if (result.deficit > 0) Text("Déficit del ciclo · ${euros(result.deficit)}",
                        color = MaterialTheme.colorScheme.error)
                }
            }
        }
        Text("El cofre comienza en cero y acumula los saldos positivos de ciclos cerrados. Los déficits se muestran por separado. Corregir un gasto anterior actualiza su cierre.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        CreatorCredit()
    }
}

@Composable
private fun CreatorCredit() {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val versionName = remember(context) {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "—"
    }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        TextButton(onClick = { uriHandler.openUri("https://github.com/StefanoMazzuka/gaston") }) {
            Text("Stefano Mazzuka · GitHub", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("Versión $versionName", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SavingsInfoDialog(dismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text("Cómo funciona el cofre de ahorro") },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Los gastos consumen primero el dinero útil. Cuando se agota, reducen el ahorro estimado del ciclo.")
                Text("El cofre empieza en cero. Al cerrar cada ciclo se añade el ahorro restante junto con el dinero útil sobrante. El ahorro del ciclo en curso sigue fuera del cofre.")
                Text("Cada cierre compara el ahorro conseguido con el objetivo y muestra si gastaste más o menos de lo previsto. Si gastas también toda la reserva, verás el déficit del ciclo por separado.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = dismiss) { Text("Entendido") } }
    )
}

@Composable
private fun BagScreen(data: BagData, today: LocalDate, busy: Boolean, back: () -> Unit, calendar: () -> Unit, vm: GastonViewModel) {
    BackHandler(enabled = busy) { }
    val context = LocalContext.current
    var spending by rememberSaveable { mutableStateOf(false) }
    var scannedAmount by rememberSaveable { mutableStateOf<Long?>(null) }
    var showBagInfo by rememberSaveable { mutableStateOf(false) }
    var editingExpenseId by rememberSaveable { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf<Expense?>(null) }
    val budget = currentBudget(data, today)
    val cycle = data.cycles.find { today.toString() >= it.start && today.toString() < it.end }
    Page(data.bag.name, today.format(dateFormat), { if (!busy) back() },
        onInfo = { showBagInfo = true }, infoDescription = "Información y plan de ahorro del saco") {
        Section {
            val initial = cycle?.initial ?: 0L
            val remaining = budget?.remaining ?: 0L
            val progress = cycleProgress(remaining, initial)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("🪙 Monedas para hoy", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                    Text(budget?.let { euros(it.today) } ?: "Actualizando…", style = MaterialTheme.typography.displaySmall,
                        color = if ((budget?.today ?: 0) < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                }
                Image(painterResource(R.drawable.coins), contentDescription = "Montaña de monedas",
                    contentScale = ContentScale.Fit, modifier = Modifier.padding(start = 12.dp).size(76.dp))
            }
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(10.dp),
                color = if (remaining < 0) MaterialTheme.colorScheme.error else if (progress < 0.2f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant,
                drawStopIndicator = {}
            )
            Text("Disponible · ${euros(remaining.coerceAtLeast(0))}",
                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text("Fin del ciclo · ${cycle?.end ?: "…"} · ${budget?.days ?: 0} días restantes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if ((budget?.today ?: 0) < 0) Text(
            if ((budget?.days ?: 0) > 1) "Has superado el margen de hoy. El presupuesto de los próximos días ya se ha ajustado."
            else "Has superado el margen de hoy. Este gasto se reflejará en el ahorro al cerrar el ciclo.",
            color = MaterialTheme.colorScheme.error)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { scannedAmount = null; spending = true }, enabled = !busy && cycle != null,
                modifier = Modifier.weight(1f).heightIn(min = 52.dp), shape = RoundedCornerShape(16.dp)) { Text("🪙 Registrar gasto") }
            ReceiptCamera(enabled = !busy && cycle != null && !spending) { amount ->
                scannedAmount = amount
                spending = true
            }
        }
        OutlinedButton(onClick = calendar, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp), shape = RoundedCornerShape(16.dp)) { Text("📅 Calendario e historial") }
        Text("🧾 Tus últimos gastos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (data.expenses.isEmpty()) Text("Aún no hay gastos. ¡Tu tesoro sigue intacto!", color = MaterialTheme.colorScheme.onSurfaceVariant)
        data.expenses.sortedByDescending { it.date }.take(10).forEach { expense ->
            Row(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(16.dp)).padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(40.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text(expense.icon.ifBlank { "❓" }, style = MaterialTheme.typography.titleMedium) }
                }
                Column(Modifier.weight(1f).padding(horizontal = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(euros(expense.cents), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text(expense.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row {
                    IconButton(onClick = { editingExpenseId = expense.id }, enabled = !busy) {
                        Icon(painterResource(R.drawable.ic_edit), contentDescription = "Editar ${expense.name}")
                    }
                    IconButton(onClick = { deleting = expense }, enabled = !busy) {
                        Icon(painterResource(R.drawable.ic_close), contentDescription = "Deshacer ${expense.name}")
                    }
                }
            }
        }
    }
    if (showBagInfo) AlertDialog(
        onDismissRequest = { showBagInfo = false },
        title = { Text("Información del saco") },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val cycleSaving = cycle?.saving ?: 0L
                val templateSaving = Money.saving(data.bag.income, data.bag.savingValue, data.bag.savingPercent)
                val hasPendingChange = cycleSaving != templateSaving

                Text("🌱 Plan de ahorro", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Ahorro deseado este ciclo · ${euros(cycleSaving)}", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                cycle?.let {
                    val balance = data.balance(it, today)
                    Text("Ahorro estimado restante · ${euros(balance.savingRemaining)}")
                    if (balance.savingConsumed > 0) Text("Has utilizado ${euros(balance.savingConsumed)} del ahorro deseado.",
                        color = MaterialTheme.colorScheme.error)
                    if (balance.deficit > 0) Text("Déficit del ciclo · ${euros(balance.deficit)}",
                        color = MaterialTheme.colorScheme.error)
                    Text("Si no gastas más, al cerrar irán ${euros(balance.saved)} al cofre.",
                        style = MaterialTheme.typography.bodySmall)
                }
                if (hasPendingChange) Text("Próximo ciclo · ${euros(templateSaving)}", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary)
                if (hasPendingChange) {
                    Text("Has editado el saco. El nuevo ahorro entrará en vigor el ${cycle?.end ?: "próximo ciclo"}.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            
                Text("Configuración base", style = MaterialTheme.typography.titleMedium)
                Text("Ingreso mensual · ${euros(data.bag.income)}")
                if (data.costs.isNotEmpty()) {
                    Text("Salidas fijas mensuales:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    data.costs.forEach { Text("  • ${it.icon.ifBlank { "🏠" }} ${it.name}: ${euros(it.cents)}", style = MaterialTheme.typography.bodyMedium) }
                }
            }
        },
        confirmButton = { TextButton(onClick = { showBagInfo = false }) { Text("Cerrar") } }
    )

    if (spending) ExpenseDialog(busy, { spending = false }, initialAmount = scannedAmount) { name, icon, amount ->
        vm.spend(data.bag.id, name, icon, amount) {
            spending = false
            playUiSound(context, R.raw.expense_added)
        }
    }
    data.expenses.find { it.id == editingExpenseId }?.let { expense ->
        ExpenseDialog(busy, { editingExpenseId = null }, existing = expense) { name, icon, amount ->
            vm.editExpense(expense.id, name, icon, amount) { editingExpenseId = null }
        }
    }
    deleting?.let { expense -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text("¿Deshacer este gasto?") }, text = { Text("${expense.name}: ${euros(expense.cents)} volverán al saldo del ciclo correspondiente.") }, confirmButton = { TextButton(onClick = { vm.undo(expense.id); deleting = null }) { Text("Deshacer") } }, dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancelar") } }) }
}

@Composable
private fun HowItWorksDialog(dismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text("¿Cómo calcula Gaston tu presupuesto?") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("1. Presupuesto Inicial del Ciclo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Al comenzar el mes, Gaston descuenta tu ahorro programado y tus salidas fijas (alquiler, luz, etc.) del ingreso total para obtener el saldo realmente libre.", style = MaterialTheme.typography.bodyMedium)
                
                Text("2. Margen Diario ('Disponible hoy')", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Ese saldo libre se divide entre los días que faltan hasta tu próximo cobro. Cada gasto reduce el dinero útil restante y el margen de hoy. Hoy cuenta en el reparto; el día de cobro pertenece al siguiente ciclo.", style = MaterialTheme.typography.bodyMedium)
                
                Text("3. Ajuste Dinámico Diario", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("• Si ahorras hoy: El dinero que no gastes hoy aumentará tu límite diario del resto del mes.\n• Si te pasas hoy: El exceso se reparte restando una pequeña cuota de los días que faltan.", style = MaterialTheme.typography.bodyMedium)
                
                Text("4. Ahorro y cofre", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Si agotas el dinero útil, los gastos reducen el ahorro estimado. Al cerrar el ciclo, el ahorro restante y el dinero útil sobrante pasan al cofre y se comparan con tu objetivo. Los pagos fijos ya están descontados: registra aquí solo gastos adicionales.")
                Text("5. Fines de semana", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Si tu día de cobro cae en sábado o domingo, el ciclo se ajusta automáticamente al siguiente lunes hábil.", style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = { TextButton(onClick = dismiss) { Text("Entendido") } }
    )
}

@Composable
private fun AmountField(value: String, change: (String) -> Unit, label: String) {
    OutlinedTextField(value, change, label = { Text(label) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
}
@Composable
private fun IconPicker(selected: String, change: (String) -> Unit) {
    val scrollState = rememberScrollState()
    val chunkSize = (expenseCategories.size + 2) / 3
    val selectedCategory = expenseCategories.firstOrNull { it.icon == selected }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Concepto: ${selectedCategory?.name ?: "Personalizado"}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(selected.ifBlank { "❓" }, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            expenseCategories.chunked(chunkSize).forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { category ->
                        val isSelected = selected == category.icon
                        Surface(
                            onClick = { change(category.icon) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                if (isSelected) 2.dp else 1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier.size(width = 88.dp, height = 60.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Text(category.icon, style = MaterialTheme.typography.titleMedium)
                                Text(category.name, style = MaterialTheme.typography.labelSmall, maxLines = 2,
                                    overflow = TextOverflow.Ellipsis, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun ExpenseDialog(busy: Boolean, dismiss: () -> Unit, existing: Expense? = null, initialAmount: Long? = null, save: (String, String, Long) -> Unit) {
    var name by rememberSaveable(existing?.id) { mutableStateOf(existing?.name ?: "Otros") }
    var amount by rememberSaveable(existing?.id) { mutableStateOf((existing?.cents ?: initialAmount)?.let { java.math.BigDecimal.valueOf(it, 2).toPlainString() }.orEmpty()) }
    var icon by rememberSaveable(existing?.id) { mutableStateOf(existing?.icon?.ifBlank { "❓" } ?: "❓") }
    var showSuggestions by rememberSaveable(existing?.id) { mutableStateOf(false) }
    val matchingSuggestions = expenseCategories.filter {
        name.isNotBlank() && it.name.startsWith(name.trim(), ignoreCase = true) && !it.name.equals(name.trim(), ignoreCase = true)
    }.take(5)
    val cents = Money.parse(amount)
    AlertDialog(onDismissRequest = { if (!busy) dismiss() }, title = { Text(if (existing == null) "Registrar gasto" else "Editar gasto") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (initialAmount != null) Text("Revisa el importe leído del ticket antes de guardar.", style = MaterialTheme.typography.bodySmall)
            AmountField(amount, { amount = it }, "Importe (€)")
            OutlinedTextField(name, { name = it; showSuggestions = true }, label = { Text("¿En qué lo has gastado?") }, singleLine = true)
            if (showSuggestions && matchingSuggestions.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Sugerencias", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        matchingSuggestions.forEach { category ->
                            AssistChip(
                                onClick = { name = category.name; icon = category.icon; showSuggestions = false },
                                label = { Text("${category.icon} ${category.name}") }
                            )
                        }
                    }
                }
            }
            IconPicker(icon) {
                icon = it
                name = expenseCategories.firstOrNull { category -> category.icon == it }?.name ?: name
            }
        }
    }, confirmButton = { TextButton(enabled = !busy && cents != null && cents > 0 && name.isNotBlank(), onClick = { save(name, icon, cents!!) }) { Text(if (busy) "Guardando…" else "Guardar gasto") } }, dismissButton = { TextButton(enabled = !busy, onClick = dismiss) { Text("Cancelar") } })
}

@Composable
private fun CreateBag(
    busy: Boolean,
    back: () -> Unit,
    existing: BagData? = null,
    onDelete: (() -> Unit)? = null,
    save: (String, Long, Int, Long, Boolean, List<FixedCost>, Long?) -> Unit
) {
    BackHandler(enabled = busy) { }
    var showDeleteConfirmation by rememberSaveable(existing?.bag?.id) { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf(existing?.bag?.name.orEmpty()) }
    var income by rememberSaveable { mutableStateOf(existing?.bag?.income?.let { java.math.BigDecimal.valueOf(it, 2).toPlainString() }.orEmpty()) }
    var day by rememberSaveable { mutableStateOf(existing?.bag?.payday?.toString() ?: "25") }
    var saving by rememberSaveable { mutableStateOf(existing?.bag?.savingValue?.let { java.math.BigDecimal.valueOf(it, 2).toPlainString() } ?: "0") }
    var percent by rememberSaveable { mutableStateOf(existing?.bag?.savingPercent ?: false) }
    var opening by rememberSaveable {
        val currentInitial = existing?.cycles?.find { LocalDate.now().toString() >= it.start && LocalDate.now().toString() < it.end }?.initial
        mutableStateOf(currentInitial?.let { java.math.BigDecimal.valueOf(it, 2).toPlainString() }.orEmpty())
    }
    var costName by rememberSaveable { mutableStateOf("") }
    var costAmount by rememberSaveable { mutableStateOf("") }
    var costIcon by rememberSaveable { mutableStateOf("🏠") }
    var editingCostId by rememberSaveable { mutableStateOf<String?>(null) }
    val costs = rememberSaveable(saver = listSaver(
        save = { list -> list.flatMap { listOf(it.id, it.name, it.icon, it.cents.toString()) } },
        restore = { values -> mutableStateListOf<FixedCost>().apply {
            values.chunked(4).forEach { add(FixedCost(it[0], "", it[1], it[2], it[3].toLong())) }
        } }
    )) { mutableStateListOf<FixedCost>().apply { addAll(existing?.costs.orEmpty()) } }
    val inc = Money.parse(income)
    val sav = Money.parse(saving)
    val payday = day.toIntOrNull()
    val available = if (inc != null && sav != null && (!percent || sav <= 10000)) inc - Money.saving(inc, sav, percent) - costs.sumOf { it.cents } else null
    val valid = name.isNotBlank() && inc != null && inc > 0 && payday != null && payday in 1..31 && available != null && available >= 0 && (opening.isBlank() || Money.parse(opening) != null) && costName.isBlank() && costAmount.isBlank() && editingCostId == null
    Page(if (existing == null) "Nuevo saco" else "Editar saco", if (existing == null) "Prepara tu próximo tesoro mensual" else "El nombre cambia ahora. El resto se aplica al siguiente ciclo; el saldo y el historial actuales se conservan.", { if (!busy) back() }) {
        Text("🏁 Tu punto de partida", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        OutlinedTextField(name, { name = it }, label = { Text("Nombre del banco") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        AmountField(income, { income = it }, "Ingreso mensual (€)")
        OutlinedTextField(day, { day = it }, label = { Text("Día de cobro (1–31)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
        Text("Si cae en fin de semana, cobrarás el lunes siguiente.", style = MaterialTheme.typography.bodySmall)
        Text("🏠 Pagos fijos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        costs.forEach { cost ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("${cost.icon.ifBlank { "🏠" }} ${cost.name} · ${euros(cost.cents)}", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                IconButton(enabled = !busy && editingCostId == null && costName.isBlank() && costAmount.isBlank(), onClick = {
                    editingCostId = cost.id
                    costName = cost.name
                    costAmount = java.math.BigDecimal.valueOf(cost.cents, 2).toPlainString()
                    costIcon = cost.icon
                }) {
                    Icon(painterResource(R.drawable.ic_edit), contentDescription = "Editar ${cost.name}")
                }
                IconButton(enabled = !busy && editingCostId != cost.id, onClick = { costs.remove(cost) }) {
                    Icon(painterResource(R.drawable.ic_close), contentDescription = "Quitar ${cost.name}", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
        OutlinedTextField(costName, { costName = it }, label = { Text("Concepto: alquiler, luz…") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        AmountField(costAmount, { costAmount = it }, "Importe de la salida (€)")
        IconPicker(costIcon) {
            costIcon = it
            costName = expenseCategories.firstOrNull { category -> category.icon == it }?.name ?: costName
        }
        OutlinedButton(onClick = {
            val cost = FixedCost(editingCostId ?: UUID.randomUUID().toString(), "", costName.trim(), costIcon, Money.parse(costAmount)!!)
            val index = costs.indexOfFirst { it.id == editingCostId }
            if (index >= 0) costs[index] = cost else costs.add(cost)
            editingCostId = null; costName = ""; costAmount = ""
        }, enabled = !busy && costName.isNotBlank() && (Money.parse(costAmount) ?: 0) > 0) { Text(if (editingCostId == null) "Añadir salida" else "Guardar salida") }
        if (editingCostId != null) TextButton(enabled = !busy, onClick = { editingCostId = null; costName = ""; costAmount = "" }) { Text("Cancelar edición de salida") }
        Text("🌱 Ahorro primero", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) { Switch(percent, { percent = it }); Text(if (percent) "Porcentaje del ingreso" else "Cantidad fija", Modifier.padding(start = 12.dp)) }
        AmountField(saving, { saving = it }, if (percent) "Ahorro (%)" else "Ahorro (€)")
        Text("🪙 Disponible mensual: ${available?.let { euros(it) } ?: "—"}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        AmountField(opening, { opening = it }, if (existing == null) "Disponible real hoy (€), opcional" else "Ajustar disponible del ciclo actual (€), opcional")
        Text(if (existing == null) "Si empiezas a mitad de mes, indica lo que te queda libre después de reservar pagos y ahorro. Vacío usa el disponible mensual completo para este primer ciclo." else "Si necesitas corregir la cantidad disponible inicial para el ciclo en curso, puedes modificar este campo.", style = MaterialTheme.typography.bodySmall)
        if (available != null && available < 0) Text("Las salidas y el ahorro superan el ingreso.", color = MaterialTheme.colorScheme.error)
        if (editingCostId != null) Text("Guarda o cancela la edición de la salida antes de guardar el saco.")
        else if (costName.isNotBlank() || costAmount.isNotBlank()) Text("Añade la salida pendiente o vacía sus campos antes de guardar.")
        Button(onClick = { save(name, inc!!, payday!!, sav!!, percent, costs.toList(), if (opening.isBlank()) null else Money.parse(opening)) }, enabled = valid && !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(16.dp)) { Text(if (busy) "Guardando…" else if (existing == null) "✨ Crear mi saco" else "Guardar cambios") }
        if (onDelete != null) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            TextButton(
                onClick = { showDeleteConfirmation = true },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(painterResource(R.drawable.ic_close), contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Eliminar saco")
            }
        }
    }
    if (showDeleteConfirmation && onDelete != null) AlertDialog(
        onDismissRequest = { if (!busy) showDeleteConfirmation = false },
        title = { Text("¿Eliminar ${existing?.bag?.name}?") },
        text = { Text("Se eliminarán este saco, sus gastos, sus reservas y todo su historial. Esta acción no se puede deshacer.") },
        confirmButton = {
            TextButton(
                enabled = !busy,
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) { Text(if (busy) "Eliminando…" else "Eliminar saco") }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = { showDeleteConfirmation = false }) { Text("Cancelar") } }
    )
}

@Composable
private fun CalendarScreen(data: BagData, today: LocalDate, back: () -> Unit, busy: Boolean, vm: GastonViewModel) {
    BackHandler(enabled = busy) { }
    var editingExpenseId by rememberSaveable { mutableStateOf<String?>(null) }
    data.expenses.find { it.id == editingExpenseId }?.let { expense ->
        ExpenseDialog(busy, { editingExpenseId = null }, existing = expense) { name, icon, amount ->
            vm.editExpense(expense.id, name, icon, amount) { editingExpenseId = null }
        }
    }
    val cycle = data.cycles.find { today.toString() >= it.start && today.toString() < it.end }
    if (cycle == null) {
        Page("Calendario", data.bag.name, back) { Text("Actualizando ciclo…") }
        return
    }
    val start = LocalDate.parse(cycle.start)
    val end = LocalDate.parse(cycle.end)
    val firstMonth = YearMonth.from(start)
    val lastMonth = YearMonth.from(end)
    var monthText by rememberSaveable { mutableStateOf(YearMonth.from(today).toString()) }
    var selectedText by rememberSaveable { mutableStateOf(today.toString()) }
    var weekOnly by rememberSaveable { mutableStateOf(true) }
    val month = YearMonth.parse(monthText).coerceIn(firstMonth, lastMonth)
    val selected = LocalDate.parse(selectedText).coerceIn(start, end)
    val weekStart = selected.minusDays((selected.dayOfWeek.value - 1).toLong())
    val weekDates = (0L..6L).map { weekStart.plusDays(it) }.filter { it >= start && it <= end }
    val visibleDates = if (weekOnly) weekDates else
        (1..month.lengthOfMonth()).map { month.atDay(it) }.filter { it >= start && it <= end }
    val allowances = remember(data, today, month, cycle, selected, weekOnly) {
        val spending = data.expenses.map { Spending(LocalDate.parse(it.date), it.cents) }
        visibleDates.associateWith { date ->
            if (date < end)
                BudgetCalculator.calendarAllowance(cycle.initial, Window(start, end), today, date, spending)
            else null
        }
    }
    Page("Calendario", data.bag.name, { if (!busy) back() }) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = weekOnly,
                onClick = { weekOnly = true },
                shape = SegmentedButtonDefaults.itemShape(0, 2)
            ) { Text("Semana") }
            SegmentedButton(
                selected = !weekOnly,
                onClick = { weekOnly = false; monthText = YearMonth.from(selected).toString() },
                shape = SegmentedButtonDefaults.itemShape(1, 2)
            ) { Text("Mes") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(enabled = if (weekOnly) weekStart > start else month > firstMonth, onClick = {
                if (weekOnly) {
                    selectedText = maxOf(start, selected.minusWeeks(1)).toString()
                    monthText = YearMonth.from(LocalDate.parse(selectedText)).toString()
                } else {
                    val previous = month.minusMonths(1)
                    monthText = previous.toString()
                    selectedText = maxOf(start, previous.atDay(1)).toString()
                }
            }) { Text("←") }
            Text(
                if (weekOnly) {
                    val weekFormat = DateTimeFormatter.ofPattern("d MMM", Spanish)
                    "${weekDates.first().format(weekFormat)} – ${weekDates.last().format(weekFormat)}"
                } else month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Spanish)),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            TextButton(enabled = if (weekOnly) weekStart.plusDays(6) < end else month < lastMonth, onClick = {
                if (weekOnly) {
                    selectedText = minOf(end, selected.plusWeeks(1)).toString()
                    monthText = YearMonth.from(LocalDate.parse(selectedText)).toString()
                } else {
                    val next = month.plusMonths(1)
                    monthText = next.toString()
                    selectedText = next.atDay(1).toString()
                }
            }) { Text("→") }
        }
        if (weekOnly) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                weekDates.forEach { date ->
                    val isEnd = date == end
                    val allowance = allowances[date]
                    val cardColor = when {
                        date == selected -> MaterialTheme.colorScheme.primaryContainer
                        isEnd -> MaterialTheme.colorScheme.tertiaryContainer
                        date == today -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.surface
                    }
                    OutlinedCard(
                        onClick = { selectedText = date.toString() },
                        modifier = Modifier.widthIn(min = 80.dp),
                        colors = CardDefaults.outlinedCardColors(containerColor = cardColor),
                        border = BorderStroke(
                            if (date == selected) 2.dp else 1.dp,
                            if (date == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Column(
                            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(date.format(DateTimeFormatter.ofPattern("EEE", Spanish)).uppercase(Spanish),
                                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                            Text(if (isEnd) "🏁" else allowance?.let { euros(it) } ?: "—",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (allowance != null && allowance < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                            Text(if (date == today) "HOY" else if (data.expenses.any { it.date == date.toString() }) "•" else " ",
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        } else {
            Row { listOf("L", "M", "X", "J", "V", "S", "D").forEach { Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { Text(it, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
            val offset = month.atDay(1).dayOfWeek.value - 1
            val count = ((offset + month.lengthOfMonth() + 6) / 7) * 7
            (0 until count).toList().chunked(7).forEach { week ->
                Row { week.forEach { index ->
                    val number = index - offset + 1
                    val date = if (number in 1..month.lengthOfMonth()) month.atDay(number).takeIf { it >= start && it <= end } else null
                    val isEnd = date == end
                    val shape = RoundedCornerShape(14.dp)
                    val cellColor = when {
                        isEnd -> MaterialTheme.colorScheme.tertiaryContainer
                        date == selected -> MaterialTheme.colorScheme.primaryContainer
                        date == today -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.surface
                    }
                    Column(
                        Modifier.weight(1f).padding(2.dp).heightIn(min = 80.dp)
                            .background(cellColor, shape)
                            .then(if (isEnd || date == selected || date == today) Modifier.border(1.dp,
                                if (isEnd) MaterialTheme.colorScheme.tertiary else if (date == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary, shape) else Modifier)
                            .then(if (date != null) Modifier.clickable { selectedText = date.toString() } else Modifier),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (date != null) {
                            if (date == today) Text("HOY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                            Text(number.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                            val allowance = allowances[date]
                            Text(if (isEnd) "🏁" else allowance?.let { java.math.BigDecimal.valueOf(it, 2).toPlainString().replace('.', ',') } ?: "—",
                                style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                color = if (allowance != null && allowance < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                            if (!isEnd && data.expenses.any { it.date == date.toString() }) Text("🪙", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                } }
            }
        }
        val expenses = data.expenses.filter { it.date == selected.toString() }
        expenses.forEach { expense ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("${expense.icon} ${expense.name} · ${euros(expense.cents)}", Modifier.weight(1f))
                TextButton(enabled = !busy, onClick = { editingExpenseId = expense.id }) { Text("Editar") }
            }
        }
        if (expenses.isEmpty()) Text("Sin gastos", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
