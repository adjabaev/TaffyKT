package be.arby.taffy.test.compute.grid

import net.asterium.taffy.compute.grid.ExplicitGrid
import net.asterium.taffy.compute.grid.types.GridTrack
import net.asterium.taffy.compute.grid.types.GridTrackKind
import net.asterium.taffy.compute.grid.types.TrackCounts
import net.asterium.taffy.geometry.Rect
import net.asterium.taffy.geometry.Size
import net.asterium.taffy.maths.axis.AbsoluteAxis
import net.asterium.taffy.style.Style
import net.asterium.taffy.style.dimension.Dimension
import net.asterium.taffy.style.dimension.LengthPercentage
import net.asterium.taffy.style.Display
import net.asterium.taffy.style.grid.*
import net.asterium.taffy.utils.tuples.Quadruple
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ExplicitGrid {
    @Test
    fun `Explicit grid sizing no repeats`() {
        val gridStyle = Quadruple(600.0f, 600.0f, 2, 4).intoGrid();
        val width = ExplicitGrid.computeExplicitGridSizeInAxis(gridStyle, AbsoluteAxis.HORIZONTAL)
        val height = ExplicitGrid.computeExplicitGridSizeInAxis(gridStyle, AbsoluteAxis.VERTICAL)
        Assertions.assertEquals(2, width)
        Assertions.assertEquals(4, height)
    }

    @Test
    fun `Explicit grid sizing auto fill exact fit`() {
        val gridStyle = Style(
            display = Display.GRID,
            size = Size(width = Dimension.fromPoints(120.0f), height = Dimension.fromPoints(80.0f)),
            gridTemplateColumns = listOf(
                TrackSizingFunction.Repeat(
                    GridTrackRepetition.AutoFill,
                    listOf(NonRepeatedTrackSizingFunction.fromPoints(40.0f))
                )
            ),
            gridTemplateRows = listOf(
                TrackSizingFunction.Repeat(
                    GridTrackRepetition.AutoFill,
                    listOf(NonRepeatedTrackSizingFunction.fromPoints(20.0f))
                )
            )
        )
        val width = ExplicitGrid.computeExplicitGridSizeInAxis(gridStyle, AbsoluteAxis.HORIZONTAL)
        val height = ExplicitGrid.computeExplicitGridSizeInAxis(gridStyle, AbsoluteAxis.VERTICAL)
        Assertions.assertEquals(3, width)
        Assertions.assertEquals(4, height)
    }

    @Test
    fun `explicit grid sizing auto fill non exact fit`() {
        val gridStyle = Style(
            display = Display.GRID,
            size = Size(width = Dimension.fromPoints(140.0f), height = Dimension.fromPoints(90.0f)),
            gridTemplateColumns = listOf(
                TrackSizingFunction.Repeat(
                    GridTrackRepetition.AutoFill,
                    listOf(NonRepeatedTrackSizingFunction.fromPoints(40.0f))
                )
            ),
            gridTemplateRows = listOf(
                TrackSizingFunction.Repeat(
                    GridTrackRepetition.AutoFill,
                    listOf(NonRepeatedTrackSizingFunction.fromPoints(20.0f))
                )
            )
        )
        val width = ExplicitGrid.computeExplicitGridSizeInAxis(gridStyle, AbsoluteAxis.HORIZONTAL)
        val height = ExplicitGrid.computeExplicitGridSizeInAxis(gridStyle, AbsoluteAxis.VERTICAL)
        Assertions.assertEquals(3, width)
        Assertions.assertEquals(4, height)
    }

    @Test
    fun `Explicit grid sizing auto fill min size exact fit`() {
        val gridStyle = Style(
            display = Display.GRID,
            size = Size(width = Dimension.fromPoints(120.0f), height = Dimension.fromPoints(80.0f)),
            gridTemplateColumns = listOf(
                TrackSizingFunction.Repeat(
                    GridTrackRepetition.AutoFill,
                    listOf(NonRepeatedTrackSizingFunction.fromPoints(40.0f))
                )
            ),
            gridTemplateRows = listOf(
                TrackSizingFunction.Repeat(
                    GridTrackRepetition.AutoFill,
                    listOf(NonRepeatedTrackSizingFunction.fromPoints(20.0f))
                )
            )
        )
        val width = ExplicitGrid.computeExplicitGridSizeInAxis(gridStyle, AbsoluteAxis.HORIZONTAL)
        val height = ExplicitGrid.computeExplicitGridSizeInAxis(gridStyle, AbsoluteAxis.VERTICAL)
        Assertions.assertEquals(3, width)
        Assertions.assertEquals(4, height)
    }

    @Test
    fun `Explicit grid sizing auto fill min size non exact fit`() {
        val gridStyle = Style(
            display = Display.GRID,
            minSize = Size(width = Dimension.fromPoints(140.0f), height = Dimension.fromPoints(90.0f)),
            gridTemplateColumns = listOf(
                TrackSizingFunction.Repeat(
                    GridTrackRepetition.AutoFill,
                    listOf(NonRepeatedTrackSizingFunction.fromPoints(40.0f))
                )
            ),
            gridTemplateRows = listOf(
                TrackSizingFunction.Repeat(
                    GridTrackRepetition.AutoFill,
                    listOf(NonRepeatedTrackSizingFunction.fromPoints(20.0f))
                )
            )
        )
        val width = ExplicitGrid.computeExplicitGridSizeInAxis(gridStyle, AbsoluteAxis.HORIZONTAL)
        val height = ExplicitGrid.computeExplicitGridSizeInAxis(gridStyle, AbsoluteAxis.VERTICAL)
        Assertions.assertEquals(4, width)
        Assertions.assertEquals(5, height)
    }

    @Test
    fun `Explicit grid sizing auto fill multiple repeated tracks`() {
        val gridStyle = Style(
            display = Display.GRID,
            size = Size(width = Dimension.fromPoints(140.0f), height = Dimension.fromPoints(100.0f)),
            gridTemplateColumns = listOf(
                TrackSizingFunction.Repeat(
                    GridTrackRepetition.AutoFill,
                    listOf(
                        NonRepeatedTrackSizingFunction.fromPoints(40.0f),
                        NonRepeatedTrackSizingFunction.fromPoints(20.0f)
                    )
                )
            ),
            gridTemplateRows = listOf(
                TrackSizingFunction.Repeat(
                    GridTrackRepetition.AutoFill,
                    listOf(
                        NonRepeatedTrackSizingFunction.fromPoints(20.0f),
                        NonRepeatedTrackSizingFunction.fromPoints(10.0f)
                    )
                )
            )
        )
        val width = ExplicitGrid.computeExplicitGridSizeInAxis(gridStyle, AbsoluteAxis.HORIZONTAL)
        val height = ExplicitGrid.computeExplicitGridSizeInAxis(gridStyle, AbsoluteAxis.VERTICAL)
        Assertions.assertEquals(4, width) // 2 repetitions * 2 repeated tracks = 4 tracks in total
        Assertions.assertEquals(6, height) // 3 repetitions * 2 repeated tracks = 4 tracks in total
    }

    @Test
    fun `Explicit grid sizing auto fill gap`() {
        val gridStyle = Style(
            display = Display.GRID,
            size = Size(width = Dimension.fromPoints(140.0f), height = Dimension.fromPoints(100.0f)),
            gridTemplateColumns = listOf(
                TrackSizingFunction.Repeat(
                    GridTrackRepetition.AutoFill,
                    listOf(NonRepeatedTrackSizingFunction.fromPoints(40.0f))
                )
            ),
            gridTemplateRows = listOf(
                TrackSizingFunction.Repeat(
                    GridTrackRepetition.AutoFill,
                    listOf(NonRepeatedTrackSizingFunction.fromPoints(20.0f))
                )
            ),
            gap = Size.fromPointsLP(20f)
        )
        val width = ExplicitGrid.computeExplicitGridSizeInAxis(gridStyle, AbsoluteAxis.HORIZONTAL)
        val height = ExplicitGrid.computeExplicitGridSizeInAxis(gridStyle, AbsoluteAxis.VERTICAL)
        Assertions.assertEquals(2, width) // 2 tracks + 1 gap
        Assertions.assertEquals(3, height) // 3 tracks + 2 gaps
    }

    @Test
    fun `Explicit grid sizing no defined size`() {
        val gridStyle = Style(
            display = Display.GRID,
            gridTemplateColumns = listOf(
                TrackSizingFunction.Repeat(
                    GridTrackRepetition.AutoFill,
                    listOf(
                        NonRepeatedTrackSizingFunction.fromPoints(40.0f),
                        NonRepeatedTrackSizingFunction.fromPercent(0.5f),
                        NonRepeatedTrackSizingFunction.fromPoints(20.0f)
                    )
                )
            ),
            gridTemplateRows = listOf(
                TrackSizingFunction.Repeat(
                    GridTrackRepetition.AutoFill,
                    listOf(NonRepeatedTrackSizingFunction.fromPoints(20.0f))
                )
            ),
            gap = Size.fromPointsLP(20f)
        )
        val width = ExplicitGrid.computeExplicitGridSizeInAxis(gridStyle, AbsoluteAxis.HORIZONTAL)
        val height = ExplicitGrid.computeExplicitGridSizeInAxis(gridStyle, AbsoluteAxis.VERTICAL)
        Assertions.assertEquals(3, width)
        Assertions.assertEquals(1, height)
    }

    @Test
    fun `Explicit grid sizing mix repeated and non repeated`() {
        val gridStyle = Style(
            display = Display.GRID,
            size = Size(width = Dimension.fromPoints(140.0f), height = Dimension.fromPoints(100.0f)),
            gridTemplateColumns = listOf(
                TrackSizingFunction.Single(NonRepeatedTrackSizingFunction.fromPoints(20.0f)),
                TrackSizingFunction.Repeat(
                    GridTrackRepetition.AutoFill,
                    listOf(NonRepeatedTrackSizingFunction.fromPoints(40.0f))
                )
            ),
            gridTemplateRows = listOf(
                TrackSizingFunction.Single(NonRepeatedTrackSizingFunction.fromPoints(40.0f)),
                TrackSizingFunction.Repeat(
                    GridTrackRepetition.AutoFill,
                    listOf(NonRepeatedTrackSizingFunction.fromPoints(20.0f))
                )
            ),
            gap = Size.fromPointsLP(20f)
        )
        val width = ExplicitGrid.computeExplicitGridSizeInAxis(gridStyle, AbsoluteAxis.HORIZONTAL)
        val height = ExplicitGrid.computeExplicitGridSizeInAxis(gridStyle, AbsoluteAxis.VERTICAL)
        Assertions.assertEquals(3, width) // 3 tracks + 2 gaps
        Assertions.assertEquals(2, height) // 2 tracks + 1 gap
    }

    @Test
    fun `Explicit grid sizing mix with padding`() {
        val gridStyle = Style(
            display = Display.GRID,
            size = Size(width = Dimension.fromPoints(120.0f), height = Dimension.fromPoints(120.0f)),
            padding = Rect(
                left = LengthPercentage.fromLength(10.0f),
                right = LengthPercentage.fromLength(10.0f),
                top = LengthPercentage.fromLength(20.0f),
                bottom = LengthPercentage.fromLength(20.0f)
            ),
            gridTemplateColumns = listOf(
                TrackSizingFunction.Repeat(
                    GridTrackRepetition.AutoFill,
                    listOf(NonRepeatedTrackSizingFunction.fromPoints(20.0f))
                )
            ),
            gridTemplateRows = listOf(
                TrackSizingFunction.Repeat(
                    GridTrackRepetition.AutoFill,
                    listOf(NonRepeatedTrackSizingFunction.fromPoints(20.0f))
                )
            )
        )
        val width = ExplicitGrid.computeExplicitGridSizeInAxis(gridStyle, AbsoluteAxis.HORIZONTAL)
        val height = ExplicitGrid.computeExplicitGridSizeInAxis(gridStyle, AbsoluteAxis.VERTICAL)
        Assertions.assertEquals(5, width) // 40px horizontal padding
        Assertions.assertEquals(4, height) // 20px vertical padding
    }

    @Test
    fun `Test initialize grid tracks`() {
        val px0 = LengthPercentage.Length(0.0f)
        val px20 = LengthPercentage.Length(20.0f)
        val px100 = LengthPercentage.Length(100.0f)

        // Setup test
        val trackTemplate = listOf(
            TrackSizingFunction.fromPoints(100.0f),
            TrackSizingFunction.Single(
                NonRepeatedTrackSizingFunction(
                    MinTrackSizingFunction.fromPoints(100.0f),
                    MaxTrackSizingFunction.fromFlex(2.0f)
                )
            ),
            TrackSizingFunction.fromFlex(1f)
        )
        val trackCounts = TrackCounts(negativeImplicit = 3, explicit = trackTemplate.size, positiveImplicit = 3)
        val autoTracks = listOf(
            NonRepeatedTrackSizingFunction.makeAuto(),
            NonRepeatedTrackSizingFunction.fromPoints(100.0f)
        )
        val gap = px20

        // Call function
        val tracks = ArrayList<GridTrack>();
        ExplicitGrid.initializeGridTracks(tracks, trackCounts, trackTemplate, autoTracks, gap) { _ -> false }

        // Assertions
        val expected = listOf(
            // Gutter
            Triple(GridTrackKind.GUTTER, MinTrackSizingFunction.Fixed(px0), MaxTrackSizingFunction.Fixed(px0)),
            // Negative implict tracks
            Triple(GridTrackKind.TRACK, MinTrackSizingFunction.Fixed(px100), MaxTrackSizingFunction.Fixed(px100)),
            Triple(GridTrackKind.GUTTER, MinTrackSizingFunction.Fixed(px20), MaxTrackSizingFunction.Fixed(px20)),
            Triple(GridTrackKind.TRACK, MinTrackSizingFunction.Auto, MaxTrackSizingFunction.Auto),
            Triple(GridTrackKind.GUTTER, MinTrackSizingFunction.Fixed(px20), MaxTrackSizingFunction.Fixed(px20)),
            Triple(GridTrackKind.TRACK, MinTrackSizingFunction.Fixed(px100), MaxTrackSizingFunction.Fixed(px100)),
            Triple(GridTrackKind.GUTTER, MinTrackSizingFunction.Fixed(px20), MaxTrackSizingFunction.Fixed(px20)),
            // Explicit tracks
            Triple(GridTrackKind.TRACK, MinTrackSizingFunction.Fixed(px100), MaxTrackSizingFunction.Fixed(px100)),
            Triple(GridTrackKind.GUTTER, MinTrackSizingFunction.Fixed(px20), MaxTrackSizingFunction.Fixed(px20)),
            Triple(
                GridTrackKind.TRACK,
                MinTrackSizingFunction.Fixed(px100),
                MaxTrackSizingFunction.Flex(2.0f)
            ), // Note: separate min-max functions
            Triple(GridTrackKind.GUTTER, MinTrackSizingFunction.Fixed(px20), MaxTrackSizingFunction.Fixed(px20)),
            Triple(
                GridTrackKind.TRACK,
                MinTrackSizingFunction.Auto,
                MaxTrackSizingFunction.Flex(1.0f)
            ), // Note: min sizing function of flex sizing functions is auto
            Triple(GridTrackKind.GUTTER, MinTrackSizingFunction.Fixed(px20), MaxTrackSizingFunction.Fixed(px20)),
            // Positive implict tracks
            Triple(GridTrackKind.TRACK, MinTrackSizingFunction.Auto, MaxTrackSizingFunction.Auto),
            Triple(GridTrackKind.GUTTER, MinTrackSizingFunction.Fixed(px20), MaxTrackSizingFunction.Fixed(px20)),
            Triple(GridTrackKind.TRACK, MinTrackSizingFunction.Fixed(px100), MaxTrackSizingFunction.Fixed(px100)),
            Triple(GridTrackKind.GUTTER, MinTrackSizingFunction.Fixed(px20), MaxTrackSizingFunction.Fixed(px20)),
            Triple(GridTrackKind.TRACK, MinTrackSizingFunction.Auto, MaxTrackSizingFunction.Auto),
            Triple(GridTrackKind.GUTTER, MinTrackSizingFunction.Fixed(px0), MaxTrackSizingFunction.Fixed(px0)),
        )

        Assertions.assertEquals(tracks.size, expected.size, "Number of tracks doesn't match");

        for ((idx, b) in tracks.zip(expected).withIndex()) {
            val (actual, d) = b
            val (kind, min, max) = d
            Assertions.assertEquals(kind, actual.kind, "Track $idx (0-based index)")
            Assertions.assertEquals(min, actual.minTrackSizingFunction, "Track $idx (0-based index)")
            Assertions.assertEquals(max, actual.maxTrackSizingFunction, "Track $idx (0-based index)")
        }
    }
}
