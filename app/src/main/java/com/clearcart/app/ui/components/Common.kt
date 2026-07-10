package com.clearcart.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clearcart.app.data.model.ConfidenceLevel
import com.clearcart.app.data.model.ProductScore
import com.clearcart.app.domain.scoring.display

@Composable
fun SectionCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
fun ScoreHeader(score: ProductScore) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            ScoreMetric(
                label = "ClearCart Score",
                value = score.overallScore.toString(),
                detail = score.scoreLabel.label,
            )
            ScoreMetric(
                label = "Personal Fit",
                value = score.personalFitScore.toString(),
                detail = score.personalFitLabel.label,
                alignEnd = true,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Confidence", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Column(horizontalAlignment = Alignment.End) {
                Text("${score.confidenceScore}/100", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                ConfidenceBadge(score.confidenceLevel)
            }
        }
    }
}

@Composable
private fun ScoreMetric(label: String, value: String, detail: String, alignEnd: Boolean = false) {
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Text(detail, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun ConfidenceBadge(level: ConfidenceLevel) {
    Text(
        text = level.display(),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelLarge,
    )
}
