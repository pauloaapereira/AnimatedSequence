/**
 * AnimationSequenceHost - A Jetpack Compose library for sequential animations.
 *
 * This library provides two main composables:
 *
 * 1. **[AnimationSequenceHost]** with **[AnimatedItem]** - For static layouts where items
 *    animate in a defined sequence with customizable transitions and delays.
 *
 * 2. **[LazyAnimationHost]** with **[animatedItems]** - For lazy lists (LazyColumn, LazyRow,
 *    LazyGrid) with staggered entrance animations and proper handling of items scrolling
 *    in and out of view.
 *
 * ## Quick Start
 *
 * ```kotlin
 * // Static content
 * AnimationSequenceHost {
 *     AnimatedItem { Text("First") }
 *     AnimatedItem { Text("Second") }
 * }
 *
 * // Lazy lists
 * LazyAnimationHost {
 *     LazyColumn {
 *         animatedItems(items, key = { it.id }) { item ->
 *             ItemCard(item)
 *         }
 *     }
 * }
 * ```
 *
 * @see AnimationSequenceHost
 * @see AnimatedItem
 * @see LazyAnimationHost
 * @see animatedItems
 */
package com.pp.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed as listItemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.currentCompositeKeyHash
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

// Configuration constants moved to a single place
private object AnimationDefaults {
    const val DEFAULT_DURATION = 300L
    const val DEFAULT_DELAY = 400L
}

// Composition local to provide the animation host
private val LocalSequentialAnimationHost =
    staticCompositionLocalOf<SequentialAnimationHost?> { null }

/**
 * Data class to represent an animation item in the sequence.
 * Uses a stable key for identity and registration order for sequencing.
 */
private data class AnimationItem(
    val key: Int,                    // Stable key (from explicit key, currentCompositeKeyHash, or derived)
    val registrationOrder: Long,     // Order in which item was first registered (for sequencing)
    val explicitIndex: Int?,         // Optional explicit index for slot reservation
    val animateOnce: Boolean,        // If true, item only animates the first time it appears
    val visibilityState: MutableTransitionState<Boolean>,
    val enterTransition: EnterTransition,
    val exitTransition: ExitTransition,
    val delay: Long,                 // Custom delay in milliseconds
    val enterDuration: Long,         // Pre-calculated enter duration
    val exitDuration: Long           // Pre-calculated exit duration
)

/**
 * Interface defining the operations that can be performed on a sequence of animations.
 *
 * This scope is provided by [AnimationSequenceHost] and allows programmatic control
 * over the animation sequence, including triggering enter/exit animations manually.
 */
interface SequentialAnimationScope {
    /**
     * Plays the enter animation for all items in the sequence, respecting their order.
     * Items with explicit indices animate in slot order; auto-indexed items fill remaining slots.
     */
    suspend fun enter()
    
    /**
     * Plays the exit animation for all items.
     * @param all If true, all items exit simultaneously. If false, items exit sequentially in reverse order.
     */
    suspend fun exit(all: Boolean = false)
    
    /**
     * Plays the enter animation for a specific item.
     * @param key The hash key of the item (from `key?.hashCode()` or `currentCompositeKeyHash`)
     * @return true if the item was found and animated, false otherwise
     */
    suspend fun enter(key: Int): Boolean
    
    /**
     * Plays the exit animation for a specific item.
     * @param key The hash key of the item
     * @return true if the item was found and animated, false otherwise
     */
    suspend fun exit(key: Int): Boolean
    
    /**
     * Checks if an animation sequence is currently in progress.
     * @return true if animations are running, false otherwise
     */
    fun isAnimating(): Boolean
}

/** Class that manages a sequence of animations with custom delays between them */
private class SequentialAnimationHost : SequentialAnimationScope {
    // Using ConcurrentHashMap for thread safety, keyed by stable composite key
    private val items = ConcurrentHashMap<Int, AnimationItem>()
    private var isAnimationInProgress = AtomicBoolean(false)
    
    // Counter for registration order - provides stable ordering based on first registration
    private val registrationCounter = AtomicLong(0)
    
    // Track items that have already been shown (for animateOnce support in lazy lists)
    // This persists even after items leave composition
    private val shownItems = ConcurrentHashMap.newKeySet<Int>()

    private var parent: SequentialAnimationHost? = null
    private val children = mutableListOf<SequentialAnimationHost>()

    /**
     * Registers a child with the host
     *
     * @param child Child to add to this host
     */
    fun addChild(child: SequentialAnimationHost) {
        children.add(child)
        child.parent = this
    }

    /**
     * Removes a child from the host
     *
     * @param child Child to remove from this host
     */
    fun removeChild(child: SequentialAnimationHost) {
        children.remove(child)
        child.parent = null
    }

