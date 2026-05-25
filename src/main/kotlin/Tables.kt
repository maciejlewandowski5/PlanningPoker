package com.example

import org.jetbrains.exposed.sql.Table

object Rooms : Table("rooms") {
    val id = varchar("id", 36)
    val code = varchar("code", 6).uniqueIndex()
    val createdAt = long("created_at")
    val votesRevealed = bool("votes_revealed").default(false)
    val votingScale = varchar("voting_scale", 255).default("1,2,3,5,8,13,21,40,100,?")
    override val primaryKey = PrimaryKey(id)
}

object Participants : Table("participants") {
    val id = varchar("id", 36)
    val roomId = varchar("room_id", 36)
    val displayName = varchar("display_name", 64)
    val joinedAt = long("joined_at")
    override val primaryKey = PrimaryKey(id)
}

object Votes : Table("votes") {
    val participantId = varchar("participant_id", 36)
    val value = varchar("value", 32).nullable()
    override val primaryKey = PrimaryKey(participantId)
}
