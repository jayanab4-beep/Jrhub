package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          WebViewComponent(url = "https://www.aplgum.com", modifier = Modifier.padding(innerPadding))
        }
      }
    }
  }
}

@Composable
fun WebViewComponent(url: String, modifier: Modifier = Modifier) {
  AndroidView(
    factory = { context ->
      WebView(context).apply {
        webViewClient = WebViewClient()
        settings.javaScriptEnabled = true
        loadUrl(url)
      }
    },
    modifier = modifier
  )
}

@Preview(showBackground = true)
@Composable
fun WebViewPreview() {
  MyApplicationTheme { WebViewComponent("https://www.aplgum.com") }
}
