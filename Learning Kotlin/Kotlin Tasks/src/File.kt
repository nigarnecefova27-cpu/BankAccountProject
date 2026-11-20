import java.util.Date

enum class TransactionType(){
    DEPOSIT, WITHDRAW, TRANSFER_IN,TRANSFER_OUT, INTEREST
}

data class Customer(
    val id: String,
    val fullName: String,
    val email: String,
    val phone: String
)

data class Transaction(
    val id: String,
    val accountId: String,
    val amount: Double,
    val fee: Double, //komissiya
    val date: Date,
    val type: TransactionType,
    val relatedAccountId: String? //deposit, withdraw
)

object IdGenerator {
    private var counter = 0
    fun nextTxId(): String {
        counter = counter + 1
        return "TX-$counter"
    }
}

sealed class OperationResult {
    data class Success(val message: String, val newBalance: Double) : OperationResult()
    data class Failure(val reason: String) : OperationResult()
}

interface Notifier {
    fun notify(customer: Customer, message: String)
}
class ConsoleNotifier(): Notifier {
    override fun notify(customer: Customer, message: String) {
        println(" ${customer.fullName} -> $message ")
    }
}


interface AccountRepository {
    fun save(account: Account)
    fun findById(id: String): Account?
    fun findAll(): List<Account>
}
class InMemoryAccountRepository : AccountRepository {
    private val accounts = mutableListOf<Account>()
    override fun save(account: Account){
        accounts.add(account)
    }
    override fun findById(id: String): Account?{
        return accounts.find { it.id == id }
    }
    override fun findAll(): List<Account> {
        return accounts.toList()
    }
}


interface TransactionRepository {
    fun saveTransaction(transaction: Transaction)
    fun getTransaction(transactionId: String): Transaction?
    fun findTransactionByAccount(accountId: String): List<Transaction>
}
class InMemoryTransactionRepository : TransactionRepository{
    private val transactions = mutableListOf<Transaction>()
    override fun saveTransaction(transaction:Transaction){
        transactions.add(transaction)
    }

    override fun getTransaction(transactionId: String):Transaction? {
        return transactions.find { it.id == transactionId }
    }

    override fun findTransactionByAccount(accountId: String): List<Transaction> {
        return transactions.filter { it.accountId == accountId }
    }
}

//transaction types
interface Withdrawable{
    fun fee(amount: Double): Double
    fun withdraw(amount: Double) :OperationResult
}

class TransferService(
    private val accounts: AccountRepository,
    private val transactions: TransactionRepository,
    private val notifier: Notifier
){
    fun transfer(fromId: String, toId: String, amount: Double): OperationResult {
        if (fromId==toId){
            return OperationResult.Failure("Cannot transfer to the same account")
        }
        if (amount<=0){
            return OperationResult.Failure("Amount must be positive")
        }
        val fromAccount = accounts.findById(fromId)?: return OperationResult.Failure("Source account $fromId not found")
        val toAccount = accounts.findById(toId)?: return OperationResult.Failure("Target account $toId not found")

        val withdrawable = fromAccount as? Withdrawable ?: return OperationResult.Failure("Account $fromId does not support withdrawals")

        val fee=withdrawable.fee(amount)
        val withdrawableResult = withdrawable.withdraw(amount)
        if (withdrawableResult is OperationResult.Failure) {
            return withdrawableResult
        }

        val depositResult = toAccount.deposit(amount)
        if (depositResult is OperationResult.Failure) {
            return depositResult
        }
        transactions.saveTransaction(  //transfer out
            Transaction(
                id = IdGenerator.nextTxId(),
                accountId = fromAccount.id,
                amount = amount,
                fee = fee,
                date = Date(),
                type = TransactionType.TRANSFER_OUT,
                relatedAccountId = toAccount.id
            )
        )

        transactions.saveTransaction(   //transfer in
            Transaction(
                id = IdGenerator.nextTxId(),
                accountId = toAccount.id,
                amount = amount,
                fee = 0.0,
                date = Date(),
                type = TransactionType.TRANSFER_IN,
                relatedAccountId = fromAccount.id
            )
        )
        notifier.notify(
            fromAccount.owner,
            "Transferred ${"%.2f".format(amount)} from ${fromAccount.id} to ${toAccount.id}, fee ${"%.2f".format(fee)}"
        )
        notifier.notify(
            toAccount.owner,
            "Received ${"%.2f".format(amount)} in ${toAccount.id} from ${fromAccount.id}"
        )

        return OperationResult.Success(
            "Transferred ${"%.2f".format(amount)} from ${fromAccount.id} to ${toAccount.id}",
            fromAccount.balance
        )
    }
}

