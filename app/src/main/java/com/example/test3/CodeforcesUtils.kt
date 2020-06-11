package com.example.test3

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class CodeforcesUtils {
    companion object {
        suspend fun getBlogCreationTimeMillis(blogID: String): Long = withContext(Dispatchers.IO){
            with(JsonReaderFromURL("https://codeforces.com/api/blogEntry.view?blogEntryId=$blogID&locale=ru") ?: return@withContext 0L ){
                beginObject()
                if(nextString("status") != "OK"){
                    if(nextString("comment") == "Call limit exceeded"){
                        delay(500)
                        return@withContext getBlogCreationTimeMillis(blogID)
                    }
                    return@withContext 0L
                }
                nextName()
                return@withContext (readObjectFields("creationTimeSeconds")[0] as Double).toInt() * 1000L
            }
        }
    }
}