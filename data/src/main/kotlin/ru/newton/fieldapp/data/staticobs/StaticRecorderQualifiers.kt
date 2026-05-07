package ru.newton.fieldapp.data.staticobs

import javax.inject.Qualifier

/** Hilt qualifier for the directory the static recorder writes into. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class StaticRecorderRoot

/** Hilt qualifier for the long-lived scope the recorder uses for its writer job. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class StaticRecorderScope
