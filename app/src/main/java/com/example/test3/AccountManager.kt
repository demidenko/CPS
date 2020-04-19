package com.example.test3

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import com.squareup.moshi.JsonReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Okio
import java.io.ByteArrayInputStream
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.Charset
import javax.net.ssl.HttpsURLConnection


abstract class AccountManager(val activity: AppCompatActivity) {
    abstract val PREFERENCES_FILE_NAME: String


    protected abstract suspend fun downloadInfo(data: String): UserInfo
    suspend fun loadInfo(data: String): UserInfo {
        return withContext(Dispatchers.IO){
            downloadInfo(data)
        }
    }

    open suspend fun loadSuggestions(str: String): List<Pair<String,String>>? {
        return null
    }

    protected abstract fun readInfo(): UserInfo
    protected abstract fun writeInfo(info: UserInfo): Boolean
    protected abstract var cachedInfo: UserInfo?

    var savedInfo: UserInfo
        get() = cachedInfo ?: readInfo().also { cachedInfo = it }
        set(info) {
            if(info == cachedInfo) return
            writeInfo(info)
            cachedInfo = info
            println("${PREFERENCES_FILE_NAME} rewrited to ${info.makeInfoString()}")
        }

    abstract fun getColor(info: UserInfo): Int?
}

enum class STATUS{
    OK,
    NOT_FOUND,
    FAILED
}
const val NOT_RATED = Int.MIN_VALUE
const val preferences_status = "preferences_status"

abstract class UserInfo{
    abstract val userID: String
    abstract val status: STATUS

    protected abstract fun makeInfoOKString(): String
    fun makeInfoString(): String {
        return when(status){
            STATUS.FAILED -> "Error on load $userID"
            STATUS.NOT_FOUND -> "Not found: $userID"
            else -> makeInfoOKString()
        }
    }
}



//------------------CODEFORCES---------------------

class CodeforcesAccountManager(activity: AppCompatActivity): AccountManager(activity) {

    data class CodeforcesUserInfo(
        override var status: STATUS,
        var handle: String,
        var rating: Int = NOT_RATED
    ): UserInfo() {
        override val userID: String
            get() = handle

        override fun makeInfoOKString(): String {
            return if(rating == NOT_RATED) "$handle [not rated]" else "$handle $rating"
        }
    }

    override val PREFERENCES_FILE_NAME = "codeforces"
    companion object{
        const val preferences_handle = "handle"
        const val preferences_rating = "rating"

        var __cachedInfo: CodeforcesUserInfo? = null

        val NAMES = JsonReader.Options.of("handle", "rating")

    }

    enum class HandleColor(val rgb: Int){
        GRAY(0x808080),
        GREEN(0x008000),
        CYAN(0x03A89E),
        BLUE(0x0000FF),
        VIOLET(0xAA00AA),
        ORANGE(0xFF8C00),
        RED(0xFF0000);

        val argb = (rgb + 0xFF000000).toInt()

        companion object {
            fun getColorByRating(rating: Int): HandleColor {
                return when {
                    rating < 1200 -> GRAY
                    rating < 1400 -> GREEN
                    rating < 1600 -> CYAN
                    rating < 1900 -> BLUE
                    rating < 2100 -> VIOLET
                    rating < 2400 -> ORANGE
                    else -> RED
                }
            }

            fun getColorByTag(tag: String): HandleColor? {
                return when (tag) {
                    "user-gray" -> GRAY
                    "user-green" -> GREEN
                    "user-cyan" -> CYAN
                    "user-blue" -> BLUE
                    "user-violet" -> VIOLET
                    "user-orange" -> ORANGE
                    "user-red", "user-legendary" -> RED
                    else -> null
                }
            }
        }
    }

    override suspend fun downloadInfo(data: String): CodeforcesUserInfo {
        val handle = data
        return try {
            val res = CodeforcesUserInfo(STATUS.FAILED, handle)
            with(JsonReaderFromURL("https://codeforces.com/api/user.info?handles=$handle") ?: return res) {
                readObject {
                    if(nextString("status") == "FAILED") return res.apply { status = STATUS.NOT_FOUND }
                    nextName()
                    readArrayOfObjects {
                        while (hasNext()) {
                            when (selectName(NAMES)) {
                                0 -> res.handle = nextString()
                                1 -> res.rating = nextInt()
                                else -> skipNameAndValue()
                            }
                        }
                    }
                }
                res.apply { status = STATUS.OK }
            }
        }catch (e: JsonEncodingException){
            CodeforcesUserInfo(STATUS.FAILED, handle)
        }catch (e: JsonDataException){
            CodeforcesUserInfo(STATUS.FAILED, handle)
        }
    }

    override var cachedInfo: UserInfo?
        get() = __cachedInfo
        set(value) { __cachedInfo = value as CodeforcesUserInfo }

