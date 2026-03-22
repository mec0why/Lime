package mec0why.lime

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Search : Screen("search")
    data object Following : Screen("following")
    data object Channel : Screen("channel/{slug}") {
        fun createRoute(slug: String) = "channel/$slug"
    }
}
