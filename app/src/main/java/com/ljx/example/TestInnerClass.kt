package com.ljx.example

import android.app.job.JobParameters
import android.app.job.JobService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TestInnerClass: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
    }

    class NotifyJobService : JobService() {
        override fun onStartJob(params: JobParameters?): Boolean {
            return false
        }

        override fun onStopJob(params: JobParameters?): Boolean {
            return false
        }

    }

    class P0 : JobService() {
        override fun onStartJob(params: JobParameters?): Boolean {
            return false
        }

        override fun onStopJob(params: JobParameters?): Boolean {
            return false
        }

    }

    class P1 : JobService() {
        override fun onStartJob(params: JobParameters?): Boolean {
            return false
        }

        override fun onStopJob(params: JobParameters?): Boolean {
            return false
        }

    }
}