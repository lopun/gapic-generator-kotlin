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

package com.google.api.examples.kotlin.util

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.google.protobuf.ByteString
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

private const val TAG = "Audio"

/**
 * Create a new emitter
 */
internal class AudioEmitter() {

    private var audioRecorder: AudioRecord? = null
    private var buffer: ByteArray? = null
    private var audioExecutor: ScheduledExecutorService? = null

    /** Start streaming  */
    fun start(subscriber: (ByteString) -> Unit) {
        // TODO: in a real app you may not want to fix these
        val encoding = AudioFormat.ENCODING_PCM_16BIT
        val channel = AudioFormat.CHANNEL_IN_MONO
        val sampleRate = 16000

        audioExecutor = Executors.newSingleThreadScheduledExecutor()

        // create and configure recorder
        // Note: ensure settings are match the speech recognition config
        audioRecorder = AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.MIC)
                .setAudioFormat(AudioFormat.Builder()
                        .setEncoding(encoding)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channel)
                        .build())
                .build()
        buffer = ByteArray(2 * AudioRecord.getMinBufferSize(sampleRate, channel, encoding))

        // start!
        Log.d(TAG, "Recording audio with buffer size of: ${buffer!!.size} bytes")
        audioRecorder!!.startRecording()

        // stream bytes as they become available in chunks equal to the buffer size
        audioExecutor!!.scheduleAtFixedRate({
            // read audio data
            val read = audioRecorder!!.read(
                    buffer!!, 0, buffer!!.size, AudioRecord.READ_BLOCKING)

            // send next chunk
            if (read > 0) {
                subscriber(ByteString.copyFrom(buffer, 0, read))
            }
        }, 0, 10, TimeUnit.MILLISECONDS)
    }

    /** Stop Streaming  */
    fun stop() {
        // stop events
        audioExecutor?.shutdown()
        audioExecutor = null

        // stop recording
        audioRecorder?.stop()
        audioRecorder?.release()
        audioRecorder = null

        // release buffer
        buffer = null
    }
}