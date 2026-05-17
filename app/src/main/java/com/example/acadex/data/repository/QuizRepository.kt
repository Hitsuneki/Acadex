package com.example.acadex.data.repository

import android.util.Log
import com.example.acadex.data.model.Difficulty
import com.example.acadex.data.model.QuizHistoryEntry
import com.example.acadex.data.model.QuizQuestion
import com.example.acadex.data.model.QuizSet
import com.example.acadex.data.result.RepoResult
import com.example.acadex.data.result.userMessage
import com.example.acadex.data.supabase.QuizHistoryInsert
import com.example.acadex.data.supabase.QuizHistoryRow
import com.example.acadex.data.supabase.QuizQuestionRow
import com.example.acadex.data.supabase.QuizSetRow
import com.example.acadex.data.supabase.SupabaseClient
import com.example.acadex.util.UserIdentity
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object QuizRepository {

    private const val TAG = "QuizRepository"
    private const val NETWORK = "No connection. Please check your internet."
    private const val SERVER = "Something went wrong. Please try again."

    suspend fun fetchQuizSets(): RepoResult<List<QuizSet>> = withContext(Dispatchers.IO) {
        runRepo {
            val sets = client().postgrest.from("quiz_sets").select {
                order(column = "created_at", order = Order.DESCENDING)
            }.decodeList<QuizSetRow>()
            sets.map { row ->
                val questions = client().postgrest.from("quiz_questions").select {
                    filter { eq("quiz_set_id", row.id) }
                    order(column = "sort_order", order = Order.ASCENDING)
                }.decodeList<QuizQuestionRow>()
                row.toQuizSet(questions)
            }.filter { it.questions.isNotEmpty() }
        }
    }

    suspend fun fetchQuizById(quizSetId: String): RepoResult<QuizSet> = withContext(Dispatchers.IO) {
        runRepo {
            val row = client().postgrest.from("quiz_sets").select {
                filter { eq("id", quizSetId) }
            }.decodeSingle<QuizSetRow>()
            val questions = client().postgrest.from("quiz_questions").select {
                filter { eq("quiz_set_id", quizSetId) }
                order(column = "sort_order", order = Order.ASCENDING)
            }.decodeList<QuizQuestionRow>()
            row.toQuizSet(questions)
        }
    }

    suspend fun recordHistory(quizSetId: String, score: Int, total: Int) {
        if (!SupabaseClient.isConfigured) return
        withContext(Dispatchers.IO) {
            try {
                val uid = UserIdentity.uidOrNull() ?: return@withContext
                client().postgrest.from("quiz_history").insert(
                    QuizHistoryInsert(userId = uid, quizSetId = quizSetId, score = score, total = total)
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to record quiz history", e)
            }
        }
    }

    suspend fun fetchHistory(): RepoResult<List<QuizHistoryEntry>> = withContext(Dispatchers.IO) {
        val uid = UserIdentity.requireUid()
        runRepo {
            val history = client().postgrest.from("quiz_history").select {
                filter { eq("user_id", uid) }
                order(column = "taken_at", order = Order.DESCENDING)
            }.decodeList<QuizHistoryRow>()

            if (history.isEmpty()) return@runRepo emptyList()

            val quizIds = history.map { it.quizSetId }.distinct()
            val sets = client().postgrest.from("quiz_sets").select {
                filter { isIn("id", quizIds) }
            }.decodeList<QuizSetRow>().associateBy { it.id }

            history.map { h ->
                val set = sets[h.quizSetId]
                QuizHistoryEntry(
                    id = h.id,
                    quizTitle = set?.title ?: "Quiz",
                    subject = set?.subject ?: "",
                    difficulty = set?.difficulty ?: "EASY",
                    score = h.score,
                    total = h.total,
                    takenAt = formatDate(h.takenAt)
                )
            }
        }
    }

    suspend fun clearHistory(): RepoResult<Unit> = withContext(Dispatchers.IO) {
        val uid = UserIdentity.requireUid()
        runRepo {
            client().postgrest.from("quiz_history").delete {
                filter { eq("user_id", uid) }
            }
            Unit
        }
    }

    private fun QuizSetRow.toQuizSet(questions: List<QuizQuestionRow>) = QuizSet(
        id = id,
        title = title,
        subject = subject,
        difficulty = when (difficulty.uppercase()) {
            "MEDIUM" -> Difficulty.MEDIUM
            "HARD" -> Difficulty.HARD
            else -> Difficulty.EASY
        },
        questions = questions.map {
            QuizQuestion(
                question = it.prompt,
                choices = it.options,
                correctIndex = it.correctIndex
            )
        }
    )

    private inline fun <T> runRepo(block: () -> T): RepoResult<T> {
        if (!SupabaseClient.isConfigured) return RepoResult.Error(SERVER)
        return try {
            RepoResult.Success(block())
        } catch (e: Exception) {
            Log.e(TAG, "Quiz error", e)
            RepoResult.Error(e.userMessage(NETWORK, SERVER), e)
        }
    }

    private fun client() = SupabaseClient.instance

    private fun formatDate(iso: String?): String {
        if (iso.isNullOrBlank()) return ""
        return runCatching {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val out = SimpleDateFormat("MMM d, yyyy", Locale.US)
            val parsed = parser.parse(iso.take(19)) ?: return iso
            out.format(parsed)
        }.getOrDefault(iso)
    }
}
