package com.fatihenes.photoreport.core.export.pdf

import android.util.Log
import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets
import java.util.Locale

object PdfMetadataWriter {
    private const val TAG = "PdfMetadataWriter"

    fun injectMetadata(
        pdfFile: File,
        title: String,
        author: String = "FotoRapor Licensed Field Professional",
        creator: String = "FotoRapor Enterprise Engineering Suite v2.5",
        subject: String = "Saha Denetim ve Teknik Gözlem Raporu",
        keywords: String = "mühendislik, şantiye, denetim, raporlama, fotorapor, md3"
    ): Boolean {
        if (!pdfFile.exists() || pdfFile.length() < 50) return false
        
        try {
            RandomAccessFile(pdfFile, "rw").use { raf ->
                val originalLength = raf.length()
                val oldStartXref = findOldStartXref(raf)
                
                raf.seek(originalLength)
                
                val cleanTitle = sanitizeLiteral(title)
                val cleanAuthor = sanitizeLiteral(author)
                val cleanCreator = sanitizeLiteral(creator)
                val cleanSubject = sanitizeLiteral(subject)
                val cleanKeywords = sanitizeLiteral(keywords)
                
                val infoObjectNumber = 9999
                val infoOffset = raf.filePointer
                
                val infoObjStr = buildString {
                    append("\n$infoObjectNumber 0 obj\n")
                    append("<<\n")
                    append("/Title ($cleanTitle)\n")
                    append("/Author ($cleanAuthor)\n")
                    append("/Creator ($cleanCreator)\n")
                    append("/Subject ($cleanSubject)\n")
                    append("/Keywords ($cleanKeywords)\n")
                    append(">>\n")
                    append("endobj\n")
                }
                raf.write(infoObjStr.toByteArray(StandardCharsets.US_ASCII))
                
                val xrefOffset = raf.filePointer
                val xrefStr = buildString {
                    append("xref\n")
                    append("$infoObjectNumber 1\n")
                    append(String.format(Locale.US, "%010d 00000 n \n", infoOffset))
                }
                raf.write(xrefStr.toByteArray(StandardCharsets.US_ASCII))
                
                val trailerStr = buildString {
                    append("trailer\n")
                    append("<<\n")
                    append("/Size ${infoObjectNumber + 1}\n")
                    if (oldStartXref != -1L) {
                        append("/Prev $oldStartXref\n")
                    }
                    append("/Info $infoObjectNumber 0 R\n")
                    append(">>\n")
                    append("startxref\n")
                    append("$xrefOffset\n")
                    append("%%EOF\n")
                }
                raf.write(trailerStr.toByteArray(StandardCharsets.US_ASCII))
            }
            Log.d(TAG, "PDF Metadata başarıyla Adobe Reader standardında gömüldü: ${pdfFile.name}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "PDF Metadata gömme hatası: ${e.message}", e)
            return false
        }
    }

    private fun sanitizeLiteral(input: String): String {
        return input.replace("(", "\\(").replace(")", "\\)").replace("\\", "\\\\").replace("\r", " ").replace("\n", " ")
    }

    private fun findOldStartXref(raf: RandomAccessFile): Long {
        try {
            val length = raf.length()
            val searchSize = minOf(1024L, length).toInt()
            val buffer = ByteArray(searchSize)
            raf.seek(length - searchSize)
            raf.readFully(buffer)
            
            val content = String(buffer, StandardCharsets.US_ASCII)
            val idx = content.lastIndexOf("startxref")
            if (idx != -1) {
                val sub = content.substring(idx + "startxref".length).trim()
                val lines = sub.split("\n", "\r")
                for (line in lines) {
                    val trimmed = line.trim()
                    if (trimmed.isNotEmpty() && trimmed.all { it.isDigit() }) {
                        return trimmed.toLong()
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Old startxref not found: ${e.message}")
        }
        return -1L
    }
}
