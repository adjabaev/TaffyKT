package be.arby.taffy.test.gentest;

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject
import com.eclipsesource.json.JsonValue
import net.asterium.taffy.lang.Option
import org.apache.commons.text.CaseUtils
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.chrome.ChromeDriverService
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.remote.RemoteWebDriver
import java.io.File
import java.net.URL
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.*

private var browser: RemoteWebDriver? = null

fun main() {
    val fixturesRoot = File("test_fixtures")
    val outputRoot = File("src/test/kotlin/net/asterium/taffy/test/fixtures")

    println("reading test fixtures from disk")

    var fixtures: List<Pair<String, File>>? = fixturesRoot.listFiles()
        ?.filter { f -> !f.name.startsWith("x") }
        ?.filter { f -> f.extension == "html" }
        ?.map { f -> Pair(f.nameWithoutExtension, f) }

    if (fixtures == null) {
        println("cancelling test generation as no fixtures were found")
        return
    }

    // val f = File("test_fixtures/grid_fit_content_points_min_content.html")
    // fixtures = listOf(Pair("grid_fit_content_points_min_content", f))

    println("starting webdriver instance")
    val service = ChromeDriverService.Builder().usingPort(4444).build()
    service.start()

    println("spawning webdriver client and collecting test descriptions")
    val chromeOptions = ChromeOptions()
    chromeOptions.addArguments("--headless", "--disable-gpu", "--whitelisted-ips")
    browser = RemoteWebDriver(URL("http://localhost:4444"), chromeOptions)

    val testDescs = ArrayList<Pair<String, JsonValue>>()
    for ((name, file) in fixtures) {
        testDescs.add(testRootElement(name, file))
    }

    println("killing webdriver instance...")
    browser!!.quit()
    service.stop()

    println("generating test sources and concatenating...")

    outputRoot.mkdirs()

    val testData: List<Pair<String, String>> = testDescs
        .map { (name, description) ->
            println("generating test contents for $name")
            Pair(name, generateTest(name, description))
        }

    testData.forEach { (file, data) ->
        val txt = data.replace("#className", CaseUtils.toCamelCase(file, true, '_'))
            .replace(
                "#testName", CaseUtils.toCamelCase(file, true, ' ')
                    .replace("_", " ")
            )
        val nm = CaseUtils.toCamelCase(file, true, '_')
        val f = File(outputRoot, "$nm.kt")
        Files.writeString(f.toPath(), txt, StandardCharsets.UTF_8)
    }
}

fun testRootElement(name: String, file: File): Pair<String, JsonValue> {
    val url = java.lang.String.format("file://%s", file.absolutePath)

    println(url)

    browser!!.navigate().to(url)
    val js = browser as JavascriptExecutor
    val result =
        js.executeScript("return JSON.stringify(describeElement(document.getElementById('test-root')))") as String
    return Pair(name, Json.parse(result).asObject())
}

fun generateTest(name: String, description: JsonValue): String {
    val nodeDescription = generateNode("node", description)
    var assertions = generateAssertions("node", description)

    val builder = """
        package net.asterium.taffy.test.fixtures
        
        import net.asterium.taffy.Taffy
        import net.asterium.taffy.geometry.*
        import net.asterium.taffy.style.*
        import net.asterium.taffy.style.dimension.*
        import net.asterium.taffy.style.enums.*
        import net.asterium.taffy.style.flex.*
        import net.asterium.taffy.style.grid.*
        import net.asterium.taffy.utils.StyleHelper.Companion.line
        import net.asterium.taffy.utils.StyleHelper.Companion.span
        import net.asterium.taffy.utils.StyleHelper.Companion.auto
        import net.asterium.taffy.test.*
        import net.asterium.taffy.utils.*
        import net.asterium.taffy.lang.Option
        import org.junit.jupiter.api.*
        
        class #className {
            @Test
            fun `#testName`() {
                val taffy = Taffy()
                #nodeDescription
                
                taffy.computeLayout(node, Size.MAX_CONTENT)
                
                println("\nComputed tree:")
                Debug.printTree(taffy, node)
                println("")
                
                $assertions
            }
        }
        
    """.trimIndent()

    return builder.replace("#nodeDescription", nodeDescription)
}

