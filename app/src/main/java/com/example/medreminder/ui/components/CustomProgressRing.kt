package com.example.medreminder.ui.components

import android.R
import android.health.connect.datatypes.units.Percentage
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medreminder.ui.theme.PrimaryTeal

@Composable
fun CustomProgressRing(
    percentage: Int = 80,
    size : Dp = 140.dp,
    strokeWidth : Dp = 12.dp,
    primaryColor : Color = PrimaryTeal,
    backGroundColor : Color = Color(0xFFE0E0E0)
    ) {
    val sweepAngle = (percentage/100f) * 360f
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(size)
    ){
        Canvas(modifier = Modifier.fillMaxSize()){
            val strokePx = strokeWidth.toPx()
            drawArc(
                color = backGroundColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokePx)
            )
            drawArc(
                color = primaryColor,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokePx,cap = StrokeCap.Round)
            )
        }
        Text(text ="$percentage",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface)
    }
}
