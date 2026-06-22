package com.myschedule.id.ui

import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import java.net.URLDecoder

// ─── File Type Detection ───────────────────────────────────────────────────────

private enum class FileType { IMAGE, PDF, OFFICE, OTHER }

private fun detectFileType(fileName: String): FileType {
    val ext = fileName.substringAfterLast('.', "").lowercase().trim()
    return when (ext) {
        "png", "jpg", "jpeg", "gif", "webp", "bmp", "svg" -> FileType.IMAGE
        "pdf"                                               -> FileType.PDF
        "doc", "docx", "xls", "xlsx", "ppt", "pptx"        -> FileType.OFFICE
        else                                                -> FileType.OTHER
    }
}

// ─── Main Screen ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileViewerScreen(
    navController: NavHostController,
    fileUrl: String,
    fileName: String
) {
    val context = LocalContext.current

    // Navigation Compose auto-decodes query params, but decode defensively
    val decodedUrl = remember(fileUrl) {
        try { URLDecoder.decode(fileUrl, "UTF-8") } catch (e: Exception) { fileUrl }
    }
    val decodedName = remember(fileName) {
        try { URLDecoder.decode(fileName, "UTF-8") } catch (e: Exception) { fileName }
    }

    val fileType = remember(decodedName) { detectFileType(decodedName) }

    // WebView reload trigger (only used for PDF & Office viewers)
    var reloadTrigger by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(fileType != FileType.IMAGE && fileType != FileType.OTHER) }

    val bluePrimary = Color(0xFF1565C0)
    val blueLight   = Color(0xFF64B5F6)

    Scaffold(
        topBar = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(brush = Brush.horizontalGradient(listOf(bluePrimary, blueLight)))
                        .statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Melihat File",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = decodedName,
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }

                        // Refresh: only useful for WebView-based viewers
                        if (fileType == FileType.PDF || fileType == FileType.OFFICE) {
                            IconButton(onClick = {
                                isLoading = true
                                reloadTrigger++
                            }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Muat Ulang", tint = Color.White)
                            }
                        }

                        // Open externally — always available
                        IconButton(onClick = {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, decodedUrl.toUri()))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Gagal membuka secara eksternal", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Buka Eksternal", tint = Color.White)
                        }
                    }
                }

                if (isLoading) {
                    LinearProgressIndicator(
                        modifier    = Modifier.fillMaxWidth(),
                        color       = Color.White,
                        trackColor  = blueLight.copy(alpha = 0.3f)
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F5F5))
        ) {
            when (fileType) {
                FileType.IMAGE  -> ImageViewer(decodedUrl)
                FileType.PDF    -> WebViewer(
                    viewerUrl     = "https://docs.google.com/viewer?embedded=true&url=${Uri.encode(decodedUrl)}",
                    reloadTrigger = reloadTrigger,
                    bluePrimary   = bluePrimary,
                    fileUrl       = decodedUrl,
                    onLoadStart   = { isLoading = true },
                    onLoadFinish  = { isLoading = false },
                    context       = context
                )
                FileType.OFFICE -> WebViewer(
                    viewerUrl     = "https://view.officeapps.live.com/op/embed.aspx?src=${Uri.encode(decodedUrl)}",
                    reloadTrigger = reloadTrigger,
                    bluePrimary   = bluePrimary,
                    fileUrl       = decodedUrl,
                    onLoadStart   = { isLoading = true },
                    onLoadFinish  = { isLoading = false },
                    context       = context
                )
                FileType.OTHER  -> UnsupportedFileView(
                    fileUrl     = decodedUrl,
                    fileName    = decodedName,
                    bluePrimary = bluePrimary,
                    context     = context
                )
            }
        }
    }
}

// ─── Image Viewer (Coil) ───────────────────────────────────────────────────────

@Composable
private fun ImageViewer(imageUrl: String) {
    Box(
        modifier         = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale       = ContentScale.Fit,
            modifier           = Modifier.fillMaxSize(),
            loading = {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            },
            error = {
                Column(
                    modifier             = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment  = Alignment.CenterHorizontally,
                    verticalArrangement  = Arrangement.Center
                ) {
                    Icon(Icons.Default.BrokenImage, null, tint = Color.White, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Gagal memuat gambar", color = Color.White, textAlign = TextAlign.Center)
                }
            }
        )
    }
}

// ─── WebView Viewer (PDF via Google Docs / DOCX via Office Online) ─────────────

@Composable
private fun WebViewer(
    viewerUrl     : String,
    reloadTrigger : Int,
    bluePrimary   : Color,
    fileUrl       : String,
    onLoadStart   : () -> Unit,
    onLoadFinish  : () -> Unit,
    context       : android.content.Context
) {
    var hasError      by remember { mutableStateOf(false) }
    var webViewRef    by remember { mutableStateOf<WebView?>(null) }

    // React to reload trigger
    LaunchedEffect(reloadTrigger) {
        if (reloadTrigger > 0) {
            hasError = false
            webViewRef?.reload()
        }
    }

    if (hasError) {
        ErrorFallbackView(fileUrl = fileUrl, bluePrimary = bluePrimary, context = context)
    } else {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).also { wv ->
                    webViewRef = wv
                    wv.settings.apply {
                        javaScriptEnabled    = true
                        domStorageEnabled    = true
                        loadWithOverviewMode = true
                        useWideViewPort      = true
                        builtInZoomControls  = true
                        displayZoomControls  = false
                        setSupportZoom(true)
                        // Desktop UA prevents forced sign-in prompts on viewer services
                        userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                "Chrome/124.0.0.0 Safari/537.36"
                    }
                    wv.webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            onLoadFinish()
                        }
                        override fun onReceivedError(
                            view    : WebView?,
                            request : WebResourceRequest?,
                            error   : WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            if (request?.isForMainFrame == true) {
                                hasError = true
                                onLoadFinish()
                            }
                        }
                    }
                    wv.loadUrl(viewerUrl)
                }
            },
            update = { wv ->
                // update called when reloadTrigger changes after factory
                webViewRef = wv
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

// ─── Fallback: error inside WebViewer ─────────────────────────────────────────

@Composable
private fun ErrorFallbackView(
    fileUrl     : String,
    bluePrimary : Color,
    context     : android.content.Context
) {
    Column(
        modifier            = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.BrokenImage,
            contentDescription = null,
            tint   = Color.Gray,
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text      = "Gagal memuat dokumen di aplikasi.",
            color     = Color.Gray,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text      = "Coba buka di browser atau aplikasi eksternal.",
            color     = Color.Gray.copy(alpha = 0.7f),
            fontSize  = 12.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, fileUrl.toUri()))
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = bluePrimary)
        ) {
            Text("Buka di Browser / Aplikasi Luar", color = Color.White)
        }
    }
}

// ─── Unsupported file type ─────────────────────────────────────────────────────

@Composable
private fun UnsupportedFileView(
    fileUrl     : String,
    fileName    : String,
    bluePrimary : Color,
    context     : android.content.Context
) {
    Column(
        modifier            = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector      = Icons.Default.InsertDriveFile,
            contentDescription = null,
            tint     = bluePrimary.copy(alpha = 0.4f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text      = fileName,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            color     = Color.DarkGray
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text      = "Format ini tidak dapat ditampilkan\ndi dalam aplikasi.",
            color     = Color.Gray,
            fontSize  = 13.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, fileUrl.toUri()))
                } catch (e: Exception) {
                    Toast.makeText(context, "Gagal membuka: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = bluePrimary)
        ) {
            Text("Buka di Aplikasi Luar", color = Color.White)
        }
    }
}