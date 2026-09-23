package com.bharath.budgettracker
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bharath.budgettracker.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
class BudgetViewModel(app:Application):AndroidViewModel(app){
 private val dao=BudgetDatabase.get(app).dao()
 val transactions=dao.transactions(); val credits=dao.totalCredits(); val expenses=dao.totalExpenses(); val loans=dao.loans(); val lendings=dao.lendings(); val sources=dao.sources(); val filters=dao.filters()
 init{viewModelScope.launch{dao.addSource(Source("default"))}}
 fun addExpense(a:Double,d:String,s:String)=viewModelScope.launch{dao.addSource(Source(s.ifBlank{"default"}));dao.insertTransaction(BudgetTransaction(date=System.currentTimeMillis(),amount=a,description=d,source=s.ifBlank{"default"},type="EXPENSE"))}
 fun addCredit(a:Double,d:String,s:String)=viewModelScope.launch{dao.addSource(Source(s.ifBlank{"default"}));dao.insertTransaction(BudgetTransaction(date=System.currentTimeMillis(),amount=a,description=d,source=s.ifBlank{"default"},type="CREDIT"))}
 fun delete(t:BudgetTransaction)=viewModelScope.launch{dao.deleteTransaction(t)}
 fun addLoan(n:String,a:Double,rs:Double,pct:Double)=viewModelScope.launch{dao.insertLoan(Loan(name=n,amount=a,date=System.currentTimeMillis(),interestRs=rs,interestPct=pct))}
 fun deleteLoan(l:Loan)=viewModelScope.launch{dao.deleteLoan(l)}
 fun addLoanPayment(l:Loan,p:Double,i:Double)=viewModelScope.launch{dao.insertLoanPayment(LoanPayment(loanId=l.id,date=System.currentTimeMillis(),principal=p,interest=i));dao.reduceLoan(l.id,p,System.currentTimeMillis())}
 fun loanPayments(l:Loan)=dao.loanPayments(l.id)
 fun addLending(n:String,a:Double,rs:Double,pct:Double)=viewModelScope.launch{dao.insertLending(Lending(name=n,amount=a,date=System.currentTimeMillis(),interestRs=rs,interestPct=pct))}
 fun deleteLending(l:Lending)=viewModelScope.launch{dao.deleteLending(l)}
 fun addLendingPayment(l:Lending,p:Double,i:Double)=viewModelScope.launch{dao.insertLendingPayment(LendingPayment(lendingId=l.id,date=System.currentTimeMillis(),principal=p,interest=i));dao.reduceLending(l.id,p,System.currentTimeMillis())}
 fun lendingPayments(l:Lending)=dao.lendingPayments(l.id)
 fun addSource(s:String)=viewModelScope.launch{if(s.isNotBlank())dao.addSource(Source(s.trim()))}
 fun deleteSource(s:Source)=viewModelScope.launch{if(s.name!="default")dao.deleteSource(s)}
 fun addFilter(s:String)=viewModelScope.launch{if(s.isNotBlank())dao.addFilter(FilterWord(s.trim()))}
 fun deleteFilter(f:FilterWord)=viewModelScope.launch{dao.deleteFilter(f)}
 fun interest(date:Long,remaining:Double,rs:Double,pct:Double):Double{val days=((System.currentTimeMillis()-date)/86400000.0).coerceAtLeast(0.0);return if(pct>0)remaining*pct/100*days/365 else rs*days/30}
}