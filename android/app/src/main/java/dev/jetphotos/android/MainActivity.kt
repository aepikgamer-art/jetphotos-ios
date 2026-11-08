package dev.jetphotos.android

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { JetPhotosApp() }
    }
}

@Composable
private fun JetPhotosApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember { JetApi() }
    var query by remember { mutableStateOf("") }
    var photos by remember { mutableStateOf<List<JetPhoto>>(emptyList()) }
    var selected by remember { mutableStateOf<JetPhoto?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var hasSearched by remember { mutableStateOf(false) }

    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
        Surface(Modifier.fillMaxSize()) {
            BoxWithConstraints {
                val tablet = maxWidth >= 840.dp
                val columns = when {
                    maxWidth >= 1200.dp -> 5
                    maxWidth >= 840.dp -> 4
                    maxWidth >= 600.dp -> 3
                    else -> 2
                }
                Row(Modifier.fillMaxSize()) {
                    if (tablet) {
                        Column(
                            Modifier.width(280.dp).fillMaxHeight()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f))
                                .padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            BrandHeader()
                            SearchControls(query, { query = it }, loading) {
                                scope.launch {
                                    loading = true; error = null; hasSearched = true; selected = null
                                    try {
                                        photos = api.search(query)
                                        if (photos.isEmpty()) error = "No photos found. Try another registration or aircraft code."
                                    } catch (e: Exception) { error = e.message ?: "Something went wrong." }
                                    loading = false
                                }
                            }
                            Spacer(Modifier.weight(1f))
                            Text("Aircraft photos from JetPhotos", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        if (!tablet) {
                            Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp)) {
                                BrandHeader()
                                Spacer(Modifier.height(14.dp))
                                SearchControls(query, { query = it }, loading) {
                                    scope.launch {
                                        loading = true; error = null; hasSearched = true; selected = null
                                        try {
                                            photos = api.search(query)
                                            if (photos.isEmpty()) error = "No photos found. Try another registration or aircraft code."
                                        } catch (e: Exception) { error = e.message ?: "Something went wrong." }
                                        loading = false
                                    }
                                }
                            }
                        } else {
                            Text(
                                if (hasSearched) "${photos.size} photos" else "Discover aircraft photos",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp)
                            )
                        }
                        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                        when {
                            error != null -> EmptyState("Search unavailable", error!!)
                            photos.isEmpty() && !loading -> EmptyState(
                                "Search aircraft photos",
                                "Enter a registration (N787BK) or aircraft code (A350-1000) to browse photos."
                            )
                            else -> LazyVerticalGrid(
                                columns = GridCells.Adaptive(if (tablet) 210.dp else 155.dp),
                                contentPadding = PaddingValues(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(photos, key = { it.link.ifBlank { it.image } }) { photo ->
                                    PhotoCard(photo) { selected = photo }
                                }
                            }
                        }
                    }
                    if (tablet && selected != null) {
                        PhotoDetail(selected!!, Modifier.width(340.dp).fillMaxHeight(),
                            onSave = { savePhoto(context, selected!!, scope) },
                            onShare = { sharePhoto(context, selected!!) },
                            onSource = { openSource(context, selected!!) },
                            onClose = { selected = null })
                    }
                }
                if (!tablet && selected != null) {
                    AlertDialog(
                        onDismissRequest = { selected = null },
                        confirmButton = { TextButton(onClick = { selected = null }) { Text("Done") } },
                        title = { Text(selected!!.airline.ifBlank { selected!!.aircraft.ifBlank { "Aircraft photo" } }) },
                        text = {
                            Column(Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState())) {
                                PhotoDetailContent(selected!!,
                                    onSave = { savePhoto(context, selected!!, scope) },
                                    onShare = { sharePhoto(context, selected!!) },
                                    onSource = { openSource(context, selected!!) })
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BrandHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("JetPhotos", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Aircraft photography", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SearchControls(
    query: String, onQuery: (String) -> Unit, loading: Boolean, onSearch: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = query, onValueChange = onQuery,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Registration or aircraft code") },
            placeholder = { Text("N787BK or A350-1000") },
            singleLine = true,
            enabled = !loading
        )
        Button(onClick = onSearch, enabled = !loading && query.isNotBlank(),
            modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Search, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (loading) "Searching…" else "Search photos")
        }
    }
}

@Composable
private fun PhotoCard(photo: JetPhoto, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            AsyncImage(
                model = photo.thumbnailUrl.ifBlank { photo.image },
                contentDescription = photo.aircraft,
                modifier = Modifier.fillMaxWidth().height(142.dp),
                contentScale = ContentScale.Crop
            )
            Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(photo.airline.ifBlank { photo.aircraft.ifBlank { "Aircraft" } },
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                    maxLines = 1)
                Text(photo.aircraft, style = MaterialTheme.typography.bodySmall, maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (photo.location.isNotBlank()) Text(photo.location,
                    style = MaterialTheme.typography.labelSmall, maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun EmptyState(title: String, message: String) {
    Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Default.Flight, null, Modifier.size(42.dp), tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(message, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PhotoDetail(
    photo: JetPhoto, modifier: Modifier = Modifier,
    onSave: () -> Unit, onShare: () -> Unit, onSource: () -> Unit, onClose: () -> Unit
) {
    Column(modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .35f))
        .verticalScroll(rememberScrollState()).padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Photo details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close details") }
        }
        PhotoDetailContent(photo, onSave, onShare, onSource)
    }
}

@Composable
private fun PhotoDetailContent(photo: JetPhoto, onSave: () -> Unit, onShare: () -> Unit, onSource: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        AsyncImage(photo.image, photo.aircraft, Modifier.fillMaxWidth().heightIn(min = 190.dp, max = 300.dp),
            contentScale = ContentScale.Fit)
        Text(photo.airline.ifBlank { photo.aircraft }, style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold)
        DetailRow("Aircraft", photo.aircraft)
        DetailRow("Serial", photo.serial)
        DetailRow("Location", photo.location)
        DetailRow("Photographer", photo.photographer)
        DetailRow("Date taken", photo.dateTaken)
        DetailRow("Uploaded", photo.dateUploaded)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onSave, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Download, null); Spacer(Modifier.width(5.dp)); Text("Save")
            }
            OutlinedButton(onClick = onShare, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Share, null); Spacer(Modifier.width(5.dp)); Text("Share")
            }
        }
        OutlinedButton(onClick = onSource, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.OpenInBrowser, null); Spacer(Modifier.width(8.dp)); Text("Open JetPhotos source")
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    if (value.isNotBlank()) Row(verticalAlignment = Alignment.Top) {
        Text(label, Modifier.width(112.dp), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun openSource(context: android.content.Context, photo: JetPhoto) {
    val url = photo.link.ifBlank { photo.image }
    if (url.isNotBlank()) context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
}

private fun sharePhoto(context: android.content.Context, photo: JetPhoto) {
    val url = photo.link.ifBlank { photo.image }
    val text = listOf(photo.aircraft, photo.airline, url).filter { it.isNotBlank() }.joinToString("\n")
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text)
    }, "Share aircraft photo"))
}

private fun savePhoto(context: android.content.Context, photo: JetPhoto, scope: kotlinx.coroutines.CoroutineScope) {
    scope.launch {
        runCatching {
            withContext(Dispatchers.IO) {
                val request = Request.Builder().url(photo.image).build()
                OkHttpClient().newCall(request).execute().use { response ->
                    check(response.isSuccessful) { "Couldn't download this photo." }
                    val bytes = response.body?.bytes() ?: error("Photo was empty.")
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, "JetPhotos-${System.currentTimeMillis()}.jpg")
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/JetPhotos")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                    val resolver = context.contentResolver
                    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                        ?: error("Couldn't create a photo in your library.")
                    resolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("Couldn't save the photo.")
                    values.clear(); values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                }
            }
        }.onFailure { android.widget.Toast.makeText(context, it.message ?: "Save failed", android.widget.Toast.LENGTH_LONG).show() }
            .onSuccess { android.widget.Toast.makeText(context, "Saved to Pictures/JetPhotos", android.widget.Toast.LENGTH_SHORT).show() }
    }
}