interface Resettable{
    fun resetCounter()
}

interface InterestBearing{
    fun applyMonthlyInterest() : Double
}

class MonthEndService(
    private val accounts: AccountRepository,
    private val transactions: TransactionRepository,
    private val notifier: Notifier
){
    fun run(){
        for(account in accounts.findAll()){
            val bearing=account as? InterestBearing
            if (bearing != null){
                val interest = bearing.applyMonthlyInterest()

                transactions.saveTransaction(Transaction(
                    IdGenerator.nextTxId(),
                    account.id,
                    interest,
                    0.0,
                    Date(),
                    TransactionType.INTEREST,
                    null
                )
                )
                notifier.notify(account.owner, "Interest ${"%.2f".format(interest)} added to ${account.id}")
            }
            (account as? Resettable)?.resetCounter()
        }
    }
}

class AccountService( //open,deposit,withdraw
    private val accounts: AccountRepository,
    private val transactions: TransactionRepository,
    private val notifier: Notifier
){
    fun open(account: Account):OperationResult{
        accounts.save(account)
        notifier.notify(account.owner,"Account ${account.id}")
        return OperationResult.Success("Account ${account.id} opened",account.balance)
    }
    fun deposit(accountId: String, amount: Double): OperationResult{
        val account= accounts.findById(accountId)?: return OperationResult.Failure("Account $accountId not found")

        val result=account.deposit(amount)
        if (result is OperationResult.Success){
            transactions.saveTransaction(
                Transaction(
                    IdGenerator.nextTxId(),
                    account.id,
                    amount,
                    0.0,
                    Date(),
                    TransactionType.DEPOSIT,
                    null
                )
            )
            notifier.notify(account.owner,"Deposited ${"%.2f".format(amount)} to ${account.id}")
        }
        return result
    }

    fun withdraw(accountId: String, amount: Double): OperationResult {
        val account = accounts.findById(accountId) ?: return OperationResult.Failure("Account $accountId not found")
        val withdrawable = account as? Withdrawable
            ?: return OperationResult.Failure("Account $accountId does not support withdrawals")

        val fee = withdrawable.fee(amount)
        val result = withdrawable.withdraw(amount)

        if (result is OperationResult.Success) {
            transactions.saveTransaction(
                Transaction(
                    IdGenerator.nextTxId(),
                    account.id,
                    amount,
                    fee,
                    Date(),
                    TransactionType.WITHDRAW,
                    null
                )
            )
            notifier.notify(account.owner,"Withdrew ${"%.2f".format(amount)} from ${account.id}, fee ${"%.2f".format(fee)}")
        }
        return result
    }
}


abstract class Account(
    val id: String,
    val owner: Customer,
    initialBalance: Double = 0.0
){
    var balance: Double = initialBalance
        protected set

    val openingBalance: Double = initialBalance

    abstract val typeName: String

    open fun deposit(amount: Double): OperationResult {
        if (amount <= 0)
            return OperationResult.Failure("Amount must be positive")
        balance += amount
        return OperationResult.Success("Deposited $amount", balance)
    }
    override fun toString() ="$id $typeName ${owner.fullName} ${"%.2f".format(balance)}"
}



class CheckingAccount(id: String, owner: Customer,initialBalance: Double=0.0 ) : Account(id, owner,initialBalance), Withdrawable{

    override val typeName="Checking"

    private val limit: Double = -500.0

    override fun fee(amount: Double): Double = amount * 0.01

    override fun withdraw(amount: Double):OperationResult {
        if (amount <= 0)
            return OperationResult.Failure("Amount must be positive")
        val fee=fee(amount)
        val temporaryBalance=balance-(amount+fee)
        if (temporaryBalance >= limit){ //overdraft
            balance = temporaryBalance
            return OperationResult.Success("Withdrew $amount (fee $fee)", balance)
        }else{
            return OperationResult.Failure("Overdraft limit exceeded")        }
    }

}

