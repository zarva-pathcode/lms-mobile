package com.myschedule.id.data

import com.myschedule.id.FirebaseInstance
import com.google.firebase.firestore.FieldValue

object QuizRepository {

    fun createQuiz(
        uniName: String,
        className: String,
        title: String,
        description: String,
        deadline: String,
        timeLimit: Int,
        passingScore: Int,
        questions: List<Map<String, Any>>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val data = hashMapOf(
            "uniName" to uniName,
            "className" to className,
            "title" to title,
            "description" to description,
            "deadline" to deadline,
            "timeLimit" to timeLimit,
            "passingScore" to passingScore,
            "totalQuestions" to questions.size,
            "questions" to questions,
            "status" to "draft",
            "createdAt" to FieldValue.serverTimestamp()
        )

        FirebaseInstance.db.collection("quizzes")
            .add(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Gagal membuat quiz") }
    }

    fun getQuizzesByClass(uniName: String, className: String, onResult: (List<Map<String, Any>>) -> Unit) {
        FirebaseInstance.db.collection("quizzes")
            .whereEqualTo("uniName", uniName)
            .whereEqualTo("className", className)
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.map { doc ->
                    val map = doc.data?.toMutableMap() ?: mutableMapOf()
                    map["id"] = doc.id
                    map
                }
                onResult(list)
            }
            .addOnFailureListener { onResult(emptyList()) }
    }

    fun getQuiz(quizId: String, onResult: (Map<String, Any>?) -> Unit) {
        FirebaseInstance.db.collection("quizzes").document(quizId).get()
            .addOnSuccessListener { doc -> onResult(doc.data) }
            .addOnFailureListener { onResult(null) }
    }

    fun startAttempt(
        quizId: String,
        studentUid: String,
        studentName: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val data = hashMapOf(
            "studentUid" to studentUid,
            "studentName" to studentName,
            "startedAt" to FieldValue.serverTimestamp(),
            "submittedAt" to null,
            "score" to -1,
            "correctCount" to 0,
            "totalQuestions" to 0,
            "passed" to false,
            "answers" to listOf<Int>()
        )

        FirebaseInstance.db.collection("quizzes").document(quizId)
            .collection("attempts").document(studentUid)
            .set(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Gagal memulai quiz") }
    }

    fun submitAttempt(
        quizId: String,
        studentUid: String,
        answers: List<Int>,
        questions: List<Map<String, Any>>,
        passingScore: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val totalQuestions = questions.size
        var correctCount = 0

        for (i in 0 until totalQuestions) {
            val correctAnswer = (questions[i]["correctAnswer"] as? Long)?.toInt() ?: -1
            if (answers.getOrElse(i) { -1 } == correctAnswer) {
                correctCount++
            }
        }

        val score = if (totalQuestions > 0) (correctCount * 100) / totalQuestions else 0
        val passed = score >= passingScore

        val data = hashMapOf(
            "answers" to answers,
            "score" to score,
            "correctCount" to correctCount,
            "totalQuestions" to totalQuestions,
            "passed" to passed,
            "submittedAt" to FieldValue.serverTimestamp()
        )

        FirebaseInstance.db.collection("quizzes").document(quizId)
            .collection("attempts").document(studentUid)
            .update(data as Map<String, Any>)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Gagal submit quiz") }
    }

    fun getAttempt(quizId: String, studentUid: String, onResult: (Map<String, Any>?) -> Unit) {
        FirebaseInstance.db.collection("quizzes").document(quizId)
            .collection("attempts").document(studentUid)
            .get()
            .addOnSuccessListener { doc -> onResult(doc.data) }
            .addOnFailureListener { onResult(null) }
    }

    fun getAttempts(quizId: String, onResult: (List<Map<String, Any>>) -> Unit) {
        FirebaseInstance.db.collection("quizzes").document(quizId)
            .collection("attempts")
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.map { doc ->
                    val map = doc.data?.toMutableMap() ?: mutableMapOf()
                    map["id"] = doc.id
                    map
                }
                onResult(list)
            }
            .addOnFailureListener { onResult(emptyList()) }
    }

    fun updateScore(
        quizId: String,
        studentUid: String,
        score: Int,
        passed: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        FirebaseInstance.db.collection("quizzes").document(quizId)
            .collection("attempts").document(studentUid)
            .update(mapOf("score" to score, "passed" to passed))
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Gagal update nilai") }
    }

    fun updateQuiz(
        quizId: String,
        title: String,
        description: String,
        deadline: String,
        timeLimit: Int,
        passingScore: Int,
        questions: List<Map<String, Any>>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val data = hashMapOf<String, Any>(
            "title" to title,
            "description" to description,
            "deadline" to deadline,
            "timeLimit" to timeLimit,
            "passingScore" to passingScore,
            "totalQuestions" to questions.size,
            "questions" to questions
        )
        FirebaseInstance.db.collection("quizzes").document(quizId)
            .update(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Gagal mengupdate quiz") }
    }

    fun updateQuizStatus(
        quizId: String,
        status: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        FirebaseInstance.db.collection("quizzes").document(quizId)
            .update("status", status)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Gagal mengubah status") }
    }

    fun deleteQuiz(
        quizId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val quizRef = FirebaseInstance.db.collection("quizzes").document(quizId)

        quizRef.collection("attempts").get()
            .addOnSuccessListener { snapshot ->
                val batch = FirebaseInstance.db.batch()
                for (doc in snapshot.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit()
                    .addOnSuccessListener { quizRef.delete().addOnSuccessListener { onSuccess() }.addOnFailureListener { onError(it.message ?: "Gagal menghapus quiz") } }
                    .addOnFailureListener { onError(it.message ?: "Gagal menghapus attempts") }
            }
            .addOnFailureListener { quizRef.delete().addOnSuccessListener { onSuccess() }.addOnFailureListener { onError(it.message ?: "Gagal menghapus quiz") } }
    }
}
