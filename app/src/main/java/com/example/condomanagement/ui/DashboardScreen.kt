
package com.example.condomanagement.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.condomanagement.data.model.Bill
import com.example.condomanagement.viewmodel.BillViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await


private val Navy = Color(0xFF192F50)
private val PageBackground = Color(0xFFF3F3F3)
private val Blue = Color(0xFF3869B7)
private val Green = Color(0xFF21866E)
private val Red = Color(0xFFDC4545)

private data class NavItem(
    val label: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    name: String,
    role: String,
    onLogout: () -> Unit,
    billViewModel: BillViewModel = viewModel()
) {
    var showAnnouncements by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var selectedBillForPayment by remember { mutableStateOf<Bill?>(null) }

    val billState by billViewModel.uiState.collectAsState()
    LaunchedEffect(role) {
        android.util.Log.d(
            "CondoManagement",
            "Dashboard role = '$role'"
        )
    }
    val navItems = listOf(
        NavItem("Home", Icons.Outlined.Home),
        NavItem("Bills", Icons.Outlined.ReceiptLong),
        NavItem("แจ้งซ่อม", Icons.Outlined.Build),
        NavItem("Profile", Icons.Outlined.Person)
    )

    LaunchedEffect(Unit) {
        billViewModel.loadBills()
    }

    Scaffold(
        containerColor = PageBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (!showAnnouncements){
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Navy)
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(58.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.13f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.Apartment,
                                contentDescription = "CondoCare",
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(Modifier.weight(1f)) {
                        Text(
                            "CondoCare",
                            color = Color.White,
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "PROPERTY MANAGEMENT",
                            color = Color(0xFFD0D9E8),
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { showLogoutDialog = true }) {
                            Icon(
                                Icons.Outlined.Logout,
                                contentDescription = "Logout",
                                tint = Color.White,
                                modifier = Modifier.size(27.dp)
                            )
                        }
                        Text("Logout", color = Color.White, fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(22.dp))

                Text(
                    "ยินดีต้อนรับกลับ, $name",
                    color = Color(0xFFD0D9E8),
                    fontSize = 17.sp
                )

                Spacer(Modifier.height(20.dp))

                Text(
                    text = when {
                        showAnnouncements -> "ประกาศจากนิติบุคคล"
                        selectedIndex == 0 -> "ห้องของคุณ ในภาพรวม"
                        selectedIndex == 1 && selectedBillForPayment != null -> "ชำระเงิน"
                        selectedIndex == 1 -> "บิลค่าใช้จ่าย"
                        selectedIndex == 2 -> "แจ้งซ่อม"
                        else -> "ข้อมูลส่วนตัว"
                    },
                    color = Color.White,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold
                )
            } }
        },
        bottomBar = {
            if (selectedBillForPayment == null && !showAnnouncements) {
                NavigationBar(containerColor = Color.White) {
                    navItems.forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = selectedIndex == index,
                            onClick = {
                                selectedIndex = index
                                selectedBillForPayment = null
                                showAnnouncements = false
                            },
                            icon = {
                                Icon(item.icon, contentDescription = item.label)
                            },
                            label = { Text(item.label) },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Blue,
                                selectedTextColor = Blue,
                                indicatorColor = Color(0xFFE5EBF5),
                                unselectedIconColor = Color.Black,
                                unselectedTextColor = Color.Black
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {

                showAnnouncements -> {
                    AnnouncementScreen(
                        onBack = { showAnnouncements = false },
                        role = role
                    )
                }


                selectedIndex == 0 -> {
                    TenantHomeContent(
                        bills = billState.bills,
                        loading = billState.loading,
                        error = billState.error,
                        onBillsClick = { selectedIndex = 1 },
                        onMaintenanceClick = { selectedIndex = 2 },
                        onAnnouncementsClick = { showAnnouncements = true }
                    )
                }

                selectedIndex == 1 -> {
                    val selectedBill = selectedBillForPayment

                    if (selectedBill != null) {
                        PaymentScreen(
                            bill = selectedBill,
                            onBack = { selectedBillForPayment = null },
                            onSubmitted = {
                                selectedBillForPayment = null
                                billViewModel.loadBills()
                            }
                        )
                    } else {
                        TenantBillsContent(
                            bills = billState.bills,
                            loading = billState.loading,
                            error = billState.error,
                            onRefresh = { billViewModel.loadBills() },
                            onPayClick = { selectedBillForPayment = it }
                        )
                    }
                }

                selectedIndex == 2 -> {
                    MaintenanceScreen()
                }

                selectedIndex == 3 -> {
                    TenantProfileContent(name = name, role = role)
                }
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("ออกจากระบบ (Logout)") },
            text = { Text("คุณต้องการออกจากระบบใช่หรือไม่?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text("Logout", color = Red)
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
private fun TenantHomeContent(
    bills: List<Bill>,
    loading: Boolean,
    error: String?,
    onBillsClick: () -> Unit,
    onMaintenanceClick: () -> Unit,
    onAnnouncementsClick: () -> Unit
) {
    val outstanding = bills.filterNot { isBillPaid(it) }.sumOf { it.amount }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = PaddingValues(top = 18.dp, bottom = 24.dp)
    ) {
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OverviewCard(
                    modifier = Modifier.weight(1f),
                    title = "ยอดค้างชำระ",
                    subtitle = "Outstanding Balance",
                    value = if (loading && bills.isEmpty()) {
                        "กำลังโหลด..."
                    } else {
                        "฿${"%,.2f".format(outstanding)}"
                    },
                    footer = "Outstanding Bills",
                    icon = Icons.Outlined.Wallet,
                    iconTint = Blue
                )
                OverviewCard(
                    modifier = Modifier.weight(1f),
                    title = "บิลทั้งหมด",
                    subtitle = "My Bills",
                    value = bills.size.toString(),
                    footer = "Billing Records",
                    icon = Icons.Outlined.Home,
                    iconTint = Green
                )
            }
        }

        item {
            Text(
                "เมนูลัด (Quick Actions)",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                QuickAction(
                    modifier = Modifier.weight(1f),
                    title = "ดูบิล",
                    icon = Icons.Outlined.ReceiptLong,
                    onClick = onBillsClick
                )
                QuickAction(
                    modifier = Modifier.weight(1f),
                    title = "แจ้งซ่อม",
                    icon = Icons.Outlined.Build,
                    onClick = onMaintenanceClick
                )
                QuickAction(
                    modifier = Modifier.weight(1f),
                    title = "ประกาศ",
                    icon = Icons.Outlined.Campaign,
                    onClick = onAnnouncementsClick
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "รายการบิล (Bills)",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onBillsClick) {
                    Text("ดูทั้งหมด")
                }
            }
        }

        when {
            loading && bills.isEmpty() -> item { LoadingMessage() }
            error != null && bills.isEmpty() -> item { ErrorMessage(error) }
            bills.isEmpty() -> item { EmptyMessage("ยังไม่มีรายการบิล") }
            else -> items(bills.take(3), key = { it.billId }) {
                BillCard(bill = it)
            }
        }
    }
}

@Composable
private fun TenantBillsContent(
    bills: List<Bill>,
    loading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onPayClick: (Bill) -> Unit
) {
    val outstanding = bills.filterNot { isBillPaid(it) }.sumOf { it.amount }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "บิลของฉัน (My Bills)",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Navy
                    )
                    Text("รายการค่าใช้จ่ายของคุณ", color = Color.Gray)
                }
                TextButton(onClick = onRefresh) {
                    Text("Refresh")
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Navy),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        "ยอดค้างชำระ (Outstanding Balance)",
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "฿${"%,.2f".format(outstanding)}",
                        color = Color.White,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        when {
            loading && bills.isEmpty() -> item { LoadingMessage() }
            error != null && bills.isEmpty() -> item { ErrorMessage(error) }
            bills.isEmpty() -> item { EmptyMessage("ยังไม่มีบิลสำหรับบัญชีนี้") }
            else -> items(bills, key = { it.billId }) { bill ->
                BillCard(bill = bill, onPayClick = { onPayClick(bill) })
            }
        }

        item {
            Text(
                "เลือกบิลที่ต้องการชำระ แล้วแนบสลิปเพื่อส่งให้เจ้าของตรวจสอบ",
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
    }
}

private fun isBillPaid(bill: Bill): Boolean =
    bill.status.equals("paid", ignoreCase = true) ||
            bill.status.equals("ชำระแล้ว", ignoreCase = true)

@Composable
private fun BillCard(
    bill: Bill,
    onPayClick: (() -> Unit)? = null
) {
    val paid = bill.status.equals("paid", ignoreCase = true)
    val underReview = bill.status.equals("under_review", ignoreCase = true)
    val rejected = bill.status.equals("rejected", ignoreCase = true)
    val overdue = bill.status.equals("overdue", ignoreCase = true)

    val statusColor = when {
        paid -> Color(0xFF16845B)
        underReview -> Blue
        rejected || overdue -> Red
        else -> Color(0xFFEA8A00)
    }
    val statusBackground = when {
        paid -> Color(0xFFE5F5ED)
        underReview -> Color(0xFFE5EBF5)
        rejected || overdue -> Color(0xFFFFE8E8)
        else -> Color(0xFFFFF1D6)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(13.dp),
                color = if (paid) Color(0xFFE5F5ED) else Color(0xFFF1F3F7),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (paid) Icons.Outlined.CheckCircle else Icons.Outlined.Payments,
                        contentDescription = null,
                        tint = if (paid) Green else Blue,
                        modifier = Modifier.size(25.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    bill.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Color(0xFF1A1A1A)
                )
                Text(
                    bill.roomId.ifBlank { "CondoCare Bill" },
                    color = Color.Gray,
                    fontSize = 11.sp
                )
                Spacer(Modifier.height(7.dp))
                Text("ครบกำหนด ${bill.dueDate}", color = Color.Gray, fontSize = 10.sp)
            }

            Spacer(Modifier.width(8.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "฿${"%,.2f".format(bill.amount)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF111827)
                )

                Surface(
                    shape = RoundedCornerShape(50),
                    color = statusBackground,
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            Modifier
                                .size(7.dp)
                                .background(statusColor, CircleShape)
                        )
                        Text(
                            when {
                                paid -> "ชำระแล้ว"
                                underReview -> "กำลังตรวจสอบ"
                                rejected -> "ไม่อนุมัติ"
                                overdue -> "เกินกำหนด"
                                else -> "รอชำระ"
                            },
                            color = statusColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }

                Text(
                    when {
                        paid -> "Paid"
                        underReview -> "Under Review"
                        rejected -> "Rejected"
                        overdue -> "Overdue"
                        else -> "Pending"
                    },
                    fontSize = 10.sp,
                    color = statusColor
                )

                if (!paid && !underReview && onPayClick != null) {
                    Button(
                        onClick = onPayClick,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Navy)
                    ) {
                        Text(
                            if (rejected) "ส่งคำขอใหม่" else "ชำระเงิน",
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    value: String,
    footer: String,
    icon: ImageVector,
    iconTint: Color
) {
    Card(
        modifier = modifier.heightIn(min = 170.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFD8D8D8)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(29.dp))
            Spacer(Modifier.height(14.dp))
            Text(title, color = Color.DarkGray, fontSize = 14.sp)
            Text(subtitle, color = Color.Gray, fontSize = 10.sp)
            Spacer(Modifier.height(10.dp))
            Text(
                value,
                color = Color.Black,
                fontSize = if (value.length > 12) 16.sp else 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            Text(footer, color = Color.Gray, fontSize = 10.sp)
        }
    }
}

@Composable
private fun QuickAction(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = title, tint = Color.Black, modifier = Modifier.size(27.dp))
            Spacer(Modifier.height(10.dp))
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        }
    }
}

@Composable
private fun TenantProfileContent(
    name: String,
    role: String
) {
    val authUser = FirebaseAuth.getInstance().currentUser
    var registeredName by remember { mutableStateOf(name) }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(authUser?.email.orEmpty()) }
    var roomId by remember { mutableStateOf("") }
    var isLoadingProfile by remember { mutableStateOf(true) }

    LaunchedEffect(authUser?.uid) {
        val uid = authUser?.uid
        if (uid == null) {
            isLoadingProfile = false
        } else {
            try {
                val document = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .get()
                    .await()

                registeredName = document.getString("name")
                    ?.takeIf { it.isNotBlank() }
                    ?: document.getString("username")
                        ?.takeIf { it.isNotBlank() }
                            ?: name

                username = document.getString("username").orEmpty()
                email = document.getString("email")
                    ?.takeIf { it.isNotBlank() }
                    ?: authUser.email.orEmpty()
                roomId = document.getString("roomId").orEmpty()
            } catch (_: Exception) {
                // แสดงข้อมูลที่โหลดได้อยู่แล้ว
            } finally {
                isLoadingProfile = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(30.dp))

        Surface(
            shape = CircleShape,
            color = Color(0xFFE5EBF5),
            modifier = Modifier.size(90.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Outlined.Person,
                    contentDescription = "Profile",
                    tint = Blue,
                    modifier = Modifier.size(45.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(registeredName, fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Navy)
        Text(
            if (role.equals("owner", true) || role.equals("admin", true)) {
                "เจ้าของคอนโด (Owner)"
            } else {
                "ผู้เช่า (Tenant)"
            },
            color = Color.Gray
        )

        Spacer(Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    "ข้อมูลบัญชี (Account Information)",
                    fontWeight = FontWeight.Bold,
                    color = Navy,
                    fontSize = 17.sp
                )
                Spacer(Modifier.height(14.dp))

                if (isLoadingProfile) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Blue)
                    Spacer(Modifier.height(12.dp))
                }

                ProfileInfoRow("ชื่อ-นามสกุล", registeredName.ifBlank { "-" })
                ProfileInfoRow("Username", username.ifBlank { "-" })
                ProfileInfoRow("อีเมล", email.ifBlank { "-" })
                ProfileInfoRow("เลขห้อง", roomId.ifBlank { "-" })
                ProfileInfoRow("Role", role.ifBlank { "-" })
            }
        }
    }
}

@Composable
private fun ProfileInfoRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Spacer(Modifier.height(3.dp))
        Text(
            value,
            fontSize = 15.sp,
            color = Color(0xFF202938),
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun LoadingMessage() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Navy)
    }
}

@Composable
private fun EmptyMessage(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Text(message, modifier = Modifier.padding(20.dp), color = Color.Gray)
    }
}

@Composable
private fun ErrorMessage(message: String) {
    Text(
        "โหลดข้อมูลไม่สำเร็จ: $message",
        color = Red,
        modifier = Modifier.padding(12.dp)
    )
}
