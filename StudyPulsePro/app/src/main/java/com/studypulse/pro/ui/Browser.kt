package com.studypulse.pro.ui

import android.annotation.SuppressLint
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

fun isAllowed(u: String, restricted: Boolean, sites: Set<String>): Boolean {
    val uri = Uri.parse(u)
    val sch = uri.scheme?.lowercase() ?: return false
    if (sch != "https" && sch != "http") return false
    if (!restricted) return true
    val h = uri.host?.lowercase() ?: return false
    return sites.any { h == it || h.endsWith(".$it") }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun StudyBrowser(restricted: Boolean, sites: Set<String>, online: Boolean, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    var url by rememberSaveable { mutableStateOf("https://en.wikipedia.org") }
    var input by rememberSaveable { mutableStateOf(url) }
    val policy by rememberUpdatedState(restricted to sites)
    val holder = remember { arrayOfNulls<WebView>(1) }

    fun go(raw: String) {
        val t = raw.trim()
        if (t.isEmpty()) return
        val u = when {
            t.startsWith("http://") || t.startsWith("https://") -> t
            t.contains('.') && !t.contains(' ') -> "https://$t"
            else -> "https://en.wikipedia.org/w/index.php?search=" + Uri.encode(t)
        }
        if (!isAllowed(u, policy.first, policy.second)) {
            Toast.makeText(ctx, "Blocked in Focus Mode. Add the site in Settings.", Toast.LENGTH_SHORT).show()
            return
        }
        holder[0]?.loadUrl(u)
    }

    Column(modifier) {
        if (!online) {
            Glass(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), RoundedCornerShape(16.dp)) {
                Text("⚠ No internet connection. Timers and alarms keep running.", Modifier.padding(12.dp), color = Yellow)
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton({ holder[0]?.let { if (it.canGoBack()) it.goBack() } }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextP)
            }
            OutlinedTextField(
                input, { input = it }, Modifier.weight(1f), singleLine = true,
                shape = RoundedCornerShape(50), colors = fieldColors(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go, keyboardType = KeyboardType.Uri),
                keyboardActions = KeyboardActions(onGo = { go(input) })
            )
            IconButton({ holder[0]?.reload() }) { Icon(Icons.Default.Refresh, "Reload", tint = Yellow) }
        }
        AndroidView(
            factory = { c ->
                val root = FrameLayout(c)
                val w = WebView(c)
                w.settings.javaScriptEnabled = true
                w.settings.domStorageEnabled = true
                w.settings.mediaPlaybackRequiresUserGesture = false
                w.settings.builtInZoomControls = true
                w.settings.displayZoomControls = false
                w.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(v: WebView, r: WebResourceRequest): Boolean {
                        val u = r.url.toString()
                        if (!isAllowed(u, policy.first, policy.second)) {
                            Toast.makeText(c, "Blocked in Focus Mode", Toast.LENGTH_SHORT).show()
                            return true
                        }
                        if (u.substringBefore('?').endsWith(".pdf", true) && !u.contains("docs.google.com/gview")) {
                            v.loadUrl("https://docs.google.com/gview?embedded=true&url=" + Uri.encode(u))
                            return true
                        }
                        return false
                    }

                    override fun onPageStarted(v: WebView, u: String, f: android.graphics.Bitmap?) {
                        url = u; input = u
                    }
                }
                w.webChromeClient = object : WebChromeClient() {
                    private var custom: View? = null
                    override fun onShowCustomView(v: View, cb: WebChromeClient.CustomViewCallback?) {
                        custom = v
                        root.addView(v, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                        w.visibility = View.GONE
                    }

                    override fun onHideCustomView() {
                        custom?.let { root.removeView(it) }
                        custom = null
                        w.visibility = View.VISIBLE
                    }
                }
                root.addView(w, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                holder[0] = w
                w.loadUrl(url)
                root
            },
            modifier = Modifier.weight(1f).fillMaxWidth(),
            onRelease = { holder[0]?.destroy(); holder[0] = null }
        )
    }
}
