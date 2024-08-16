package be.arby.taffy.tests.compute.grid

import be.arby.taffy.*
import be.arby.taffy.compute.grid.computeExplicitGridSizeInAxis
import be.arby.taffy.compute.grid.initializeGridTracks
import be.arby.taffy.compute.grid.types.GridTrack
import be.arby.taffy.compute.grid.types.GridTrackKind
import be.arby.taffy.compute.grid.types.TrackCounts
import be.arby.taffy.compute.grid.util.intoGrid
import be.arby.taffy.geom.AbsoluteAxis
import be.arby.taffy.geom.Rect
import be.arby.taffy.geom.Size
import be.arby.taffy.lang.Option
import be.arby.taffy.lang.collections.enumerate
import be.arby.taffy.lang.collections.len
import be.arby.taffy.lang.tuples.T3
import be.arby.taffy.lang.tuples.T4
import be.arby.taffy.style.Display
import be.arby.taffy.style.Style
import be.arby.taffy.style.dimension.LengthPercentage
import be.arby.taffy.style.grid.*
import be.arby.taffy.tests.assertEq
import org.junit.jupiter.api.Test

class ExplicitGrid {
    @Test
    fun `explicit grid sizing no repeats`() {
        val gridStyle = T4(600f, 600f, 2, 4).intoGrid()
        val preferredSize = gridStyle.size.map { s -> s.intoOption() }
        val width = computeExplicitGridSizeInAxis(
            gridStyle,
            gridStyle.gridTemplateColumns,
            preferredSize,
            AbsoluteAxis.HORIZONTAL
        )
        val height = computeExplicitGridSizeInAxis(
            gridStyle as GridContainerStyle,
            gridStyle.gridTemplateRows,
            preferredSize,
            AbsoluteAxis.VERTICAL
        )
        assertEq(width, 2)
        assertEq(height, 4)
    }

    @Test
    fun `explicit grid sizing auto fill exact fit`() {
        val gridStyle = Style(
            display = Display.GRID,
            size = Size(width = length(120f), height = length(80f)),
            gridTemplateColumns = vec(rpt(GridTrackRepetition.AutoFill, vec(length(40f)))),
            gridTemplateRows = vec(rpt(GridTrackRepetition.AutoFill, vec(length(20f))))
        )
        val preferredSize = gridStyle.size.map { s -> s.intoOption() }
        val width = computeExplicitGridSizeInAxis(
            gridStyle,
            gridStyle.gridTemplateColumns,
            preferredSize,
            AbsoluteAxis.HORIZONTAL,
        )
        val height = computeExplicitGridSizeInAxis(
            gridStyle,
            gridStyle.gridTemplateRows,
            preferredSize,
            AbsoluteAxis.VERTICAL,
        )
        assertEq(width, 3)
        assertEq(height, 4)
    }

    @Test
    fun `explicit grid sizing auto fill non exact fit`() {
        val gridStyle = Style(
            display = Display.GRID,
            size = Size(width = length(140f), height = length(90f)),
            gridTemplateColumns = vec(rpt(GridTrackRepetition.AutoFill, vec(length(40f)))),
            gridTemplateRows = vec(rpt(GridTrackRepetition.AutoFill, vec(length(20f))))
        )
        val preferredSize = gridStyle.size.map { s -> s.intoOption() }
        val width = computeExplicitGridSizeInAxis(
            gridStyle,
            gridStyle.gridTemplateColumns,
            preferredSize,
            AbsoluteAxis.HORIZONTAL,
        )
        val height = computeExplicitGridSizeInAxis(
            gridStyle,
            gridStyle.gridTemplateRows,
            preferredSize,
            AbsoluteAxis.VERTICAL,
        )
        assertEq(width, 3)
        assertEq(height, 4)
    }

    @Test
    fun `explicit grid sizing auto fill min size exact fit`() {
        val gridStyle = Style(
            display = Display.GRID,
            minSize = Size(width = length(120f), height = length(80f)),
            gridTemplateColumns = vec(rpt(GridTrackRepetition.AutoFill, vec(length(40f)))),
            gridTemplateRows = vec(rpt(GridTrackRepetition.AutoFill, vec(length(20f))))
        )
        val innerContainerSize = Size(width = Option.from(120f), height = Option.from(80f))
        val width = computeExplicitGridSizeInAxis(
            gridStyle,
            gridStyle.gridTemplateColumns,
            innerContainerSize,
            AbsoluteAxis.HORIZONTAL,
        )
        val height = computeExplicitGridSizeInAxis(
            gridStyle,
            gridStyle.gridTemplateRows,
            innerContainerSize,
            AbsoluteAxis.VERTICAL,
        )
        assertEq(width, 3)
        assertEq(height, 4)
    }

