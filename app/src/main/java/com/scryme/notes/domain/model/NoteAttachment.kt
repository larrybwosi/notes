package com.scryme.notes.domain.model

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class NoteAttachment(
    val id: String,
    val name: String,
    val uri: String,
    val size: String,
    val mimeType: String,
    val addedAt: Long,
)

object AttachmentHelper {
    fun getAttachmentsForNote(
        context: Context,
        noteId: String,
    ): List<NoteAttachment> {
        val prefs = context.getSharedPreferences("notes_prefs", Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("attachments_note_$noteId", "[]") ?: "[]"
        return deserializeAttachments(jsonStr)
    }

    fun saveAttachmentsForNote(
        context: Context,
        noteId: String,
        attachments: List<NoteAttachment>,
    ) {
        val prefs = context.getSharedPreferences("notes_prefs", Context.MODE_PRIVATE)
        val jsonStr = serializeAttachments(attachments)
        prefs.edit().putString("attachments_note_$noteId", jsonStr).apply()
    }

    fun addAttachmentToNote(
        context: Context,
        noteId: String,
        attachment: NoteAttachment,
    ) {
        val current = getAttachmentsForNote(context, noteId).toMutableList()
        current.add(attachment)
        saveAttachmentsForNote(context, noteId, current)
    }

    fun removeAttachmentFromNote(
        context: Context,
        noteId: String,
        attachmentId: String,
    ) {
        val current = getAttachmentsForNote(context, noteId).filter { it.id != attachmentId }
        saveAttachmentsForNote(context, noteId, current)
    }

    fun serializeAttachments(attachments: List<NoteAttachment>): String {
        val jsonArray = JSONArray()
        for (attachment in attachments) {
            val jsonObject =
                JSONObject().apply {
                    put("id", attachment.id)
                    put("name", attachment.name)
                    put("uri", attachment.uri)
                    put("size", attachment.size)
                    put("mimeType", attachment.mimeType)
                    put("addedAt", attachment.addedAt)
                }
            jsonArray.put(jsonObject)
        }
        return jsonArray.toString()
    }

    fun deserializeAttachments(jsonStr: String): List<NoteAttachment> {
        val list = mutableListOf<NoteAttachment>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val attachment =
                    NoteAttachment(
                        id = jsonObject.getString("id"),
                        name = jsonObject.getString("name"),
                        uri = jsonObject.getString("uri"),
                        size = jsonObject.getString("size"),
                        mimeType = jsonObject.getString("mimeType"),
                        addedAt = jsonObject.optLong("addedAt", System.currentTimeMillis()),
                    )
                list.add(attachment)
            }
        } catch (e: Exception) {
            // Log or ignore corrupt json
        }
        return list
    }
}
