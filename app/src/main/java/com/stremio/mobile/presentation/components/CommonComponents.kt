package com.stremio.mobile.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stremio.mobile.core.theme.AccentPurple
import com.stremio.mobile.core.theme.MutedText

@Composable
fun StremioMark(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = com.stremio.mobile.R.drawable.ic_stremio_splash_logo),
        contentDescription = null,
        modifier = modifier,
    )
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(start = contentGutter(), end = 12.dp),
        color = Color.White,
        fontSize = 22.sp,
        lineHeight = 27.sp,
        fontWeight = FontWeight.ExtraBold,
    )
}

@Composable
fun EmptyState(message: String) {
    Text(
        text = message,
        modifier = Modifier.padding(horizontal = contentGutter()),
        color = MutedText,
        fontSize = 15.sp,
        lineHeight = 21.sp,
    )
}

@Composable
fun LoadingRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(28.dp),
            color = AccentPurple,
        )
    }
}

@Composable
fun GlassPill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0x3DFFFFFF))
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}
