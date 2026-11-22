package com.brennensubs.billvault.android

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONObject

data class Pending(
    val id: String,
    val operation: String,
    val localId: Long,
    val remoteId: Long?,
    val payload: JSONObject,
)

class VaultStorage(context: Context) : SQLiteOpenHelper(context, "billvault-mobile.db", null, 1) {
    private val prefs = context.getSharedPreferences("vault-selection", Context.MODE_PRIVATE)

    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE profiles(id TEXT PRIMARY KEY,json TEXT NOT NULL)")
        db.execSQL(
            "CREATE TABLE snapshots(vault_id TEXT PRIMARY KEY REFERENCES profiles(id) ON DELETE CASCADE,json TEXT NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE outbox(id TEXT PRIMARY KEY,vault_id TEXT NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,operation TEXT NOT NULL,local_id INTEGER NOT NULL,remote_id INTEGER,payload TEXT NOT NULL,sequence INTEGER NOT NULL)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        error("No migration path; database preserved")
    }

    fun active(): Profile? {
        val id = prefs.getString("active", null) ?: return null
        readableDatabase.rawQuery("SELECT json FROM profiles WHERE id=?", arrayOf(id)).use {
            return if (it.moveToFirst()) Profile.parse(JSONObject(it.getString(0))) else null
        }
    }

    fun select(profile: Profile) {
        writableDatabase.insertWithOnConflict(
            "profiles",
            null,
            ContentValues().apply {
                put("id", profile.id)
                put("json", profile.json().toString())
            },
            SQLiteDatabase.CONFLICT_IGNORE,
        )
        writableDatabase.update(
            "profiles",
            ContentValues().apply { put("json", profile.json().toString()) },
            "id=?",
            arrayOf(profile.id),
        )
        prefs.edit().putString("active", profile.id).apply()
    }

    fun profiles(): List<Profile> {
        val list = mutableListOf<Profile>()
        readableDatabase.rawQuery("SELECT json FROM profiles ORDER BY rowid DESC", null).use {
            while (it.moveToNext()) list += Profile.parse(JSONObject(it.getString(0)))
        }
        return list
    }

    fun clearSelection() {
        prefs.edit().remove("active").apply()
    }

    fun snapshot(id: String): Snapshot {
        readableDatabase.rawQuery("SELECT json FROM snapshots WHERE vault_id=?", arrayOf(id)).use {
            return if (it.moveToFirst()) Snapshot.parse(JSONObject(it.getString(0)))
            else Snapshot.empty()
        }
    }

    private fun writeSnapshot(id: String, snapshot: Snapshot) {
        writableDatabase.insertWithOnConflict(
            "snapshots",
            null,
            ContentValues().apply {
                put("vault_id", id)
                put("json", snapshot.json().toString())
            },
            SQLiteDatabase.CONFLICT_REPLACE,
        )
    }

    fun pending(id: String): List<Pending> {
        val list = mutableListOf<Pending>()
        readableDatabase
            .rawQuery(
                "SELECT id,operation,local_id,remote_id,payload FROM outbox WHERE vault_id=? ORDER BY sequence,rowid",
                arrayOf(id),
            )
            .use {
                while (it.moveToNext()) list +=
                    Pending(
                        it.getString(0),
                        it.getString(1),
                        it.getLong(2),
                        if (it.isNull(3)) null else it.getLong(3),
                        JSONObject(it.getString(4)),
                    )
            }
        return list
    }

    fun save(
        id: String,
        snapshot: Snapshot,
        pending: Pending? = null,
        acknowledge: String? = null,
    ) {
        writableDatabase.beginTransaction()
        try {
            writeSnapshot(id, snapshot)
            if (pending != null)
                writableDatabase.insertOrThrow(
                    "outbox",
                    null,
                    ContentValues().apply {
                        put("id", pending.id)
                        put("vault_id", id)
                        put("operation", pending.operation)
                        put("local_id", pending.localId)
                        put("remote_id", pending.remoteId)
                        put("payload", pending.payload.toString())
                        put("sequence", System.currentTimeMillis())
                    },
                )
            if (acknowledge != null)
                writableDatabase.delete("outbox", "id=? AND vault_id=?", arrayOf(acknowledge, id))
            writableDatabase.setTransactionSuccessful()
        } finally {
            writableDatabase.endTransaction()
        }
    }

    fun discard(id: String) {
        writableDatabase.delete("outbox", "vault_id=?", arrayOf(id))
    }
}

class SecretStorage(context: Context) {
    private val prefs = context.getSharedPreferences("connection-secrets", Context.MODE_PRIVATE)

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey("billvault-connections", null) as? SecretKey)?.let {
            return it
        }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            .apply {
                init(
                    KeyGenParameterSpec.Builder(
                            "billvault-connections",
                            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                        )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .build()
                )
            }
            .generateKey()
    }

    fun put(id: String, secret: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        prefs
            .edit()
            .putString(
                id,
                Base64.encodeToString(
                    cipher.iv + cipher.doFinal(secret.toByteArray(Charsets.UTF_8)),
                    Base64.NO_WRAP,
                ),
            )
            .apply()
    }

    fun get(id: String): String? {
        val value = prefs.getString(id, null) ?: return null
        val bytes = Base64.decode(value, Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
        return cipher.doFinal(bytes.copyOfRange(12, bytes.size)).toString(Charsets.UTF_8)
    }

    fun remove(id: String) {
        prefs.edit().remove(id).apply()
    }
}
