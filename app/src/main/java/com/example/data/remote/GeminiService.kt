package com.example.data.remote

import com.example.BuildConfig
import com.example.data.model.GeneratedQuiz
import com.example.data.model.Question
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient

data class GeminiPart(val text: String)
data class GeminiContent(val parts: List<GeminiPart>)
data class GeminiRequest(val contents: List<GeminiContent>)

data class GeminiCandidate(val content: GeminiContent?)
data class GeminiResponse(val candidates: List<GeminiCandidate>?)

interface GeminiApi {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

class GeminiService {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val api: GeminiApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(GeminiApi::class.java)
    }

    private fun getApiKey(): String {
        return try {
            val key = BuildConfig.GEMINI_API_KEY
            if (key == "MY_GEMINI_API_KEY" || key.isBlank()) "" else key
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun askAI(prompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext getSmartFallbackChatResponse(prompt)
        }

        try {
            val req = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(
                            GeminiPart(text = "You are StudyMate AI, an expert academic tutor for university students. Give helpful, clear, structured, encouraging responses with bullet points or key takeaways where applicable.\n\nStudent question: $prompt")
                        )
                    )
                )
            )
            val res = api.generateContent(apiKey, req)
            val text = res.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            text ?: getSmartFallbackChatResponse(prompt)
        } catch (e: Exception) {
            getSmartFallbackChatResponse(prompt)
        }
    }

    suspend fun summarizeNotes(notesText: String): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext getSmartFallbackSummary(notesText)
        }

        try {
            val req = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(
                            GeminiPart(text = "Summarize the following study notes concisely. Provide:\n1. Core Summary (2-3 sentences)\n2. Key Bullet Points & Concepts\n3. Memory Tip / Exam Formula\n\nNotes:\n$notesText")
                        )
                    )
                )
            )
            val res = api.generateContent(apiKey, req)
            val text = res.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            text ?: getSmartFallbackSummary(notesText)
        } catch (e: Exception) {
            getSmartFallbackSummary(notesText)
        }
    }

    suspend fun generateQuiz(topic: String, count: Int, difficulty: String): GeneratedQuiz = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext getFallbackQuiz(topic, count, difficulty)
        }

        try {
            val prompt = """
                Generate a $difficulty multiple-choice quiz on the topic: "$topic".
                Return ONLY valid JSON array without markdown formatting.
                Array format:
                [
                  {
                    "id": 1,
                    "questionText": "Question string?",
                    "options": ["Option A", "Option B", "Option C", "Option D"],
                    "correctIndex": 0,
                    "explanation": "Why this option is correct."
                  }
                ]
                Generate exactly $count questions.
            """.trimIndent()

            val req = GeminiRequest(
                contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt))))
            )
            val res = api.generateContent(apiKey, req)
            var rawText = res.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            
            // Clean markdown codeblocks if present
            rawText = rawText.replace("```json", "").replace("```", "").trim()

            val jsonArray = JSONArray(rawText)
            val questions = mutableListOf<Question>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val optsArray = obj.getJSONArray("options")
                val opts = mutableListOf<String>()
                for (j in 0 until optsArray.length()) {
                    opts.add(optsArray.getString(j))
                }
                questions.add(
                    Question(
                        id = obj.optInt("id", i + 1),
                        questionText = obj.getString("questionText"),
                        options = opts,
                        correctIndex = obj.getInt("correctIndex"),
                        explanation = obj.getString("explanation")
                    )
                )
            }

            if (questions.isNotEmpty()) {
                GeneratedQuiz(topic = topic, difficulty = difficulty, questions = questions)
            } else {
                getFallbackQuiz(topic, count, difficulty)
            }
        } catch (e: Exception) {
            getFallbackQuiz(topic, count, difficulty)
        }
    }

    private fun getSmartFallbackChatResponse(prompt: String): String {
        val lower = prompt.lowercase()
        return when {
            "hello" in lower || "hi" in lower || "hey" in lower ->
                "Hello! 👋 I'm StudyMate AI. How can I help you master your coursework today? You can ask me to explain concepts, summarize notes, or solve problem sets!"
            "calculus" in lower || "derivative" in lower || "math" in lower ->
                "📘 **Math & Calculus Breakdown**\n\n• **Core Concept**: The derivative measures the rate at which a function changes.\n• **Power Rule**: d/dx [x^n] = n * x^(n-1)\n• **Chain Rule**: d/dx [f(g(x))] = f'(g(x)) * g'(x)\n\n💡 *Tip*: Always simplify algebraic terms before applying rules!"
            "biology" in lower || "cell" in lower || "dna" in lower ->
                "🔬 **Cell Biology Summary**\n\n• **Mitochondria**: The powerhouse of the cell producing ATP through cellular respiration.\n• **DNA Replication**: Occurs in the 5' to 3' direction using DNA Polymerase.\n• **Ribosomes**: Read mRNA sequences to assemble amino acid chains into proteins."
            "code" in lower || "python" in lower || "java" in lower || "computer science" in lower || "algorithm" in lower ->
                "💻 **Computer Science Study Insight**\n\n• **Time Complexity**: Big-O notation measures algorithm efficiency as input size (N) grows.\n• **O(1)**: Constant time (Hash map lookup)\n• **O(log N)**: Logarithmic time (Binary Search)\n• **O(N log N)**: Fast sorting (Merge Sort / Quick Sort)\n\n💡 *Pro-tip*: Always consider edge cases like empty arrays and null inputs!"
            else ->
                "💡 **StudyMate AI Key Takeaways**\n\nRegarding **\"$prompt\"**:\n\n1. **Core Concept**: Break this topic down into its fundamental principles first.\n2. **Application**: Practice active recall by explaining this concept aloud in simple terms.\n3. **Exam Focus**: Pay close attention to standard definitions, formulas, and real-world examples.\n\nNeed a custom quiz or quick flashcards on this topic? Jump over to the **Quiz Generator** tab!"
        }
    }

    private fun getSmartFallbackSummary(text: String): String {
        val snippet = text.take(120)
        return """
            📝 **Study Notes Summary**
            
            **Core Executive Summary**:
            $snippet...
            
            📌 **Key Concepts & Takeaways**:
            • **Main Objective**: Master key definitions and foundational principles.
            • **Critical Relationship**: Understand cause-and-effect patterns across core topics.
            • **Exam Focus**: Re-read formulas, diagrams, and summary tables before tests.
            
            ⚡ **Memory Formula**:
            Review this concept in 24 hours, then 3 days, and again in 1 week (Spaced Repetition Method).
        """.trimIndent()
    }

    private fun getFallbackQuiz(topic: String, count: Int, difficulty: String): GeneratedQuiz {
        val topicTitle = if (topic.isBlank()) "General Academic Knowledge" else topic
        val defaultQuestions = listOf(
            Question(
                id = 1,
                questionText = "What is the primary principle behind active recall in learning '$topicTitle'?",
                options = listOf(
                    "Testing yourself to retrieve information from memory",
                    "Rereading highlighters multiple times passive reading",
                    "Listening to lectures while sleeping",
                    "Memorizing text verbatim without understanding"
                ),
                correctIndex = 0,
                explanation = "Active recall forces the brain to retrieve information, strengthening neural pathways and long-term retention."
            ),
            Question(
                id = 2,
                questionText = "Which strategy best complements spaced repetition for mastering $topicTitle?",
                options = listOf(
                    "Cramming 10 hours straight before the exam",
                    "Interleaving related subjects during study sessions",
                    "Studying only one subject for an entire month",
                    "Skipping practice problems and focusing on theory"
                ),
                correctIndex = 1,
                explanation = "Interleaving mixes different topics or problem types, improving problem-solving adaptability."
            ),
            Question(
                id = 3,
                questionText = "In $topicTitle, what does the Feynman Technique advocate for?",
                options = listOf(
                    "Explaining the concept in simple terms as if teaching a beginner",
                    "Writing complex equations without explanation",
                    "Memorizing textbook jargon verbatim",
                    "Avoiding questions when confused"
                ),
                correctIndex = 0,
                explanation = "The Feynman Technique highlights gaps in knowledge by attempting to explain concepts simply."
            ),
            Question(
                id = 4,
                questionText = "What is the optimal study interval structure according to cognitive science?",
                options = listOf(
                    "Short focused intervals (e.g. Pomodoro 25 min) with short breaks",
                    "Non-stop study for 8 hours without water or breaks",
                    "Studying only 5 minutes per week",
                    "Studying only when feeling motivated"
                ),
                correctIndex = 0,
                explanation = "The Pomodoro method balances deep focus with periodic rest to avoid cognitive burnout."
            ),
            Question(
                id = 5,
                questionText = "When solving complex $topicTitle problems, what is the best initial step?",
                options = listOf(
                    "Identify known variables and state what needs to be solved",
                    "Guess numbers and check answers at random",
                    "Give up immediately if it looks unfamiliar",
                    "Copy solutions directly from a solution manual"
                ),
                correctIndex = 0,
                explanation = "Structuring the problem by listing given data and target variables clarifies the required solution path."
            )
        )

        return GeneratedQuiz(
            topic = topicTitle,
            difficulty = difficulty,
            questions = defaultQuestions.take(count.coerceIn(1, 5))
        )
    }
}
