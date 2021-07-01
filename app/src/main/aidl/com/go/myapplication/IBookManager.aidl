// IBookManager.aidl
package com.go.myapplication;

// Declare any non-default types here with import statements
import com.go.myapplication.Book;
import com.go.myapplication.IOnNewBookArrivedListener;


interface IBookManager {
    List<Book> getBookList();
    void addBook(in Book book);
    void registerListener(IOnNewBookArrivedListener listener);
    void unregisterlistener(IOnNewBookArrivedListener listener);
}