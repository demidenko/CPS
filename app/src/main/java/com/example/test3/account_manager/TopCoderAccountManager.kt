package com.example.test3.account_manager

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.example.test3.*
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import com.squareup.moshi.JsonReader

class TopCoderAccountManager(activity: AppCompatActivity): AccountManager(activity) {

    data class TopCoderUserInfo(
        override var status: STATUS,
        var handle: String,
        var rating_algorithm: Int = NOT_RATED,
        var rating_marathon: Int = NOT_RATED
    ) : UserInfo(){
        override val userID: String
            get() = handle

        override fun makeInfoOKString(): String {
            return if(rating_algorithm == NOT_RATED) "$handle [not rated]" else "$handle $rating_algorithm"
        }
    }

    override val PREFERENCES_FILE_NAME: String
        get() = preferences_file_name

    companion object{
        const val preferences_file_name = "topcoder"
        const val preferences_handle = "handle"
        const val preferences_rating_algorithm = "rating_algorithm"
        const val preferences_rating_marathon = "rating_marathon"

        var __cachedInfo: TopCoderUserInfo? = null

        val NAMES = JsonReader.Options.of(
            "handle",
            "ratingSummary",
            "error"
        )
    }



    override suspend fun downloadInfo(data: String): UserInfo {
        val handle = data
        return try{
            val res = TopCoderUserInfo(STATUS.FAILED, handle)
            with(JsonReaderFromURL("https://api.topcoder.com/v2/users/$handle") ?: return res) {
                readObject {
                    when(selectName(NAMES)){
                        0 -> res.handle = nextString()
                        1 -> readArray{
                            var name: String? = null
                            var rating: Int? = null
                            readObject {
                                while(hasNext()){
                                    when(nextName()){
                                        "name" -> name = nextString()
                                        "rating" -> rating = nextInt()
                                        else -> skipValue()
                                    }
                                }
                            }
                            when (name) {
                                "Algorithm" -> res.rating_algorithm = rating!!
                                "Marathon Match" -> res.rating_marathon = rating!!
                            }
                        }
                        2 -> {
                            //error
                            if((readObjectFields("name")[0] as String) == "Not Found") return@with res.apply { status = STATUS.NOT_FOUND }
                            return@with res
                        }
                        else -> skipNameAndValue()
                    }
                }
                res.apply { status = STATUS.OK }
            }
        } catch (e: JsonEncodingException){
            TopCoderUserInfo(STATUS.FAILED, handle)
        } catch (e: JsonDataException){
            TopCoderUserInfo(STATUS.FAILED, handle)
        }
    }

    override var cachedInfo: UserInfo?
        get() = __cachedInfo
        set(value) { __cachedInfo = value as TopCoderUserInfo }

    override fun readInfo(): TopCoderUserInfo = with(prefs){
        TopCoderUserInfo(
            STATUS.valueOf(getString(preferences_status, null) ?: STATUS.FAILED.name),
            handle = getString(preferences_handle, null) ?: "",
            rating_algorithm = getInt(preferences_rating_algorithm, NOT_RATED),
            rating_marathon = getInt(preferences_rating_marathon, NOT_RATED)
        )
    }

    override fun writeInfo(info: UserInfo) = with(prefs.edit()){
        putString(preferences_status, info.status.name)
        info as TopCoderUserInfo
        putString(preferences_handle, info.handle)
        putInt(preferences_rating_algorithm, info.rating_algorithm)
        putInt(preferences_rating_marathon, info.rating_marathon)
        commit()
    }

    override fun getColor(info: UserInfo): Int? = with(info as TopCoderUserInfo){
        if(status != STATUS.OK || rating_algorithm == NOT_RATED) return null
        return when{
            rating_algorithm < 900 -> 0xFF999999 //gray
            rating_algorithm < 1200 -> 0xFF00A900 //green
            rating_algorithm < 1500 -> 0xFF6666FE //blue
            rating_algorithm < 2200 -> 0xFFDDCC00 //yellow
            else -> 0xFFEE0000 //red
        }.toInt()
    }

}