package com.myschedule.id.data

import com.myschedule.id.FirebaseInstance
import com.google.firebase.firestore.FieldValue

object TaskRepository {

    // Teacher: Create Task for a specific class in a university
    fun createTask(
        uniName: String,
        className: String,
        title: String,
        description: String,
        deadline: String,
        fileUrl: String? = null,
        fileName: String? = null,
        linkUrl: String? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val data = hashMapOf(
            "uniName" to uniName,
            "className" to className,
            "title" to title,
            "description" to description,
            "deadline" to deadline,
            "fileUrl" to fileUrl,
            "fileName" to fileName,
            "linkUrl" to linkUrl,
            "createdAt" to FieldValue.serverTimestamp()
        )

        FirebaseInstance.db.collection("tasks")
            .add(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Gagal membuat tugas") }
    }

    // Get Tasks for a specific class
    fun getTasksByClass(uniName: String, className: String, onResult: (List<Map<String, Any>>) -> Unit) {
        FirebaseInstance.db.collection("tasks")
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
    }

    // Student: Submit Task (save file URL)
    fun submitTaskFile(
        taskId: String,
        studentUid: String,
        studentName: String,
        fileUrl: String,
        fileName: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val submissionData = hashMapOf(
            "studentUid" to studentUid,
            "studentName" to studentName,
            "fileUrl" to fileUrl,
            "fileName" to fileName,
            "submittedAt" to FieldValue.serverTimestamp()
        )

        FirebaseInstance.db.collection("tasks").document(taskId)
            .collection("submissions").document(studentUid)
            .set(submissionData)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Gagal mengumpulkan tugas") }
    }

    // Student: Submit Task Link (without file)
    fun submitTaskLink(
        taskId: String,
        studentUid: String,
        studentName: String,
        linkUrl: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val submissionData = hashMapOf(
            "studentUid" to studentUid,
            "studentName" to studentName,
            "submissionLink" to linkUrl,
            "submittedAt" to FieldValue.serverTimestamp()
        )

        FirebaseInstance.db.collection("tasks").document(taskId)
            .collection("submissions").document(studentUid)
            .set(submissionData)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Gagal mengumpulkan tugas") }
    }

    // Get Submissions for a task
    fun getSubmissions(taskId: String, onResult: (List<Map<String, Any>>) -> Unit) {
        FirebaseInstance.db.collection("tasks").document(taskId)
            .collection("submissions")
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.map { doc ->
                    val map = doc.data?.toMutableMap() ?: mutableMapOf()
                    map["id"] = doc.id
                    map
                }
                onResult(list)
            }
    }
    
    // Get single submission by student
    fun getStudentSubmission(taskId: String, studentUid: String, onResult: (Map<String, Any>?) -> Unit) {
        FirebaseInstance.db.collection("tasks").document(taskId)
            .collection("submissions").document(studentUid)
            .get()
            .addOnSuccessListener { doc ->
                onResult(doc.data)
            }
    }

    // Teacher: Update Grade for a submission
    fun updateGrade(taskId: String, studentUid: String, grade: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        FirebaseInstance.db.collection("tasks").document(taskId)
            .collection("submissions").document(studentUid)
            .update("grade", grade)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Gagal update nilai") }
    }

    // Student: Delete own submission
    fun deleteSubmission(taskId: String, studentUid: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        FirebaseInstance.db.collection("tasks").document(taskId)
            .collection("submissions").document(studentUid)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Gagal hapus submission") }
    }
}
