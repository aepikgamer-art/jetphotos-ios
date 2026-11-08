package dev.jetphotos.android

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private data class JetPhoto(
    val image: String,
    val link: String,
    val thumbnail: String,
    val dateTaken: String,
    val dateUploaded: String,
    val location: String,
    val photographer: String,
    val aircraft: String,
    val serial: String,
    val airline: String
) {
    val id: String get() = link.ifBlank { image }
}

private class JetApiException(message: String) : Exception(message)

private object JetApi {
    suspend fun search(query: String, count: Int): List<JetPhoto> = withContext(Dispatchers.IO) {
        val term = query.trim()
        if (term.isEmpty()) throw JetApiException("Enter an aircraft registration or code.")
        if (term.any(Char::isWhitespace)) {
            throw JetApiException("Use a registration or single aircraft code without spaces, like N787BK or A350-1000.")
        }

        val encoded = java.net.URLEncoder.encode(term, "UTF-8")
        val url = URL("https://www.jetapi.dev/api?reg=$encoded&photos=${count.coerceIn(1, 20)}&only_jp=true")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 25_000
            setRequestProperty("Accept", "application/json")
        }

        try {
            when (val status = connection.responseCode) {
                404 -> throw JetApiException("No photos were returned for that search.")
                400 -> throw JetApiException("JetAPI rejected that search. Use a registration or aircraft code without spaces.")
                in 200..299 -> Unit
                else -> throw JetApiException("JetAPI returned error $status. Try again in a bit.")
            }
            val root = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val images = root.optJSONArray("Images") ?: return@withContext emptyList()
            buildList {
                for (index in 0 until images.length()) {
                    val item = images.optJSONObject(index) ?: continue
                    fun value(key: String): String =
                        item.optString(key, "").takeUnless { it == "null" } ?: ""
                    add(
                        JetPhoto(
                            image = normalizeImageUrl(value("Image")),
                            link = value("Link"),
                            thumbnail = normalizeImageUrl(value("Thumbnail")),
                            dateTaken = value("DateTaken"),
                            dateUploaded = value("DateUploaded"),
                            location = value("Location"),
                            photographer = value("Photographer"),
                            aircraft = value("Aircraft"),
                            serial = value("Serial"),
                            airline = value("Airline")
                        )
                    )
                }
            }
        } catch (error: JetApiException) {
            throw error
        } catch (error: Exception) {
            throw JetApiException(error.localizedMessage ?: "Couldn't load photos. Check your connection and try again.")
        } finally {
            connection.disconnect()
        }
    }

    private fun normalizeImageUrl(value: String): String = when {
        value.startsWith("https://") || value.startsWith("http://") -> value
        value.startsWith("//") -> "https:$value"
        value.isNotBlank() -> "https:$value"
        else -> ""
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { JetPhotosApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JetPhotosApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var photos by remember { mutableStateOf<List<JetPhoto>>(emptyList()) }
    var selected by remember { mutableStateOf<JetPhoto?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var photoCount by remember { mutableIntStateOf(12) }
    var settingsOpen by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    val tablet = LocalConfiguration.current.screenWidthDp >= 840

    fun search() {
        scope.launch {
            loading = true
            error = null
            selected = null
            try {
                photos = JetApi.search(query, photoCount)
                if (photos.isEmpty()) error = "Nothing found for “${query.trim()}”. Check the registration or aircraft code and try again."
            } catch (failure: Exception) {
                error = failure.message ?: "Something went wrong."
                photos = emptyList()
            } finally {
                loading = false
            }
        }
    }

    fun save(photo: JetPhoto) {
        scope.launch {
            saving = true
            try {
                savePhotoToGallery(context, photo)
                Toast.makeText(context, "Saved to your photo library", Toast.LENGTH_SHORT).show()
            } catch (failure: Exception) {
                Toast.makeText(context, failure.message ?: "Couldn't save photo", Toast.LENGTH_LONG).show()
            } finally {
                saving = false
            }
        }
    }

    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("JetPhotos", fontWeight = FontWeight.Bold)
                            Text("Aircraft photography", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    actions = {
                        Box {
                            TextButton(onClick = { settingsOpen = true }) { Text("$photoCount photos") }
                            DropdownMenu(expanded = settingsOpen, onDismissRequest = { settingsOpen = false }) {
                                listOf(6, 12, 20).forEach { amount ->
                                    DropdownMenuItem(
                                        text = { Text("$amount photos per search") },
                                        onClick = { photoCount = amount; settingsOpen = false }
                                    )
                                }
                            }
                        }
                    }
                )
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it.uppercase() },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("Registration or aircraft code") },
                        placeholder = { Text("N787BK or A350-1000") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { search() })
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { search() }, enabled = !loading) {
                        if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Text("Search")
                    }
                }
                if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())

                if (tablet && selected != null) {
                    Row(
                        Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        PhotoGrid(photos, selected, { selected = it }, Modifier.weight(1.15f))
                        PhotoDetailPanel(
                            photo = selected!!,
                            saving = saving,
                            onSave = { save(it) },
                            onShare = { sharePhoto(context, it) },
                            modifier = Modifier.weight(0.85f)
                        )
                    }
                } else if (error != null) {
                    EmptyState("No photos found", error!!, Modifier.fillMaxSize())
                } else if (photos.isEmpty() && !loading) {
                    EmptyState(
                        "Search aircraft photos",
                        "Search by registration (N787BK) or a single aircraft code (A350-1000).",
                        Modifier.fillMaxSize()
                    )
                } else {
                    PhotoGrid(
                        photos,
                        selected,
                        { selected = it },
                        Modifier.fillMaxSize()
                    )
                }
            }
        }

        if (!tablet && selected != null) {
            ModalBottomSheet(onDismissRequest = { selected = null }) {
                PhotoDetailPanel(
                    photo = selected!!,
                    saving = saving,
                    onSave = { save(it) },
                    onShare = { sharePhoto(context, it) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                )
            }
        }
    }
}

