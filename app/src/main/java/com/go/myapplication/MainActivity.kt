package com.go.myapplication

import android.annotation.SuppressLint
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "zqn-activity"
        const val MESSAGE_NEW_BOOK_ARRIVED = 1
    }

    lateinit var mTvBookList: TextView
    var mRemoteBookManager: IBookManager? = null

    val mIOnNewBookArrivedListener = object : IOnNewBookArrivedListener.Stub() {
        @Throws(RemoteException::class)
        override fun onNewBookArrived(newBook: Book?) {
            Log.d(TAG, "mIOnNewBookArrivedListener 客户端收到回调：THREAD:${Thread.currentThread().name}  PROCESS:${Application.getProcessName()}")
            mHandler.obtainMessage(MESSAGE_NEW_BOOK_ARRIVED, newBook).sendToTarget()
        }
    }

    val mHandler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MESSAGE_NEW_BOOK_ARRIVED -> {
                    Log.d(TAG, "收到新书：${msg.obj}")
                    BookListAsyncTask().execute()
                }
                else -> super.handleMessage(msg)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mTvBookList = findViewById(R.id.main_tv_book_list)
    }

    override fun onDestroy() {
        mRemoteBookManager?.let {
            if (it.asBinder().isBinderAlive) {
                try {
                    Log.d(TAG, "解除注册")
                    it.unregisterlistener(mIOnNewBookArrivedListener)
                } catch (e: RemoteException) {

                }
            }
        }
        try {
            unbindService(mConnection)
        } catch (e: Exception) {
        }
        super.onDestroy()
    }

    fun bindService(view: View) {
        bindService(
            Intent(this, BookManagerService::class.java),
            mConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    fun getBookList(view: View) {
        BookListAsyncTask().execute()
        Toast.makeText(applicationContext, "正在获取中。。。", Toast.LENGTH_SHORT).show()
    }

    private val mConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val bookManager = IBookManager.Stub.asInterface(service)
            try {
                mRemoteBookManager = bookManager
                val newBook = Book(3, "kotlin")
                bookManager.addBook(newBook)
                BookListAsyncTask().execute()
                bookManager.registerListener(mIOnNewBookArrivedListener)
            } catch (e: Exception) {
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mRemoteBookManager = null
            Log.d(TAG, "绑定结束")
        }
    }

    @SuppressLint("StaticFieldLeak")
    inner class BookListAsyncTask : AsyncTask<Void, Void, List<Book>>() {
        override fun doInBackground(vararg params: Void?): List<Book>? {
            var list: List<Book>? = null
            try {
                Log.d(TAG, "BookListAsyncTask：doInBackground THREAD:${Thread.currentThread().name} PROCESS:${Application.getProcessName()}")
                list = mRemoteBookManager?.bookList
            } catch (e: RemoteException) {
            }
            return list
        }

        override fun onPostExecute(result: List<Book>) {
            Log.d(TAG, "BookListAsyncTask：onPostExecute THREAD:${Thread.currentThread().name}   PROCESS:${Application.getProcessName()}")
            var content = ""
            for (i in result.indices) {
                content += result[i].toString() + "\n"
            }
            mTvBookList.text = content
        }
    }

    inner class BookNumAsyncTask : AsyncTask<Void, Void, Int>() {
        override fun doInBackground(vararg params: Void?): Int {
            Log.d(TAG, "BookNumAsyncTask：doInBackground THREAD:${Thread.currentThread().name}  PROCESS:${Application.getProcessName()}")
            return getListNum()
        }

        override fun onPostExecute(result: Int?) {
            Log.d(TAG, "BookNumAsyncTask：doInBackground THREAD:${Thread.currentThread().name}  PROCESS:${Application.getProcessName()}")
            Toast.makeText(applicationContext, "图书数量：$result", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getListNum(): Int {
        return mRemoteBookManager?.let {
            try {
                val list = it.bookList
                list.size
            } catch (e: RemoteException) {
                0
            }
        } ?: 0
    }
}