fun generateAssertions(ident: String, node: JsonValue): String {
    val o = node.asObject()
    val layout = o["layout"].asObject()

    val readF32 = { s: String -> layout[s].asFloat() }
    val width = readF32("width")
    val height = readF32("height")
    val x = readF32("x")
    val y = readF32("y")
    val scrollWidth = readF32("scrollWidth")
    val scrollHeight = readF32("scrollHeight")

    val c = ArrayList<String>()
    if (o.isArray("children")) {
        val a = o.get("children").asArray()
        for ((idx, child) in a.withIndex()) {
            c.add(generateAssertions("${ident}$idx", child))
        }
    }
    var children = c.fold("") { a, b -> "$a $b" }
    val ix = ident.substring(4)

    return """val (location$ix, size$ix) = Pair($ident.data.layout.location, $ident.data.layout.size)
                Assertions.assertEquals(${width}f, size$ix.width, "width of node $ident. Expected ${width}f. Actual ${"$"}{size$ix.width}f")
                Assertions.assertEquals(${height}f, size$ix.height, "height of node $ident. Expected ${height}f. Actual ${"$"}{size$ix.height}f")
                Assertions.assertEquals(${x}f, location$ix.x, "x of node $ident. Expected ${x}f. Actual ${"$"}{location$ix.x}f")
                Assertions.assertEquals(${y}f, location$ix.y, "y of node $ident. Expected ${y}f. Actual ${"$"}{location$ix.y}f")

               $children
    """.trimIndent()
}

