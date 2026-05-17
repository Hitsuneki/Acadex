package com.example.acadex.data

import com.example.acadex.data.model.Comment
import com.example.acadex.data.model.Difficulty
import com.example.acadex.data.model.FileType
import com.example.acadex.data.model.QuizQuestion
import com.example.acadex.data.model.QuizSet
import com.example.acadex.data.model.ResourceFile

object MockDataSource {

    val subjects = listOf("All", "Math", "Science", "English", "Filipino", "History", "CS")

    var profileName: String = "Student"

    private var nextFileId = 6

    val files: MutableList<ResourceFile> = mutableListOf(
        ResourceFile(0, "Calculus Finals Reviewer", "", "Math", FileType.PDF, "Juan dela Cruz", "May 10, 2025", 4.2f, 18, 84,
            mutableListOf(Comment("Ana R.", "Clear and complete.", "May 11, 2025"))),
        ResourceFile(1, "Cell Division Notes", "", "Science", FileType.PDF, "Ana Reyes", "May 9, 2025", 4.7f, 24, 61),
        ResourceFile(2, "Philippine Literature Guide", "", "Filipino", FileType.DOC, "Carlo Bautista", "May 8, 2025", 4.0f, 12, 33),
        ResourceFile(3, "World War II Timeline", "", "History", FileType.IMAGE, "Maria Santos", "May 7, 2025", 4.5f, 15, 47),
        ResourceFile(4, "Algebra Practice Set", "", "Math", FileType.QUIZ, "Pedro Cruz", "May 6, 2025", 4.8f, 31, 92, isSaved = true),
        ResourceFile(5, "Python Basics Reviewer", "", "CS", FileType.PDF, "Lea Gomez", "May 5, 2025", 4.3f, 20, 55)
    )

    val quizSets: List<QuizSet> = listOf(
        QuizSet(
            id = 0,
            title = "Algebra Fundamentals",
            subject = "Math",
            difficulty = Difficulty.EASY,
            questions = listOf(
                QuizQuestion("What is 2x + 4 = 10?", listOf("x = 2", "x = 3", "x = 4", "x = 5"), 1),
                QuizQuestion("Slope of y = 3x + 7?", listOf("3", "7", "10", "1"), 0),
                QuizQuestion("x² = 16 means x = ?", listOf("±4", "4", "-4", "8"), 0),
                QuizQuestion("(x+2)(x-2) = ?", listOf("x²-4", "x²+4", "x²-2", "x²+2x"), 0),
                QuizQuestion("5² + 3² = ?", listOf("34", "64", "25", "9"), 0)
            )
        ),
        QuizSet(
            id = 1,
            title = "Cell Biology Challenge",
            subject = "Science",
            difficulty = Difficulty.MEDIUM,
            questions = listOf(
                QuizQuestion("Powerhouse of the cell?", listOf("Nucleus", "Mitochondria", "Ribosome", "Golgi"), 1),
                QuizQuestion("DNA replication occurs in?", listOf("G1", "S", "G2", "M"), 1),
                QuizQuestion("No nucleus?", listOf("Eukaryotic", "Prokaryotic", "Plant", "Animal"), 1),
                QuizQuestion("Produces gametes?", listOf("Mitosis", "Meiosis", "Fission", "Budding"), 1),
                QuizQuestion("Cell membrane made of?", listOf("Protein only", "Lipids & proteins", "Carbs", "DNA"), 1)
            )
        )
    )

    enum class SortOption { NEWEST, MOST_DOWNLOADED, TOP_RATED }

    fun getFileById(id: Int): ResourceFile? = files.find { it.id == id }
    fun getQuizById(id: Int): QuizSet? = quizSets.find { it.id == id }
    fun createFileId(): Int = nextFileId++
    fun getSavedFiles(): List<ResourceFile> = files.filter { it.isSaved }

    fun filterFiles(subject: String, query: String, sort: SortOption): List<ResourceFile> {
        var list = files.toList()
        if (subject != "All") list = list.filter { it.subject == subject }
        if (query.isNotBlank()) {
            val q = query.lowercase()
            list = list.filter {
                it.title.lowercase().contains(q) || it.uploaderName.lowercase().contains(q) ||
                    it.subject.lowercase().contains(q)
            }
        }
        return when (sort) {
            SortOption.NEWEST -> list.sortedByDescending { it.uploadDate }
            SortOption.MOST_DOWNLOADED -> list.sortedByDescending { it.downloadCount }
            SortOption.TOP_RATED -> list.sortedByDescending { it.rating }
        }
    }
}
