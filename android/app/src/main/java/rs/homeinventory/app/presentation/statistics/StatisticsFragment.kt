package rs.homeinventory.app.presentation.statistics

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.android.material.color.MaterialColors
import com.google.android.material.R as MaterialR
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import rs.homeinventory.app.R
import rs.homeinventory.app.databinding.FragmentStatisticsBinding
import rs.homeinventory.app.databinding.ItemStatisticsCategoryRowBinding
import rs.homeinventory.app.databinding.ItemStatisticsUnconvertedRowBinding
import rs.homeinventory.app.databinding.ItemStatisticsWarrantyRowBinding
import rs.homeinventory.app.util.UiState
import rs.homeinventory.app.util.chartCategoryColors
import rs.homeinventory.app.util.warrantyStatusColorRes
import rs.homeinventory.app.util.warrantyStatusLabelRes

// SCR-08 — cita iskljucivo iz Room-a (FR-078); sva cetiri stanja iz BR-017. Error je odbrambeno
// stanje (Room citanje/obrada normalno ne baca) dodato u tiketu 27, ne redovan put kao kod mreznih ekrana.
@AndroidEntryPoint
class StatisticsFragment : Fragment(R.layout.fragment_statistics) {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StatisticsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentStatisticsBinding.bind(view)

        binding.buttonErrorRetry.setOnClickListener { viewModel.retry() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect(::render) }
            }
        }
    }

    private fun render(state: UiState<StatisticsUi>) {
        binding.progressLoading.isVisible = state is UiState.Loading
        binding.scrollContent.isVisible = state is UiState.Success
        binding.groupEmpty.isVisible = state is UiState.Empty
        binding.groupError.isVisible = state is UiState.Error

        when (state) {
            is UiState.Success -> renderContent(state.data)
            is UiState.Error -> binding.textErrorMessage.text = state.message
            UiState.Loading, UiState.Empty -> Unit
        }
    }

    private fun renderContent(data: StatisticsUi) {
        binding.textTotalItemsValue.text = data.totalItemCount.toString()
        binding.textTotalValue.text = data.totalValueFormatted
        binding.textCategoryCountValue.text = data.categoryCount.toString()
        binding.textAverageValue.text = data.averageValueFormatted

        renderUnconverted(data.unconvertedAmounts)
        renderPieChart(data.categoryStats)
        renderBarChart(data.categoryStats)
        renderCategoryTable(data.categoryStats)

        binding.textMostExpensiveItem.text = data.mostExpensiveItem?.let {
            getString(R.string.statistics_most_expensive_item, it.name, it.categoryName, it.valueFormatted)
        } ?: getString(R.string.statistics_most_expensive_none)

        renderWarrantyBreakdown(data.warrantyBreakdown)
    }

    // BR-013 — svaka valuta bez dostupnog kursa dobija svoj red, umesto da se tiho izostavi ili
    // pogresno sabere kao da vredi isto (kurs 1.0).
    private fun renderUnconverted(unconverted: List<UnconvertedAmountUi>) {
        binding.containerUnconverted.isVisible = unconverted.isNotEmpty()
        binding.containerUnconverted.removeAllViews()
        unconverted.forEach { amount ->
            val row = ItemStatisticsUnconvertedRowBinding.inflate(
                LayoutInflater.from(requireContext()), binding.containerUnconverted, false
            )
            row.textUnconvertedRow.text =
                getString(R.string.statistics_unconverted_row, amount.currency, amount.amountFormatted)
            binding.containerUnconverted.addView(row.root)
        }
    }

    // FR-074 — pie chart raspodele vrednosti po kategorijama; boje deljene sa bar chart-om ispod
    // preko iste palete (chartCategoryColors), da ista kategorija bude prepoznatljiva na oba grafikona.
    private fun renderPieChart(stats: List<CategoryStatUi>) {
        val chart: PieChart = binding.chartCategoryValue
        val onSurfaceColor = themeOnSurfaceColor()
        val colors = chartCategoryColors(requireContext(), stats.size)

        // Kategorija bez vrednosti (npr. svi predmeti bez cene) nema sta da prikaze na pie chart-u —
        // nulta isecka i dalje iscrtava labelu koja se preklapa sa susednom (MPAndroidChart ogranicenje).
        // Indeksi se cuvaju da boja ostane ista kao na bar chart-u ispod i za kategorije koje ovde nisu prikazane.
        val visibleIndices = stats.indices.filter { stats[it].valueMinor > 0 }
        val entries = visibleIndices.map { PieEntry(stats[it].valueMinor / 100f, stats[it].categoryName) }
        val dataSet = PieDataSet(entries, "").apply {
            setColors(visibleIndices.map { colors[it] })
            sliceSpace = 2f
            setValueTextColor(onSurfaceColor)
            valueTextSize = 12f
        }
        val data = PieData(dataSet).apply {
            setValueFormatter(PercentFormatter(chart))
            setValueTextColor(onSurfaceColor)
        }

        chart.data = data
        chart.description.isEnabled = false
        chart.setDrawEntryLabels(false)
        chart.setUsePercentValues(true)
        chart.setHoleColor(Color.TRANSPARENT)
        // Do 11 podrazumevanih kategorija (FR-041) ne staje u jedan red — bez wrap-a legenda se
        // secala na pola i ostatak kategorija ostajao bez vidljivog imena/boje.
        chart.legend.apply {
            textColor = onSurfaceColor
            isWordWrapEnabled = true
            textSize = 11f
            formSize = 10f
        }
        chart.setExtraOffsets(4f, 4f, 4f, 4f)
        chart.invalidate()
    }

    // FR-075 — bar chart broja predmeta po kategorijama, isti redosled/boje kao pie chart iznad.
    private fun renderBarChart(stats: List<CategoryStatUi>) {
        val chart: BarChart = binding.chartCategoryCount
        val onSurfaceColor = themeOnSurfaceColor()
        val colors = chartCategoryColors(requireContext(), stats.size)

        val entries = stats.mapIndexed { index, stat -> BarEntry(index.toFloat(), stat.itemCount.toFloat()) }
        val dataSet = BarDataSet(entries, getString(R.string.statistics_count_by_category_title)).apply {
            setColors(colors)
            setValueTextColor(onSurfaceColor)
        }
        val data = BarData(dataSet).apply { setValueTextColor(onSurfaceColor) }

        chart.data = data
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.axisRight.isEnabled = false
        chart.axisLeft.textColor = onSurfaceColor
        chart.axisLeft.axisMinimum = 0f
        chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            textColor = onSurfaceColor
            granularity = 1f
            labelRotationAngle = -45f
            setDrawGridLines(false)
            valueFormatter = IndexAxisValueFormatter(stats.map { it.categoryName })
        }
        chart.setFitBars(true)
        chart.invalidate()
    }

    private fun renderCategoryTable(stats: List<CategoryStatUi>) {
        binding.containerCategoryTable.removeAllViews()
        stats.forEach { stat ->
            val row = ItemStatisticsCategoryRowBinding.inflate(
                LayoutInflater.from(requireContext()), binding.containerCategoryTable, false
            )
            row.textCategoryName.text = stat.categoryName
            row.textCategoryCount.text = stat.itemCount.toString()
            row.textCategoryValue.text = stat.valueFormatted
            binding.containerCategoryTable.addView(row.root)
        }
    }

    // BR-010 — uvek sve cetiri grane statusa, i sa brojem 0.
    private fun renderWarrantyBreakdown(breakdown: List<WarrantyBreakdownUi>) {
        binding.containerWarrantyBreakdown.removeAllViews()
        breakdown.forEach { entry ->
            val row = ItemStatisticsWarrantyRowBinding.inflate(
                LayoutInflater.from(requireContext()), binding.containerWarrantyBreakdown, false
            )
            row.textWarrantyStatusLabel.text = getString(warrantyStatusLabelRes(entry.status))
            row.textWarrantyStatusCount.text = entry.itemCount.toString()
            row.dotWarrantyStatus.background.mutate().setTint(
                ContextCompat.getColor(requireContext(), warrantyStatusColorRes(entry.status))
            )
            binding.containerWarrantyBreakdown.addView(row.root)
        }
    }

    private fun themeOnSurfaceColor(): Int =
        MaterialColors.getColor(binding.root, MaterialR.attr.colorOnSurface)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