fun generateNode(ident: String, node: JsonValue): String {
    val o = node.asObject();
    val jo = o["style"].asObject()

    fun quoteObjectValue(propName: String, style: JsonObject, quoter: (JsonObject) -> String): Option<String> {
        return if (style.isObject(propName)) {
            val v = style[propName]
            Option.Some(quoter.invoke(v.asObject()))
        } else {
            Option.None
        }
    }

    fun quoteProp(propName: String, value: String): String {
        return "$propName = $value"
    }

    fun quoteObjectProp(propName: String, style: JsonObject, quoter: (JsonObject) -> String): String {
        val p = quoteObjectValue(propName, style, quoter)
        return when {
            p.isSome() -> quoteProp(propName, p.unwrap())
            else -> ""
        }
    }

    fun quoteArrayProp(propName: String, style: JsonObject, quoter: (Array<JsonValue>) -> String): String {
        return if (style.isArray(propName)) {
            val b = style[propName]
            val propValue = quoter(b.asArray().values().toTypedArray())
            quoteProp(propName, propValue)
        } else {
            ""
        }
    }

    fun getStringValue(propName: String, style: JsonObject): Option<String> {
        return if (style.isString(propName)) {
            val b = style[propName]
            Option.Some(b.asString())
        } else {
            Option.None
        }
    }

    fun getNumberValue(propName: String, style: JsonObject): Option<Float> {
        return if (style.isNumber(propName)) {
            val b = style[propName]
            Option.Some(b.asFloat())
        } else {
            Option.None
        }
    }

    fun quoteNumberProp(propName: String, style: JsonObject, quoter: (Float) -> String): String {
        return if (style.isNumber(propName)) {
            val b = style[propName]
            quoteProp(propName, quoter(b.asFloat()))
        } else {
            ""
        }
    }

    val display = if (jo.isString("display")) {
        when (jo["display"].asString()) {
            "none" -> "display = Display.NONE"
            "grid" -> "display = Display.GRID"
            else -> "display = Display.FLEX"
        }
    } else {
        ""
    }

    val position = if (jo.isString("position")) {
        when (jo["position"].asString()) {
            "absolute" -> "position = Position.ABSOLUTE"
            else -> ""
        }
    } else {
        ""
    }

    val direction = if (jo.isString("direction")) {
        when (jo["direction"].asString()) {
            "rtl" -> "direction = Direction.RTL"
            "ltr" -> "direction = Direction.LTR"
            else -> ""
        }
    } else {
        ""
    }

    val flexDirection = if (jo.isString("flexDirection")) {
        when (jo["flexDirection"].asString()) {
            "row-reverse" -> "flexDirection = FlexDirection.ROW_REVERSE"
            "column" -> "flexDirection = FlexDirection.COLUMN"
            "column-reverse" -> "flexDirection = FlexDirection.COLUMN_REVERSE"
            else -> ""
        }
    } else {
        ""
    }

    val flexWrap = if (jo.isString("flexWrap")) {
        when (jo["flexWrap"].asString()) {
            "wrap" -> "flexWrap = FlexWrap.WRAP"
            "wrap-reverse" -> "flexWrap = FlexWrap.WRAP_REVERSE"
            else -> ""
        }
    } else {
        ""
    }

    val overflow = if (jo.isString("overflow")) {
        when (jo["overflow"].asString()) {
            "hidden" -> "overflow = Overflow.HIDDEN"
            "scroll" -> "overflow = Overflow.SCROLL"
            else -> ""
        }
    } else {
        ""
    }

    val alignItems = if (jo.isString("alignItems")) {
        when (jo["alignItems"].asString()) {
            "flex-start" -> "alignItems = Option.Some(AlignItems.FLEX_START)"
            "start" -> "alignItems = Option.Some(AlignItems.START)"
            "flex-end" -> "alignItems = Option.Some(AlignItems.FLEX_END)"
            "end" -> "alignItems = Option.Some(AlignItems.END)"
            "center" -> "alignItems = Option.Some(AlignItems.CENTER)"
            "baseline" -> "alignItems = Option.Some(AlignItems.BASELINE)"
            "stretch" -> "alignItems = Option.Some(AlignItems.STRETCH)"
            else -> ""
        }
    } else {
        ""
    }

    val alignSelf = if (jo.isString("alignSelf")) {
        when (jo["alignSelf"].asString()) {
            "flex-start" -> "alignSelf = Option.Some(AlignSelf.FLEX_START)"
            "start" -> "alignSelf = Option.Some(AlignSelf.START)"
            "flex-end" -> "alignSelf = Option.Some(AlignSelf.FLEX_END)"
            "end" -> "alignSelf = Option.Some(AlignSelf.END)"
            "center" -> "alignSelf = Option.Some(AlignSelf.CENTER)"
            "baseline" -> "alignSelf = Option.Some(AlignSelf.BASELINE)"
            "stretch" -> "alignSelf = Option.Some(AlignSelf.STRETCH)"
            else -> ""
        }
    } else {
        ""
    }

    val justifyItems = if (jo.isString("justifyItems")) {
        when (jo["justifyItems"].asString()) {
            "flex-start", "start" -> "justifyItems = Option.Some(JustifyItems.START)"
            "flex-end", "end" -> "justifyItems = Option.Some(JustifyItems.END)"
            "center" -> "justifyItems = Option.Some(JustifyItems.CENTER)"
            "baseline" -> "justifyItems = Option.Some(JustifyItems.BASELINE)"
            "stretch" -> "justifyItems = Option.Some(JustifyItems.STRETCH)"
            else -> ""
        }
    } else {
        ""
    }

    val justifySelf = if (jo.isString("justifySelf")) {
        when (jo["justifySelf"].asString()) {
            "flex-start" -> "justifySelf = Option.Some(JustifySelf.FLEX_START)"
            "start" -> "justifySelf = Option.Some(JustifySelf.START)"
            "flex-end" -> "justifySelf = Option.Some(JustifySelf.FLEX_END)"
            "end" -> "justifySelf = Option.Some(JustifySelf.END)"
            "center" -> "justifySelf = Option.Some(JustifySelf.CENTER)"
            "baseline" -> "justifySelf = Option.Some(JustifySelf.BASELINE)"
            "stretch" -> "justifySelf = Option.Some(JustifySelf.STRETCH)"
            else -> ""
        }
    } else {
        ""
    }

    val alignContent = if (jo.isString("alignContent")) {
        when (jo["alignContent"].asString()) {
            "flex-start" -> "alignContent = Option.Some(AlignContent.FLEX_START)"
            "start" -> "alignContent = Option.Some(AlignContent.START)"
            "flex-end" -> "alignContent = Option.Some(AlignContent.FLEX_END)"
            "end" -> "alignContent = Option.Some(AlignContent.END)"
            "center" -> "alignContent = Option.Some(AlignContent.CENTER)"
            "stretch" -> "alignContent = Option.Some(AlignContent.STRETCH)"
            "space-between" -> "alignContent = Option.Some(AlignContent.SPACE_BETWEEN)"
            "space-around" -> "alignContent = Option.Some(AlignContent.SPACE_AROUND)"
            "space-evenly" -> "alignContent = Option.Some(AlignContent.SPACE_EVENLY)"
            else -> ""
        }
    } else {
        ""
    }

    val justifyContent = if (jo.isString("justifyContent")) {
        when (jo["justifyContent"].asString()) {
            "flex-start" -> "justifyContent = Option.Some(JustifyContent.FLEX_START)"
            "start" -> "justifyContent = Option.Some(JustifyContent.START)"
            "flex-end" -> "justifyContent = Option.Some(JustifyContent.FLEX_END)"
            "end" -> "justifyContent = Option.Some(JustifyContent.END)"
            "center" -> "justifyContent = Option.Some(JustifyContent.CENTER)"
            "stretch" -> "justifyContent = Option.Some(JustifyContent.STRETCH)"
            "space-between" -> "justifyContent = Option.Some(JustifyContent.SPACE_BETWEEN)"
            "space-around" -> "justifyContent = Option.Some(JustifyContent.SPACE_AROUND)"
            "space-evenly" -> "justifyContent = Option.Some(JustifyContent.SPACE_EVENLY)"
            else -> ""
        }
    } else {
        ""
    }

    val flexGrow = quoteNumberProp("flexGrow", jo) { value: Float -> "${value}f" }
    val flexShrink = quoteNumberProp("flexShrink", jo) { value: Float -> "${value}f" }

    val flexBasis = quoteObjectProp("flexBasis", jo, ::generateDimension)
    val size = quoteObjectProp("size", jo, ::generateSize)
    val minSize = quoteObjectProp("minSize", jo, ::generateSize)
    val maxSize = quoteObjectProp("maxSize", jo, ::generateSize)
    val aspectRatio = quoteNumberProp("aspectRatio", jo) { value: Float -> "Option.Some(${value}f)" }

    val gap = quoteObjectProp("gap", jo, ::generateGap);

    val gridTemplateRows = quoteArrayProp("gridTemplateRows", jo, ::generateTrackDefinitionList)
    val gridTemplateColumns = quoteArrayProp("gridTemplateColumns", jo, ::generateTrackDefinitionList)
    val gridAutoRows = quoteArrayProp("gridAutoRows", jo, ::generateTrackDefinitionList)
    val gridAutoColumns = quoteArrayProp("gridAutoColumns", jo, ::generateTrackDefinitionList)
    val gridAutoFlow = quoteObjectProp("gridAutoFlow", jo, ::generateGridAutoFlow)

    val defaultGridPlacement = "auto()"
    val gridRowStart = quoteObjectValue("gridRowStart", jo, ::generateGridPosition)
    val gridRowEnd = quoteObjectValue("gridRowEnd", jo, ::generateGridPosition)
    val gridRow = if (gridRowStart.isSome() || gridRowEnd.isSome()) {
        quoteProp(
            "gridRow",
            generateLine(gridRowStart.unwrapOr(defaultGridPlacement), gridRowEnd.unwrapOr(defaultGridPlacement)),
        )
    } else {
        ""
    }

    val gridColumnStart = quoteObjectValue("gridColumnStart", jo, ::generateGridPosition)
    val gridColumnEnd = quoteObjectValue("gridColumnEnd", jo, ::generateGridPosition)
    val gridColumn = if (gridColumnStart.isSome() || gridColumnEnd.isSome()) {
        quoteProp(
            "gridColumn",
            generateLine(gridColumnStart.unwrapOr(defaultGridPlacement), gridColumnEnd.unwrapOr(defaultGridPlacement)),
        )
    } else {
        ""
    }

    val textContent = getStringValue("textContent", o)
    val writingMode = getStringValue("writingMode", jo)
    val rawAspectRatio = getNumberValue("aspectRatio", jo)

    val measureFunc = textContent.map { text -> generateMeasureFunction(text, writingMode, rawAspectRatio) }

    val margin = edgesQuoted(jo, "margin", ::generateLengthPercentageAuto, "LengthPercentageAuto.Points(0f)")
    val padding = edgesQuoted(jo, "padding", ::generateLengthPercentage, "LengthPercentage.Points(0f)")
    val border = edgesQuoted(jo, "border", ::generateLengthPercentage, "LengthPercentage.Points(0f)")
    val inset = edgesQuoted(jo, "inset", ::generateLengthPercentageAuto, "LengthPercentageAuto.Auto")

    // Quote children
    val childDescriptions: List<JsonValue> = when {
        o.isArray("children") -> o["children"].asArray().toList()
        else -> listOf()
    }
    val hasChildren = childDescriptions.isNotEmpty()
    val (childrenBody: List<String>, children: List<String>) = if (hasChildren) {
        val body = childDescriptions.withIndex().map { (i, child) -> generateNode("${ident}$i", child) }
        val idents = childDescriptions.withIndex().map { (i, _) -> "${ident}$i" }
        Pair(body, idents)
    } else {
        Pair(ArrayList(), ArrayList())
    }

    val styles = listOf(
        display,
        direction,
        position,
        flexDirection,
        flexWrap,
        overflow,
        alignItems,
        alignSelf,
        justifyItems,
        justifySelf,
        alignContent,
        justifyContent,
        flexGrow,
        flexShrink,
        flexBasis,
        gap,
        gridTemplateRows,
        gridTemplateColumns,
        gridAutoRows,
        gridAutoColumns,
        gridAutoFlow,
        gridRow,
        gridColumn,
        size,
        minSize,
        maxSize,
        aspectRatio,
        margin,
        padding,
        inset,
        border
    )

    var mergedStyles = styles.filter { s -> s.isNotEmpty() }.joinToString(",\n            ")
    if (mergedStyles.isNotEmpty()) {
        mergedStyles = "\n            $mergedStyles\n        "
    }

    val style = "Style($mergedStyles)"

    return if (hasChildren) {
        val bd = childrenBody.joinToString("\n        ")
        val ch = children.joinToString(", ")
        """$bd
        val $ident = taffy.newLeafWithChildren($style, listOf($ch))
        """.trimIndent()
    } else if (measureFunc.isSome()) {
        "val $ident = taffy.newLeafWithMeasure($style) { ${measureFunc.unwrap()}"
    } else {
        "val $ident = taffy.newLeaf($style)"
    }
}

