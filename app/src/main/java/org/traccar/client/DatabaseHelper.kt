/*
 * Copyright 2015 - 2022 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress("DEPRECATION", "StaticFieldLeak")
package org.traccar.client

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.AsyncTask
import java.sql.Date

data class FormSubmission(
    val id: String,
    val containerId: String,
    val comment: String,
    val deviceId: String,
    val timestamp: Long
)

data class User(
    val id: Long,
    val phone: String?,
    val firstName: String?,
    val lastName: String?,
    val password: String?
)


class DatabaseHelper(context: Context?) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    interface DatabaseHandler<T> {
        fun onComplete(success: Boolean, result: T)
    }

    private abstract class DatabaseAsyncTask<T>(val handler: DatabaseHandler<T?>) : AsyncTask<Unit, Unit, T?>() {

        private var error: RuntimeException? = null

        override fun doInBackground(vararg params: Unit): T? {
            return try {
                executeMethod()
            } catch (error: RuntimeException) {
                this.error = error
                null
            }
        }

        protected abstract fun executeMethod(): T

        override fun onPostExecute(result: T?) {
            handler.onComplete(error == null, result)
        }
    }

    private val db: SQLiteDatabase = writableDatabase

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE position (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "deviceId TEXT," +
                    "time INTEGER," +
                    "latitude REAL," +
                    "longitude REAL," +
                    "altitude REAL," +
                    "speed REAL," +
                    "course REAL," +
                    "accuracy REAL," +
                    "battery REAL," +
                    "charging INTEGER," +
                    "mock INTEGER)"
        )
        db.execSQL(
            "CREATE TABLE form_submissions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "containerId TEXT," +
                    "comment TEXT," +
                    "deviceId TEXT," +
                    "timestamp INTEGER)"
        )
        db.execSQL(
            "CREATE TABLE user (" +
                    "id INTEGER PRIMARY KEY," +
                    "phone TEXT," +
                    "firstName TEXT," +
                    "lastName TEXT," +
                    "password TEXT)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS position;")
        db.execSQL("DROP TABLE IF EXISTS form_submissions;")
        db.execSQL("DROP TABLE IF EXISTS user;")
        onCreate(db)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS position;")
        db.execSQL("DROP TABLE IF EXISTS form_submissions;")
        db.execSQL("DROP TABLE IF EXISTS user;")
        onCreate(db)
    }

    fun insertPosition(position: Position) {
        val values = ContentValues()
        values.put("deviceId", position.deviceId)
        values.put("time", position.time.time)
        values.put("latitude", position.latitude)
        values.put("longitude", position.longitude)
        values.put("altitude", position.altitude)
        values.put("speed", position.speed)
        values.put("course", position.course)
        values.put("accuracy", position.accuracy)
        values.put("battery", position.battery)
        values.put("charging", if (position.charging) 1 else 0)
        values.put("mock", if (position.mock) 1 else 0)
        db.insertOrThrow("position", null, values)
    }

    fun insertPositionAsync(position: Position, handler: DatabaseHandler<Unit?>) {
        object : DatabaseAsyncTask<Unit>(handler) {
            override fun executeMethod() {
                insertPosition(position)
            }
        }.execute()
    }

    @SuppressLint("Range")
    fun selectPosition(): Position? {
        db.rawQuery("SELECT * FROM position ORDER BY id LIMIT 1", null).use { cursor ->
            if (cursor.count > 0) {
                cursor.moveToFirst()
                return Position(
                    id = cursor.getLong(cursor.getColumnIndex("id")),
                    deviceId = cursor.getString(cursor.getColumnIndex("deviceId")),
                    time = Date(cursor.getLong(cursor.getColumnIndex("time"))),
                    latitude = cursor.getDouble(cursor.getColumnIndex("latitude")),
                    longitude = cursor.getDouble(cursor.getColumnIndex("longitude")),
                    altitude = cursor.getDouble(cursor.getColumnIndex("altitude")),
                    speed = cursor.getDouble(cursor.getColumnIndex("speed")),
                    course = cursor.getDouble(cursor.getColumnIndex("course")),
                    accuracy = cursor.getDouble(cursor.getColumnIndex("accuracy")),
                    battery = cursor.getDouble(cursor.getColumnIndex("battery")),
                    charging = cursor.getInt(cursor.getColumnIndex("charging")) > 0,
                    mock = cursor.getInt(cursor.getColumnIndex("mock")) > 0,
                )
            }
        }
        return null
    }

    fun selectPositionAsync(handler: DatabaseHandler<Position?>) {
        object : DatabaseAsyncTask<Position?>(handler) {
            override fun executeMethod(): Position? {
                return selectPosition()
            }
        }.execute()
    }

    fun deletePosition(id: Long) {
        if (db.delete("position", "id = ?", arrayOf(id.toString())) != 1) {
            throw SQLException()
        }
    }

    fun deletePositionAsync(id: Long, handler: DatabaseHandler<Unit?>) {
        object : DatabaseAsyncTask<Unit>(handler) {
            override fun executeMethod() {
                deletePosition(id)
            }
        }.execute()
    }

    fun insertFormSubmission(submission: FormSubmission) {
        val values = ContentValues()
        values.put("containerId", submission.containerId)
        values.put("comment", submission.comment)
        values.put("deviceId", submission.deviceId)
        values.put("timestamp", submission.timestamp)
        db.insertOrThrow("form_submissions", null, values)
    }

    fun insertFormSubmissionAsync(submission: FormSubmission, handler: DatabaseHandler<Unit?>) {
        object : DatabaseAsyncTask<Unit>(handler) {
            override fun executeMethod() {
                insertFormSubmission(submission)
            }
        }.execute()
    }

    @SuppressLint("Range")
    fun selectAllFormSubmissions(): List<FormSubmission> {
        val submissions = mutableListOf<FormSubmission>()
        db.rawQuery("SELECT * FROM form_submissions ORDER BY timestamp DESC", null).use { cursor ->
            while (cursor.moveToNext()) {
                submissions.add(
                    FormSubmission(
                        id = cursor.getLong(cursor.getColumnIndex("id")).toString(),
                        containerId = cursor.getString(cursor.getColumnIndex("containerId")),
                        comment = cursor.getString(cursor.getColumnIndex("comment")),
                        deviceId = cursor.getString(cursor.getColumnIndex("deviceId")),
                        timestamp = cursor.getLong(cursor.getColumnIndex("timestamp"))
                    )
                )
            }
        }
        return submissions
    }

    fun selectAllFormSubmissionsAsync(handler: DatabaseHandler<List<FormSubmission>?>) {
        object : DatabaseAsyncTask<List<FormSubmission>>(handler) {
            override fun executeMethod(): List<FormSubmission> {
                return selectAllFormSubmissions()
            }
        }.execute()
    }

    fun deleteFormSubmission(id: Long) {
        if (db.delete("form_submissions", "id = ?", arrayOf(id.toString())) != 1) {
            throw SQLException()
        }
    }

    fun deleteFormSubmissionAsync(id: Long, handler: DatabaseHandler<Unit?>) {
        object : DatabaseAsyncTask<Unit>(handler) {
            override fun executeMethod() {
                deleteFormSubmission(id)
            }
        }.execute()
    }


    // User methods
    fun insertUser(user: User) {
        val values = ContentValues()
        values.put("id", user.id)
        values.put("phone", user.phone)
        values.put("firstName", user.firstName)
        values.put("lastName", user.lastName)
        values.put("password", user.password)
        db.insertOrThrow("user", null, values)
    }

    fun insertUserAsync(user: User, handler: DatabaseHandler<Unit?>) {
        object : DatabaseAsyncTask<Unit?>(handler) {
            override fun executeMethod(): Unit? {
                insertUser(user)
                return null
            }
        }.execute()
    }

    @SuppressLint("Range")
    fun selectUser(): User? {
        db.rawQuery("SELECT * FROM user LIMIT 1", null).use { cursor ->
            if (cursor.count > 0) {
                cursor.moveToFirst()
                return User(
                    id = cursor.getLong(cursor.getColumnIndex("id")),
                    phone = cursor.getString(cursor.getColumnIndex("phone")),
                    firstName = cursor.getString(cursor.getColumnIndex("firstName")),
                    lastName = cursor.getString(cursor.getColumnIndex("lastName")),
                    password = cursor.getString(cursor.getColumnIndex("password"))
                )
            }
        }
        return null
    }

    fun selectUserAsync(handler: DatabaseHandler<User?>) {
        object : DatabaseAsyncTask<User?>(handler) {
            override fun executeMethod(): User? {
                return selectUser()
            }
        }.execute()
    }

    companion object {
        const val DATABASE_VERSION = 4
        const val DATABASE_NAME = "traccar.db"
    }

}
