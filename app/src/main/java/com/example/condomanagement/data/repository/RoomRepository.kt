
package com.example.condomanagement.data.repository

import com.example.condomanagement.data.model.Room
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class RoomRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun getAllRooms(): List<Room> {
        val snapshot = db.collection("rooms").get().await()

        return snapshot.documents.mapNotNull { document ->
            document.toObject(Room::class.java)?.copy(
                roomId = document.id
            )
        }.sortedBy { it.roomNumber }
    }

    suspend fun addRoom(room: Room) {
        val roomId = room.roomId.trim()

        require(roomId.isNotBlank()) {
            "กรุณากรอกเลขห้อง"
        }
        require(room.monthlyRent >= 0) {
            "ค่าเช่าต้องไม่ติดลบ"
        }
        require(room.floor >= 0) {
            "ชั้นต้องไม่ติดลบ"
        }

        val roomRef = db.collection("rooms").document(roomId)
        val existingRoom = roomRef.get().await()

        require(!existingRoom.exists()) {
            "เลขห้องนี้มีอยู่แล้ว"
        }

        val newRoom = room.copy(
            roomId = roomId,
            roomNumber = room.roomNumber.trim().ifBlank { roomId },
            tower = room.tower.trim().ifBlank { "A" },
            username = "",
            tenantUid = "",
            status = room.status.ifBlank { "available" }
        )

        roomRef.set(newRoom).await()
    }

    suspend fun setRoomUsername(roomId: String, username: String) {
        val normalizedUsername = username.trim().lowercase()

        require(roomId.isNotBlank()) {
            "ไม่พบเลขห้อง"
        }
        require(normalizedUsername.matches(Regex("^[a-zA-Z0-9_.-]{3,30}$"))) {
            "Username ต้องมี 3–30 ตัว และใช้ได้เฉพาะตัวอักษร ตัวเลข _ . -"
        }

        val roomRef = db.collection("rooms").document(roomId)
        val roomSnapshot = roomRef.get().await()

        require(roomSnapshot.exists()) {
            "ไม่พบห้องนี้"
        }

        val currentRoom = roomSnapshot.toObject(Room::class.java)
            ?: throw Exception("ไม่สามารถอ่านข้อมูลห้องได้")

        require(currentRoom.tenantUid.isBlank()) {
            "ห้องนี้มีผู้เช่าแล้ว ไม่สามารถเปลี่ยน Username ได้"
        }

        val duplicate = db.collection("rooms")
            .whereEqualTo("username", normalizedUsername)
            .get()
            .await()
            .documents
            .any { it.id != roomId }

        require(!duplicate) {
            "Username นี้ถูกกำหนดให้ห้องอื่นแล้ว"
        }

        roomRef.update("username", normalizedUsername).await()
    }

    suspend fun deleteRoom(roomId: String) {
        val roomRef = db.collection("rooms").document(roomId)
        val snapshot = roomRef.get().await()

        require(snapshot.exists()) {
            "ไม่พบห้องนี้"
        }

        val room = snapshot.toObject(Room::class.java)
            ?: throw Exception("ไม่สามารถอ่านข้อมูลห้องได้")

        require(room.tenantUid.isBlank()) {
            "ลบไม่ได้ เพราะห้องนี้มีผู้เช่าแล้ว"
        }

        val bills = db.collection("bills")
            .whereEqualTo("roomId", roomId)
            .limit(1)
            .get()
            .await()

        require(bills.isEmpty) {
            "ลบไม่ได้ เพราะห้องนี้มีประวัติบิลอยู่"
        }

        roomRef.delete().await()
    }

    suspend fun assignTenant(roomId: String, username: String) {
        val normalizedUsername = username.trim().lowercase()

        val roomRef = db.collection("rooms").document(roomId)
        val roomSnapshot = roomRef.get().await()

        require(roomSnapshot.exists()) {
            "ไม่พบห้องนี้"
        }

        val room = roomSnapshot.toObject(Room::class.java)
            ?: throw Exception("ไม่สามารถอ่านข้อมูลห้องได้")

        require(room.tenantUid.isBlank()) {
            "ห้องนี้มีผู้เช่าแล้ว"
        }
        require(room.username.lowercase() == normalizedUsername) {
            "Username ไม่ตรงกับที่เจ้าของกำหนดไว้"
        }

        val userSnapshot = db.collection("users")
            .whereEqualTo("username", normalizedUsername)
            .limit(1)
            .get()
            .await()

        val userDocument = userSnapshot.documents.firstOrNull()
            ?: throw Exception("ยังไม่มีบัญชีผู้ใช้ Username นี้")

        val userRef = userDocument.reference

        db.runTransaction { transaction ->
            val latestRoom = transaction.get(roomRef)
            val latestUser = transaction.get(userRef)

            if (!latestRoom.exists() || !latestUser.exists()) {
                throw Exception("ไม่พบข้อมูลห้องหรือผู้ใช้")
            }

            if (latestRoom.getString("tenantUid").orEmpty().isNotBlank()) {
                throw Exception("ห้องนี้มีผู้เช่าแล้ว")
            }

            transaction.update(roomRef, "tenantUid", userDocument.id)
            transaction.update(roomRef, "status", "occupied")
            transaction.update(userRef, "roomId", roomId)

            null
        }.await()
    }
}