fun generateSize(size: JsonObject): String {
    val w = dimQuoted(size, "width", ::generateDimension, "Dimension.Auto")
    val h = dimQuoted(size, "height", ::generateDimension, "Dimension.Auto")
    return "Size($w, $h)"
}

fun generateGap(size: JsonObject): String {
    val w = dimQuotedRenamed(size, "column", "width", ::generateLengthPercentage, "LengthPercentage.Points(0f)")
    val h = dimQuotedRenamed(size, "row", "height", ::generateLengthPercentage, "LengthPercentage.Points(0f)")
    return "Size($w, $h)"
}

fun generateLengthPercentage(dimen: JsonObject): String {
    val unit = dimen["unit"]
    val value = if (dimen.isNumber("value")) dimen["value"].asFloat() else 0f

    return when (unit.asString()) {
        "points" -> "LengthPercentage.Points(${value}f)"
        "percent" -> "LengthPercentage.Percent(${value}f)"
        else -> throw UnsupportedOperationException("unreachable")
    }
}

fun generateLengthPercentageAuto(dimen: JsonObject): String {
    val unit = dimen["unit"]
    val value = if (dimen.isNumber("value")) dimen["value"].asFloat() else 0f

    return when (unit.asString()) {
        "auto" -> "LengthPercentageAuto.Auto"
        "points" -> "LengthPercentageAuto.Points(${value}f)"
        "percent" -> "LengthPercentageAuto.Percent(${value}f)"
        else -> throw UnsupportedOperationException("unreachable")
    }
}

