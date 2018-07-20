/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.api.examples.kotlin.client

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.TextView
import com.google.cloud.speech.v1.LongRunningRecognizeRequest
import com.google.cloud.speech.v1.RecognitionAudio
import com.google.cloud.speech.v1.RecognitionConfig
import com.google.cloud.speech.v1.SpeechClient
import com.google.common.io.ByteStreams
import com.google.experimental.examples.kotlin.R
import com.google.api.examples.kotlin.util.OnMainThread
import com.google.protobuf.ByteString

/**
 * Kotlin example showcasing LRO using the client library.
 *
 * @author jbolinger
 */
class MainActivityLRO : AppCompatActivity() {

    private val client by lazy {
        // create a client using a service account for simplicity
        // refer to see MainActivity for more details on how to authenticate
        applicationContext.resources.openRawResource(R.raw.sa).use {
            SpeechClient.fromServiceAccount(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val textView: TextView = findViewById(R.id.text_view)

        // get audio
        val audio = applicationContext.resources.openRawResource(R.raw.audio).use {
            ByteString.copyFrom(ByteStreams.toByteArray(it))
        }

        val lro = client.longRunningRecognize(LongRunningRecognizeRequest.newBuilder()
                .setAudio(RecognitionAudio.newBuilder()
                        .setContent(audio)
                        .build())
                .setConfig(RecognitionConfig.newBuilder()
                        .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                        .setSampleRateHertz(16000)
                        .setLanguageCode("en-US")
                        .build())
                .build())

        lro.enqueue(OnMainThread) {
            textView.text = "The API says: ${it.body}\n via operation: ${lro.operation?.name}"
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        client.shutdownChannel()
    }
}