    /**
     * Registers an animation item with the host using a stable key.
     *
     * @param key Stable key for identity (from explicit key param or currentCompositeKeyHash)
     * @param explicitIndex Optional explicit index for ordering (overrides registration order)
     * @param animateOnce If true, item only animates on first appearance (for lazy lists)
     * @param enterTransition Animation to play when entering
     * @param exitTransition Animation to play when exiting
     * @param delay Time to wait after this animation before starting the next one
     * @return MutableTransitionState that controls the visibility of this item
     */
    fun registerItem(
        key: Int,
        explicitIndex: Int?,
        animateOnce: Boolean,
        enterTransition: EnterTransition,
        exitTransition: ExitTransition,
        delay: Long
    ): MutableTransitionState<Boolean> {
        // Check if this key is already registered (item still in composition)
        val existingItem = items[key]
        if (existingItem != null) {
            // Item already exists - return existing state (preserves registration order)
            return existingItem.visibilityState
        }

        // Check if this item was previously shown (for animateOnce support)
        val wasAlreadyShown = key in shownItems
        
        // Create visibility state - if animateOnce and already shown, start visible
        val visibilityState = MutableTransitionState(wasAlreadyShown && animateOnce)
        
        // Create a new item with current registration order
        val newItem = AnimationItem(
            key = key,
            registrationOrder = registrationCounter.getAndIncrement(),
            explicitIndex = explicitIndex,
            animateOnce = animateOnce,
            visibilityState = visibilityState,
            enterTransition = enterTransition,
            exitTransition = exitTransition,
            delay = delay,
            enterDuration = calculateTransitionDuration(enterTransition),
            exitDuration = calculateTransitionDuration(exitTransition)
        )
        items[key] = newItem
        
        // If animateOnce and not already shown, trigger immediate animation
        // This makes items animate as they appear (for lazy lists)
        if (animateOnce && !wasAlreadyShown) {
            visibilityState.targetState = true
            shownItems.add(key)
        }
        
        return visibilityState
    }

    /**
     * Unregisters an animation item
     *
     * @param key The key of the item to unregister
     */
    fun unregisterItem(key: Int) {
        items.remove(key)
    }

    /** Clears all items from the host */
    fun clearItems() {
        items.clear()
        shownItems.clear()
        registrationCounter.set(0)
    }

    /**
     * Returns items sorted with true interleaving:
     * - Explicit indices reserve specific slots (0, 1, 5, etc.)
     * - Auto items fill the remaining slots in registration order
     *
     * Example: AnimatedItem(auto), AnimatedItem(index=5), AnimatedItem(auto)
     * Result: auto1→slot0, auto2→slot1, explicit→slot5
     * Order: slot0, slot1, slot5 = auto1, auto2, explicit
     */
    private fun getSortedItems(): List<AnimationItem> {
        val explicitItems = items.values.filter { it.explicitIndex != null }
        val autoItems = items.values
            .filter { it.explicitIndex == null }
            .sortedBy { it.registrationOrder }

        // Get all slots claimed by explicit indices
        val claimedSlots = explicitItems.mapNotNull { it.explicitIndex }.toSet()

        // Assign auto items to available slots (skip claimed ones)
        var nextSlot = 0
        val autoItemsWithSlots = autoItems.map { item ->
            while (nextSlot in claimedSlots) {
                nextSlot++
            }
            val slot = nextSlot
            nextSlot++
            item to slot
        }

        // Combine explicit items (with their index as slot) and auto items (with assigned slots)
        val explicitWithSlots = explicitItems.map { it to it.explicitIndex!! }

        return (explicitWithSlots + autoItemsWithSlots)
            .sortedBy { it.second }
            .map { it.first }
    }

    /** Plays the enter animation for all items in sequence based on interleaved order */
    override suspend fun enter() {
        // Atomically check and set - if compareAndSet fails, another animation is in progress
        if (!isAnimationInProgress.compareAndSet(false, true)) return
        try {

            // Calculate the maximum exit transition duration
            val maxExitDuration = items.values.maxOfOrNull { it.exitDuration } ?: 0L

            // Set all items to hidden state
            items.values.forEach { it.visibilityState.targetState = false }

            // Wait for all exit animations to complete
            delay(maxExitDuration)

            // Get items sorted with true interleaving
            val sortedItems = getSortedItems()

            // Start enter animations in sequence with custom delays
            for (item in sortedItems) {
                try {
                    item.visibilityState.targetState = true
                    delay(item.delay)
                } catch (_: CancellationException) {
                    throw CancellationException()
                } catch (_: Exception) {
                    // Continue with next animation on error
                }
            }
        } catch (_: CancellationException) {
            // Animation was cancelled - clean exit
        } catch (_: Exception) {
            // Unexpected error - animation sequence interrupted
        } finally {
            isAnimationInProgress.set(false)
        }
    }

