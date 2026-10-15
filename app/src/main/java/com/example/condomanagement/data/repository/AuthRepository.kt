
package com.example.condomanagement.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Locale

data class UserProfile(
    val uid: String = "",
    val username: String = "",
    val name: String = "",
    val email: String = "",
    val roomId: String = "",
    val role: String = "tenant"
)

class AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    val currentUser
        get() = auth.currentUser

    suspend fun register(
        username: String,
        email: String,
        password: String
    ): Result<Unit> {
        val normalizedUsername =
            username.trim().lowercase(Locale.ROOT)
        val normalizedEmail =
            email.trim().lowercase(Locale.ROOT)

        if (!normalizedUsername.matches(Regex("^[a-z0-9_.]{3,20}$"))) {
            return Result.failure(
                IllegalArgumentException(
                    "Username ต้องมี 3-20 ตัวอักษร ใช้ a-z, 0-9, _ หรือ ."
                )
            )
        }

        if (!android.util.Patterns.EMAIL_ADDRESS
                .matcher(normalizedEmail).matches()
        ) {
            return Result.failure(
                IllegalArgumentException("กรุณากรอก Email ให้ถูกต้อง")
            )
        }

        if (password.length < 6) {
            return Result.failure(
                IllegalArgumentException("รหัสผ่านต้องมีอย่างน้อย 6 ตัว")
            )
        }

        var createdUid: String? = null

        return try {
            val usernameRef = db.collection("usernames")
                .document(normalizedUsername)

            val existing = usernameRef.get().await()

            require(!existing.exists()) {
                "Username นี้ถูกใช้แล้ว"
            }

            val result = auth.createUserWithEmailAndPassword(
                normalizedEmail,
                password
            ).await()

            val firebaseUser = result.user
                ?: throw IllegalStateException("สร้างบัญชีไม่สำเร็จ")

            val uid = firebaseUser.uid
            createdUid = uid

            val userRef = db.collection("users").document(uid)

            // เขียนข้อมูลสอง collection ใน batch เดียวกัน
            db.runBatch { batch ->
                batch.set(
                    userRef,
                    mapOf(
                        "uid" to uid,
                        "username" to normalizedUsername,
                        "name" to normalizedUsername,
                        "email" to normalizedEmail,
                        "roomId" to "",
                        "role" to "tenant"
                    )
                )

                batch.set(
                    usernameRef,
                    mapOf(
                        "uid" to uid,
                        "email" to normalizedEmail,
                        "roomId" to ""
                    )
                )
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            // หากเขียน Firestore ไม่สำเร็จ จะไม่ปล่อยให้
            // ViewModel เข้าใจว่าสมัครสำเร็จ
            if (createdUid != null) {
                auth.signOut()
            }

            Result.failure(
                when (e) {
                    is IllegalArgumentException -> e
                    else -> IllegalStateException(
                        e.message ?: "สมัครสมาชิกไม่สำเร็จ กรุณาลองใหม่",
                        e
                    )
                }
            )
        }
    }

    suspend fun login(
        usernameOrEmail: String,
        password: String
    ): Result<Unit> {
        return try {
            val input = usernameOrEmail.trim()

            require(input.isNotBlank()) {
                "กรุณากรอก Username หรือ Email"
            }
            require(password.isNotBlank()) {
                "กรุณากรอกรหัสผ่าน"
            }

            val email = if (input.contains("@")) {
                input.lowercase(Locale.ROOT)
            } else {
                val username = input.lowercase(Locale.ROOT)

                val usernameDoc = db.collection("usernames")
                    .document(username)
                    .get()
                    .await()

                usernameDoc.getString("email")
                    ?: throw IllegalArgumentException(
                        "ไม่พบ Username นี้ กรุณาตรวจสอบอีกครั้ง"
                    )
            }

            auth.signInWithEmailAndPassword(email, password).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserProfile(): UserProfile {
        val firebaseUser = auth.currentUser
            ?: throw IllegalStateException("กรุณาเข้าสู่ระบบ")

        val snapshot = db.collection("users")
            .document(firebaseUser.uid)
            .get()
            .await()

        if (!snapshot.exists()) {
            throw IllegalStateException(
                "ไม่พบข้อมูลผู้ใช้ใน Firestore"
            )
        }

        return UserProfile(
            uid = firebaseUser.uid,
            username = snapshot.getString("username").orEmpty(),
            name = snapshot.getString("name")
                ?: snapshot.getString("username")
                ?: firebaseUser.email.orEmpty(),
            email = snapshot.getString("email")
                ?: firebaseUser.email.orEmpty(),
            roomId = snapshot.getString("roomId").orEmpty(),
            role = snapshot.getString("role") ?: "tenant"
        )
    }

    fun logout() {
        auth.signOut()
    }
}