fun generateDimension(dimen: JsonObject): String {
    val unit = dimen["unit"]
    val value = if (dimen.isNumber("value")) dimen["value"].asFloat() else 0f

    return when (unit.asString()) {
        "auto" -> "Dimension.Auto"
        "points" -> "Dimension.Points(${value}f)"
        "percent" -> "Dimension.Percent(${value}f)"
        else -> throw UnsupportedOperationException("unreachable")
    }
}

fun generateGridAutoFlow(autoFlow: JsonObject): String {
    val direction = autoFlow["direction"].asString()
    val algorithm = autoFlow["algorithm"].asString()

    return when (Pair(direction, algorithm)) {
        Pair("row", "sparse") -> "GridAutoFlow.ROW"
        Pair("column", "sparse") -> "GridAutoFlow.COLUMN"
        Pair("row", "dense") -> "GridAutoFlow.ROW_DENSE"
        Pair("column", "dense") -> "GridAutoFlow.COLUMN_DENSE"
        else -> ""
    }
}

fun generateLine(start: String, end: String): String {
    return "Line(start = $start, end = $end)"
}

fun generateGridPosition(gridPosition: JsonObject): String {
    val kind = gridPosition["kind"]
    val value = gridPosition["value"]

    return when (kind.asString()) {
        "auto" -> "auto()"
        "span" -> "span(%s)".format(value.asInt())
        "line" -> "line(%s)".format(value.asInt())
        else -> ""
    }
}

