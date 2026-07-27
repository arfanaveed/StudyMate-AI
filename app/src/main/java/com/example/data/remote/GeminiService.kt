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
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient

data class GeminiPart(val text: String)
data class GeminiContent(val parts: List<GeminiPart>)
data class GeminiRequest(val contents: List<GeminiContent>)

data class GeminiCandidate(val content: GeminiContent?)
data class GeminiResponse(val candidates: List<GeminiCandidate>?)

interface GeminiApi {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
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
        val envKey = try { System.getenv("GEMINI_API_KEY") ?: "" } catch (e: Exception) { "" }
        if (envKey.isNotBlank() && envKey != "MY_GEMINI_API_KEY") return envKey

        val buildConfigKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
        if (buildConfigKey.isNotBlank() && buildConfigKey != "MY_GEMINI_API_KEY") return buildConfigKey

        return ""
    }

    private val candidateModels = listOf("gemini-1.5-flash", "gemini-2.0-flash", "gemini-1.5-pro")

    suspend fun askAI(prompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isNotEmpty()) {
            val systemInstruction = """
                You are StudyMate AI, an expert, highly encouraging, and intelligent academic tutor for university and college students. Your goal is to guide students to deep understanding by explaining concepts with clarity, precision, and subject-specific depth.

                Subject-Adaptive Guidance:
                - Mathematics / Physics: Provide step-by-step mathematical reasoning, clear formulas, worked examples, and intuitive physical explanations.
                - Computer Science / Engineering: Provide clean code snippets, algorithm explanations, time/space complexity (Big-O), and practical debugging tips.
                - Biology / Medicine / Chemistry: Explain cellular/molecular mechanisms, physiological pathways, chemical reactions, and clear biological analogies.
                - History / Social Sciences / Literature: Provide historical context, cause-and-effect relationships, key arguments, themes, and critical analysis.

                Formatting Rules:
                - Do NOT output the same repetitive rigid template blocks for every answer. Adapt naturally to what is asked.
                - Do NOT generate quizzes unless the user explicitly asks for a quiz.
                - Do NOT generate summaries unless the user explicitly asks to summarize notes.
                - Use markdown formatting (bolding, headers, code blocks, bullet points) naturally where it improves legibility.
            """.trimIndent()

            val req = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(
                            GeminiPart(text = "$systemInstruction\n\nStudent question: $prompt")
                        )
                    )
                )
            )

            for (model in candidateModels) {
                try {
                    val res = api.generateContent(model, apiKey, req)
                    val text = res.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    if (!text.isNullOrBlank()) {
                        return@withContext text
                    }
                } catch (e: Exception) {
                    // Try next candidate model
                }
            }
        }

        getSmartFallbackChatResponse(prompt)
    }

    suspend fun summarizeNotes(notesText: String): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isNotEmpty()) {
            val prompt = """
                You are an expert academic note summarizer. Analyze the provided study notes, textbook excerpt, or lecture transcript and produce a clear, high-density, beautifully structured summary.

                Required Structure:
                1. 📌 **Executive Summary**: A concise 2-3 sentence overview of the core subject matter.
                2. 💡 **Key Concepts & Core Definitions**: Bullet points with bold headers explaining essential terms, formulas, mechanisms, or dates.
                3. 🎯 **Crucial Exam Takeaways**: High-value study points, common pitfalls, and memory shortcuts for test prep.

                Notes to summarize:
                $notesText
            """.trimIndent()

            val req = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(
                            GeminiPart(text = prompt)
                        )
                    )
                )
            )

            for (model in candidateModels) {
                try {
                    val res = api.generateContent(model, apiKey, req)
                    val text = res.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    if (!text.isNullOrBlank()) {
                        return@withContext text
                    }
                } catch (e: Exception) {
                    // Try next candidate model
                }
            }
        }

        getSmartFallbackSummary(notesText)
    }

    suspend fun generateQuiz(topic: String, count: Int, difficulty: String): GeneratedQuiz = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isNotEmpty()) {
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

            for (model in candidateModels) {
                try {
                    val res = api.generateContent(model, apiKey, req)
                    var rawText = res.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                    rawText = rawText.replace("```json", "").replace("```", "").trim()

                    if (rawText.isNotBlank()) {
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
                            return@withContext GeneratedQuiz(topic = topic, difficulty = difficulty, questions = questions)
                        }
                    }
                } catch (e: Exception) {
                    // Try next candidate model
                }
            }
        }

        getFallbackQuiz(topic, count, difficulty)
    }

    private fun getSmartFallbackChatResponse(prompt: String): String {
        val lower = prompt.lowercase()
        return when {
            "hello" in lower || "hi" in lower || "hey" in lower || "greetings" in lower ->
                "Hello! 👋 I'm StudyMate AI, your academic tutor. Whether you need deep explanations in Mathematics, Computer Science, Biology, History, or Physics, I'm here to help you master your coursework. What topic are we exploring today?"

            "calculus" in lower || "derivative" in lower || "integral" in lower || "math" in lower || "algebra" in lower ->
                "📐 **Mathematical Analysis & Problem Solving**\n\n" +
                "To tackle mathematical concepts like derivatives or integration, we break down the problem into structured steps:\n\n" +
                "1. **Foundational Principle**: Differentiation represents instantaneous rate of change (f'(x) = lim_{Δx → 0} [f(x+Δx) - f(x)] / Δx), while integration represents cumulative area under a curve.\n" +
                "2. **Worked Example (Power Rule)**:\n" +
                "   • For f(x) = 3x⁴ - 5x² + 7:\n" +
                "   • f'(x) = 3 · (4x³) - 5 · (2x) + 0 = 12x³ - 10x\n" +
                "3. **Key Integration Technique**: Integration by parts relies on ∫ u dv = uv - ∫ v du.\n\n" +
                "💡 *Tutor Advice*: When solving complex calculus problems, double check whether substitution (u-sub) simplifies the expression before applying advanced methods."

            "biology" in lower || "cell" in lower || "dna" in lower || "genetics" in lower || "photosynthesis" in lower || "enzyme" in lower ->
                "🔬 **Biological Systems & Cellular Mechanisms**\n\n" +
                "Understanding biological processes requires connecting cellular mechanisms to physiological function:\n\n" +
                "• **Cellular Respiration**: C₆H₁₂O₆ + 6O₂ → 6CO₂ + 6H₂O + 36 ATP. Occurs across Glycolysis (cytosol), Krebs Cycle (mitochondrial matrix), and Electron Transport Chain (inner membrane).\n" +
                "• **DNA Replication & Central Dogma**: DNA → Transcription → mRNA → Translation → Functional Protein.\n" +
                "• **Enzyme Kinetics**: Enzymes act as biological catalysts by lowering activation energy (Ea) without altering free energy change (ΔG).\n\n" +
                "🧬 *Concept Connection*: Always remember that structure determines function—from amino acid folding in proteins to the lipid bilayer of cell membranes."

            "code" in lower || "python" in lower || "java" in lower || "computer science" in lower || "algorithm" in lower || "data structure" in lower ->
                "💻 **Computer Science & Algorithmic Design**\n\n" +
                "In software engineering and data structures, performance and readability go hand-in-hand:\n\n" +
                "```python\n" +
                "# Binary Search Algorithm - O(log N) Time Complexity\n" +
                "def binary_search(arr, target):\n" +
                "    low, high = 0, len(arr) - 1\n" +
                "    while low <= high:\n" +
                "        mid = (low + high) // 2\n" +
                "        if arr[mid] == target:\n" +
                "            return mid\n" +
                "        elif arr[mid] < target:\n" +
                "            low = mid + 1\n" +
                "        else:\n" +
                "            high = mid - 1\n" +
                "    return -1\n" +
                "```\n\n" +
                "• **Time Complexity Analysis**:\n" +
                "  - **O(1)**: Direct array index lookup\n" +
                "  - **O(log N)**: Dividing search space in half each iteration\n" +
                "  - **O(N log N)**: Efficient sorting algorithms (Merge Sort, Quick Sort)\n\n" +
                "⚡ *Engineering Principle*: Always check boundary conditions (e.g. empty inputs, single element arrays) before finalizing your code solution."

            "history" in lower || "war" in lower || "revolution" in lower || "century" in lower || "government" in lower ->
                "🏛️ **Historical Context & Critical Analysis**\n\n" +
                "Analyzing historical events involves examining cause-and-effect, social dynamics, and ideological shifts:\n\n" +
                "• **Primary Drivers of Historical Change**:\n" +
                "  1. **Economic Factors**: Resource scarcity, trade routes, and industrialization.\n" +
                "  2. **Ideological Transformations**: Enlightenment philosophy, nationalism, and technological revolutions.\n" +
                "  3. **Geopolitical Coalitions**: Treaties, diplomatic alliances, and balance-of-power shifts.\n\n" +
                "• **Analytical Framework**: Compare primary source perspectives with secondary historiography to evaluate cause vs. immediate catalyst.\n\n" +
                "📜 *Exam Tip*: When writing history essays, structure arguments using point-evidence-analysis (PEA) paragraphs."

            "physics" in lower || "force" in lower || "energy" in lower || "quantum" in lower || "thermodynamics" in lower ->
                "⚡ **Physics Principles & Problem Analysis**\n\n" +
                "Physics connects mathematical equations with real-world physical behavior:\n\n" +
                "• **Newton's Laws of Motion**:\n" +
                "  - F_net = m · a (Force equals mass times acceleration)\n" +
                "  - Conservation of Energy: E_total = K + U = ½mv² + mgh = constant\n" +
                "• **Thermodynamics First Law**: ΔU = Q - W (Change in internal energy equals heat added minus work done by system).\n\n" +
                "🎯 *Problem Solving Strategy*: Always draw a Free-Body Diagram (FBD) and choose a clear coordinate system before writing force balance equations."

            else ->
                "📚 **Academic Conceptual Guide**\n\n" +
                "Let's break down **\"$prompt\"** thoroughly from a tutor's perspective:\n\n" +
                "• **Core Foundation**: To understand this topic deeply, start by identifying the central governing principles and key definitions.\n" +
                "• **Real-World Application**: Connect this concept to practical examples or case studies in your coursework.\n" +
                "• **Study Recommendation**: Practice active recall—try explaining this concept back in your own words without looking at notes!\n\n" +
                "If you have a specific problem set, textbook excerpt, or formula you'd like us to step through together, paste it here!"
        }
    }

    private fun getSmartFallbackSummary(text: String): String {
        val cleanText = text.trim()
        val sentences = cleanText.split(Regex("(?<=[.!?])\\s+")).filter { it.isNotBlank() }
        val preview = if (sentences.size >= 2) sentences.take(2).joinToString(" ") else cleanText.take(150)

        // Extract key terms or bullet items if present
        val lines = cleanText.lines().filter { it.isNotBlank() }
        val bulletItems = lines.filter { it.startsWith("-") || it.startsWith("*") || it.startsWith("•") || it.matches(Regex("^\\d+\\..*")) }

        val keyPoints = if (bulletItems.size >= 3) {
            bulletItems.take(4).joinToString("\n") { "• **Key Note**: ${it.removePrefix("-").removePrefix("*").removePrefix("•").trim()}" }
        } else {
            val middleSentences = if (sentences.size > 2) sentences.drop(2).take(3) else sentences
            middleSentences.joinToString("\n") { "• **Concept Point**: ${it.trim()}" }
        }

        return """
            📝 **Comprehensive Note Summary**

            📌 **Executive Overview**:
            $preview

            💡 **Core Academic Concepts & Key Takeaways**:
            $keyPoints

            🎯 **Exam Preparation & High-Value Focus**:
            • **Critical Focus Area**: Pay special attention to definitions, relationships, and cause-and-effect mechanisms highlighted in the notes above.
            • **Active Review Strategy**: Test your recall on these key takeaways after 24 hours to maximize retention.
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
