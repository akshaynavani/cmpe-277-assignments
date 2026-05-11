package com.example.android.interviewassistant.domain

import com.example.android.interviewassistant.data.local.entity.FlashcardEntity
import java.util.concurrent.TimeUnit

object SpacedRepetition {

    /**
     * SM-2 algorithm. Grades: 0=Again, 1=Wrong, 2=Hard, 3=Good, 4=Correct, 5=Perfect.
     * UI exposes 4 buttons: Again(0), Hard(2), Good(3), Easy(5).
     */
    fun review(card: FlashcardEntity, grade: Int): FlashcardEntity {
        val newEaseFactor = maxOf(
            1.3,
            card.easeFactor + 0.1 - (5 - grade) * (0.08 + (5 - grade) * 0.02)
        )

        val (newRepetitions, newInterval) = if (grade >= 3) {
            val reps = card.repetitions + 1
            val interval = when (reps) {
                1 -> 1
                2 -> 6
                else -> (card.interval * card.easeFactor).toInt().coerceAtLeast(1)
            }
            Pair(reps, interval)
        } else {
            Pair(0, 1)
        }

        val now = System.currentTimeMillis()
        val nextReviewMs = now + TimeUnit.DAYS.toMillis(newInterval.toLong())

        return card.copy(
            easeFactor = newEaseFactor,
            repetitions = newRepetitions,
            interval = newInterval,
            nextReview = nextReviewMs,
            lastReviewed = now
        )
    }

    fun gradeLabel(grade: Int): String = when (grade) {
        0 -> "Again"
        2 -> "Hard"
        3 -> "Good"
        5 -> "Easy"
        else -> "Grade $grade"
    }
}