fun generateTrackDefinitionList(vararg rawList: JsonValue): String {
    val list = rawList.map { obj ->
        when {
            obj.isObject -> generateTrackDefinition(obj.asObject())
            else -> throw UnsupportedOperationException("unreachable")
        }
    }

    val ch = list.joinToString(", ")
    return "listOf($ch)"
}

fun generateTrackDefinition(trackDefinition: JsonObject): String {
    val kind = trackDefinition["kind"].asString()
    val name = { trackDefinition["name"].asString() }
    val arguments = { trackDefinition.get("arguments").asArray() }

    return when (kind) {
        "scalar" -> generateScalarDefinition(trackDefinition)
        "function" -> {
            val (name, arguments) = Pair(name(), arguments())
            when (name) {
                "fit-content" -> {
                    if (arguments.size() != 1) {
                        throw IllegalArgumentException("fit-content function with the wrong number of arguments")
                    }
                    val arg = arguments.get(0)
                    val argument = when {
                        arg.isObject -> generateScalarDefinition(arg.asObject())
                        else -> throw UnsupportedOperationException("unreachable")
                    }
                    "StyleHelper.fitContent($argument)"
                }

                // TODO: Add support for fit-content
                "minmax" -> {
                    if (arguments.size() != 2) {
                        throw IllegalArgumentException("minmax function with the wrong number of arguments")
                    }
                    var arg = arguments.get(0)
                    val min = when {
                        arg.isObject -> generateScalarDefinition(arg.asObject())
                        else -> throw UnsupportedOperationException("unreachable")
                    }
                    arg = arguments.get(1)
                    val max = when {
                        arg.isObject -> generateScalarDefinition(arg.asObject())
                        else -> throw UnsupportedOperationException("unreachable")
                    }
                    "StyleHelper.minmax($min, $max)"
                }

                "repeat" -> {
                    if (arguments.size() < 2) {
                        throw IllegalArgumentException("repeat function with the wrong number of arguments")
                    }
                    var arg = arguments.get(0)
                    val repetition = when {
                        arg.isObject -> {
                            arg = arg.asObject()
                            val unit = arg["unit"].asString()
                            val value = if (arg.isNumber("value")) arg["value"].asLong() else 0

                            when (unit) {
                                "auto-fill" -> "GridTrackRepetition.AutoFill"
                                "auto-fit" -> "GridTrackRepetition.AutoFit"
                                "integer" -> {
                                    "GridTrackRepetition.Count(${value})"
                                }

                                else -> throw UnsupportedOperationException("unreachable")
                            }
                        }

                        else -> throw UnsupportedOperationException("unreachable")
                    }
                    val trackList = generateTrackDefinitionList(*arguments.toList().drop(1).toTypedArray())
                    "StyleHelper.repeat($repetition, $trackList)"
                }

                else -> throw UnsupportedOperationException("unreachable")
            }
        }

        else -> throw UnsupportedOperationException("unreachable")
    }
}

