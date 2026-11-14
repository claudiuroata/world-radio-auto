package com.worldradio.auto.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.worldradio.auto.R
import com.worldradio.auto.data.model.Station
import com.worldradio.auto.ui.theme.DarkCardBorder
import com.worldradio.auto.ui.theme.DarkCardElevated
import com.worldradio.auto.ui.theme.DeepObsidian
import com.worldradio.auto.ui.theme.ElectricEmerald
import com.worldradio.auto.ui.theme.LiveRed
import com.worldradio.auto.ui.theme.StarGold
import com.worldradio.auto.ui.theme.TextHighEmphasis
import com.worldradio.auto.ui.theme.TextMediumEmphasis

@Composable
fun MiniPlayer(
    station: Station?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    isShuffleEnabled: Boolean,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    onPlayPauseClick: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onShuffleToggle: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onHideToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (station == null) return

    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulseAlpha"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .shadow(12.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black, spotColor = ElectricEmerald.copy(alpha = 0.15f))
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, DarkCardBorder, RoundedCornerShape(20.dp)),
        color = DarkCardElevated
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Station artwork thumbnail with subtle inner elevation
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                if (!station.favicon.isNullOrBlank()) {
                    AsyncImage(
                        model = station.favicon,
                        contentDescription = station.name,
                        modifier = Modifier.size(44.dp),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Radio,
                        contentDescription = null,
                        tint = ElectricEmerald,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Title and Live Station Indicator
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = TextHighEmphasis,
                        letterSpacing = (-0.2).sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Pulsing Live Indicator
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isPlaying) LiveRed.copy(alpha = pulseAlpha) else Color.Gray)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (isBuffering) "Buffering stream..." else (station.country ?: "ONLINE RADIO"),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isBuffering) ElectricEmerald else TextMediumEmphasis,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Tactile Favorite Star Button
            IconButton(
                onClick = onFavoriteToggle,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    painter = painterResource(if (station.isFavorite) R.drawable.ic_star_on else R.drawable.ic_star_off),
                    contentDescription = "Favorite",
                    tint = if (station.isFavorite) StarGold else TextMediumEmphasis,
                    modifier = Modifier.size(19.dp)
                )
            }

            // Shuffle Mode Toggle Button
            IconButton(
                onClick = onShuffleToggle,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    painter = painterResource(if (isShuffleEnabled) R.drawable.ic_shuffle_on else R.drawable.ic_shuffle_off),
                    contentDescription = if (isShuffleEnabled) "Shuffle: On" else "Shuffle: Off",
                    tint = if (isShuffleEnabled) ElectricEmerald else TextMediumEmphasis.copy(alpha = 0.45f),
                    modifier = Modifier.size(19.dp)
                )
            }

            // Hide / Dislike Station Button
            IconButton(
                onClick = onHideToggle,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    painter = painterResource(if (station.isHidden) R.drawable.ic_hide_on else R.drawable.ic_hide_off),
                    contentDescription = if (station.isHidden) "Unhide station" else "Hide station",
                    tint = if (station.isHidden) LiveRed else TextMediumEmphasis.copy(alpha = 0.65f),
                    modifier = Modifier.size(19.dp)
                )
            }

            // Previous Station Button
            IconButton(
                onClick = onSkipPrevious,
                enabled = canSkipPrevious,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous Station",
                    tint = if (canSkipPrevious) TextHighEmphasis else TextMediumEmphasis.copy(alpha = 0.3f),
                    modifier = Modifier.size(21.dp)
                )
            }

            Spacer(modifier = Modifier.width(2.dp))

            // Glowing Floating Play/Pause/Buffer Button
            IconButton(
                onClick = onPlayPauseClick,
                modifier = Modifier
                    .size(40.dp)
                    .shadow(6.dp, CircleShape, spotColor = ElectricEmerald)
                    .clip(CircleShape)
                    .background(ElectricEmerald)
            ) {
                if (isBuffering) {
                    CircularProgressIndicator(
                        color = DeepObsidian,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = DeepObsidian,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(2.dp))

            // Next Station Button
            IconButton(
                onClick = onSkipNext,
                enabled = canSkipNext,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next Station",
                    tint = if (canSkipNext) TextHighEmphasis else TextMediumEmphasis.copy(alpha = 0.3f),
                    modifier = Modifier.size(21.dp)
                )
            }
        }
    }
}
