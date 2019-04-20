// Copyright (c) 2019 . Wilberforce Uwadiegwu. All Rights Reserved.

package com.jadebyte.jadeplayer.main.common.network

import android.content.SharedPreferences
import android.text.TextUtils
import androidx.core.content.edit
import com.jadebyte.jadeplayer.common.App
import com.jadebyte.jadeplayer.main.common.data.CloudKeys
import com.jadebyte.jadeplayer.main.common.utils.ConvertUtils
import dagger.Lazy
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

/**
 * Created by Wilberforce on 19/04/2019 at 22:25.
 * Original source: https://gist.github.com/alex-shpak/da1e65f52dc916716930
 */
class HttpInterceptor : Interceptor {

    @Inject
    lateinit var okHttpClient: Lazy<OkHttpClient>
    @Inject
    lateinit var preferences: SharedPreferences
    @Inject
    lateinit var cloudKeys: CloudKeys

    init {
        App.appComponent.inject(this)
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()

        if (!request.url().uri().authority.contains("spotify")) {
            return chain.proceed(request)
        } else if (request.method() == "POST") {
            return chain.proceed(request)
        }


        if (TextUtils.isEmpty(cloudKeys.spotifySecret)) {
            Timber.w("intercept: Spotify secret is empty")
            return chain.proceed(request)
        }

        //Build new request
        val builder = request.newBuilder()
        // builder.header("Accept", "application/json"); //if necessary, say to consume JSON

        val token = getAccessToken() //save token of this request for future
        setAuthHeader(builder, token) //write current token to request

        request = builder.build() //overwrite old request
        val response = chain.proceed(request) //perform request, here original request will be executed

        if (response.code() == 401) { //if unauthorized
            synchronized(okHttpClient) {
                //perform all 401 in sync blocks, to avoid multiply token updates
                val code = refreshToken() / 100 //refresh token
                if (code != 2) { //if refresh token failed for some reason
                    if (code == 4) {//only if response is 400, 500 might mean that token was not updated
                        logout() //go to login screen
                    }
                    return response //if token refresh failed - show error to user
                }

                if (getAccessToken() != null) { //retry requires new auth token,
                    setAuthHeader(builder, getAccessToken()) //set auth token to updated
                    request = builder.build()
                    response.close()
                    return chain.proceed(request) //repeat request with new token
                }
            }
        }

        return response
    }

    private fun setAuthHeader(builder: Request.Builder, token: String?) {
        if (token != null)
        //Add Auth token to each request if authorized
            builder.header("Authorization", String.format("Bearer %s", token))
    }

    private fun refreshToken(): Int {
        //Refresh token, synchronously, save it, and return result code
        //you might use retrofit here
        val base64 = ConvertUtils.stringToBase64(cloudKeys.spotifySecret!!)

        val mediaType = MediaType.parse("application/x-www-form-urlencoded")
        val body = RequestBody.create(mediaType, "grant_type=client_credentials")
        val request = Request.Builder()
            .url("https://accounts.spotify.com/api/token")
            .post(body)
            .addHeader("content-type", "application/x-www-form-urlencoded")
            .addHeader("authorization", String.format("Basic %s", base64))
            .build()

        try {
            val response = okHttpClient.get().newCall(request).execute()
            val jsonObject = JSONObject(response.body()!!.string())
            Timber.i("refreshToken: $jsonObject")
            val accessToken = jsonObject.getString("access_token")
            preferences.edit {
                putString(spotifyToken, accessToken)
            }
            return response.code()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        return -1
    }

    private fun logout() {
        //logout your user
        throw RuntimeException("Authentication not successful")
    }

    private fun getAccessToken(): String? {
        return preferences.getString(spotifyToken, null)
    }
}


const val spotifyToken = "com.jadebyte.jadeplayer.main.common.network.spotifyToken"