fun generateScalarDefinition(trackDefinition: JsonObject): String {
    val unit = { trackDefinition.get("unit").asString() }
    val value = { trackDefinition.get("value").asFloat() }

    return when (unit()) {
        "auto" -> "StyleHelper.auto()"
        "min-content" -> "StyleHelper.minContent()"
        "max-content" -> "StyleHelper.maxContent()"
        "points", "percent", "fraction" -> {
            val value = value()
            when (unit()) {
                "points" -> "StyleHelper.points(${value}f)"
                "percent" -> "StyleHelper.percent(${value}f)"
                "fraction" -> "StyleHelper.fraction(${value}f)"
                else -> throw UnsupportedOperationException("unreachable")
            }
        }

        else -> throw UnsupportedOperationException("unreachable")
    }
}

fun generateMeasureFunction(textContent: String, writingMode: Option<String>, aspectRatio: Option<Float>): String {
    val writingModeToken = if (writingMode.isSome()) {
        when (writingMode.unwrap()) {
            "vertical-rl", "vertical-lr" -> "WritingMode.VERTICAL"
            else -> "WritingMode.HORIZONTAL"
        }
    } else {
        "WritingMode.HORIZONTAL"
    }

    val aspectRatioToken = when {
        aspectRatio.isSome() -> "Option.Some(${aspectRatio.unwrap()}f)"
        else -> "Option.None"
    }

    return """knownDimensions, availableSpace -> 
            val text = "${textContent.replace("\n", "\\n")}"
            FixtureUtils.measureStandardText(knownDimensions, availableSpace, text, $writingModeToken, $aspectRatioToken)
        }""".trimIndent()
}

fun dimQuotedRenamed(
    obj: JsonObject,
    inName: String,
    outName: String,
    valueMapper: (JsonObject) -> String,
    default: String
): String {
    val o: JsonValue? = obj[inName]
    return when {
        (o != null && o.isObject) -> {
            val dim = valueMapper(o.asObject())
            return "$outName = $dim"
        }

        else -> return "$outName = $default"
    }
}

fun dimQuoted(obj: JsonObject, dimName: String, valueMapper: (JsonObject) -> String, default: String): String {
    return dimQuotedRenamed(obj, dimName, dimName, valueMapper, default)
}

fun edgesQuoted(style: JsonObject, value: String, valueMapper: (JsonObject) -> String, defaultValue: String): String {
    val o: JsonValue? = style[value]
    return when {
        (o != null && o.isObject) -> {
            val ob = o.asObject()
            val left = dimQuoted(ob, "left", valueMapper, defaultValue)
            val right = dimQuoted(ob, "right", valueMapper, defaultValue)
            val top = dimQuoted(ob, "top", valueMapper, defaultValue)
            val bottom = dimQuoted(ob, "bottom", valueMapper, defaultValue)

            val edges = "Rect($left, $right, $top, $bottom)"

            "$value = $edges"
        }

        else -> ""
    }
}

fun JsonObject.isString(key: String): Boolean {
    return this[key] != null && this[key].isString
}

fun JsonObject.isNumber(key: String): Boolean {
    return this[key] != null && this[key].isNumber
}

fun JsonObject.isObject(key: String): Boolean {
    return this[key] != null && this[key].isObject
}

fun JsonObject.isArray(key: String): Boolean {
    return this[key] != null && this[key].isArray
}
