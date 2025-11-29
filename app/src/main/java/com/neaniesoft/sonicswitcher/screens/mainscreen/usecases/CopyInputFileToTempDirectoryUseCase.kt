package com.neaniesoft.sonicswitcher.screens.mainscreen.usecases

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.neaniesoft.sonicswitcher.di.MainScreenModule
import com.neaniesoft.sonicswitcher.screens.mainscreen.errors.FileCopyException
import com.neaniesoft.sonicswitcher.screens.mainscreen.models.FileWithUri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named

class CopyInputFileToTempDirectoryUseCase
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val contentResolver: ContentResolver,
        private val getFileDisplayName: GetFileDisplayNameUseCase,
        @Named(MainScreenModule.CACHE_DIR) private val cacheDir: File,
    ) {
        operator fun invoke(inputUri: Uri): FileWithUri {
            val fileName = getFileDisplayName(inputUri)
            return try {
                checkNotNull(contentResolver.openInputStream(inputUri)).use { inputStream ->
                    val outputFile = File(cacheDir, fileName)
                    FileOutputStream(outputFile).use { outputStream ->
                        inputStream.copyTo(outputStream, 8 * 1024)
                    }
                    outputFile
                }
            } catch (e: FileNotFoundException) {
                throw FileCopyException("File not found.", e)
            } catch (e: SecurityException) {
                throw FileCopyException("Security exception while copying file", e)
            } catch (e: IOException) {
                throw FileCopyException("IOException while copying buffer to file", e)
            }.let { file ->
                FileWithUri(file, file.contentUri())
            }
        }

        private fun File.contentUri(): Uri {
            return try {
                FileProvider.getUriForFile(context, "com.neaniesoft.sonicswitcher.FileProvider", this)
            } catch (e: IllegalArgumentException) {
                throw FileCopyException("Could not get Uri for File", e)
            }
        }
    }
