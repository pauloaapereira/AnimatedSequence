package com.pp.animatedsequence

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.pp.animatedsequence.ui.theme.AnimatedSequenceTheme
import com.pp.library.AnimatedItem
import com.pp.library.AnimationSequenceHost
import com.pp.library.LazyAnimationHost
import com.pp.library.animatedItems
import com.pp.library.trackItems
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AnimatedSequenceTheme {
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
                var navigationKey by remember { mutableIntStateOf(0) }

                when (val screen = currentScreen) {
                    Screen.Home -> HomeScreen(onNavigate = { 
                        navigationKey++  // Increment to force fresh composition
                        currentScreen = it 
                    })
                    else -> key(navigationKey) {  // Fresh composition each visit
                        ExampleScreen(
                            screen = screen,
                            onBack = { currentScreen = Screen.Home }
                        )
                    }
                }
            }
        }
    }
}

sealed class Screen(val title: String) {
    data object Home : Screen("Examples")
    data object BasicAutoIndex : Screen("Basic (Auto Index)")
    data object ExplicitIndex : Screen("Explicit Index Ordering")
    data object LazyListStagger : Screen("Lazy List - Staggered")
    data object LazyListCustom : Screen("Lazy List - Custom Animations")
    data object LazyListDynamic : Screen("Lazy List - Dynamic Items")
    data object NestedAnimations : Screen("Nested Animations")
    data object ControlledAnimation : Screen("Manual Control")
}

private val accentColor = Color(0xFFC3B1E1)
private val backgroundColor = Color(0xFFC1E1C1)

@Composable
fun HomeScreen(onNavigate: (Screen) -> Unit) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = backgroundColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "🎉 AnimationSequenceHost",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Tap an example to see it in action",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(8.dp))

            ExampleButton("📦 Basic (Auto Index)", "Items animate in composition order") {
                onNavigate(Screen.BasicAutoIndex)
            }

            ExampleButton("🔢 Explicit Index Ordering", "Mix auto and explicit indices") {
                onNavigate(Screen.ExplicitIndex)
            }

            ExampleButton("📜 Lazy List - Staggered", "Beautiful cascading entrance") {
                onNavigate(Screen.LazyListStagger)
            }

            ExampleButton("🎨 Lazy List - Custom", "Per-item custom animations") {
                onNavigate(Screen.LazyListCustom)
            }

            ExampleButton("➕ Lazy List - Dynamic", "Add and remove items individually") {
                onNavigate(Screen.LazyListDynamic)
            }

            ExampleButton("🌳 Nested Animations", "Hierarchical animation hosts") {
                onNavigate(Screen.NestedAnimations)
            }

            ExampleButton("🎮 Manual Control", "Control animations with buttons") {
                onNavigate(Screen.ControlledAnimation)
            }
        }
    }
}

@Composable
fun ExampleButton(title: String, description: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, fontWeight = FontWeight.SemiBold)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExampleScreen(screen: Screen, onBack: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(screen.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = accentColor)
            )
        },
        containerColor = backgroundColor
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (screen) {
                Screen.BasicAutoIndex -> BasicAutoIndexExample()
                Screen.ExplicitIndex -> ExplicitIndexExample()
                Screen.LazyListStagger -> LazyListStaggerExample()
                Screen.LazyListCustom -> LazyListCustomExample()
                Screen.LazyListDynamic -> LazyListDynamicExample()
                Screen.NestedAnimations -> NestedAnimationsExample()
                Screen.ControlledAnimation -> ControlledAnimationExample()
                Screen.Home -> {} // Not used here
            }
        }
    }
}

// =============================================================================
// Example 1: Basic Auto Index
// =============================================================================

@Composable
fun BasicAutoIndexExample() {
    AnimationSequenceHost(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AnimatedItem {
                ExampleCard("First Item", "Animates 1st (auto slot 0)")
            }
            AnimatedItem {
                ExampleCard("Second Item", "Animates 2nd (auto slot 1)")
            }
            AnimatedItem {
                ExampleCard("Third Item", "Animates 3rd (auto slot 2)")
            }
            AnimatedItem(
                enter = scaleIn() + fadeIn(),
                exit = scaleOut()
            ) {
                ExampleCard("Fourth Item", "Custom scale animation")
            }
        }
    }
}

