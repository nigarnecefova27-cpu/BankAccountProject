import java.util.Date

enum class TransactionType(){
    DEPOSIT, WITHDRAW, TRANSFER_IN,TRANSFER_OUT, INTEREST
}

data class Customer(
    val id: String,
    val fullName: String,
    val email: String,
    val phone: String,
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

object IdGenerator {  //?
    private var counter = 0
    fun nextTxId(): String {
        counter = counter + 1
        return "TX-$counter"
    }
}

interface TransactionRepository {
    fun saveTransaction(transaction: Transaction)

    fun getTransaction(transactionId: String): Transaction?
}

class InMemoryTransactionRepository : TransactionRepository{
    private val transactions = mutableListOf<Transaction>()
    override fun saveTransaction(transaction:Transaction){
        transactions.add(transaction)
    }

    override fun getTransaction(transactionId: String):Transaction? {
        return transactions.find { it.id == transactionId }
    }
}

sealed class OperationResult {
    data class Success(val message: String, val newBalance: Double) : OperationResult()
    data class Failure(val reason: String) : OperationResult()
}

interface Withdrawable{
    fun fee(amount: Double): Double
    fun withdraw(amount: Double) :OperationResult
}

interface InterestBearing{
    fun monthlyInterest() : Double
}


//?
abstract class Account(
    val id: String,
    val owner: Customer,
    protected val transactionRepository: TransactionRepository) {

    var balance: Double = 0.0
        protected set

    abstract val typeName: String

    open fun deposit(amount: Double): OperationResult {
        if (amount <= 0)
            return OperationResult.Failure("Amount must be positive")
        balance += amount
        return OperationResult.Success("Deposited $amount", balance)
    }
    override fun toString() =
        "$id $typeName ${owner.fullName} ${"%.2f".format(balance)}"
}



class CheckingAccount(
    id: String,
    owner: Customer,
    transactionRepository: TransactionRepository) :
    Account(id, owner,transactionRepository), Withdrawable{

    override val typeName="Checking"
    private fun buildTransaction(
        amount: Double,
        fee: Double,
        type : TransactionType,
        relatedAccountId: String? = null
    ): Transaction {
        return Transaction(
            id =IdGenerator.nextTxId(),
            accountId = this.id,
            type = type,
            amount = amount,
            fee = fee,
            date = Date(),
            relatedAccountId=relatedAccountId
        )
    }

    private var limit: Double = -500.0

    override fun fee(amount: Double): Double = amount * 0.01

    override fun withdraw(amount: Double):OperationResult {
        if (amount <= 0)
            return OperationResult.Failure("Amount must be positive")
        val fee=fee(amount)
        val temporaryBalance=balance-(amount+fee)
        if (temporaryBalance >= limit){ //overdraft
            balance = temporaryBalance
            val transaction = buildTransaction(amount,fee,TransactionType.WITHDRAW)
            transactionRepository.saveTransaction(transaction)
            return OperationResult.Success("Withdrew $amount (fee $fee)", balance)
        }else{
            return OperationResult.Failure("Overdraft limit exceeded")        }
    }

}

class SavingsAccount(
    id: String,
    owner: Customer,
    transactionRepository: TransactionRepository):
    Account(id, owner,transactionRepository),Withdrawable,InterestBearing{

    override val typeName="Savings"

    private val maxWithdrawalsPerMonth=3
    private var withdrawalCount=0
    private val interestRate =0.02

    private fun buildTransaction(
        amount: Double,
        fee: Double,
        type : TransactionType,
        relatedAccountId: String? = null
    ): Transaction {
        return Transaction(
            id = IdGenerator.nextTxId(),
            accountId=this.id,
            type = type,
            amount = amount,
            fee=fee,
            relatedAccountId=relatedAccountId,
            date = Date()
        )
    }

    override fun fee(amount: Double): Double=1.0

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
            val transaction = buildTransaction(amount,fee,TransactionType.WITHDRAW)
            transactionRepository.saveTransaction(transaction)
            return OperationResult.Success("Withdraw $amount (fee $fee)", balance)
        } else {
            return OperationResult.Failure("Insufficient funds")
        }
    }
    override fun monthlyInterest(): Double {
        val interest=balance * interestRate
        balance += interest
        return interest
    }
}

class DepositAccount(
    id: String,
    owner: Customer,
    transactionRepository: TransactionRepository):
    Account(id, owner,transactionRepository),InterestBearing{

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

    override fun monthlyInterest(): Double {
        val interest=balance * interestRate
        balance += interest
        monthsPassed++
        return interest
    }
}















