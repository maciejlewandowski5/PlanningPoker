package com.example

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class RoomRepository {

    private val chars = ('A'..'Z') + ('0'..'9')

    private fun generateCode() = (1..6).map { chars.random() }.joinToString("")

    suspend fun createRoom(votingScale: String): Pair<String, String> = withContext(Dispatchers.IO) {
        transaction {
            val id = UUID.randomUUID().toString()
            var code: String
            do {
                code = generateCode()
                val exists = Rooms.selectAll().where { Rooms.code eq code }.count() > 0L
            } while (exists)
            Rooms.insert {
                it[Rooms.id] = id
                it[Rooms.code] = code
                it[createdAt] = System.currentTimeMillis()
                it[Rooms.votesRevealed] = false
                it[Rooms.votingScale] = votingScale
            }
            Pair(id, code)
        }
    }

    suspend fun findRoomByCode(code: String): ResultRow? = withContext(Dispatchers.IO) {
        transaction {
            Rooms.selectAll().where { Rooms.code eq code }.singleOrNull()
        }
    }

    suspend fun addParticipant(roomId: String, displayName: String): String = withContext(Dispatchers.IO) {
        transaction {
            val id = UUID.randomUUID().toString()
            Participants.insert {
                it[Participants.id] = id
                it[Participants.roomId] = roomId
                it[Participants.displayName] = displayName
                it[joinedAt] = System.currentTimeMillis()
            }
            id
        }
    }

    suspend fun findParticipant(participantId: String): ResultRow? = withContext(Dispatchers.IO) {
        transaction {
            Participants.selectAll().where { Participants.id eq participantId }.singleOrNull()
        }
    }

    suspend fun removeParticipant(participantId: String) = withContext(Dispatchers.IO) {
        transaction {
            Votes.deleteWhere { Votes.participantId eq participantId }
            Participants.deleteWhere { Participants.id eq participantId }
        }
    }

    suspend fun upsertVote(participantId: String, voteValue: String) = withContext(Dispatchers.IO) {
        transaction {
            Votes.upsert {
                it[Votes.participantId] = participantId
                it[value] = voteValue
            }
        }
    }

    suspend fun setRevealed(roomId: String, revealed: Boolean) = withContext(Dispatchers.IO) {
        transaction {
            Rooms.update({ Rooms.id eq roomId }) {
                it[votesRevealed] = revealed
            }
        }
    }

    suspend fun resetRound(roomId: String) = withContext(Dispatchers.IO) {
        transaction {
            val participantIds = Participants
                .selectAll()
                .where { Participants.roomId eq roomId }
                .map { it[Participants.id] }
            if (participantIds.isNotEmpty()) {
                Votes.deleteWhere { Votes.participantId inList participantIds }
            }
            Rooms.update({ Rooms.id eq roomId }) {
                it[votesRevealed] = false
            }
        }
    }

    suspend fun getRoomState(roomId: String): RoomState? = withContext(Dispatchers.IO) {
        transaction {
            val room = Rooms.selectAll().where { Rooms.id eq roomId }.singleOrNull()
                ?: return@transaction null
            val revealed = room[Rooms.votesRevealed]
            val participants = Participants
                .join(Votes, JoinType.LEFT, Participants.id, Votes.participantId)
                .selectAll()
                .where { Participants.roomId eq roomId }
                .orderBy(Participants.joinedAt, SortOrder.ASC)
                .map { row ->
                    val hasVoted = row.getOrNull(Votes.participantId) != null
                    val voteValue = row.getOrNull(Votes.value)
                    ParticipantState(
                        participantId = row[Participants.id],
                        displayName = row[Participants.displayName],
                        hasVoted = hasVoted,
                        vote = if (revealed) voteValue else null
                    )
                }
            RoomState(
                roomId = room[Rooms.id],
                code = room[Rooms.code],
                votesRevealed = revealed,
                participants = participants,
                votingScale = room[Rooms.votingScale]
            )
        }
    }
}
