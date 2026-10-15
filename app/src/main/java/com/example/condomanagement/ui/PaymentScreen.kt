
package com.example.condomanagement.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.condomanagement.data.model.Bill
import com.example.condomanagement.data.repository.PaymentRepository
import kotlinx.coroutines.launch
import androidx.activity.result.PickVisualMediaRequest
import java.io.File
import java.io.FileOutputStream
private val PaymentNavy = Color(0xFF192F50)
private val PaymentGreen = Color(0xFF16845B)

@Composable
fun PaymentScreen(
    bill: Bill,
    onBack: () -> Unit,
    onSubmitted: () -> Unit
) {
    var slipUri by remember { mutableStateOf<Uri?>(null) }
    var uploading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf(false) }
    var showSlipPreview by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val repository = remember(context) {
        PaymentRepository(context.applicationContext)
    }


    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) {
            error = "ยังไม่ได้เลือกรูปสลิป"
            return@rememberLauncherForActivityResult
        }

        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: throw Exception("ไม่สามารถอ่านรูปที่เลือกได้")

            val file = File(context.cacheDir, "payment_slip.jpg")

            input.use { source ->
                FileOutputStream(file).use { destination ->
                    source.copyTo(destination)
                }
            }

            slipUri = Uri.fromFile(file)
            error = null
        } catch (e: Exception) {
            slipUri = null
            error = "คัดลอกรูปไม่สำเร็จ: ${e.localizedMessage}"
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TextButton(
                onClick = onBack,
                enabled = !uploading
            ) {
                Text("← กลับ", color = PaymentNavy)
            }

            Text(
                text = "ชำระเงิน",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = PaymentNavy
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF3F5F9)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = bill.title,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text("ห้อง ${bill.roomId}")

                    Text(
                        text = "฿${"%,.2f".format(bill.amount)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = PaymentNavy
                    )

                    Text("สถานะ: รอชำระ")
                }
            }

            Text(
                text = "แนบหลักฐานการโอนเงิน",
                fontWeight = FontWeight.SemiBold
            )

            OutlinedButton(
                onClick = {
                    imagePicker.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                },
                enabled = !uploading && !success,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (slipUri == null) {
                        "เลือกรูปสลิป"
                    } else {
                        "เปลี่ยนรูปสลิป"
                    }
                )
            }

            slipUri?.let { uri ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AsyncImage(
                            model = uri,
                            contentDescription = "ตัวอย่างสลิป",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp),
                            contentScale = ContentScale.Fit
                        )

                        TextButton(
                            onClick = { showSlipPreview = true },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("ดูรูปสลิปขนาดใหญ่")
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF4DF)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "กรุณาตรวจสอบรูปสลิปให้ชัดเจนก่อนส่ง " +
                            "ระบบจะย่อและบีบอัดรูป แล้วบันทึกไว้ใน Firestore " +
                            "เพื่อให้เจ้าของห้องตรวจสอบได้",
                    modifier = Modifier.padding(14.dp),
                    color = Color(0xFF805500),
                    fontSize = 13.sp
                )
            }

            error?.let { message ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFEEEE)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (success) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE8F7EF)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "ส่งหลักฐานสำเร็จแล้ว กรุณารอเจ้าของตรวจสอบ",
                        modifier = Modifier.padding(14.dp),
                        color = PaymentGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Button(
                onClick = {
                    val selectedUri = slipUri ?: return@Button

                    scope.launch {
                        uploading = true
                        error = null

                        try {
                            repository.submitPayment(
                                bill = bill,
                                slipUri = selectedUri
                            )
                            success = true
                        } catch (e: Exception) {
                            error = e.localizedMessage
                                ?: "ส่งหลักฐานไม่สำเร็จ กรุณาลองใหม่"
                        } finally {
                            uploading = false
                        }
                    }
                },
                enabled = slipUri != null && !uploading && !success,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PaymentNavy
                )
            ) {
                if (uploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("กำลังบันทึกสลิป...")
                } else {
                    Text("ส่งหลักฐานการชำระเงิน")
                }
            }

            if (success) {
                Button(
                    onClick = onSubmitted,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PaymentGreen
                    )
                ) {
                    Text("กลับไปหน้าบิล")
                }
            }

            Spacer(Modifier.height(16.dp))
        }

        Spacer(Modifier.height(12.dp))
    }

    if (showSlipPreview && slipUri != null) {
        Dialog(
            onDismissRequest = {
                showSlipPreview = false
            }
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AsyncImage(
                        model = slipUri,
                        contentDescription = "รูปสลิปขนาดใหญ่",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 500.dp),
                        contentScale = ContentScale.Fit
                    )

                    TextButton(
                        onClick = {
                            showSlipPreview = false
                        }
                    ) {
                        Text("ปิด")
                    }
                }
            }
        }
    }
}
