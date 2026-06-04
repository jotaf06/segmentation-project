package com.ufal.segmentationapp

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.ufal.segmentationapp.ui.theme.SegmentationAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var segmenter: Segmenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        segmenter = Segmenter(this)

        enableEdgeToEdge()
        setContent {
            SegmentationAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    // --- Piece 1: State ---
                    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
                    var maskBitmap     by remember { mutableStateOf<Bitmap?>(null) }
                    var isRunning      by remember { mutableStateOf(false) }

                    val scope = rememberCoroutineScope()

                    // --- Piece 2: Image picker ---
                    val pickImage = rememberLauncherForActivityResult(
                        ActivityResultContracts.GetContent()
                    ) { uri ->
                        uri?.let {
                            val source = ImageDecoder.createSource(contentResolver, it)
                            originalBitmap = ImageDecoder.decodeBitmap(source)
                                .copy(Bitmap.Config.ARGB_8888, false)
                            maskBitmap = null  // reset mask when a new image is picked
                        }
                    }

                    // --- Piece 4: Layout ---
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Segmentação de Imagens", style = MaterialTheme.typography.titleLarge)

                        Spacer(modifier = Modifier.height(4.dp))

                        // Original image
                        ImageBox(
                            bitmap = originalBitmap,
                            label = "Imagem Original"
                        )

                        // Mask image
                        ImageBox(
                            bitmap = maskBitmap,
                            label = "Máscara Segmentada"
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Buttons row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(onClick = {
                                pickImage.launch("image/*")
                            }) {
                                Text("Selecionar")
                            }

                            Button(
                                enabled = originalBitmap != null && !isRunning,
                                onClick = {
                                    // --- Piece 3: Inference on background thread ---
                                    scope.launch(Dispatchers.Default) {
                                        isRunning = true
                                        maskBitmap = segmenter.segment(originalBitmap!!)
                                        isRunning = false
                                    }
                                }
                            ) {
                                if (isRunning) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.height(18.dp)
                                    )
                                } else {
                                    Text("Segmentar")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        segmenter.close()  // release model from memory when app closes
    }
}

// Reusable composable: shows a bitmap or a placeholder box with a label
@androidx.compose.runtime.Composable
fun ImageBox(bitmap: Bitmap?, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(Color(0xFFEEEEEE)),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = label,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(label, color = Color.Gray)
            }
        }
    }
}
