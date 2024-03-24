package gay.extremist.models

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column

object Comments: IntIdTable() {
    val accountID: Column<EntityID<Int>>  = reference("accountID", Accounts)
    val videoID: Column<EntityID<Int>>  = reference("videoID", Videos)
    val parentID: Column<EntityID<Int>?>  = reference("parentID", Comments).nullable()
    val comment: Column<String> = text("comment")
}

class Comment(id: EntityID<Int>): Entity<Int>(id){
    companion object: EntityClass<Int, Comment> (Comments)

    var account by Account referencedOn Comments.accountID
    var video by Video referencedOn Comments.videoID
    var parentComment by Comment optionalReferencedOn Comments.parentID
    var comment by Comments.comment

    val childComments by Comment optionalReferrersOn Comments.parentID

}