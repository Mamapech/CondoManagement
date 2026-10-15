

package com.example.condomanagement.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val AnnouncementNavy = Color(0xFF192F50)
private val AnnouncementBackground = Color(0xFFF3F3F3)
private val AnnouncementGold = Color(0xFFD4AF37)
private val AnnouncementRed = Color(0xFFDC4545)
private val AnnouncementGreen = Color(0xFF16845B)
private val AnnouncementText = Color(0xFF252B36)
private val AnnouncementMuted = Color(0xFF7A8493)

private data class AnnouncementItem(
    val id: String,
    val title: String,
    val description: String,
    val createdAt: Long
)

private fun isAdminRole(role: String?): Boolean {
    val r = role?.trim()?.lowercase(Locale.ROOT)
    return r == "admin" || r == "owner"
}

/**
 * role: ถ้าไม่ส่งมา (หรือส่งค่าว่าง) หน้านี้จะอ่าน role จาก Firestore users/{uid} เอง
 * จึงใช้งานได้ทันทีแม้ Admin Dashboard ยังเรียก AnnouncementsScreen(onBack = ...) แบบเดิม
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementsScreen(
    onBack: () -> Unit,
    role: String = ""
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val scope = rememberCoroutineScope()

    var fetchedRole by remember { mutableStateOf<String?>(null) }
    val isAdmin = isAdminRole(role) || isAdminRole(fetchedRole)

    var announcements by remember { mutableStateOf<List<AnnouncementItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var publishing by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    // อ่าน role จาก Firestore
    LaunchedEffect(auth.currentUser?.uid) {
        val uid = auth.currentUser?.uid ?: return@LaunchedEffect
        try {
            fetchedRole = db.collection("users").document(uid).get().await().getString("role")
        } catch (_: Exception) {
            // ใช้ role ที่ส่งเข้ามาแทน
        }
    }

    // โหลดประกาศแบบเรียลไทม์
    DisposableEffect(Unit) {
        val registration = db.collection("announcements")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    loadError = "ไม่สามารถโหลดประกาศได้: ${error.localizedMessage ?: "กรุณาตรวจสอบการเชื่อมต่อ"}"
                    isLoading = false
                } else {
                    announcements = snapshot?.documents
                        ?.map { document ->
                            AnnouncementItem(
                                id = document.id,
                                title = document.getString("title") ?: "ประกาศ",
                                description = document.getString("message")
                                    ?: document.getString("description")
                                    ?: document.getString("content")
                                    ?: "",
                                createdAt = document.getLong("createdAt") ?: 0L
                            )
                        }
                        ?.sortedByDescending { it.createdAt }
                        ?: emptyList()
                    loadError = null
                    isLoading = false
                }
            }
        onDispose { registration.remove() }
    }

    fun publish() {
        if (title.isBlank() || message.isBlank()) {
            formError = "กรุณากรอกหัวข้อและรายละเอียดประกาศ"
            successMessage = null
            return
        }
        scope.launch {
            publishing = true
            formError = null
            successMessage = null
            try {
                val user = auth.currentUser ?: throw Exception("กรุณาเข้าสู่ระบบใหม่")

                // ตรวจสิทธิ์ซ้ำก่อนเขียน Firestore
                val profile = db.collection("users").document(user.uid).get().await()
                if (!profile.exists() || !isAdminRole(profile.getString("role"))) {
                    throw Exception("บัญชีนี้ไม่มีสิทธิ์เผยแพร่ประกาศ")
                }

                db.collection("announcements")
                    .add(
                        hashMapOf(
                            "title" to title.trim(),
                            "message" to message.trim(),
                            "createdAt" to System.currentTimeMillis(),
                            "createdBy" to user.uid
                        )
                    )
                    .await()

                title = ""
                message = ""
                successMessage = "เผยแพร่ประกาศเรียบร้อยแล้ว"
            } catch (e: Exception) {
                formError = e.localizedMessage ?: "เผยแพร่ประกาศไม่สำเร็จ"
            } finally {
                publishing = false
            }
        }
    }

    Scaffold(
        containerColor = AnnouncementBackground,
        topBar = {
            TopAppBar(
                // ไม่เว้นที่ status bar ซ้ำ เพราะ Dashboard จัดการให้แล้ว
                windowInsets = WindowInsets(0, 0, 0, 0),
                modifier = Modifier.heightIn(min = 64.dp),
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "ประกาศจากนิติบุคคล",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isAdmin) "จัดการประกาศสำหรับลูกบ้าน" else "ข่าวสารสำหรับลูกบ้าน",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.78f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "ย้อนกลับ",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AnnouncementNavy)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ฟอร์มสร้างประกาศ (เฉพาะ admin/owner)
            if (isAdmin) {
                item {
                    PublishFormCard(
                        title = title,
                        message = message,
                        publishing = publishing,
                        onTitleChange = { title = it },
                        onMessageChange = { message = it },
                        onPublish = ::publish
                    )
                }

                if (formError != null) {
                    item {
                        Text(
                            text = formError.orEmpty(),
                            color = AnnouncementRed,
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
            }

            // หัวข้อรายการ
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ข่าวสารล่าสุด",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = AnnouncementNavy
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = "ติดตามข่าวสารและประกาศของคอนโด",
                            fontSize = 12.sp,
                            color = AnnouncementMuted
                        )
                    }
                    Surface(shape = RoundedCornerShape(12.dp), color = Color.White) {
                        Text(
                            text = "${announcements.size} รายการ",
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AnnouncementNavy
                        )
                    }
                }
            }

            when {
                isLoading -> item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AnnouncementNavy, strokeWidth = 3.dp)
                    }
                }

                loadError != null -> item {
                    EmptyStateCard(
                        title = "โหลดประกาศไม่สำเร็จ",
                        message = loadError.orEmpty(),
                        icon = {
                            Icon(
                                Icons.Outlined.Refresh,
                                contentDescription = null,
                                tint = AnnouncementRed,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    )
                }

                announcements.isEmpty() -> item {
                    EmptyStateCard(
                        title = "ยังไม่มีประกาศ",
                        message = "เมื่อมีข่าวสารจากนิติบุคคล จะแสดงในหน้านี้",
                        icon = {
                            Icon(
                                Icons.Outlined.Campaign,
                                contentDescription = null,
                                tint = AnnouncementGold,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    )
                }

                else -> items(items = announcements, key = { it.id }) { announcement ->
                    AnnouncementCard(announcement)
                }
            }
        }
    }
}

@Composable
private fun PublishFormCard(
    title: String,
    message: String,
    publishing: Boolean,
    onTitleChange: (String) -> Unit,
    onMessageChange: (String) -> Unit,
    onPublish: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                    imageVector = Icons.Outlined.Campaign,
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
                onValueChange = onTitleChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("หัวข้อประกาศ") },
                placeholder = { Text("เช่น แจ้งปิดปรับปรุงลิฟต์") },
                singleLine = true,
                enabled = !publishing
            )

            OutlinedTextField(
                value = message,
                onValueChange = onMessageChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 150.dp),
                label = { Text("รายละเอียดประกาศ") },
                placeholder = { Text("พิมพ์ข้อความที่ต้องการแจ้งผู้เช่า") },
                minLines = 4,
                enabled = !publishing
            )

            Text(
                text = "กรุณาตรวจสอบข้อความก่อนเผยแพร่",
                color = AnnouncementMuted,
                fontSize = 12.sp
            )

            Button(
                onClick = onPublish,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = !publishing,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AnnouncementNavy)
            ) {
                if (publishing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Text("กำลังเผยแพร่...", modifier = Modifier.padding(start = 8.dp))
                } else {
                    Text("เผยแพร่ประกาศ")
                }
            }
        }
    }
}

@Composable
private fun AnnouncementCard(announcement: AnnouncementItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(13.dp),
                    color = Color(0xFFFFF6DC)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Campaign,
                            contentDescription = null,
                            tint = AnnouncementGold,
                            modifier = Modifier.size(25.dp)
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = announcement.title,
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = AnnouncementNavy
                    )

                    if (announcement.createdAt > 0L) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarToday,
                                contentDescription = null,
                                tint = AnnouncementMuted,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = formatAnnouncementDate(announcement.createdAt),
                                fontSize = 11.sp,
                                color = AnnouncementMuted
                            )
                        }
                    }
                }
            }

            if (announcement.description.isNotBlank()) {
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFFE9ECF1), thickness = 1.dp)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = announcement.description,
                    fontSize = 14.sp,
                    lineHeight = 23.sp,
                    color = AnnouncementText
                )
            }
        }
    }
}

@Composable
private fun EmptyStateCard(
    title: String,
    message: String,
    icon: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFF5F1E7),
                modifier = Modifier.size(68.dp)
            ) {
                Box(contentAlignment = Alignment.Center) { icon() }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = AnnouncementNavy
            )

            Spacer(Modifier.height(7.dp))

            Text(
                text = message,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = AnnouncementMuted
            )
        }
    }
}

private fun formatAnnouncementDate(timestamp: Long): String =
    try {
        SimpleDateFormat("dd/MM/yyyy · HH:mm น.", Locale("th", "TH")).format(Date(timestamp))
    } catch (_: Exception) {
        "ไม่ระบุวันที่"
    }