    @Test
    fun `explicit grid sizing auto fill min size non exact fit`() {
        val gridStyle = Style(
            display = Display.GRID,
            minSize = Size(width = length(140f), height = length(90f)),
            gridTemplateColumns = vec(rpt(GridTrackRepetition.AutoFill, vec(length(40f)))),
            gridTemplateRows = vec(rpt(GridTrackRepetition.AutoFill, vec(length(20f))))
        )
        val innerContainerSize = Size(width = Option.from(140f), height = Option.from(90f))
        val width = computeExplicitGridSizeInAxis(
            gridStyle,
            gridStyle.gridTemplateColumns,
            innerContainerSize,
            AbsoluteAxis.HORIZONTAL,
        )
        val height = computeExplicitGridSizeInAxis(
            gridStyle,
            gridStyle.gridTemplateRows,
            innerContainerSize,
            AbsoluteAxis.VERTICAL,
        )
        assertEq(width, 4)
        assertEq(height, 5)
    }

    @Test
    fun `explicit grid sizing auto fill multiple repeated tracks`() {
        val gridStyle = Style(
            display = Display.GRID,
            size = Size(width = length(140f), height = length(100f)),
            gridTemplateColumns = vec(rpt(GridTrackRepetition.AutoFill, vec(length(40f), length(20f)))),
            gridTemplateRows = vec(rpt(GridTrackRepetition.AutoFill, vec(length(20f), length(10f))))
        )
        val preferredSize = gridStyle.size.map { s -> s.intoOption() }
        val width = computeExplicitGridSizeInAxis(
            gridStyle,
            gridStyle.gridTemplateColumns,
            preferredSize,
            AbsoluteAxis.HORIZONTAL,
        )
        val height = computeExplicitGridSizeInAxis(
            gridStyle,
            gridStyle.gridTemplateRows,
            preferredSize,
            AbsoluteAxis.VERTICAL,
        )
        assertEq(width, 4) // 2 repetitions * 2 repeated tracks = 4 tracks in total
        assertEq(height, 6) // 3 repetitions * 2 repeated tracks = 4 tracks in total
    }

    @Test
    fun `explicit grid sizing auto fill gap`() {
        val gridStyle = Style(
            display = Display.GRID,
            size = Size(width = length(140f), height = length(100f)),
            gridTemplateColumns = vec(rpt(GridTrackRepetition.AutoFill, vec(length(40f)))),
            gridTemplateRows = vec(rpt(GridTrackRepetition.AutoFill, vec(length(20f)))),
            gap = length(20f)
        )
        val preferredSize = gridStyle.size.map { s -> s.intoOption() }
        val width = computeExplicitGridSizeInAxis(
            gridStyle,
            gridStyle.gridTemplateColumns,
            preferredSize,
            AbsoluteAxis.HORIZONTAL,
        )
        val height = computeExplicitGridSizeInAxis(
            gridStyle,
            gridStyle.gridTemplateRows,
            preferredSize,
            AbsoluteAxis.VERTICAL,
        )
        assertEq(width, 2) // 2 tracks + 1 gap
        assertEq(height, 3) // 3 tracks + 2 gaps
    }

    @Test
    fun `explicit grid sizing no defined size`() {
        val gridStyle = Style(
            display = Display.GRID,
            gridTemplateColumns = vec(rpt(GridTrackRepetition.AutoFill, vec(length(40f), percent(0.5f), length(20f)))),
            gridTemplateRows = vec(rpt(GridTrackRepetition.AutoFill, vec(length(20f)))),
            gap = length(20f)
        )
        val preferredSize = gridStyle.size.map { s -> s.intoOption() }
        val width = computeExplicitGridSizeInAxis(
            gridStyle,
            gridStyle.gridTemplateColumns,
            preferredSize,
            AbsoluteAxis.HORIZONTAL,
        )
        val height = computeExplicitGridSizeInAxis(
            gridStyle,
            gridStyle.gridTemplateRows,
            preferredSize,
            AbsoluteAxis.VERTICAL,
        )
        assertEq(width, 3)
        assertEq(height, 1)
    }

    @Test
    fun `explicit grid sizing mix repeated and non repeated`() {
        val gridStyle = Style(
            display = Display.GRID,
            size = Size(width = length(140f), height = length(100f)),
            gridTemplateColumns = vec(length(20f), rpt(GridTrackRepetition.AutoFill, vec(length(40f)))),
            gridTemplateRows = vec(length(40f), rpt(GridTrackRepetition.AutoFill, vec(length(20f)))),
            gap = length(20f)
        )
        val preferredSize = gridStyle.size.map { s -> s.intoOption() }
        val width = computeExplicitGridSizeInAxis(
            gridStyle,
            gridStyle.gridTemplateColumns,
            preferredSize,
            AbsoluteAxis.HORIZONTAL,
        )
        val height = computeExplicitGridSizeInAxis(
            gridStyle,
            gridStyle.gridTemplateRows,
            preferredSize,
            AbsoluteAxis.VERTICAL,
        )
        assertEq(width, 3) // 3 tracks + 2 gaps
        assertEq(height, 2) // 2 tracks + 1 gap
    }

