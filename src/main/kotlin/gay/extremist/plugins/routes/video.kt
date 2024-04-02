package gay.extremist.plugins.routes

import gay.extremist.BASE_VIDEO_STORAGE_PATH
import gay.extremist.TMP_VIDEO_STORAGE
import gay.extremist.dao.DatabaseFactory.dbQuery
import gay.extremist.dao.accountDao
import gay.extremist.dao.tagDao
import gay.extremist.dao.videoDao
import gay.extremist.data_classes.ErrorResponse
import gay.extremist.data_classes.VideoResponse
import gay.extremist.models.Tag
import gay.extremist.util.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

fun Route.createVideoRoutes() = route("/videos") {
    route("/{id}") {
        get { handleGetVideo() }
        delete { handleDeleteVideo() } //        TODO UPDATE VIDEO ENDPOINT
        post { handleUpdateVideo() }
    }

    post { handleUploadVideo() }
}

suspend fun PipelineContext<Unit, ApplicationCall>.handleUpdateVideo() {
    val headers = requiredHeaders(headerAccountId, headerToken) ?: return
    val accountId = idParameter() ?: return

    val token = headers[headerToken]
    val account = accountDao.readAccount(accountId) ?: return call.respond(ErrorResponse.notFound("Account"))

    if (account.token != token) return call.respond(ErrorResponse.accountTokenInvalid)

    // TODO IMPLEMENT
    // Recieve some json with the video details.
}

suspend fun PipelineContext<Unit, ApplicationCall>.handleUploadVideo() {
    val headers = requiredHeaders(headerAccountId, headerToken) ?: return
    val accountId = idParameter() ?: return

    val token = headers[headerToken]
    val account = accountDao.readAccount(accountId) ?: return call.respond(ErrorResponse.notFound("Account"))

    if (account.token != token) return call.respond(ErrorResponse.accountTokenInvalid)

    val multiPartData = runCatching {
        call.receiveMultipart()
    }.getOrNull() ?: return call.respond(ErrorResponse.videoUploadFailed)

    var title = ""
    val fileName = ""
    var videoDescription = ""
    var tags: Array<String> = emptyArray()

    val timestamp = System.currentTimeMillis()
    multiPartData.forEachPart {
        when (it) {
            is PartData.FormItem -> when (it.name) {
                "title" -> title = it.value
                "description" -> videoDescription = it.value
                "tags" -> tags = Json.decodeFromString<Array<String>>(it.value)
                else -> {}
            }

            is PartData.FileItem -> {
                val originalFileName = it.originalFileName ?: "upload.mp4"
                File("$BASE_VIDEO_STORAGE_PATH/${account.id}/$timestamp/$originalFileName").apply {
                    mkdirs()
                    appendBytes(it.streamProvider().readBytes())
                }
            }

            else -> {}
        }
        it.dispose()
    }

    when {
        title.isEmpty() -> return call.respond(ErrorResponse.videoTitleEmpty)
        title.length > 255 -> return call.respond(ErrorResponse.videoTitleTooLong)
        videoDescription.isEmpty() -> return call.respond(ErrorResponse.videoDescriptionEmpty)
        videoDescription.length > 4000 -> return call.respond(ErrorResponse.videoDescriptionTooLong)
        tags.isEmpty() -> return call.respond(ErrorResponse.videoTagsEmpty)
        tags.size > 16 -> return call.respond(ErrorResponse.videoTagsTooMany)
    }

    val tagObjects: SizedCollection<Tag> =
        SizedCollection(tags.map { tagDao.findTagByName(it) ?: tagDao.createTag(it) })

    val video = videoDao.createVideo(
        account,
        "$TMP_VIDEO_STORAGE/${account.id}/$timestamp/$fileName",
        title,
        videoDescription,
        tagObjects,
    )

    call.respond(video.id.value)

    val newOutputDir = File("$BASE_VIDEO_STORAGE_PATH/${video.id.value}").apply(File::mkdirs)

    // May take some time (much)
    VideoConverter.convert(video.videoPath, newOutputDir.canonicalPath)

    File("$TMP_VIDEO_STORAGE/${account.id}/$timestamp/").deleteRecursively()

    videoDao.updateVideoPath(video.id.value, "/static/videos/${video.id.value}")
}

suspend fun PipelineContext<Unit, ApplicationCall>.handleDeleteVideo() {
    val headers = requiredHeaders(headerAccountId, headerToken) ?: return

    val videoId = idParameter() ?: return
    val video = videoDao.readVideo(videoId) ?: return call.respond(ErrorResponse.videoNotFound)

    val accountId = convert(headers[headerAccountId], String::toInt) ?: return
    val account = accountDao.readAccount(accountId) ?: return call.respond(ErrorResponse.accountNotFound)

    val token = headers[headerToken] ?: return

    when {
        transaction { account.id.value != video.creator.id.value } -> {
            return call.respond(ErrorResponse.videoNotOwnedByAccount)
        }

        (account.token != token) -> return call.respond(ErrorResponse.accountTokenInvalid)

        else -> {
            if (File("$BASE_VIDEO_STORAGE_PATH/${video.id.value}").deleteRecursively()) return call.respond(
                ErrorResponse.videoDeleteFailed
            )

            videoDao.deleteVideo(videoId)

            call.respond(HttpStatusCode.OK)
        }
    }
}

private suspend fun PipelineContext<Unit, ApplicationCall>.handleGetVideo() {
    val videoId = idParameter() ?: return
    val video = videoDao.readVideo(videoId) ?: return call.respond(ErrorResponse.notFound("Video"))
    if (video.videoPath.contains("tmp")) return call.respond(ErrorResponse.videoNotProcessed)

    call.respond(
        VideoResponse(
            video.id.value,
            video.title,
            video.description,
            video.videoPath,
            dbQuery { video.tags.map { it.tag } },
            video.creator.id.value,
            video.uploadDate.toString()
        )
    )
}


fun Application.staticRoutes() = routing {
    staticFiles("/static/videos", File(BASE_VIDEO_STORAGE_PATH))
}