package com.example.android.interviewassistant.domain

import com.example.android.interviewassistant.data.local.AppDatabase
import com.example.android.interviewassistant.data.local.entity.*
import com.google.gson.Gson
import java.util.UUID

class DemoDataSeeder(private val db: AppDatabase) {

    private val gson = Gson()
    private val now = System.currentTimeMillis()
    private val oneDay = 86_400_000L
    private val oneHour = 3_600_000L

    suspend fun seed(role: String, level: String) {
        seedSessions(role, level)
        seedFlashcards(role)
        seedDailyFaqs(role, level)
    }

    // ── Sessions + Messages + Scores + Episodic Memory ───────────────────────

    private suspend fun seedSessions(role: String, level: String) {
        val sessions = listOf(
            SessionSeed(
                daysAgo = 3,
                domain = "algorithms",
                overallScore = 62f,
                summary = "Solid grasp of basic data structures but struggled with time complexity analysis for graph algorithms. Communication was clear but answers lacked depth on edge cases.",
                weakSpots = listOf("Graph traversal", "Time complexity", "Edge case handling"),
                questions = listOf(
                    QaPair(
                        q = "Can you explain the difference between a stack and a queue? When would you use each?",
                        a = "A stack is LIFO and a queue is FIFO. I'd use a stack for undo operations or DFS, and a queue for BFS or task scheduling.",
                        topic = "Data Structures", clarity = 78f, correctness = 82f, communication = 75f, edgeCases = 55f,
                        feedback = "Good foundational understanding. Consider mentioning thread-safe variants like ConcurrentLinkedQueue for production use."
                    ),
                    QaPair(
                        q = "How would you detect a cycle in a linked list? What's the time and space complexity?",
                        a = "I'd use Floyd's tortoise and hare algorithm with two pointers moving at different speeds. It's O(n) time and O(1) space.",
                        topic = "Linked Lists", clarity = 80f, correctness = 85f, communication = 72f, edgeCases = 50f,
                        feedback = "Correct algorithm choice. You should also discuss what happens when the list is empty or has only one node."
                    ),
                    QaPair(
                        q = "Given a graph with weighted edges, how would you find the shortest path between two nodes?",
                        a = "I would use Dijkstra's algorithm. Start from the source, use a priority queue, and relax edges greedily.",
                        topic = "Graph Algorithms", clarity = 65f, correctness = 70f, communication = 68f, edgeCases = 40f,
                        feedback = "Correct choice for non-negative weights but you didn't mention that limitation. For negative weights you'd need Bellman-Ford."
                    )
                )
            ),
            SessionSeed(
                daysAgo = 2,
                domain = "system-design",
                overallScore = 71f,
                summary = "Demonstrated good high-level thinking for URL shortener design. Load balancing and caching strategies were well-articulated. Needs more depth on database partitioning and failure scenarios.",
                weakSpots = listOf("Database sharding", "Failure recovery", "Capacity estimation"),
                questions = listOf(
                    QaPair(
                        q = "Design a URL shortener service like bit.ly. Walk me through your approach.",
                        a = "I'd use a base62 encoding of an auto-incrementing ID stored in a relational DB. Put a cache layer in front with Redis for hot URLs, and use a load balancer to distribute traffic.",
                        topic = "URL Shortener", clarity = 75f, correctness = 78f, communication = 80f, edgeCases = 60f,
                        feedback = "Good starting point. Consider discussing hash collisions, custom aliases, and analytics tracking as product requirements."
                    ),
                    QaPair(
                        q = "How would you handle the scenario where your URL shortener gets 10x the expected traffic?",
                        a = "I'd scale horizontally with more app servers behind the load balancer, add read replicas for the database, and increase the Redis cache capacity. Could also use a CDN for redirect responses.",
                        topic = "Scalability", clarity = 82f, correctness = 75f, communication = 78f, edgeCases = 55f,
                        feedback = "Reasonable scaling strategy. You should quantify expected QPS and discuss database partitioning or sharding strategy for the shortcode-to-URL mapping."
                    )
                )
            ),
            SessionSeed(
                daysAgo = 1,
                domain = "behavioral",
                overallScore = 79f,
                summary = "Strong STAR format responses with concrete examples. Leadership and conflict resolution examples were compelling. Could improve on quantifying impact and tying outcomes to business metrics.",
                weakSpots = listOf("Quantifying impact", "Business metrics"),
                questions = listOf(
                    QaPair(
                        q = "Tell me about a time you had to deal with a difficult team member. How did you handle it?",
                        a = "In my last project, a teammate consistently missed deadlines. I scheduled a private 1-on-1, learned they were struggling with unclear requirements. I started writing clearer specs and paired with them on complex tasks. The team's velocity improved by 20% over the next sprint.",
                        topic = "Conflict Resolution", clarity = 85f, correctness = 80f, communication = 88f, edgeCases = 70f,
                        feedback = "Excellent STAR format. The quantified outcome (20% velocity improvement) is strong. Consider also mentioning how you followed up to ensure the improvement was sustained."
                    ),
                    QaPair(
                        q = "Describe a situation where you had to make a technical decision with incomplete information.",
                        a = "We needed to choose between microservices and a monolith for a new product. With limited load data, I proposed starting monolithic with clear module boundaries, planning to extract services later based on actual traffic patterns. This saved us 3 months of upfront infrastructure work.",
                        topic = "Decision Making", clarity = 80f, correctness = 82f, communication = 85f, edgeCases = 65f,
                        feedback = "Good pragmatic approach. Strengthen the answer by discussing how you mitigated the risk of your decision being wrong — what was your Plan B?"
                    )
                )
            )
        )

        for (s in sessions) {
            val sessionId = UUID.randomUUID().toString()
            val sessionTime = now - (s.daysAgo * oneDay) + (10 * oneHour)

            db.sessionDao().insertSession(
                SessionEntity(
                    id = sessionId,
                    role = role,
                    level = level,
                    domain = s.domain,
                    status = "completed",
                    createdAt = sessionTime,
                    endedAt = sessionTime + (30 * 60_000L),
                    overallScore = s.overallScore,
                    summary = s.summary,
                    weakSpots = gson.toJson(s.weakSpots)
                )
            )

            var msgTime = sessionTime
            for ((idx, qa) in s.questions.withIndex()) {
                db.sessionDao().insertMessage(
                    MessageEntity(
                        id = UUID.randomUUID().toString(),
                        sessionId = sessionId,
                        role = "interviewer",
                        content = qa.q,
                        timestamp = msgTime
                    )
                )
                msgTime += 60_000L

                db.sessionDao().insertMessage(
                    MessageEntity(
                        id = UUID.randomUUID().toString(),
                        sessionId = sessionId,
                        role = "candidate",
                        content = qa.a,
                        timestamp = msgTime
                    )
                )
                msgTime += 60_000L

                db.sessionDao().insertScore(
                    ScoreEntity(
                        id = UUID.randomUUID().toString(),
                        sessionId = sessionId,
                        topic = qa.topic,
                        clarity = qa.clarity,
                        correctness = qa.correctness,
                        communication = qa.communication,
                        edgeCases = qa.edgeCases,
                        feedback = qa.feedback,
                        createdAt = sessionTime + ((idx + 1) * 5 * 60_000L)
                    )
                )
            }

            db.episodicMemoryDao().insert(
                EpisodicMemoryEntity(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    summary = s.summary,
                    createdAt = sessionTime + (30 * 60_000L)
                )
            )
        }
    }

