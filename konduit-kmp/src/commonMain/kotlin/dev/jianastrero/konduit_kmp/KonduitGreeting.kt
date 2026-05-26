package dev.jianastrero.konduit_kmp

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun KonduitGreeting(modifier: Modifier = Modifier) {
    Text(
        text = "Hello from konduit-kmp library!",
        modifier = modifier
    )
}
