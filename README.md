# 🎉 AnimationSequence

[![](https://img.shields.io/maven-central/v/io.github.pauloaapereira/animatedsequence.svg?label=Maven%20Central)](https://search.maven.org/artifact/io.github.pauloaapereira/animatedsequence)

**AnimationSequence** is a powerful yet intuitive Jetpack Compose library designed to simplify sequential animations in your Android apps. Easily orchestrate elegant, responsive, and hierarchical animations with minimal effort, enhancing user experience and bringing your UI to life.

<p align="center">
  <img src="./media/example.gif" alt="animated" />
</p>

---

## 🌍 Now with Compose Multiplatform Support!

As of **version 1.1.0**, `AnimationSequence` supports **Jetpack Compose Multiplatform**, allowing you to create rich animated UIs not only for Android, but also for **iOS**,  **Desktop** and **Web (Wasm)** (where supported).

In addition:
- We’ve moved to **Maven Central** for publishing (JitPack doesn't support multiplatform yet!).
- New dependency coordinates: `io.github.pauloaapereira:animatedsequence`.

---

## ✨ Why Use AnimationSequence?

- ✅ **Simple & Intuitive**: Effortlessly animate elements sequentially.
- ✅ **Multiplatform Ready**: Works across Android, iOS, Desktop, and Web (Wasm).
- ✅ **Highly Customizable**: Full control over individual animations, delays, and transitions.
- ✅ **Hierarchical Animations**: Seamlessly manage nested animations.
- ✅ **Robust**: Efficient resource cleanup and error handling built-in.

---

## 🚀 Quick Start

Here's a simple example to get you started:

```kotlin
AnimationSequenceHost {
    AnimatedItem(index = 0) {
        Text("Hello, Compose Animations!")
    }
    AnimatedItem(index = 1) {
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
- `enter(index: Int)` - Starts the enter animation for a specific indexed item.
- `exit(index: Int)` - Starts the exit animation for a specific indexed item.

### 🖌️ AnimatedItem

Defines individual animated components within the `AnimationSequenceHost`.

**Parameters:**

- `modifier: Modifier` *(optional)* - Modifier applied to the animated content.
- `index: Int` - Unique identifier defining the animation order.
- `delayAfterAnimation: Long` *(optional, default: `400ms`)* - Delay before the next animation starts.
- `enter: EnterTransition` *(optional, default: `fadeIn` with 300ms duration)* - Defines the animation when entering.
- `exit: ExitTransition` *(optional, default: `fadeOut` with 300ms duration)* - Defines the animation when exiting.
- `content: @Composable () -> Unit` - The content to be animated.

## 🎯 Advanced Example

Use `SequentialAnimationScope` for detailed control:

```kotlin
AnimationSequenceHost(startByDefault = false) { scope ->
    LaunchedEffect(Unit) {
        scope.enter()        // Start all animations sequentially
        scope.exit(1)        // Exit animation of item at index 1
        scope.enter(2)       // Enter animation of item at index 2
    }

    AnimatedItem(index = 0) {
        Text("First Animated Item")
    }
    AnimatedItem(index = 1) {
        Text("Second Animated Item")
    }
    AnimatedItem(index = 2) {
        Text("Third Animated Item")
    }
}
```

## 🌳 Hierarchical Animations

Effortlessly manage nested animations:

```kotlin
AnimationSequenceHost {
    AnimatedItem(index = 0) {
        Text("Parent Animated Text")
    }

    AnimationSequenceHost {
        AnimatedItem(index = 0) {
            Text("Child Animated Text 1")
        }
        AnimatedItem(index = 1) {
            Text("Child Animated Text 2")
        }
    }
}
```

When the parent exits, all children automatically perform their exit animations sequentially.

## ⚙️ Installation

> 🆕 **Now available on Maven Central!**

**Step 1. Add the Maven Central repository** (if not already included)**

In your `settings.gradle.kts` (or `settings.gradle`):

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        // others ...
    }
}
```

**Step 2. Add the dependency in your `build.gradle.kts`:**

```kotlin
dependencies {
    implementation("io.github.pauloaapereira:animatedsequence:<version>")
}
```

Replace `<version>` with the latest release version. [![](https://img.shields.io/maven-central/v/io.github.pauloaapereira/animatedsequence.svg?label=Maven%20Central)](https://search.maven.org/artifact/io.github.pauloaapereira/animatedsequence)

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
