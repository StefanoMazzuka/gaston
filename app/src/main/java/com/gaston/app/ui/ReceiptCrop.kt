package com.gaston.app.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

/** Decode a bounded image and apply the camera's EXIF orientation before preview and crop. */
private fun loadReceipt(path: String): Bitmap {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    var sample = 1
    while (max(bounds.outWidth, bounds.outHeight) / sample > 3000) sample *= 2
    val source = requireNotNull(BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample }))
    val matrix = Matrix()
    when (ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.setRotate(90f); matrix.postScale(-1f, 1f) }
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
        ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.setRotate(270f); matrix.postScale(-1f, 1f) }
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(270f)
    }
    val result = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    if (result !== source) source.recycle()
    return result
}

@Composable
internal fun ReceiptCrop(path: String, cancel: () -> Unit, whole: () -> Unit, cropped: (String) -> Unit) {
    var bitmap by remember(path) { mutableStateOf<Bitmap?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var zoom by rememberSaveable(path) { mutableStateOf(1f) }
    var frameHeight by rememberSaveable(path) { mutableStateOf(.25f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(path) {
        try { bitmap = withContext(Dispatchers.IO) { loadReceipt(path) } }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { error = "No se pudo mostrar la foto. Puedes leer el ticket completo o repetirla." }
    }
    Dialog(onDismissRequest = { if (!saving) cancel() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize().safeDrawingPadding(), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Selecciona el importe", style = MaterialTheme.typography.titleLarge)
                Text("Amplía y mueve la foto hasta colocar la cantidad dentro del recuadro.")
                val image = bitmap
                val frameWidth = viewport.width * .9f
                val frameH = viewport.height * frameHeight
                val scale = if (image != null) max(frameWidth / image.width, frameH / image.height) * zoom else 1f
                val limitX = if (image != null) ((image.width * scale - frameWidth) / 2).coerceAtLeast(0f) else 0f
                val limitY = if (image != null) ((image.height * scale - frameH) / 2).coerceAtLeast(0f) else 0f
                val offset = Offset(pan.x.coerceIn(-limitX, limitX), pan.y.coerceIn(-limitY, limitY))
                val left = if (image != null) (viewport.width - image.width * scale) / 2 + offset.x else 0f
                val top = if (image != null) (viewport.height - image.height * scale) / 2 + offset.y else 0f
                val imageBitmap = remember(image) { image?.asImageBitmap() }
                Canvas(Modifier.fillMaxWidth().weight(1f).onSizeChanged { viewport = it }
                    .pointerInput(image, viewport, frameHeight, saving) {
                        detectTransformGestures { _, movement, factor, _ ->
                            if (!saving && image != null) {
                                zoom = (zoom * factor).coerceIn(1f, 8f)
                                val newScale = max(viewport.width * .9f / image.width, viewport.height * frameHeight / image.height) * zoom
                                val x = ((image.width * newScale - viewport.width * .9f) / 2).coerceAtLeast(0f)
                                val y = ((image.height * newScale - viewport.height * frameHeight) / 2).coerceAtLeast(0f)
                                pan = Offset((pan.x + movement.x).coerceIn(-x, x), (pan.y + movement.y).coerceIn(-y, y))
                            }
                        }
                    }) {
                    drawRect(Color.Black)
                    if (imageBitmap != null) withTransform({ translate(left, top); scale(scale, scale, Offset.Zero) }) { drawImage(imageBitmap) }
                    val x = (size.width - frameWidth) / 2
                    val y = (size.height - frameH) / 2
                    val shade = Color.Black.copy(alpha = .6f)
                    drawRect(shade, size = Size(size.width, y))
                    drawRect(shade, Offset(0f, y + frameH), Size(size.width, y))
                    drawRect(shade, Offset(0f, y), Size(x, frameH))
                    drawRect(shade, Offset(x + frameWidth, y), Size(x, frameH))
                    drawRect(Color.White, Offset(x, y), Size(frameWidth, frameH), style = Stroke(2.dp.toPx()))
                }
                if (image == null && error == null) LinearProgressIndicator(Modifier.fillMaxWidth())
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Text("Zoom")
                Slider(value = zoom, onValueChange = { zoom = it; pan = Offset.Zero }, valueRange = 1f..8f, enabled = !saving && image != null)
                Text("Altura del recuadro")
                Slider(value = frameHeight, onValueChange = { frameHeight = it; pan = Offset.Zero }, valueRange = .12f.. .8f, enabled = !saving && image != null)
                Button(enabled = !saving && image != null && viewport.width > 0 && viewport.height > 0, modifier = Modifier.fillMaxWidth(), onClick = {
                    val source = image ?: return@Button
                    val x = floor(((viewport.width - frameWidth) / 2 - left) / scale).toInt().coerceIn(0, source.width - 1)
                    val y = floor(((viewport.height - frameH) / 2 - top) / scale).toInt().coerceIn(0, source.height - 1)
                    val width = ceil(frameWidth / scale).toInt().coerceIn(1, source.width - x)
                    val height = ceil(frameH / scale).toInt().coerceIn(1, source.height - y)
                    saving = true
                    scope.launch {
                        var file: File? = null
                        try {
                            val result = withContext(Dispatchers.IO) {
                                val region = Bitmap.createBitmap(source, x, y, width, height)
                                try {
                                    File.createTempFile("crop-", ".png", File(path).parentFile).also { output ->
                                        file = output
                                        output.outputStream().use { check(region.compress(Bitmap.CompressFormat.PNG, 100, it)) }
                                    }
                                } finally { if (region !== source) region.recycle() }
                            }
                            cropped(result.absolutePath)
                        } catch (e: CancellationException) { file?.delete(); throw e }
                        catch (e: Exception) { file?.delete(); error = "No se pudo recortar. Prueba otra vez o lee el ticket completo." }
                        finally { saving = false }
                    }
                }) { Text(if (saving) "Preparando…" else "Usar esta zona") }
                TextButton(onClick = whole, enabled = !saving, modifier = Modifier.fillMaxWidth()) { Text("Leer ticket completo") }
                TextButton(onClick = cancel, enabled = !saving, modifier = Modifier.fillMaxWidth()) { Text("Cancelar") }
            }
        }
    }
}
