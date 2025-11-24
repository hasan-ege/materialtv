package com.example.materialtv

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.materialtv.model.ContinueWatchingItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContinueWatchingRow(
    items: List<ContinueWatchingItem>,
    onItemClick: (ContinueWatchingItem) -> Unit,
    onPin: (ContinueWatchingItem) -> Unit,
    onRemove: (ContinueWatchingItem) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<ContinueWatchingItem?>(null) }

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text("Continue Watching", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items, key = { it.streamId }) { item ->
                var isPressed by remember { mutableStateOf(false) }
                val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, label = "")

                Column(
                    modifier = Modifier
                        .width(140.dp)
                        .scale(scale)
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    isPressed = event.type == PointerEventType.Press
                                }
                            }
                        }
                        .combinedClickable(
                            onClick = { onItemClick(item) },
                            onLongClick = {
                                selectedItem = item
                                showMenu = true
                            }
                        )
                ) {
                    Box {
                        AsyncImage(
                            model = item.streamIcon,
                            contentDescription = item.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(12.dp))
                                .shadow(elevation = 4.dp, shape = RoundedCornerShape(12.dp))
                        )
                        if (item.isPinned) {
                            Icon(
                                Icons.Default.PushPin,
                                contentDescription = "Pinned",
                                tint = Color.White,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            )
                        }
                        LinearProgressIndicator(
                            progress = { item.position.toFloat() / item.duration.toFloat() },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(4.dp)
                        )
                    }
                    Text(item.name, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text(if (selectedItem?.isPinned == true) "Unpin" else "Pin") },
                onClick = {
                    selectedItem?.let(onPin)
                    showMenu = false
                }
            )
            DropdownMenuItem(
                text = { Text("Remove") },
                onClick = {
                    selectedItem?.let(onRemove)
                    showMenu = false
                }
            )
        }
    }
}
