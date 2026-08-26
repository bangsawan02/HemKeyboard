package com.example.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object DatabaseProvider {
    @Volatile
    private var INSTANCE: KeyboardDatabase? = null

    fun getDatabase(context: Context): KeyboardDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                KeyboardDatabase::class.java,
                "keyboard_database"
            )
            .fallbackToDestructiveMigration(true)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    // Pre-populate database with common Indonesian words
                    CoroutineScope(Dispatchers.IO).launch {
                        val initialWords = listOf(
                            "saya", "yang", "dan", "untuk", "dengan", "kamu", "dia", "kita", "mereka",
                            "bisa", "ada", "dari", "akan", "dalam", "bukan", "sudah", "belum", "sangat", "ialah",
                            "adalah", "karena", "tetapi", "bahwa", "seperti", "kalau", "jika", "maka", "pada", "ke",
                            "tentang", "banyak", "sedikit", "semua", "beberapa", "tahun", "hari", "waktu", "buku", "rumah",
                            "jalan", "sekolah", "anak", "orang", "kerja", "makan", "minum", "tidur", "pergi", "datang",
                            "baru", "lama", "besar", "kecil", "bagus", "jelek", "baik", "buruk", "sehat", "sakit",
                            "nama", "apa", "siapa", "mengapa", "bagaimana", "kapan", "di mana", "terima", "kasih", "sama",
                            "bantu", "buat", "tahu", "paham", "mengerti", "maaf", "halo", "selamat", "pagi", "siang",
                            "sore", "malam", "kembali", "sama-sama", "permisi", "silakan", "tolong", "lagi", "pun",
                            "ingin", "mau", "boleh", "harus", "mungkin", "pasti", "telah", "sedang", "pernah", "terus",
                            "jadi", "sebagai", "oleh", "atau", "hanya", "juga", "sampai", "sekarang", "besok", "kemarin",
                            "nanti", "tadi", "tengah", "depan", "belakang", "atas", "bawah", "luar", "dalam", "antara",
                            "sekali", "sering", "jarang", "selalu", "kadang", "tapi", "kan", "kok", "sih", "deh"
                        )
                        val timestamp = System.currentTimeMillis()
                        initialWords.distinct().forEach { word ->
                            db.execSQL("INSERT OR IGNORE INTO words (word, frequency, isUserCustom, timestamp) VALUES ('$word', 5, 0, $timestamp)")
                        }
                    }
                }
            })
            .build()
            INSTANCE = instance
            instance
        }
    }
}
