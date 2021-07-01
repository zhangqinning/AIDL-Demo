// IOnNewBookArrivedListener.aidl
package com.go.myapplication;

// Declare any non-default types here with import statements

import com.go.myapplication.Book;

interface IOnNewBookArrivedListener {
   void onNewBookArrived(in Book newBook);
}