    /** Plays the exit animation for all items in reverse interleaved order */
    override suspend fun exit(all: Boolean) {
        // Atomically check and set - if compareAndSet fails, another animation is in progress
        if (!isAnimationInProgress.compareAndSet(false, true)) return
        try {

            for (child in children) {
                child.exit(all)
            }

            if (all) {
                items.values.forEach { it.visibilityState.targetState = false }
                isAnimationInProgress.set(false)
                return
            }

            // Get items sorted with true interleaving, then reverse for exit
            val sortedItems = getSortedItems().reversed()

            // Start exit animations in sequence with custom delays
            for (item in sortedItems) {
                try {
                    item.visibilityState.targetState = false
                    delay(item.delay)
                } catch (_: CancellationException) {
                    throw CancellationException()
                } catch (_: Exception) {
                    // Continue with next animation on error
                }
            }
            items.values.forEach { it.visibilityState.targetState = false }
        } catch (_: CancellationException) {
            // Animation was cancelled - clean exit
        } catch (_: Exception) {
            // Unexpected error - animation sequence interrupted
        } finally {
            isAnimationInProgress.set(false)
        }
    }

    /**
     * Plays the enter animation for a specific item by key
     *
     * @param key The key of the item to animate
     * @return true if the item was found and animated, false otherwise
     */
    override suspend fun enter(key: Int): Boolean {
        val matchingItem = items[key] ?: return false

        try {
            // Set item to hidden state
            matchingItem.visibilityState.targetState = false

            // Wait for exit animation to complete
            delay(matchingItem.exitDuration)

            // Start enter animation
            matchingItem.visibilityState.targetState = true
            return true
        } catch (_: Exception) {
            return false
        }
    }

    /**
     * Plays the exit animation for a specific item by key
     *
     * @param key The key of the item to animate
     * @return true if the item was found and animated, false otherwise
     */
    override suspend fun exit(key: Int): Boolean {
        val matchingItem = items[key] ?: return false
        matchingItem.visibilityState.targetState = false
        return true
    }

    /**
     * Checks if animation is currently in progress
     *
     * @return true if an animation is running, false otherwise
     */
    override fun isAnimating(): Boolean {
        return isAnimationInProgress.get()
    }

    /**
     * Calculates the duration of a transition. Uses a safer approach than reflection.
     *
     * @param transition The transition to analyze
     * @return The duration in milliseconds
     */
    private fun calculateTransitionDuration(transition: Any): Long {
        // First try to extract duration directly from TweenSpec if possible
        if (transition is EnterTransition) {
            val tweenField =
                transition.javaClass.declaredFields.find {
                    it.name.contains("spec", ignoreCase = true) ||
                            it.name.contains("tween", ignoreCase = true)
                }

            if (tweenField != null) {
                try {
                    tweenField.isAccessible = true
                    val spec = tweenField.get(transition)
                    if (spec is TweenSpec<*>) {
                        return spec.durationMillis.toLong()
                    }
                } catch (e: Exception) {
                    // Fall back to default if reflection fails
                }
            }
        }

        // For more complex transitions, use a conservative estimate
        // Most Compose animations default to around 300ms
        return AnimationDefaults.DEFAULT_DURATION
    }
}

/** Composable function that creates and manages a sequential animation host */
@Composable
fun AnimationSequenceHost(
    modifier: Modifier = Modifier,
    startByDefault: Boolean = true,
    content: @Composable BoxScope.(scope: SequentialAnimationScope) -> Unit
) {
    val parent = LocalSequentialAnimationHost.current

    // Create and remember the animation host
    val host = remember { SequentialAnimationHost() }

    // Register/unregister with parent in DisposableEffect to avoid duplicate registrations
    DisposableEffect(host) {
        parent?.addChild(host)
        onDispose {
            parent?.removeChild(host)
            host.clearItems()
        }
    }

    // Start the animation sequence automatically if startByDefault is true
    if (startByDefault) {
        LaunchedEffect(Unit) {
            try {
                host.enter()
            } catch (_: CancellationException) {
                // Normal cancellation when composable leaves
            } catch (_: Exception) {
                // Animation error - composable may have left composition
            }
        }
    }

    // Provide the host to child composables
    CompositionLocalProvider(LocalSequentialAnimationHost provides host) {
        Box(modifier = modifier) { content(host) }
    }
}

