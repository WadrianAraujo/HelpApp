package com.phaethoncreations.helpapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.Console
import java.io.IOException
import java.util.Locale
import java.util.Objects
import kotlin.math.log

class MainActivity : AppCompatActivity() {
    lateinit var textToSpeech: TextToSpeech
    lateinit var recyclerView: RecyclerView
    lateinit var welcomeText: TextView
    lateinit var messageEditText: EditText
    lateinit var sendButton: ImageButton
    lateinit var messageList: MutableList<Message>
    lateinit var messageAdapter: MessageAdapter
    lateinit var micButton: ImageButton
    val REQUEST_CODE_SPEECH_INPUT = 1
    val client = OkHttpClient()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        messageList = ArrayList()
        recyclerView = findViewById(R.id.recycler_view)
        welcomeText = findViewById(R.id.welcome_text)
        messageEditText = findViewById(R.id.message_edit_text)
        sendButton = findViewById(R.id.send_btn)
        micButton = findViewById(R.id.mic_btn)
        messageAdapter = MessageAdapter(messageList)
        recyclerView.adapter = messageAdapter
        val layoutManger = LinearLayoutManager(this)
        layoutManger.stackFromEnd = true
        recyclerView.layoutManager = layoutManger

        sendButton.setOnClickListener {
            val question = messageEditText.text.toString().trim { it <= ' ' }
            addToChat(question, Message.SENT_BY_ME)
            fetchData(question)
            messageEditText.setText("")
            //callAPI(question)
            welcomeText.visibility = View.GONE
        }

        micButton.setOnClickListener {

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)

            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Text")

            try {
                startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT)
            } catch (e: Exception) {
                Toast.makeText(this, " " + e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_SPEECH_INPUT) {
            if (resultCode == RESULT_OK && data != null) {
                val res: ArrayList<String> =
                    data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS) as ArrayList<String>
                val question = Objects.requireNonNull(res)[0]
                addToChat(question, Message.SENT_BY_ME)
                fetchData(question)
                messageEditText.setText("")
                welcomeText.visibility = View.GONE
            }
        }
    }

    private fun addToChat(message: String, sentBy: String) {
        runOnUiThread {
            messageList.add(Message(message, sentBy))
            messageAdapter.notifyDataSetChanged()
            recyclerView.smoothScrollToPosition(messageAdapter.itemCount)
        }
    }

    fun addResponse(response: String?) {
        messageList.removeAt(messageList.size - 1)
        addToChat(response!!, Message.SENT_BY_BOT)
    }

    companion object {
        val JSON: MediaType = "application/json; charset=utf-8".toMediaType()
    }

    private fun fetchData(userInput: String) {
        val url = "https://espyrtanos-oficina-3-ap2-first-aid-rest-api.hf.space/run/predict"

        messageList.add(Message("Typing...", Message.SENT_BY_BOT))

        val jsonArray = JSONArray()
        jsonArray.put(userInput)

        val json = JSONObject()
        json.put("data", jsonArray)

        val requestBody =
            RequestBody.create("application/json".toMediaTypeOrNull(), json.toString())

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseData = response.body?.string()
                    val jsonResponse = JSONObject(responseData)
                    val data = jsonResponse.getJSONArray("data")[0]
                    val duration = jsonResponse.getDouble("duration")

                    addResponse(data.toString())

                    textToSpeech = TextToSpeech(applicationContext,TextToSpeech.OnInitListener {
                        if (it == TextToSpeech.SUCCESS){

                            textToSpeech.language = Locale.ENGLISH
                            textToSpeech.setSpeechRate(1.0f)
                            textToSpeech.speak(data.toString(), TextToSpeech.QUEUE_ADD, null)

                        }
                    })

                    runOnUiThread {
                        println("Data: $data")
                        println("Duration: $duration")
                    }
                } else {
                    val errorBody = response.body?.string()
                    println("Error Code: ${response.code}")
                    println("Error Message: $errorBody")
                }
            }
        })
    }
}