
package com.example.condomanagement.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.condomanagement.data.model.Bill
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.util.UUID

class PaymentRepository(
    private val context: Context,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    companion object {
        private const val MAX_IMAGE_BYTES = 450 * 1024
    }

    /**
     * ย่อรูปและบีบอัดเป็น JPEG ให้มีขนาดไม่เกิน 450 KB
     * จากนั้นแปลงเป็น Base64 เพื่อบันทึกใน Firestore
     */

    private fun compressSlip(uri: Uri): String {
        val resolver = context.contentResolver


        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        val boundsStream = try {
            resolver.openInputStream(uri)
        } catch (e: Exception) {
            throw Exception(
                "เปิดรูปไม่ได้: ${e.javaClass.simpleName} - ${e.message}",
                e
            )
        } ?: throw Exception("เปิดรูปไม่ได้: InputStream เป็น null, URI=$uri")

        boundsStream.use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
        }

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw Exception(
                "อ่านข้อมูลรูปไม่ได้: width=${bounds.outWidth}, height=${bounds.outHeight}"
            )
        }

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw Exception("ไฟล์รูปไม่ถูกต้อง กรุณาเลือกรูปใหม่")
        }

        var sampleSize = 1
        while (
            bounds.outWidth / sampleSize > 1000 ||
            bounds.outHeight / sampleSize > 1000
        ) {
            sampleSize *= 2
        }

        val bitmap = resolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(
                input,
                null,
                BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                })
        } ?: throw Exception("ไม่สามารถอ่านรูปสลิปได้")

        try {
            for (quality in listOf(75, 65, 55, 45, 35, 25)) {
                val output = ByteArrayOutputStream()

                bitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    quality,
                    output
                )

                val bytes = output.toByteArray()
                output.close()

                if (bytes.size <= MAX_IMAGE_BYTES) {
                    return Base64.encodeToString(
                        bytes,
                        Base64.NO_WRAP
                    )
                }
            }

            throw Exception(
                "รูปสลิปมีขนาดใหญ่เกินไป กรุณาเลือกรูปที่เล็กลง"
            )
        } finally {
            bitmap.recycle()
        }
    }

    suspend fun submitPayment(bill: Bill, slipUri: Uri) {
        val user = auth.currentUser
            ?: throw Exception("กรุณาเข้าสู่ระบบก่อน")

        require(bill.billId.isNotBlank()) {
            "ไม่พบรหัสบิล"
        }
        require(bill.amount > 0) {
            "จำนวนเงินไม่ถูกต้อง"
        }

        val billRef = db.collection("bills").document(bill.billId)
        val latestBill = billRef.get().await()

        require(latestBill.exists()) {
            "ไม่พบบิลนี้ในระบบ"
        }

        require(latestBill.getString("uid") == user.uid) {
            "คุณไม่มีสิทธิ์ชำระบิลนี้"
        }

        val latestStatus = latestBill.getString("status").orEmpty()
        require(latestStatus !in listOf("paid", "approved", "under_review")) {
            "บิลนี้ถูกชำระหรืออยู่ระหว่างตรวจสอบแล้ว"
        }

        val existing = db.collection("payments")
            .whereEqualTo("uid", user.uid)
            .whereEqualTo("billId", bill.billId)
            .whereEqualTo("status", "under_review")
            .get()
            .await()

        if (!existing.isEmpty) {
            throw Exception("บิลนี้มีรายการชำระที่รอตรวจสอบอยู่แล้ว")
        }

        // ไม่มีการอัปโหลด Storage รูปจะอยู่ในเอกสาร Firestore โดยตรง
        val slipBase64 = compressSlip(slipUri)
        val paymentId = UUID.randomUUID().toString()
        val paymentRef = db.collection("payments").document(paymentId)

        val paymentData = hashMapOf(
            "paymentId" to paymentId,
            "uid" to user.uid,
            "billId" to bill.billId,
            "roomId" to bill.roomId,
            "amount" to bill.amount,
            "slipBase64" to slipBase64,
            "slipAttached" to true,
            "status" to "under_review",
            "createdAt" to System.currentTimeMillis()
        )

        val batch = db.batch()
        batch.set(paymentRef, paymentData)
        batch.update(billRef, "status", "under_review")
        batch.commit().await()
    }

    suspend fun approvePayment(paymentId: String) {
        val paymentRef = db.collection("payments").document(paymentId)
        val payment = paymentRef.get().await()

        require(payment.exists()) { "ไม่พบรายการชำระเงิน" }
        require(payment.getString("status") == "under_review") {
            "รายการนี้ไม่ได้อยู่ระหว่างตรวจสอบ"
        }

        val billId = payment.getString("billId")
            ?: throw Exception("ไม่พบรหัสบิล")

        val billRef = db.collection("bills").document(billId)
        val bill = billRef.get().await()

        require(bill.exists()) { "ไม่พบบิลที่เกี่ยวข้อง" }
        require(bill.getString("status") == "under_review") {
            "สถานะบิลเปลี่ยนไปแล้ว กรุณาตรวจสอบอีกครั้ง"
        }

        val batch = db.batch()
        batch.update(
            paymentRef,
            mapOf(
                "status" to "approved",
                "reviewedAt" to System.currentTimeMillis()
            )
        )
        batch.update(billRef, "status", "paid")
        batch.commit().await()
    }

    suspend fun rejectPayment(paymentId: String, reason: String) {
        require(reason.isNotBlank()) {
            "กรุณาระบุเหตุผลที่ปฏิเสธ"
        }

        val paymentRef = db.collection("payments").document(paymentId)
        val payment = paymentRef.get().await()

        require(payment.exists()) { "ไม่พบรายการชำระเงิน" }
        require(payment.getString("status") == "under_review") {
            "รายการนี้ไม่ได้อยู่ระหว่างตรวจสอบ"
        }

        val billId = payment.getString("billId")
            ?: throw Exception("ไม่พบรหัสบิล")

        val billRef = db.collection("bills").document(billId)
        val bill = billRef.get().await()

        require(bill.exists()) { "ไม่พบบิลที่เกี่ยวข้อง" }
        require(bill.getString("status") == "under_review") {
            "สถานะบิลเปลี่ยนไปแล้ว กรุณาตรวจสอบอีกครั้ง"
        }

        val batch = db.batch()
        batch.update(
            paymentRef,
            mapOf(
                "status" to "rejected",
                "rejectionReason" to reason.trim(),
                "reviewedAt" to System.currentTimeMillis()
            )
        )
        batch.update(billRef, "status", "rejected")
        batch.commit().await()
    }
}
