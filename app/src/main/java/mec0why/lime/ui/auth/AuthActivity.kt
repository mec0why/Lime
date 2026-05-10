package mec0why.lime.ui.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mec0why.lime.LimeApp
import mec0why.lime.ui.theme.DarkBackground
import mec0why.lime.ui.theme.LimeGreen
import mec0why.lime.ui.theme.LimeTheme

class AuthActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_AUTH_URL = "auth_url"

        fun createIntent(context: Context, authUrl: String): Intent =
            Intent(context, AuthActivity::class.java).apply {
                putExtra(EXTRA_AUTH_URL, authUrl)
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val authUrl = intent.getStringExtra(EXTRA_AUTH_URL) ?: run {
            finish()
            return
        }
        val authManager = (applicationContext as LimeApp).authManager

        setContent {
            LimeTheme {
                var isLoading by remember { mutableStateOf(true) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkBackground)
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    userAgentString = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                                    setSupportZoom(false)
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                }
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView, url: String) {
                                        isLoading = false
                                    }

                                    override fun shouldOverrideUrlLoading(
                                        view: WebView,
                                        request: WebResourceRequest
                                    ): Boolean {
                                        val uri = request.url
                                        if (uri.host == "localhost" && uri.path == "/auth/callback") {
                                            val code = uri.getQueryParameter("code") ?: return false
                                            val state = uri.getQueryParameter("state") ?: return false
                                            CoroutineScope(Dispatchers.Main).launch {
                                                authManager.exchangeCode(code, state)
                                            }
                                            setResult(Activity.RESULT_OK)
                                            finish()
                                            return true
                                        }
                                        return false
                                    }
                                }
                                loadUrl(authUrl)
                            }
                        }
                    )

                    if (isLoading) {
                        CircularProgressIndicator(
                            color = LimeGreen,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    IconButton(
                        onClick = { finish() },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Zamknij",
                            tint = androidx.compose.ui.graphics.Color.White
                        )
                    }
                }
            }
        }
    }
}