/**
 * Composable for individual animated items that participate in a sequence.
 *
 * ## Ordering Behavior (True Interleaving)
 *
 * Items with explicit indices reserve specific "slots" in the animation sequence.
 * Items without indices (auto) fill the remaining slots in the order they were first composed.
 *
 * **Example:**
 * ```
 * AnimatedItem { Text("A") }           // auto - gets slot 0
 * AnimatedItem(index = 5) { Text("B") } // explicit - reserves slot 5
 * AnimatedItem { Text("C") }           // auto - gets slot 1 (next available)
 * ```
 * **Animation order:** A (slot 0) → C (slot 1) → B (slot 5)
 *
 * ## Lazy List Support
 *
 * For lazy lists (LazyColumn, LazyRow, etc.), use the `key` and `animateOnce` parameters:
 *
 * ```
 * LazyColumn {
 *     items(list, key = { it.id }) { item ->
 *         AnimatedItem(
 *             key = item.id,        // Stable identity across scroll
 *             index = list.indexOf(item), // Use list position for ordering
 *             animateOnce = true    // Don't re-animate when scrolling back
 *         ) {
 *             ItemContent(item)
 *         }
 *     }
 * }
 * ```
 *
 * @param modifier Modifier to apply to the animated container
 * @param key Optional stable key for identity. Required for lazy lists where
 *            multiple items share the same call site. If null, uses currentCompositeKeyHash.
 * @param index Optional explicit slot for animation order. If null, the item fills
 *              the next available slot based on composition order.
 * @param animateOnce If true, item only animates on first appearance and stays visible
 *                    when scrolled out and back in. Ideal for lazy lists.
 * @param delayAfterAnimation Delay in milliseconds before the next item starts animating.
 *                            For animateOnce mode, this is ignored (items animate immediately).
 * @param enter The enter transition animation
 * @param exit The exit transition animation
 * @param content The content to animate
 */
@Composable
fun AnimatedItem(
    modifier: Modifier = Modifier,
    key: Any? = null,
    index: Int? = null,
    animateOnce: Boolean = false,
    delayAfterAnimation: Long = AnimationDefaults.DEFAULT_DELAY,
    enter: EnterTransition = fadeIn(tween(AnimationDefaults.DEFAULT_DURATION.toInt())),
    exit: ExitTransition = fadeOut(tween(AnimationDefaults.DEFAULT_DURATION.toInt())),
    content: @Composable () -> Unit
) {
    // Get the animation host from the composition local
    val host =
        LocalSequentialAnimationHost.current
            ?: error("AnimatedItem must be used within an AnimationSequenceHost")

    // Determine stable key:
    // - If explicit key provided, use its hashCode (for lazy lists)
    // - Otherwise, use Compose's currentCompositeKeyHash (unique per call site)
    val stableKey = key?.hashCode() ?: currentCompositeKeyHash

    // Register with stable key - registration order is captured on first registration
    // and preserved across recompositions
    val visibilityState = remember(stableKey) {
        host.registerItem(
            key = stableKey,
            explicitIndex = index,
            animateOnce = animateOnce,
            enterTransition = enter,
            exitTransition = exit,
            delay = delayAfterAnimation
        )
    }

    // Clean up when the composable leaves the composition
    DisposableEffect(stableKey) {
        onDispose { host.unregisterItem(stableKey) }
    }

    // Use AnimatedVisibility with the state managed by the host
    AnimatedVisibility(
        modifier = modifier,
        visibleState = visibilityState,
        enter = enter,
        exit = exit
    ) {
        content()
    }
}

// =============================================================================
// LAZY ANIMATION HOST - Clean, Scoped API for Lazy List Animations
// =============================================================================

/**
 * How long after host creation to apply stagger (items appearing after this get no stagger).
 */
private const val STAGGER_WINDOW_MS = 1000L

/**
 * Wrapper for items managed by [LazyAnimationHost].
 * Contains the item data, unique key, and visibility state.
 *
 * @param T The type of the item data
 * @property data The actual item data
 * @property key Unique key for this item
 * @property visibilityState State controlling enter/exit animations
 */
class AnimatedLazyItem<T> internal constructor(
    val data: T,
    val key: Any,
    val visibilityState: MutableTransitionState<Boolean>,
    internal var isRemoving: Boolean = false
)

/**
 * Scope for managing lazy list animations. Tracks which items have been shown,
 * manages stagger timing, and handles dynamic item enter/exit animations.
 *
 * Created and provided by [LazyAnimationHost].
 */