    @Test
    fun `explicit grid sizing mix with padding`() {
        val gridStyle = Style(
            display = Display.GRID,
            size = Size(width = length(120f), height = length(120f)),
            padding = Rect(left = length(10f), right = length(10f), top = length(20f), bottom = length(20f)),
            gridTemplateColumns = vec(rpt(GridTrackRepetition.AutoFill, vec(length(20f)))),
            gridTemplateRows = vec(rpt(GridTrackRepetition.AutoFill, vec(length(20f))))
        )
        val innerContainerSize = Size(width = Option.from(100f), height = Option.from(80f))
        val width = computeExplicitGridSizeInAxis(
            gridStyle,
            gridStyle.gridTemplateColumns,
            innerContainerSize,
            AbsoluteAxis.HORIZONTAL,
        )
        val height = computeExplicitGridSizeInAxis(
            gridStyle,
            gridStyle.gridTemplateRows,
            innerContainerSize,
            AbsoluteAxis.VERTICAL,
        )
        assertEq(width, 5) // 40px horizontal padding
        assertEq(height, 4) // 20px vertical padding
    }

    @Test
    fun `test initialize grid tracks`() {
        val px0 = LengthPercentage.Length(0f)
        val px20 = LengthPercentage.Length(20f)
        val px100 = LengthPercentage.Length(100f)

        // Setup test
        val trackTemplate = vec(length(100f), minmax(length(100f), fr(2f)), fr(1f))
        val trackCounts = TrackCounts(negativeImplicit = 3, explicit = trackTemplate.len(), positiveImplicit = 3)
        val autoTracks = vec<NonRepeatedTrackSizingFunction>(auto(), length(100f))
        val gap = px20

        // Call function
        val tracks = vec<GridTrack>()
        initializeGridTracks(tracks, trackCounts, trackTemplate, autoTracks, gap) { false }

        // Assertions
        val expected = vec(
            // Gutter
            T3(GridTrackKind.GUTTER, MinTrackSizingFunction.Fixed(px0), MaxTrackSizingFunction.Fixed(px0)),
            // Negative implicit tracks
            T3(GridTrackKind.TRACK, MinTrackSizingFunction.Fixed(px100), MaxTrackSizingFunction.Fixed(px100)),
            T3(GridTrackKind.GUTTER, MinTrackSizingFunction.Fixed(px20), MaxTrackSizingFunction.Fixed(px20)),
            T3(GridTrackKind.TRACK, MinTrackSizingFunction.Auto, MaxTrackSizingFunction.Auto),
            T3(GridTrackKind.GUTTER, MinTrackSizingFunction.Fixed(px20), MaxTrackSizingFunction.Fixed(px20)),
            T3(GridTrackKind.TRACK, MinTrackSizingFunction.Fixed(px100), MaxTrackSizingFunction.Fixed(px100)),
            T3(GridTrackKind.GUTTER, MinTrackSizingFunction.Fixed(px20), MaxTrackSizingFunction.Fixed(px20)),
            // Explicit tracks
            T3(GridTrackKind.TRACK, MinTrackSizingFunction.Fixed(px100), MaxTrackSizingFunction.Fixed(px100)),
            T3(GridTrackKind.GUTTER, MinTrackSizingFunction.Fixed(px20), MaxTrackSizingFunction.Fixed(px20)),
            T3(GridTrackKind.TRACK, MinTrackSizingFunction.Fixed(px100), MaxTrackSizingFunction.Fraction(2f)),
            // Note: separate min-max functions
            T3(GridTrackKind.GUTTER, MinTrackSizingFunction.Fixed(px20), MaxTrackSizingFunction.Fixed(px20)),
            T3(GridTrackKind.TRACK, MinTrackSizingFunction.Auto, MaxTrackSizingFunction.Fraction(1f)),
            // Note: min sizing function of flex sizing functions is auto
            T3(GridTrackKind.GUTTER, MinTrackSizingFunction.Fixed(px20), MaxTrackSizingFunction.Fixed(px20)),
            // Positive implicit tracks
            T3(GridTrackKind.TRACK, MinTrackSizingFunction.Auto, MaxTrackSizingFunction.Auto),
            T3(GridTrackKind.GUTTER, MinTrackSizingFunction.Fixed(px20), MaxTrackSizingFunction.Fixed(px20)),
            T3(GridTrackKind.TRACK, MinTrackSizingFunction.Fixed(px100), MaxTrackSizingFunction.Fixed(px100)),
            T3(GridTrackKind.GUTTER, MinTrackSizingFunction.Fixed(px20), MaxTrackSizingFunction.Fixed(px20)),
            T3(GridTrackKind.TRACK, MinTrackSizingFunction.Auto, MaxTrackSizingFunction.Auto),
            T3(GridTrackKind.GUTTER, MinTrackSizingFunction.Fixed(px0), MaxTrackSizingFunction.Fixed(px0)),
        )

        assertEq(tracks.len(), expected.len(), "Number of tracks doesn't match")

        for ((idx, sec) in tracks.zip(expected).enumerate()) {
            val actual = sec.first
            val (kind, min, max) = sec.second

            assertEq(actual.kind, kind, "Track $idx (0-based index)")
            assertEq(actual.minTrackSizingFunction, min, "Track $idx (0-based index)")
            assertEq(actual.maxTrackSizingFunction, max, "Track $idx (0-based index)")
        }
    }
}
