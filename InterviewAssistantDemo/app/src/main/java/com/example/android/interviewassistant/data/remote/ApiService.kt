package com.example.android.interviewassistant.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {

    @POST("context/build")
    suspend fun buildContext(@Body req: BuildContextRequest): BuildContextResponse

    @POST("session/begin")
    suspend fun beginSession(@Body req: BeginSessionRequest): BeginSessionResponse

    @POST("session/evaluate")
    suspend fun evaluateAnswer(@Body req: EvaluateAnswerRequest): EvaluateAnswerResponse

    @POST("session/wrap")
    suspend fun wrapSession(@Body req: WrapSessionRequest): WrapSessionResponse

    @POST("study/ask")
    suspend fun askStudy(@Body req: AskStudyRequest): AskStudyResponse

    @POST("study/generate")
    suspend fun generateCards(@Body req: GenerateCardsRequest): GenerateCardsResponse

    @POST("study/daily-faq")
    suspend fun fetchDailyFaq(@Body req: DailyFaqRequest): DailyFaqResponse

    @GET("health")
    suspend fun health(): HealthResponse
}
