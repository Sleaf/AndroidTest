package com.sleaf.androidtest

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.view.View


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun onButton1(view: View?) {
        val intent = Intent(this, FirstActivity::class.java)
        startActivityForResult(intent, 0)
    }

    fun onButton2(view: View?) {
        val intent = Intent(this, DownloadActivity::class.java)
        startActivityForResult(intent, 0)
    }

}
