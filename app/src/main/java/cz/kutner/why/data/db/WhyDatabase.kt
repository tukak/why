package cz.kutner.why.data.db

import androidx.room3.AutoMigration
import androidx.room3.Database
import androidx.room3.RoomDatabase

@Database(
    entities = [Reason::class, UnlockEvent::class, TypedOffer::class],
    version = 4,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3), AutoMigration(from = 3, to = 4)],
)
abstract class WhyDatabase : RoomDatabase() {
    abstract fun reasonDao(): ReasonDao
    abstract fun unlockDao(): UnlockDao
    abstract fun offerDao(): OfferDao
}
