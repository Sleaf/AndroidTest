package com.sleaf.androidtest

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.*
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.io.Serializable
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlin.collections.ArrayList

class DownloadActivity : AppCompatActivity() {
    var mDownloadFilename: TextView? = null
    var mProgressBar: ProgressBar? = null
    var mToggle: Button? = null
    var isStart = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_download)
        mDownloadFilename = findViewById(R.id.downloadFilename)
        mProgressBar = findViewById(R.id.progressBar)
        mProgressBar?.max = 100
        mToggle = findViewById(R.id.toggleBtn)
        //创建文件信息对象
        val fileInfo = FileInfo(0, "http://180.153.105.146/sqdd.myapp.com/myapp/qqteam/tim/down/tim.apk?mkey=5abbb9fedaff7890&f=8f88&c=0&p=.apk", "tim.apk", 0, 0)
        mDownloadFilename?.text = fileInfo.fileName;
        //添加事件监听
        mToggle!!.setOnClickListener(View.OnClickListener {
            val intent = Intent(this@DownloadActivity, DownloadService::class.java)
            isStart = !isStart
            mToggle!!.text = if (isStart) "暂停下载" else "继续下载"
            intent.action = if (isStart) DownloadService.ACTION_START else DownloadService.ACTION_STOP
            intent.putExtra("fileInfo", fileInfo)
            startService(intent)
        })
        //注册广播器
        val filter = IntentFilter();
        filter.addAction(DownloadService.ACTION_UPDATE)
        registerReceiver(mReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mReceiver)
    }

    val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            if (DownloadService.ACTION_UPDATE == intent.action) {
                val finished = intent.getIntExtra("finished", 0)
                mProgressBar!!.progress = finished
                if (finished == 100) {
                    isStart = false
                    mToggle?.text = "重新下载"
                }
            }
        }
    }
}