class LazyAnimationScope internal constructor(
    /** Delay in ms between each item's staggered animation */
    val staggerDelayMillis: Int,
    /** Default enter transition for items */
    val defaultEnter: EnterTransition,
    /** Default exit transition for items */
    val defaultExit: ExitTransition
) {
    // Track which items have been shown (for animateOnce)
    internal val shownItems: MutableSet<Any> = java.util.Collections.synchronizedSet(mutableSetOf())
    
    // Track when this scope was created (for stagger timing)
    @Volatile
    internal var startTime: Long = System.currentTimeMillis()
    
    // Generation counter - incrementing this causes items to be treated as "new"
    // Using mutableStateOf so Compose observes changes and recomposes
    internal var generation by mutableStateOf(0)
    
    /**
     * Returns whether the given item key has already been animated.
     * Includes generation in the check so reset() causes re-animation.
     */
    fun wasShown(key: Any): Boolean = "$key-$generation" in shownItems
    
    /**
     * Marks an item as shown (will not re-animate when scrolled back).
     */
    internal fun markShown(key: Any) { shownItems.add("$key-$generation") }
    
    /**
     * Calculates stagger delay for an item at the given index.
     * Returns 0 if we're past the initial stagger window.
     */
    internal fun calculateStagger(index: Int): Int {
        val elapsed = System.currentTimeMillis() - startTime
        return if (elapsed < STAGGER_WINDOW_MS && staggerDelayMillis > 0) {
            index * staggerDelayMillis
        } else {
            0
        }
    }
    
    /**
     * Resets all tracking state, allowing animations to replay with stagger.
     * Call this when you want to replay entrance animations.
     */
    fun reset() {
        generation++  // Increment generation so all items appear "new"
        startTime = System.currentTimeMillis()
        // Note: We don't clear shownItems - the generation change handles it
    }
}

/**
 * CompositionLocal providing the current [LazyAnimationScope].
 */
val LocalLazyAnimationScope = staticCompositionLocalOf<LazyAnimationScope?> { null }

/**
 * Host composable for lazy list animations. All state is scoped to this host,
 * so navigating away and back will replay animations (no global state).
 *
 * ## Simple Usage
 * ```
 * LazyAnimationHost {
 *     LazyColumn {
 *         animatedItems(items, key = { it.id }) { item ->
 *             ItemCard(item)
 *         }
 *     }
 * }
 * ```
 *
 * ## With Custom Defaults
 * ```
 * LazyAnimationHost(
 *     staggerDelayMillis = 100,
 *     defaultEnter = fadeIn() + slideInVertically(),
 *     defaultExit = fadeOut()
 * ) {
 *     LazyColumn {
 *         animatedItems(items, key = { it.id }) { item ->
 *             ItemCard(item)
 *         }
 *     }
 * }
 * ```
 *
 * ## Dynamic Lists with Exit Animations
 * For lists where items are added/removed and you need exit animations:
 * ```
 * LazyAnimationHost { scope ->
 *     val animatedList = scope.trackItems(items, key = { it.id })
 *
 *     LazyColumn {
 *         animatedItems(animatedList) { item ->
 *             ItemCard(item.data)
 *         }
 *     }
 * }
 * ```
 *
 * @param staggerDelayMillis Delay between each item's animation on initial load. Default 50ms.
 * @param defaultEnter Default enter transition. Default is fadeIn(300ms).
 * @param defaultExit Default exit transition. Default is fadeOut(300ms).
 * @param content The content, typically containing a LazyColumn/LazyRow/LazyGrid.
 */
@Composable
fun LazyAnimationHost(
    staggerDelayMillis: Int = 50,
    defaultEnter: EnterTransition = fadeIn(tween(300)),
    defaultExit: ExitTransition = fadeOut(tween(300)),
    content: @Composable (LazyAnimationScope) -> Unit
) {
    val scope = remember {
        LazyAnimationScope(
            staggerDelayMillis = staggerDelayMillis,
            defaultEnter = defaultEnter,
            defaultExit = defaultExit
        )
    }
    
    CompositionLocalProvider(LocalLazyAnimationScope provides scope) {
        content(scope)
    }
}

/**
 * Tracks items for dynamic add/remove with proper enter and exit animations.
 * Removed items stay in composition until their exit animation completes.
 *
 * **Usage:**
 * ```
 * LazyAnimationHost { scope ->
 *     var items by remember { mutableStateOf(listOf(1, 2, 3)) }
 *     val animatedList = scope.trackItems(items, key = { it })
 *
 *     LazyColumn {
 *         animatedItems(animatedList) { item ->
 *             ItemCard(item.data)
 *         }
 *     }
 * }
 * ```
 *
 * @param items The source list of items
 * @param key Function to extract a unique key for each item
 * @return List of [AnimatedLazyItem] to pass to animatedItems
 */
