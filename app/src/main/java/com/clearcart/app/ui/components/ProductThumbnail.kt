package com.clearcart.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.clearcart.app.data.model.Product
import com.clearcart.app.data.model.ProductType

@Composable
fun ProductThumbnail(
    product: Product,
    modifier: Modifier = Modifier,
    size: Dp = 76.dp,
) {
    val shape = RoundedCornerShape(12.dp)
    val visual = product.thumbnailVisual()
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(visual.background),
        contentAlignment = Alignment.Center,
    ) {
        if (product.imageUrl.isNullOrBlank()) {
            Icon(
                imageVector = visual.icon,
                contentDescription = null,
                tint = visual.tint,
                modifier = Modifier.size(size * 0.46f),
            )
        } else {
            AsyncImage(
                model = product.imageUrl,
                contentDescription = product.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
fun CategoryThumbnail(
    category: String,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
) {
    val shape = RoundedCornerShape(12.dp)
    val visual = thumbnailVisualFor(category)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(visual.background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = visual.icon,
            contentDescription = null,
            tint = visual.tint,
            modifier = Modifier.size(size * 0.46f),
        )
    }
}

private data class ThumbnailVisual(
    val icon: ImageVector,
    val background: Color,
    val tint: Color,
)

@Composable
private fun Product.thumbnailVisual(): ThumbnailVisual {
    val text = listOf(name, brand, category, productType.name).joinToString(" ").lowercase()
    return thumbnailVisualFor(text, productType)
}

@Composable
private fun thumbnailVisualFor(
    text: String,
    productType: ProductType = ProductType.Unknown,
): ThumbnailVisual {
    val neutral = ThumbnailVisual(
        icon = Icons.Default.ShoppingBag,
        background = MaterialTheme.colorScheme.surfaceVariant,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    return when {
        text.contains("protein") || text.contains("shake") -> ThumbnailVisual(
            icon = Icons.Default.LocalDrink,
            background = Color(0xFFE6F3EE),
            tint = Color(0xFF24795D),
        )
        text.contains("cereal") || text.contains("oat") || text.contains("crunch") -> ThumbnailVisual(
            icon = Icons.Default.BakeryDining,
            background = Color(0xFFFFF2D7),
            tint = Color(0xFF9A5F00),
        )
        text.contains("soda") || text.contains("sparkling") || text.contains("beverage") -> ThumbnailVisual(
            icon = Icons.Default.WaterDrop,
            background = Color(0xFFE6F1FF),
            tint = Color(0xFF21629D),
        )
        text.contains("shampoo") || text.contains("body wash") || productType == ProductType.Cosmetic -> ThumbnailVisual(
            icon = Icons.Default.Spa,
            background = Color(0xFFF0E8F8),
            tint = Color(0xFF70508E),
        )
        text.contains("household") -> ThumbnailVisual(
            icon = Icons.Default.LocalFlorist,
            background = Color(0xFFEAF3E2),
            tint = Color(0xFF527A35),
        )
        else -> neutral
    }
}
