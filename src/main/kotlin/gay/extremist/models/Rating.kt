package gay.extremist.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption

object Ratings: IntIdTable() {
    val videoID: Column<EntityID<Int>> = reference("videoID", Videos, onDelete = ReferenceOption.CASCADE)
    val accountID: Column<EntityID<Int>> = reference("accountID", Accounts, onDelete = ReferenceOption.CASCADE)
    val rating: Column<Int> = integer("rating")
    init {
        uniqueIndex("UI_Ratings_vid_acc", videoID, accountID)
    }
}

class Rating(id: EntityID<Int>): Entity<Int>(id) {
    fun toResponse() = RatingResponse(
        id.value, video.toDisplayResponse(), account.toDisplayResponse(), rating
    )


    companion object: EntityClass<Int,Rating> (Ratings)

    var video by Video referencedOn Ratings.videoID
    var account by Account referencedOn Ratings.accountID
    var rating by Ratings.rating

}

@Serializable
data class RatingResponse(val id: Int, val videoDisplayResponse: VideoDisplayResponse, val account: AccountDisplayResponse, val rating: Int)