    // ── Flashcards ───────────────────────────────────────────────────────────

    private suspend fun seedFlashcards(role: String) {
        val cards = listOf(
            CardSeed("What is the time complexity of HashMap.get() in Java?",
                "O(1) average case due to hash-based indexing. Worst case is O(n) when many keys collide into the same bucket, though Java 8+ uses balanced trees for long chains bringing worst case to O(log n).",
                "Data Structures", reviewedDaysAgo = 2, easeFactor = 2.8, interval = 6, reps = 3),
            CardSeed("Explain the CAP theorem.",
                "In a distributed system you can only guarantee two of three properties: Consistency (all nodes see the same data), Availability (every request gets a response), and Partition tolerance (system works despite network failures). Most real systems choose AP or CP.",
                "System Design", reviewedDaysAgo = 1, easeFactor = 2.5, interval = 3, reps = 2),
            CardSeed("What is the difference between a process and a thread?",
                "A process is an independent execution unit with its own memory space. A thread is a lightweight unit within a process that shares memory with other threads. Threads are cheaper to create and context-switch but require synchronization for shared data.",
                "Operating Systems", reviewedDaysAgo = null, easeFactor = 2.5, interval = 1, reps = 0),
            CardSeed("What is database normalization? Name the first three normal forms.",
                "Normalization reduces data redundancy by organizing tables. 1NF: atomic values, no repeating groups. 2NF: 1NF + no partial dependencies on composite keys. 3NF: 2NF + no transitive dependencies (non-key columns depend only on the primary key).",
                "Databases", reviewedDaysAgo = 3, easeFactor = 2.6, interval = 4, reps = 2),
            CardSeed("Explain the SOLID principles in object-oriented design.",
                "S: Single Responsibility — one reason to change. O: Open/Closed — open for extension, closed for modification. L: Liskov Substitution — subtypes must be substitutable. I: Interface Segregation — prefer small interfaces. D: Dependency Inversion — depend on abstractions.",
                "OOP Design", reviewedDaysAgo = null, easeFactor = 2.5, interval = 1, reps = 0),
            CardSeed("What is a deadlock? How can you prevent it?",
                "A deadlock occurs when two or more threads are blocked forever, each waiting for a resource held by the other. Prevent via: consistent lock ordering, timeout-based locking, deadlock detection algorithms, or using lock-free data structures.",
                "Concurrency", reviewedDaysAgo = 1, easeFactor = 2.3, interval = 2, reps = 2),
            CardSeed("What is the difference between TCP and UDP?",
                "TCP is connection-oriented with guaranteed delivery, ordering, and flow control — used for HTTP, email, file transfer. UDP is connectionless with no delivery guarantees — used for streaming, gaming, DNS where speed matters more than reliability.",
                "Networking", reviewedDaysAgo = null, easeFactor = 2.5, interval = 1, reps = 0),
            CardSeed("Explain how garbage collection works in the JVM.",
                "The JVM divides heap into Young (Eden + Survivor) and Old generations. New objects go to Eden; minor GC copies survivors. Objects surviving multiple cycles promote to Old gen. Major GC cleans Old gen. Modern collectors like G1 aim for predictable pause times.",
                "JVM Internals", reviewedDaysAgo = 2, easeFactor = 2.7, interval = 5, reps = 3),
            CardSeed("What is the STAR method for behavioral interviews?",
                "STAR stands for Situation (context), Task (your responsibility), Action (what you did), Result (outcome with metrics). Structure every behavioral answer this way to give concrete, measurable examples instead of vague claims.",
                "Behavioral", reviewedDaysAgo = 0, easeFactor = 2.9, interval = 8, reps = 4),
            CardSeed("What is an API gateway and why use one?",
                "An API gateway is a single entry point for client requests that handles routing, authentication, rate limiting, load balancing, and response caching. It decouples clients from backend microservice topology and centralizes cross-cutting concerns.",
                "System Design", reviewedDaysAgo = null, easeFactor = 2.5, interval = 1, reps = 0)
        )

        val entities = cards.map { c ->
            val lastReviewed = c.reviewedDaysAgo?.let { now - (it * oneDay) }
            val nextReview = if (c.reviewedDaysAgo != null) {
                (lastReviewed ?: now) + (c.interval * oneDay)
            } else {
                now - oneHour // unreviewed cards are due now
            }
            FlashcardEntity(
                id = UUID.randomUUID().toString(),
                question = c.question,
                answer = c.answer,
                topic = c.topic,
                source = "generated",
                createdAt = now - (5 * oneDay) + (cards.indexOf(c) * oneHour),
                easeFactor = c.easeFactor,
                interval = c.interval,
                repetitions = c.reps,
                nextReview = nextReview,
                lastReviewed = lastReviewed
            )
        }
        db.flashcardDao().insertAll(entities)
    }