    override fun readInfo(): CodeforcesUserInfo = with(activity.getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE)){
        return CodeforcesUserInfo(
            STATUS.valueOf(getString(preferences_status, null) ?: STATUS.FAILED.name),
            getString(preferences_handle, "") ?: "",
            getInt(preferences_rating, NOT_RATED)
        )
    }

    override fun writeInfo(info: UserInfo) = with(activity.getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE).edit()){
        putString(preferences_status, info.status.name)
        info as CodeforcesUserInfo
        putString(preferences_handle, info.handle)
        putInt(preferences_rating, info.rating)
        commit()
    }

    override fun getColor(info: UserInfo): Int? = with(info as CodeforcesUserInfo){
        if(status != STATUS.OK || rating == NOT_RATED) return null
        return HandleColor.getColorByRating(info.rating).argb
    }

    override suspend fun loadSuggestions(str: String): List<Pair<String, String>>? {
        val s = readURLData("https://codeforces.com/data/handles?q=$str") ?: return null
        val res = ArrayList<Pair<String, String>>()
        s.split('\n').filter { !it.contains('=') }.forEach {
            val i = it.indexOf('|')
            if (i != -1) {
                val handle = it.substring(i + 1)
                res += Pair(handle, handle)
            }
        }
        return res
    }
}

//----------AtCoder-----------

class AtCoderAccountManager(activity: AppCompatActivity): AccountManager(activity){

    data class AtCoderUserInfo(
        override var status: STATUS,
        var handle: String,
        var rating: Int = NOT_RATED
    ): UserInfo(){
        override val userID: String
            get() = handle

        override fun makeInfoOKString(): String {
            return if(rating == NOT_RATED) "$handle [not rated]" else "$handle $rating"
        }
    }

    override val PREFERENCES_FILE_NAME = "atcoder"
    companion object{
        const val preferences_handle = "handle"
        const val preferences_rating = "rating"

        var __cachedInfo: AtCoderUserInfo? = null
    }


    override suspend fun downloadInfo(data: String): UserInfo {
        val handle = data
        val res = AtCoderUserInfo(STATUS.FAILED, handle)
        val s = readURLData("https://atcoder.jp/users/$handle") ?: return res
        var i = s.lastIndexOf("class=\"username\"")
        if(i==-1) return res.apply { status = STATUS.NOT_FOUND }
        i = s.indexOf("</span", i)
        res.handle = s.substring(s.lastIndexOf('>',i)+1, i)
        i = s.indexOf("<th class=\"no-break\">Rating</th>")
        if(i!=-1){
            i = s.indexOf("</span", i)
            res.rating = s.substring(s.lastIndexOf('>',i)+1, i).toInt()
        }else res.rating = NOT_RATED
        res.status = STATUS.OK
        return res
    }

    override var cachedInfo: UserInfo?
        get() = __cachedInfo
        set(value) { __cachedInfo = value as AtCoderUserInfo }

    override fun readInfo(): AtCoderUserInfo = with(activity.getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE)){
        return AtCoderUserInfo(
            STATUS.valueOf(getString(preferences_status, null) ?: STATUS.FAILED.name),
            getString(preferences_handle, "") ?: "",
            getInt(preferences_rating, NOT_RATED)
        )
    }

    override fun writeInfo(info: UserInfo) = with(activity.getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE).edit()){
        putString(preferences_status, info.status.name)
        info as AtCoderUserInfo
        putString(preferences_handle, info.handle)
        putInt(preferences_rating, info.rating)
        commit()
    }

    override fun getColor(info: UserInfo): Int?  = with(info as AtCoderUserInfo){
        if(status != STATUS.OK || rating == NOT_RATED) return null
        return when{
            rating < 400 -> 0xFF808080 //gray
            rating < 800 -> 0xFF804000 //brown
            rating < 1200 -> 0xFF008000 //green
            rating < 1600 -> 0xFF00C0C0 //cyan
            rating < 2000 -> 0xFF0000FF //blue
            rating < 2400 -> 0xFFC0C000 //yellow
            rating < 2800 -> 0xFFFF8000 //orange
            else -> 0xFFFF0000 //red
        }.toInt()
    }

    override suspend fun loadSuggestions(str: String): List<Pair<String, String>>? {
        val s = readURLData("https://atcoder.jp/ranking/all?f.UserScreenName=*$str*") ?: return null
        val res = ArrayList<Pair<String, String>>()
        var i = s.indexOf("<div class=\"table-responsive\">")
        while(true){
            i = s.indexOf("<td class=\"no-break\">", i+1)
            if(i==-1) break
            var j = s.indexOf("</span></a>", i)
            val handle = s.substring(s.lastIndexOf('>', j-1)+1, j)
            j = s.indexOf("<td", j+1)
            j = s.indexOf("<td", j+1)
            val rating = s.substring(s.indexOf("<b>",j)+3, s.indexOf("</b>",j))
            res += Pair("$handle $rating", handle)
        }
        return res
    }
}

