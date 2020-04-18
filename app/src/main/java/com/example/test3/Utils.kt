package com.example.test3

import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Okio
import java.io.InputStream
import java.net.SocketTimeoutException
import java.net.URL
import java.nio.charset.Charset
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*

suspend fun createConnectionStream(address: String): InputStream? = withContext(Dispatchers.IO){
    val c = (URL(address).openConnection() as HttpsURLConnection).apply {
        connectTimeout = 30000
        readTimeout = 30000
    }
    return@withContext try {
        when (c.responseCode) {
            HttpsURLConnection.HTTP_OK -> c.inputStream
            else -> c.errorStream
        }
    }catch (e : SocketTimeoutException){
        null
    }catch (e: SSLException){
        ignoreBadSSL()
        createConnectionStream(address)
    }
}

suspend fun readURLData(address: String, charset: Charset = Charsets.UTF_8): String? = withContext(Dispatchers.IO){
    val c = createConnectionStream(address)
    c?.reader(charset)?.readText()
}

fun ignoreBadSSL(){
    val sc: SSLContext = SSLContext.getInstance("SSL")
    sc.init(null, arrayOf(object : X509TrustManager {
        override fun getAcceptedIssuers(): Array<X509Certificate?>? = null
        override fun checkClientTrusted(certs: Array<X509Certificate?>?, authType: String?) { }
        override fun checkServerTrusted(certs: Array<X509Certificate?>?, authType: String?) { }
    }), SecureRandom())
    HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)
    HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }
}


///JsonReader help
suspend fun JsonReaderFromURL(address: String): JsonReader?{
    return JsonReader.of(Okio.buffer(Okio.source(createConnectionStream(address)?: return null)))
}

inline fun JsonReader.readObject(body: () -> Unit) {
    beginObject()
    while (hasNext()) body()
    endObject()
}

fun JsonReader.readObjectFields(vararg strings: String): Array<Any?> {
    beginObject()
    val names = JsonReader.Options.of(*strings)
    val res = Array<Any?>(strings.size){ null }
    while(hasNext()){
        val i = selectName(names)
        if(0<=i && i<res.size) res[i] = readJsonValue()
        else skipValue()
    }
    endObject()
    return res
}


inline fun JsonReader.readArray(body: () -> Unit) {
    beginArray()
    while (hasNext()) body()
    endArray()
}

inline fun JsonReader.readArrayOfObjects(body: () -> Unit) {
    beginArray()
    while (hasNext()){
        beginObject()
        body()
        endObject()
    }
    endArray()
}

fun JsonReader.skipNameAndValue() {
    skipName()
    skipValue()
}

fun JsonReader.nextString(name: String): String {
    val s = nextName()
    if(s != name) throw JsonDataException("name $name expected but $s found")
    return nextString()
}