// =============================================================================
// Example 2: Explicit Index Ordering
// =============================================================================

@Composable
fun ExplicitIndexExample() {
    AnimationSequenceHost(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AnimatedItem {
                ExampleCard("A", "Auto → slot 0 (animates 1st)")
            }
            AnimatedItem(index = 5) {
                ExampleCard("B", "Explicit index=5 (animates last)")
            }
            AnimatedItem {
                ExampleCard("C", "Auto → slot 1 (animates 2nd)")
            }
            AnimatedItem(index = 2) {
                ExampleCard("D", "Explicit index=2 (animates 3rd)")
            }
            AnimatedItem {
                ExampleCard("E", "Auto → slot 3 (animates 4th)")
            }
        }
    }
}

// =============================================================================
// Example 3: Lazy List with Stagger
// =============================================================================

@Composable
fun LazyListStaggerExample() {
    val items = remember { (1..30).map { "Item #$it" } }

    // LazyAnimationHost provides scoped state - items animate once with stagger
    LazyAnimationHost { scope ->
        Column(modifier = Modifier.fillMaxSize()) {
            // Replay button
            Button(
                onClick = { scope.reset() },  // Reset to replay animations!
                colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("🔄 Replay Animations")
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                animatedItems(items, key = { it }) { item ->
                    ExampleCard(item, "Staggered fade-in animation")
                }
            }
        }
    }
}

// =============================================================================
// Example 4: Lazy List with Custom Animations
// =============================================================================

@Composable
fun LazyListCustomExample() {
    val items = remember { (1..30).map { it to "Item #$it" } }

    LazyAnimationHost(staggerDelayMillis = 80) { scope ->
        Column(modifier = Modifier.fillMaxSize()) {
            // Replay button
            Button(
                onClick = { scope.reset() },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("🔄 Replay Animations")
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                animatedItems(
                    items = items,
                    key = { it.first },
                    enter = { _, item, staggerDelay ->
                        when {
                            // Every 5th item scales in
                            item.first % 5 == 0 -> scaleIn(tween(400, delayMillis = staggerDelay)) +
                                    fadeIn(tween(400, delayMillis = staggerDelay))
                            // Odd items slide from left
                            item.first % 2 == 1 -> slideInHorizontally(tween(300, delayMillis = staggerDelay)) { -it } +
                                    fadeIn(tween(300, delayMillis = staggerDelay))
                            // Even items slide from right
                            else -> slideInHorizontally(tween(300, delayMillis = staggerDelay)) { it } +
                                    fadeIn(tween(300, delayMillis = staggerDelay))
                        }
                    },
                    exit = { _, _ -> fadeOut() }
                ) { item ->
                    val description = when {
                        item.first % 5 == 0 -> "Scale animation"
                        item.first % 2 == 1 -> "Slide from left"
                        else -> "Slide from right"
                    }
                    ExampleCard(item.second, description)
                }
            }
        }
    }
}

// =============================================================================
// Example 5: Lazy List with Dynamic Items
// =============================================================================