@Composable
private fun PhotoGrid(
    photos: List<JetPhoto>,
    selected: JetPhoto?,
    onSelect: (JetPhoto) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 168.dp),
        modifier = modifier,
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(photos, key = { it.id }) { photo ->
            Column(
                Modifier.clip(RoundedCornerShape(16.dp))
                    .background(if (selected?.id == photo.id) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onSelect(photo) }
                    .padding(8.dp)
            ) {
                AsyncImage(
                    model = photo.thumbnail.ifBlank { photo.image },
                    contentDescription = "Aircraft photo",
                    modifier = Modifier.fillMaxWidth().height(142.dp).clip(RoundedCornerShape(11.dp)),
                    contentScale = ContentScale.Crop
                )
                Text(
                    photo.airline.ifBlank { photo.aircraft.ifBlank { "Aircraft photo" } },
                    modifier = Modifier.padding(top = 8.dp),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    photo.photographer.ifBlank { photo.location },
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PhotoDetailPanel(
    photo: JetPhoto,
    saving: Boolean,
    onSave: (JetPhoto) -> Unit,
    onShare: (JetPhoto) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
        AsyncImage(
            model = photo.image.ifBlank { photo.thumbnail },
            contentDescription = "Full size aircraft photo",
            modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp, max = 430.dp).clip(RoundedCornerShape(18.dp)),
            contentScale = ContentScale.Fit
        )
        Text(
            photo.airline.ifBlank { photo.aircraft.ifBlank { "Aircraft photo" } },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 14.dp)
        )
        Text(
            photo.photographer.ifBlank { "Photographer not listed" },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onSave(photo) }, enabled = !saving, modifier = Modifier.weight(1f)) {
                if (saving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Icon(Icons.Default.Download, contentDescription = null)
                Spacer(Modifier.width(6.dp)); Text("Save")
            }
            OutlinedButton(onClick = { onShare(photo) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(Modifier.width(6.dp)); Text("Share")
            }
            OutlinedButton(onClick = {
                if (photo.link.isNotBlank()) context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(photo.link)))
                else Toast.makeText(context, "Source link unavailable", Toast.LENGTH_SHORT).show()
            }) { Icon(Icons.Default.OpenInNew, contentDescription = "Open JetPhotos source") }
        }
        HorizontalDivider()
        DetailRow("Aircraft", photo.aircraft)
        DetailRow("Airline", photo.airline)
        DetailRow("Serial", photo.serial)
        DetailRow("Location", photo.location)
        DetailRow("Photographer", photo.photographer)
        DetailRow("Date taken", photo.dateTaken)
        DetailRow("Uploaded", photo.dateUploaded)
        Text(
            "Photo credit belongs to the photographer on JetPhotos. JetPhotos is an independent photo browsing app.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    if (value.isNotBlank()) {
        Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.Top) {
            Text(label, modifier = Modifier.width(112.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun EmptyState(title: String, message: String, modifier: Modifier = Modifier) {
    Box(modifier.padding(28.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private suspend fun savePhotoToGallery(context: Context, photo: JetPhoto) = withContext(Dispatchers.IO) {
    if (photo.image.isBlank()) throw IllegalStateException("This photo has no downloadable image URL.")
    val bitmap = URL(photo.image).openStream().use { BitmapFactory.decodeStream(it) }
        ?: throw IllegalStateException("The photo couldn't be downloaded.")
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "JetPhotos-${System.currentTimeMillis()}.jpg")
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/JetPhotos")
    }
    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        ?: throw IllegalStateException("Couldn't create a photo in your gallery.")
    context.contentResolver.openOutputStream(uri)?.use { output ->
        if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output)) throw IllegalStateException("Couldn't save the photo.")
    } ?: throw IllegalStateException("Couldn't open your photo library.")
}

private fun sharePhoto(context: Context, photo: JetPhoto) {
    val shareText = buildString {
        append(photo.aircraft.ifBlank { "Aircraft photo" })
        if (photo.photographer.isNotBlank()) append(" · Photo by ${photo.photographer}")
        if (photo.link.isNotBlank()) append("\n${photo.link}")
        else if (photo.image.isNotBlank()) append("\n${photo.image}")
    }
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }, "Share JetPhotos photo"))
}
