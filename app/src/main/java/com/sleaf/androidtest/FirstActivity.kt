package com.sleaf.androidtest

import android.app.Activity
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.NotificationCompat.getExtras
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.EditText

class FirstActivity : AppCompatActivity() {
    companion object {
        //发送Intent对应字符串内容的key
        val Intent_key = "MESSAGE"
    }

    //EditText
    private var editText: EditText? = null

    private fun initView() {
        editText = findViewById<View>(R.id.edit_message) as EditText?
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //设置布局
        setContentView(R.layout.activity_first)
        initView()
    }

    //发送消息，启动secondActivity!
    fun sendMessage(view: View) {
        val intent = Intent(this, SecondActivity::class.java)
        val text = editText!!.text.toString()
        intent.putExtra(Intent_key, text)
        startActivityForResult(intent, 0)//此处的requestCode应与下面结果处理函中调用的requestCode一致
    }

    //结果处理函数，当从secondActivity中返回时调用此函数
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0 && resultCode == Activity.RESULT_OK) {
            val bundle = data.extras
            var text: String? = null
            if (bundle != null)
                text = bundle.getString("second")
            Log.d("text", text)
            editText!!.setText(text)
        }
    }
}
