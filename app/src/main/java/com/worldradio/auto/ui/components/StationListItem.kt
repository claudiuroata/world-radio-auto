package com.worldradio.auto.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.worldradio.auto.data.model.Station
import com.worldradio.auto.ui.theme.DarkCardBorder
import com.worldradio.auto.ui.theme.DarkCardElevated
import com.worldradio.auto.ui.theme.DarkCardHighlight
import com.worldradio.auto.ui.theme.DarkCardSurface
import com.worldradio.auto.ui.theme.ElectricEmerald
import com.worldradio.auto.ui.theme.LiveRed
import com.worldradio.auto.ui.theme.StarGold
import com.worldradio.auto.ui.theme.TextHighEmphasis
import com.worldradio.auto.ui.theme.TextLowEmphasis
import com.worldradio.auto.ui.theme.TextMediumEmphasis

@Composable
fun StationListItem(
    station: Station,
    isPlayingThisStation: Boolean,
    onStationClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onHideToggle: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 5.dp)
            .shadow(
                elevation = if (isPlayingThisStation) 6.dp else 2.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = if (isPlayingThisStation) ElectricEmerald.copy(alpha = 0.2f) else Color.Black
            )
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                color = if (isPlayingThisStation) ElectricEmerald.copy(alpha = 0.7f) else DarkCardBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onStationClick),
        color = if (isPlayingThisStation) DarkCardHighlight else DarkCardSurface,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Modern Station Artwork with subtle drop shadow
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(DarkCardElevated),
                contentAlignment = Alignment.Center
            ) {
                if (!station.favicon.isNullOrBlank()) {
                    AsyncImage(
                        model = station.favicon,
                        contentDescription = station.name,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(13.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Radio,
                        contentDescription = null,
                        tint = if (isPlayingThisStation) ElectricEmerald else TextMediumEmphasis,
                        modifier = Modifier.size(28.dp)
                    )
                }

                if (isPlayingThisStation) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedWaveformIndicator()
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Station Metadata Details
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 6.dp)
            ) {
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (isPlayingThisStation) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isPlayingThisStation) ElectricEmerald else TextHighEmphasis,
                        fontSize = 15.5.sp,
                        letterSpacing = (-0.2).sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                val subtitle = buildString {
                    if (!station.country.isNullOrBlank()) append(station.country)
                    if (station.tags.isNotEmpty()) {
                        if (isNotEmpty()) append(" • ")
                        append(station.tags.take(2).joinToString(", "))
                    }
                }

                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextMediumEmphasis,
                            fontSize = 12.5.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(5.dp))

                // Modern Pill Badges
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (station.clickCount > 0) {
                        ModernPillBadge(text = "${station.clickCount} clicks")
                    } else if (station.votes > 0) {
                        ModernPillBadge(text = "★ ${station.votes}")
                    }
                    if (station.bitrate > 0) {
                        ModernPillBadge(text = "${station.bitrate} kbps")
                    }
                    if (!station.codec.isNullOrBlank()) {
                        ModernPillBadge(text = station.codec.uppercase())
                    }
                }
            }

            // Tactile Favorite Star Button
            IconButton(
                onClick = onFavoriteToggle,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = if (station.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (station.isFavorite) "Remove favorite" else "Add favorite",
                    tint = if (station.isFavorite) StarGold else TextLowEmphasis,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Hide / Unhide Station Button
            IconButton(
                onClick = onHideToggle,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = if (station.isHidden) Icons.Filled.VisibilityOff else Icons.Outlined.VisibilityOff,
                    contentDescription = if (station.isHidden) "Unhide station" else "Hide station",
                    tint = if (station.isHidden) LiveRed else TextLowEmphasis,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun ModernPillBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(DarkCardElevated)
            .border(0.5.dp, DarkCardBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = TextMediumEmphasis,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AnimatedWaveformIndicator() {
    val transition = rememberInfiniteTransition(label = "waveform")
    val h1 by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h1"
    )
    val h2 by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(560, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h2"
    )
    val h3 by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(360, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h3"
    )
    val h4 by transition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(490, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h4"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.height(20.dp)
    ) {
        Box(modifier = Modifier.width(3.dp).height(20.dp * h1).clip(CircleShape).background(ElectricEmerald))
        Box(modifier = Modifier.width(3.dp).height(20.dp * h2).clip(CircleShape).background(ElectricEmerald))
        Box(modifier = Modifier.width(3.dp).height(20.dp * h3).clip(CircleShape).background(ElectricEmerald))
        Box(modifier = Modifier.width(3.dp).height(20.dp * h4).clip(CircleShape).background(ElectricEmerald))
    }
}
