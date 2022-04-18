package com.demich.cps.contests

class Contest {

    enum class Platform {
        unknown,
        codeforces,
        atcoder,
        topcoder,
        codechef,
        google,
        dmoj
        ;

        /*fun getIcon(): Int {
            return when(this) {
                codeforces -> R.drawable.ic_logo_codeforces
                atcoder -> R.drawable.ic_logo_atcoder
                topcoder -> R.drawable.ic_logo_topcoder
                codechef -> R.drawable.ic_logo_codechef
                google -> R.drawable.ic_logo_google
                dmoj -> R.drawable.ic_logo_dmoj
                else -> R.drawable.ic_cup
            }
        }*/

        companion object {
            fun getAll(): List<Platform> = values().filter { it != unknown }
        }
    }

}