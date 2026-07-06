package com.clearcart.app.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.clearcart.app.data.repository.AppContainer
import com.clearcart.app.scanner.BarcodeAnalyzer
import kotlinx.coroutines.launch

@Composable
fun ScannerScreen(container: AppContainer, navController: NavController) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val preferences by container.preferencesRepository.state.collectAsState()
    val scope = rememberCoroutineScope()
    var hasPermission by remember { mutableStateOf(false) }
    var manualCode by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var torchEnabled by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
    }

    fun lookup(code: String) {
        if (loading || code.isBlank()) return
        loading = true
        error = null
        scope.launch {
            container.productRepository.lookupProduct(code.trim(), preferences)
                .onSuccess { navController.navigate("product/${it.barcode}") }
                .onFailure { error = "Product not found. You can enter it manually or scan a label instead." }
            loading = false
        }
    }

    LaunchedEffect(Unit) { permissionLauncher.launch(Manifest.permission.CAMERA) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Scan Product", style = MaterialTheme.typography.headlineMedium)
        Box(
            Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.Black),
        ) {
            if (hasPermission) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val providerFuture = ProcessCameraProvider.getInstance(ctx)
                        providerFuture.addListener({
                            val provider = providerFuture.get()
                            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                            val analysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also { it.setAnalyzer(ContextCompat.getMainExecutor(ctx), BarcodeAnalyzer(::lookup)) }
                            provider.unbindAll()
                            camera = provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                            camera?.cameraControl?.enableTorch(torchEnabled)
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize(),
                )
                IconButton(
                    onClick = {
                        val next = !torchEnabled
                        torchEnabled = next
                        camera?.cameraControl?.enableTorch(next)
                    },
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color(0x88000000), RoundedCornerShape(99.dp)),
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = "Torch", tint = Color.White)
                }
            } else {
                Text("Camera permission is needed to scan barcodes.", color = Color.White, modifier = Modifier.align(Alignment.Center))
            }
        }
        if (loading) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CircularProgressIndicator()
                Text("Looking up product data...")
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        OutlinedTextField(
            value = manualCode,
            onValueChange = { manualCode = it },
            label = { Text("Manual barcode entry") },
            leadingIcon = { Icon(Icons.Default.Keyboard, null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = { lookup(manualCode) }, modifier = Modifier.fillMaxWidth()) { Text("Look Up Barcode") }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { navController.navigate("manual") }, modifier = Modifier.weight(1f)) { Text("Add Manually") }
            OutlinedButton(onClick = { navController.navigate("ocr") }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.TextFields, null)
                Text("Scan Label")
            }
        }
    }
}
