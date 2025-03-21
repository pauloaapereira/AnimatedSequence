package com.pp.animatedsequence

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.pp.animatedsequence.ui.theme.AnimatedSequenceTheme
import com.pp.library.AnimatedItem
import com.pp.library.AnimationSequenceHost
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AnimatedSequenceTheme {
                val coroutineScope = rememberCoroutineScope()

                AnimationSequenceHost { scope ->
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .windowInsetsPadding(WindowInsets.systemBars)
                                    .padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Button(onClick = {
                                    coroutineScope.launch { scope.enter() }
                                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC3B1E1), contentColor = Color.Black)) {
                                    Text("Show")
                                }
                                Button(onClick = {
                                    coroutineScope.launch { scope.exit() }
                                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC3B1E1), contentColor = Color.Black)) {
                                    Text("Hide")
                                }
                            }
                        },
                        containerColor = Color(0xFFC1E1C1)
                    ) { innerPadding ->
                        Box(modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)) {
                            Example()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Example() {
    Column(modifier = Modifier.fillMaxSize()) {
        AnimatedItem(
            index = 0,
            enter = fadeIn(tween(1000)).plus(scaleIn()),
            exit = scaleOut(),
            delayAfterAnimation = 400
        ) {
            SubcomposeAsyncImage(
                modifier = Modifier
                    .requiredHeight(250.dp)
                    .fillMaxWidth(),
                model = ImageRequest.Builder(LocalContext.current)
                    .data("https://www.pokemon.com/static-assets/content-assets/cms2/img/pokedex/full/001.png")
                    .build(),
                loading = { CircularProgressIndicator(modifier = Modifier.requiredSize(24.dp)) },
                contentDescription = "Bulbasaur",
            )
        }

        AnimatedItem(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            index = 1,
            enter = fadeIn(tween(400)).plus(scaleIn()),
            exit = scaleOut(),
            delayAfterAnimation = 400
        ) {
            Text("Bulbasaur", style = MaterialTheme.typography.displaySmall)
        }

        AnimatedItem(
            modifier = Modifier.padding(16.dp),
            index = 2,
            enter = fadeIn(tween(400)).plus(scaleIn()),
            exit = scaleOut()
        ) {
            Card(modifier = Modifier.fillMaxWidth().animateContentSize(), colors = CardDefaults.cardColors(containerColor = Color(0xFFC3B1E1))) {
                AnimationSequenceHost(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Column {
                        AnimatedItem(index = 0) {
                            Text(
                                text = "For some time after its birth, it uses the nutrients that are packed into the seed on its back in order to grow.",
                            )
                        }
                        AnimatedItem(index = 1) {
                            Row {
                                Text("Trainer: ")
                                Row {
                                    Icon(Icons.Filled.AccountCircle, contentDescription = "Profile")
                                    Text("Ash Ketchum")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}