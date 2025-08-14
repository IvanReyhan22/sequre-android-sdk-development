package id.sequre.scanner_sdk.common.utils

import android.graphics.Bitmap
import android.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

data class ComponentStats(
    val label: Int,
    val boundingBox: BoundingBox,
    val area: Int,
    val centroid: Pair<Float, Float>
)

data class BoundingBox(
    val minX: Int, val maxX: Int, val minY: Int, val maxY: Int
)

fun connectedComponentsWithStats(binaryBitmap: Bitmap): Pair<Int, List<ComponentStats>> {
    val width = binaryBitmap.width
    val height = binaryBitmap.height

    // Convert bitmap to a flat pixel array for fast access
    val pixels = IntArray(width * height)
    binaryBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

    // Visited array to mark pixels we've already processed
    val visited = BooleanArray(width * height)

    // Directions for 8-connected neighbors (diagonal neighbors included)
    val directions = arrayOf(
        Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1),  // Horizontal & Vertical
        Pair(-1, -1), Pair(-1, 1), Pair(1, -1), Pair(1, 1)  // Diagonal
    )

    val components = mutableListOf<ComponentStats>()
    var label = 0  // Label for components

    // Helper function to perform DFS and mark connected components
    fun dfs(x: Int, y: Int, label: Int): ComponentStats {
        val stack = mutableListOf(Pair(x, y))
        visited[y * width + x] = true

        // Track the bounding box, centroid calculation, and area
        var minX = x
        var maxX = x
        var minY = y
        var maxY = y
        var area = 0
        var sumX = 0
        var sumY = 0

        val pixelsInComponent = mutableListOf<Pair<Int, Int>>()

        while (stack.isNotEmpty()) {
            val (cx, cy) = stack.removeAt(stack.size - 1)
            pixelsInComponent.add(Pair(cx, cy))
            area++

            sumX += cx
            sumY += cy

            // Update bounding box
            minX = minOf(minX, cx)
            maxX = maxOf(maxX, cx)
            minY = minOf(minY, cy)
            maxY = maxOf(maxY, cy)

            // Explore neighbors
            for (direction in directions) {
                val nx = cx + direction.first
                val ny = cy + direction.second
                if (nx in 0 until width && ny in 0 until height && !visited[ny * width + nx] &&
                    Color.red(pixels[ny * width + nx]) == 255  // White pixel
                ) {
                    visited[ny * width + nx] = true
                    stack.add(Pair(nx, ny))
                }
            }
        }

        // Calculate centroid
        val centroidX = sumX / area.toFloat()
        val centroidY = sumY / area.toFloat()

        // Return the component statistics
        return ComponentStats(
            label = label,
            boundingBox = BoundingBox(minX, maxX, minY, maxY),
            area = area,
            centroid = Pair(centroidX, centroidY)
        )
    }

    // Step 1: Parallelize the component search using coroutines
    runBlocking {
        val chunkSize = height / 4  // Split the image into 4 chunks for parallel processing
        val jobs = mutableListOf<Job>()

        // Process each chunk in parallel
        for (i in 0 until 4) {
            val startY = i * chunkSize
            val endY = if (i == 3) height else (i + 1) * chunkSize

            val job = launch(Dispatchers.Default) {
                for (y in startY until endY) {
                    for (x in 0 until width) {
                        if (Color.red(pixels[y * width + x]) == 255 && !visited[y * width + x]) {
                            label++
                            val componentStats = dfs(x, y, label)
                            synchronized(components) {
                                components.add(componentStats)
                            }
                        }
                    }
                }
            }
            jobs.add(job)
        }

        // Wait for all coroutines to finish
        jobs.joinAll()
    }

    // Return the number of components and the list of component stats
    return Pair(components.size, components)
}
