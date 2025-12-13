package com.pp.sample

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pp.animatedsequence.AnimatedItem
import com.pp.animatedsequence.AnimationSequenceHost
import com.pp.animatedsequence.LazyAnimationHost
import com.pp.animatedsequence.animatedItems
import com.pp.animatedsequence.trackItems
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import animatedsequence.sample.generated.resources.Res
import animatedsequence.sample.generated.resources.bulbasaur

private val accentColor = Color(0xFFC3B1E1)
private val backgroundColor = Color(0xFFC1E1C1)

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

@Composable
@Preview
fun App() {
    MaterialTheme {
        var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
        var navigationKey by remember { mutableIntStateOf(0) }

        when (val screen = currentScreen) {
            Screen.Home -> HomeScreen(onNavigate = {
                navigationKey++
                currentScreen = it
            })
            else -> key(navigationKey) {
                ExampleScreen(
                    screen = screen,
                    onBack = { currentScreen = Screen.Home }
                )
            }
        }
    }
}

@Composable
fun HomeScreen(onNavigate: (Screen) -> Unit) {
    val safeInsets = WindowInsets.safeDrawing.asPaddingValues()
    
    Scaffold(
        backgroundColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = { Text("🎉 AnimationSequence") },
                backgroundColor = accentColor,
                modifier = Modifier.padding(top = safeInsets.calculateTopPadding())
            )
        }
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
                text = "Tap an example to see it in action",
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        backgroundColor = Color.White,
        shape = RoundedCornerShape(12.dp),
        elevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, fontWeight = FontWeight.SemiBold)
            Text(text = description, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun ExampleScreen(screen: Screen, onBack: () -> Unit) {
    val safeInsets = WindowInsets.safeDrawing.asPaddingValues()
    
    Scaffold(
        backgroundColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = { Text(screen.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                backgroundColor = accentColor,
                modifier = Modifier.padding(top = safeInsets.calculateTopPadding())
            )
        }
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
                Screen.Home -> {}
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

    LazyAnimationHost { scope ->
        Column(modifier = Modifier.fillMaxSize()) {
            Button(
                onClick = { scope.reset() },
                colors = ButtonDefaults.buttonColors(backgroundColor = accentColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("🔄 Replay Animations", color = Color.Black)
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
            Button(
                onClick = { scope.reset() },
                colors = ButtonDefaults.buttonColors(backgroundColor = accentColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("🔄 Replay Animations", color = Color.Black)
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
                            item.first % 5 == 0 -> scaleIn(tween(400, delayMillis = staggerDelay)) +
                                    fadeIn(tween(400, delayMillis = staggerDelay))
                            item.first % 2 == 1 -> slideInHorizontally(tween(300, delayMillis = staggerDelay)) { -it } +
                                    fadeIn(tween(300, delayMillis = staggerDelay))
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
                colors = ButtonDefaults.buttonColors(backgroundColor = accentColor),
                modifier = Modifier.weight(1f)
            ) {
                Text("➕ Add", color = Color.Black)
            }
            Button(
                onClick = {
                    if (items.isNotEmpty()) items = items.dropLast(1)
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = accentColor),
                modifier = Modifier.weight(1f),
                enabled = items.isNotEmpty()
            ) {
                Text("➖ Remove", color = Color.Black)
            }
        }

        Text(
            text = "Items: ${items.size}",
            modifier = Modifier.padding(horizontal = 16.dp),
            fontSize = 12.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(8.dp))

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
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = Color.White,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Item #${animatedItem.data}", fontWeight = FontWeight.SemiBold)
                            Text(
                                "✕",
                                color = Color.Red,
                                fontSize = 20.sp,
                                modifier = Modifier.clickable {
                                    items = items.filter { it != animatedItem.data }
                                }
                            )
                        }
                    }
                }
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
                    backgroundColor = accentColor,
                    shape = RoundedCornerShape(12.dp)
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
                    colors = ButtonDefaults.buttonColors(backgroundColor = accentColor),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("▶ Enter All", color = Color.Black)
                }
                Button(
                    onClick = { coroutineScope.launch { scope.exit() } },
                    colors = ButtonDefaults.buttonColors(backgroundColor = accentColor),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("◀ Exit All", color = Color.Black)
                }
            }

            AnimatedItem(
                enter = fadeIn(tween(500)) + scaleIn(tween(500)),
                exit = fadeOut(tween(300)) + scaleOut(tween(300)),
                delayAfterAnimation = 300
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color.White,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(Color(0xFF78C850)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(Res.drawable.bulbasaur),
                            contentDescription = "Bulbasaur",
                            modifier = Modifier.height(150.dp)
                        )
                    }
                }
            }

            AnimatedItem(
                enter = fadeIn(tween(400)),
                exit = fadeOut(),
                delayAfterAnimation = 200
            ) {
                Text(
                    "Bulbasaur",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            AnimatedItem(
                enter = fadeIn(tween(400)),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = accentColor,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "For some time after its birth, it uses the nutrients that are packed into the seed on its back in order to grow."
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("👤 Trainer: Ash Ketchum", fontWeight = FontWeight.Medium)
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
        backgroundColor = Color.White,
        shape = RoundedCornerShape(12.dp),
        elevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, fontWeight = FontWeight.SemiBold)
            Text(text = description, fontSize = 12.sp, color = Color.Gray)
        }
    }
}
