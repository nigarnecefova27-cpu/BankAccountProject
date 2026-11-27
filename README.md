# Mini Bank — Kotlin OOP & SOLID
Pure Kotlin console implementation of a mini banking system built with object-oriented design, capability interfaces, `OperationResult` modeling, and constructor-injected services wired in `main()`.

## Domain Model
* **Data & Enum Types:** `Customer`, `Transaction`, `TransactionType` (`DEPOSIT`, `WITHDRAW`, `TRANSFER_IN`, `TRANSFER_OUT`, `INTEREST`), and sealed `OperationResult` (`Success`, `Failure`).
* **ID Generation:** `IdGenerator` object generating sequential IDs (`TX-1`, `TX-2`, ...).
* **Account Hierarchy (`Account`):** Holds `id`, `owner`, `openingBalance`, and `balance` (public getter, `protected set`), along with `deposit(amount)` and formatted `toString()`.
  * `CheckingAccount`: Overdraft limit down to `-500.00`, `1%` withdrawal fee, implements `Withdrawable`.
  * `SavingsAccount`: Up to `3` withdrawals per month, `1.00` withdrawal fee, `2%` monthly interest, implements `Withdrawable`, `InterestBearing`, and `Resettable`.
  * `DepositAccount`: Does not implement `Withdrawable`, applies `1%` monthly interest up to `termMonths` (`12`), and rejects deposits once the term ends; implements `InterestBearing`.

## Services & Architecture

* **Repositories:** `AccountRepository` (`InMemoryAccountRepository`) and `TransactionRepository` (`InMemoryTransactionRepository`).
* **Notifications:** `Notifier` interface with `ConsoleNotifier` passed into services via constructors.
* **Services:**
  * `AccountService`: Handles `open`, `deposit`, and `withdraw`, saving transactions and sending notifications on success.
  * `TransferService`: Validates accounts, `Withdrawable` capability, and operation outcomes before recording paired `TRANSFER_OUT` and `TRANSFER_IN` transactions and notifying both owners.
  * `MonthEndService`: Iterates through accounts using capability checks (`InterestBearing` and `Resettable`) without concrete subclass checks.
  * `StatementPrinter`: Formats account statements (opening balance, transactions with `from`/`to` or `fee`, and closing balance) without modifying state.

## Design Notes

1. **Adding a new notification channel (e.g. `SmsNotifier`):** Only a new class implementing `Notifier` and changing the `Notifier` instance in `main()` are needed because services depend on the `Notifier` abstraction.
2. **Why `DepositAccount` does not implement `Withdrawable`:** Omitting the interface reflects capability accurately in the type system instead of exposing an unsupported withdrawal operation.
3. **Where fee rules live:** Each `Withdrawable` account defines its own `fee(amount)` logic (`CheckingAccount` vs. `SavingsAccount`), keeping fee rules encapsulated with the account rather than inside services.
4. **If `MonthEndService` depended on `Account` instead of `InterestBearing`:** Interest processing would either expose non-interest accounts to interest logic or require explicit type branching.
5. **`account as? Withdrawable` vs. `account is SavingsAccount`:** Checking the capability interface works for any withdrawable account type without changing existing code, whereas checking a concrete class ties behavior to a specific type.
6. **How `transfer()` protects state:** Checks and `OperationResult` guards run before saving `TRANSFER_OUT` / `TRANSFER_IN` records or sending notifications, preventing partial updates when validation or execution fails.
