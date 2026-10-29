package com.studypulse.pro.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Liquid(content: @Composable BoxScope.() -> Unit) {
    val t = rememberInfiniteTransition(label = "bg")
    val a = t.animateFloat(0f, 1f, infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Reverse), label = "a")
    Box(
        Modifier.fillMaxSize().background(Black).drawBehind {
            val v = a.value
            val w = size.width
            val h = size.height
            drawRect(
                Brush.radialGradient(
                    listOf(Yellow.copy(alpha = .20f), Color.Transparent),
                    center = Offset(w * (.15f + .6f * v), h * (.12f + .25f * v)), radius = maxOf(w, h) * .55f
                )
            )
            drawRect(
                Brush.radialGradient(
                    listOf(Color(0xFFFF9100).copy(alpha = .12f), Color.Transparent),
                    center = Offset(w * (.9f - .6f * v), h * (.85f - .3f * v)), radius = maxOf(w, h) * .5f
                )
            )
        },
        content = content
    )
}

@Composable
fun Glass(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier
            .clip(shape)
            .background(Brush.linearGradient(listOf(GlassC.copy(alpha = .78f), GlassC.copy(alpha = .50f))))
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = .09f), Color.Transparent), endY = 140f))
            .border(
                1.dp,
                Brush.linearGradient(listOf(Color.White.copy(alpha = .38f), Color.White.copy(alpha = .05f), Color.White.copy(alpha = .20f))),
                shape
            ),
        content = content
    )
}

@Composable
fun Chip(text: String, hot: Boolean) {
    Glass(shape = RoundedCornerShape(50)) {
        Text(
            text, Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            color = if (hot) Yellow else TextS, fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp
        )
    }
}

@Composable
fun Ring(progress: Float, modifier: Modifier, stroke: Dp = 14.dp, content: @Composable BoxScope.() -> Unit) {
    val p by animateFloatAsState(progress, tween(900, easing = LinearEasing), label = "ring")
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(stroke)) {
            val st = stroke.toPx()
            drawArc(Color.White.copy(alpha = .08f), 0f, 360f, false, style = Stroke(st))
            drawArc(Yellow.copy(alpha = .22f), -90f, 360f * p, false, style = Stroke(st * 1.9f, cap = StrokeCap.Round))
            drawArc(Yellow, -90f, 360f * p, false, style = Stroke(st, cap = StrokeCap.Round))
        }
        content()
    }
}

@Composable
fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextP, unfocusedTextColor = TextP,
    focusedBorderColor = Yellow, unfocusedBorderColor = Color.White.copy(alpha = .22f),
    cursorColor = Yellow, focusedLabelColor = Yellow, unfocusedLabelColor = TextS
)

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) =
    Button(
        onClick, modifier, shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(containerColor = Yellow, contentColor = Black)
    ) { Text(text, fontWeight = FontWeight.SemiBold) }
