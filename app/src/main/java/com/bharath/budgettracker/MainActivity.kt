package com.bharath.budgettracker
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.bharath.budgettracker.data.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity:ComponentActivity(){override fun onCreate(b:Bundle?){super.onCreate(b);setContent{BudgetTheme{BudgetApp()}}}}

private val Brand=Color(0xFF315DFF)
private val Positive=Color(0xFF16845B)
private val Negative=Color(0xFFC43D4B)

@Composable private fun BudgetTheme(content:@Composable()->Unit){
 MaterialTheme(colorScheme=lightColorScheme(
  primary=Brand,onPrimary=Color.White,primaryContainer=Color(0xFFE8EDFF),
  background=Color(0xFFF7F8FC),surface=Color.White,surfaceVariant=Color(0xFFF0F2F7),error=Negative
 ),content=content)
}

@Composable fun BudgetApp(vm:BudgetViewModel=viewModel()){
 val nav=rememberNavController()
 val backStack by nav.currentBackStackEntryAsState()
 val route=backStack?.destination?.route?:"dashboard"
 val items=listOf(
  Triple("dashboard","Home",Icons.Default.Dashboard),
  Triple("transactions","Activity",Icons.Default.ReceiptLong),
  Triple("analytics","Insights",Icons.Default.BarChart),
  Triple("loans","Loans",Icons.Default.AccountBalance),
  Triple("lend","Lend",Icons.Default.Handshake),
  Triple("admin","Settings",Icons.Default.Settings)
 )
 Scaffold(containerColor=Color(0xFFF7F8FC),bottomBar={
  NavigationBar(containerColor=Color.White,tonalElevation=4.dp){
   items.forEach{item->
    NavigationBarItem(selected=route==item.first,onClick={nav.navigate(item.first){popUpTo("dashboard"){saveState=true};launchSingleTop=true;restoreState=true}},
     icon={Icon(item.third,item.second)},label={Text(item.second)})
   }
  }
 }){p->
  NavHost(nav,"dashboard",Modifier.padding(p).fillMaxSize()){
   composable("dashboard"){Dashboard(vm)}
   composable("transactions"){Transactions(vm)}
   composable("analytics"){Analytics(vm)}
   composable("loans"){Loans(vm)}
   composable("lend"){Lend(vm)}
   composable("admin"){Admin(vm)}
  }
 }
}