@Composable
fun <T> LazyAnimationScope.trackItems(
    items: List<T>,
    key: (T) -> Any
): List<AnimatedLazyItem<T>> {
    var animatedItems by remember { mutableStateOf<List<AnimatedLazyItem<T>>>(emptyList()) }
    val currentKeys = remember(items) { items.map { key(it) }.toSet() }
    
    LaunchedEffect(items) {
        val existingByKey = animatedItems.associateBy { it.key }
        val removedKeys = existingByKey.keys - currentKeys
        
        // Create/update items for current list
        val activeItems = items.map { item ->
            val itemKey = key(item)
            existingByKey[itemKey]?.let { existing ->
                // Keep existing state
                AnimatedLazyItem(
                    data = item,
                    key = itemKey,
                    visibilityState = existing.visibilityState.apply { targetState = true },
                    isRemoving = false
                )
            } ?: AnimatedLazyItem(
                // New item - start invisible, animate in
                data = item,
                key = itemKey,
                visibilityState = MutableTransitionState(false).apply { targetState = true },
                isRemoving = false
            )
        }
        
        // Mark removed items for exit animation
        val removingItems = animatedItems
            .filter { it.key in removedKeys }
            .onEach { holder ->
                holder.visibilityState.targetState = false
                holder.isRemoving = true
            }
        
        animatedItems = activeItems + removingItems
    }
    
    // Clean up items when their exit animation completes (isIdle = animation finished)
    val removingItems = animatedItems.filter { it.isRemoving }
    val allRemovingIdle = removingItems.all { it.visibilityState.isIdle }
    
    LaunchedEffect(allRemovingIdle, removingItems.size) {
        if (removingItems.isNotEmpty() && allRemovingIdle) {
            animatedItems = animatedItems.filter { !it.isRemoving }
        }
    }
    
    return animatedItems
}

// =============================================================================
// LAZY LIST EXTENSION FUNCTIONS
// =============================================================================

/**
 * Adds animated items to a LazyColumn/LazyRow. Must be used inside a [LazyAnimationHost].
 *
 * ## Features
 * - Staggered entrance animations on initial load
 * - Items animate once and don't re-animate when scrolled back into view
 * - Custom enter/exit transitions per item
 * - For dynamic add/remove with exit animations, use [LazyAnimationScope.trackItems]
 *
 * ## Usage
 * ```
 * LazyAnimationHost {
 *     LazyColumn {
 *         animatedItems(items, key = { it.id }) { item ->
 *             ItemCard(item)
 *         }
 *     }
 * }
 * ```
 *
 * ## Custom Animations
 * ```
 * LazyAnimationHost {
 *     LazyColumn {
 *         animatedItems(
 *             items = items,
 *             key = { it.id },
 *             enter = { _, _, delay -> fadeIn(tween(300, delayMillis = delay)) + scaleIn() },
 *             exit = { _, _ -> fadeOut() + shrinkOut() }
 *         ) { item ->
 *             ItemCard(item)
 *         }
 *     }
 * }
 * ```
 *
 * @param items The list of items to display
 * @param key Function returning a stable, unique key for each item
 * @param enter Custom enter transition. Receives (index, item, staggerDelay).
 * @param exit Custom exit transition. Receives (index, item).
 * @param contentType Content type factory for recycling performance
 * @param itemContent The composable content for each item
 */
fun <T> LazyListScope.animatedItems(
    items: List<T>,
    key: (T) -> Any,
    enter: ((index: Int, item: T, staggerDelay: Int) -> EnterTransition)? = null,
    exit: ((index: Int, item: T) -> ExitTransition)? = null,
    contentType: (T) -> Any? = { null },
    itemContent: @Composable LazyItemScope.(T) -> Unit
) {
    listItemsIndexed(
        items = items,
        key = { _, item -> key(item) },
        contentType = { _, item -> contentType(item) }
    ) { index, item ->
        val scope = LocalLazyAnimationScope.current
            ?: error("animatedItems must be used inside LazyAnimationHost")
        
        val itemKey = key(item)
        val generation = scope.generation  // Read generation to trigger recomposition on reset()
        val alreadyShown = scope.wasShown(itemKey)
        val staggerDelay = if (!alreadyShown) scope.calculateStagger(index) else 0
        
        // Include generation in remember key so reset() creates new visibility states
        val visibilityState = remember(itemKey, generation) {
            MutableTransitionState(alreadyShown).apply {
                if (!alreadyShown) {
                    targetState = true
                    scope.markShown(itemKey)
                }
            }
        }
        
        // Create enter transition with stagger delay baked in
        // Note: For custom defaultEnter with stagger, use the `enter` lambda parameter instead
        val effectiveEnter = if (staggerDelay > 0) {
            fadeIn(tween(300, delayMillis = staggerDelay))
        } else {
            scope.defaultEnter
        }
        
        AnimatedVisibility(
            visibleState = visibilityState,
            enter = enter?.invoke(index, item, staggerDelay) ?: effectiveEnter,
            exit = exit?.invoke(index, item) ?: scope.defaultExit
        ) {
            itemContent(item)
        }
    }
}

