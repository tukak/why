package cz.kutner.why.data.db

import androidx.room3.Database
import androidx.room3.RoomDatabase

@Database(
    entities = [Reason::class, UnlockEvent::class, TypedOffer::class],
    version = 1,
    exportSchema = true,
)
abstract class WhyDatabase : RoomDatabase() {
    abstract fun reasonDao(): ReasonDao
    abstract fun unlockDao(): UnlockDao
    abstract fun offerDao(): OfferDao
}
