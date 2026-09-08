package com.stremio.mobile.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.sp
import com.stremio.mobile.core.theme.ScreenGutter

@Composable
fun BoardHeader(
    onOpenSearch: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 11.dp, top = 6.dp, end = ScreenGutter),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StremioMark(
            modifier = Modifier
                .padding(start = 5.dp)
                .size(44.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "STREMIO",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Normal,
        )
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(if (LocalIsTv.current) 56.dp else 44.dp)
                .tvClickable(shape = RoundedCornerShape(14.dp), onClick = onOpenSearch),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = Color.White,
                modifier = Modifier.size(if (LocalIsTv.current) 34.dp else 28.dp),
            )
        }
    }
}
