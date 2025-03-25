package com.pp.sample

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.pp.animatedsequence.AnimatedItem
import com.pp.animatedsequence.AnimationSequenceHost
import kotlinx.coroutines.delay
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        AnimationSequenceHost(modifier = Modifier.fillMaxSize()) { scope ->
            LaunchedEffect(Unit) {
                scope.enter()
                delay(2000)
                scope.exit()
                delay(2000)
                scope.enter(0)
                delay(2000)
                scope.enter(2)
                delay(2000)
                scope.enter(1)
            }
            Column(modifier = Modifier.align(Alignment.Center)) {
                AnimatedItem(index = 0) {
                    Text(text = "Text 0")
                }
                AnimatedItem(index = 1) {
                    Text(text = "Text 1")
                }
                AnimatedItem(index = 2) {
                    Text(text = "Text 2")
                }
            }
        }
    }
}