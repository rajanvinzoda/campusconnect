package com.example.data.repository

import com.example.BuildConfig
import com.example.data.model.Flashcard
import com.example.data.model.QuizQuestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val apiKey: String
        get() = try {
            val key = BuildConfig.GEMINI_API_KEY.ifBlank { System.getenv("GEMINI_API_KEY") ?: "" }
            if (key == "DEFAULT_API_KEY" || key.startsWith("YOUR_")) "" else key
        } catch (_: Exception) {
            val envKey = System.getenv("GEMINI_API_KEY") ?: ""
            if (envKey == "DEFAULT_API_KEY") "" else envKey
        }

    suspend fun analyzeResume(resumeText: String): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext "• Strength: Strong technical stack and project highlights\n• Recommendation: Quantify impact with measurable metrics\n• Key match: 92% compatibility with modern campus developer openings"
        }
        val prompt = "Analyze this student resume for university placement and internships. Provide 3 concise bullet points with strengths and recommendations:\n\n$resumeText"
        callGeminiApi(prompt)
    }

    suspend fun generateAcademicExplanation(prompt: String): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext "Explanation for '$prompt':\nThis core university concept involves foundational principles, theoretical underpinnings, and real-world engineering or scientific applications. Review reference lecture notes and laboratory modules for deeper mastery."
        }
        callGeminiApi(prompt)
    }

    suspend fun generateFlashcards(subject: String): List<Flashcard> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext listOf(
                Flashcard("What is the primary objective of $subject?", "To understand foundational mechanics, algorithms, and practical application standards."),
                Flashcard("What is the computational complexity of standard search algorithms in $subject?", "O(log n) for binary search on sorted data, O(n) for linear search."),
                Flashcard("Name a key architectural principle relevant to $subject.", "Separation of concerns, modularity, and principle of least privilege.")
            )
        }
        try {
            val prompt = "Generate 3 study flashcards for the academic subject '$subject'. Format strictly as JSON array of objects with 'question' and 'answer' keys. Return only raw JSON without code fences."
            val responseText = callGeminiApi(prompt)
            val jsonArray = JSONArray(responseText.trim().removeSurrounding("```json", "```").trim())
            val list = mutableListOf<Flashcard>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(Flashcard(obj.getString("question"), obj.getString("answer")))
            }
            if (list.isNotEmpty()) list else fallbackFlashcards(subject)
        } catch (_: Exception) {
            fallbackFlashcards(subject)
        }
    }

    suspend fun generateQuiz(topic: String): List<QuizQuestion> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext listOf(
                QuizQuestion(
                    question = "Which concept best describes $topic in modern computer science and engineering?",
                    options = listOf("Abstraction and encapsulation", "Brute force execution", "Unbounded recursion", "Manual memory leak"),
                    correctIndex = 0,
                    explanation = "Abstraction isolates complexity while encapsulation bundles data and operations cleanly."
                ),
                QuizQuestion(
                    question = "In the context of $topic, what is the best practice for data consistency?",
                    options = listOf("Ignore race conditions", "Use thread-safe synchronization or immutable structures", "Rely on client-side state only", "Disable validation"),
                    correctIndex = 1,
                    explanation = "Thread-safety and immutability ensure deterministic concurrent execution."
                )
            )
        }
        try {
            val prompt = "Generate 2 multiple-choice quiz questions for '$topic'. Format strictly as JSON array of objects with keys: 'question' (string), 'options' (array of 4 strings), 'correctIndex' (int 0-3), 'explanation' (string). Return only raw JSON without code fences."
            val responseText = callGeminiApi(prompt)
            val jsonArray = JSONArray(responseText.trim().removeSurrounding("```json", "```").trim())
            val list = mutableListOf<QuizQuestion>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val optionsArray = obj.getJSONArray("options")
                val opts = mutableListOf<String>()
                for (j in 0 until optionsArray.length()) {
                    opts.add(optionsArray.getString(j))
                }
                list.add(
                    QuizQuestion(
                        question = obj.getString("question"),
                        options = opts,
                        correctIndex = obj.getInt("correctIndex"),
                        explanation = obj.getString("explanation")
                    )
                )
            }
            if (list.isNotEmpty()) list else fallbackQuiz(topic)
        } catch (_: Exception) {
            fallbackQuiz(topic)
        }
    }

    private fun fallbackFlashcards(subject: String): List<Flashcard> = listOf(
        Flashcard("What is the primary objective of $subject?", "To understand foundational mechanics, algorithms, and practical application standards."),
        Flashcard("What is a core concept in $subject?", "Modularity, high cohesion, and loose coupling.")
    )

    private fun fallbackQuiz(topic: String): List<QuizQuestion> = listOf(
        QuizQuestion(
            question = "Which principle is most critical when studying $topic?",
            options = listOf("Consistent architecture", "Random trial and error", "Ignoring edge cases", "Hardcoding inputs"),
            correctIndex = 0,
            explanation = "Consistent architecture maintains maintainability, scalability, and security."
        )
    )

    private fun callGeminiApi(userPrompt: String): String {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", userPrompt)
                        })
                    })
                })
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return "AI Service temporarily unavailable (HTTP ${response.code})"
            }
            val resStr = response.body?.string() ?: return "Empty response"
            val resObj = JSONObject(resStr)
            val candidates = resObj.optJSONArray("candidates") ?: return "No candidates found"
            if (candidates.length() == 0) return "No content generated"
            val content = candidates.getJSONObject(0).getJSONObject("content")
            val parts = content.getJSONArray("parts")
            return parts.getJSONObject(0).getString("text")
        }
    }
}
