package com.example.test3

import com.squareup.moshi.JsonEncodingException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

object CodeforcesUtils {

    suspend fun getBlogCreationTimeMillis(blogID: String): Long = withContext(Dispatchers.IO){
        with(JsonReaderFromURL("https://codeforces.com/api/blogEntry.view?blogEntryId=$blogID&locale=ru") ?: return@withContext 0L ){
            try {
                beginObject()
                if (nextString("status") != "OK") {
                    if (nextString("comment") == "Call limit exceeded") {
                        delay(500)
                        return@withContext getBlogCreationTimeMillis(blogID)
                    }
                    return@withContext 0L
                }
                nextName()
                return@withContext (readObjectFields("creationTimeSeconds")[0] as Double).toInt() * 1000L
            } catch (e: JsonEncodingException){
                return@withContext 0L
            }
        }
    }

}