    // ── Daily FAQs ───────────────────────────────────────────────────────────

    private suspend fun seedDailyFaqs(role: String, level: String) {
        val dayFaqs = listOf(
            DayFaqSeed(daysAgo = 2, faqs = listOf(
                FaqSeed("What is the difference between an abstract class and an interface?",
                    "An abstract class can have state (fields) and partial implementation. An interface defines a contract with no state (before Java 8 default methods). Use abstract classes for shared base behavior; interfaces for capability contracts across unrelated classes.",
                    "geeksforgeeks"),
                FaqSeed("How would you design a rate limiter?",
                    "Use a token bucket or sliding window algorithm. Track requests per user/IP in Redis with TTL-based keys. Return HTTP 429 when the limit is exceeded. Consider distributed rate limiting with a centralized store for multi-server deployments.",
                    "leetcode"),
                FaqSeed("Explain eventual consistency in distributed systems.",
                    "Eventual consistency means that given enough time without new updates, all replicas will converge to the same state. It trades immediate consistency for higher availability and partition tolerance (AP in CAP theorem). Used by DynamoDB, Cassandra.",
                    "stackoverflow"),
                FaqSeed("What questions should I ask the interviewer at the end?",
                    "Ask about team culture, current technical challenges, growth opportunities, and the product roadmap. Avoid asking about salary in technical rounds. Good examples: 'What does the onboarding process look like?' or 'What's the biggest technical challenge your team is facing?'",
                    "glassdoor"),
                FaqSeed("How do you handle a situation where your manager disagrees with your technical approach?",
                    "Present data and trade-offs objectively. Build a proof-of-concept if feasible. Ultimately respect the decision while documenting your concerns. Frame it as collaboration, not confrontation — use STAR format for your answer.",
                    "glassdoor"),
                FaqSeed("What is the difference between horizontal and vertical scaling?",
                    "Vertical scaling (scale up) means adding more CPU/RAM to a single machine — simpler but has hardware limits. Horizontal scaling (scale out) means adding more machines — requires load balancing and distributed state management but scales practically without limit.",
                    "interviewbit"),
                FaqSeed("Explain the concept of database indexing. When should you NOT use an index?",
                    "An index is a data structure (usually B-tree) that speeds up lookups at the cost of write performance and storage. Avoid indexes on frequently updated columns, low-cardinality columns, or small tables where full scans are faster.",
                    "internet"),
                FaqSeed("What are microservices? What are the trade-offs vs. a monolith?",
                    "Microservices decompose an app into independently deployable services. Benefits: independent scaling, polyglot tech stacks, fault isolation. Trade-offs: network latency, distributed transactions, operational complexity, debugging difficulty.",
                    "leetcode")
            )),
            DayFaqSeed(daysAgo = 1, faqs = listOf(
                FaqSeed("What is dependency injection and why is it useful?",
                    "DI is a design pattern where dependencies are provided to a class rather than created internally. Benefits: testability (easy mocking), loose coupling, and configurability. Common frameworks: Spring (Java), Dagger/Hilt (Android), Guice.",
                    "geeksforgeeks"),
                FaqSeed("How would you troubleshoot a production service that's responding slowly?",
                    "Check metrics dashboards (latency, error rate, throughput). Look at recent deployments. Check database query performance (slow query log). Review CPU/memory/disk I/O. Check external dependency health. Use distributed tracing to find bottlenecks.",
                    "glassdoor"),
                FaqSeed("Explain the difference between SQL and NoSQL databases.",
                    "SQL databases (PostgreSQL, MySQL) use structured schemas with ACID transactions — great for complex queries and relationships. NoSQL (MongoDB, DynamoDB) offers flexible schemas and horizontal scaling — better for unstructured data and high write throughput.",
                    "interviewbit"),
                FaqSeed("What is a load balancer and what algorithms does it use?",
                    "A load balancer distributes traffic across servers. Algorithms: Round Robin (equal distribution), Least Connections (send to least busy), IP Hash (session affinity), Weighted (based on server capacity). L4 operates at TCP level, L7 at HTTP level.",
                    "internet"),
                FaqSeed("Describe a time you improved the performance of an existing system.",
                    "Use STAR format. Example: identified N+1 query problem via profiling (Situation/Task). Added eager loading and database indexes, implemented Redis caching for hot data (Action). Reduced API latency from 800ms to 120ms, cut DB load by 60% (Result).",
                    "glassdoor"),
                FaqSeed("What is Docker and how does it differ from a virtual machine?",
                    "Docker uses OS-level containerization — containers share the host kernel and are lightweight (MB, seconds to start). VMs include a full OS with a hypervisor — heavier (GB, minutes to start) but provide stronger isolation. Docker is standard for microservice deployment.",
                    "stackoverflow"),
                FaqSeed("How do you ensure your code is production-ready?",
                    "Write unit and integration tests with good coverage. Use CI/CD pipelines. Add logging, monitoring, and alerting. Handle errors gracefully. Conduct code reviews. Load test critical paths. Document API contracts and runbooks.",
                    "leetcode"),
                FaqSeed("What is the two-pointer technique? Give an example.",
                    "Two-pointer uses two indices traversing an array, often from both ends toward the center. Example: finding a pair that sums to a target in a sorted array — left pointer starts at 0, right at end, move inward based on current sum vs. target. O(n) time.",
                    "interviewbit")
            )),
            DayFaqSeed(daysAgo = 0, faqs = listOf(
                FaqSeed("What is REST and what makes an API RESTful?",
                    "REST (Representational State Transfer) uses HTTP methods (GET, POST, PUT, DELETE) on resources identified by URLs. Key constraints: stateless requests, uniform interface, client-server separation, cacheable responses, layered system architecture.",
                    "geeksforgeeks"),
                FaqSeed("Explain the concept of Big O notation with examples.",
                    "Big O describes the upper bound of an algorithm's growth rate. O(1): hash lookup. O(log n): binary search. O(n): linear scan. O(n log n): merge sort. O(n²): bubble sort. O(2ⁿ): recursive Fibonacci. Focus on worst-case behavior as input grows.",
                    "leetcode"),
                FaqSeed("How would you design a chat application like Slack?",
                    "Use WebSockets for real-time messaging. Store messages in a database (Cassandra for scale). Implement channels/DMs with access control. Add message search via Elasticsearch. Handle presence (online/offline) with Redis pub/sub. CDN for file attachments.",
                    "interviewbit"),
                FaqSeed("What is CI/CD and why is it important?",
                    "CI (Continuous Integration) merges code frequently with automated builds/tests. CD (Continuous Delivery/Deployment) automates release to staging or production. Benefits: faster feedback loops, fewer integration issues, reliable and repeatable deployments.",
                    "internet"),
                FaqSeed("Tell me about a project you're most proud of.",
                    "Use STAR format highlighting technical complexity and personal impact. Emphasize: the problem you solved, technologies chosen and why, challenges overcome, quantified results (users served, performance gains, revenue impact). Show ownership and learning.",
                    "glassdoor"),
                FaqSeed("What is caching and what are common eviction strategies?",
                    "Caching stores frequently accessed data in fast storage (memory) to reduce latency. Eviction strategies: LRU (Least Recently Used), LFU (Least Frequently Used), FIFO (First In First Out), TTL (Time To Live). Common tools: Redis, Memcached.",
                    "stackoverflow"),
                FaqSeed("How do you handle conflicts during code review?",
                    "Focus on the code, not the person. Provide clear reasoning with examples or documentation. Ask questions instead of making demands. Offer alternatives. If deadlocked, defer to team conventions or get a third opinion. Ultimately, prioritize team velocity.",
                    "glassdoor"),
                FaqSeed("What is the difference between a mutex and a semaphore?",
                    "A mutex (mutual exclusion) allows only one thread to access a resource — like a single key to a room. A semaphore allows N concurrent accesses — like a parking lot with N spots. Mutex is owned by a thread; semaphores have no ownership concept.",
                    "leetcode")
            ))
        )

        for (day in dayFaqs) {
            val fetchedAt = now - (day.daysAgo * oneDay) + (8 * oneHour)
            val entities = day.faqs.map { faq ->
                DailyFaqEntity(
                    id = UUID.randomUUID().toString(),
                    topic = role,
                    question = faq.question,
                    answer = faq.answer,
                    source = faq.source,
                    fetchedAt = fetchedAt
                )
            }
            db.dailyFaqDao().insertAll(entities)
        }
    }

    // ── Seed data classes ────────────────────────────────────────────────────

    private data class QaPair(
        val q: String, val a: String, val topic: String,
        val clarity: Float, val correctness: Float,
        val communication: Float, val edgeCases: Float,
        val feedback: String
    )

    private data class SessionSeed(
        val daysAgo: Int, val domain: String, val overallScore: Float,
        val summary: String, val weakSpots: List<String>,
        val questions: List<QaPair>
    )

    private data class CardSeed(
        val question: String, val answer: String, val topic: String,
        val reviewedDaysAgo: Int?, val easeFactor: Double,
        val interval: Int, val reps: Int
    )

    private data class FaqSeed(val question: String, val answer: String, val source: String)
    private data class DayFaqSeed(val daysAgo: Int, val faqs: List<FaqSeed>)
}
