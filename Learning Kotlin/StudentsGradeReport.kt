fun main(){
    //1
    val user1=Std("Nicat", kotlin.intArrayOf(91, 35, 78, 90, 87))
    val user2=Std("Sevil", kotlin.intArrayOf(86, 78, 45, 67, 89))
    val user3=Std("Leyla", kotlin.intArrayOf(56, 68, 42, 57, 32))
    val user4=Std("Samir", kotlin.intArrayOf(90, 90, 90, 95, 100))
    val user5=Std("Nigar", kotlin.intArrayOf(86, 91, 78, 87, 89))

    var studentsArray= kotlin.arrayOf(user1, user2, user3, user4, user5)

    kotlin.io.println("--- Students ---")
    //2
    printReport(studentsArray)
    kotlin.io.println("...")

    //3
    val countOfPassed=countStudents(studentsArray){
        it.isPassed(it.average(it.scores),it.scores)
    }
    kotlin.io.println("Passed : $countOfPassed")

    val countOfA=countStudents(studentsArray){
        it.letterGrade(it.average(it.scores))=="A"
    }
    kotlin.io.println("Grade A : $countOfA")

    val countOf100=countStudents(studentsArray){
        it.highestScore(it.scores)==100
    }
    kotlin.io.println("How many have at least one score of 100 : $countOf100 ")

    //4
    val bestStudent=findBest(studentsArray)
    if(bestStudent==null){
        kotlin.io.println("No students.")
    }else{
        kotlin.io.println("Best student: ${bestStudent.name}")
    }

    //5
    val namesOfFailed=studentsArray.filter {
        it.isPassed(it.average(it.scores),it.scores) == false
    }
        .map {  //?
            it.name
        }
    kotlin.io.println("Failed : ${namesOfFailed.joinToString(", ")}")

    val ranking=studentsArray.sortedByDescending {
        it.average(it.scores)
    }
        .map {  //?
            it.name
        }
    kotlin.io.println("Ranking: ${ranking.joinToString(", ")}")
    kotlin.io.println()

    for (student in studentsArray){
        student.applyBonus{it+5}
    }

    printReport(studentsArray)

}


fun printReport(students: Array<Std>) {
    for (student in students){
        kotlin.io.print(" ${student.name}   | ")

        val avg= kotlin.String.Companion.format("%.1f",  student.average(student.scores)  )
        kotlin.io.print("avg: $avg   | ")

        kotlin.io.print("max: ${student.highestScore(student.scores)}   |")
        kotlin.io.print("grade: ${student.letterGrade(student.average(student.scores))}   |")

        if (student.isPassed(
                student.average(student.scores),
                student.scores)
            ){
            kotlin.io.println(" PASSED ")
        }else{
            kotlin.io.println(" FAILED ")
        }
    }
}

fun countStudents(students: Array<Std>,condition: (Std) -> Boolean): Int{
    var count = 0
    for (student in students){
        if (condition(student)){
            count++
        }
    }
    return count
}

fun findBest(students: Array<Std>): Std?{
    if (students.isEmpty()) {
        return null
    }

    var bestStd = students[0]
    var maxAvg=bestStd.average(bestStd.scores)

    for (student in students){
        val currentAvg=student.average(student.scores)

        if (currentAvg>maxAvg){
            maxAvg=currentAvg
            bestStd = student
        }
    }
    return bestStd
}

class Std(val name: String, val scores: IntArray){
    fun average(scoresArray: IntArray) : Double{
        var sumOfScores=0
        for (i in scoresArray){
            sumOfScores += i
        }
        return sumOfScores.toDouble() / scoresArray.size
    }

    fun highestScore(scoreArray: IntArray) : Int{
        var max=scoreArray[0]
        for(i in scoreArray){
            if(i>max){
                max=i
            }
        }
        return max
    }

    fun letterGrade(average: Double): String{
        return when (average.toInt()){
            in 90..100->  "A"
            in 80..89->  "B"
            in 70..79->  "C"
            in 60..69->  "D"
            in 0 until 60->  "F"
            else -> "Wrong information"
        }
    }

    fun isPassed(averageOfScores:Double,scoreArray: IntArray) : Boolean{
        if (averageOfScores>=60){
            var n=0
            for (i in scoreArray){
                if (i>40){
                    n++
                }
            }
            if (n==scoreArray.size){
                return true
            }
        }
        return false
    }

    fun applyBonus(transform: (Int) -> Int){
        for (i in 0..scores.size-1 ){
            var newScore=transform(scores[i])
            if (newScore >100){
                newScore=100
            }
            scores[i]=newScore
        }
    }
}
//