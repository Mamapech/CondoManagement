package com.example.condomanagement.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val MaintenanceNavy = Color(0xFF192F50)
private val MaintenanceBackground = Color(0xFFF3F3F3)
private val MaintenanceBlue = Color(0xFF3869B7)
private val MaintenanceRed = Color(0xFFDC4545)

private data class MaintenanceCategory(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val background: Color
)

private data class MaintenanceRequest(
    val id: String = "",
    val uid: String = "",
    val roomId: String = "",
    val category: String = "",
    val title: String = "",
    val description: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val status: String = "pending"
)

private val maintenanceCategories = listOf(
    MaintenanceCategory(
        id = "plumbing",
        title = "ซ่อมประปา",
        description = "ท่อน้ำ ก๊อกน้ำ น้ำรั่ว",
        icon = Icons.Outlined.WaterDrop,
        color = Color(0xFF2563EB),
        background = Color(0xFFEFF6FF)
    ),
    MaintenanceCategory(
        id = "electrical",
        title = "ซ่อมไฟฟ้า",
        description = "ไฟดับ ปลั๊กไฟ สวิตช์",
        icon = Icons.Outlined.Bolt,
        color = Color(0xFFB45309),
        background = Color(0xFFFEF3C7)
    ),
    MaintenanceCategory(
        id = "furniture",
        title = "เฟอร์นิเจอร์",
        description = "เตียง ตู้ โต๊ะ เก้าอี้",
        icon = Icons.Outlined.Build,
        color = Color(0xFF7E22CE),
        background = Color(0xFFF3E8FF)
    ),
    MaintenanceCategory(
        id = "other",
        title = "อื่น ๆ",
        description = "ปัญหาอื่นภายในห้อง",
        icon = Icons.Outlined.MoreHoriz,
        color = Color(0xFF334155),
        background = Color(0xFFF1F5F9)
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceScreen() {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var requests by remember {
        mutableStateOf<List<MaintenanceRequest>>(emptyList())
    }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }

    var error by remember { mutableStateOf<String?>(null) }
    var userRoomId by remember { mutableStateOf<String?>(null) }
    var checkingRoom by remember { mutableStateOf(true) }
    var roomCheckError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            loading = false
            error = "กรุณาเข้าสู่ระบบก่อน"
            onDispose { }
        } else {
            val registration = db.collection("maintenanceRequests")
                .whereEqualTo("uid", uid)
                .addSnapshotListener { snapshot, exception ->
                    if (exception != null) {
                        error = exception.localizedMessage
                            ?: "โหลดสถานะแจ้งซ่อมไม่สำเร็จ"
                        loading = false
                    } else if (snapshot != null) {
                        requests = snapshot.documents.map { doc ->
                            MaintenanceRequest(
                                id = doc.id,
                                uid = doc.getString("uid").orEmpty(),
                                roomId = doc.getString("roomId").orEmpty(),
                                category = doc.getString("category").orEmpty(),
                                title = doc.getString("title").orEmpty(),
                                description = doc.getString("description").orEmpty(),
                                createdAt = doc.getLong("createdAt") ?: 0L,
                                updatedAt = doc.getLong("updatedAt") ?: 0L,
                                status = doc.getString("status") ?: "pending"
                            )
                        }.sortedByDescending { it.createdAt }

                        error = null
                        loading = false
                    }
                }

            onDispose {
                registration.remove()
            }
        }
    }

    LaunchedEffect(Unit) {
        checkingRoom = true
        roomCheckError = null

        val user = auth.currentUser

        if (user == null) {
            userRoomId = null
            roomCheckError = "กรุณาเข้าสู่ระบบก่อน"
            checkingRoom = false
        } else {
            try {
                val userSnapshot = db.collection("users")
                    .document(user.uid)
                    .get(com.google.firebase.firestore.Source.SERVER)
                    .await()

                userRoomId = userSnapshot
                    .getString("roomId")
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }

                checkingRoom = false
            } catch (e: Exception) {
                userRoomId = null
                roomCheckError =
                    "ตรวจสอบห้องพักไม่สำเร็จ กรุณาลองใหม่"
                checkingRoom = false
            }
        }
    }

    var selectedCategory by remember {
        mutableStateOf<MaintenanceCategory?>(null)
    }
    var editingRequest by remember {
        mutableStateOf<MaintenanceRequest?>(null)
    }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var expandedRequestId by remember { mutableStateOf<String?>(null) }

    fun loadRequests() {
        scope.launch {
            loading = true
            error = null

            try {
                val uid = auth.currentUser?.uid
                    ?: throw Exception("กรุณาเข้าสู่ระบบก่อน")

                val snapshot = db.collection("maintenanceRequests")
                    .whereEqualTo("uid", uid)
                    .get()
                    .await()

                requests = snapshot.documents.map { doc ->
                    MaintenanceRequest(
                        id = doc.id,
                        uid = doc.getString("uid").orEmpty(),
                        roomId = doc.getString("roomId").orEmpty(),
                        category = doc.getString("category").orEmpty(),
                        title = doc.getString("title").orEmpty(),
                        description = doc.getString("description").orEmpty(),
                        createdAt = doc.getLong("createdAt") ?: 0L,
                        updatedAt = doc.getLong("updatedAt") ?: 0L,
                        status = doc.getString("status") ?: "pending"
                    )
                }.sortedByDescending { it.createdAt }
            } catch (e: Exception) {
                error = e.localizedMessage ?: "โหลดประวัติไม่สำเร็จ"
            } finally {
                loading = false
            }

        }
    }

    LaunchedEffect(Unit) {
        loadRequests()
    }

    fun openCreateForm(category: MaintenanceCategory) {
        selectedCategory = category
        editingRequest = null
        title = ""
        description = ""
    }

    fun openEditForm(request: MaintenanceRequest) {
        selectedCategory = maintenanceCategories.firstOrNull {
            it.id == request.category
        } ?: maintenanceCategories.last()

        editingRequest = request
        title = request.title
        description = request.description
        expandedRequestId = null
    }

    fun saveRequest() {
        if (title.isBlank()) {
            scope.launch {
                snackbarHostState.showSnackbar("กรุณากรอกหัวข้อปัญหา")
            }
            return
        }

        if (description.isBlank()) {
            scope.launch {
                snackbarHostState.showSnackbar("กรุณากรอกรายละเอียดปัญหา")
            }
            return
        }

        val category = selectedCategory ?: return

        scope.launch {
            saving = true

            try {
                val user = auth.currentUser
                    ?: throw Exception("กรุณาเข้าสู่ระบบก่อน")


                val userSnapshot = db.collection("users")
                    .document(user.uid)
                    .get(
                        com.google.firebase.firestore.Source.SERVER
                    )
                    .await()

                val roomId = userSnapshot
                    .getString("roomId")
                    ?.trim()
                    .orEmpty()

                if (roomId.isBlank()) {
                    throw Exception(
                        "คุณยังไม่มีห้องพัก จึงไม่สามารถแจ้งซ่อมได้ กรุณาติดต่อผู้ดูแลคอนโด"
                    )
                }


                val now = System.currentTimeMillis()
                val oldRequest = editingRequest

                if (oldRequest == null) {
                    val ref = db.collection("maintenanceRequests").document()

                    val data = hashMapOf(
                        "requestId" to ref.id,
                        "uid" to user.uid,
                        "roomId" to roomId,
                        "category" to category.id,
                        "title" to title.trim(),
                        "description" to description.trim(),
                        "createdAt" to now,
                        "updatedAt" to now
                    )

                    ref.set(data).await()
                } else {
                    val ref = db.collection("maintenanceRequests")
                        .document(oldRequest.id)

                    val current = ref.get().await()

                    if (current.getString("uid") != user.uid) {
                        throw Exception("คุณไม่มีสิทธิ์แก้ไขรายการนี้")
                    }

                    ref.update(
                        mapOf(
                            "category" to category.id,
                            "title" to title.trim(),
                            "description" to description.trim(),
                            "updatedAt" to now
                        )
                    ).await()
                }

                selectedCategory = null
                editingRequest = null
                title = ""
                description = ""

                loadRequests()

                snackbarHostState.showSnackbar(
                    if (oldRequest == null) {
                        "ส่งคำขอแจ้งซ่อมเรียบร้อยแล้ว"
                    } else {
                        "บันทึกการแก้ไขเรียบร้อยแล้ว"
                    }
                )
            } catch (e: Exception) {
                snackbarHostState.showSnackbar(
                    e.localizedMessage ?: "บันทึกข้อมูลไม่สำเร็จ"
                )
            } finally {
                saving = false
            }
        }
    }

    Scaffold(
        containerColor = MaintenanceBackground,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(
                top = 18.dp,
                bottom = 28.dp
            )
        ) {
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = "แจ้งซ่อม",
                        color = MaintenanceNavy,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "เลือกประเภทปัญหาที่ต้องการแจ้ง",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )

                }
            }


            item {
                when {
                    checkingRoom -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaintenanceNavy
                            )
                        }
                    }

                    roomCheckError != null -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    roomCheckError.orEmpty(),
                                    color = MaintenanceRed
                                )

                                TextButton(
                                    onClick = {
                                        scope.launch {
                                            checkingRoom = true
                                            roomCheckError = null

                                            try {
                                                val user = auth.currentUser
                                                    ?: throw Exception(
                                                        "กรุณาเข้าสู่ระบบก่อน"
                                                    )

                                                val snapshot = db
                                                    .collection("users")
                                                    .document(user.uid)
                                                    .get(
                                                        com.google.firebase.firestore.Source.SERVER
                                                    )
                                                    .await()

                                                userRoomId = snapshot
                                                    .getString("roomId")
                                                    ?.trim()
                                                    ?.takeIf { it.isNotBlank() }
                                            } catch (e: Exception) {
                                                roomCheckError =
                                                    "ตรวจสอบห้องพักไม่สำเร็จ กรุณาลองใหม่"
                                            } finally {
                                                checkingRoom = false
                                            }
                                        }
                                    }
                                ) {
                                    Text("ลองอีกครั้ง")
                                }
                            }
                        }
                    }

                    userRoomId.isNullOrBlank() -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(22.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Build,
                                    contentDescription = null,
                                    tint = MaintenanceBlue,
                                    modifier = Modifier.size(40.dp)
                                )

                                Text(
                                    text = "ยังไม่มีห้องพัก",
                                    color = MaintenanceNavy,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )

                                Text(
                                    text = "คุณต้องมีห้องพักที่ได้รับการจัดสรรก่อน จึงจะสามารถแจ้งซ่อมได้ กรุณาติดต่อผู้ดูแลคอนโดเพื่อขอจัดสรรห้องพัก",
                                    color = Color.Gray,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    else -> {
                        CategoryGrid(
                            onSelect = { category ->
                                openCreateForm(category)
                            }
                        )
                    }
                }
            }


            item {
                HorizontalDivider(color = Color(0xFFE0E0E0))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "ประวัติการแจ้งซ่อม",
                            color = MaintenanceNavy,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "รายการแจ้งซ่อมทั้งหมดของคุณ",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }

                    TextButton(
                        onClick = { loadRequests() }
                    ) {
                        Text("Refresh")
                    }
                }
            }

            if (loading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(25.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaintenanceNavy
                        )
                    }
                }
            } else if (error != null) {
                item {
                    Text(
                        text = "โหลดข้อมูลไม่สำเร็จ: $error",
                        color = MaintenanceRed
                    )
                }
            } else if (requests.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(25.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Build,
                                contentDescription = null,
                                tint = MaintenanceBlue,
                                modifier = Modifier.size(38.dp)
                            )

                            Spacer(Modifier.height(10.dp))

                            Text(
                                text = "ยังไม่มีประวัติการแจ้งซ่อม",
                                color = MaintenanceNavy,
                                fontWeight = FontWeight.SemiBold
                            )

                            Text(
                                text = "เลือกประเภทด้านบนเพื่อแจ้งปัญหา",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            } else {
                items(
                    items = requests,
                    key = { it.id }
                ) { request ->
                    MaintenanceHistoryCard(
                        request = request,
                        expanded = expandedRequestId == request.id,
                        onSwipeRight = {
                            expandedRequestId = request.id
                        },
                        onSwipeLeft = {
                            if (expandedRequestId == request.id) {
                                expandedRequestId = null
                            }
                        },
                        onEdit = {
                            openEditForm(request)
                        }
                    )
                }
            }
        }
    }

    if (selectedCategory != null) {
        val category = selectedCategory!!

        AlertDialog(
            onDismissRequest = {
                if (!saving) {
                    selectedCategory = null
                    editingRequest = null
                }
            },
            title = {
                Text(
                    text = if (editingRequest == null) {
                        "แจ้ง${category.title}"
                    } else {
                        "แก้ไขรายการแจ้งซ่อม"
                    },
                    color = MaintenanceNavy,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "ประเภท: ${category.title}",
                        color = category.color,
                        fontWeight = FontWeight.SemiBold
                    )

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("หัวข้อปัญหา") },
                        placeholder = {
                            Text("เช่น ก๊อกน้ำรั่วในห้องน้ำ")
                        },
                        singleLine = true,
                        enabled = !saving,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            keyboardType = KeyboardType.Text
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("รายละเอียดปัญหา") },
                        placeholder = {
                            Text("อธิบายว่าปัญหาเกิดขึ้นอย่างไร")
                        },
                        minLines = 3,
                        maxLines = 5,
                        enabled = !saving,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            keyboardType = KeyboardType.Text
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { saveRequest() },
                    enabled = !saving,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaintenanceNavy
                    )
                ) {
                    if (saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )

                        Spacer(Modifier.width(8.dp))
                    }

                    Text(
                        if (editingRequest == null) "ส่งคำขอ" else "บันทึก"
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        selectedCategory = null
                        editingRequest = null
                    },
                    enabled = !saving
                ) {
                    Text("ยกเลิก")
                }
            }
        )
    }
}

