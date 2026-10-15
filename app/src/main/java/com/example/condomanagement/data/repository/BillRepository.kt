
package com.example.condomanagement.data.repository

import com.example.condomanagement.data.model.Bill
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class BillRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun getMyBills(): List<Bill> {
        val uid = auth.currentUser?.uid
            ?: throw Exception("กรุณาเข้าสู่ระบบก่อน")

        val snapshot = db.collection("bills")
            .whereEqualTo("uid", uid)
            .get()
            .await()

        return snapshot.documents.mapNotNull { document ->
            document.toObject(Bill::class.java)?.copy(
                billId = document.id
            )
        }
    }
}