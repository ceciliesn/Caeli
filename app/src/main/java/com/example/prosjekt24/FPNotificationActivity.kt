package com.example.prosjekt24

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class FPNotificationActivity : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val notificationView =  inflater.inflate(R.layout.fragment_notification,container,false)

        return notificationView
    }

}

