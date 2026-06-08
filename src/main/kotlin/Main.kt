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
    //TODO: validate the link to check if it is from mangakatana
    println("Enter the link to a manga to continue: ")
    val mangaURL = readlnOrNull()

    if (mangaURL == null) {
        println("Invalid Input. exiting...")
    } else {
        println("Starting...")
        val mangaData = getMangaData(mangaURL)

        println("Recent Chapters:")
        mangaData.chapters.entries.take(5).forEach { (name, url) ->
            println("$name -> $url")
        }
        runBlocking { downloadChapters(mangaData.title, mangaData.chapters.values.toList()) }
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
    chaptersURLs: List<String>,
    destination: String = "${System.getProperty("user.home")}/Downloads/Manga"
) {
    val safeTitle = title
        .replace(Regex("""[^\w\- ]"""), "")
        .replace(" ", "_")
    val mangaFolder = File(if (title.isNotBlank()) "$destination/$safeTitle" else destination)
    if (!mangaFolder.exists()) {
        mangaFolder.mkdirs()
    }

    chaptersURLs.forEachIndexed { i, chapterURL ->
        val pageURLs = mutableListOf<String>()
        var chapterName = ""

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
                            chapterName = attribute("data-alt")

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

        // Fallback name
        if (chapterName.isBlank()) {
            chapterName = "Chapter ${chaptersURLs.size - i}"
        }

        println("Downloading $chapterName")
        downloadChapter(chapterName, pageURLs, mangaFolder)
        delay(1000)
    }
}

fun downloadChapter(chapterName: String, pageURLs: List<String>, outputFolder: File) {
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