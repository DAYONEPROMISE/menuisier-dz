package dz.menuisier.app.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "client_orders")
data class ClientOrder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientName: String,
    val phone: String = "",
    /** Total price in Algerian Dinars (DZD). */
    val totalDzd: Long,
    /** Deposit already paid in DZD (acompte). */
    val acompteDzd: Long = 0,
    val status: String = "EN_ATTENTE",
    val createdAt: Long = System.currentTimeMillis(),
) {
    val resteDzd: Long get() = (totalDzd - acompteDzd).coerceAtLeast(0)
}

@Entity(tableName = "cut_parts")
data class CutPart(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Long,
    val label: String,
    val lengthMm: Int,
    val widthMm: Int = 0,
    val quantity: Int,
)

@Dao
interface OrderDao {
    @Query("SELECT * FROM client_orders ORDER BY createdAt DESC")
    fun observeOrders(): Flow<List<ClientOrder>>

    @Insert
    suspend fun insertOrder(order: ClientOrder): Long

    @Query("UPDATE client_orders SET acompteDzd = :acompte WHERE id = :id")
    suspend fun updateAcompte(id: Long, acompte: Long)

    @Query("UPDATE client_orders SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Insert
    suspend fun insertPart(part: CutPart): Long

    @Query("SELECT * FROM cut_parts WHERE orderId = :orderId")
    suspend fun partsForOrder(orderId: Long): List<CutPart>
}

@Database(entities = [ClientOrder::class, CutPart::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun orderDao(): OrderDao
}
