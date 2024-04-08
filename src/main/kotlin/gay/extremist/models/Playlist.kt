package gay.extremist.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption

object Playlists : IntIdTable() {
    val ownerId: Column<EntityID<Int>> = reference("ownerID", Accounts, onDelete = ReferenceOption.CASCADE)
    val name: Column<String> = varchar("name", 255)
    val description: Column<String> = varchar("description", 255)
}

class Playlist(id: EntityID<Int>) : Entity<Int>(id) {
    companion object : EntityClass<Int, Playlist>(Playlists)

    var owner by Account referencedOn Playlists.ownerId

    var name by Playlists.name
    var description by Playlists.description
    var videos by Video via PlaylistContainsVideo

    fun toDisplayResponse() = PlaylistDisplayResponse(id.value, name)
    fun toResponse() =
        PlaylistResponse(id.value, owner.id.value, name, description, videos.map(Video::toDisplayResponse))

}

@Serializable
data class PlaylistDisplayResponse(
    val id: Int, val name: String
)

@Serializable
data class NewPlaylistData(
    val name: String, val description: String
)

@Serializable
data class PlaylistResponse(
    val id: Int,
    val owner: Int,
    val name: String,
    val description: String,
    val videos: List<VideoDisplayResponse>
)