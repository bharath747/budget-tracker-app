package com.bharath.budgettracker.data
import androidx.room.*
import kotlinx.coroutines.flow.Flow
@Entity(tableName="transactions") data class BudgetTransaction(@PrimaryKey(autoGenerate=true) val id:Long=0,val date:Long,val amount:Double,val description:String,val source:String,val type:String)
@Entity(tableName="loans") data class Loan(@PrimaryKey(autoGenerate=true) val id:Long=0,val name:String,val amount:Double,val date:Long,val interestRs:Double=0.0,val interestPct:Double=0.0,val remaining:Double=amount)
@Entity(tableName="loan_payments") data class LoanPayment(@PrimaryKey(autoGenerate=true) val id:Long=0,val loanId:Long,val date:Long,val principal:Double=0.0,val interest:Double=0.0)
@Dao interface BudgetDao {
 @Query("SELECT * FROM transactions ORDER BY date DESC,id DESC") fun transactions():Flow<List<BudgetTransaction>>
 @Insert suspend fun insertTransaction(t:BudgetTransaction)
 @Delete suspend fun deleteTransaction(t:BudgetTransaction)
 @Query("SELECT COALESCE(SUM(CASE WHEN type='CREDIT' THEN amount ELSE 0 END),0) FROM transactions") fun totalCredits():Flow<Double>
 @Query("SELECT COALESCE(SUM(CASE WHEN type='EXPENSE' THEN amount ELSE 0 END),0) FROM transactions") fun totalExpenses():Flow<Double>
 @Query("SELECT * FROM loans ORDER BY date DESC") fun loans():Flow<List<Loan>>
 @Insert suspend fun insertLoan(l:Loan):Long
 @Delete suspend fun deleteLoan(l:Loan)
 @Insert suspend fun insertPayment(p:LoanPayment)
 @Query("UPDATE loans SET remaining = remaining - :principal WHERE id=:loanId") suspend fun reduceLoan(loanId:Long,principal:Double)
}
@Database(entities=[BudgetTransaction::class,Loan::class,LoanPayment::class],version=1,exportSchema=false)
abstract class BudgetDatabase:RoomDatabase(){ abstract fun dao():BudgetDao
 companion object { @Volatile private var INSTANCE:BudgetDatabase?=null
 fun get(context:android.content.Context)=INSTANCE?:synchronized(this){INSTANCE?:Room.databaseBuilder(context.applicationContext,BudgetDatabase::class.java,"budget-tracker.db").build().also{INSTANCE=it}} } }