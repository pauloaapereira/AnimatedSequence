# 🎉 AnimationSequence

[![](https://jitpack.io/v/pauloaapereira/AnimatedSequence.svg)](https://jitpack.io/#pauloaapereira/AnimatedSequence)

**AnimationSequence** is a powerful yet intuitive Jetpack Compose library designed to simplify sequential animations in your Android apps. Easily orchestrate elegant, responsive, and hierarchical animations with minimal effort, enhancing user experience and bringing your UI to life.

<p align="center">
  <img src="./media/example.gif" alt="animated" />
</p>

---

## ✨ Why Use AnimationSequence?

- ✅ **Simple & Intuitive**: Effortlessly animate elements sequentially — no manual index management needed!
- ✅ **Highly Customizable**: Full control over individual animations, delays, and transitions.
- ✅ **Hierarchical Animations**: Seamlessly manage nested animations.
- ✅ **Lazy List Support**: Beautiful staggered animations for LazyColumn/LazyRow/LazyGrid.
- ✅ **Automatic Ordering**: Items animate in composition order, or use explicit indices for precise control.
- ✅ **Robust**: Efficient resource cleanup and error handling built-in.

---

## 🚀 Quick Start

Here's a simple example to get you started:

```kotlin
AnimationSequenceHost {
    AnimatedItem {
        Text("Hello, Compose Animations!")
    }
    AnimatedItem {
        Button(onClick = {}) { Text("Animated Button") }
    }
}
```

## 📖 Detailed Usage

### 📌 AnimationSequenceHost

The `AnimationSequenceHost` composable is the core of this library, managing the sequential animation flow.

**Parameters:**

- `modifier: Modifier` *(optional)* - Modifier to be applied to the host container.
- `startByDefault: Boolean` *(optional, default: `true`)* - Automatically starts animations upon composition if set to `true`.
- `content: @Composable (scope: SequentialAnimationScope) -> Unit` - The composable content, providing access to the `SequentialAnimationScope` for granular animation control.

### SequentialAnimationScope

Provides detailed control over animations:

- `enter()` - Animates all items sequentially.
- `exit(all: Boolean = false)` - Animates exit transitions sequentially. When `all` is `true`, exits all animations simultaneously.
- `enter(key: Int)` - Starts the enter animation for a specific item by its hash key.
- `exit(key: Int)` - Starts the exit animation for a specific item by its hash key.
- `isAnimating()` - Returns `true` if an animation sequence is currently in progress.

### 🖌️ AnimatedItem

Defines individual animated components within the `AnimationSequenceHost`.

**Automatic Ordering:** Items animate in the order they are composed — no need to specify indices manually!

```kotlin
AnimationSequenceHost {
    AnimatedItem { Text("Animates 1st") }
    AnimatedItem { Text("Animates 2nd") }
    AnimatedItem { Text("Animates 3rd") }
}
```

**Parameters:**

- `modifier: Modifier` *(optional)* - Modifier applied to the animated content.
- `key: Any?` *(optional)* - Stable key for identity. Required for lazy lists.
- `index: Int?` *(optional)* - Explicit slot to reserve in the animation order. If null, automatically assigned based on composition order.
- `animateOnce: Boolean` *(optional, default: `false`)* - If true, item only animates on first appearance.
- `delayAfterAnimation: Long` *(optional, default: `400ms`)* - Delay before the next animation starts.
- `enter: EnterTransition` *(optional, default: `fadeIn` with 300ms duration)* - Defines the animation when entering.
- `exit: ExitTransition` *(optional, default: `fadeOut` with 300ms duration)* - Defines the animation when exiting.
- `content: @Composable () -> Unit` - The content to be animated.

**Mixing Explicit and Auto Indices:**

Explicit indices reserve specific slots; auto items fill the remaining slots:

```kotlin
AnimationSequenceHost {
    AnimatedItem { Text("A") }              // Auto → slot 0
    AnimatedItem(index = 5) { Text("B") }   // Explicit → slot 5
    AnimatedItem { Text("C") }              // Auto → slot 1
}
// Animation order: A (0) → C (1) → B (5)
```

## 📜 Lazy List Animations

Beautiful staggered animations for `LazyColumn`, `LazyRow`, and `LazyGrid` with a clean, scoped API.

### Basic Usage

Wrap your lazy list in `LazyAnimationHost`:

```kotlin
LazyAnimationHost {
    LazyColumn {
        animatedItems(items, key = { it.id }) { item ->
            ListItemContent(item)
        }
    }
}
```

That's it! Items animate with a staggered fade-in effect and won't re-animate when scrolled back into view.

### Custom Stagger Timing

```kotlin
LazyAnimationHost(staggerDelayMillis = 100) {
    LazyColumn {
        animatedItems(items, key = { it.id }) { item ->
            ListItemContent(item)
        }
    }
}
```

### Custom Animations Per Item

Full control over enter/exit transitions with access to the calculated stagger delay:

```kotlin
LazyAnimationHost(staggerDelayMillis = 80) {
    LazyColumn {
        animatedItems(
            items = myList,
            key = { it.id },
            enter = { index, item, staggerDelay ->
                // Use staggerDelay in your animation spec!
                if (item.isSpecial) {
                    scaleIn(tween(300, delayMillis = staggerDelay)) +
                    fadeIn(tween(300, delayMillis = staggerDelay))
                } else {
                    slideInHorizontally(tween(300, delayMillis = staggerDelay)) { it } +
                    fadeIn(tween(300, delayMillis = staggerDelay))
                }
            },
            exit = { _, _ -> fadeOut() }
        ) { item ->
            ListItemContent(item)
        }
    }
}
```

### LazyAnimationHost Parameters

- `staggerDelayMillis: Int` *(default: `50`)* - Delay between each item's animation.
- `defaultEnter: EnterTransition` *(default: `fadeIn(300ms)`)* - Default enter transition.
- `defaultExit: ExitTransition` *(default: `fadeOut(300ms)`)* - Default exit transition.

### LazyAnimationScope Functions

The scope provides access to useful functions:

- `reset()` - Clears all tracking and restarts stagger timing. Use to replay animations.
- `wasShown(key: Any)` - Check if an item has already been animated.

```kotlin
LazyAnimationHost { scope ->
    Button(onClick = { scope.reset() }) {
        Text("Replay Animations")
    }
    LazyColumn { ... }
}
```

### animatedItems Parameters

- `items: List<T>` - The list of items to display.
- `key: (T) -> Any` - **Required.** Stable, unique key for each item.
- `enter: ((index, item, staggerDelay) -> EnterTransition)?` - Custom enter transition per item.
- `exit: ((index, item) -> ExitTransition)?` - Custom exit transition per item.

> **Note:** Stagger is automatically applied only to initially visible items. Items scrolled into view later animate immediately without stagger delay.

### Dynamic Lists with Exit Animations

For lists where items are dynamically added/removed and you need **exit animations**, use `scope.trackItems()`:

```kotlin
var items by remember { mutableStateOf(listOf(1, 2, 3)) }

LazyAnimationHost { scope ->
    // trackItems manages visibility state for proper exit animations
    val animatedList = scope.trackItems(items, key = { it })
    
    LazyColumn {
        animatedItems(
            items = animatedList,
            enter = { _, _, _ -> slideInHorizontally { it } + fadeIn() },
            exit = { _, _ -> slideOutHorizontally { -it } + fadeOut() }
        ) { animatedItem ->
            ItemCard(
                item = animatedItem.data,  // Access actual data via .data
                onRemove = { items = items - animatedItem.data }
            )
        }
    }
}
```

**Why is this needed?** When items are removed from a LazyColumn, the composable is immediately disposed. `trackItems()` keeps removed items in composition until their exit animation completes.

### Benefits of LazyAnimationHost

- ✅ **Scoped State**: All animation state is scoped to the host — no global state!
- ✅ **Automatic Reset**: Navigate away and back, animations replay automatically.
- ✅ **Clean API**: No manual state management needed.
- ✅ **Consistent with AnimationSequenceHost**: Familiar pattern for the whole library.

---

## 🎯 Advanced Example

Use `SequentialAnimationScope` for detailed control:

```kotlin
AnimationSequenceHost(startByDefault = false) { scope ->
    LaunchedEffect(Unit) {
        scope.enter()        // Start all animations sequentially
    }

    AnimatedItem {
        Text("First Animated Item")
    }
    AnimatedItem {
        Text("Second Animated Item")
    }
    AnimatedItem {
        Text("Third Animated Item")
    }
}
```

## 🌳 Hierarchical Animations

Effortlessly manage nested animations:

```kotlin
AnimationSequenceHost {
    AnimatedItem {
        Text("Parent Animated Text")
    }

    AnimationSequenceHost {
        AnimatedItem {
            Text("Child Animated Text 1")
        }
        AnimatedItem {
            Text("Child Animated Text 2")
        }
    }
}
```

When the parent exits, all children automatically perform their exit animations sequentially.

## 🔄 Migration Guide

### From 1.x to 2.x

**Good news!** The library is **fully backward compatible**. Your existing code will continue to work without any changes.

#### What's New

1. **Automatic Indexing**: The `index` parameter is now **optional**. Items automatically animate in composition order:
   ```kotlin
   // Before (still works!)
   AnimatedItem(index = 0) { Text("First") }
   AnimatedItem(index = 1) { Text("Second") }

   // After (simpler!)
   AnimatedItem { Text("First") }
   AnimatedItem { Text("Second") }
   ```

2. **Lazy List Support**: New `LazyAnimationHost` and `animatedItems` for beautiful staggered animations in lazy lists.

3. **New Parameters**: `key` and `animateOnce` for advanced use cases (lazy lists, conditional items).

#### Breaking Changes

None! All existing APIs work exactly as before.

---

## ⚙️ Installation

**Step 1. Add the JitPack repository to your build file**

In your `settings.gradle.kts` (or `settings.gradle`) file, add:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

**Step 2. Add the dependency in your build.gradle.kts file**
```kotlin
dependencies {
    implementation("com.github.pauloaapereira:animatedsequence:<version>")
}
```
Replace `<version>` with the latest release version. [![](https://jitpack.io/v/pauloaapereira/AnimatedSequence.svg)](https://jitpack.io/#pauloaapereira/AnimatedSequence)

## 📌 Contribution

Contributions, issues, and feature requests are welcome! Feel free to check the [issues page](https://github.com/pauloaapereira/AnimatedSequence/issues).

## 📄 License

Distributed under the Apache License 2.0.
<a href="https://opensource.org/licenses/Apache-2.0"><img alt="License" src="https://img.shields.io/badge/License-Apache%202.0-blue.svg"/></a>

## Find this repository useful? :]

Feel free to support me and my new content on: 

<a href="https://www.buymeacoffee.com/ppereira"><img alt="BuyMeACoffee" src="https://badges.aleen42.com/src/buymeacoffee.svg"/></a> 

<a href="https://www.paypal.com/donate?hosted_button_id=68Q9V7ZGGAW2W"><img alt="Paypal" src="https://badges.aleen42.com/src/paypal.svg"/></a> 

---

Happy Animating! ✨🚀
