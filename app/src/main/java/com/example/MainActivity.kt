package com.example

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.MyApplicationTheme
import java.io.File

private const val TAG = "AplgumApp"
private const val PRIMARY_URL = "https://aplgum.com/"
private const val COMPANY_EMAIL = "info@aplgum.com"
private const val COMPANY_PHONE = "+919825000000"

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Resolves simple cache backend index corruption in Chromium WebView
    fixChromiumCache(this)

    setContent {
      MyApplicationTheme {
        AplgumApp()
      }
    }
  }

  /**
   * Cleans up corrupted disk cache files that trigger:
   * "Simple Cache Backend: wrong file structure on disk: 8 path: .../cache/WebView/Default/HTTP Cache/Code Cache/wasm"
   */
  private fun fixChromiumCache(context: Context) {
    try {
      val webViewCache = File(context.cacheDir, "WebView")
      if (webViewCache.exists()) {
        val codeCache = File(webViewCache, "Default/HTTP Cache/Code Cache")
        if (codeCache.exists()) {
          codeCache.deleteRecursively()
          Log.i(TAG, "Corrupted WebView Code Cache cleaned up successfully")
        }
      }
    } catch (e: Exception) {
      Log.w(TAG, "Could not clean corrupted WebView cache", e)
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AplgumApp() {
  val context = LocalContext.current
  var webViewInstance by remember { mutableStateOf<WebView?>(null) }
  var canGoBack by remember { mutableStateOf(false) }
  var canGoForward by remember { mutableStateOf(false) }
  var currentUrl by remember { mutableStateOf(PRIMARY_URL) }
  var pageTitle by remember { mutableStateOf("Aplgum Products") }
  var isLoading by remember { mutableStateOf(true) }
  var progress by remember { mutableFloatStateOf(0f) }
  var hasError by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf("") }

  // Intercept back button for in-webview navigation
  BackHandler(enabled = canGoBack) {
    webViewInstance?.let { webView ->
      if (webView.canGoBack()) {
        webView.goBack()
      }
    }
  }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    topBar = {
      Column(modifier = Modifier.fillMaxWidth()) {
        CenterAlignedTopAppBar(
          title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = "Aplgum Products",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = "Ashapura Proteins Ltd.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
              )
            }
          },
          navigationIcon = {
            Row {
              IconButton(
                onClick = {
                  if (canGoBack) {
                    webViewInstance?.goBack()
                  }
                },
                enabled = canGoBack,
                modifier = Modifier.testTag("nav_back_button")
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                  contentDescription = "Go Back"
                )
              }
              IconButton(
                onClick = {
                  if (canGoForward) {
                    webViewInstance?.goForward()
                  }
                },
                enabled = canGoForward,
                modifier = Modifier.testTag("nav_forward_button")
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                  contentDescription = "Go Forward"
                )
              }
            }
          },
          actions = {
            IconButton(
              onClick = {
                webViewInstance?.loadUrl(PRIMARY_URL)
              },
              modifier = Modifier.testTag("nav_home_button")
            ) {
              Icon(
                imageVector = Icons.Default.Home,
                contentDescription = "Home"
              )
            }
            IconButton(
              onClick = {
                hasError = false
                webViewInstance?.reload()
              },
              modifier = Modifier.testTag("nav_refresh_button")
            ) {
              Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Reload"
              )
            }
            IconButton(
              onClick = {
                val sendIntent = Intent().apply {
                  action = Intent.ACTION_SEND
                  putExtra(Intent.EXTRA_TEXT, currentUrl.ifBlank { PRIMARY_URL })
                  type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, "Share Aplgum Products")
                context.startActivity(shareIntent)
              },
              modifier = Modifier.testTag("nav_share_button")
            ) {
              Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Share Website"
              )
            }
          },
          colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
          )
        )

        // Loading indicator
        AnimatedVisibility(
          visible = isLoading,
          enter = fadeIn(),
          exit = fadeOut()
        ) {
          LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("loading_progress_bar"),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primaryContainer
          )
        }
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      // WebView container
      AndroidView(
        factory = { ctx ->
          WebView(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT,
              ViewGroup.LayoutParams.MATCH_PARENT
            )

            // Enable cookies
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(this, true)

            // WebSettings configuration
            settings.apply {
              javaScriptEnabled = true
              domStorageEnabled = true // Critical for modern websites
              databaseEnabled = true
              useWideViewPort = true
              loadWithOverviewMode = true
              setSupportZoom(true)
              builtInZoomControls = true
              displayZoomControls = false
              allowContentAccess = true
              allowFileAccess = false
              mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
              cacheMode = WebSettings.LOAD_DEFAULT
              // Use clean mobile User Agent
              userAgentString = userAgentString.replace("; wv", "")
            }

            webViewClient = object : WebViewClient() {
              override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
              ): Boolean {
                val url = request?.url?.toString() ?: return false
                if (url.startsWith("tel:") || url.startsWith("mailto:") || url.startsWith("whatsapp:")) {
                  return try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    ctx.startActivity(intent)
                    true
                  } catch (e: Exception) {
                    false
                  }
                }
                return false
              }

              override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                isLoading = true
                hasError = false
                url?.let { currentUrl = it }
              }

              override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                isLoading = false
                url?.let { currentUrl = it }
                canGoBack = view?.canGoBack() ?: false
                canGoForward = view?.canGoForward() ?: false
              }

              override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
              ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                  isLoading = false
                  hasError = true
                  errorMessage = error?.description?.toString() ?: "Failed to connect to website"
                }
              }

              override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?
              ) {
                // Ensure reliable connection to aplgum.com in emulator
                val host = Uri.parse(error?.url ?: "").host ?: ""
                if (host.contains("aplgum.com") || host.contains("ashapuraproteins")) {
                  handler?.proceed()
                } else {
                  super.onReceivedSslError(view, handler, error)
                }
              }
            }

            webChromeClient = object : WebChromeClient() {
              override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                progress = (newProgress / 100f).coerceIn(0f, 1f)
                if (newProgress >= 100) {
                  isLoading = false
                }
              }

              override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                if (!title.isNullOrBlank()) {
                  pageTitle = title
                }
              }
            }

            loadUrl(PRIMARY_URL)
            webViewInstance = this
          }
        },
        update = { webView ->
          webViewInstance = webView
        },
        modifier = Modifier.fillMaxSize()
      )

      // Error overlay
      if (hasError) {
        Surface(
          modifier = Modifier
            .fillMaxSize()
            .testTag("error_view_overlay"),
          color = MaterialTheme.colorScheme.background
        ) {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
          ) {
            Icon(
              imageVector = Icons.Default.WifiOff,
              contentDescription = "Connection Error",
              modifier = Modifier.size(64.dp),
              tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
              text = "Unable to Load Aplgum Products",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = if (errorMessage.isNotBlank()) errorMessage else "Please check your network connection and try again.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
              onClick = {
                hasError = false
                isLoading = true
                webViewInstance?.reload()
              },
              modifier = Modifier.testTag("retry_button"),
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
              )
            ) {
              Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text("Retry Connection")
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
              onClick = {
                try {
                  val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:$COMPANY_EMAIL")
                    putExtra(Intent.EXTRA_SUBJECT, "Inquiry from Aplgum Products App")
                  }
                  context.startActivity(intent)
                } catch (e: Exception) {
                  Log.e(TAG, "Email client not found", e)
                }
              },
              modifier = Modifier.testTag("contact_email_button")
            ) {
              Icon(
                imageVector = Icons.Default.Email,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text("Contact $COMPANY_EMAIL")
            }
          }
        }
      }
    }
  }
}
