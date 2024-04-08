package gay.extremist.dao

import gay.extremist.dao.DatabaseFactory.dbQuery
import gay.extremist.models.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.or
import java.security.MessageDigest
import java.util.*


class AccountDaoImpl : AccountDao {
    override suspend fun createAccount(username: String, email: String, password: String): Account? = dbQuery {
        val account = Account.find { (Accounts.username eq username) or (Accounts.email eq email) }.firstOrNull()
        val hashedPassword = password.hashCode().toString()
        when (account) {
            null -> Account.new {
                this.username = username
                this.email = email
                this.password = hashedPassword
                this.token = UUID.nameUUIDFromBytes((username + hashedPassword).toByteArray()).toString()
            }

            else -> null
        }
    }

    override suspend fun readAccount(id: Int): Account? = dbQuery {
        Account.findById(id)
    }

    override suspend fun readAccountAll(): List<Account> = dbQuery {
        Account.all().toList()
    }

    override suspend fun updateAccount(id: Int, username: String, email: String, password: String): Boolean = dbQuery {
        when (val account = Account.findById(id)) {
            null -> return@dbQuery false
            else -> {
                account.username = username
                account.email = email
                account.password = password.hashCode().toString()
                return@dbQuery true
            }
        }
    }

    override suspend fun deleteAccount(id: Int): Boolean = dbQuery {
        when (val account = Account.findById(id)) {
            null -> return@dbQuery false
            else -> {
                account.delete()
                return@dbQuery true
            }
        }
    }

    override suspend fun getToken(email: String, password: String): String? = dbQuery {
        Account.find {
            Accounts.email eq email
            Accounts.password eq password.hashCode().toString().also {
                println(password.hashCode().toString())
            }
        }.firstOrNull()?.token
    }

    override suspend fun getIdByUsername(username: String): Int? = dbQuery {
        Account.find { Accounts.username eq username }.firstOrNull()?.id?.value
    }

    override suspend fun addFollowedAccount(id: Int, account: Account): Boolean = dbQuery {
        val follower = Account.findById(id)
        val followedAccounts = follower?.followedAccounts

        try {
            follower?.followedAccounts = SizedCollection(followedAccounts!! + account)
            true
        } catch (e: NullPointerException) {
            false
        }
    }

    override suspend fun getFollowedTags(id: Int): List<Tag> = dbQuery {
        Account.findById(id)?.followedTags?.toList() ?: emptyList()
    }

    override suspend fun getFollowedAccounts(id: Int): List<Account> = dbQuery {
        Account.findById(id)?.followedAccounts?.toList() ?: emptyList()
    }

    override suspend fun removeFollowedAccount(id: Int, account: Account): Boolean = dbQuery {
        val follower = Account.findById(id)
        val followedAccounts = follower?.followedAccounts

        try {
            follower?.followedAccounts = SizedCollection(followedAccounts!! - account)
            true
        } catch (e: NullPointerException) {
            false
        }
    }

    override suspend fun addFollowedTag(id: Int, tag: Tag): Boolean = dbQuery {
        val follower = Account.findById(id) ?: return@dbQuery false
        val followedAccounts = follower.followedTags

        runCatching {
            follower.followedTags = SizedCollection(followedAccounts + tag)
        }.isSuccess
    }

    override suspend fun removeFollowedTag(id: Int, tag: Tag): Boolean = dbQuery {
        val follower = Account.findById(id) ?: return@dbQuery false
        val followedAccounts = follower.followedTags

        runCatching {
            follower.followedTags = SizedCollection(followedAccounts - tag)
        }.isSuccess
    }

    override suspend fun getVideosFromAccount(accountId: Int): List<Video> =
        dbQuery { readAccount(accountId)?.videos?.toList() ?: emptyList() }

    override suspend fun getPlaylistsFromAccount(accountId: Int): List<Playlist> = dbQuery {
        readAccount(accountId)?.playlists?.toList() ?: emptyList()
    }
}

val accountDao: AccountDao = AccountDaoImpl()
