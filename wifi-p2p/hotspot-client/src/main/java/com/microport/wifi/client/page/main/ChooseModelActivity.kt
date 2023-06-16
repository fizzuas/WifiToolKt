package com.microport.wifi.client.page.main

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.microport.wifi.client.R


class ChooseModelActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_model)

        findViewById<Button>(R.id.but_p2p).setOnClickListener {
            val intent = Intent(this, PermissionActivity::class.java)
            intent.putExtra("TYPE",1)
            startActivity(intent)
        }
        findViewById<Button>(R.id.but_hotspot).setOnClickListener {
            val intent = Intent(this, PermissionActivity::class.java)
            intent.putExtra("TYPE",2)
            startActivity(intent)
        }
    }
}