class SavingsAccount(
    id: String,
    owner: Customer,
    initialBalance: Double=0.0):
    Account(id, owner,initialBalance),
    Withdrawable,InterestBearing,Resettable{

    override val typeName="Savings"

    private val maxWithdrawalsPerMonth=3
    private var withdrawalCount=0
    private val interestRate =0.02

    override fun fee(amount: Double): Double=1.00

    override fun withdraw(amount: Double) : OperationResult{
        if (amount <= 0)
            return OperationResult.Failure("Amount must be positive")
        if (withdrawalCount >= maxWithdrawalsPerMonth) {
            return OperationResult.Failure("Monthly withdrawal limit reached")
        }

        val fee=fee(amount)
        val temporaryBalance = balance - (amount+fee)
        if (temporaryBalance >= 0) { //overdraft
            balance = temporaryBalance
            withdrawalCount++
            return OperationResult.Success("Withdraw $amount (fee $fee)", balance)
        } else {
            return OperationResult.Failure("Insufficient funds")
        }
    }
    override fun applyMonthlyInterest(): Double {
        val interest=balance * interestRate
        balance += interest
        return interest
    }

    override fun resetCounter() {
        withdrawalCount=0
    }
}

class DepositAccount(id: String,owner: Customer,initialBalance: Double=0.0 ): Account(id, owner,initialBalance),InterestBearing{

    override val typeName="Deposit"

    private val interestRate = 0.01
    private val termMonths = 12
    private var monthsPassed = 0

    override fun deposit(amount: Double): OperationResult {
        if (monthsPassed>=termMonths) {
            return OperationResult.Failure("Term has ended, deposits are not allowed")
        }
        return super.deposit(amount)
    }

    override fun applyMonthlyInterest(): Double {
        val interest=balance * interestRate
        balance += interest
        monthsPassed++
        return interest
    }
}


class StatementPrinter(
    private val accounts: AccountRepository,
    private val transactions: TransactionRepository
) {
    fun print(accountId: String) {
        val account = accounts.findById(accountId) ?: return
        val accountTransactions = transactions.findTransactionByAccount(accountId)

        println("Statement for ${account.id} (${account.owner.fullName})")
        println("Opening balance:  %15.2f".format(account.openingBalance))

        for (tx in accountTransactions) {
            val sign = when (tx.type) {
                TransactionType.DEPOSIT, TransactionType.TRANSFER_IN, TransactionType.INTEREST -> "+"
                TransactionType.WITHDRAW, TransactionType.TRANSFER_OUT -> "-"
            }
            val amountStr = "$sign${"%.2f".format(tx.amount)}"
            val extra = when (tx.type) {
                TransactionType.TRANSFER_IN -> tx.relatedAccountId?.let { "  from $it" } ?: ""
                TransactionType.TRANSFER_OUT -> tx.relatedAccountId?.let { "  to $it" } ?: ""
                else -> if (tx.fee > 0.0) "  fee ${"%.2f".format(tx.fee)}" else ""
            }
            println("%-4s  %-11s  %8s$extra".format(tx.id, tx.type.name, amountStr))
        }

        println("Closing balance:  %15.2f".format(account.balance))
    }
}


fun main() {
    val accounts = InMemoryAccountRepository()
    val transactions = InMemoryTransactionRepository()
    val notifier: Notifier = ConsoleNotifier()

    val accountService = AccountService(accounts, transactions, notifier)
    val transferService = TransferService(accounts, transactions, notifier)
    val monthEnd = MonthEndService(accounts, transactions, notifier)
    val printer = StatementPrinter(accounts, transactions)

    val aysel = Customer("CU-1", "Aysel Mammadova", "aysel@mail.az", "+994501112233")
    val murad = Customer("CU-2", "Murad Aliyev", "murad@mail.az", "+994552223344")

    accountService.open(CheckingAccount("C-1", aysel, initialBalance = 1000.0))
    accountService.open(SavingsAccount("S-1", aysel, initialBalance = 2000.0))
    accountService.open(DepositAccount("D-1", murad, initialBalance = 5000.0))

    println("\n--- Operations ---")
    println("1. " + accountService.deposit("C-1", 200.0))
    println("2. " + accountService.withdraw("C-1", 300.0))
    println("3. " + accountService.withdraw("C-1", 1300.0))
    println("4. " + accountService.withdraw("C-1", 100.0))
    println("5. " + transferService.transfer("S-1", "C-1", 500.0))
    println("6. " + accountService.withdraw("D-1", 100.0))
    repeat(3) { i -> println("${7 + i}. " + accountService.withdraw("S-1", 50.0)) }

    println("\n--- Month end ---")
    monthEnd.run()

    println("\n--- Balances ---")
    accounts.findAll().forEach(::println)

    println()
    printer.print("C-1")
}



