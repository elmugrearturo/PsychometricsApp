package com.arturocuriel.mipersonalidad.models

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
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

class ServerCommunication(val serverDomain : String,
                          val url : String,
                          val certificatePin : String,
                          val jsonPayload: String) {

    private fun secureCommunicationClient() : OkHttpClient{
                  // Certificate pinning
                val certificatePinner = CertificatePinner.Builder()
                    .add(serverDomain, certificatePin)
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

    private fun createRequest() : Request {
        // Create Request
        val request = Request.Builder()
            .url(url)
            .post(jsonPayload.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()

        return request
    }

    fun sendData(secure : Boolean, callback: (Boolean) -> Unit) {
        val okHttpClient = if (!secure) {
            nonSecureCommunicationClient()
        } else {
            secureCommunicationClient()
        }

        val request = createRequest()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Handle failure
                callback(false)
            }

            override fun onResponse(call: Call, response: Response) {
                // Handle success
                callback(true)
            }
        })
    }
}