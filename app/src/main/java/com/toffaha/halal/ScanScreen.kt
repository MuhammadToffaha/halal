package com.toffaha.halal

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

@Composable
fun ScanScreen(state: AppState) {
    val ctx = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(Unit) { if (!granted) launcher.launch(Manifest.permission.CAMERA) }
    var torch by remember { mutableStateOf(false) }
    var manual by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current

    Column(Modifier.fillMaxSize()) {
        Box(
            Modifier.weight(1f).fillMaxWidth().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (granted) {
                CameraScanner(
                    active = !state.loading && state.result == null,
                    epoch = state.scanEpoch,
                    torch = torch,
                ) { code ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    state.lookup(code)
                }
                // aiming frame
                Box(
                    Modifier.fillMaxWidth(0.8f).height(150.dp)
                        .border(BorderStroke(3.dp, Gold), RoundedCornerShape(16.dp))
                )
                Text(
                    L("Point the camera at the barcode", "Kamera auf den Barcode richten"),
                    color = Color.White,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
                TextButton(
                    onClick = { torch = !torch },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp)
                ) {
                    Surface(shape = RoundedCornerShape(50), color = Color.Black.copy(alpha = 0.5f)) {
                        Text(if (torch) L("🔦 On", "🔦 An") else L("🔦 Off", "🔦 Aus"), color = Color.White, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                    }
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Text("📷", fontSize = 56.sp)
                    Text(L("Camera permission is needed to scan barcodes", "Kamerazugriff wird zum Scannen benötigt"), color = Color.White, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) { Text(L("Allow camera", "Kamera erlauben")) }
                }
            }
            if (state.loading) {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(28.dp))
                        Spacer(Modifier.size(12.dp))
                        Text(L("Checking product…", "Produkt wird geprüft…"))
                    }
                }
            }
        }

        state.error?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = manual,
                onValueChange = { manual = it.filter(Char::isDigit).take(14) },
                label = { Text(L("⌨️ Or type barcode", "⌨️ Oder Barcode eingeben")) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { state.lookup(manual) }),
                modifier = Modifier.weight(1f)
            )
            Button(onClick = { state.lookup(manual) }, enabled = manual.length >= 6) { Text(L("Check", "Prüfen")) }
        }
    }
}

@Composable
private fun CameraScanner(active: Boolean, epoch: Int, torch: Boolean, onCode: (String) -> Unit) {
    val ctx = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val callback by rememberUpdatedState(onCode)
    val enabled = remember { AtomicBoolean(true) }
    val resumeAt = remember { AtomicLong(0) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E)
                .build()
        )
    }

    LaunchedEffect(active, epoch) {
        enabled.set(active)
        // short pause so the same barcode is not read again right after closing a result
        if (active) resumeAt.set(System.currentTimeMillis() + 1500)
    }
    LaunchedEffect(torch, camera) { camera?.cameraControl?.enableTorch(torch) }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { ProcessCameraProvider.getInstance(ctx).get().unbindAll() }
            scanner.close()
            executor.shutdown()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { c ->
            val view = PreviewView(c)
            val future = ProcessCameraProvider.getInstance(c)
            future.addListener({
                val provider = future.get()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(view.surfaceProvider) }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(executor) { proxy ->
                    scanFrame(
                        proxy, scanner,
                        gate = { enabled.get() && System.currentTimeMillis() >= resumeAt.get() },
                    ) { code -> if (enabled.compareAndSet(true, false)) callback(code) }
                }
                try {
                    provider.unbindAll()
                    camera = provider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                } catch (e: Exception) {
                    camera = null
                }
            }, ContextCompat.getMainExecutor(c))
            view
        }
    )
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
private fun scanFrame(proxy: ImageProxy, scanner: BarcodeScanner, gate: () -> Boolean, onCode: (String) -> Unit) {
    val media = proxy.image
    if (media == null || !gate()) {
        proxy.close(); return
    }
    val image = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
    scanner.process(image)
        .addOnSuccessListener { codes ->
            codes.firstNotNullOfOrNull { b -> b.rawValue?.takeIf { v -> v.length >= 8 && v.all(Char::isDigit) } }
                ?.let(onCode)
        }
        .addOnCompleteListener { proxy.close() }
}
