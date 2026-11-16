import FilmContent

enum class Genre(val displayName:String){
    ACTION("Action"),
    ADVENTURE("Adventure"),
    ANIMATION("Animation"),
    COMEDY("Comedy"),
    HORROR("Horror"),
    DOCUMENTARY("Documentary"),
    DRAMA("Drama");
}

enum class SubscriptionType(val price: Double,val maxQuality:Int){
    FREE(0.0,480),
    BASIC(4.99,720),
    PREMIUM(9.99,1080);
}

data class Movie(
    val id: String,
    val title: String,
    val genre: Genre,
    val durationMinutes: Int,
    val rating: Double,
    val minSubscriptionType: SubscriptionType
)

data class Series(
    val id: String,
    val title: String,
    val genre: Genre,
    val durationMinutes: Int,
    val seasons: Int,
    val totalEpisodes: Int,
    val rating: Double,
    val minSubscriptionType: SubscriptionType
)

//?   dinamikdir.
sealed class PlaybackResult{
    data class Success(val data:Movie ,val quality:Int): PlaybackResult()
    data class Error(val message:String ): PlaybackResult()
    data class AccessDenied(val requiredPlan:SubscriptionType): PlaybackResult()
    object Loading: PlaybackResult() //singleton
}

interface Playable{
    fun play():String

    fun stop(){ //default implementation-override edilmir
        println("Playable stopped.")
        return
    }
}
interface Rateable{
    fun rate(score: Double)
}

interface Downloadable{
    fun download():String
}

abstract class Content(val title : String ,val durationMinutes: Int){
    abstract fun getInfo() : String
    fun formattedDuration() : String {
        val hours = durationMinutes / 60
        val minutes = durationMinutes % 60
        return "${hours}h ${minutes}m"
    }
}

class FilmContent(val movie: Movie):
    Content(movie.title, durationMinutes = movie.durationMinutes),
    Playable ,
    Rateable {
    override fun getInfo(): String {
        return "Movie: ${movie.title} \nGenre:${movie.genre.displayName} \nRating: ${movie.rating}% \nDuration: ${formattedDuration()} "
    }
    override fun play(): String {
        return " Playing Movie is ${title}"
    }

    override fun rate(score: Double) { //?
        println("Rated '$title' with score: $score")
    }
}

class SeriesContent(val series: Series):
    Content(series.title, durationMinutes = series.durationMinutes),
    Playable{
    override fun getInfo(): String {
        return "Series: ${series.title} \nGenre:${series.genre.displayName} \nSeasons: ${series.seasons} \nTotal Episodes: ${series.totalEpisodes} \nDurations: ${formattedDuration()} \nRating: ${series.rating}%"
    }
    override fun play(): String {
        return " Playing Series: $title"
    }
}

class MovieRepository {
    private val movies = mutableListOf<Movie>()
    private val seriesList = mutableListOf<Series>()

    init {
        movies.add(Movie("001m","Inception",Genre.ACTION,148,8.8,SubscriptionType.PREMIUM))
        movies.add(Movie("002m", "Titanic", Genre.DRAMA, 195, 8.9, SubscriptionType.BASIC))
        movies.add(Movie("003m", "Toy Story", Genre.ANIMATION, 81, 8.3, SubscriptionType.FREE))

        seriesList.add(Series("001s","Breaking Bad",Genre.DRAMA,49,5,62,9.5, SubscriptionType.BASIC))
        seriesList.add(Series("002s","Stranger Things",Genre.DRAMA,60,4,34,8.7,SubscriptionType.BASIC))
        seriesList.add(Series("003s","Game of Trones",Genre.ACTION,80,8,73,9.2,SubscriptionType.PREMIUM))
    }

    fun getAllContent(): List<Content> {
        val films = movies.map { FilmContent(it) }
        val series = seriesList.map { SeriesContent(it) }
        return films + series
    }

    fun addMovie(movie: Movie) {
        movies.add(movie)
    }
}

fun main() {
    val repository = MovieRepository()

    for (content in repository.getAllContent()) {
        println(content.getInfo())
        println("-------------------")
    }

    // sonradan əlavə etmək:
    repository.addMovie(Movie("004m", "Superbad", Genre.COMEDY, 113, 7.6, SubscriptionType.FREE))
}




