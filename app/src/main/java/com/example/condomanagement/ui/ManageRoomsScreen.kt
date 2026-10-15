
package com.example.condomanagement.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.NumberFormat
import java.util.Locale
import androidx.compose.material.icons.filled.Clear

private val AdminNavy = Color(0xFF192F50)
private val AdminGold = Color(0xFFD4AF37)
private val AdminBackground = Color(0xFFF3F4F8)
private val AdminGreen = Color(0xFF16845B)
private val AdminOrange = Color(0xFFD9822B)
private val AdminRed = Color(0xFFDC4545)

private data class AdminRoom(
    val roomId: String = "",
    val roomNumber: String = "",
    val tower: String = "A",
    val floor: Int = 1,
    val monthlyRent: Double = 0.0,
    val username: String = "",
    val tenantUid: String = "",
    val status: String = "available"
)

private data class AdminBillSummary(
    val billId: String = "",
    val roomId: String = "",
    val title: String = "ค่าใช้จ่าย",
    val amount: Double = 0.0,
    val status: String = "pending",
    val dueDate: String = "",
    val rejectionReason: String = ""
)

private data class AdminNavItem(
    val title: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageRoomsScreen(
    onBack: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val scope = rememberCoroutineScope()
    var showAddBillDialog by remember { mutableStateOf(false) }
    var billTargetRoom by remember { mutableStateOf<AdminRoom?>(null) }
    var rooms by remember { mutableStateOf<List<AdminRoom>>(emptyList()) }
    var bills by remember { mutableStateOf<List<AdminBillSummary>>(emptyList()) }
    var paymentReasons by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedRoom by remember { mutableStateOf<AdminRoom?>(null) }
    var paymentTargetRoom by remember {
        mutableStateOf<AdminRoom?>(null)
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<AdminRoom?>(null) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    val navItems = listOf(
        AdminNavItem("Home", Icons.Outlined.Home),
        AdminNavItem("ห้องพัก", Icons.Outlined.MeetingRoom),
        AdminNavItem("ประกาศ", Icons.Outlined.Campaign),
        AdminNavItem("แจ้งซ่อม", Icons.Outlined.Build),
        AdminNavItem("โปรไฟล์", Icons.Outlined.Person)
    )

    DisposableEffect(Unit) {
        val registration: ListenerRegistration = db.collection("rooms")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    errorMessage = "โหลดห้องพักไม่สำเร็จ: ${error.localizedMessage}"
                    loading = false
                } else {
                    rooms = snapshot?.documents.orEmpty().map { doc ->
                        AdminRoom(
                            roomId = doc.getString("roomId") ?: doc.id,
                            roomNumber = doc.getString("roomNumber") ?: doc.id,
                            tower = doc.getString("tower") ?: "A",
                            floor = (doc.getLong("floor") ?: 1L).toInt(),
                            monthlyRent = doc.getDouble("monthlyRent")
                                ?: (doc.getLong("monthlyRent") ?: 0L).toDouble(),
                            username = doc.getString("username") ?: "",
                            tenantUid = doc.getString("tenantUid") ?: "",
                            status = doc.getString("status") ?: "available"
                        )
                    }.sortedWith(
                        compareBy<AdminRoom> { it.tower }
                            .thenBy { it.floor }
                            .thenBy { it.roomNumber }
                    )
                    loading = false
                    errorMessage = null
                }
            }

        onDispose {
            registration.remove()
        }
    }

    DisposableEffect(Unit) {
        val billsRegistration = db.collection("bills")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    errorMessage = "โหลดสถานะบิลไม่สำเร็จ: ${error.localizedMessage}"
                } else {
                    bills = snapshot?.documents.orEmpty().map { doc ->
                        AdminBillSummary(
                            billId = doc.id,
                            roomId = doc.getString("roomId").orEmpty(),
                            title = doc.getString("title") ?: "ค่าใช้จ่าย",
                            amount = doc.getDouble("amount")
                                ?: (doc.getLong("amount") ?: 0L).toDouble(),
                            status = doc.getString("status") ?: "pending",
                            dueDate = doc.getString("dueDate").orEmpty()
                        )
                    }
                }
            }

        val paymentsRegistration = db.collection("payments")
            .addSnapshotListener { snapshot, error ->
                if (error == null) {
                    paymentReasons = snapshot?.documents.orEmpty()
                        .filter { it.getString("status") == "rejected" }
                        .associate { doc ->
                            (doc.getString("billId") ?: "") to
                                    doc.getString("rejectionReason").orEmpty()
                        }
                }
            }

        onDispose {
            billsRegistration.remove()
            paymentsRegistration.remove()
        }
    }

    val paidBills = bills.count { it.status.equals("paid", true) || it.status.equals("approved", true) }
    val reviewBills = bills.count { it.status.equals("under_review", true) }
    val rejectedBills = bills.count { it.status.equals("rejected", true) }
    val unpaidBills = bills.count {
        it.status !in listOf("paid", "approved", "under_review", "rejected")
    }

    val occupiedRooms = rooms.count {
        it.status.equals("occupied", true) || it.tenantUid.isNotBlank()
    }
    val availableRooms = rooms.size - occupiedRooms

    Scaffold(
        containerColor = AdminBackground,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AdminNavy)
                    .statusBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(13.dp),
                        color = Color.White.copy(alpha = 0.13f),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.Apartment,
                                contentDescription = null,
                                tint = AdminGold,
                                modifier = Modifier.size(29.dp)
                            )
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(Modifier.weight(1f)) {
                        Text(
                            "CondoCare Admin",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "PROPERTY MANAGEMENT",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 10.sp,
                            letterSpacing = 1.sp
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { showLogoutDialog = true }) {
                            Icon(
                                Icons.Outlined.Logout,
                                contentDescription = "Logout",
                                tint = Color.White
                            )
                        }
                        Text("Logout", color = Color.White, fontSize = 10.sp)
                    }
                }

                Spacer(Modifier.height(15.dp))

                Text(
                    "ยินดีต้อนรับ, Admin",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    when (selectedTab) {
                        0 -> "ภาพรวมคอนโด"
                        1 -> "จัดการห้องพัก"
                        2 -> "ประกาศจากนิติบุคคล"
                        3 -> "จัดการแจ้งซ่อม"
                        else -> "โปรไฟล์แอดมิน"
                    },
                    color = Color.White,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                Box(
                    Modifier
                        .width(46.dp)
                        .height(3.dp)
                        .background(AdminGold, RoundedCornerShape(50))
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 4.dp
            ) {
                navItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(item.icon, contentDescription = item.title)
                        },
                        label = {
                            Text(item.title, fontSize = 10.sp)
                        },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AdminNavy,
                            selectedTextColor = AdminNavy,
                            indicatorColor = Color(0xFFE6ECF7),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )
                }
            }
        }
    ) { padding ->
        when (selectedTab) {
            0, 1 -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(
                        horizontal = 16.dp,
                        vertical = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (selectedTab == 0) {
                        item {
                            Text(
                                "ภาพรวมห้องพัก",
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                                color = AdminNavy
                            )
                        }

                        item {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AdminSummaryCard(
                                    modifier = Modifier.weight(1f),
                                    title = "ห้องทั้งหมด",
                                    value = rooms.size.toString(),
                                    icon = Icons.Outlined.Apartment,
                                    color = Color(0xFF3478C7),
                                    background = Color(0xFFE4F0FF)
                                )
                                AdminSummaryCard(
                                    modifier = Modifier.weight(1f),
                                    title = "มีผู้เช่า",
                                    value = occupiedRooms.toString(),
                                    icon = Icons.Outlined.Person,
                                    color = AdminGreen,
                                    background = Color(0xFFDDF5E8)
                                )
                                AdminSummaryCard(
                                    modifier = Modifier.weight(1f),
                                    title = "ห้องว่าง",
                                    value = availableRooms.toString(),
                                    icon = Icons.Outlined.MeetingRoom,
                                    color = AdminOrange,
                                    background = Color(0xFFFFF1D6)
                                )
                            }
                        }

                        item {
                            Text(
                                "สถานะการชำระเงิน",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = AdminNavy
                            )
                        }

                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AdminSummaryCard(
                                        modifier = Modifier.weight(1f),
                                        title = "จ่ายแล้ว",
                                        value = paidBills.toString(),
                                        unit = "บิล",
                                        icon = Icons.Outlined.CheckCircle,
                                        color = AdminGreen,
                                        background = Color(0xFFDDF5E8)
                                    )
                                    AdminSummaryCard(
                                        modifier = Modifier.weight(1f),
                                        title = "รออนุมัติ",
                                        value = reviewBills.toString(),
                                        unit = "บิล",
                                        icon = Icons.Outlined.Refresh,
                                        color = AdminOrange,
                                        background = Color(0xFFFFF1D6)
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AdminSummaryCard(
                                        modifier = Modifier.weight(1f),
                                        title = "ปฏิเสธ",
                                        value = rejectedBills.toString(),
                                        unit = "บิล",
                                        icon = Icons.Outlined.Delete,
                                        color = AdminRed,
                                        background = Color(0xFFFCE3E3)
                                    )
                                    AdminSummaryCard(
                                        modifier = Modifier.weight(1f),
                                        title = "ยังไม่จ่าย",
                                        value = unpaidBills.toString(),
                                        unit = "บิล",
                                        icon = Icons.Outlined.MeetingRoom,
                                        color = Color(0xFF64748B),
                                        background = Color(0xFFE9EEF4)
                                    )
                                }
                            }
                        }
                    } else {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "จัดการห้องพัก",
                                        fontSize = 21.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AdminNavy
                                    )
                                    Text(
                                        "เพิ่ม แก้ไข ผูกผู้เช่า หรือลบห้อง",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        loading = true
                                        db.collection("rooms").get()
                                            .addOnSuccessListener { snapshot ->
                                                rooms = snapshot.documents.map { doc ->
                                                    AdminRoom(
                                                        roomId = doc.getString("roomId") ?: doc.id,
                                                        roomNumber = doc.getString("roomNumber") ?: doc.id,
                                                        tower = doc.getString("tower") ?: "A",
                                                        floor = (doc.getLong("floor") ?: 1L).toInt(),
                                                        monthlyRent = doc.getDouble("monthlyRent")
                                                            ?: (doc.getLong("monthlyRent") ?: 0L).toDouble(),
                                                        username = doc.getString("username") ?: "",
                                                        tenantUid = doc.getString("tenantUid") ?: "",
                                                        status = doc.getString("status") ?: "available"
                                                    )
                                                }
                                                loading = false
                                            }
                                            .addOnFailureListener {
                                                loading = false
                                                errorMessage = it.localizedMessage
                                            }
                                    }
                                ) {
                                    Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
                                }
                            }
                        }
                    }

                    if (selectedTab == 1) {
                        item {
                            Button(
                                onClick = {
                                    errorMessage = null
                                    successMessage = null
                                    showAddDialog = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AdminNavy
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("+ เพิ่มห้องพัก", modifier = Modifier.padding(5.dp))
                            }
                        }
                    }

                    if (selectedTab == 0) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "รายการห้องพัก",
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AdminNavy,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    "${rooms.size} ห้อง",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    if (loading && rooms.isEmpty()) {
                        item {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(35.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = AdminNavy)
                            }
                        }
                    } else if (rooms.isEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White
                                ),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(25.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Outlined.MeetingRoom,
                                        contentDescription = null,
                                        tint = AdminGold,
                                        modifier = Modifier.size(42.dp)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "ยังไม่มีห้องพัก",
                                        color = AdminNavy,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "กดเพิ่มห้องพักเพื่อเริ่มต้น",
                                        color = Color.Gray,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    } else {
                        items(rooms, key = { it.roomId }) { room ->
                            AdminRoomCard(
                                room = room,
                                bills = bills.filter { it.roomId == room.roomId },
                                rejectionReasons = paymentReasons,
                                onClick = {
                                    selectedRoom = room
                                    errorMessage = null
                                    successMessage = null
                                }
                            )
                        }
                    }

                    errorMessage?.let { message ->
                        item {
                            Text(
                                message,
                                color = AdminRed,
                                fontSize = 13.sp
                            )
                        }
                    }

                    successMessage?.let { message ->
                        item {
                            Text(
                                message,
                                color = AdminGreen,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            2 -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    AnnouncementsScreen(onBack = { selectedTab = 0 })
                }
            }

            3 -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    // หน้าจัดการแจ้งซ่อมสำหรับแอดมิน แยกจากหน้าลูกบ้าน
                    AdminMaintenanceScreen()
                }
            }

            else -> {
                AdminProfileContent(
                    onLogout = { showLogoutDialog = true },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }
        }
    }

    if (showAddDialog) {
        AddRoomDialog(
            saving = saving,
            onDismiss = { if (!saving) showAddDialog = false },
            onSave = { number, tower, floor, rent ->
                scope.launch {
                    saving = true
                    errorMessage = null

                    try {
                        val roomNumber = number.trim()
                        val roomRef = db.collection("rooms").document(roomNumber)
                        val existing = roomRef.get().await()

                        require(!existing.exists()) {
                            "เลขห้องนี้มีอยู่แล้ว"
                        }

                        require(roomNumber.isNotBlank()) {
                            "กรุณากรอกเลขห้อง"
                        }

                        roomRef.set(
                            mapOf(
                                "roomId" to roomNumber,
                                "roomNumber" to roomNumber,
                                "tower" to tower.trim(),
                                "floor" to floor,
                                "monthlyRent" to rent,
                                "username" to "",
                                "tenantUid" to "",
                                "status" to "available"
                            )
                        ).await()

                        successMessage = "เพิ่มห้อง $roomNumber สำเร็จ"
                        showAddDialog = false
                        selectedTab = 1
                    } catch (e: Exception) {
                        errorMessage = e.message ?: "เพิ่มห้องไม่สำเร็จ"
                    } finally {
                        saving = false
                    }
                }
            }
        )
    }

    paymentTargetRoom?.let { room ->
        RoomPaymentVerificationScreen(
            roomId = room.roomId,
            roomNumber = room.roomNumber,
            onBack = {
                paymentTargetRoom = null
                selectedRoom = room
            }
        )
    }

    selectedRoom?.let { room ->
        RoomDetailsDialog(
            onManageBills = {
                billTargetRoom = room
                showAddBillDialog = true
            },
            onVerifyPayments = {
                paymentTargetRoom = room
                selectedRoom = null
            },

            room = room,
            saving = saving,
            onDismiss = { if (!saving) selectedRoom = null },
            onAssignTenant = { username ->
                scope.launch {
                    saving = true
                    errorMessage = null

                    try {
                        val normalizedUsername = username.trim().lowercase(Locale.ROOT)
                        require(normalizedUsername.isNotBlank()) {
                            "กรุณากรอก Username"
                        }
                        require(room.tenantUid.isBlank() || room.tenantUid == normalizedUsername) {
                            "ห้องนี้มีผู้เช่าอยู่แล้ว กรุณายกเลิกการผูกเดิมก่อน"
                        }

                        val usersSnapshot = db.collection("users")
                            .whereEqualTo("username", normalizedUsername)
                            .limit(2)
                            .get()
                            .await()

                        require(!usersSnapshot.isEmpty) {
                            "ไม่พบ Username นี้ กรุณาให้ผู้เช่าสมัครสมาชิกก่อน"
                        }
                        require(usersSnapshot.size() == 1) {
                            "พบ Username ซ้ำในระบบ กรุณาตรวจสอบข้อมูลผู้ใช้"
                        }

                        val userDoc = usersSnapshot.documents.first()
                        val uid = userDoc.id
                        val existingRoomId = userDoc.getString("roomId").orEmpty()

                        require(existingRoomId.isBlank() || existingRoomId == room.roomId) {
                            "ผู้เช่าคนนี้ผูกกับห้อง $existingRoomId อยู่แล้ว"
                        }

                        val batch = db.batch()
                        val roomRef = db.collection("rooms").document(room.roomId)
                        val userRef = db.collection("users").document(uid)

                        batch.update(
                            roomRef,
                            mapOf(
                                "tenantUid" to uid,
                                "username" to normalizedUsername,
                                "status" to "occupied"
                            )
                        )

                        batch.update(userRef, "roomId", room.roomId)

                        val usernameRef = db.collection("usernames")
                            .document(normalizedUsername)

                        val usernameDoc = usernameRef.get().await()
                        if (usernameDoc.exists()) {
                            batch.set(
                                usernameRef,
                                mapOf("roomId" to room.roomId),
                                SetOptions.merge()
                            )
                        }

                        batch.commit().await()

                        successMessage = "ผูก $normalizedUsername กับห้อง ${room.roomNumber} สำเร็จ"
                        selectedRoom = null
                    } catch (e: Exception) {
                        errorMessage = e.message ?: "ผูกผู้เช่าไม่สำเร็จ"
                    } finally {
                        saving = false
                    }
                }
            },
            onEdit = { number, tower, floor, rent ->
                scope.launch {
                    saving = true
                    errorMessage = null

                    try {
                        require(number.trim() == room.roomNumber) {
                            "ไม่อนุญาตให้เปลี่ยนเลขห้องผ่านหน้านี้"
                        }

                        db.collection("rooms")
                            .document(room.roomId)
                            .update(
                                mapOf(
                                    "tower" to tower.trim(),
                                    "floor" to floor,
                                    "monthlyRent" to rent
                                )
                            )
                            .await()

                        successMessage = "แก้ไขห้อง ${room.roomNumber} สำเร็จ"
                        selectedRoom = null
                    } catch (e: Exception) {
                        errorMessage = e.message ?: "แก้ไขห้องไม่สำเร็จ"
                    } finally {
                        saving = false
                    }
                }
            },
            onDelete = {
                if (room.tenantUid.isBlank() &&
                    !room.status.equals("occupied", true)
                ) {
                    showDeleteDialog = room
                    selectedRoom = null
                } else {
                    errorMessage = "ไม่สามารถลบห้องที่มีผู้เช่าได้ กรุณายกเลิกการผูกผู้เช่าก่อน"
                }
            },
            onUnassignTenant = {
                scope.launch {
                    saving = true
                    errorMessage = null

                    try {
                        require(room.tenantUid.isNotBlank()) {
                            "ห้องนี้ไม่มีผู้เช่า"
                        }

                        val batch = db.batch()
                        batch.update(
                            db.collection("rooms").document(room.roomId),
                            mapOf(
                                "tenantUid" to "",
                                "username" to "",
                                "status" to "available"
                            )
                        )

                        batch.update(
                            db.collection("users").document(room.tenantUid),
                            "roomId",
                            ""
                        )

                        val usernameRef = db.collection("usernames")
                            .document(room.username.lowercase(Locale.ROOT))
                        val usernameDoc = usernameRef.get().await()

                        if (usernameDoc.exists()) {
                            batch.set(
                                usernameRef,
                                mapOf("roomId" to ""),
                                SetOptions.merge()
                            )
                        }

                        batch.commit().await()
                        successMessage = "ยกเลิกการผูกผู้เช่าสำเร็จ"
                        selectedRoom = null
                    } catch (e: Exception) {
                        errorMessage = e.message ?: "ยกเลิกการผูกไม่สำเร็จ"
                    } finally {
                        saving = false
                    }
                }
            }
        )
    }

    billTargetRoom?.takeIf { showAddBillDialog }?.let { room ->
        AddBillDialog(
            room = room,
            saving = saving,
            onDismiss = {
                if (!saving) {
                    showAddBillDialog = false
                }
            },
            onSave = { title, amount, dueDate ->
                scope.launch {
                    saving = true
                    errorMessage = null
                    successMessage = null

                    try {
                        // ตรวจสอบข้อมูลห้องล่าสุดก่อนสร้างบิล
                        val latestRoom = db.collection("rooms")
                            .document(room.roomId)
                            .get()
                            .await()

                        val tenantUid = latestRoom
                            .getString("tenantUid")
                            .orEmpty()

                        require(tenantUid.isNotBlank()) {
                            "ห้องนี้ยังไม่มีผู้เช่า กรุณาผูกผู้เช่าก่อนเพิ่มค่าใช้จ่าย"
                        }

                        require(title.isNotBlank()) {
                            "กรุณาระบุรายการค่าใช้จ่าย"
                        }

                        require(amount > 0.0) {
                            "จำนวนเงินต้องมากกว่า 0 บาท"
                        }

                        require(
                            dueDate.matches(
                                Regex("\\d{4}-\\d{2}-\\d{2}")
                            )
                        ) {
                            "กรุณาระบุวันครบกำหนดในรูปแบบ YYYY-MM-DD"
                        }

                        // บันทึกบิลโดยผูกกับ UID ของผู้เช่า
                        db.collection("bills")
                            .add(
                                mapOf(
                                    "uid" to tenantUid,
                                    "roomId" to room.roomId,
                                    "title" to title.trim(),
                                    "amount" to amount,
                                    "dueDate" to dueDate.trim(),
                                    "status" to "pending",
                                    "createdAt" to System.currentTimeMillis()
                                )
                            )
                            .await()

                        successMessage =
                            "เพิ่มค่าใช้จ่ายให้ห้อง ${room.roomNumber} สำเร็จ"

                        showAddBillDialog = false
                        selectedRoom = null

                    } catch (e: Exception) {
                        errorMessage =
                            e.message ?: "เพิ่มค่าใช้จ่ายไม่สำเร็จ"
                    } finally {
                        saving = false
                    }
                }
            }
        )
    }

    showDeleteDialog?.let { room ->
        AlertDialog(
            onDismissRequest = { if (!saving) showDeleteDialog = null },
            icon = {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = AdminRed
                )
            },
            title = { Text("ยืนยันการลบห้องพัก") },
            text = {
                Text(
                    "ต้องการลบห้อง ${room.roomNumber} ใช่หรือไม่? " +
                            "ข้อมูลห้องนี้จะถูกลบออกจากระบบ"
                )
            },
            confirmButton = {
                Button(
                    enabled = !saving,
                    onClick = {
                        scope.launch {
                            saving = true
                            errorMessage = null

                            try {
                                val ref = db.collection("rooms")
                                    .document(room.roomId)
                                val latest = ref.get().await()

                                require(latest.exists()) {
                                    "ไม่พบห้องนี้แล้ว"
                                }
                                require(
                                    latest.getString("tenantUid").isNullOrBlank() &&
                                            !latest.getString("status")
                                                .equals("occupied", true)
                                ) {
                                    "ห้องนี้มีผู้เช่าอยู่ ไม่สามารถลบได้"
                                }

                                ref.delete().await()
                                successMessage = "ลบห้อง ${room.roomNumber} สำเร็จ"
                                showDeleteDialog = null
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "ลบห้องไม่สำเร็จ"
                            } finally {
                                saving = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AdminRed
                    )
                ) {
                    Text(if (saving) "กำลังลบ..." else "ยืนยันลบ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("ยกเลิก")
                }
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("ออกจากระบบ") },
            text = { Text("คุณต้องการออกจากระบบใช่หรือไม่?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text("ออกจากระบบ", color = AdminRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("ยกเลิก")
                }
            }
        )
    }
}

@Composable
private fun AdminSummaryCard(
    modifier: Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    background: Color,
    unit: String = "ห้อง"
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = background)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(23.dp))
            Text(title, fontSize = 11.sp, color = Color(0xFF4B5563))
            Text(
                value,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                color = AdminNavy
            )
            Text(unit, fontSize = 10.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun AdminRoomCard(
    room: AdminRoom,
    bills: List<AdminBillSummary>,
    rejectionReasons: Map<String, String>,
    onClick: () -> Unit
) {
    val occupied = room.tenantUid.isNotBlank() ||
            room.status.equals("occupied", true)

    val rent = NumberFormat.getNumberInstance(Locale.US)
        .format(room.monthlyRent)

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(13.dp),
                    color = if (occupied) Color(0xFFDDF5E8) else Color(0xFFFFF1D6),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.MeetingRoom,
                            contentDescription = null,
                            tint = if (occupied) AdminGreen else AdminOrange,
                            modifier = Modifier.size(27.dp)
                        )
                    }
                }

                Column(Modifier.weight(1f)) {
                    Text(
                        room.roomNumber,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = AdminNavy
                    )
                    Text(
                        "Tower ${room.tower} · ชั้น ${room.floor}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        "฿$rent / เดือน",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AdminNavy
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = if (occupied) Color(0xFFDDF5E8) else Color(0xFFFFF1D6)
                ) {
                    Text(
                        if (occupied) "มีผู้เช่า" else "ห้องว่าง",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        color = if (occupied) AdminGreen else AdminOrange,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (room.username.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = Color(0xFFE9ECF1))
                Spacer(Modifier.height(9.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Person,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "ผู้เช่า: ${room.username}",
                        color = Color.DarkGray,
                        fontSize = 12.sp
                    )
                }
            }

            if (bills.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = Color(0xFFE9ECF1))
                Spacer(Modifier.height(8.dp))
                Text(
                    "สถานะบิล",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = AdminNavy
                )
                bills.sortedByDescending { it.dueDate }.forEach { bill ->
                    val (statusText, statusColor, statusBackground) = when (bill.status.lowercase(Locale.ROOT)) {
                        "paid", "approved" -> Triple("จ่ายแล้ว", AdminGreen, Color(0xFFDDF5E8))
                        "under_review" -> Triple("รออนุมัติ", AdminOrange, Color(0xFFFFF1D6))
                        "rejected" -> Triple("ปฏิเสธ", AdminRed, Color(0xFFFCE3E3))
                        else -> Triple("ยังไม่จ่าย", Color(0xFF64748B), Color(0xFFE9EEF4))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(bill.title, fontSize = 12.sp, color = Color.DarkGray)
                            Text(
                                "฿${NumberFormat.getNumberInstance(Locale.US).format(bill.amount)}" +
                                        if (bill.dueDate.isNotBlank()) " · ครบกำหนด ${bill.dueDate}" else "",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                            if (bill.status.equals("rejected", true) && rejectionReasons[bill.billId].orEmpty().isNotBlank()) {
                                Text(
                                    "เหตุผล: ${rejectionReasons[bill.billId]}",
                                    fontSize = 11.sp,
                                    color = AdminRed
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(50.dp),
                            color = statusBackground
                        ) {
                            Text(
                                statusText,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                color = statusColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            } else if (occupied) {
                Spacer(Modifier.height(8.dp))
                Text("ยังไม่มีบิล", fontSize = 12.sp, color = Color(0xFF64748B))
            }

            Spacer(Modifier.height(9.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    "แตะเพื่อดูรายละเอียดและจัดการ",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = null,
                    tint = AdminNavy,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

@Composable
private fun AddRoomDialog(
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, Int, Double) -> Unit
) {
    var roomNumber by remember { mutableStateOf("") }
    var tower by remember { mutableStateOf("A") }
    var floor by remember { mutableStateOf("") }
    var rent by remember { mutableStateOf("7000") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("เพิ่มห้องพัก", color = AdminNavy, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = roomNumber,
                    onValueChange = { roomNumber = it.trim() },
                    label = { Text("เลขห้อง") },
                    placeholder = { Text("เช่น A-1206") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tower,
                    onValueChange = { tower = it },
                    label = { Text("อาคาร / Tower") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = floor,
                    onValueChange = { floor = it.filter(Char::isDigit) },
                    label = { Text("ชั้น") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = rent,
                    onValueChange = { rent = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("ค่าเช่ารายเดือน (บาท)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "ห้องใหม่จะมีสถานะเป็นห้องว่างโดยอัตโนมัติ",
                    color = AdminGreen,
                    fontSize = 12.sp
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !saving,
                onClick = {
                    val parsedFloor = floor.toIntOrNull()
                    val parsedRent = rent.toDoubleOrNull()

                    if (roomNumber.isNotBlank() &&
                        tower.isNotBlank() &&
                        parsedFloor != null &&
                        parsedFloor > 0 &&
                        parsedRent != null &&
                        parsedRent >= 0
                    ) {
                        onSave(roomNumber, tower, parsedFloor, parsedRent)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AdminNavy)
            ) {
                Text(if (saving) "กำลังบันทึก..." else "เพิ่มห้อง")
            }
        },
        dismissButton = {
            TextButton(enabled = !saving, onClick = onDismiss) {
                Text("ยกเลิก")
            }
        }
    )
}

@Composable
private fun RoomDetailsDialog(
    room: AdminRoom,
    saving: Boolean,
    onDismiss: () -> Unit,
    onAssignTenant: (String) -> Unit,
    onEdit: (String, String, Int, Double) -> Unit,
    onDelete: () -> Unit,
    onUnassignTenant: () -> Unit,
    onManageBills: () -> Unit,
    onVerifyPayments: () -> Unit
){
    var username by remember(room.roomId) { mutableStateOf(room.username) }
    var editing by remember(room.roomId) { mutableStateOf(false) }
    var roomNumber by remember(room.roomId) { mutableStateOf(room.roomNumber) }
    var tower by remember(room.roomId) { mutableStateOf(room.tower) }
    var floor by remember(room.roomId) { mutableStateOf(room.floor.toString()) }
    var rent by remember(room.roomId) { mutableStateOf(room.monthlyRent.toString()) }

    val occupied = room.tenantUid.isNotBlank() ||
            room.status.equals("occupied", true)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        room.roomNumber,
                        color = AdminNavy,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        if (occupied) "มีผู้เช่า" else "ห้องว่าง",
                        fontSize = 12.sp,
                        color = if (occupied) AdminGreen else AdminOrange
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    enabled = !saving
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "ปิด",
                        tint = AdminNavy
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 470.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (editing) {
                    OutlinedTextField(
                        value = roomNumber,
                        onValueChange = { roomNumber = it },
                        label = { Text("เลขห้อง") },
                        enabled = false,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tower,
                        onValueChange = { tower = it },
                        label = { Text("อาคาร") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = floor,
                        onValueChange = { floor = it.filter(Char::isDigit) },
                        label = { Text("ชั้น") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = rent,
                        onValueChange = { rent = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("ค่าเช่ารายเดือน") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    RoomInfoLine("เลขห้อง", room.roomNumber)
                    RoomInfoLine("อาคาร", room.tower)
                    RoomInfoLine("ชั้น", room.floor.toString())
                    RoomInfoLine(
                        "ค่าเช่ารายเดือน",
                        "฿${NumberFormat.getNumberInstance(Locale.US).format(room.monthlyRent)}"
                    )

                    HorizontalDivider(color = Color(0xFFE5E7EB))

                    Text(
                        "ผูกบัญชีผู้เช่า",
                        color = AdminNavy,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "กรอก Username ของผู้เช่าที่สมัครสมาชิกไว้แล้ว",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username ผู้เช่า") },
                        placeholder = { Text("เช่น beauty") },
                        leadingIcon = {
                            Icon(Icons.Outlined.PersonAdd, contentDescription = null)
                        },
                        singleLine = true,
                        enabled = !occupied && !saving,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (occupied) {
                        Text(
                            "ผูกกับ Username: ${room.username}",
                            fontSize = 12.sp,
                            color = AdminGreen
                        )
                    }
                }
            }
        },
        confirmButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (editing) {
                    Button(
                        enabled = !saving,
                        onClick = {
                            val f = floor.toIntOrNull()
                            val r = rent.toDoubleOrNull()

                            if (tower.isNotBlank() &&
                                f != null && f > 0 &&
                                r != null && r >= 0
                            ) {
                                onEdit(roomNumber, tower, f, r)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AdminNavy
                        )
                    ) {
                        Text(if (saving) "กำลังบันทึก..." else "บันทึก")
                    }
                } else {
                    if (occupied) {
                        OutlinedButton(
                            enabled = !saving,
                            onClick = onVerifyPayments,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("ตรวจสอบการชำระเงิน")
                        }

                        TextButton(
                            enabled = !saving,
                            onClick = onUnassignTenant
                        ) {
                            Text(
                                "ยกเลิกการผูกผู้เช่า",
                                color = AdminOrange
                            )
                        }
                    } else {
                        Button(
                            enabled = !saving && username.isNotBlank(),
                            onClick = {
                                onAssignTenant(username)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AdminNavy
                            )
                        ) {
                            Text(
                                if (saving) "กำลังผูก..." else "ยืนยันผูกผู้เช่า"
                            )
                        }
                    }

                    Button(
                        enabled = !saving && room.tenantUid.isNotBlank(),
                        onClick = onManageBills,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AdminGold
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "จัดการค่าใช้จ่าย",
                            color = AdminNavy
                        )
                    }

                    TextButton(
                        enabled = !saving,
                        onClick = { editing = true }
                    ) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("แก้ไขห้อง")
                    }

                    TextButton(
                        enabled = !saving && !occupied,
                        onClick = onDelete
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = AdminRed
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "ลบห้อง",
                            color = AdminRed
                        )
                    }
                }
            }
        },

    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddBillDialog(
    room: AdminRoom,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (title: String, amount: Double, dueDate: String) -> Unit
) {
    val billTypes = listOf(
        "ค่าเช่าห้อง",
        "ค่าน้ำ",
        "ค่าไฟ",
        "ค่าส่วนกลาง",
        "ค่าที่จอดรถ",
        "ค่าอินเทอร์เน็ต",
        "ค่าทำความสะอาด",
        "ค่าใช้จ่ายอื่น ๆ"
    )

    var selectedType by remember(room.roomId) {
        mutableStateOf(billTypes.first())
    }
    var customTitle by remember(room.roomId) {
        mutableStateOf("")
    }
    var amount by remember(room.roomId) {
        mutableStateOf(room.monthlyRent.toString())
    }
    var dueDate by remember(room.roomId) {
        mutableStateOf("")
    }
    var expanded by remember {
        mutableStateOf(false)
    }

    val finalTitle = if (selectedType == "ค่าใช้จ่ายอื่น ๆ") {
        customTitle
    } else {
        selectedType
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "เพิ่มค่าใช้จ่าย",
                    color = AdminNavy,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "ห้อง ${room.roomNumber}",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = {
                        expanded = !expanded
                    }
                ) {
                    OutlinedTextField(
                        value = selectedType,
                        onValueChange = {},
                        readOnly = true,
                        label = {
                            Text("ประเภทค่าใช้จ่าย")
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = expanded
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        singleLine = true
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = {
                            expanded = false
                        }
                    ) {
                        billTypes.forEach { type ->
                            DropdownMenuItem(
                                text = {
                                    Text(type)
                                },
                                onClick = {
                                    selectedType = type
                                    amount = if (type == "ค่าเช่าห้อง") {
                                        room.monthlyRent.toString()
                                    } else {
                                        ""
                                    }
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                if (selectedType == "ค่าใช้จ่ายอื่น ๆ") {
                    OutlinedTextField(
                        value = customTitle,
                        onValueChange = {
                            customTitle = it
                        },
                        label = {
                            Text("ชื่อรายการ")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it.filter { c ->
                            c.isDigit() || c == '.'
                        }
                    },
                    label = {
                        Text("จำนวนเงิน (บาท)")
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = dueDate,
                    onValueChange = {
                        dueDate = it.take(10)
                    },
                    label = {
                        Text("วันครบกำหนด (YYYY-MM-DD)")
                    },
                    placeholder = {
                        Text("2026-10-31")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text(
                    "รายการนี้จะแสดงในหน้าบิลของผู้เช่าห้องนี้",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !saving &&
                        finalTitle.isNotBlank() &&
                        (amount.toDoubleOrNull() ?: 0.0) > 0.0 &&
                        dueDate.matches(
                            Regex("\\d{4}-\\d{2}-\\d{2}")
                        ),
                onClick = {
                    val parsedAmount = amount.toDoubleOrNull()
                    if (parsedAmount != null) {
                        onSave(
                            finalTitle,
                            parsedAmount,
                            dueDate
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AdminNavy
                )
            ) {
                Text(
                    if (saving) "กำลังบันทึก..." else "เพิ่มบิล"
                )
            }
        },
        dismissButton = {
            TextButton(
                enabled = !saving,
                onClick = onDismiss
            ) {
                Text("ยกเลิก")
            }
        }
    )
}
@Composable
private fun RoomInfoLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.Gray, fontSize = 13.sp)
        Text(
            value,
            color = AdminNavy,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun AdminProfileContent(
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val user = FirebaseAuth.getInstance().currentUser

    Column(
        modifier = modifier
            .background(AdminBackground)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(28.dp))

        Surface(
            shape = CircleShape,
            color = Color(0xFFE4F0FF),
            modifier = Modifier.size(86.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Outlined.Person,
                    contentDescription = null,
                    tint = AdminNavy,
                    modifier = Modifier.size(42.dp)
                )
            }
        }

        Spacer(Modifier.height(15.dp))

        Text(
            "Admin",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = AdminNavy
        )
        Text(user?.email.orEmpty(), color = Color.Gray)

        Spacer(Modifier.height(22.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(18.dp)) {
                RoomInfoLine("อีเมล", user?.email.orEmpty())
                Spacer(Modifier.height(12.dp))
                RoomInfoLine("UID", user?.uid.orEmpty())
                Spacer(Modifier.height(12.dp))
                RoomInfoLine("Role", "admin")
            }
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = AdminRed)
        ) {
            Icon(Icons.Outlined.Logout, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("ออกจากระบบ")
        }
    }
}