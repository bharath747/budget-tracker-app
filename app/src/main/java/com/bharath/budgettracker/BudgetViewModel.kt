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
 private fun dateMillis(value:Any?):Long{
  if(value==null) return System.currentTimeMillis()
  if(value is Number) return value.toLong()
  val text=value.toString()
  text.toLongOrNull()?.let{return it}
  return try{java.time.Instant.parse(text).toEpochMilli()}catch(_:Exception){
   try{java.text.SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy",java.util.Locale.US).parse(text)?.time ?: System.currentTimeMillis()}
   catch(_:Exception){System.currentTimeMillis()}
  }
 }
 fun restoreJson(json:String,onDone:(Boolean,String)->Unit)=viewModelScope.launch{
  try{
   val r=JSONObject(json)
   val versionValue=r.opt("version")
   val isCurrentBackup=versionValue == null || versionValue.toString().isBlank() ||
       versionValue.toString().toDoubleOrNull() == 1.0
   if(!isCurrentBackup){
    throw IllegalArgumentException("Unsupported backup version $versionValue")
   }
   val isLegacyWebBackup=versionValue == null
   val ts=if(!isLegacyWebBackup) {
    r.optJSONArray("transactions")?.let{a->(0 until a.length()).map{val o=a.getJSONObject(it);BudgetTransaction(o.optLong("id"),o.optLong("date"),o.optDouble("amount"),o.optString("description"),o.optString("source","default"),o.optString("type"))}}?:emptyList()
   } else {
    buildList {
     val months=r.optJSONArray("month-summary") ?: JSONArray()
     for(i in 0 until months.length()){
      val month=months.optJSONObject(i) ?: continue
      val expenses=month.optJSONArray("expenses") ?: JSONArray()
      for(j in 0 until expenses.length()){
       val o=expenses.optJSONObject(j) ?: continue
       add(BudgetTransaction(0,dateMillis(o.opt("date")),o.optDouble("amount"),o.optString("description"),o.optString("source","default"),"EXPENSE"))
      }
      val credits=month.optJSONArray("credits") ?: JSONArray()
      for(j in 0 until credits.length()){
       val o=credits.optJSONObject(j) ?: continue
       add(BudgetTransaction(0,dateMillis(o.opt("date")),o.optDouble("amount"),o.optString("description"),o.optString("source","default"),"CREDIT"))
      }
     }
    }
   }
   val ls=if(!isLegacyWebBackup) {
    r.optJSONArray("loans")?.let{a->(0 until a.length()).map{val o=a.getJSONObject(it);Loan(o.optLong("id"),o.optString("name"),o.optDouble("amount"),o.optLong("date"),o.optDouble("interestRs"),o.optDouble("interestPct"),o.optDouble("remaining",o.optDouble("amount")),o.optLong("lastInterestPaid",o.optLong("date")))}}?:emptyList()
   } else {
    r.optJSONArray("loan-summary")?.let{a->(0 until a.length()).map{val o=a.getJSONObject(it);Loan(o.optLong("id"),o.optString("loanName"),o.optDouble("loanAmount"),dateMillis(o.opt("loanDate")),o.optDouble("interestInRs"),o.optDouble("interestInperc"),o.optDouble("loanRemaining",o.optDouble("loanAmount")),dateMillis(o.opt("lastInterestPaid",o.opt("loanDate"))))}}?:emptyList()
   }
   val lp=if(!isLegacyWebBackup) {
    r.optJSONArray("loanPayments")?.let{a->(0 until a.length()).map{val o=a.getJSONObject(it);LoanPayment(o.optLong("id"),o.optLong("loanId"),o.optLong("date"),o.optDouble("principal"),o.optDouble("interest"))}}?:emptyList()
   } else emptyList()
   val ld=if(!isLegacyWebBackup) {
    r.optJSONArray("lendings")?.let{a->(0 until a.length()).map{val o=a.getJSONObject(it);Lending(o.optLong("id"),o.optString("name"),o.optDouble("amount"),o.optLong("date"),o.optDouble("interestRs"),o.optDouble("interestPct"),o.optDouble("remaining",o.optDouble("amount")),o.optLong("lastInterestPaid",o.optLong("date")))}}?:emptyList()
   } else {
    r.optJSONArray("lend-summary")?.let{a->(0 until a.length()).map{val o=a.getJSONObject(it);Lending(o.optLong("id"),o.optString("loanName"),o.optDouble("loanAmount"),dateMillis(o.opt("loanDate")),o.optDouble("interestInRs"),o.optDouble("interestInperc"),o.optDouble("loanRemaining",o.optDouble("loanAmount")),dateMillis(o.opt("lastInterestPaid",o.opt("loanDate"))))}}?:emptyList()
   }
   val ldp=if(!isLegacyWebBackup) {
    r.optJSONArray("lendingPayments")?.let{a->(0 until a.length()).map{val o=a.getJSONObject(it);LendingPayment(o.optLong("id"),o.optLong("lendingId"),o.optLong("date"),o.optDouble("principal"),o.optDouble("interest"))}}?:emptyList()
   } else emptyList()
   val ss=if(!isLegacyWebBackup) {
    r.optJSONArray("sources")?.let{a->(0 until a.length()).map{Source(a.getJSONObject(it).optString("name"))}}?:emptyList()
   } else {
    buildList {
     val arrays=r.optJSONArray("source-info")
     for(i in 0 until (arrays?.length() ?: 0)){
      val o=arrays?.optJSONObject(i) ?: continue
      val values=o.optJSONArray("sources") ?: continue
      for(j in 0 until values.length()) add(Source(values.optString(j)))
     }
    }
   }
   val fs=if(!isLegacyWebBackup) {
    r.optJSONArray("filters")?.let{a->(0 until a.length()).map{FilterWord(a.getJSONObject(it).optString("word"))}}?:emptyList()
   } else {
    buildList {
     val arrays=r.optJSONArray("filters-info")
     for(i in 0 until (arrays?.length() ?: 0)){
      val o=arrays?.optJSONObject(i) ?: continue
      val values=o.optJSONArray("filters") ?: continue
      for(j in 0 until values.length()) add(FilterWord(values.optString(j)))
     }
    }
   }
   dao.clearAll()
   dao.insertTransactions(ts);dao.insertLoans(ls);dao.insertLoanPayments(lp);dao.insertLendings(ld);dao.insertLendingPayments(ldp);dao.addSources(ss);dao.addFilters(fs)
   setInitialAmount(r.optDouble("initialAmount",0.0),r.optLong("initialDate",System.currentTimeMillis()));dao.addSource(Source("default"));onDone(true,"Data restored successfully")
  }catch(e:Exception){onDone(false,"Restore failed: "+(e.message?:"Invalid backup file"))}
 }
 fun deleteAllData(onDone:(Boolean)->Unit)=viewModelScope.launch{try{dao.clearAll();prefs.edit().clear().apply();initialAmount.value=0.0;initialDate.value=System.currentTimeMillis();dao.addSource(Source("default"));onDone(true)}catch(_:Exception){onDone(false)}}
}