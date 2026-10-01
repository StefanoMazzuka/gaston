package com.gaston.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.core.content.FileProvider
import com.gaston.app.domain.ReceiptAmounts
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.CancellationException
import java.io.File
import java.math.BigDecimal
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Composable
fun ReceiptCamera(enabled: Boolean, onAmount: (Long) -> Unit) {
    val context = LocalContext.current
    var photoPath by rememberSaveable { mutableStateOf<String?>(null) }
    var selecting by rememberSaveable { mutableStateOf(false) }
    var reading by rememberSaveable { mutableStateOf(false) }
    var recognizedText by rememberSaveable { mutableStateOf<String?>(null) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val deliverAmount by rememberUpdatedState(onAmount)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) selecting = true
        else {
            photoPath?.let { File(it).delete() }
            photoPath = null
        }
    }
    LaunchedEffect(reading, photoPath) {
        val path = photoPath ?: return@LaunchedEffect
        if (!reading) return@LaunchedEffect
        try {
            val input = withContext(Dispatchers.IO) { InputImage.fromFilePath(context, Uri.fromFile(File(path))) }
            val text = suspendCancellableCoroutine<String> { continuation ->
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                try {
                    recognizer.process(input)
                        .addOnSuccessListener { if (continuation.isActive) continuation.resume(it.text) }
                        .addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
                        .addOnCompleteListener { recognizer.close() }
                } catch (e: Exception) {
                    recognizer.close()
                    if (continuation.isActive) continuation.resumeWithException(e)
                }
            }
            val candidates = ReceiptAmounts.parse(text)
            val totals = candidates.filter { it.isTotal }
            when {
                candidates.isEmpty() -> error = "No se encontraron importes. Prueba otra foto con el ticket enfocado o introduce el importe manualmente."
                totals.size == 1 -> deliverAmount(totals.single().cents)
                candidates.size == 1 -> deliverAmount(candidates.single().cents)
                else -> recognizedText = text
            }
            File(path).delete()
            photoPath = null
            reading = false
        } catch (e: CancellationException) {
            // Keep the photo for the restored screen to resume recognition.
            throw e
        } catch (e: Exception) {
            File(path).delete()
            photoPath = null
            reading = false
            error = "No se pudo leer el ticket. Puedes repetir la foto o escribir el importe."
        }
    }
    OutlinedButton(enabled = enabled && photoPath == null && !reading, onClick = {
        try {
            val directory = File(context.cacheDir, "receipts").apply { mkdirs() }
            // Remove abandoned captures after one day.
            directory.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > 86_400_000L }?.forEach { it.delete() }
            val photo = File.createTempFile("ticket-", ".jpg", directory)
            photoPath = photo.absolutePath
            launcher.launch(FileProvider.getUriForFile(context, "${context.packageName}.receipts", photo))
        } catch (e: Exception) {
            photoPath?.let { File(it).delete() }
            photoPath = null
            error = "No se pudo abrir la cámara. Puedes introducir el importe manualmente."
        }
    }, modifier = Modifier.heightIn(min = 52.dp).semantics { contentDescription = "Fotografiar ticket" }) {
        Text(if (reading) "Leyendo…" else "📷")
    }
    if (selecting) photoPath?.let { original ->
        ReceiptCrop(original,
            cancel = { File(original).delete(); photoPath = null; selecting = false },
            whole = { selecting = false; reading = true },
            cropped = { croppedPath ->
                photoPath = croppedPath
                File(original).delete()
                selecting = false
                reading = true
            })
    }
    if (reading) AlertDialog(onDismissRequest = {}, title = { Text("Leyendo ticket…") },
        text = { LinearProgressIndicator(Modifier.fillMaxWidth()) }, confirmButton = {})
    recognizedText?.let { text ->
        AlertDialog(onDismissRequest = { recognizedText = null }, title = { Text("Elige el importe") },
            text = {
                Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                    ReceiptAmounts.parse(text).forEach { candidate ->
                        TextButton(onClick = { recognizedText = null; deliverAmount(candidate.cents) }) {
                            Column {
                                Text("${BigDecimal.valueOf(candidate.cents, 2).toPlainString().replace('.', ',')} €")
                                Text(candidate.label, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }, confirmButton = { TextButton(onClick = { recognizedText = null }) { Text("Cancelar") } })
    }
    error?.let { message ->
        AlertDialog(onDismissRequest = { error = null }, title = { Text("Lectura del ticket") },
            text = { Text(message) }, confirmButton = { TextButton(onClick = { error = null }) { Text("Entendido") } })
    }
}
