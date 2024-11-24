package com.example.mydialer

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.IOException

data class Contact(
    val name: String,
    val phone: String,
    val type: String
)

class ContactDiffCallback : DiffUtil.ItemCallback<Contact>() {
    override fun areItemsTheSame(oldItem: Contact, newItem: Contact): Boolean {
        return oldItem.phone == newItem.phone
    }

    override fun areContentsTheSame(oldItem: Contact, newItem: Contact): Boolean {
        return oldItem == newItem
    }
}

class MainActivity : AppCompatActivity() {
    private val client = OkHttpClient()
    private lateinit var contactAdapter: ContactAdapter
    private lateinit var contactsList: List<Contact>
    private lateinit var searchEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Timber.plant(Timber.DebugTree())

        searchEditText = findViewById(R.id.et_search)
        val recyclerView = findViewById<RecyclerView>(R.id.rView)

        recyclerView.layoutManager = LinearLayoutManager(this)
        contactAdapter = ContactAdapter()
        recyclerView.adapter = contactAdapter
        val sharedPreferences = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val searchFilter = sharedPreferences.getString("SEARCH_FILTER", "")
        searchEditText.setText(searchFilter)

        loadContacts("https://drive.google.com/uc?export=download&id=1-KO-9GA3NzSgIc1dkAsNm8Dqw0fuPxcR")

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val searchText = s.toString()
                Timber.d("Searching for: $searchText")

                if (searchText.isEmpty()) {
                    contactAdapter.submitList(contactsList)
                } else {
                    val filteredContacts = contactsList.filter { contact ->
                        contact.name.contains(searchText, ignoreCase = true)
                    }
                    contactAdapter.submitList(filteredContacts)
                }
            }
            override fun afterTextChanged(s: Editable?) {
                val searchText = s.toString()
                Timber.d("Searching for: $searchText")

                val sharedPreferences = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.putString("SEARCH_FILTER", searchText)
                editor.apply()
            }

        })
    }

    private fun loadContacts(url: String) {
        Thread {
            val request = Request.Builder().url(url).build()
            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val json = response.body?.string()
                    if (json != null) {
                        val contacts = parseContacts(json)
                        runOnUiThread {
                            contactsList = contacts
                            contactAdapter.submitList(contactsList)
                        }
                    }
                } else {
                    Timber.e("Failed to access file: ${response.message}")
                }
            } catch (e: IOException) {
                Timber.e(e, "Error loading contacts")
            }
        }.start()
    }

    private fun parseContacts(json: String): List<Contact> {
        val listType = object : TypeToken<List<Contact>>() {}.type
        return Gson().fromJson(json, listType)?: emptyList()
    }
}