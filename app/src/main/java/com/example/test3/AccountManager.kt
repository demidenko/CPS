package com.example.test3

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.InputStreamReader
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.Charset
import javax.net.ssl.HttpsURLConnection


abstract class AccountManager(val activity: AppCompatActivity) {
    abstract val preferences_file_name: String

    abstract suspend fun loadInfo(data: String): UserInfo?
    open suspend fun loadSuggestions(str: String): List<Pair<String,String>>? {
        return null
    }

    abstract fun getSavedInfo(): UserInfo
    abstract fun saveInfo(info: UserInfo): Boolean

    abstract fun getColor(info: UserInfo): Int?

}

abstract class UserInfo{
    abstract var usedID: String
    abstract fun makeInfoString(): String
}



//------------------CODEFORCES---------------------

class CodeforcesAccountManager(activity: AppCompatActivity): AccountManager(activity) {

    data class CodeforcesUserInfo(
        var handle: String,
        var rating: Int
    ): UserInfo() {
        override var usedID: String
            get() = handle
            set(value) {handle = value}

        override fun makeInfoString(): String {
            return when(rating){
                NOT_FOUND -> "Not found: $handle"
                NOT_RATED -> "$handle [not rated]"
                else -> "$handle $rating"
            }
        }
    }

    override val preferences_file_name = "codeforces"
    companion object{
        const val preferences_handle = "handle"
        const val preferences_rating = "rating"

        const val NOT_RATED = Int.MIN_VALUE
        const val NOT_FOUND = Int.MAX_VALUE
    }

    override suspend fun loadInfo(data: String): CodeforcesUserInfo? {
        val handle = data
        val s = readURLData("https://codeforces.com/api/user.info?handles=$handle") ?: return null
        var i = s.indexOf("\"handle\"")
        if(i==-1) return CodeforcesUserInfo(data, NOT_FOUND)
        val res = CodeforcesUserInfo(s.substring(i+10, s.indexOf('"',i+10)), NOT_RATED)
        i = s.indexOf("\"rating\"")
        if(i!=-1) res.rating = s.substring(i+9, s.indexOf(',',i+9)).toInt()
        return res
    }

    override fun getSavedInfo(): CodeforcesUserInfo = with(activity.getSharedPreferences(preferences_file_name, Context.MODE_PRIVATE)){
        return CodeforcesUserInfo(
            getString(preferences_handle, "") ?: "",
            getInt(preferences_rating, NOT_FOUND)
        )
    }

    override fun saveInfo(info: UserInfo) = with(activity.getSharedPreferences(preferences_file_name, Context.MODE_PRIVATE).edit()){
        info as CodeforcesUserInfo
        putString(preferences_handle, info.handle)
        putInt(preferences_rating, info.rating)
        commit()
    }

    override fun getColor(info: UserInfo): Int? = with(info as CodeforcesUserInfo){
        if(rating == NOT_FOUND || rating == NOT_RATED) return null
        return when{
            rating < 1200 -> 0xFF808080 //gray
            rating < 1400 -> 0xFF008000 //green
            rating < 1600 -> 0xFF03A89E //cyan
            rating < 1900 -> 0xFF0000FF //blue
            rating < 2100 -> 0xFFAA00AA //violet
            rating < 2400 -> 0xFFFF8C00 //orange
            else -> 0xFFFF0000 //red
        }.toInt()
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
        var handle: String,
        var rating: Int
    ): UserInfo(){
        override var usedID: String
            get() = handle
            set(value) {handle = value}

        override fun makeInfoString(): String {
            return when(rating){
                NOT_FOUND -> "Not found: $handle"
                NOT_RATED -> "$handle [not rated]"
                else -> "$handle $rating"
            }
        }
    }

    override val preferences_file_name = "atcoder"
    companion object{
        const val preferences_handle = "handle"
        const val preferences_rating = "rating"

        const val NOT_RATED = Int.MIN_VALUE
        const val NOT_FOUND = Int.MAX_VALUE
    }

    override suspend fun loadInfo(data: String): UserInfo? {
        val handle = data
        val s = readURLData("https://atcoder.jp/users/$handle") ?: return null
        val res = AtCoderUserInfo(handle, NOT_FOUND)
        var i = s.lastIndexOf("class=\"username\"")
        if(i==-1) return res
        i = s.indexOf("</span", i)
        res.handle = s.substring(s.lastIndexOf('>',i)+1, i)
        i = s.indexOf("<th class=\"no-break\">Rating</th>")
        if(i!=-1){
            i = s.indexOf("</span", i)
            res.rating = s.substring(s.lastIndexOf('>',i)+1, i).toInt()
        }else res.rating = NOT_RATED
        return res
    }

    override fun getSavedInfo(): AtCoderUserInfo = with(activity.getSharedPreferences(preferences_file_name, Context.MODE_PRIVATE)){
        return AtCoderUserInfo(
            getString(preferences_handle, "") ?: "",
            getInt(preferences_rating, NOT_FOUND)
        )
    }

