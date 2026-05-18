package com.example.acadex.data.repository

import com.example.acadex.BuildConfig
import com.example.acadex.data.model.GutendexBook
import com.example.acadex.data.model.GutendexResponse
import com.example.acadex.data.result.RepoResult
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface GutendexService {
    @GET("books")
    suspend fun getBooks(
        @Query("search") search: String?,
        @Query("topic") topic: String?,
        @Query("page") page: Int?
    ): GutendexResponse

    @GET("books/{id}")
    suspend fun getBook(@Path("id") id: Int): GutendexBook
}

object GutendexRepository {

    private val api: GutendexService

    init {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://gutendex.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(GutendexService::class.java)
    }

    // Cache structure
    private var cachedResponse: GutendexResponse? = null
    private var cachedQuery: String? = null
    private var cachedTopic: String? = null
    private var cachedPage: Int? = null
    private var cacheTimestamp: Long = 0L

    suspend fun fetchBooks(
        search: String?,
        topic: String?,
        page: Int
    ): RepoResult<GutendexResponse> {
        val now = System.currentTimeMillis()
        val queryStr = search?.trim() ?: ""
        val topicStr = topic?.trim() ?: ""
        
        // Cache hit condition: same search, topic, page and within 5 minutes
        if (cachedResponse != null &&
            cachedQuery == queryStr &&
            cachedTopic == topicStr &&
            cachedPage == page &&
            (now - cacheTimestamp) < 5 * 60 * 1000
        ) {
            return RepoResult.Success(cachedResponse!!)
        }

        return try {
            val response = api.getBooks(
                search = if (queryStr.isEmpty()) null else queryStr,
                topic = if (topicStr.isEmpty()) null else topicStr,
                page = page
            )
            // Cache the result
            cachedResponse = response
            cachedQuery = queryStr
            cachedTopic = topicStr
            cachedPage = page
            cacheTimestamp = now
            
            RepoResult.Success(response)
        } catch (e: Exception) {
            RepoResult.Error("Failed to load books: ${e.localizedMessage}", e)
        }
    }

    suspend fun fetchBookDetails(id: Int): RepoResult<GutendexBook> {
        return try {
            val book = api.getBook(id)
            RepoResult.Success(book)
        } catch (e: Exception) {
            RepoResult.Error("Failed to fetch book details: ${e.localizedMessage}", e)
        }
    }
}