@Composable
fun LazyListDynamicExample() {
    var items by remember { mutableStateOf(listOf(1, 2, 3)) }
    var nextId by remember { mutableIntStateOf(4) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Control buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    items = items + nextId
                    nextId++
                },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black),
                modifier = Modifier.weight(1f)
            ) {
                Text("➕ Add Item")
            }
            Button(
                onClick = {
                    if (items.isNotEmpty()) {
                        items = items.dropLast(1)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black),
                modifier = Modifier.weight(1f),
                enabled = items.isNotEmpty()
            ) {
                Text("➖ Remove Last")
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    // Add item at the beginning
                    items = listOf(nextId) + items
                    nextId++
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                modifier = Modifier.weight(1f)
            ) {
                Text("Add First")
            }
            Button(
                onClick = {
                    if (items.isNotEmpty()) {
                        items = items.drop(1)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                modifier = Modifier.weight(1f),
                enabled = items.isNotEmpty()
            ) {
                Text("Remove First")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Items: ${items.size}",
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Use LazyAnimationHost with scope.trackItems for proper enter/exit animations!
        LazyAnimationHost { scope ->
            val animatedList = scope.trackItems(items, key = { it })

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                animatedItems(
                    items = animatedList,
                    enter = { _, _, _ -> slideInHorizontally { it } + fadeIn() },
                    exit = { _, _ -> slideOutHorizontally { -it } + fadeOut() }
                ) { animatedItem ->
                    DynamicItemCard(
                        id = animatedItem.data,
                        onRemove = { items = items.filter { it != animatedItem.data } }
                    )
                }
            }
        }
    }
}

@Composable
fun DynamicItemCard(id: Int, onRemove: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Item #$id", fontWeight = FontWeight.SemiBold)
                Text(
                    text = "Tap X to remove individually",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            IconButton(onClick = onRemove) {
                Text("✕", style = MaterialTheme.typography.titleLarge, color = Color.Red)
            }
        }
    }
}

// =============================================================================
// Example 6: Nested Animations
// =============================================================================

@Composable
fun NestedAnimationsExample() {
    AnimationSequenceHost(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AnimatedItem {
                ExampleCard("Parent Item 1", "Outer host")
            }

            AnimatedItem {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = accentColor)
                ) {
                    AnimationSequenceHost(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AnimatedItem(
                                enter = slideInHorizontally { -it } + fadeIn()
                            ) {
                                Text("🐣 Nested Child 1", fontWeight = FontWeight.Medium)
                            }
                            AnimatedItem(
                                enter = slideInHorizontally { -it } + fadeIn()
                            ) {
                                Text("🐥 Nested Child 2", fontWeight = FontWeight.Medium)
                            }
                            AnimatedItem(
                                enter = slideInHorizontally { -it } + fadeIn()
                            ) {
                                Text("🐔 Nested Child 3", fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }

            AnimatedItem {
                ExampleCard("Parent Item 2", "Continues after nested completes")
            }
        }
    }
}

// =============================================================================
// Example 7: Manual Control
// =============================================================================

@Composable
fun ControlledAnimationExample() {
    val coroutineScope = rememberCoroutineScope()

    AnimationSequenceHost(
        modifier = Modifier.fillMaxSize(),
        startByDefault = false
    ) { scope ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { coroutineScope.launch { scope.enter() } },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("▶ Enter All")
                }
                Button(
                    onClick = { coroutineScope.launch { scope.exit() } },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("◀ Exit All")
                }
            }

            AnimatedItem(
                enter = fadeIn(tween(500)) + scaleIn(tween(500)),
                exit = fadeOut(tween(300)) + scaleOut(tween(300)),
                delayAfterAnimation = 300
            ) {
                SubcomposeAsyncImage(
                    modifier = Modifier
                        .requiredHeight(200.dp)
                        .fillMaxWidth(),
                    model = ImageRequest.Builder(LocalContext.current)
                        .data("https://www.pokemon.com/static-assets/content-assets/cms2/img/pokedex/full/001.png")
                        .build(),
                    loading = { 
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.requiredSize(32.dp))
                        }
                    },
                    contentDescription = "Bulbasaur",
                )
            }

            AnimatedItem(
                enter = fadeIn(tween(400)) + slideInVertically { it },
                exit = fadeOut(),
                delayAfterAnimation = 200
            ) {
                Text(
                    "Bulbasaur",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            AnimatedItem(
                enter = fadeIn(tween(400)),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = accentColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "For some time after its birth, it uses the nutrients that are packed into the seed on its back in order to grow.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AccountCircle, contentDescription = null)
                            Text(" Trainer: Ash Ketchum", fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// Shared Components
// =============================================================================

@Composable
fun ExampleCard(title: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, fontWeight = FontWeight.SemiBold)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}