/**
 * Adds animated items from [LazyAnimationScope.trackItems] with proper enter/exit animations.
 * Use this overload for dynamic lists where items are added and removed.
 *
 * ## Usage
 * ```
 * LazyAnimationHost { scope ->
 *     var items by remember { mutableStateOf(listOf(1, 2, 3)) }
 *     val animatedList = scope.trackItems(items, key = { it })
 *
 *     LazyColumn {
 *         animatedItems(animatedList) { item ->
 *             ItemCard(item.data)
 *         }
 *     }
 * }
 * ```
 *
 * @param items List from [LazyAnimationScope.trackItems]
 * @param enter Custom enter transition. Receives (index, item, staggerDelay).
 * @param exit Custom exit transition. Receives (index, item).
 * @param contentType Content type factory for recycling performance
 * @param itemContent The composable content for each item
 */
fun <T> LazyListScope.animatedItems(
    items: List<AnimatedLazyItem<T>>,
    enter: ((index: Int, item: T, staggerDelay: Int) -> EnterTransition)? = null,
    exit: ((index: Int, item: T) -> ExitTransition)? = null,
    contentType: (AnimatedLazyItem<T>) -> Any? = { null },
    itemContent: @Composable LazyItemScope.(AnimatedLazyItem<T>) -> Unit
) {
    listItemsIndexed(
        items = items,
        key = { _, animatedItem -> animatedItem.key },
        contentType = { _, animatedItem -> contentType(animatedItem) }
    ) { index, animatedItem ->
        val scope = LocalLazyAnimationScope.current
            ?: error("animatedItems must be used inside LazyAnimationHost")
        
        val staggerDelay = if (!animatedItem.isRemoving) scope.calculateStagger(index) else 0
        
        val effectiveEnter = if (staggerDelay > 0) {
            fadeIn(tween(300, delayMillis = staggerDelay))
        } else {
            scope.defaultEnter
        }
        
        AnimatedVisibility(
            visibleState = animatedItem.visibilityState,
            enter = enter?.invoke(index, animatedItem.data, staggerDelay) ?: effectiveEnter,
            exit = exit?.invoke(index, animatedItem.data) ?: scope.defaultExit
        ) {
            itemContent(animatedItem)
        }
    }
}

/**
 * Adds animated items with index to a LazyColumn/LazyRow. Must be used inside [LazyAnimationHost].
 *
 * Same as [animatedItems] but provides the index to the content lambda.
 */
fun <T> LazyListScope.animatedItemsIndexed(
    items: List<T>,
    key: (Int, T) -> Any,
    enter: ((index: Int, item: T, staggerDelay: Int) -> EnterTransition)? = null,
    exit: ((index: Int, item: T) -> ExitTransition)? = null,
    contentType: (Int, T) -> Any? = { _, _ -> null },
    itemContent: @Composable LazyItemScope.(Int, T) -> Unit
) {
    listItemsIndexed(
        items = items,
        key = key,
        contentType = contentType
    ) { index, item ->
        val scope = LocalLazyAnimationScope.current
            ?: error("animatedItemsIndexed must be used inside LazyAnimationHost")
        
        val itemKey = key(index, item)
        val generation = scope.generation
        val alreadyShown = scope.wasShown(itemKey)
        val staggerDelay = if (!alreadyShown) scope.calculateStagger(index) else 0
        
        val visibilityState = remember(itemKey, generation) {
            MutableTransitionState(alreadyShown).apply {
                if (!alreadyShown) {
                    targetState = true
                    scope.markShown(itemKey)
                }
            }
        }
        
        val effectiveEnter = if (staggerDelay > 0) {
            fadeIn(tween(300, delayMillis = staggerDelay))
        } else {
            scope.defaultEnter
        }
        
        AnimatedVisibility(
            visibleState = visibilityState,
            enter = enter?.invoke(index, item, staggerDelay) ?: effectiveEnter,
            exit = exit?.invoke(index, item) ?: scope.defaultExit
        ) {
            itemContent(index, item)
        }
    }
}

/**
 * Adds animated items to a LazyVerticalGrid/LazyHorizontalGrid. Must be used inside [LazyAnimationHost].
 *
 * Same features as [LazyListScope.animatedItems] but for grids.
 */
