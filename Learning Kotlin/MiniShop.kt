fun main(){
    //1
    kotlin.io.println("----- MY MINI SHOP -----")
    val p1=Product("Laptop",1200.0,5)
    val p2 = Product("Mouse", 25.0, 12)
    val p3 = Product("Keyboard", 45.0, 8)
    var p4 = Product("Monitor", 300.0, 4)
    var p5 = Product("Headset", 60.0, 10)
    var p6 = Product("Phone", 850.0, 3)

    var arrayOfProducts = kotlin.arrayOf(p1, p2, p3, p4, p5, p6)

    for (product in arrayOfProducts) {
        product.printMiniShop(arrayOfProducts)
    }
    kotlin.io.println()

    //2
    val orderNames = kotlin.arrayOf("Mouse", "Laptop", "Laptop", "Unknown", "Monitor", "Phone", "Keyboard", "Headset")
    val orderAmounts= kotlin.intArrayOf(2, 5, 1, 3, 0, 10, 3, 4)


    //3
    val myShop=Shop(arrayOfProducts)
    kotlin.io.println("----- Order Report -----")
    for (i in orderNames.indices) {
        myShop.processOrder(orderNames[i], orderAmounts[i])
    }
    kotlin.io.println()

    //4
    kotlin.io.println("----- Final stocks -----")
    myShop.printInvertory(arrayOfProducts)
    kotlin.io.println()

    //5
    var discountPercent =20
    kotlin.io.println("Percent: ${discountPercent} % ")
    kotlin.io.println("--- Product - New Price ---")
    myShop.applyDiscount(discountPercent){
        it.stock<5
    }
    kotlin.io.println()

    discountPercent=10
    kotlin.io.println("Percent: ${discountPercent} % ")
    kotlin.io.println("--- Product - New Price ---")
    myShop.applyDiscount(discountPercent){
        it.price>1000
    }
    kotlin.io.println()

    discountPercent=95
    kotlin.io.println("Percent: ${discountPercent} % ")
    kotlin.io.println("--- Product - New Price ---")
    myShop.applyDiscount(discountPercent){
        it.stock<5
    }
    kotlin.io.println()
    for (product in arrayOfProducts) {
        product.printMiniShop(arrayOfProducts)
    }
    kotlin.io.println()

    //6
    kotlin.io.println("----- Final stocks again -----")
    myShop.printInvertory(arrayOfProducts)
    kotlin.io.println()
    //7
    kotlin.io.println("---Total revenue: ${myShop.revenue} AZN")

    val totalInventoryValues = myShop.total {
        it.price * it.stock
    }
    kotlin.io.println("---Total Inventory Values: $totalInventoryValues AZN")

    val totalStocks=myShop.total{
        it.stock.toDouble()
    }
    kotlin.io.println("---Total of all products: ${totalStocks.toInt()}")
    kotlin.io.println()

    //8
    //built-in function ?
    kotlin.io.println("----- Out of stocks -----")
    val outOfStockNames = myShop.products
        .filter { it.stock == 0 }
        .map { it.name }
    kotlin.io.println(outOfStockNames)

    kotlin.io.println()
    kotlin.io.println("----- The most expensive product -----")
    val maxPrice = myShop.products
        .maxByOrNull { it.price }
    if (maxPrice != null) {
        kotlin.io.println(" ${maxPrice.name} - ${maxPrice.price} AZN")
    }
    kotlin.io.println()

    //BONUS
    kotlin.io.println("----- RESTOCK -----")
    kotlin.io.println("threshold = 50 , amount = 10")
    myShop.restock(threshold = 50, amount = 10)

}


class Product(val name: String, var price: Double, var stock:Int){
    fun printMiniShop(arrayOfProducts: Array<Product>){
        kotlin.io.println("$name | $price   | $stock")
    }

    fun isAvailable(): Boolean {
        return  stock > 0
    }

    fun sell(amount:Int):Boolean{
        if (amount <= 0){
            kotlin.io.println("$name ->Invalid amount")
            return false
        }
        else if (stock == 0){
            kotlin.io.println("$name ->Out of stock")
            return false
        }
        else if (amount>stock){
            kotlin.io.println("$name ->Only $stock left")
            return false
        }else {
            stock = stock - amount
            var total=amount*price
            kotlin.io.println("Sold $amount × $name for $total AZN")
            return true
        }
    }
}


class Shop(val products: Array<Product>, var revenue:Double=0.0){
    fun findProduct(name:String):Product?{
        for (product in products){
            if (product.name.equals(name,ignoreCase = true)) {
                return product
            }
        }
        return null
    }

    fun processOrder(name: String,amount:Int){
        val product = findProduct(name)
        if (product == null){
            kotlin.io.println("Product '$name' not found")
        }else{
            val isSold=product.sell(amount)
            if (isSold){
                revenue=revenue + amount*product.price
            }
        }
    }

    fun printInvertory(arrayOfProducts:Array<Product>){
        for (i in 0 until arrayOfProducts.size){
            if (products[i].isAvailable()){
                kotlin.io.println(" ${i + 1} . ${arrayOfProducts[i].name}   |  ${arrayOfProducts[i].stock}")
            }else{
                kotlin.io.println(" ${i + 1} . ${arrayOfProducts[i].name}   |  OUT OF STOCK ")

            }
        }
    }

    fun applyDiscount(percent:Int,condition: (Product)->Boolean) {
        if (percent >= 1 && percent <= 90) {
            for (i in 0 until products.size) {
                if (condition(products[i])) {
                    products[i].price -= products[i].price * percent / 100
                    kotlin.io.println("${i + 1}. ${products[i].name}     ${products[i].price}")
                } else {
                    kotlin.io.println("${i + 1}. ${products[i].name}      ERROR ")
                    return
                }
            }
        }else {
            kotlin.io.println(" --- ERROR --- ")
            return
            }
    }


    fun total(selector:(Product)->Double): Double{
        var sum=0.0
        for (product in products){
            sum+=selector(product)
        }
        return sum
    }

    fun restock(threshold: Int,amount:Int){
        var i=0
        for (product in products){
            if (product.price<threshold){
                i++
                product.price +=amount
                kotlin.io.println("${product.name} is restocked! New price is ${product.price} .")
            }
        }
        if (i==0){
            kotlin.io.println("Nothing to restock.")
        }

    }

}
