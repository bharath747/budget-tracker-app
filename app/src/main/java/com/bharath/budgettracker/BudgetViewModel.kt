package com.bharath.budgettracker
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bharath.budgettracker.data.*
import kotlinx.coroutines.launch
class BudgetViewModel(app:Application):AndroidViewModel(app){
 private val dao=BudgetDatabase.get(app).dao()
 val transactions=dao.transactions(); val credits=dao.totalCredits(); val expenses=dao.totalExpenses(); val loans=dao.loans()
 fun addExpense(a:Double,d:String,s:String)=viewModelScope.launch{dao.insertTransaction(BudgetTransaction(date=System.currentTimeMillis(),amount=a,description=d,source=s,type="EXPENSE"))}
 fun addCredit(a:Double,d:String,s:String)=viewModelScope.launch{dao.insertTransaction(BudgetTransaction(date=System.currentTimeMillis(),amount=a,description=d,source=s,type="CREDIT"))}
 fun delete(t:BudgetTransaction)=viewModelScope.launch{dao.deleteTransaction(t)}
 fun addLoan(n:String,a:Double,r:Double)=viewModelScope.launch{dao.insertLoan(Loan(name=n,amount=a,date=System.currentTimeMillis(),interestPct=r))}
 fun deleteLoan(l:Loan)=viewModelScope.launch{dao.deleteLoan(l)}
 fun addPayment(l:Loan,p:Double,i:Double)=viewModelScope.launch{dao.insertPayment(LoanPayment(loanId=l.id,date=System.currentTimeMillis(),principal=p,interest=i));dao.reduceLoan(l.id,p)}
 fun interest(l:Loan):Double{val days=((System.currentTimeMillis()-l.date)/86400000.0).coerceAtLeast(0.0);return if(l.interestPct>0)l.remaining*l.interestPct/100*days/365 else l.interestRs*days/30}
}