package com.neaniesoft.sonicswitcher.screens.mainscreen

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class GetFileDisplayNameUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contentResolver: ContentResolver
) {
    operator fun invoke(uri: Uri?): String {
        return if (uri != null && uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        cursor.getString(index)
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
        } else {
            null
        } ?: uri?.pathSegments?.lastOrNull() ?: "unknown"
    }
}
