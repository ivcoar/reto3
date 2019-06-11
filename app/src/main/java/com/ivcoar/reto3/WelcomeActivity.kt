package com.ivcoar.reto3

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.EditText
import kotlinx.android.synthetic.main.activity_welcome.*

class WelcomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)
    }

    fun login(view: View) {
        val name = nameText.text.toString()
        if (name.isBlank()) {
            nameText.error = getString(R.string.err_empty_name)
        }
        else {
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra(EXTRA_NAME, nameText.text.toString().trim())
            }
            startActivity(intent)
        }
    }
}
