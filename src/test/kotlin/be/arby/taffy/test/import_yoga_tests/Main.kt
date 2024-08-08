package be.arby.taffy.test.import_yoga_tests

import java.io.File
import java.nio.file.Files
import java.nio.file.Path

data class YogaFixture(val fileName: Path, val name: String, val content: String) {
}

/*
 * TODO: Convert script over here:
 * https://github.com/DioxusLabs/taffy/blob/main/scripts/import-yoga-tests/src/main.rs
 * to kotlin
 */
fun main() {
    val scriptRootDir = File("scripts/import-yoga-tests")
    val taffyFixturesDir = File("test_fixtures")

    val taffyFixtureNames = taffyFixturesDir.listFiles()
        ?.map { f ->
            val fileName = f.nameWithoutExtension
            if (fileName.startsWith("x")) {
                return@map fileName.substring(1)
            } else {
                fileName
            }
        }?.toList() ?: return

    // Get Taffy fixture template
    val fixtureTemplatePath = File(scriptRootDir, "FIXTURE_TEMPLATE.html")
    val fixtureTemplate = Files.readString(fixtureTemplatePath.toPath(), Charsets.UTF_8)

    // Get Yoga fixtures dir
    val yogaFixturesDir = System.getenv("YOGA_FIXTURE_DIR")
    if (yogaFixturesDir == null || yogaFixturesDir.isEmpty()) {
        return
    }
    val fixturesFileList = File(yogaFixturesDir)

    val yogaFixtures = fixturesFileList.listFiles()
        ?.filter { f -> f.isFile && f.extension == "html" }
        ?.flatMap { f ->
            val path = f.toPath()
            val fileContent = Files.readString(path, Charsets.UTF_8)

            fileContent.split("\n\n").map { sn ->
                val snippet = sn.trim()
                val name = snippet.substring(snippet.indexOf('"') + 1, snippet.lastIndexOf('"'))
                val nameReplaceSnippet = snippet.replace(name, "test-root")

                val taffyName = name.replace("row_gap", "gap_row_gap")
                    .replace("column_gap", "gap_column_gap")

                YogaFixture(fileName = path, name = taffyName, content = nameReplaceSnippet)
            }.toList()
        }?.toList() ?: return

    for (fixture in yogaFixtures) {
        if (!taffyFixtureNames.contains(fixture.name)) {
            val newFixturePath = File(taffyFixturesDir, "${fixture.name}.html")
            val newFixtureContent = fixtureTemplate.replace("__HTML_GOES_HERE__", fixture.content)

            println("Writing new fixture ${fixture.name}")
            Files.writeString(newFixturePath.toPath(), newFixtureContent, Charsets.UTF_8);
        }
    }
}
