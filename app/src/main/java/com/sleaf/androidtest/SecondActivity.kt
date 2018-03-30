package com.sleaf.androidtest

import android.app.Activity
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.view.View
import android.widget.Button
import android.widget.TextView


class SecondActivity : AppCompatActivity() {
    private var button: Button? = null
    private var textView: TextView? = null

    //设置类ButtonListener实现接口,OnClickListener,在其中可以指定不同id的button对应不同的点击事件
    //可以借此将代码抽出来，提高代码的可阅读性
    private inner class ButtonListener : View.OnClickListener {
        override fun onClick(v: View) {
            when (v.getId()) {
                R.id.button -> {
                    val intent = intent
                    //这里使用bundle绷带来传输数据
                    val bundle = Bundle()
                    //传输的内容仍然是键值对的形式
                    bundle.putString("second", "hello world from secondActivity!")//回发的消息,hello world from secondActivity!
                    intent.putExtras(bundle)
                    setResult(Activity.RESULT_OK, intent)
                    finish()
                }
            }
        }
    }

    //初始化View
    fun initView() {
        button = findViewById<View>(R.id.button) as Button
        textView = findViewById<View>(R.id.textView) as TextView?
        button!!.setOnClickListener(
                ButtonListener()
        )
        val intent = intent
        val text = intent.getStringExtra(FirstActivity.Intent_key)
        textView!!.text = text
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)
        initView()
    }
}
