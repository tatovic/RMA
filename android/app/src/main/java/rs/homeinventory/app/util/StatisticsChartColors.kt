package rs.homeinventory.app.util

import android.content.Context
import androidx.core.content.ContextCompat
import rs.homeinventory.app.R

// FR-074/FR-075 — jedna paleta deljena izmedju pie i bar chart-a na SCR-08, da ista kategorija ima
// istu boju na oba grafikona. Ciklicno se ponavlja ako ima vise kategorija nego boja u paleti.
private val CHART_CATEGORY_COLOR_RES = intArrayOf(
    R.color.chart_category_1, R.color.chart_category_2, R.color.chart_category_3,
    R.color.chart_category_4, R.color.chart_category_5, R.color.chart_category_6,
    R.color.chart_category_7, R.color.chart_category_8, R.color.chart_category_9,
    R.color.chart_category_10, R.color.chart_category_11
)

fun chartCategoryColors(context: Context, count: Int): List<Int> =
    (0 until count).map { ContextCompat.getColor(context, CHART_CATEGORY_COLOR_RES[it % CHART_CATEGORY_COLOR_RES.size]) }
