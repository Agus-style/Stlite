package com.roblox.studiolite.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

// ─── Result wrapper ───────────────────────────────────────────────────────────

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val code: Int, val message: String) : ApiResult<Nothing>()
    data class NetworkError(val exception: Exception) : ApiResult<Nothing>()
}

// ─── Roblox Open Cloud API ────────────────────────────────────────────────────

class RobloxOpenCloudApi(private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("x-api-key", apiKey)
                .addHeader("Accept", "application/json")
                .build()
            chain.proceed(request)
        }
        .build()

    companion object {
        private const val BASE_URL = "https://apis.roblox.com"
    }

    // ── List universes (games) owned by user ──────────────────────────────────

    suspend fun listUniverses(): ApiResult<List<Universe>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/cloud/v2/universes")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext ApiResult.Error(response.code, body)
            }

            val json = JSONObject(body)
            val universes = mutableListOf<Universe>()
            val arr = json.optJSONArray("universes") ?: return@withContext ApiResult.Success(universes)

            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                universes.add(
                    Universe(
                        id = obj.optString("universeId"),
                        name = obj.optString("displayName"),
                        description = obj.optString("description"),
                        visibility = obj.optString("visibility")
                    )
                )
            }
            ApiResult.Success(universes)
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }

    // ── Get place info ────────────────────────────────────────────────────────

    suspend fun getPlaceInfo(universeId: String, placeId: String): ApiResult<PlaceInfo> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$BASE_URL/cloud/v2/universes/$universeId/places/$placeId")
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    return@withContext ApiResult.Error(response.code, body)
                }

                val json = JSONObject(body)
                ApiResult.Success(
                    PlaceInfo(
                        id = json.optString("placeId"),
                        universeId = universeId,
                        name = json.optString("displayName"),
                        description = json.optString("description"),
                        serverSize = json.optInt("serverSize"),
                    )
                )
            } catch (e: Exception) {
                ApiResult.NetworkError(e)
            }
        }

    // ── Upload / publish place (.rbxl file) ───────────────────────────────────

    suspend fun publishPlace(
        universeId: String,
        placeId: String,
        rbxlFile: File,
        versionType: String = "Published" // "Published" or "Saved"
    ): ApiResult<PublishResult> = withContext(Dispatchers.IO) {
        try {
            val mediaType = "application/octet-stream".toMediaType()
            val requestBody = rbxlFile.asRequestBody(mediaType)

            val request = Request.Builder()
                .url("$BASE_URL/universes/v1/$universeId/places/$placeId/versions?versionType=$versionType")
                .addHeader("Content-Type", "application/octet-stream")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext ApiResult.Error(response.code, body)
            }

            val json = JSONObject(body)
            ApiResult.Success(
                PublishResult(
                    versionNumber = json.optInt("versionNumber"),
                    universeId = universeId,
                    placeId = placeId
                )
            )
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }

    // ── Upload asset (Model, Image, Audio, etc.) ──────────────────────────────

    suspend fun uploadAsset(
        creatorType: String, // "User" or "Group"
        creatorId: String,
        assetType: String,   // "Model", "Decal", "Audio", etc.
        name: String,
        description: String,
        file: File,
    ): ApiResult<AssetUploadResult> = withContext(Dispatchers.IO) {
        try {
            val requestJson = JSONObject().apply {
                put("assetType", assetType)
                put("displayName", name)
                put("description", description)
                put("creationContext", JSONObject().apply {
                    put("creator", JSONObject().apply {
                        put("${creatorType.lowercase()}Id", creatorId)
                    })
                })
            }

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("request", requestJson.toString())
                .addFormDataPart(
                    "fileContent",
                    file.name,
                    file.asRequestBody("application/octet-stream".toMediaType())
                )
                .build()

            val request = Request.Builder()
                .url("$BASE_URL/assets/v1/assets")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext ApiResult.Error(response.code, body)
            }

            val json = JSONObject(body)
            // Response is an Operation - poll for completion
            val operationId = json.optString("path").substringAfterLast("/")
            ApiResult.Success(
                AssetUploadResult(
                    operationId = operationId,
                    done = json.optBoolean("done", false)
                )
            )
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }

    // ── Poll asset upload operation ───────────────────────────────────────────

    suspend fun getOperationStatus(operationPath: String): ApiResult<AssetOperationResult> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$BASE_URL/$operationPath")
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    return@withContext ApiResult.Error(response.code, body)
                }

                val json = JSONObject(body)
                val done = json.optBoolean("done", false)
                val response2 = json.optJSONObject("response")
                val assetId = response2?.optString("assetId") ?: ""

                ApiResult.Success(AssetOperationResult(done = done, assetId = assetId))
            } catch (e: Exception) {
                ApiResult.NetworkError(e)
            }
        }

    // ── Validate API key ──────────────────────────────────────────────────────

    suspend fun validateApiKey(): ApiResult<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/cloud/v2/universes")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (response.code == 401 || response.code == 403) {
                return@withContext ApiResult.Error(response.code, "Invalid API key")
            }
            ApiResult.Success(true)
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }
}

// ─── Data classes ─────────────────────────────────────────────────────────────

data class Universe(
    val id: String,
    val name: String,
    val description: String,
    val visibility: String,
)

data class PlaceInfo(
    val id: String,
    val universeId: String,
    val name: String,
    val description: String,
    val serverSize: Int,
)

data class PublishResult(
    val versionNumber: Int,
    val universeId: String,
    val placeId: String,
)

data class AssetUploadResult(
    val operationId: String,
    val done: Boolean,
)

data class AssetOperationResult(
    val done: Boolean,
    val assetId: String,
)
