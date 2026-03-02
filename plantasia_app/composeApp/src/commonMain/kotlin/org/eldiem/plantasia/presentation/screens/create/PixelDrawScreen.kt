package org.eldiem.plantasia.presentation.screens.create

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val CANVAS_SIZE = 240
private const val TRANSPARENT = 0x00000000

enum class DrawTool { Pen, Eraser, Fill }

private val presetColors = listOf(
    Color.Black, Color.White, Color.Red, Color(0xFFFF6600),
    Color.Yellow, Color(0xFF00CC00), Color(0xFF0066FF), Color(0xFF9900CC),
    Color(0xFF663300), Color(0xFFFF69B4), Color.Gray, Color(0xFF00CCCC),
)

private fun checkerColor(x: Int, y: Int): Color =
    if ((x + y) % 2 == 0) Color(0xFFCCCCCC) else Color(0xFFFFFFFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PixelDrawScreen(
    onDone: (ByteArray) -> Unit,
    onBack: () -> Unit
) {
    val pixels = remember { IntArray(CANVAS_SIZE * CANVAS_SIZE) { TRANSPARENT } }

    var currentTool by remember { mutableStateOf(DrawTool.Pen) }
    var currentColor by remember { mutableStateOf(Color.Black) }
    var brushSize by remember { mutableIntStateOf(1) }

    // Undo/redo stacks store diffs: list of (index, oldColor) pairs
    val undoStack = remember { mutableStateListOf<List<Pair<Int, Int>>>() }
    val redoStack = remember { mutableStateListOf<List<Pair<Int, Int>>>() }

    // Current stroke accumulator - plain list, no need for observable state
    val currentStrokeDiff = remember { mutableListOf<Pair<Int, Int>>() }

    // Zoom/pan state
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Trigger canvas redraw on pixel changes
    var pixelVersion by remember { mutableIntStateOf(0) }

    // Pre-rendered bitmap buffer — single drawImage call instead of 57K+ drawRects
    val bitmapBuffer = remember {
        val bmp = ImageBitmap(CANVAS_SIZE, CANVAS_SIZE)
        val canvas = androidx.compose.ui.graphics.Canvas(bmp)
        val paint = Paint()
        for (y in 0 until CANVAS_SIZE) {
            for (x in 0 until CANVAS_SIZE) {
                paint.color = checkerColor(x, y)
                canvas.drawRect(
                    x.toFloat(), y.toFloat(),
                    (x + 1).toFloat(), (y + 1).toFloat(),
                    paint
                )
            }
        }
        bmp
    }
    val bitmapCanvas = remember { androidx.compose.ui.graphics.Canvas(bitmapBuffer) }
    val bitmapPaint = remember { Paint() }

    // Last pixel position for Bresenham line interpolation
    val lastPixel = remember { intArrayOf(-1, -1) }

    fun updateBitmapPixel(x: Int, y: Int, color: Int) {
        bitmapPaint.color = if (color == TRANSPARENT) checkerColor(x, y) else Color(color)
        bitmapCanvas.drawRect(
            x.toFloat(), y.toFloat(),
            (x + 1).toFloat(), (y + 1).toFloat(),
            bitmapPaint
        )
    }

    fun applyPixel(x: Int, y: Int, color: Int) {
        if (x < 0 || x >= CANVAS_SIZE || y < 0 || y >= CANVAS_SIZE) return
        val idx = y * CANVAS_SIZE + x
        if (pixels[idx] != color) {
            currentStrokeDiff.add(idx to pixels[idx])
            pixels[idx] = color
            updateBitmapPixel(x, y, color)
        }
    }

    fun applyBrush(cx: Int, cy: Int, color: Int) {
        val half = brushSize / 2
        for (dy in 0 until brushSize) {
            for (dx in 0 until brushSize) {
                applyPixel(cx - half + dx, cy - half + dy, color)
            }
        }
    }

    // Bresenham's line algorithm for smooth continuous line drawing
    fun drawPixelLine(x0: Int, y0: Int, x1: Int, y1: Int, color: Int) {
        var dx = abs(x1 - x0)
        var dy = -abs(y1 - y0)
        val sx = if (x0 < x1) 1 else -1
        val sy = if (y0 < y1) 1 else -1
        var err = dx + dy
        var cx = x0
        var cy = y0

        while (true) {
            applyBrush(cx, cy, color)
            if (cx == x1 && cy == y1) break
            val e2 = 2 * err
            if (e2 >= dy) {
                err += dy
                cx += sx
            }
            if (e2 <= dx) {
                err += dx
                cy += sy
            }
        }
    }

    fun commitStroke() {
        if (currentStrokeDiff.isNotEmpty()) {
            undoStack.add(currentStrokeDiff.toList())
            redoStack.clear()
            currentStrokeDiff.clear()
            pixelVersion++
        }
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val diff = undoStack.removeLast()
        val reverseDiff = diff.map { (idx, _) -> idx to pixels[idx] }
        for ((idx, oldColor) in diff) {
            pixels[idx] = oldColor
            updateBitmapPixel(idx % CANVAS_SIZE, idx / CANVAS_SIZE, oldColor)
        }
        redoStack.add(reverseDiff)
        pixelVersion++
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val diff = redoStack.removeLast()
        val reverseDiff = diff.map { (idx, _) -> idx to pixels[idx] }
        for ((idx, oldColor) in diff) {
            pixels[idx] = oldColor
            updateBitmapPixel(idx % CANVAS_SIZE, idx / CANVAS_SIZE, oldColor)
        }
        undoStack.add(reverseDiff)
        pixelVersion++
    }

    fun floodFill(startX: Int, startY: Int, fillColor: Int) {
        if (startX < 0 || startX >= CANVAS_SIZE || startY < 0 || startY >= CANVAS_SIZE) return
        val targetColor = pixels[startY * CANVAS_SIZE + startX]
        if (targetColor == fillColor) return

        val queue = ArrayDeque<Pair<Int, Int>>()
        queue.add(startX to startY)
        while (queue.isNotEmpty()) {
            val (x, y) = queue.removeFirst()
            if (x < 0 || x >= CANVAS_SIZE || y < 0 || y >= CANVAS_SIZE) continue
            val idx = y * CANVAS_SIZE + x
            if (pixels[idx] != targetColor) continue
            currentStrokeDiff.add(idx to pixels[idx])
            pixels[idx] = fillColor
            updateBitmapPixel(x, y, fillColor)
            queue.add((x + 1) to y)
            queue.add((x - 1) to y)
            queue.add(x to (y + 1))
            queue.add(x to (y - 1))
        }
        commitStroke()
    }

    fun screenToPixel(pos: Offset, canvasSize: IntSize): Pair<Int, Int> {
        val px = ((pos.x - offsetX) / scale / (canvasSize.width.toFloat() / CANVAS_SIZE)).toInt()
        val py = ((pos.y - offsetY) / scale / (canvasSize.height.toFloat() / CANVAS_SIZE)).toInt()
        return px to py
    }

    fun exportToPng(): ByteArray {
        val imageBitmap = ImageBitmap(CANVAS_SIZE, CANVAS_SIZE)
        val canvas = androidx.compose.ui.graphics.Canvas(imageBitmap)
        val paint = Paint()
        for (y in 0 until CANVAS_SIZE) {
            for (x in 0 until CANVAS_SIZE) {
                val c = pixels[y * CANVAS_SIZE + x]
                paint.color = if (c == TRANSPARENT) Color.White else Color(c)
                canvas.drawRect(
                    x.toFloat(), y.toFloat(),
                    (x + 1).toFloat(), (y + 1).toFloat(),
                    paint
                )
            }
        }
        return encodeBitmapToPng(imageBitmap)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Draw Plant") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("\u2190 Back")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        onDone(exportToPng())
                    }) {
                        Text("Done")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tool bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DrawTool.entries.forEach { tool ->
                    FilterChip(
                        selected = currentTool == tool,
                        onClick = { currentTool = tool },
                        label = { Text(tool.name) }
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = ::undo, enabled = undoStack.isNotEmpty()) {
                    Text("Undo")
                }
                TextButton(onClick = ::redo, enabled = redoStack.isNotEmpty()) {
                    Text("Redo")
                }
            }

            // Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFFE0E0E0))
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.5f, 20f)
                                offsetX += pan.x
                                offsetY += pan.y
                            }
                        }
                        .pointerInput(currentTool) {
                            // Only currentTool as key — switching between Fill/Pen/Eraser
                            // needs to restart gesture detection. Color and brush size are
                            // read dynamically from state inside the handlers.
                            // pixelVersion must NOT be a key here — it changes on every
                            // pixel drawn, which would restart and cancel the drag gesture.
                            if (currentTool == DrawTool.Fill) {
                                detectTapGestures { pos ->
                                    val (px, py) = screenToPixel(pos, size)
                                    val fillColorInt = currentColor.toArgb()
                                    floodFill(px, py, fillColorInt)
                                }
                            } else {
                                detectDragGestures(
                                    onDragStart = { pos ->
                                        val (px, py) = screenToPixel(pos, size)
                                        val c = if (currentTool == DrawTool.Eraser) TRANSPARENT else currentColor.toArgb()
                                        applyBrush(px, py, c)
                                        lastPixel[0] = px
                                        lastPixel[1] = py
                                        pixelVersion++
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        val (px, py) = screenToPixel(change.position, size)
                                        val c = if (currentTool == DrawTool.Eraser) TRANSPARENT else currentColor.toArgb()
                                        // Interpolate between last and current position
                                        if (lastPixel[0] >= 0) {
                                            drawPixelLine(lastPixel[0], lastPixel[1], px, py, c)
                                        } else {
                                            applyBrush(px, py, c)
                                        }
                                        lastPixel[0] = px
                                        lastPixel[1] = py
                                        pixelVersion++
                                    },
                                    onDragEnd = {
                                        lastPixel[0] = -1
                                        lastPixel[1] = -1
                                        commitStroke()
                                    }
                                )
                            }
                        }
                ) {
                    // Read pixelVersion to trigger redraw when pixels change
                    pixelVersion

                    val scaledW = size.width * scale
                    val scaledH = size.height * scale

                    // Single drawImage call replaces 57K+ individual drawRect calls
                    drawImage(
                        image = bitmapBuffer,
                        srcOffset = IntOffset.Zero,
                        srcSize = IntSize(CANVAS_SIZE, CANVAS_SIZE),
                        dstOffset = IntOffset(offsetX.roundToInt(), offsetY.roundToInt()),
                        dstSize = IntSize(scaledW.roundToInt(), scaledH.roundToInt()),
                        filterQuality = FilterQuality.None // Nearest-neighbor for crisp pixels
                    )

                    // Draw grid lines when zoomed in enough
                    if (scale > 4f) {
                        val cellW = size.width / CANVAS_SIZE * scale
                        val cellH = size.height / CANVAS_SIZE * scale
                        val gridColor = Color(0x22000000)
                        val startX = max(0, ((-offsetX) / cellW).toInt())
                        val endX = min(CANVAS_SIZE, ((size.width - offsetX) / cellW).toInt() + 1)
                        val startY = max(0, ((-offsetY) / cellH).toInt())
                        val endY = min(CANVAS_SIZE, ((size.height - offsetY) / cellH).toInt() + 1)

                        for (x in startX..endX) {
                            val xPos = x * cellW + offsetX
                            drawLine(gridColor, Offset(xPos, startY * cellH + offsetY), Offset(xPos, endY * cellH + offsetY))
                        }
                        for (y in startY..endY) {
                            val yPos = y * cellH + offsetY
                            drawLine(gridColor, Offset(startX * cellW + offsetX, yPos), Offset(endX * cellW + offsetX, yPos))
                        }
                    }
                }
            }

            // Brush size selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Brush:", style = MaterialTheme.typography.labelMedium)
                listOf(1, 2, 4, 8).forEach { size ->
                    FilterChip(
                        selected = brushSize == size,
                        onClick = { brushSize = size },
                        label = { Text("${size}px") }
                    )
                }
            }

            // Color palette
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(presetColors) { color ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .then(
                                if (color == currentColor)
                                    Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                else
                                    Modifier.border(1.dp, Color.Gray, CircleShape)
                            )
                            .clickable { currentColor = color }
                    )
                }
            }

            // RGB sliders
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                var red by remember { mutableFloatStateOf(0f) }
                var green by remember { mutableFloatStateOf(0f) }
                var blue by remember { mutableFloatStateOf(0f) }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("R", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(16.dp))
                    Slider(value = red, onValueChange = {
                        red = it
                        currentColor = Color(red, green, blue)
                    }, modifier = Modifier.weight(1f))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("G", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(16.dp))
                    Slider(value = green, onValueChange = {
                        green = it
                        currentColor = Color(red, green, blue)
                    }, modifier = Modifier.weight(1f))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("B", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(16.dp))
                    Slider(value = blue, onValueChange = {
                        blue = it
                        currentColor = Color(red, green, blue)
                    }, modifier = Modifier.weight(1f))
                }

                // Custom color preview
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(currentColor)
                        .border(1.dp, Color.Gray, CircleShape)
                        .align(Alignment.End)
                )
            }
        }
    }
}

// Expect function for PNG encoding (platform specific)
expect fun encodeBitmapToPng(bitmap: ImageBitmap): ByteArray
