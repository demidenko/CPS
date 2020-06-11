package com.example.test3

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class CodeforcesUtils {
    companion object {
        suspend fun getBlogCreationTimeSeconds(blogID: String): Int = withContext(Dispatchers.IO){
            with(JsonReaderFromURL("https://codeforces.com/api/blogEntry.view?blogEntryId=$blogID&locale=ru") ?: return@withContext 0 ){
                beginObject()
                if(nextString("status") != "OK"){
                    if(nextString("comment") == "Call limit exceeded"){
                        delay(500)
                        return@withContext getBlogCreationTimeSeconds(blogID)
                    }
                    return@withContext 0
                }
                nextName()
                return@withContext (readObjectFields("creationTimeSeconds")[0] as Double).toInt()
            }
        }
    }
}