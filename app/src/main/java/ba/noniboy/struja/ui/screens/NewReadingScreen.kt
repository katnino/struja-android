package ba.noniboy.struja.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import ba.noniboy.struja.domain.models.BillResult
import ba.noniboy.struja.ui.components.BillBreakdown
import ba.noniboy.struja.ui.components.CameraPreview
import ba.noniboy.struja.ui.components.DividerItem
import ba.noniboy.struja.ui.components.ExtractedPreview
import ba.noniboy.struja.ui.viewmodel.NewReadingViewModel
import kotlinx.coroutines.launch
import java.io.File
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PhotoLibrary

/**
 * New reading screen — allows user to enter reading values manually or via camera/OCR.
 * Port of the web app's `/meters/[id]/readings/new` page.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewReadingScreen(
    meterId: String,
    onBack: () -> Unit,
    viewModel: NewReadingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    val meter by viewModel.meter.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadMeter(meterId)
    }

    val tabOptions = listOf("Ručno", "Kamera")
    var selectedTab by remember { mutableStateOf(0) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    // Camera permission
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Permission will be re-checked when camera button is clicked
    }

    // Gallery picker
    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val bitmap = loadBitmapFromUri(context, it)
                bitmap?.let { b ->
                    viewModel.runOcr(b)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Novo očitanje") },
                navigationIcon = {
                    IconButton(onClick = { onBack.invoke() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (!uiState.isSaving) {
                        viewModel.saveReading { onBack() }
                    }
                }
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Spremi"
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Meter info
            meter?.let { m ->
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = m.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tarifa: ${m.tariffGroup} • ${m.approvedKw} kW",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        m.notes?.let { n ->
                            if (n.isNotBlank()) {
                                Text(
                                    text = n,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }

            // Tab selector
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.padding(horizontal = 16.dp),
                containerColor = Color.Transparent,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            ) {
                tabOptions.forEachIndexed { index, option ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(option) },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            when (selectedTab) {
                0 -> {
                    // Manual entry
                    ManualEntrySection(
                        uiState = uiState,
                        onVtChanged = viewModel::onVtChanged,
                        onMtChanged = viewModel::onMtChanged,
                        onRecordedAtChanged = viewModel::onRecordedAtChanged
                    )
                }

                1 -> {
                    // Camera / Gallery
                    val hasCameraPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasCameraPermission) {
                        // Show camera preview with capture button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .padding(16.dp)
                        ) {
                            CameraPreview(
                                modifier = Modifier.fillMaxSize(),
                                cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
                                onImageCaptureReady = { capture ->
                                    imageCapture = capture
                                }
                            )

                            // Capture button at the bottom center
                            val captureExecutor = ContextCompat.getMainExecutor(context)
                            IconButton(
                                onClick = {
                                    val capture = imageCapture ?: return@IconButton
                                    val photoFile = File(context.cacheDir, "temp_meter_photo.jpg")
                                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                                    capture.takePicture(
                                        outputOptions,
                                        captureExecutor,
                                        object : ImageCapture.OnImageSavedCallback {
                                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                                val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                                                bitmap?.let { b ->
                                                    viewModel.runOcr(b)
                                                }
                                            }

                                            override fun onError(exception: ImageCaptureException) {
                                                // Handle capture error silently
                                            }
                                        }
                                    )
                                },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .background(Color.Black.copy(alpha = 0.4f), shape = RoundedCornerShape(50))
                                    .size(64.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Camera,
                                    contentDescription = "Snapshot",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    } else {
                        // Need camera permission
                        CameraSection(
                            onGalleryClick = { galleryPicker.launch("image/*") },
                            onCameraClick = {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        )
                    }

                    // Show OCR result preview if available
                    val ocrResult by viewModel.ocrResult.collectAsState()
                    uiState.ocrConfidence?.let { _ ->
                        ExtractedPreview(
                            result = ocrResult,
                            onVtChanged = viewModel::onVtChanged,
                            onMtChanged = viewModel::onMtChanged,
                            onConfirm = { selectedTab = 0 }
                        )
                    }
                }
            }

            // Bill preview
            uiState.previewBill?.let { bill ->
                BillPreviewCard(bill = bill)
            }

            // Error message
            uiState.error?.let { error ->
                Text(
                    text = error,
                    color = Color(0xFFFF5252),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun BillPreviewCard(
    bill: BillResult,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A3A5F).copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Pregled procjene računa",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            DividerItem(label = "Ukupna potrošnja (kWh)", value = bill.consumptionKwh)
            DividerItem(label = "Energija", value = bill.totalEnergy)
            DividerItem(label = "Podračun", value = bill.subtotal, isBold = true)
            DividerItem(label = "PDV (17%)", value = bill.vatAmount)
            DividerItem(
                label = "UKUPNO",
                value = bill.total,
                isBold = true,
                modifier = Modifier.padding(top = 4.dp)
            )

            if (bill.blocks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                BillBreakdown(blocks = bill.blocks)
            }
        }
    }
}

@Composable
fun ManualEntrySection(
    uiState: ba.noniboy.struja.ui.viewmodel.NewReadingUiState,
    onVtChanged: (String) -> Unit,
    onMtChanged: (String) -> Unit,
    onRecordedAtChanged: (String) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = uiState.recordedAt,
            onValueChange = { if (it.length <= 10) onRecordedAtChanged(it) },
            label = { Text("Datum (YYYY-MM-DD)") },
            placeholder = { Text("npr. 2026-06-15") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = uiState.vt,
            onValueChange = { if (it.length <= 5 || it.isEmpty()) onVtChanged(it) },
            label = { Text("VT (gornja linija)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = uiState.mt,
            onValueChange = { if (it.length <= 5 || it.isEmpty()) onMtChanged(it) },
            label = { Text("MT (donja linija)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun CameraSection(
    onGalleryClick: () -> Unit,
    onCameraClick: () -> Unit
) {
    Row(modifier = Modifier.padding(16.dp)) {
        OutlinedButton(
            onClick = onGalleryClick,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = null)
            Text("  Galerija", style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(modifier = Modifier.width(8.dp))
        OutlinedButton(
            onClick = onCameraClick,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Camera, contentDescription = null)
            Text("  Kamera", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// Helper to load bitmap from URI
private fun loadBitmapFromUri(context: android.content.Context, uri: Uri): Bitmap? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        BitmapFactory.decodeStream(inputStream)
    } catch (e: Exception) {
        null
    }
}
