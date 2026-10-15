
package com.example.condomanagement.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val AnnouncementNavy = Color(0xFF192F50)
private val AnnouncementBackground = Color(0xFFF3F4F8)
private val AnnouncementGreen = Color(0xFF16845B)

private data class CondoAnnouncementItem(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val createdAt: Long = 0L,
    val createdBy: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun AnnouncementScreen(
    onBack: () -> Unit,
    role: String
) {

    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val scope = rememberCoroutineScope()

    // ใช้ Role ที่ส่งมาจาก Dashboard
    val normalizedRole = role.trim().lowercase(Locale.ROOT)
    val isAdmin = normalizedRole == "admin" ||
            normalizedRole == "owner"

    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    var announcements by remember {
        mutableStateOf<List<CondoAnnouncementItem>>(emptyList())
    }

    var loading by remember { mutableStateOf(true) }
    var publishing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    // โหลดประกาศแบบเรียลไทม์
    DisposableEffect(Unit) {
        val registration: ListenerRegistration =
            db.collection("announcements")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        errorMessage =
                            "โหลดประกาศไม่สำเร็จ: ${error.localizedMessage}"
                        loading = false
                    } else {
                        announcements = snapshot?.documents
                            .orEmpty()
                            .map { document ->
                                CondoAnnouncementItem(
                                    id = document.id,
                                    title = document.getString("title").orEmpty(),
                                    message = document.getString("message").orEmpty(),
                                    createdAt = document.getLong("createdAt") ?: 0L,
                                    createdBy = document.getString("createdBy").orEmpty()
                                )
                            }
                            .sortedByDescending { it.createdAt }

                        loading = false
                    }
                }

        onDispose {
            registration.remove()
        }
    }

    Scaffold(
        containerColor = AnnouncementBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "ประกาศข่าวสาร",
                            color = AnnouncementNavy,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isAdmin) {
                                "สำหรับแอดมิน"
                            } else {
                                "ข่าวสารสำหรับผู้เช่า"
                            },
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
                            tint = AnnouncementNavy
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                horizontal = 16.dp,
                vertical = 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ฟอร์มสร้างประกาศสำหรับ Admin/Owner
            if (isAdmin) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Campaign,
                                    contentDescription = null,
                                    tint = AnnouncementNavy
                                )
                                Text(
                                    text = "สร้างประกาศใหม่",
                                    color = AnnouncementNavy,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("หัวข้อประกาศ") },
                                placeholder = {
                                    Text("เช่น แจ้งปิดปรับปรุงลิฟต์")
                                },
                                singleLine = true,
                                enabled = !publishing
                            )

                            OutlinedTextField(
                                value = message,
                                onValueChange = { message = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp),
                                label = { Text("รายละเอียดประกาศ") },
                                placeholder = {
                                    Text("พิมพ์ข้อความที่ต้องการแจ้งผู้เช่า")
                                },
                                minLines = 4,
                                enabled = !publishing
                            )

                            Text(
                                text = "กรุณาตรวจสอบข้อความก่อนเผยแพร่",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )

                            Button(
                                onClick = {
                                    if (title.isBlank() || message.isBlank()) {
                                        errorMessage =
                                            "กรุณากรอกหัวข้อและรายละเอียดประกาศ"
                                        successMessage = null
                                        return@Button
                                    }

                                    scope.launch {
                                        publishing = true
                                        errorMessage = null
                                        successMessage = null

                                        try {
                                            val user = auth.currentUser
                                                ?: throw Exception(
                                                    "กรุณาเข้าสู่ระบบใหม่"
                                                )

                                            // ตรวจสิทธิ์ซ้ำก่อนเขียน Firestore
                                            val profile = db.collection("users")
                                                .document(user.uid)
                                                .get()
                                                .await()

                                            val currentRole = profile
                                                .getString("role")
                                                ?.trim()
                                                ?.lowercase(Locale.ROOT)

                                            if (!profile.exists() ||
                                                (currentRole != "admin" &&
                                                        currentRole != "owner")
                                            ) {
                                                throw Exception(
                                                    "บัญชีนี้ไม่มีสิทธิ์เผยแพร่ประกาศ"
                                                )
                                            }

                                            val data = hashMapOf(
                                                "title" to title.trim(),
                                                "message" to message.trim(),
                                                "createdAt" to
                                                        System.currentTimeMillis(),
                                                "createdBy" to user.uid
                                            )

                                            db.collection("announcements")
                                                .add(data)
                                                .await()

                                            title = ""
                                            message = ""
                                            successMessage =
                                                "เผยแพร่ประกาศเรียบร้อยแล้ว"
                                        } catch (e: Exception) {
                                            errorMessage =
                                                e.localizedMessage
                                                    ?: "เผยแพร่ประกาศไม่สำเร็จ"
                                        } finally {
                                            publishing = false
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                enabled = !publishing,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AnnouncementNavy
                                )
                            ) {
                                if (publishing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Text(
                                        "กำลังเผยแพร่...",
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                } else {
                                    Text("เผยแพร่ประกาศ")
                                }
                            }
                        }
                    }
                }
            }

            if (errorMessage != null) {
                item {
                    Text(
                        text = errorMessage.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp
                    )
                }
            }

            if (successMessage != null) {
                item {
                    Text(
                        text = successMessage.orEmpty(),
                        color = AnnouncementGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            item {
                Text(
                    text = "ประกาศทั้งหมด (${announcements.size})",
                    color = AnnouncementNavy,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (loading) {
                item {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(20.dp)
                            .size(32.dp),
                        color = AnnouncementNavy
                    )
                }
            } else if (announcements.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        )
                    ) {
                        Text(
                            text = "ยังไม่มีประกาศข่าวสาร",
                            modifier = Modifier.padding(24.dp),
                            color = Color.Gray
                        )
                    }
                }
            } else {
                items(
                    items = announcements,
                    key = { it.id }
                ) { announcement ->
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
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Campaign,
                                    contentDescription = null,
                                    tint = AnnouncementNavy
                                )
                                Text(
                                    text = announcement.title,
                                    color = AnnouncementNavy,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = announcement.message,
                                color = Color(0xFF374151),
                                fontSize = 14.sp
                            )

                            Text(
                                text = "เผยแพร่เมื่อ ${
                                    formatAnnouncementDate(announcement.createdAt)
                                }",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatAnnouncementDate(timestamp: Long): String {
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
