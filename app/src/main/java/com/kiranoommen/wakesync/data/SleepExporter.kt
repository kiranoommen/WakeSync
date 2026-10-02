package com.kiranoommen.wakesync.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.kiranoommen.wakesync.domain.NightAnalytics
import com.kiranoommen.wakesync.domain.PeriodAnalytics
import java.io.File
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object SleepExporter {

    fun shareCsv(
        context: Context,
        nights: List<NightAnalytics>,
        retain: Boolean = false
    ) {
        val file = exportFile(
            context = context,
            fileName = "wakesync-sleep.csv",
            retain = retain
        )
        val zone = ZoneId.systemDefault()
        val time = DateTimeFormatter.ofPattern("h:mm a")

        file.bufferedWriter().use { writer ->
            writer.appendLine(
                "Date,Total Sleep,Efficiency,Deep %,REM %,Light %,WASO,Latency,Bedtime,Wake Time,HRV ms,RHR bpm,Score"
            )

            nights.forEach { night ->
                val start = night.night.start.atZone(zone).format(time)
                val end = night.night.end.atZone(zone).format(time)

                writer.appendLine(
                    listOf(
                        night.date.toString(),
                        night.asleepMinutes.toString(),
                        (night.efficiencyPercent ?: "").toString(),
                        night.deepPercent.toString(),
                        night.remPercent.toString(),
                        night.lightPercent.toString(),
                        night.wasoMinutes.toString(),
                        (night.onsetLatencyMinutes ?: "").toString(),
                        start,
                        end,
                        (night.night.averageHrvMs ?: "").toString(),
                        (night.night.restingHeartRateBpm ?: "").toString(),
                        night.score.toString()
                    ).joinToString(",") { escapeCsv(it) }
                )
            }
        }

        shareFile(
            context = context,
            file = file,
            mimeType = "text/csv",
            chooserTitle = "Share WakeSync sleep CSV"
        )
    }

    fun sharePdf(
        context: Context,
        analytics: PeriodAnalytics,
        retain: Boolean = false
    ) {
        val file = exportFile(
            context = context,
            fileName = "wakesync-sleep-summary.pdf",
            retain = retain
        )
        val document = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val pageInfo = PdfDocument.PageInfo.Builder(612, 792, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        var y = 64f

        paint.textSize = 28f
        paint.isFakeBoldText = true
        canvas.drawText("WakeSync Sleep Summary", 42f, y, paint)

        y += 34f
        paint.textSize = 12f
        paint.isFakeBoldText = false
        canvas.drawText(
            "Generated locally from Health Connect data.",
            42f,
            y,
            paint
        )

        y += 42f
        paint.textSize = 18f
        paint.isFakeBoldText = true
        canvas.drawText(
            "Average score: " + (analytics.averageScore?.toString() ?: "—"),
            42f,
            y,
            paint
        )

        y += 28f
        paint.textSize = 13f
        paint.isFakeBoldText = false
        canvas.drawText(
            "Average sleep: " + formatMinutes(analytics.averageSleepMinutes),
            42f,
            y,
            paint
        )
        y += 20f
        canvas.drawText(
            "Average efficiency: " +
                (analytics.averageEfficiencyPercent?.toString()?.plus("%") ?: "—"),
            42f,
            y,
            paint
        )
        y += 20f
        canvas.drawText(
            "Sleep debt estimate: " + formatMinutes(analytics.sleepDebtMinutes),
            42f,
            y,
            paint
        )
        y += 20f
        canvas.drawText(
            "Sleep Regularity Index: " +
                (analytics.regularityScore?.toString()?.plus(" / 100") ?: "Learning"),
            42f,
            y,
            paint
        )

        y += 38f
        paint.textSize = 16f
        paint.isFakeBoldText = true
        canvas.drawText("Recent nights", 42f, y, paint)

        paint.textSize = 10.5f
        paint.isFakeBoldText = false

        analytics.nights.take(18).forEach { night ->
            y += 20f
            if (y > 740f) return@forEach

            val row =
                night.date.format(DateTimeFormatter.ofPattern("MMM d")) +
                    "   " +
                    formatMinutes(night.asleepMinutes) +
                    "   Eff " +
                    (night.efficiencyPercent?.toString()?.plus("%") ?: "—") +
                    "   Deep " + night.deepPercent + "%" +
                    "   REM " + night.remPercent + "%" +
                    "   Score " + night.score

            canvas.drawText(row, 42f, y, paint)
        }

        y += 34f
        paint.textSize = 9.5f
        canvas.drawText(
            "WakeSync metrics are wellness estimates from consumer wearable data, not clinical measurements.",
            42f,
            y.coerceAtMost(760f),
            paint
        )

        document.finishPage(page)

        file.outputStream().use { document.writeTo(it) }
        document.close()

        shareFile(
            context = context,
            file = file,
            mimeType = "application/pdf",
            chooserTitle = "Share WakeSync sleep summary"
        )
    }


    fun shareStoryCard(
        context: Context,
        analytics: PeriodAnalytics,
        retain: Boolean = false
    ) {
        val file = exportFile(
            context = context,
            fileName = "wakesync-story.png",
            retain = retain
        )

        val width = 1080
        val height = 1920
        val bitmap = Bitmap.createBitmap(
            width,
            height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.shader = LinearGradient(
            0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            intArrayOf(
                Color.rgb(6, 8, 18),
                Color.rgb(25, 18, 58),
                Color.rgb(5, 34, 45)
            ),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(
            0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            paint
        )
        paint.shader = null

        paint.color = Color.argb(
            44,
            124,
            58,
            237
        )
        canvas.drawCircle(
            170f,
            250f,
            330f,
            paint
        )

        paint.color = Color.argb(
            38,
            6,
            182,
            212
        )
        canvas.drawCircle(
            930f,
            1590f,
            380f,
            paint
        )

        paint.color = Color.WHITE
        paint.textSize = 70f
        paint.isFakeBoldText = true
        canvas.drawText(
            "WakeSync",
            90f,
            150f,
            paint
        )

        paint.textSize = 36f
        paint.isFakeBoldText = false
        paint.color = Color.rgb(
            148,
            163,
            184
        )
        canvas.drawText(
            "My recent sleep trend",
            90f,
            205f,
            paint
        )

        val dates = analytics.nights
            .map { it.date }
            .sorted()
        if (dates.isNotEmpty()) {
            paint.textSize = 26f
            paint.color = Color.rgb(
                148,
                163,
                184
            )
            val rangeLabel =
                dates.first().format(
                    DateTimeFormatter.ofPattern("MMM d")
                ) +
                    " – " +
                    dates.last().format(
                        DateTimeFormatter.ofPattern("MMM d")
                    )
            canvas.drawText(
                rangeLabel,
                90f,
                250f,
                paint
            )
        }

        val score = analytics.averageScore ?: 0
        paint.color = scoreColorForStory(score)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 34f
        paint.strokeCap = Paint.Cap.ROUND
        canvas.drawArc(
            220f,
            360f,
            860f,
            1000f,
            -90f,
            360f * score.coerceIn(0, 100) / 100f,
            false,
            paint
        )

        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        paint.textSize = 176f
        paint.isFakeBoldText = true
        val scoreText = score.toString()
        val scoreWidth = paint.measureText(scoreText)
        canvas.drawText(
            scoreText,
            width / 2f - scoreWidth / 2f,
            745f,
            paint
        )

        paint.textSize = 34f
        paint.isFakeBoldText = false
        paint.color = Color.rgb(
            148,
            163,
            184
        )
        val scoreLabel = "WakeSync Sleep Score"
        val scoreLabelWidth =
            paint.measureText(scoreLabel)
        canvas.drawText(
            scoreLabel,
            width / 2f - scoreLabelWidth / 2f,
            810f,
            paint
        )

        val cardPaint = Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            color = Color.argb(
                22,
                255,
                255,
                255
            )
        }

        val metrics = listOf(
            "AVG SLEEP" to formatMinutes(
                analytics.averageSleepMinutes
            ),
            "EFFICIENCY" to (
                analytics.averageEfficiencyPercent
                    ?.toString()
                    ?.plus("%")
                    ?: "—"
                ),
            "REGULARITY" to (
                analytics.regularityScore
                    ?.toString()
                    ?.plus("/100")
                    ?: "Learning"
                )
        )

        var left = 70f
        metrics.forEach { (label, value) ->
            canvas.drawRoundRect(
                left,
                1120f,
                left + 295f,
                1370f,
                44f,
                44f,
                cardPaint
            )

            paint.color = Color.WHITE
            paint.textSize = 48f
            paint.isFakeBoldText = true
            canvas.drawText(
                value,
                left + 28f,
                1240f,
                paint
            )

            paint.color = Color.rgb(
                148,
                163,
                184
            )
            paint.textSize = 23f
            paint.isFakeBoldText = true
            canvas.drawText(
                label,
                left + 28f,
                1300f,
                paint
            )

            left += 325f
        }

        paint.color = Color.WHITE
        paint.textSize = 42f
        paint.isFakeBoldText = true
        canvas.drawText(
            "Better sleep. Brighter mornings.",
            90f,
            1570f,
            paint
        )

        paint.color = Color.rgb(
            148,
            163,
            184
        )
        paint.textSize = 28f
        paint.isFakeBoldText = false
        canvas.drawText(
            "Generated locally from my selected sleep period.",
            90f,
            1620f,
            paint
        )
        canvas.drawText(
            "No location or personal identifiers included.",
            90f,
            1660f,
            paint
        )

        file.outputStream().use {
            bitmap.compress(
                Bitmap.CompressFormat.PNG,
                100,
                it
            )
        }
        bitmap.recycle()

        shareFile(
            context = context,
            file = file,
            mimeType = "image/png",
            chooserTitle =
                "Share WakeSync story card"
        )
    }


    fun clearGeneratedExports(
        context: Context
    ) {
        File(
            context.filesDir,
            "exports"
        ).deleteRecursively()

        listOf(
            "wakesync-sleep.csv",
            "wakesync-sleep-summary.pdf",
            "wakesync-story.png"
        ).forEach { name ->
            File(
                context.cacheDir,
                name
            ).delete()
        }
    }

    private fun exportFile(
        context: Context,
        fileName: String,
        retain: Boolean
    ): File {
        return if (retain) {
            val directory = File(
                context.filesDir,
                "exports"
            ).apply {
                mkdirs()
            }

            val dot = fileName.lastIndexOf('.')
            val base =
                if (dot > 0) {
                    fileName.substring(
                        0,
                        dot
                    )
                } else {
                    fileName
                }
            val extension =
                if (dot > 0) {
                    fileName.substring(dot)
                } else {
                    ""
                }

            File(
                directory,
                base +
                    "-" +
                    System.currentTimeMillis() +
                    extension
            )
        } else {
            File(
                context.cacheDir,
                fileName
            )
        }
    }

    private fun scoreColorForStory(
        score: Int
    ): Int =
        when {
            score >= 90 ->
                Color.rgb(16, 185, 129)
            score >= 75 ->
                Color.rgb(6, 182, 212)
            score >= 60 ->
                Color.rgb(245, 158, 11)
            else ->
                Color.rgb(239, 68, 68)
        }

    private fun shareFile(
        context: Context,
        file: File,
        mimeType: String,
        chooserTitle: String
    ) {
        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".files",
            file
        )

        val share = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(share, chooserTitle))
    }

    private fun escapeCsv(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"" + escaped + "\""
    }

    private fun formatMinutes(minutes: Long): String {
        val hours = minutes / 60
        val remainder = minutes % 60
        return if (hours > 0) {
            hours.toString() + "h " + remainder.toString() + "m"
        } else {
            remainder.toString() + "m"
        }
    }
}
