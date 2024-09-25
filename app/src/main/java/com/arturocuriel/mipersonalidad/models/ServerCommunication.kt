package com.arturocuriel.mipersonalidad.models

import android.content.Context
import okhttp3.Call
import okhttp3.Callback
import okhttp3.CertificatePinner
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager
import android.util.Base64
import android.util.Log

class ServerCommunication(val serverDomain : String,
                          val url : String,
                          val certificatePins : Array<String>,
                          val signingKey : String,
                          val jsonPayload: String) {

    private fun secureCommunicationClient() : OkHttpClient{
        // Certificate pinning
        val certificatePinner = CertificatePinner.Builder()
            .add(serverDomain, pins = certificatePins)
            .build()

        val okHttpClient = OkHttpClient.Builder()
            .certificatePinner(certificatePinner)
            .build()

        return okHttpClient
    }

    private fun nonSecureCommunicationClient() : OkHttpClient{
        val trustManager = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(trustManager), SecureRandom())

        val okHttpClient = OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .hostnameVerifier { _, _ -> true }
            .build()

        return okHttpClient
    }

    private fun generateHmacSHA256Signature(data: String, secret: String): String {
        val secretKeySpec = SecretKeySpec(secret.toByteArray(), "HmacSHA256")
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(secretKeySpec)
        val hmac = mac.doFinal(data.toByteArray())
        return Base64.encodeToString(hmac, Base64.NO_WRAP)
    }

    private fun createRequest(secure: Boolean) : Request {
        val request : Request = if (secure) {
            val hmacSignature = generateHmacSHA256Signature(jsonPayload, signingKey)

            Request.Builder()
                .url(url)
                .addHeader("HMAC-Signature", hmacSignature)
                .post(jsonPayload.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
                .build()
        } else {
             Request.Builder()
                .url(url)
                .post(jsonPayload.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
                .build()
        }
        return request
    }

    fun sendData(secure : Boolean, callback: (Boolean) -> Unit) {
        val okHttpClient = if (!secure) {
            nonSecureCommunicationClient()
        } else {
            secureCommunicationClient()
        }

        val request = createRequest(secure)
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Handle failure
                callback(false)
            }

            override fun onResponse(call: Call, response: Response) {
                // Handle success
                if (response.code == 200) {
                    callback(true)
                } else {
                    callback(false)
                }
            }
        })
    }
}