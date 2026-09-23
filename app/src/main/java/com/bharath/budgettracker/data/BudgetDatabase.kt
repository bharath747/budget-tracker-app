package com.bharath.budgettracker.data
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName="transactions")
data class BudgetTransaction(@PrimaryKey(autoGenerate=true) val id:Long=0,val date:Long,val amount:Double,val description:String,val source:String,val type:String)

@Entity(tableName="loans")
data class Loan(@PrimaryKey(autoGenerate=true) val id:Long=0,val name:String,val amount:Double,val date:Long,val interestRs:Double=0.0,val interestPct:Double=0.0,val remaining:Double=amount,val lastInterestPaid:Long=date)

@Entity(tableName="loan_payments")
data class LoanPayment(@PrimaryKey(autoGenerate=true) val id:Long=0,val loanId:Long,val date:Long,val principal:Double=0.0,val interest:Double=0.0)

@Entity(tableName="lendings")
data class Lending(@PrimaryKey(autoGenerate=true) val id:Long=0,val name:String,val amount:Double,val date:Long,val interestRs:Double=0.0,val interestPct:Double=0.0,val remaining:Double=amount,val lastInterestPaid:Long=date)

@Entity(tableName="lending_payments")
data class LendingPayment(@PrimaryKey(autoGenerate=true) val id:Long=0,val lendingId:Long,val date:Long,val principal:Double=0.0,val interest:Double=0.0)

@Entity(tableName="sources")
data class Source(@PrimaryKey val name:String)

@Entity(tableName="filters")
data class FilterWord(@PrimaryKey val word:String)

@Dao interface BudgetDao {
 @Query("SELECT * FROM transactions ORDER BY date DESC,id DESC") fun transactions():Flow<List<BudgetTransaction>>
 @Insert suspend fun insertTransaction(t:BudgetTransaction)
 @Insert suspend fun insertTransactions(items:List<BudgetTransaction>)
 @Delete suspend fun deleteTransaction(t:BudgetTransaction)
 @Query("DELETE FROM transactions") suspend fun clearTransactions()
 @Query("SELECT COALESCE(SUM(CASE WHEN type='CREDIT' THEN amount ELSE 0 END),0) FROM transactions") fun totalCredits():Flow<Double>
 @Query("SELECT COALESCE(SUM(CASE WHEN type='EXPENSE' THEN amount ELSE 0 END),0) FROM transactions") fun totalExpenses():Flow<Double>
 @Query("SELECT * FROM loans ORDER BY date DESC") fun loans():Flow<List<Loan>>
 @Insert suspend fun insertLoan(l:Loan):Long
 @Insert suspend fun insertLoans(items:List<Loan>)
 @Delete suspend fun deleteLoan(l:Loan)
 @Query("DELETE FROM loans") suspend fun clearLoans()
 @Query("UPDATE loans SET remaining=remaining-:principal,lastInterestPaid=:date WHERE id=:loanId") suspend fun reduceLoan(loanId:Long,principal:Double,date:Long)
 @Query("SELECT * FROM loan_payments WHERE loanId=:loanId ORDER BY date DESC") fun loanPayments(loanId:Long):Flow<List<LoanPayment>>
 @Insert suspend fun insertLoanPayment(p:LoanPayment)
 @Insert suspend fun insertLoanPayments(items:List<LoanPayment>)
 @Query("DELETE FROM loan_payments") suspend fun clearLoanPayments()
 @Query("DELETE FROM loan_payments WHERE id=:id") suspend fun deleteLoanPayment(id:Long)
 @Query("SELECT * FROM lendings ORDER BY date DESC") fun lendings():Flow<List<Lending>>
 @Insert suspend fun insertLending(l:Lending):Long
 @Insert suspend fun insertLendings(items:List<Lending>)
 @Delete suspend fun deleteLending(l:Lending)
 @Query("DELETE FROM lendings") suspend fun clearLendings()
 @Query("UPDATE lendings SET remaining=remaining-:principal,lastInterestPaid=:date WHERE id=:id") suspend fun reduceLending(id:Long,principal:Double,date:Long)
 @Query("SELECT * FROM lending_payments WHERE lendingId=:id ORDER BY date DESC") fun lendingPayments(id:Long):Flow<List<LendingPayment>>
 @Insert suspend fun insertLendingPayment(p:LendingPayment)
 @Insert suspend fun insertLendingPayments(items:List<LendingPayment>)
 @Query("DELETE FROM lending_payments") suspend fun clearLendingPayments()
 @Query("DELETE FROM lending_payments WHERE id=:id") suspend fun deleteLendingPayment(id:Long)
 @Query("SELECT * FROM sources ORDER BY name") fun sources():Flow<List<Source>>
 @Insert(onConflict=OnConflictStrategy.IGNORE) suspend fun addSource(s:Source)
 @Insert(onConflict=OnConflictStrategy.IGNORE) suspend fun addSources(items:List<Source>)
 @Query("DELETE FROM sources") suspend fun clearSources()
 @Delete suspend fun deleteSource(s:Source)
 @Query("SELECT * FROM filters ORDER BY word") fun filters():Flow<List<FilterWord>>
 @Insert(onConflict=OnConflictStrategy.IGNORE) suspend fun addFilter(f:FilterWord)
 @Insert(onConflict=OnConflictStrategy.IGNORE) suspend fun addFilters(items:List<FilterWord>)
 @Query("DELETE FROM filters") suspend fun clearFilters()
 @Delete suspend fun deleteFilter(f:FilterWord)
 @Transaction suspend fun clearAll(){clearLoanPayments();clearLendingPayments();clearTransactions();clearLoans();clearLendings();clearSources();clearFilters()}
}
@Database(entities=[BudgetTransaction::class,Loan::class,LoanPayment::class,Lending::class,LendingPayment::class,Source::class,FilterWord::class],version=3,exportSchema=false)
abstract class BudgetDatabase:RoomDatabase(){ abstract fun dao():BudgetDao
 companion object {
 @Volatile private var INSTANCE:BudgetDatabase?=null
 private val MIGRATION_2_3=object:Migration(2,3){override fun migrate(db:SupportSQLiteDatabase){db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_date ON transactions(date)")}}
 fun get(context:android.content.Context)=INSTANCE?:synchronized(this){INSTANCE?:Room.databaseBuilder(context.applicationContext,BudgetDatabase::class.java,"budget-tracker.db").addMigrations(MIGRATION_2_3).build().also{INSTANCE=it}} } }