@Composable fun Dashboard(vm:BudgetViewModel){
 val c by vm.credits.collectAsState(0.0);val e by vm.expenses.collectAsState(0.0);val loans by vm.loans.collectAsState(emptyList());val lends by vm.lendings.collectAsState(emptyList());val initial by vm.initialAmount.collectAsState();val tx by vm.transactions.collectAsState(emptyList())
 val balance=initial+c-e
 LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
  item{Column(verticalArrangement=Arrangement.spacedBy(4.dp)){Text("Budget overview",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text("Your personal financial control center",color=MaterialTheme.colorScheme.onSurfaceVariant)}}
  item{Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(26.dp),colors=CardDefaults.cardColors(containerColor=Brand)){Column(Modifier.padding(22.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text("Available balance",color=Color.White.copy(alpha=.8f));Text("₹ "+String.format(Locale.getDefault(),"%,.2f",balance),color=Color.White,style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("Income ₹"+String.format(Locale.getDefault(),"%,.0f",c),color=Color.White.copy(alpha=.8f));Text("Spent ₹"+String.format(Locale.getDefault(),"%,.0f",e),color=Color.White.copy(alpha=.8f))}}}}
  item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){Summary("Income",c);Summary("Expenses",e)}}
  item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){Summary("You owe",loans.sumOf{it.remaining});Summary("Owed to you",lends.sumOf{it.remaining})}}
  item{Text("Recent activity",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)}
  if(tx.isEmpty()) item{Text("No transactions yet",color=MaterialTheme.colorScheme.onSurfaceVariant)}
  else items(tx.take(5),key={it.id}){t->Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=Color.White)){ListItem(headlineContent={Text(t.description.ifBlank{t.type},maxLines=1)},supportingContent={Text(date(t.date)+" • "+t.source)},trailingContent={Text((if(t.type=="EXPENSE")"−" else "+")+" ₹"+String.format(Locale.getDefault(),"%,.2f",t.amount),color=if(t.type=="EXPENSE")Negative else Positive,fontWeight=FontWeight.Bold)})}}}
 }
}
@Composable fun Summary(t:String,v:Double){
 Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=Color.White)){
  Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
   Text(t,style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)
   Text("₹ "+String.format(Locale.getDefault(),"%,.2f",v),style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
  }
 }
}
@Composable fun Transactions(vm:BudgetViewModel){
 val list by vm.transactions.collectAsState(emptyList());var show by remember{mutableStateOf(false)}
 Column(Modifier.padding(12.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("Transactions",style=MaterialTheme.typography.headlineSmall);Button({show=true}){Text("Add")}}
 LazyColumn(Modifier.weight(1f)){items(list){t->ListItem(headlineContent={Text(t.description.ifBlank{t.type})},supportingContent={Text(date(t.date)+" • "+t.source)},trailingContent={Text((if(t.type=="EXPENSE")"-" else "+")+" ₹"+"%.2f".format(t.amount))},modifier=Modifier.fillMaxWidth())}}}
 if(show)TransactionDialog({show=false},vm)
}
@Composable fun TransactionDialog(close:()->Unit,vm:BudgetViewModel){
 var a by remember{mutableStateOf("")};var d by remember{mutableStateOf("")};var s by remember{mutableStateOf("default")};var credit by remember{mutableStateOf(false)}
 AlertDialog(onDismissRequest=close,title={Text(if(credit)"Add Credit" else "Add Expense")},text={Column{OutlinedTextField(a,{a=it},label={Text("Amount")});OutlinedTextField(d,{d=it},label={Text("Description")});OutlinedTextField(s,{s=it},label={Text("Source")});Row{Checkbox(credit,{credit=!credit});Text("Credit",Modifier.padding(top=12.dp))}}},confirmButton={Button({a.toDoubleOrNull()?.takeIf{it>0}?.let{if(credit)vm.addCredit(it,d,s)else vm.addExpense(it,d,s)};close()}){Text("Save")}},dismissButton={TextButton(close){Text("Cancel")}})
}
@Composable fun Analytics(vm:BudgetViewModel){
 val list by vm.transactions.collectAsState(emptyList());val filters by vm.filters.collectAsState(emptyList());var month by remember{mutableIntStateOf(Calendar.getInstance().get(Calendar.MONTH))};var year by remember{mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR))}
 val cal=Calendar.getInstance();cal.set(year,month,1,0,0,0);val from=cal.timeInMillis;cal.add(Calendar.MONTH,1);val to=cal.timeInMillis
 val selected=list.filter{it.date>=from&&it.date<to&&!filters.any{f->it.description.contains(f.word,true)}};val exp=selected.filter{it.type=="EXPENSE"}.sumOf{it.amount};val cr=selected.filter{it.type=="CREDIT"}.sumOf{it.amount}
 Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text("Analytics",style=MaterialTheme.typography.headlineSmall);Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button({if(month==0){month=11;year--}else month--}){Text("<")};Text(SimpleDateFormat("MMMM yyyy",Locale.getDefault()).format(Date(from)),Modifier.padding(top=12.dp));Button({if(month==11){month=0;year++}else month++}){Text(">")}};Summary("Monthly credits",cr);Summary("Monthly expenses",exp);Summary("Monthly balance",cr-exp);Text("Transactions: "+selected.size);Text("Excluded keywords: "+filters.joinToString{it.word})}
}
@Composable fun PaymentDialog(title:String,max:Double,save:(Double,Double)->Unit,close:()->Unit){
 var p by remember{mutableStateOf("")};var i by remember{mutableStateOf("")}
 AlertDialog(onDismissRequest=close,title={Text(title)},text={Column{Text("Principal cannot exceed ₹%.2f".format(max));OutlinedTextField(p,{p=it},label={Text("Principal")});OutlinedTextField(i,{i=it},label={Text("Interest")})}},confirmButton={Button({val pv=p.toDoubleOrNull()?:0.0;val iv=i.toDoubleOrNull()?:0.0;if(pv in 0.0..max&&iv>=0)save(pv,iv)}){Text("Save")}},dismissButton={TextButton(close){Text("Cancel")}})
}
@Composable fun Loans(vm:BudgetViewModel){
 val loans by vm.loans.collectAsState(emptyList());var add by remember{mutableStateOf(false)};var payment by remember{mutableStateOf<Loan?>(null)}
 Column(Modifier.padding(12.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("Loans",style=MaterialTheme.typography.headlineSmall);Button({add=true}){Text("Add Loan")}}
 LazyColumn(Modifier.weight(1f)){items(loans){l->Card(Modifier.fillMaxWidth().padding(4.dp)){Column(Modifier.padding(12.dp)){Text(l.name,style=MaterialTheme.typography.titleMedium);Text("Original ₹%.2f • Remaining ₹%.2f".format(l.amount,l.remaining));Text("Accrued interest ₹%.2f".format(vm.interest(l.lastInterestPaid,System.currentTimeMillis(),l.remaining,l.interestRs,l.interestPct)));Row{Button({payment=l}){Text("Payment")};TextButton({vm.deleteLoan(l)}){Text("Delete")}}}}}}}
 if(add)LoanDialog({add=false},vm);payment?.let{l->PaymentDialog("Loan payment",l.remaining,{p,i->vm.addLoanPayment(l,p,i);payment=null},{payment=null})}
}
@Composable fun LoanDialog(close:()->Unit,vm:BudgetViewModel){
 var n by remember{mutableStateOf("")};var a by remember{mutableStateOf("")};var rs by remember{mutableStateOf("")};var pct by remember{mutableStateOf("")}
 AlertDialog(onDismissRequest=close,title={Text("Add Loan")},text={Column{OutlinedTextField(n,{n=it},label={Text("Loan name")});OutlinedTextField(a,{a=it},label={Text("Amount")});OutlinedTextField(pct,{pct=it},label={Text("Annual interest %")});OutlinedTextField(rs,{rs=it},label={Text("Interest ₹ / month")})}},confirmButton={Button({a.toDoubleOrNull()?.let{vm.addLoan(n,it,rs.toDoubleOrNull()?:0.0,pct.toDoubleOrNull()?:0.0)};close()}){Text("Save")}},dismissButton={TextButton(close){Text("Cancel")}})
}
@Composable fun Lend(vm:BudgetViewModel){
 val list by vm.lendings.collectAsState(emptyList());var add by remember{mutableStateOf(false)};var payment by remember{mutableStateOf<Lending?>(null)}
 Column(Modifier.padding(12.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("Lend",style=MaterialTheme.typography.headlineSmall);Button({add=true}){Text("Add Lend")}}
 LazyColumn(Modifier.weight(1f)){items(list){l->Card(Modifier.fillMaxWidth().padding(4.dp)){Column(Modifier.padding(12.dp)){Text(l.name,style=MaterialTheme.typography.titleMedium);Text("Lent ₹%.2f • Remaining ₹%.2f".format(l.amount,l.remaining));Text("Interest ₹%.2f".format(vm.interest(l.lastInterestPaid,System.currentTimeMillis(),l.remaining,l.interestRs,l.interestPct)));Row{Button({payment=l}){Text("Received")};TextButton({vm.deleteLending(l)}){Text("Delete")}}}}}}}
 if(add)LendDialog({add=false},vm);payment?.let{l->PaymentDialog("Lend received payment",l.remaining,{p,i->vm.addLendingPayment(l,p,i);payment=null},{payment=null})}
}
@Composable fun LendDialog(close:()->Unit,vm:BudgetViewModel){
 var n by remember{mutableStateOf("")};var a by remember{mutableStateOf("")};var r by remember{mutableStateOf("")}
 AlertDialog(onDismissRequest=close,title={Text("Add Lend")},text={Column{OutlinedTextField(n,{n=it},label={Text("Person / purpose")});OutlinedTextField(a,{a=it},label={Text("Amount")});OutlinedTextField(r,{r=it},label={Text("Annual interest %")})}},confirmButton={Button({a.toDoubleOrNull()?.let{vm.addLending(n,it,0.0,r.toDoubleOrNull()?:0.0)};close()}){Text("Save")}},dismissButton={TextButton(close){Text("Cancel")}})
}
@Composable fun Admin(vm:BudgetViewModel){
 val context=LocalContext.current
 val sources by vm.sources.collectAsState(emptyList())
 val filters by vm.filters.collectAsState(emptyList())
 var s by remember{mutableStateOf("")};var f by remember{mutableStateOf("")};var initial by remember{mutableStateOf("")}
 var showDelete by remember{mutableStateOf(false)}
 val backupLauncher=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri->
  if(uri!=null) vm.backupJson{json->try{if(json==null)throw IllegalStateException();context.contentResolver.openOutputStream(uri)?.use{it.write(json.toByteArray())};Toast.makeText(context,"Backup saved",Toast.LENGTH_SHORT).show()}catch(e:Exception){Toast.makeText(context,"Backup failed",Toast.LENGTH_LONG).show()}}
 }
 val restoreLauncher=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
  if(uri!=null) try{
   val json=context.contentResolver.openInputStream(uri)?.bufferedReader()?.use{it.readText()} ?: ""
   vm.restoreJson(json){ok,msg->Toast.makeText(context,msg,Toast.LENGTH_LONG).show()}
  }catch(e:Exception){Toast.makeText(context,"Could not read backup file",Toast.LENGTH_LONG).show()}
 }
 Column(Modifier.padding(16.dp).fillMaxSize(),verticalArrangement=Arrangement.spacedBy(12.dp)){
  Text("Settings & Data",style=MaterialTheme.typography.headlineSmall)
  Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
   Text("Starting balance",style=MaterialTheme.typography.titleMedium)
   OutlinedTextField(initial,{initial=it},label={Text("Initial balance")},modifier=Modifier.fillMaxWidth())
   Button({initial.toDoubleOrNull()?.let{vm.setInitialAmount(it,System.currentTimeMillis())}},modifier=Modifier.fillMaxWidth()){Text("Save starting balance")}
  }}
  Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
   Text("Backup & restore",style=MaterialTheme.typography.titleMedium)
   Text("Keep a copy of your transactions, loans, lending records, sources and filters.")
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
    OutlinedButton({backupLauncher.launch("budget-tracker-backup.json")},Modifier.weight(1f)){Text("Backup")}
    Button({restoreLauncher.launch(arrayOf("application/json","text/plain"))},Modifier.weight(1f)){Text("Restore")}
   }
  }}
  Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
   Text("Sources",style=MaterialTheme.typography.titleMedium)
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(s,{s=it},label={Text("New source")},modifier=Modifier.weight(1f));Button({vm.addSource(s);s=""}){Text("Add")}}
   Text(sources.joinToString(" • "){it.name},style=MaterialTheme.typography.bodySmall)
  }}
  Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
   Text("Analytics filters",style=MaterialTheme.typography.titleMedium)
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(f,{f=it},label={Text("Exclude keyword")},modifier=Modifier.weight(1f));Button({vm.addFilter(f);f=""}){Text("Add")}}
   Text(filters.joinToString(" • "){it.word},style=MaterialTheme.typography.bodySmall)
  }}
  Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
   Text("Danger zone",style=MaterialTheme.typography.titleMedium)
   Text("Permanently removes all transactions, loans, lending records, payments, sources and filters from this device.")
   OutlinedButton({showDelete=true},modifier=Modifier.fillMaxWidth()){Text("Delete all data")}
  }}
 }
 if(showDelete)AlertDialog(
  onDismissRequest={showDelete=false},
  title={Text("Delete all data?")},
  text={Text("This cannot be undone. If you need the data later, create a backup first.")},
  confirmButton={Button({showDelete=false;vm.deleteAllData{ok->Toast.makeText(context,if(ok)"All data deleted" else "Delete failed",Toast.LENGTH_LONG).show()}}){Text("Delete permanently")}},
  dismissButton={TextButton({showDelete=false}){Text("Cancel")}}
 )
}