    override fun saveInfo(info: UserInfo) = with(activity.getSharedPreferences(preferences_file_name, Context.MODE_PRIVATE).edit()){
        info as AtCoderUserInfo
        putString(preferences_handle, info.handle)
        putInt(preferences_rating, info.rating)
        commit()
    }

    override fun getColor(info: UserInfo): Int?  = with(info as AtCoderUserInfo){
        if(rating == NOT_FOUND || rating == NOT_RATED) return null
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
        var handle: String,
        var rating_algorithm: Int
    ) : UserInfo(){
        override var usedID: String
            get() = handle
            set(value) { handle = value }

        override fun makeInfoString(): String {
            return when(rating_algorithm){
                NOT_FOUND -> "Not found: $handle"
                NOT_RATED -> "$handle [not rated]"
                else -> "$handle $rating_algorithm"
            }
        }
    }

    override val preferences_file_name = "topcoder"
    companion object{
        const val preferences_handle = "handle"
        const val preferences_rating_algorithm = "rating_algorithm"

        const val NOT_RATED = Int.MIN_VALUE
        const val NOT_FOUND = Int.MAX_VALUE
    }

    override suspend fun loadInfo(data: String): UserInfo? {
        val handle = data
        val s = readURLData("https://api.topcoder.com/v2/users/$handle") ?: return null
        if(s.contains("\"name\": \"Not Found\"")) return TopCoderUserInfo(handle, NOT_FOUND)
        var i = s.indexOf("\"handle\"")
        i = s.indexOf("\"", i+10)
        val res = TopCoderUserInfo(s.substring(i+1, s.indexOf('"',i+1)), NOT_RATED)
        i = s.indexOf("\"name\": \"Algorithm\"")
        if(i!=-1){
            i = s.indexOf("\"rating\":", i+1)
            res.rating_algorithm = s.substring(s.indexOf(": ",i)+2, s.indexOf(",",i)).toInt()
        }
        return res
    }

    override fun getSavedInfo(): TopCoderUserInfo = with(activity.getSharedPreferences(preferences_file_name, Context.MODE_PRIVATE)){
        return TopCoderUserInfo(
            getString(preferences_handle, "") ?: "",
            getInt(preferences_rating_algorithm, NOT_FOUND)
        )
    }

    override fun saveInfo(info: UserInfo) = with(activity.getSharedPreferences(preferences_file_name, Context.MODE_PRIVATE).edit()){
        info as TopCoderUserInfo
        putString(preferences_handle, info.handle)
        putInt(preferences_rating_algorithm, info.rating_algorithm)
        commit()
    }

    override fun getColor(info: UserInfo): Int? = with(info as TopCoderUserInfo){
        if(rating_algorithm == NOT_FOUND || rating_algorithm == NOT_RATED) return null
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
        var id: String,
        var userName: String,
        var rating: Int = 0,
        var solvedTasks: Int = 0
    ): UserInfo() {
        override var usedID: String
            get() = id
            set(value) {id = value}

        override fun makeInfoString(): String {
            if(userName.isEmpty()) return "[Not found]"
            return "$userName [$solvedTasks / $rating]"
        }
    }

    override val preferences_file_name = "acmp"
    companion object{
        const val preferences_userid = "userid"
        const val preferences_username = "username"
        const val preferences_rating = "rating"
        const val preferences_count_of_solved_tasks = "count_of_solved_tasks"
    }

    override suspend fun loadInfo(data: String): ACMPUserInfo? {
        val s  = readURLData("https://acmp.ru/index.asp?main=user&id=$data", Charset.forName("windows-1251")) ?: return null
        val res = ACMPUserInfo(data, "")
        if(!s.contains("index.asp?main=status&id_mem=$data")) return res
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
        return res
    }

    override fun getSavedInfo(): ACMPUserInfo = with(activity.getSharedPreferences(preferences_file_name, Context.MODE_PRIVATE)){
        return ACMPUserInfo(
            getString(preferences_userid, "") ?: "",
            getString(preferences_username, "") ?: "",
            getInt(preferences_rating,0),
            getInt(preferences_count_of_solved_tasks, 0)
        )
    }

    override fun saveInfo(info: UserInfo) = with(activity.getSharedPreferences(preferences_file_name, Context.MODE_PRIVATE).edit()){
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

suspend fun readURLData(address: String, charset: Charset = Charsets.UTF_8): String? = withContext(Dispatchers.IO){
    val c = URL(address).openConnection() as HttpsURLConnection
    c.connectTimeout = 30000
    c.readTimeout = 30000
    return@withContext try{
        when(c.responseCode) {
            HttpsURLConnection.HTTP_OK -> c.inputStream
            else -> c.errorStream
        }.reader(charset).readText()
    }catch (e : SocketTimeoutException){
        null
    }
}