@Composable
private fun CategoryGrid(
    onSelect: (MaintenanceCategory) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        maintenanceCategories.chunked(2).forEach { rowCategories ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowCategories.forEach { category ->
                    Card(
                        onClick = { onSelect(category) },
                        modifier = Modifier
                            .weight(1f)
                            .height(145.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 0.dp
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(15.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                shape = RoundedCornerShape(13.dp),
                                color = category.background,
                                modifier = Modifier.size(43.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = category.icon,
                                        contentDescription = null,
                                        tint = category.color,
                                        modifier = Modifier.size(25.dp)
                                    )
                                }
                            }

                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = category.title,
                                    color = MaintenanceNavy,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )

                                Text(
                                    text = category.description,
                                    color = Color.Gray,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MaintenanceStatusBadge(status: String) {
    val label = when (status) {
        "pending" -> "รอดำเนินการ"
        "in_progress" -> "กำลังซ่อม"
        "completed" -> "ซ่อมเสร็จแล้ว"
        else -> "รอดำเนินการ"
    }

    val color = when (status) {
        "completed" -> Color(0xFF16845B)
        "in_progress" -> Color(0xFF3869B7)
        else -> Color(0xFFD9822B)
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(
                horizontal = 10.dp,
                vertical = 6.dp
            ),
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun MaintenanceHistoryCard(
    request: MaintenanceRequest,
    expanded: Boolean,
    onSwipeRight: () -> Unit,
    onSwipeLeft: () -> Unit,
    onEdit: () -> Unit
) {
    val category = maintenanceCategories.firstOrNull {
        it.id == request.category
    } ?: maintenanceCategories.last()

    val density = LocalDensity.current
    val revealWidth = 92.dp
    val revealWidthPx = with(density) { revealWidth.toPx() }

    var offsetX by remember(request.id) {
        mutableFloatStateOf(
            if (expanded) revealWidthPx else 0f
        )
    }

    LaunchedEffect(expanded) {
        offsetX = if (expanded) revealWidthPx else 0f
    }

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            onClick = onEdit,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(revealWidth)
                .matchParentSize(),
            shape = RoundedCornerShape(18.dp),
            color = MaintenanceNavy,
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "แก้ไขรายการ",
                    modifier = Modifier.size(25.dp)
                )

                Spacer(Modifier.height(5.dp))

                Text(
                    text = "แก้ไข",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset {
                    IntOffset(
                        x = offsetX.roundToInt(),
                        y = 0
                    )
                }
                .pointerInput(request.id, expanded, revealWidthPx) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            offsetX = (offsetX + dragAmount)
                                .coerceIn(0f, revealWidthPx)
                        },
                        onDragEnd = {
                            if (offsetX >= revealWidthPx * 0.35f) {
                                offsetX = revealWidthPx
                                onSwipeRight()
                            } else {
                                offsetX = 0f
                                onSwipeLeft()
                            }
                        },
                        onDragCancel = {
                            offsetX = 0f
                            onSwipeLeft()
                        }
                    )
                },
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 0.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(15.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = category.background,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = category.icon,
                                contentDescription = null,
                                tint = category.color,
                                modifier = Modifier.size(25.dp)
                            )
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = request.title,
                            color = MaintenanceNavy,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = category.title,
                            color = category.color,
                            fontSize = 12.sp
                        )

                        Spacer(Modifier.height(6.dp))

                        MaintenanceStatusBadge(request.status)

                        Text(
                            text = "ห้อง ${request.roomId.ifBlank { "-" }}",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }

                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = "เลื่อนไปทางขวาเพื่อแก้ไข",
                        tint = Color.LightGray
                    )
                }

                if (request.description.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = request.description,
                        color = Color.DarkGray,
                        fontSize = 13.sp
                    )
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "วันที่แจ้ง: ${
                        if (request.createdAt > 0L) {
                            SimpleDateFormat(
                                "dd/MM/yyyy HH:mm",
                                Locale("th", "TH")
                            ).format(Date(request.createdAt))
                        } else {
                            "-"
                        }
                    }",
                    color = Color.Gray,
                    fontSize = 10.sp
                )
            }
        }
    }
}