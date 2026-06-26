package com.enoch02

import it.skrape.core.htmlDocument
import it.skrape.fetcher.BrowserFetcher
import it.skrape.fetcher.response
import it.skrape.fetcher.skrape
import it.skrape.selects.html5.div
import it.skrape.selects.html5.h1
import it.skrape.selects.html5.img
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.File
import java.net.URI
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

val HEADERS =
    mapOf("User-Agent" to "Mozilla/5.0 (Macintosh; Intel Mac OS X 15_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36")

//TODO: implement a frontend for this (with kobweb) that allows one to input a URL and download manga
fun main() {
    println("Enter the link to a manga to continue: ")
    val mangaURL = readlnOrNull()

    if (mangaURL == null) {
        println("Invalid Input. Exiting...")
    } else if (!mangaURL.contains("https://mangakatana.com")) {
        println("Invalid URL.\nA valid URL should contain at least `https://mangakatana.com`.\nExiting...")
    } else {
        println("Starting...")
        val mangaData = getMangaData(mangaURL)
        val chapterList = mangaData.chapters.toList()

        if (chapterList.isEmpty()) {
            println("No chapters found. Exiting...")
            return
        }

        val totalChapters = chapterList.size
        println("\nFound $totalChapters chapters.")

        if (totalChapters <= 10) {
            chapterList.forEachIndexed { index, (name, _) ->
                println("[${index + 1}] $name")
            }
        } else {
            println("\nNewest Chapters:")
            chapterList.take(5).forEachIndexed { index, (name, _) ->
                println("[${index + 1}] $name")
            }
            println("...")
            println("Oldest Chapters:")
            chapterList.takeLast(5).forEachIndexed { index, (name, _) ->
                val actualIndex = totalChapters - 5 + index
                println("[${actualIndex + 1}] $name")
            }

            print("\nDo you want to see all chapters? (y/N): ")
            val showAll = readlnOrNull()?.trim()?.lowercase() ?: ""
            if (showAll == "y" || showAll == "yes") {
                println("\nAll Chapters:")
                chapterList.forEachIndexed { index, (name, _) ->
                    println("[${index + 1}] $name")
                }
            }
        }

        while (true) {
            println("\nEnter chapter indices/ranges to download (e.g., '1-5, 8, 12-15' or 'all'): ")
            val choiceInput = readlnOrNull()
            if (choiceInput.isNullOrBlank()) {
                println("No input provided. Exiting...")
                return
            }
            val selectedIndices = parseIndices(choiceInput, totalChapters)
            if (selectedIndices.isEmpty()) {
                println("Invalid input or no chapters match. Please try again.")
                continue
            }

            println("Selected ${selectedIndices.size} chapter(s) for download.")
            val chaptersToDownload = selectedIndices.sorted().associate { index ->
                chapterList[index - 1]
            }

            runBlocking { downloadChapters(mangaData.title, chaptersToDownload) }
            break
        }
    }
}

data class MangaData(val title: String, val chapters: Map<String, String>)

fun getMangaData(mangaURL: String): MangaData {
    var mangaTitle = ""
    val chapters = skrape(BrowserFetcher) {
        request {
            url = mangaURL
            headers = HEADERS
        }

        response {
            htmlDocument {
                h1 {
                    withClass = "heading"

                    findFirst { mangaTitle = text }
                }

                findFirst("div.chapters") {
                    findAll("div.chapter a")
                        .associate { element ->
                            val name = element.text.trim()
                            val url = element.attribute("href")
                            name to url
                        }
                }
            }
        }
    }

    return MangaData(mangaTitle, chapters)
}

suspend fun downloadChapters(
    title: String,
    chapters: Map<String, String>,
    destination: String = "${System.getProperty("user.home")}/Downloads/Manga"
) {
    val safeTitle = title
        .replace(Regex("""[^\w\- ]"""), "")
        .replace(" ", "_")
    val mangaFolder = File(if (title.isNotBlank()) "$destination/$safeTitle" else destination)
    if (!mangaFolder.exists()) {
        mangaFolder.mkdirs()
    }

    chapters.forEach { (name, chapterURL) ->
        val pageURLs = mutableListOf<String>()

        skrape(BrowserFetcher) {
            request {
                url = chapterURL
                headers = HEADERS
            }

            response {
                htmlDocument {
                    div {
                        withId = "imgs"

                        findFirst {
                            img {
                                findAll {
                                    forEach {
                                        // TODO: add support for the alt src
                                        val imgUrl = it.attribute("data-src")
                                        pageURLs.add(imgUrl)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        println("Downloading $name")
        downloadChapterCBZ(name, pageURLs, mangaFolder)
        delay(1000)
    }
}

fun downloadChapterCBZ(chapterName: String, pageURLs: List<String>, outputFolder: File) {
    val safeName = chapterName
        .replace(Regex("""[^\w\- ]"""), "")
        .replace(" ", "_")
    val cbzFile = File(outputFolder, "$safeName.cbz")

    if (cbzFile.exists()) {
        println("$chapterName seems to have been downloaded. Skipping...")
        return
    }

    try {
        ZipOutputStream(cbzFile.outputStream().buffered()).use { zipOut ->
            val total = pageURLs.size

            pageURLs.forEachIndexed { index, url ->
                val pageNumber = index + 1
                val percent = ((pageNumber.toDouble() / total) * 100).toInt()

                println("Downloading page $pageNumber/$total ($percent%)")

                val imageBytes = URI.create(url).toURL().openStream().use { it.readBytes() }
                val fileName = "page_${pageNumber.toString().padStart(3, '0')}.jpg"

                zipOut.putNextEntry(ZipEntry(fileName))
                zipOut.write(imageBytes)
                zipOut.closeEntry()
            }

            println("CBZ created: ${cbzFile.absolutePath}")
        }
    } catch (e: Exception) {
        if (cbzFile.exists()) {
            cbzFile.delete()
            println("Download failed. Deleted incomplete file: ${cbzFile.absolutePath}")
        }
    }
}

/**
 * Parses user input containing comma-separated lists of single indices or hyphenated ranges.
 * Examples: "1", "1-5", "1, 3, 5-8", "all"
 */
fun parseIndices(input: String, maxIndex: Int): Set<Int> {
    val indices = mutableSetOf<Int>()
    val parts = input.split(",")
    for (part in parts) {
        val trimmed = part.trim()
        if (trimmed.isEmpty()) continue
        if (trimmed.equals("all", ignoreCase = true)) {
            return (1..maxIndex).toSet()
        }
        if (trimmed.contains("-")) {
            val rangeParts = trimmed.split("-")
            if (rangeParts.size == 2) {
                val start = rangeParts[0].trim().toIntOrNull()
                val end = rangeParts[1].trim().toIntOrNull()
                if (start != null && end != null) {
                    val rangeStart = minOf(start, end).coerceIn(1, maxIndex)
                    val rangeEnd = maxOf(start, end).coerceIn(1, maxIndex)
                    for (i in rangeStart..rangeEnd) {
                        indices.add(i)
                    }
                }
            }
        } else {
            val index = trimmed.toIntOrNull()
            if (index != null && index in 1..maxIndex) {
                indices.add(index)
            }
        }
    }
    return indices
}