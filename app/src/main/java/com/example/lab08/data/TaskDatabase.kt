package com.example.lab08.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.lab08.model.Task

@Database(entities = [Task::class], version = 1)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
}