fun <T> LazyGridScope.animatedItems(
    items: List<T>,
    key: (T) -> Any,
    enter: ((index: Int, item: T, staggerDelay: Int) -> EnterTransition)? = null,
    exit: ((index: Int, item: T) -> ExitTransition)? = null,
    contentType: (T) -> Any? = { null },
    itemContent: @Composable LazyGridItemScope.(T) -> Unit
) {
    gridItemsIndexed(
        items = items,
        key = { _, item -> key(item) },
        contentType = { _, item -> contentType(item) }
    ) { index, item ->
        val scope = LocalLazyAnimationScope.current
            ?: error("animatedItems must be used inside LazyAnimationHost")
        
        val itemKey = key(item)
        val generation = scope.generation
        val alreadyShown = scope.wasShown(itemKey)
        val staggerDelay = if (!alreadyShown) scope.calculateStagger(index) else 0
        
        val visibilityState = remember(itemKey, generation) {
            MutableTransitionState(alreadyShown).apply {
                if (!alreadyShown) {
                    targetState = true
                    scope.markShown(itemKey)
                }
            }
        }
        
        val effectiveEnter = if (staggerDelay > 0) {
            fadeIn(tween(300, delayMillis = staggerDelay))
        } else {
            scope.defaultEnter
        }
        
        AnimatedVisibility(
            visibleState = visibilityState,
            enter = enter?.invoke(index, item, staggerDelay) ?: effectiveEnter,
            exit = exit?.invoke(index, item) ?: scope.defaultExit
        ) {
            itemContent(item)
        }
    }
}

/**
 * Adds animated items from [LazyAnimationScope.trackItems] to a grid.
 */
fun <T> LazyGridScope.animatedItems(
    items: List<AnimatedLazyItem<T>>,
    enter: ((index: Int, item: T, staggerDelay: Int) -> EnterTransition)? = null,
    exit: ((index: Int, item: T) -> ExitTransition)? = null,
    contentType: (AnimatedLazyItem<T>) -> Any? = { null },
    itemContent: @Composable LazyGridItemScope.(AnimatedLazyItem<T>) -> Unit
) {
    gridItemsIndexed(
        items = items,
        key = { _, animatedItem -> animatedItem.key },
        contentType = { _, animatedItem -> contentType(animatedItem) }
    ) { index, animatedItem ->
        val scope = LocalLazyAnimationScope.current
            ?: error("animatedItems must be used inside LazyAnimationHost")
        
        val staggerDelay = if (!animatedItem.isRemoving) scope.calculateStagger(index) else 0
        
        val effectiveEnter = if (staggerDelay > 0) {
            fadeIn(tween(300, delayMillis = staggerDelay))
        } else {
            scope.defaultEnter
        }
        
        AnimatedVisibility(
            visibleState = animatedItem.visibilityState,
            enter = enter?.invoke(index, animatedItem.data, staggerDelay) ?: effectiveEnter,
            exit = exit?.invoke(index, animatedItem.data) ?: scope.defaultExit
        ) {
            itemContent(animatedItem)
        }
    }
}

/**
 * Adds animated items with index to a grid. Must be used inside [LazyAnimationHost].
 */
fun <T> LazyGridScope.animatedItemsIndexed(
    items: List<T>,
    key: (Int, T) -> Any,
    enter: ((index: Int, item: T, staggerDelay: Int) -> EnterTransition)? = null,
    exit: ((index: Int, item: T) -> ExitTransition)? = null,
    contentType: (Int, T) -> Any? = { _, _ -> null },
    itemContent: @Composable LazyGridItemScope.(Int, T) -> Unit
) {
    gridItemsIndexed(
        items = items,
        key = key,
        contentType = contentType
    ) { index, item ->
        val scope = LocalLazyAnimationScope.current
            ?: error("animatedItemsIndexed must be used inside LazyAnimationHost")
        
        val itemKey = key(index, item)
        val generation = scope.generation
        val alreadyShown = scope.wasShown(itemKey)
        val staggerDelay = if (!alreadyShown) scope.calculateStagger(index) else 0
        
        val visibilityState = remember(itemKey, generation) {
            MutableTransitionState(alreadyShown).apply {
                if (!alreadyShown) {
                    targetState = true
                    scope.markShown(itemKey)
                }
            }
        }
        
        val effectiveEnter = if (staggerDelay > 0) {
            fadeIn(tween(300, delayMillis = staggerDelay))
        } else {
            scope.defaultEnter
        }
        
        AnimatedVisibility(
            visibleState = visibilityState,
            enter = enter?.invoke(index, item, staggerDelay) ?: effectiveEnter,
            exit = exit?.invoke(index, item) ?: scope.defaultExit
        ) {
            itemContent(index, item)
        }
    }
}
