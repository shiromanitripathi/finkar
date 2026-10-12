package com.spendtrack.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendtrack.data.Categories
import com.spendtrack.data.Fmt
import com.spendtrack.data.UiTx

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    val m = if (onClick != null) modifier.clip(shape).clickable(onClick = onClick) else modifier
    Card(
        modifier = m,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun EmojiBadge(emoji: String, color: Color, size: Dp = 44.dp) {
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(color.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        Text(emoji, fontSize = (size.value * 0.46f).sp)
    }
}

@Composable
fun SectionTitle(text: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium)
        if (actionLabel != null) TextButton(onClick = onAction) { Text(actionLabel) }
    }
}

@Composable
fun EmptyState(emoji: String, title: String, message: String) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 28.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 44.sp)
        Spacer(Modifier.height(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AmountField(
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Amount"
) {
    OutlinedTextField(
        value = value,
        onValueChange = { v ->
            if (v.all { it.isDigit() || it == '.' } && v.count { it == '.' } <= 1 && v.length <= 10) {
                onChange(v)
            }
        },
        label = { Text(label) },
        prefix = { Text("₹ ") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun progressColor(fraction: Float): Color = when {
    fraction >= 1f -> MaterialTheme.colorScheme.error
    fraction >= 0.8f -> Amber
    else -> MaterialTheme.colorScheme.primary
}

@Composable
fun TxRow(item: UiTx, onClick: () -> Unit) {
    val info = Categories.info(item.tx.category)
    val income = item.tx.isIncome
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EmojiBadge(info.emoji, Color(info.color))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.tx.merchant,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                Fmt.dateTime(item.tx.timestamp) + " · " + item.tx.category,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                (if (income) "+" else "") + Fmt.money(item.tx.amount),
                style = MaterialTheme.typography.titleSmall,
                color = if (income) Mint else MaterialTheme.colorScheme.onSurface
            )
            if (item.owed > 0.0) {
                Text(
                    "friends owe " + Fmt.money(item.owed),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/** Donut chart; [center] is drawn in the hole. */
@Composable
fun DonutChart(
    slices: List<Pair<Color, Double>>,
    modifier: Modifier = Modifier,
    thickness: Dp = 26.dp,
    center: @Composable () -> Unit = {}
) {
    val total = slices.sumOf { it.second }
    val track = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = thickness.toPx()
            val topLeft = Offset(stroke / 2, stroke / 2)
            val arcSize = Size(size.width - stroke, size.height - stroke)
            if (total <= 0.0) {
                drawArc(
                    color = track, startAngle = 0f, sweepAngle = 360f, useCenter = false,
                    topLeft = topLeft, size = arcSize, style = Stroke(stroke)
                )
            } else {
                var start = -90f
                slices.forEach { (color, value) ->
                    val sweep = (value / total * 360.0).toFloat()
                    val gap = if (slices.size > 1) 2f else 0f
                    drawArc(
                        color = color,
                        startAngle = start,
                        sweepAngle = (sweep - gap).coerceAtLeast(0.5f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Butt)
                    )
                    start += sweep
                }
            }
        }
        center()
    }
}

/** Simple animated bar chart; the last bar is highlighted. */
@Composable
fun BarChart(
    values: List<Double>,
    labels: List<String>,
    color: Color,
    modifier: Modifier = Modifier.fillMaxWidth().height(150.dp)
) {
    val max = (values.maxOrNull() ?: 0.0).coerceAtLeast(1.0)
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        values.forEachIndexed { i, v ->
            val target = if (v <= 0.0) 0.03f else (v / max).toFloat().coerceIn(0.05f, 1f)
            val frac by animateFloatAsState(target, tween(700), label = "bar")
            Column(
                Modifier.weight(1f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        Modifier
                            .fillMaxWidth(0.7f)
                            .fillMaxHeight(frac)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (i == values.lastIndex) color else color.copy(alpha = 0.4f))
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    labels.getOrElse(i) { "" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (i == values.lastIndex) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
