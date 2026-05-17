package com.example.classhub.data

import com.example.classhub.data.models.Comment
import com.example.classhub.data.models.QuizQuestion
import com.example.classhub.data.models.QuizSet
import com.example.classhub.data.models.ResourceFile

object MockDataSource {

    private var nextFileId = 7
    private var nextQuizId = 3

    val subjects = listOf(
        "All", "Math", "Science", "English", "History",
        "Filipino", "PE", "Computer Science", "Other"
    )

    val files: MutableList<ResourceFile> = mutableListOf(
        ResourceFile(
            id = 1,
            title = "Algebra Final Reviewer",
            description = "Comprehensive reviewer covering linear equations, quadratics, and factoring for the final exam.",
            subject = "Math",
            fileType = "PDF",
            uploaderName = "Maria Santos",
            uploadDate = "May 12, 2025",
            rating = 4.5f,
            ratingCount = 24,
            downloadCount = 156,
            comments = mutableListOf(
                Comment("Juan Dela Cruz", "Super helpful! Saved my grade.", "May 13, 2025"),
                Comment("Ana Reyes", "Clear explanations on quadratics.", "May 14, 2025")
            )
        ),
        ResourceFile(
            id = 2,
            title = "Cell Biology Lecture Notes",
            description = "Detailed notes on cell structure, mitosis, and meiosis with diagrams.",
            subject = "Science",
            fileType = "DOC",
            uploaderName = "Dr. Lee Chen",
            uploadDate = "May 10, 2025",
            rating = 4.8f,
            ratingCount = 31,
            downloadCount = 203,
            comments = mutableListOf(
                Comment("Mark Tan", "Best science notes I've found!", "May 11, 2025")
            )
        ),
        ResourceFile(
            id = 3,
            title = "Rizal Life Timeline Infographic",
            description = "Visual timeline of Jose Rizal's life events for History class.",
            subject = "History",
            fileType = "Image",
            uploaderName = "Patricia Gomez",
            uploadDate = "May 8, 2025",
            rating = 4.2f,
            ratingCount = 15,
            downloadCount = 89,
            comments = mutableListOf()
        ),
        ResourceFile(
            id = 4,
            title = "English Grammar Practice Set",
            description = "50 grammar exercises with answer key for SAT-style prep.",
            subject = "English",
            fileType = "PDF",
            uploaderName = "Teacher Ramos",
            uploadDate = "May 5, 2025",
            rating = 4.0f,
            ratingCount = 18,
            downloadCount = 134,
            comments = mutableListOf(
                Comment("Sofia Lim", "Great for review week.", "May 6, 2025")
            ),
            isSaved = true
        ),
        ResourceFile(
            id = 5,
            title = "Filipino Panitikan Summary",
            description = "Buod ng Noli Me Tangere at El Filibusterismo — per kabanata.",
            subject = "Filipino",
            fileType = "PDF",
            uploaderName = "Carlos Mendoza",
            uploadDate = "May 3, 2025",
            rating = 4.6f,
            ratingCount = 42,
            downloadCount = 278,
            comments = mutableListOf(
                Comment("Liza Cruz", "Salamat po! Malaking tulong.", "May 4, 2025")
            )
        ),
        ResourceFile(
            id = 6,
            title = "Python Basics Cheat Sheet",
            description = "Quick reference for variables, loops, functions, and lists.",
            subject = "Computer Science",
            fileType = "Quiz",
            uploaderName = "Dev Club",
            uploadDate = "May 1, 2025",
            rating = 4.9f,
            ratingCount = 56,
            downloadCount = 312,
            comments = mutableListOf(
                Comment("Alex Wu", "Perfect before the coding quiz!", "May 2, 2025")
            )
        )
    )

    val quizSets: MutableList<QuizSet> = mutableListOf(
        QuizSet(
            id = 1,
            title = "Algebra Basics Quiz",
            subject = "Math",
            difficulty = "Easy",
            questions = listOf(
                QuizQuestion(
                    "What is the value of x in 2x + 4 = 10?",
                    listOf("2", "3", "4", "5"),
                    1
                ),
                QuizQuestion(
                    "Which expression is equivalent to (x + 2)(x - 2)?",
                    listOf("x² - 4", "x² + 4", "x² - 2", "x² + 2x"),
                    0
                ),
                QuizQuestion(
                    "What is the slope of y = 3x + 7?",
                    listOf("3", "7", "10", "1"),
                    0
                ),
                QuizQuestion(
                    "Solve: x² = 16",
                    listOf("x = ±4", "x = 4", "x = -4", "x = 8"),
                    0
                ),
                QuizQuestion(
                    "What is 5² + 3²?",
                    listOf("34", "64", "25", "9"),
                    0
                )
            )
        ),
        QuizSet(
            id = 2,
            title = "Cell Biology Challenge",
            subject = "Science",
            difficulty = "Medium",
            questions = listOf(
                QuizQuestion(
                    "Which organelle is known as the powerhouse of the cell?",
                    listOf("Nucleus", "Mitochondria", "Ribosome", "Golgi apparatus"),
                    1
                ),
                QuizQuestion(
                    "During which phase does DNA replication occur?",
                    listOf("G1", "S", "G2", "M"),
                    1
                ),
                QuizQuestion(
                    "What type of cell lacks a nucleus?",
                    listOf("Eukaryotic", "Prokaryotic", "Plant", "Animal"),
                    1
                ),
                QuizQuestion(
                    "Which process produces gametes?",
                    listOf("Mitosis", "Meiosis", "Binary fission", "Budding"),
                    1
                ),
                QuizQuestion(
                    "The cell membrane is primarily made of:",
                    listOf("Proteins only", "Lipids and proteins", "Carbohydrates", "DNA"),
                    1
                )
            )
        )
    )

    var profileName: String = "Student"

    fun getFileById(id: Int): ResourceFile? = files.find { it.id == id }

    fun getQuizById(id: Int): QuizSet? = quizSets.find { it.id == id }

    fun addFile(file: ResourceFile) {
        files.add(0, file)
    }

    fun createFileId(): Int = nextFileId++

    fun getSavedFiles(): List<ResourceFile> = files.filter { it.isSaved }

    fun filterFiles(
        subject: String,
        query: String,
        sortBy: SortOption
    ): List<ResourceFile> {
        var result = files.toList()
        if (subject != "All") {
            result = result.filter { it.subject == subject }
        }
        if (query.isNotBlank()) {
            val q = query.lowercase()
            result = result.filter {
                it.title.lowercase().contains(q) ||
                    it.description.lowercase().contains(q) ||
                    it.uploaderName.lowercase().contains(q) ||
                    it.subject.lowercase().contains(q)
            }
        }
        result = when (sortBy) {
            SortOption.NEWEST -> result.sortedByDescending { parseDateOrder(it.uploadDate) }
            SortOption.MOST_DOWNLOADED -> result.sortedByDescending { it.downloadCount }
            SortOption.HIGHEST_RATED -> result.sortedByDescending { it.rating }
        }
        return result
    }

    private fun parseDateOrder(date: String): Int {
        val months = listOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        )
        val parts = date.replace(",", "").split(" ")
        if (parts.size >= 3) {
            val month = months.indexOf(parts[0])
            val day = parts[1].toIntOrNull() ?: 0
            val year = parts[2].toIntOrNull() ?: 0
            return year * 10000 + month * 100 + day
        }
        return 0
    }

    enum class SortOption {
        NEWEST, MOST_DOWNLOADED, HIGHEST_RATED
    }
}
