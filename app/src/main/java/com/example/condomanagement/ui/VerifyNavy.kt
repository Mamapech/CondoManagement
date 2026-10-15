package com.example.condomanagement.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.condomanagement.data.repository.PaymentRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import androidx.compose.ui.platform.LocalContext
private val VerifyNavy = Color(0xFF192F50)
private val VerifyGreen = Color(0xFF16845B)
private val VerifyRed = Color(0xFFDC4545)
private val VerifyGold = Color(0xFFD4AF37)
private val VerifyBackground = Color(0xFFF3F4F8)

private data class PaymentRecord(
    val paymentId: String = "",
    val uid: String = "",
    val billId: String = "",
    val roomId: String = "",
    val amount: Double = 0.0,
    val slipUrl: String = "",
    val status: String = "",
    val rejectionReason: String = "",
    val createdAt: Long = 0L
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentVerificationScreen(
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
        mutableStateOf<List<PaymentRecord>>(emptyList())
    }
    var loading by remember(roomId) { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var processingId by remember { mutableStateOf<String?>(null) }

    var previewUrl by remember { mutableStateOf<String?>(null) }
    var rejectingPayment by remember { mutableStateOf<PaymentRecord?>(null) }
    var rejectionReason by remember { mutableStateOf("") }

    DisposableEffect(roomId) {
        loading = true

        val registration: ListenerRegistration =
            db.collection("payments")
                .whereEqualTo("roomId", roomId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        errorMessage = error.localizedMessage
                            ?: "โหลดข้อมูลการชำระเงินไม่สำเร็จ"
                        loading = false
                    } else {
                        payments = snapshot?.documents
                            ?.mapNotNull { doc ->
                                val amount = doc.getDouble("amount")
                                    ?: doc.getLong("amount")?.toDouble()
                                    ?: 0.0

                                PaymentRecord(
                                    paymentId = doc.getString("paymentId")
                                        ?: doc.id,
                                    uid = doc.getString("uid").orEmpty(),
                                    billId = doc.getString("billId").orEmpty(),
                                    roomId = doc.getString("roomId").orEmpty(),
                                    amount = amount,
                                    slipUrl = doc.getString("slipUrl").orEmpty(),
                                    status = doc.getString("status").orEmpty(),
                                    rejectionReason = doc
                                        .getString("rejectionReason").orEmpty(),
                                    createdAt = doc.getLong("createdAt") ?: 0L
                                )
                            }
                            ?.sortedByDescending { it.createdAt }
                            .orEmpty()

                        errorMessage = null
                        loading = false
                    }
                }

        onDispose { registration.remove() }
    }

    val moneyFormat = remember {
        NumberFormat.getNumberInstance(Locale("th", "TH")).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "ตรวจสอบการชำระเงิน",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "ห้อง $roomNumber",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "กลับ"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            loading = true
                            db.collection("payments")
                                .whereEqualTo("roomId", roomId)
                                .get()
                                .addOnSuccessListener { snapshot ->
                                    payments = snapshot.documents.mapNotNull { doc ->
                                        val amount = doc.getDouble("amount")
                                            ?: doc.getLong("amount")?.toDouble()
                                            ?: 0.0

                                        PaymentRecord(
                                            paymentId = doc.getString("paymentId")
                                                ?: doc.id,
                                            uid = doc.getString("uid").orEmpty(),
                                            billId = doc.getString("billId").orEmpty(),
                                            roomId = doc.getString("roomId").orEmpty(),
                                            amount = amount,
                                            slipUrl = doc.getString("slipUrl").orEmpty(),
                                            status = doc.getString("status").orEmpty(),
                                            rejectionReason = doc
                                                .getString("rejectionReason").orEmpty(),
                                            createdAt = doc.getLong("createdAt") ?: 0L
                                        )
                                    }.sortedByDescending { it.createdAt }
                                    loading = false
                                    errorMessage = null
                                }
                                .addOnFailureListener { e ->
                                    loading = false
                                    errorMessage = e.localizedMessage
                                        ?: "โหลดข้อมูลไม่สำเร็จ"
                                }
                        }
                    ) {
                        Icon(
                            Icons.Outlined.Refresh,
                            contentDescription = "รีเฟรช"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = VerifyNavy,
                    navigationIconContentColor = VerifyNavy,
                    actionIconContentColor = VerifyNavy
                )
            )
        },
        containerColor = VerifyBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("รายการชำระเงินทั้งหมด", color = Color.Gray)
                        Text(
                            "${payments.size} รายการ",
                            style = MaterialTheme.typography.headlineSmall,
                            color = VerifyNavy,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    val pendingCount = payments.count {
                        it.status == "under_review"
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("รอตรวจสอบ", color = Color.Gray)
                        Text(
                            "$pendingCount รายการ",
                            color = if (pendingCount > 0) VerifyGold else VerifyGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            errorMessage?.let {
                Text(it, color = VerifyRed)
                Spacer(Modifier.height(8.dp))
            }
            message?.let {
                Text(it, color = VerifyGreen)
                Spacer(Modifier.height(8.dp))
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
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "ยังไม่มีรายการชำระเงินของห้องนี้",
                            color = Color.Gray
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 20.dp)
                    ) {
                        items(payments, key = { it.paymentId }) { payment ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "ยอดชำระ ฿${moneyFormat.format(payment.amount)}",
                                                color = VerifyNavy,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Text(
                                                "รหัสบิล: ${payment.billId}",
                                                color = Color.Gray,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }

                                        val statusColor = when (payment.status) {
                                            "approved" -> VerifyGreen
                                            "rejected" -> VerifyRed
                                            "under_review" -> Color(0xFFB7791F)
                                            else -> Color.Gray
                                        }
                                        Surface(
                                            color = statusColor.copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Text(
                                                text = when (payment.status) {
                                                    "under_review" -> "รอตรวจสอบ"
                                                    "approved" -> "อนุมัติแล้ว"
                                                    "rejected" -> "ปฏิเสธ"
                                                    else -> payment.status
                                                },
                                                color = statusColor,
                                                modifier = Modifier.padding(
                                                    horizontal = 10.dp,
                                                    vertical = 5.dp
                                                ),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    if (payment.slipUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = payment.slipUrl,
                                            contentDescription = "สลิปการชำระเงิน",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 220.dp),
                                            contentScale = ContentScale.Fit
                                        )

                                        OutlinedButton(
                                            onClick = { previewUrl = payment.slipUrl },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("เปิดดูสลิปขนาดใหญ่")
                                        }
                                    } else {
                                        Text(
                                            "ไม่พบ URL ของสลิปในรายการนี้",
                                            color = VerifyRed,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }

                                    if (payment.status == "rejected" &&
                                        payment.rejectionReason.isNotBlank()
                                    ) {
                                        Text(
                                            "เหตุผลที่ปฏิเสธ: ${payment.rejectionReason}",
                                            color = VerifyRed
                                        )
                                    }

                                    if (payment.status == "under_review") {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedButton(
                                                modifier = Modifier.weight(1f),
                                                enabled = processingId == null,
                                                onClick = {
                                                    rejectingPayment = payment
                                                    rejectionReason = ""
                                                },
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    contentColor = VerifyRed
                                                )
                                            ) {
                                                Text("ปฏิเสธ")
                                            }

                                            Button(
                                                modifier = Modifier.weight(1f),
                                                enabled = processingId == null,
                                                onClick = {
                                                    scope.launch {
                                                        processingId = payment.paymentId
                                                        errorMessage = null
                                                        message = null
                                                        try {
                                                            repository.approvePayment(
                                                                payment.paymentId
                                                            )
                                                            message = "อนุมัติการชำระเงินเรียบร้อย"
                                                        } catch (e: Exception) {
                                                            errorMessage = e.localizedMessage
                                                                ?: "อนุมัติไม่สำเร็จ"
                                                        } finally {
                                                            processingId = null
                                                        }
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = VerifyGreen
                                                )
                                            ) {
                                                if (processingId == payment.paymentId) {
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
                    }
                }
            }
        }
    }

    previewUrl?.let { url ->
        Dialog(onDismissRequest = { previewUrl = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AsyncImage(
                        model = url,
                        contentDescription = "สลิปขนาดใหญ่",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 600.dp),
                        contentScale = ContentScale.Fit
                    )
                    TextButton(onClick = { previewUrl = null }) {
                        Text("ปิด")
                    }
                }
            }
        }
    }

    rejectingPayment?.let { payment ->
        AlertDialog(
            onDismissRequest = {
                if (processingId == null) rejectingPayment = null
            },
            title = { Text("ปฏิเสธการชำระเงิน") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("กรุณาระบุเหตุผลเพื่อให้ผู้เช่าทราบ")
                    OutlinedTextField(
                        value = rejectionReason,
                        onValueChange = { rejectionReason = it },
                        label = { Text("เหตุผลที่ปฏิเสธ") },
                        placeholder = { Text("เช่น ยอดเงินไม่ตรงกับบิล") },
                        minLines = 2,
                        enabled = processingId == null,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = rejectionReason.isNotBlank() && processingId == null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VerifyRed
                    ),
                    onClick = {
                        scope.launch {
                            processingId = payment.paymentId
                            errorMessage = null
                            message = null
                            try {
                                repository.rejectPayment(
                                    paymentId = payment.paymentId,
                                    reason = rejectionReason
                                )
                                message = "ปฏิเสธคำขอเรียบร้อย"
                                rejectingPayment = null
                            } catch (e: Exception) {
                                errorMessage = e.localizedMessage
                                    ?: "ปฏิเสธไม่สำเร็จ"
                            } finally {
                                processingId = null
                            }
                        }
                    }
                ) {
                    Text("ยืนยันปฏิเสธ")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = processingId == null,
                    onClick = { rejectingPayment = null }
                ) {
                    Text("ยกเลิก")
                }
            }
        )
    }
}