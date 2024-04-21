package com.intern

import android.app.ProgressDialog
import android.content.ContentValues.TAG
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URL
import kotlin.concurrent.thread


class MainActivity : ComponentActivity() {

    suspend fun insertBitmaps(items: JsonArray, context: Context) {
        val progressDialog = ProgressDialog(context)
        progressDialog.setMessage("Загрузка...")
        progressDialog.setCancelable(false)
        progressDialog.show()
        val tableLayout = findViewById<TableLayout>(R.id.content_table)
        for (item in items.iterator()) {
            val attachments: JsonArray = item.jsonObject["attachments"]!!.jsonArray
            val tableRow = TableRow(this)
            val linearLayout = LinearLayout(this)
            linearLayout.orientation = LinearLayout.VERTICAL
            for (attachment in attachments.iterator()) {
                if (attachment.jsonObject["type"].toString().drop(1).dropLast(1) == "photo") {
                    val sizes: JsonArray = attachment.jsonObject["photo"]?.jsonObject?.get("sizes")!!.jsonArray
                    val url = sizes[sizes.size - 1].jsonObject["url"].toString().drop(1).dropLast(1)

                    val imageView = ImageView(this)
                    val inputStream = withContext(Dispatchers.IO) {
                        URL(url).openConnection().getInputStream()
                    }
                    val bitmap = withContext(Dispatchers.IO){
                        BitmapFactory.decodeStream(inputStream)
                    }
                    imageView.setImageBitmap(bitmap)
                    imageView.setPadding(10, 10, 20, 10)


                    val layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                    layoutParams.setMargins(10, 10, 0, 10)
                    linearLayout.addView(imageView, layoutParams)
                }
            }
            val textView = TextView(this)
            textView.text = item.jsonObject["text"]?.jsonPrimitive?.content
            linearLayout.addView(textView, LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
            val linearLayoutParams = TableRow.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
            linearLayoutParams.setMargins(10, 10, 10, 10)
            tableRow.addView(linearLayout, linearLayoutParams)
            tableRow.background = AppCompatResources.getDrawable(this, R.drawable.rectangle)
            tableLayout.addView(tableRow, TableLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        }
        progressDialog.dismiss()
    }
    fun callAuthorise(context: Context, editText: EditText, mainLayout:ConstraintLayout) {
        GlobalScope.launch(Dispatchers.Main) {
            authorise(context, editText, mainLayout)
        }
    }
    suspend fun authorise(context: Context, editText: EditText, mainLayout:ConstraintLayout) {
        val enteredToken: String = editText.text.toString()
        val wallGetRequest: String = withContext(Dispatchers.IO) {
            URL("https://api.vk.com/method/wall.get?owner_id=-146026097&domain=orel_codes&offset=0&count=50&filter=all&extended=0&access_token=$enteredToken&v=5.131").readText()
        }
        val wallGetResponse = withContext(Dispatchers.IO) {
            Json.decodeFromString<JsonObject>(wallGetRequest)
        }
        val elementsLayout = layoutInflater.inflate(R.layout.activity_elements, mainLayout, false)
        setContentView(elementsLayout)

        val items: JsonArray = wallGetResponse["response"]?.jsonObject?.get("items")!!.jsonArray

        insertBitmaps(items, this)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        val mainLayout = findViewById<ConstraintLayout>(R.id.main_activity)
        val editText = findViewById<EditText>(R.id.editText)
        findViewById<Button>(R.id.start_button)?.setOnClickListener((View.OnClickListener {
            val enteredUrl: String = editText.text.toString()
            if (enteredUrl.isEmpty()) {
                Toast.makeText(applicationContext, "Адрес сообщества пуст", Toast.LENGTH_SHORT).show()
            } else {
                callAuthorise(this, editText, mainLayout)
            }
        }))
    }
}

