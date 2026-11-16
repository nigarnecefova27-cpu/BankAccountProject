//sealed class Result{  //sealed lass constructoru private dir basqa package de islemir
//    class Fail(val reason: String): Result()
//
//    class Success(val amount: Double): Result()
//}
//
//
//fun withdraw(amount:Double):Result{
//    return if (amount <= 0){
//         Fail("Amount must be greater than zero")
//    }
//    else if(amount > 100){
//         Fail("Amount must be smaller than 100")
//    }
//    else{
//        Success(amount+amount*0.1)
//    }
//}
//
//
//fun main(){
//    val process:Result =withdraw(2.0)
//
//    when(process){ //is relation
//        is Success->{
//            println("Success")
//        }
//        is Fail->{
//            println("Fail")
//        }
//        else -> {
//            println("Another operation")
//        }
//    }
//}