//服务信息
class DownloadService : Service() {
    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_UPDATE = "ACTION_UPDATE"
        val DOWNLOAD_PATH = "${Environment.getExternalStorageDirectory().absolutePath}/Downloads/"
        const val MSG_INIT = 0

    }

    var mTask: DownloadTask? = null

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        //获得传来的参数
        val fileInfo: FileInfo? = intent.getSerializableExtra("fileInfo") as FileInfo
        Log.i("test", "${intent.action}:\t$fileInfo")
        when (intent.action) {
            ACTION_START -> {
                //启动初始化线程
                InitThread(fileInfo).start()
            }
            ACTION_STOP -> {
                if (mTask != null) {
                    mTask!!.isPause = true
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder {
        TODO("not implemented") //To change body of created functions use File | Sett ings | File Templates.
    }

    val mHandle = object : Handler() {
        override fun handleMessage(msg: Message?) {
            when (msg?.what) {
                MSG_INIT -> {
                    val fileInfo = msg.obj as FileInfo
                    Log.i("test", "Init:$fileInfo")
                    //启动下载任务
                    mTask = DownloadTask(this@DownloadService, fileInfo)
                    mTask!!.download()
                }
                else -> {
                }
            }
        }
    }

    inner class InitThread(private val mFileInfo: FileInfo?) : Thread() {
        override fun run() {
            var conn: HttpURLConnection? = null
            var raf: RandomAccessFile? = null
            try {
                //连接网络文件
                val url = URL(mFileInfo?.url)
                var length = -1
                conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 3000
                conn.requestMethod = "GET"
                if (conn.responseCode == HttpsURLConnection.HTTP_OK) {
                    //获得长度
                    length = conn.contentLength
                }
                if (length < 0) return
                val dir = File(DOWNLOAD_PATH)
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                //创建文件
                val file = File(dir, mFileInfo?.fileName)
                raf = RandomAccessFile(file, "rwd")
                //设置长度
                raf.setLength(length.toLong())
                mFileInfo?.length = length
                mHandle.obtainMessage(MSG_INIT, mFileInfo).sendToTarget()
            } catch (e: Exception) {
                Log.e("test", e.toString())
            } finally {
                conn?.disconnect()
                raf?.close()
            }
        }
    }
}

//文件信息
class FileInfo(val id: Int,
               val url: String,
               val fileName: String,
               var length: Int,
               val finished: Int) : Serializable {
    override fun toString(): String {
        return "fileInfo(id=$id, url=$url, fileName=$fileName, length=$length, finished=$finished)"
    }
}

//线程信息
class ThreadInfo(val id: Int, val url: String, val start: Int, val end: Int, val finished: Int) {
    override fun toString(): String {
        return "ThreadInfo(id=$id, start=$start, end=$end, finished=$finished)"
    }
}

//数据库
class DBHepler(context: Context?, name: String = DB_NAME, factory: SQLiteDatabase.CursorFactory? = null, version: Int = VERSION, errorHandler: DatabaseErrorHandler? = null) : SQLiteOpenHelper(context, name, factory, version, errorHandler) {
    companion object {
        const val DB_NAME = "download.db"
        const val VERSION = 1
        const val SQL_CREATE = "create table thread_info(_id integer primary key autoincrement, thread_id integer, url text, start integer,end integer,finished integer)"
        const val SQL_DROP = "drop table if exists thread_info"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL(SQL_DROP)
        db.execSQL(SQL_CREATE)
    }
}

//数据访问接口
interface ThreadDAO {
    fun insertThread(threadInfo: ThreadInfo)
    fun deleteThread(url: String, thread_id: Int)
    fun updateThread(url: String, thread_id: Int, finished: Int)
    fun getThreads(url: String): List<ThreadInfo>
    fun isExists(url: String, thread_id: Int): Boolean
}

class ThreadDAOImpl(context: Context?) : ThreadDAO {
    val mHelper = DBHepler(context)

    override fun insertThread(threadInfo: ThreadInfo) {
        val db = mHelper.writableDatabase
        db.execSQL("insert into thread_info(thread_id,url,start,end,finished) values(?,?,?,?,?)",
                arrayOf<Any>(threadInfo.id, threadInfo.url, threadInfo.start, threadInfo.end, threadInfo.finished))
        db.close()
    }

    override fun deleteThread(url: String, thread_id: Int) {
        val db = mHelper.writableDatabase
        db.execSQL("delete from thread_info where url= ? and thread_id= ? ", arrayOf(url, thread_id))
        db.close()
    }

    override fun updateThread(url: String, thread_id: Int, finished: Int) {
        val db = mHelper.writableDatabase
        db.execSQL("update thread_info set finished = ? where url= ? and thread_id= ? ", arrayOf(finished, url, thread_id))
        db.close()
    }

    override fun getThreads(url: String): List<ThreadInfo> {
        val db = mHelper.writableDatabase
        val list = ArrayList<ThreadInfo>()
        val cursor = db.rawQuery("select * from thread_info where url = ? ", arrayOf(url))
        while (cursor.moveToNext()) {
            list.add(ThreadInfo(cursor.getInt(cursor.getColumnIndex("thread_id")),
                    cursor.getString(cursor.getColumnIndex("url")),
                    cursor.getInt(cursor.getColumnIndex("start")),
                    cursor.getInt(cursor.getColumnIndex("end")),
                    cursor.getInt(cursor.getColumnIndex("finished"))))
        }
        cursor.close()
        db.close()
        return list
    }

    override fun isExists(url: String, thread_id: Int): Boolean {
        val db = mHelper.writableDatabase
        val cursor = db.rawQuery("select * from thread_info where url = ? and thread_id = ? ", arrayOf(url, thread_id.toString()))
        val res = cursor.moveToNext()
        cursor.close()
        db.close()
        return res
    }
}

class DownloadTask(val mContext: Context, val mFileInfo: FileInfo) {
    val mDao = ThreadDAOImpl(mContext)
    var mFinished = 0
    var isPause = false

    fun download() {
        //读取线程信息
        val threadInfoArr = mDao.getThreads(mFileInfo.url)
        val threadInfo =
                if (threadInfoArr.isEmpty()) {
                    ThreadInfo(0, mFileInfo.url, 0, mFileInfo.length, 0)
                } else {
                    threadInfoArr[0]
                }
        DownloadThread(threadInfo).start()

    }

    inner class DownloadThread(private val mThreadInfo: ThreadInfo) : Thread() {
        override fun run() {
            //插入线程信息
            if (!mDao.isExists(mThreadInfo.url, mThreadInfo.id)) {
                mDao.insertThread(mThreadInfo)
            }
            var conn: HttpURLConnection? = null
            var raf: RandomAccessFile? = null
            var input: InputStream? = null
            try {
                val url = URL(mThreadInfo.url)
                conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 3000
                conn.requestMethod = "GET"
                //设置下载位置
                val start = mThreadInfo.start + mThreadInfo.finished
                conn.setRequestProperty("Range", "bytes=$start-${mThreadInfo.end}")
                //设置写入位置
                val file = File(DownloadService.DOWNLOAD_PATH, mFileInfo.fileName)
                raf = RandomAccessFile(file, "rwd")
                raf.seek(start.toLong())
                val intent = Intent(DownloadService.ACTION_UPDATE)
                mFinished += mThreadInfo.finished
                //开始下载
                if (conn.responseCode == HttpsURLConnection.HTTP_PARTIAL) {
                    //读取数据
                    input = conn.inputStream
                    val buffer = ByteArray(1024 * 4)
                    var len = -1
                    var time = System.currentTimeMillis()
                    do {
                        len = input.read(buffer)
                        if (len == -1) break
                        raf.write(buffer, 0, len)
                        //更新进度
                        mFinished += len
                        if (System.currentTimeMillis() - time > 500) {
                            time = System.currentTimeMillis()
                            intent.putExtra("finished", ((mFinished.toDouble() / mFileInfo.length.toDouble()) * 100).toInt())
                            mContext.sendBroadcast(intent)
                            Log.i("test", "cur:$mFinished total:${mFileInfo.length} per:${((mFinished.toDouble() / mFileInfo.length.toDouble()) * 100).toInt()}%")
                        }
                        //在下载暂停时保存进度
                        if (isPause) {
                            mDao.updateThread(mThreadInfo.url, mThreadInfo.id, mFinished)
                            return
                        }
                    } while (len != -1)
                    //删除线程信息
                    mDao.deleteThread(mThreadInfo.url, mThreadInfo.id)
                    intent.putExtra("finished", 100)
                    mContext.sendBroadcast(intent)
                }
            } catch (e: Exception) {
                Log.e("test", e.toString())
            } finally {
                raf?.close()
                input?.close()
                conn!!.disconnect()

            }
        }

    }
}