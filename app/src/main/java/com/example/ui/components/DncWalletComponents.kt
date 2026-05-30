package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun GlowCard(
    modifier: Modifier = Modifier,
    borderColor: Color = TechCyan.copy(alpha = 0.35f),
    content: @Composable ColumnScope.() -> Unit
) {
    // Beautiful glass-designed container with gradient brush border
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0x1F22D3EE),  // Soft ambient cyan top reflection
                        CardSlate           // Solid obsidian base
                    )
                )
            )
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
            .padding(20.dp),
        content = content
    )
}

@Composable
fun StatusIndicator(
    label: String,
    isActive: Boolean,
    activeColor: Color = DncGreen,
    inactiveColor: Color = Color(0xFF334155),
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(if (isActive) activeColor else inactiveColor)
                .border(2.dp, if (isActive) activeColor.copy(alpha = 0.5f) else Color.Transparent, RoundedCornerShape(5.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (isActive) TextLight else SubtleText,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun TerminalConsole(
    title: String,
    logs: List<String>,
    modifier: Modifier = Modifier,
    maxHeightDp: Int = 180
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF070A0F))
            .border(1.dp, BorderSlate, RoundedCornerShape(16.dp))
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F1319))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "⚡ $title",
                color = TechCyan,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFEF4444)))
                Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFF59E0B)))
                Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF10B981)))
            }
        }
        
        // Terminal text
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeightDp.dp)
                .padding(14.dp)
        ) {
            if (logs.isEmpty()) {
                Text(
                    text = "No diagnostics available. System idle.",
                    color = SubtleText,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(logs) { log ->
                        val isAlert = log.contains("⚠️") || log.contains("Warning")
                        val isSuccess = log.contains("🎉") || log.contains("✓")
                        val textColor = when {
                            isAlert -> AlertAmber
                            isSuccess -> DncGreen
                            else -> TextLight
                        }
                        Text(
                            text = log,
                            color = textColor,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