//-------TopCoder---------

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

    override val PREFERENCES_FILE_NAME = "topcoder"
    companion object{
        const val preferences_handle = "handle"
        const val preferences_rating_algorithm = "rating_algorithm"
        const val preferences_rating_marathon = "rating_marathon"

        var __cachedInfo: TopCoderUserInfo? = null

        val NAMES = JsonReader.Options.of("handle", "ratingSummary", "error")
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

    override fun readInfo(): TopCoderUserInfo = with(activity.getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE)){
        return TopCoderUserInfo(
            STATUS.valueOf(getString(preferences_status, null) ?: STATUS.FAILED.name),
            getString(preferences_handle, "") ?: "",
            getInt(preferences_rating_algorithm, NOT_RATED),
            getInt(preferences_rating_marathon, NOT_RATED)
        )
    }

    override fun writeInfo(info: UserInfo) = with(activity.getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE).edit()){
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

//------ACMP-------

class ACMPAccountManager(activity: AppCompatActivity): AccountManager(activity) {
    data class ACMPUserInfo(
        override var status: STATUS,
        var id: String,
        var userName: String = "",
        var rating: Int = 0,
        var solvedTasks: Int = 0
    ): UserInfo() {
        override val userID: String
            get() = id

        override fun makeInfoOKString(): String {
            return "$userName [$solvedTasks / $rating]"
        }
    }



    override val PREFERENCES_FILE_NAME = "acmp"
    companion object{
        const val preferences_userid = "userid"
        const val preferences_username = "username"
        const val preferences_rating = "rating"
        const val preferences_count_of_solved_tasks = "count_of_solved_tasks"

        var __cachedInfo: ACMPUserInfo? = null
    }

    override suspend fun downloadInfo(data: String): ACMPUserInfo {
        val res = ACMPUserInfo(STATUS.FAILED, data)
        val s  = readURLData("https://acmp.ru/index.asp?main=user&id=$data", Charset.forName("windows-1251")) ?: return res
        if(!s.contains("index.asp?main=status&id_mem=$data")) return res.apply { status = STATUS.NOT_FOUND }
        var i = s.indexOf("<title>")
        if(i!=-1){
            res.userName = s.substring(s.indexOf('>',i)+1, s.indexOf("</title>"))
        }
        i = s.indexOf("Решенные задачи")
        if(i!=-1){
            i = s.indexOf('(', i)
            res.solvedTasks = s.substring(i+1, s.indexOf(')',i)).toInt()
        }
        i = s.indexOf("<b class=btext>Рейтинг:")
        if(i!=-1){
            i = s.indexOf(':', i)
            res.rating = s.substring(i+2, s.indexOf('/', i)-1).toInt()
        }
        res.status = STATUS.OK
        return res
    }

    override var cachedInfo: UserInfo?
        get() = __cachedInfo
        set(value) { __cachedInfo = value as ACMPUserInfo }

    override fun readInfo(): ACMPUserInfo = with(activity.getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE)){
        return ACMPUserInfo(
            STATUS.valueOf(getString(preferences_status,null) ?: STATUS.FAILED.name),
            getString(preferences_userid, "") ?: "",
            getString(preferences_username, "") ?: "",
            getInt(preferences_rating,0),
            getInt(preferences_count_of_solved_tasks, 0)
        )
    }

    override fun writeInfo(info: UserInfo) = with(activity.getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE).edit()){
        putString(preferences_status, info.status.name)
        info as ACMPUserInfo
        putString(preferences_userid, info.id)
        putString(preferences_username, info.userName)
        putInt(preferences_rating, info.rating)
        putInt(preferences_count_of_solved_tasks, info.solvedTasks)
        commit()
    }

    override fun getColor(info: UserInfo): Int? {
        return null
    }

    override suspend fun loadSuggestions(str: String): List<Pair<String, String>>? {
        if(str.toIntOrNull()!=null) return null
        val s = readURLData("https://acmp.ru/index.asp?main=rating&str="+URLEncoder.encode(str, "windows-1251"), Charset.forName("windows-1251")) ?: return null
        val res = ArrayList<Pair<String,String>>()
        var k = s.indexOf("<table cellspacing=1 cellpadding=2 align=center class=main>")
        while(true){
            k = s.indexOf("<tr class=white>", k+1)
            if(k==-1) break
            var i = s.indexOf("<td>", k+1)
            i = s.indexOf('>',i+4)
            val userid = s.substring(s.lastIndexOf("id=",i)+3, i)
            val username = s.substring(i+1, s.indexOf("</a",i))
            res += Pair("$username $userid", userid)
        }
        return res
    }
}


//-------ProjectEuler---------
//TODO
