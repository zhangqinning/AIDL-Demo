package com.go.myapplication

import android.annotation.SuppressLint
import android.app.Application
import android.app.Service
import android.content.Intent
import android.os.*
import android.util.Log
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

class BookManagerService : Service() {
    companion object {
        const val TAG = "zqn-service"
    }

    val mBookList = CopyOnWriteArrayList<Book>()
    val mListenerList = RemoteCallbackList<IOnNewBookArrivedListener>()
    val mIsServiceDestroyed = AtomicBoolean(false)

    private val mBinder = object : IBookManager.Stub() {
        override fun getBookList(): MutableList<Book> {
            Log.d(TAG, "service getBookList service THREAD:${Thread.currentThread().name}  PROCESS:${Application.getProcessName()}")
            SystemClock.sleep(5000)
            return mBookList
        }

        override fun addBook(book: Book?) {
            mBookList.add(book)
        }

        override fun registerListener(listener: IOnNewBookArrivedListener) {
            mListenerList.register(listener)
            val num = mListenerList.beginBroadcast()
            mListenerList.finishBroadcast()
            Log.d(TAG, "添加完成，注册接口数：$num service registerListener THREAD:${Thread.currentThread().name}  PROCESS:${Application.getProcessName()}")
        }

        @Throws(RemoteException::class)
        override fun unregisterlistener(listener: IOnNewBookArrivedListener) {
            mListenerList.unregister(listener)
            val num = mListenerList.beginBroadcast()
            mListenerList.finishBroadcast()
            Log.d(TAG, "删除完成，注册接口数：$num")
        }

    }

    override fun onCreate() {
        super.onCreate()
        mBookList.add(Book(1, "Android"))
        mBookList.add(Book(2, "iOS"))
        Thread(ServiceWorker()).start()
    }

    override fun onDestroy() {
        mIsServiceDestroyed.set(true)
        mHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder {
        return mBinder
    }

    val mHandler =
        @SuppressLint("HandlerLeak")
        object : Handler() {
            override fun handleMessage(msg: Message) {
                val bookID = 1 + mBookList.size
                val newBook = Book(bookID, "新书: $bookID")
                try {
                    onNewBookArrived(newBook)
                } catch (e: RemoteException) {
                }
            }
        }

    @Throws(RemoteException::class)
    fun onNewBookArrived(book: Book) {
        mBookList.add(book)
        Log.d(TAG, "发送通知的数量：${mBookList.size}")
        val num = mListenerList.beginBroadcast()
        for (i in 0 until num) {
            val listener = mListenerList.getBroadcastItem(i)
            Log.d(TAG, "发送通知：$listener")
            listener.onNewBookArrived(book)
        }
        mListenerList.finishBroadcast()
    }

    var num = 0

    inner class ServiceWorker : Runnable {
        override fun run() {
            while (!mIsServiceDestroyed.get()) {
                try {
                    Thread.sleep(5000)
                } catch (e: Exception) {
                }
                num++
                if (num == 10) {
                    mIsServiceDestroyed.set(true)
                }
                mHandler.sendMessage(Message())
            }
        }
    }

}



