package com.bharath.budgettracker
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bharath.budgettracker.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class BudgetViewModel(app:Application):AndroidViewModel(app){
 private val dao=BudgetDatabase.get(app).dao()
 private val prefs=app.getSharedPreferences("settings",Context.MODE_PRIVATE)
 val transactions=dao.transactions()
 val credits=dao.totalCredits()
 val expenses=dao.totalExpenses()
 val loans=dao.loans()
 val lendings=dao.lendings()
 val sources=dao.sources()
 val filters=dao.filters()
 val initialAmount=MutableStateFlow(prefs.getFloat("initialAmount",0f).toDouble())
 val initialDate=MutableStateFlow(prefs.getLong("initialDate",System.currentTimeMillis()))
 init{viewModelScope.launch{dao.addSource(Source("default"))}}
 fun setInitialAmount(a:Double,d:Long){prefs.edit().putFloat("initialAmount",a.toFloat()).putLong("initialDate",d).apply();initialAmount.value=a;initialDate.value=d}
 fun addExpense(a:Double,d:String,s:String,date:Long=System.currentTimeMillis())=viewModelScope.launch{dao.addSource(Source(s.ifBlank{"default"}));dao.insertTransaction(BudgetTransaction(date=date,amount=a,description=d,source=s.ifBlank{"default"},type="EXPENSE"))}
 fun addCredit(a:Double,d:String,s:String,date:Long=System.currentTimeMillis())=viewModelScope.launch{dao.addSource(Source(s.ifBlank{"default"}));dao.insertTransaction(BudgetTransaction(date=date,amount=a,description=d,source=s.ifBlank{"default"},type="CREDIT"))}
 fun delete(t:BudgetTransaction)=viewModelScope.launch{dao.deleteTransaction(t)}
 fun addLoan(n:String,a:Double,rs:Double,pct:Double,date:Long=System.currentTimeMillis())=viewModelScope.launch{dao.insertLoan(Loan(name=n,amount=a,date=date,interestRs=rs,interestPct=pct))}
 fun deleteLoan(l:Loan)=viewModelScope.launch{dao.deleteLoan(l)}
 fun addLoanPayment(l:Loan,p:Double,i:Double,date:Long=System.currentTimeMillis())=viewModelScope.launch{dao.insertLoanPayment(LoanPayment(loanId=l.id,date=date,principal=p,interest=i));dao.reduceLoan(l.id,p,date)}
 fun loanPayments(l:Loan)=dao.loanPayments(l.id)
 fun addLending(n:String,a:Double,rs:Double,pct:Double,date:Long=System.currentTimeMillis())=viewModelScope.launch{dao.insertLending(Lending(name=n,amount=a,date=date,interestRs=rs,interestPct=pct))}
 fun deleteLending(l:Lending)=viewModelScope.launch{dao.deleteLending(l)}
 fun addLendingPayment(l:Lending,p:Double,i:Double,date:Long=System.currentTimeMillis())=viewModelScope.launch{dao.insertLendingPayment(LendingPayment(lendingId=l.id,date=date,principal=p,interest=i));dao.reduceLending(l.id,p,date)}
 fun lendingPayments(l:Lending)=dao.lendingPayments(l.id)
 fun addSource(s:String)=viewModelScope.launch{if(s.isNotBlank())dao.addSource(Source(s.trim()))}
 fun deleteSource(s:Source)=viewModelScope.launch{if(s.name!="default")dao.deleteSource(s)}
 fun addFilter(s:String)=viewModelScope.launch{if(s.isNotBlank())dao.addFilter(FilterWord(s.trim()))}
 fun deleteFilter(f:FilterWord)=viewModelScope.launch{dao.deleteFilter(f)}
 fun interest(from:Long,to:Long,remaining:Double,rs:Double,pct:Double):Double{val days=((to-from)/86400000.0).coerceAtLeast(0.0);return if(pct>0)remaining*pct/100*days/365 else rs*days/30}
 fun backupJson(onDone:(String?)->Unit)=viewModelScope.launch(Dispatchers.IO){
  try {
   val tx=dao.transactions().first(); val ls=dao.loans().first(); val ld=dao.lendings().first()
   val root=JSONObject().put("version",1).put("initialAmount",initialAmount.value).put("initialDate",initialDate.value)
   root.put("transactions",JSONArray().also{a->tx.forEach{t->a.put(JSONObject().put("id",t.id).put("date",t.date).put("amount",t.amount).put("description",t.description).put("source",t.source).put("type",t.type))}})
   root.put("loans",JSONArray().also{a->ls.forEach{l->a.put(JSONObject().put("id",l.id).put("name",l.name).put("amount",l.amount).put("date",l.date).put("interestRs",l.interestRs).put("interestPct",l.interestPct).put("remaining",l.remaining).put("lastInterestPaid",l.lastInterestPaid))}})
   root.put("loanPayments",JSONArray().also{a->ls.forEach{l->dao.loanPayments(l.id).first().forEach{p->a.put(JSONObject().put("id",p.id).put("loanId",p.loanId).put("date",p.date).put("principal",p.principal).put("interest",p.interest))}}})
   root.put("lendings",JSONArray().also{a->ld.forEach{l->a.put(JSONObject().put("id",l.id).put("name",l.name).put("amount",l.amount).put("date",l.date).put("interestRs",l.interestRs).put("interestPct",l.interestPct).put("remaining",l.remaining).put("lastInterestPaid",l.lastInterestPaid))}})
   root.put("lendingPayments",JSONArray().also{a->ld.forEach{l->dao.lendingPayments(l.id).first().forEach{p->a.put(JSONObject().put("id",p.id).put("lendingId",p.lendingId).put("date",p.date).put("principal",p.principal).put("interest",p.interest))}}})
   root.put("sources",JSONArray().also{a->dao.sources().first().forEach{a.put(JSONObject().put("name",it.name))}})
   root.put("filters",JSONArray().also{a->dao.filters().first().forEach{a.put(JSONObject().put("word",it.word))}})
   val json=root.toString(2)
   withContext(Dispatchers.Main){onDone(json)}
  } catch(_:Exception) { withContext(Dispatchers.Main){onDone(null)} }
 }
 fun restoreJson(json:String,onDone:(Boolean,String)->Unit)=viewModelScope.launch{
  try{
   val r=JSONObject(json); if(r.optInt("version",-1)!=1) throw IllegalArgumentException("Unsupported backup version")
   val ts=r.optJSONArray("transactions")?.let{a->(0 until a.length()).map{val o=a.getJSONObject(it);BudgetTransaction(o.optLong("id"),o.optLong("date"),o.optDouble("amount"),o.optString("description"),o.optString("source","default"),o.optString("type"))}}?:emptyList()
   val ls=r.optJSONArray("loans")?.let{a->(0 until a.length()).map{val o=a.getJSONObject(it);Loan(o.optLong("id"),o.optString("name"),o.optDouble("amount"),o.optLong("date"),o.optDouble("interestRs"),o.optDouble("interestPct"),o.optDouble("remaining",o.optDouble("amount")),o.optLong("lastInterestPaid",o.optLong("date")))}}?:emptyList()
   val lp=r.optJSONArray("loanPayments")?.let{a->(0 until a.length()).map{val o=a.getJSONObject(it);LoanPayment(o.optLong("id"),o.optLong("loanId"),o.optLong("date"),o.optDouble("principal"),o.optDouble("interest"))}}?:emptyList()
   val ld=r.optJSONArray("lendings")?.let{a->(0 until a.length()).map{val o=a.getJSONObject(it);Lending(o.optLong("id"),o.optString("name"),o.optDouble("amount"),o.optLong("date"),o.optDouble("interestRs"),o.optDouble("interestPct"),o.optDouble("remaining",o.optDouble("amount")),o.optLong("lastInterestPaid",o.optLong("date")))}}?:emptyList()
   val ldp=r.optJSONArray("lendingPayments")?.let{a->(0 until a.length()).map{val o=a.getJSONObject(it);LendingPayment(o.optLong("id"),o.optLong("lendingId"),o.optLong("date"),o.optDouble("principal"),o.optDouble("interest"))}}?:emptyList()
   val ss=r.optJSONArray("sources")?.let{a->(0 until a.length()).map{Source(a.getJSONObject(it).optString("name"))}}?:emptyList()
   val fs=r.optJSONArray("filters")?.let{a->(0 until a.length()).map{FilterWord(a.getJSONObject(it).optString("word"))}}?:emptyList()
   dao.clearAll()
   dao.insertTransactions(ts);dao.insertLoans(ls);dao.insertLoanPayments(lp);dao.insertLendings(ld);dao.insertLendingPayments(ldp);dao.addSources(ss);dao.addFilters(fs)
   setInitialAmount(r.optDouble("initialAmount",0.0),r.optLong("initialDate",System.currentTimeMillis()));dao.addSource(Source("default"));onDone(true,"Data restored successfully")
  }catch(e:Exception){onDone(false,"Restore failed: "+(e.message?:"Invalid backup file"))}
 }
 fun deleteAllData(onDone:(Boolean)->Unit)=viewModelScope.launch{try{dao.clearAll();prefs.edit().clear().apply();initialAmount.value=0.0;initialDate.value=System.currentTimeMillis();dao.addSource(Source("default"));onDone(true)}catch(_:Exception){onDone(false)}}
}