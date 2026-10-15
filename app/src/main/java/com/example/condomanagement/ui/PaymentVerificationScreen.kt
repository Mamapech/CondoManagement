
package com.example.condomanagement.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.condomanagement.data.repository.PaymentRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val VerifyNavy = Color(0xFF192F50)
private val VerifyGold = Color(0xFFD4AF37)
private val VerifyBackground = Color(0xFFF3F4F8)
private val VerifyGreen = Color(0xFF16845B)
private val VerifyOrange = Color(0xFFD9822B)
private val VerifyRed = Color(0xFFDC4545)

private data class VerificationPaymentRecord(
    val paymentId: String = "",
    val uid: String = "",
    val billId: String = "",
    val roomId: String = "",
    val amount: Double = 0.0,
    val slipBase64: String = "",
    val status: String = "under_review",
    val rejectionReason: String = "",
    val createdAt: Long = 0L
)

private fun documentToPayment(
    document: com.google.firebase.firestore.DocumentSnapshot
): VerificationPaymentRecord {
    val amount = document.getDouble("amount")
        ?: document.getLong("amount")?.toDouble()
        ?: 0.0

    return VerificationPaymentRecord(
        paymentId = document.getString("paymentId") ?: document.id,
        uid = document.getString("uid").orEmpty(),
        billId = document.getString("billId").orEmpty(),
        roomId = document.getString("roomId").orEmpty(),
        amount = amount,
        slipBase64 = document.getString("slipBase64").orEmpty(),
        status = document.getString("status") ?: "under_review",
        rejectionReason = document.getString("rejectionReason").orEmpty(),
        createdAt = document.getLong("createdAt") ?: 0L
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomPaymentVerificationScreen(
    roomId: String,
    roomNumber: String,
    onBack: () -> Unit
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val context = LocalContext.current
    val repository = remember(context) {
        PaymentRepository(context.applicationContext)
    }
    val scope = rememberCoroutineScope()

    var payments by remember(roomId) {
        mutableStateOf<List<VerificationPaymentRecord>>(emptyList())
    }

    var loading by remember(roomId) {
        mutableStateOf(true)
    }

    var refreshing by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    var successMessage by remember {
        mutableStateOf<String?>(null)
    }

    var processingPaymentId by remember {
        mutableStateOf<String?>(null)
    }

    var rejectingPayment by remember {
        mutableStateOf<VerificationPaymentRecord?>(null)
    }

    var rejectionReason by remember {
        mutableStateOf("")
    }

    // รับรายการชำระเงินจาก Firestore แบบเรียลไทม์
    DisposableEffect(roomId) {
        loading = true

        val registration: ListenerRegistration =
            db.collection("payments")
                .whereEqualTo("roomId", roomId)
                .addSnapshotListener { snapshot, error ->

                    if (error != null) {
                        errorMessage =
                            "โหลดรายการชำระเงินไม่สำเร็จ: ${error.localizedMessage}"
                        loading = false
                        refreshing = false
                    } else {
                        payments = snapshot?.documents.orEmpty()
                            .map { documentToPayment(it) }
                            .sortedByDescending { it.createdAt }

                        errorMessage = null
                        loading = false
                        refreshing = false
                    }
                }

        onDispose {
            registration.remove()
        }
    }

    val pendingCount = payments.count {
        it.status == "under_review"
    }

    val currencyFormat = remember {
        NumberFormat.getNumberInstance(Locale("th", "TH")).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }

    Scaffold(
        containerColor = VerifyBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "ตรวจสอบการชำระเงิน",
                            color = VerifyNavy,
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp
                        )
                        Text(
                            text = "ห้อง $roomNumber",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "กลับ",
                            tint = VerifyNavy
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            refreshing = true
                            scope.launch {
                                try {
                                    val snapshot = db.collection("payments")
                                        .whereEqualTo("roomId", roomId)
                                        .get()
                                        .await()

                                    payments = snapshot.documents
                                        .map { documentToPayment(it) }
                                        .sortedByDescending { it.createdAt }

                                    errorMessage = null
                                } catch (e: Exception) {
                                    errorMessage =
                                        e.localizedMessage
                                            ?: "รีเฟรชข้อมูลไม่สำเร็จ"
                                } finally {
                                    refreshing = false
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "รีเฟรช",
                            tint = VerifyNavy
                        )
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // สรุปรายการชำระเงิน
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "รายการชำระเงินทั้งหมด",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${payments.size} รายการ",
                            color = VerifyNavy,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            "รอตรวจสอบ",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "$pendingCount รายการ",
                            color = VerifyOrange,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (refreshing) {
                LinearProgressIndicatorCompat()
            }

            errorMessage?.let { message ->
                Text(
                    text = message,
                    color = VerifyRed,
                    modifier = Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 6.dp
                    ),
                    fontSize = 13.sp
                )
            }

            successMessage?.let { message ->
                Text(
                    text = message,
                    color = VerifyGreen,
                    modifier = Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 6.dp
                    ),
                    fontSize = 13.sp
                )
            }

            when {
                loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = VerifyNavy)
                    }
                }

                payments.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "ยังไม่มีรายการชำระเงิน",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = VerifyNavy
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "เมื่อผู้เช่าส่งหลักฐานการชำระเงิน รายการจะแสดงที่นี่",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding =
                            androidx.compose.foundation.layout.PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 0.dp,
                                bottom = 24.dp
                            ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = payments,
                            key = { it.paymentId }
                        ) { payment ->

                            PaymentVerificationCard(
                                payment = payment,
                                currencyFormat = currencyFormat,
                                processing =
                                    processingPaymentId == payment.paymentId,
                                onApprove = {
                                    scope.launch {
                                        processingPaymentId = payment.paymentId
                                        errorMessage = null
                                        successMessage = null

                                        try {
                                            repository.approvePayment(
                                                payment.paymentId
                                            )
                                            successMessage =
                                                "อนุมัติการชำระเงินสำเร็จ"
                                        } catch (e: Exception) {
                                            errorMessage =
                                                e.localizedMessage
                                                    ?: "อนุมัติไม่สำเร็จ"
                                        } finally {
                                            processingPaymentId = null
                                        }
                                    }
                                },
                                onReject = {
                                    rejectingPayment = payment
                                    rejectionReason = ""
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog สำหรับปฏิเสธรายการ
    rejectingPayment?.let { payment ->
        AlertDialog(
            onDismissRequest = {
                if (processingPaymentId == null) {
                    rejectingPayment = null
                }
            },
            title = {
                Text(
                    "ปฏิเสธการชำระเงิน",
                    color = VerifyNavy,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "กรุณาระบุเหตุผลที่ปฏิเสธรายการนี้",
                        fontSize = 14.sp
                    )

                    OutlinedTextField(
                        value = rejectionReason,
                        onValueChange = { rejectionReason = it },
                        label = { Text("เหตุผลที่ปฏิเสธ") },
                        placeholder = {
                            Text("เช่น สลิปไม่ถูกต้อง หรือยอดเงินไม่ตรง")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = processingPaymentId == null
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = rejectionReason.isNotBlank() &&
                            processingPaymentId == null,
                    onClick = {
                        scope.launch {
                            processingPaymentId = payment.paymentId
                            errorMessage = null
                            successMessage = null

                            try {
                                repository.rejectPayment(
                                    paymentId = payment.paymentId,
                                    reason = rejectionReason
                                )
                                successMessage = "ปฏิเสธการชำระเงินแล้ว"
                                rejectingPayment = null
                            } catch (e: Exception) {
                                errorMessage =
                                    e.localizedMessage
                                        ?: "ปฏิเสธรายการไม่สำเร็จ"
                            } finally {
                                processingPaymentId = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VerifyRed
                    )
                ) {
                    Text("ยืนยันปฏิเสธ")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = processingPaymentId == null,
                    onClick = { rejectingPayment = null }
                ) {
                    Text("ยกเลิก")
                }
            }
        )
    }
}

@Composable
private fun PaymentVerificationCard(
    payment: VerificationPaymentRecord,
    currencyFormat: NumberFormat,
    processing: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val statusColor = when (payment.status) {
        "approved", "paid" -> VerifyGreen
        "rejected" -> VerifyRed
        else -> VerifyOrange
    }

    val statusText = when (payment.status) {
        "approved", "paid" -> "อนุมัติแล้ว"
        "rejected" -> "ปฏิเสธแล้ว"
        else -> "รอตรวจสอบ"
    }

    // แปลง Base64 ที่อ่านจาก Firestore กลับเป็น Bitmap
    val slipBitmap: Bitmap? = remember(payment.slipBase64) {
        if (payment.slipBase64.isBlank()) {
            null
        } else {
            runCatching {
                val bytes = Base64.decode(
                    payment.slipBase64,
                    Base64.DEFAULT
                )
                BitmapFactory.decodeByteArray(
                    bytes,
                    0,
                    bytes.size
                )
            }.getOrNull()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "ยอดชำระ ฿${currencyFormat.format(payment.amount)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = VerifyNavy
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "รหัสบิล: ${payment.billId}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = statusColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(
                            horizontal = 10.dp,
                            vertical = 6.dp
                        )
                    )
                }
            }

            Text(
                text = "วันที่ส่ง: ${formatPaymentDate(payment.createdAt)}",
                fontSize = 12.sp,
                color = Color.Gray
            )

            HorizontalDividerCompat()

            Text(
                text = "หลักฐานการชำระเงิน",
                color = VerifyNavy,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            if (slipBitmap != null) {
                AsyncImage(
                    model = slipBitmap,
                    contentDescription = "สลิปชำระเงิน",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .background(
                            Color(0xFFF3F4F8),
                            RoundedCornerShape(12.dp)
                        ),
                    contentScale = ContentScale.Fit
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .background(
                            Color(0xFFFFF0F0),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (payment.slipBase64.isBlank()) {
                            "ไม่พบข้อมูลรูปสลิปใน Firestore"
                        } else {
                            "ไม่สามารถแสดงรูปสลิปได้"
                        },
                        color = VerifyRed,
                        fontSize = 12.sp
                    )
                }
            }

            if (
                payment.status == "rejected" &&
                payment.rejectionReason.isNotBlank()
            ) {
                Text(
                    text = "เหตุผลที่ปฏิเสธ: ${payment.rejectionReason}",
                    color = VerifyRed,
                    fontSize = 12.sp
                )
            }

            if (payment.status == "under_review") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        enabled = !processing,
                        onClick = onReject,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(50.dp)
                    ) {
                        Text(
                            "ปฏิเสธ",
                            color = VerifyRed
                        )
                    }

                    Button(
                        enabled = !processing,
                        onClick = onApprove,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VerifyGreen
                        )
                    ) {
                        if (processing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("อนุมัติ")
                        }
                    }
                }
            }
        }
    }
}

private fun formatPaymentDate(timestamp: Long): String {
    if (timestamp <= 0L) return "ไม่ระบุวันที่"

    return try {
        SimpleDateFormat(
            "dd/MM/yyyy HH:mm",
            Locale("th", "TH")
        ).format(Date(timestamp))
    } catch (_: Exception) {
        "ไม่ระบุวันที่"
    }
}

@Composable
private fun LinearProgressIndicatorCompat() {
    androidx.compose.material3.LinearProgressIndicator(
        modifier = Modifier.fillMaxWidth(),
        color = VerifyGold
    )
}

@Composable
private fun HorizontalDividerCompat() {
    androidx.compose.material3.HorizontalDivider(
        color = Color(0xFFE5E7EB)
    )
}
