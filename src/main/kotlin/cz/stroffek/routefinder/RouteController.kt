package cz.stroffek.routefinder

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.net.URL
import javax.servlet.http.HttpServletResponse

@RestController
class RouteController {
    private var borders: MutableMap<String, List<String>>

    init {
        val countries: JSONArray = JSONTokener(
            URL("https://raw.githubusercontent.com/mledoze/countries/master/countries.json")
                .openStream()
        ).nextValue() as JSONArray
        borders = mutableMapOf()
        countries.forEach { cit ->
            if (cit is JSONObject) {
                val cca3 = cit.get("cca3") as String
                val cBorders = cit.get("borders") as JSONArray
                borders[cca3] = cBorders.filterIsInstance<String>()
            }
        }
    }

    @GetMapping("/routing/hello")
    fun hello() = "HELLO!"

    @GetMapping("/routing/{origin}/{destination}")
    fun shortestRoute(
        response : HttpServletResponse,
        @PathVariable("origin") origin : String,
        @PathVariable("destination") destination : String
    ) {
        // We will use Dijkstra's algorithm to find the shortest path
        val visitedCountries = mutableMapOf<String, String>()
        val countryQueue = mutableListOf<String>()
        countryQueue.add(origin)

        // Go over the country borders graph until either there are no more countries
        // to visit or we have reached the destination country
        while (countryQueue.isNotEmpty() && !visitedCountries.containsKey(destination)) {
            val cc = countryQueue.removeFirst()
            borders[cc]?.forEach {
                if (!visitedCountries.containsKey(it)) {
                    visitedCountries[it] = cc
                    countryQueue.add(it)
                }
            }
        }

        // Now reconstruct the path from the destination to origin from visited map
        if (visitedCountries.containsKey(destination)) {
            val route = mutableListOf<String>()
            var cc = destination
            route.add(0, cc)
            while (cc != origin) {
                cc = visitedCountries[cc]!!
                route.add(0, cc)
            }

            response.contentType = "application/json"
            val result = JSONObject()
            result.put("route", route)
            response.writer.print(result.toString())
        } else {
            // The route does not exist
            response.status = 400
        }
    }
}