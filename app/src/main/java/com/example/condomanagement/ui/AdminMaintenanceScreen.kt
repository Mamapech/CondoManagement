
package com.example.condomanagement.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val AdminRepairNavy = Color(0xFF192F50)
private val AdminRepairBackground = Color(0xFFF3F4F8)
private val AdminRepairGreen = Color(0xFF16845B)
private val AdminRepairOrange = Color(0xFFD9822B)
private val AdminRepairRed = Color(0xFFDC4545)

private data class AdminRepairRequest(
    val id: String,
    val uid: String,
    val roomId: String,
    val category: String,
    val title: String,
    val description: String,
    val createdAt: Long,
    val status: String
)

private val repairStatuses = listOf(
    "pending" to "รอดำเนินการ",
    "in_progress" to "กำลังซ่อม",
    "completed" to "ซ่อมเสร็จแล้ว"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMaintenanceScreen() {
    val db = remember { FirebaseFirestore.getInstance() }

    var requests by remember {
        mutableStateOf<List<AdminRepairRequest>>(emptyList())
    }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedRequest by remember {
        mutableStateOf<AdminRepairRequest?>(null)
    }
    var savingStatus by remember { mutableStateOf(false) }
    var statusError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        val registration: ListenerRegistration =
            db.collection("maintenanceRequests")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        errorMessage =
                            "โหลดรายการแจ้งซ่อมไม่สำเร็จ: ${error.localizedMessage}"
                        loading = false
                    } else {
                        requests = snapshot?.documents.orEmpty()
                            .map { doc ->
                                AdminRepairRequest(
                                    id = doc.id,
                                    uid = doc.getString("uid").orEmpty(),
                                    roomId = doc.getString("roomId").orEmpty(),
                                    category = doc.getString("category").orEmpty(),
                                    title = doc.getString("title").orEmpty(),
                                    description = doc.getString("description").orEmpty(),
                                    createdAt = doc.getLong("createdAt") ?: 0L,
                                    status = doc.getString("status") ?: "pending"
                                )
                            }
                            .sortedByDescending { it.createdAt }

                        errorMessage = null
                        loading = false
                    }
                }

        onDispose {
            registration.remove()
        }
    }

    Scaffold(
        containerColor = AdminRepairBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "จัดการแจ้งซ่อม",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = AdminRepairNavy
                    )
                    Text(
                        "ตรวจสอบปัญหาแยกตามห้องพัก",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }

                IconButton(onClick = {
                    loading = true
                    // Snapshot listener จะอัปเดตรายการให้อัตโนมัติ
                }) {
                    Icon(
                        Icons.Outlined.Refresh,
                        contentDescription = "รีเฟรช"
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                RepairSummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "ทั้งหมด",
                    count = requests.size,
                    color = AdminRepairNavy
                )
                RepairSummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "รอดำเนินการ",
                    count = requests.count {
                        it.status == "pending"
                    },
                    color = AdminRepairOrange
                )
                RepairSummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "กำลังซ่อม",
                    count = requests.count {
                        it.status == "in_progress"
                    },
                    color = Color(0xFF3869B7)
                )
            }

            Spacer(Modifier.height(16.dp))

            when {
                loading && requests.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AdminRepairNavy)
                    }
                }

                errorMessage != null && requests.isEmpty() -> {
                    Text(
                        text = errorMessage.orEmpty(),
                        modifier = Modifier.padding(20.dp),
                        color = AdminRepairRed
                    )
                }

                requests.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.Build,
                                contentDescription = null,
                                modifier = Modifier.size(42.dp),
                                tint = AdminRepairNavy
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "ยังไม่มีรายการแจ้งซ่อม",
                                fontWeight = FontWeight.Bold,
                                color = AdminRepairNavy
                            )
                            Text(
                                "เมื่อมีลูกบ้านส่งคำร้อง รายการจะแสดงที่นี่",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            horizontal = 16.dp,
                            vertical = 4.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(requests, key = { it.id }) { request ->
                            Card(
                                onClick = { selectedRequest = request },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                "ห้อง ${request.roomId.ifBlank { "ไม่ระบุห้อง" }}",
                                                fontSize = 19.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AdminRepairNavy
                                            )
                                            Text(
                                                repairCategoryName(request.category),
                                                color = Color.Gray,
                                                fontSize = 12.sp
                                            )
                                        }

                                        RepairStatusBadge(request.status)
                                    }

                                    HorizontalDivider(color = Color(0xFFE9ECF1))

                                    Text(
                                        request.title.ifBlank { "ไม่มีหัวข้อ" },
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF202938)
                                    )

                                    Text(
                                        request.description.ifBlank {
                                            "ไม่มีรายละเอียดเพิ่มเติม"
                                        },
                                        color = Color.Gray,
                                        fontSize = 13.sp,
                                        maxLines = 2
                                    )

                                    Text(
                                        "วันที่แจ้ง: ${repairDate(request.createdAt)}",
                                        color = Color.Gray,
                                        fontSize = 11.sp
                                    )

                                    Text(
                                        "แตะเพื่อดูรายละเอียดและอัปเดตสถานะ",
                                        color = AdminRepairNavy,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedRequest?.let { request ->
        AlertDialog(
            onDismissRequest = {
                if (!savingStatus) selectedRequest = null
            },
            title = {
                Text(
                    "รายละเอียดแจ้งซ่อม",
                    color = AdminRepairNavy,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "ห้อง: ${request.roomId.ifBlank { "ไม่ระบุห้อง" }}",
                        fontWeight = FontWeight.Bold
                    )
                    Text("ประเภท: ${repairCategoryName(request.category)}")
                    Text("หัวข้อ: ${request.title}")
                    Text("รายละเอียด: ${request.description}")
                    Text("วันที่แจ้ง: ${repairDate(request.createdAt)}")
                    Text(
                        "รหัสผู้แจ้ง: ${request.uid.ifBlank { "ไม่ระบุ" }}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )

                    HorizontalDivider()

                    Text(
                        "เปลี่ยนสถานะงาน",
                        fontWeight = FontWeight.Bold,
                        color = AdminRepairNavy
                    )



                    repairStatuses.forEach { (value, label) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = request.status == value,
                                enabled = !savingStatus,
                                onClick = {
                                    if (!savingStatus && request.status != value) {
                                        savingStatus = true
                                        statusError = null

                                        db.collection("maintenanceRequests")
                                            .document(request.id)
                                            .update("status", value)
                                            .addOnSuccessListener {
                                                selectedRequest = request.copy(
                                                    status = value
                                                )
                                                savingStatus = false
                                                statusError = null
                                            }
                                            .addOnFailureListener { exception ->
                                                statusError =
                                                    "บันทึกสถานะไม่สำเร็จ: ${
                                                        exception.localizedMessage
                                                            ?: "ไม่ทราบสาเหตุ"
                                                    }"
                                                savingStatus = false
                                            }
                                    }
                                }
                            )

                            Text(label)
                        }
                    }

                    statusError?.let { message ->
                        Text(
                            text = message,
                            color = AdminRepairRed,
                            fontSize = 12.sp
                        )
                    }

                    if (savingStatus) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = AdminRepairNavy
                        )
                    }

                }
            },
            confirmButton = {
                TextButton(
                    onClick = { selectedRequest = null },
                    enabled = !savingStatus
                ) {
                    Text("ปิด")
                }
            }
        )
    }
}

@Composable
private fun RepairSummaryCard(
    modifier: Modifier = Modifier,
    title: String,
    count: Int,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                count.toString(),
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                title,
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun RepairStatusBadge(status: String) {
    val label = repairStatuses.firstOrNull {
        it.first == status
    }?.second ?: status

    val color = when (status) {
        "completed" -> AdminRepairGreen
        "in_progress" -> Color(0xFF3869B7)
        else -> AdminRepairOrange
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.10f)
    ) {
        Text(
            label,
            modifier = Modifier.padding(
                horizontal = 10.dp,
                vertical = 6.dp
            ),
            fontSize = 11.sp,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun repairCategoryName(category: String): String =
    when (category) {
        "plumbing" -> "ซ่อมประปา"
        "electrical" -> "ซ่อมไฟฟ้า"
        "furniture" -> "เฟอร์นิเจอร์"
        "other" -> "อื่น ๆ"
        else -> category.ifBlank { "ไม่ระบุประเภท" }
    }

private fun repairDate(timestamp: Long): String {
    if (timestamp <= 0L) return "ไม่ระบุวันที่"

    return SimpleDateFormat(
        "dd/MM/yyyy HH:mm",
        Locale("th", "TH")
    ).format(Date(timestamp))
}
