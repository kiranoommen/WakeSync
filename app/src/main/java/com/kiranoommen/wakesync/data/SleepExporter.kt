package com.kiranoommen.wakesync.data

import android.content.Context
import android.content.Intent
import android.graphics.Paint
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
        nights: List<NightAnalytics>
    ) {
        val file = File(context.cacheDir, "wakesync-sleep.csv")
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
        analytics: PeriodAnalytics
    ) {
        val file = File(context.cacheDir, "wakesync-sleep-summary.pdf")
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
