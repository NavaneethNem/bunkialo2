package com.codialo.bunkialo.schedule

import java.time.DayOfWeek
import java.time.LocalDateTime
import kotlin.math.roundToInt

enum class TimetableDay(
    val dayOfWeek: DayOfWeek,
    val shortName: String,
) {
    MONDAY(DayOfWeek.MONDAY, "MON"),
    TUESDAY(DayOfWeek.TUESDAY, "TUE"),
    WEDNESDAY(DayOfWeek.WEDNESDAY, "WED"),
    THURSDAY(DayOfWeek.THURSDAY, "THU"),
    FRIDAY(DayOfWeek.FRIDAY, "FRI"),
    SATURDAY(DayOfWeek.SATURDAY, "SAT"),
    SUNDAY(DayOfWeek.SUNDAY, "SUN"),
}

enum class Course(
    val label: String,
    val shortLabel: String,
    val faculty: String,
    val pastel: Pastel,
) {
    DAA(
        "Design and Analysis of Algorithms",
        "DAA",
        "Priyadharshini",
        Pastel.ROSE
    ),

    TOC(
        "Theory of Computation",
        "TOC",
        "Divya",
        Pastel.LAVENDER
    ),

    IT_WORKSHOP(
        "IT Workshop III",
        "IT",
        "Deepak",
        Pastel.MINT
    ),

    PROBABILITY(
        "Probability, Statistics and Random Processes",
        "PSRP",
        "Anandhu",
        Pastel.BLUE
    ),

    DBMS(
        "Database Management Systems",
        "DBMS",
        "Venkatesh",
        Pastel.PEACH
    ),

    DSA_II(
        "Data Structures II",
        "DSA II",
        "Sara",
        Pastel.YELLOW
    ),

    COGNITIVE_SCIENCE(
        "Introduction to Cognitive Science",
        "CogSci",
        "Gayathri",
        Pastel.LILAC
    ),
}

data class TimetableCourse(
    val id: String,
    val label: String,
    val faculty: String,
    val pastel: Pastel,
)

enum class Pastel {
    ROSE,
    LAVENDER,
    MINT,
    BLUE,
    PEACH,
    YELLOW,
    LILAC,
    AQUA,
}

data class TimetableEvent(
    val course: TimetableCourse,
    val startMinutes: Int,
    val endMinutes: Int,
    val isLab: Boolean = false,
) {
    // Full subject name for the normal timetable UI
    val title: String
        get() = course.label

    // Abbreviated name for the complication
    val shortTitle: String
        get() = course.abbreviatedName()

    val time: String
        get() = "${formatTime(startMinutes)}–${formatTime(endMinutes)}"

    fun isHappeningAt(minutes: Int): Boolean =
        minutes in startMinutes until endMinutes

    fun isHappeningAt(now: LocalDateTime): Boolean =
        isHappeningAt(now.hour * 60 + now.minute)
}

fun TimetableCourse.abbreviatedName(): String {
    // 1. Direct match by id in Course enum
    Course.entries.firstOrNull { it.name.equals(id, ignoreCase = true) }?.let { return it.shortLabel }

    // 2. Direct match by label in Course enum
    Course.entries.firstOrNull { it.label.equals(label, ignoreCase = true) }?.let { return it.shortLabel }

    // 3. Match by shortLabel in Course enum
    Course.entries.firstOrNull {
        it.shortLabel.equals(id, ignoreCase = true) || it.shortLabel.equals(label, ignoreCase = true)
    }?.let { return it.shortLabel }

    // 4. Extract acronym or abbreviation from parentheses, e.g. "Database Management Systems (DBMS)" -> "DBMS"
    val parenMatch = Regex("\\(([A-Za-z0-9\\s]{2,7})\\)").find(label)
    if (parenMatch != null) {
        return parenMatch.groupValues[1].trim()
    }

    // 5. Course code prefix like "CSE311 - Database Management Systems" or "CSE311: Database Management Systems"
    val codePrefixRegex = Regex("^([A-Za-z]{2,}\\s*\\d{2,})\\s*(?:[-:]\\s*)?")
    val codeMatch = codePrefixRegex.find(label.trim())
    if (codeMatch != null) {
        val courseCode = codeMatch.groupValues[1].replace("\\s+".toRegex(), "")
        val restOfName = label.trim().substring(codeMatch.range.last + 1).trim()
        Course.entries.firstOrNull { it.label.equals(restOfName, ignoreCase = true) }?.let { return it.shortLabel }
        if (courseCode.length in 3..7) {
            return courseCode
        }
    }

    // 6. If already short (<= 6 chars), use label
    if (label.length <= 6) return label

    // 7. Acronym from words (e.g., "Operating Systems" -> "OS", "Software Engineering" -> "SE")
    val stopWords = setOf("and", "of", "to", "in", "for", "the", "on", "with", "a", "an")
    val words = label.split("\\s+".toRegex())
        .map { it.trim().trim('(', ')', '-', ':', ',') }
        .filter { it.isNotBlank() && it.lowercase() !in stopWords }

    if (words.size in 2..5) {
        val acronym = words.mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
        if (acronym.length in 2..6) return acronym
    }

    return label.take(6)
}

fun breakHoursBetween(
    previous: TimetableEvent,
    next: TimetableEvent
): Int? {
    val gapMinutes = next.startMinutes - previous.endMinutes
    val roundedHours = (gapMinutes / 60.0).roundToInt()

    return roundedHours.takeIf {
        gapMinutes >= 30 && it > 0
    }
}

fun breakLabelBetween(
    previous: TimetableEvent,
    next: TimetableEvent
): String? {
    val hours = breakHoursBetween(previous, next) ?: return null

    val isLunchBreak =
        previous.endMinutes == 13 * 60 + 25 &&
        next.startMinutes == 14 * 60 + 30

    return if (isLunchBreak) {
        "$hours hr lunch break"
    } else {
        "$hours hr break"
    }
}

data class ScheduledEvent(
    val day: TimetableDay,
    val event: TimetableEvent,
)

private fun time(hour: Int, minute: Int): Int =
    hour * 60 + minute

private fun formatTime(minutes: Int): String {
    val hour = minutes / 60
    val minute = minutes % 60

    val hour12 = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }

    return "%d:%02d".format(hour12, minute)
}

private fun event(
    course: Course,
    startHour: Int,
    startMinute: Int,
    endHour: Int,
    endMinute: Int,
    isLab: Boolean = false,
): TimetableEvent = TimetableEvent(
    course = course.toTimetableCourse(),
    startMinutes = time(startHour, startMinute),
    endMinutes = time(endHour, endMinute),
    isLab = isLab,
)

private fun Course.toTimetableCourse(): TimetableCourse =
    TimetableCourse(
        id = name,
        label = label,
        faculty = faculty,
        pastel = pastel,
    )

val templateTimetable: Map<TimetableDay, List<TimetableEvent>> = mapOf(

    TimetableDay.MONDAY to listOf(
        event(
            Course.DAA,
            9, 0,
            10, 0
        ),

        event(
            Course.DAA,
            12, 0,
            13, 0
        ),

        event(
            Course.TOC,
            14, 0,
            15, 0
        ),

        event(
            Course.TOC,
            16, 0,
            17, 0
        ),
    ),

    TimetableDay.TUESDAY to listOf(
        event(
            Course.TOC,
            9, 0,
            10, 0
        ),

        event(
            Course.IT_WORKSHOP,
            10, 0,
            11, 0
        ),

        event(
            Course.DAA,
            11, 0,
            12, 0
        ),

        event(
            Course.TOC,
            12, 3,
            13, 0
        ),

        event(
            Course.DAA,
            14, 0,
            15, 0
        ),

        event(
            Course.DAA,
            16, 0,
            17, 0
        ),
    ),

    TimetableDay.WEDNESDAY to listOf(
        event(
            Course.IT_WORKSHOP,
            9, 0,
            11, 0,
            isLab = true
        ),

        event(
            Course.PROBABILITY,
            11, 0,
            11, 55
        ),

        event(
            Course.DBMS,
            12, 0,
            13, 0
        ),

        event(
            Course.DSA_II,
            14, 0,
            16, 0,
            isLab = true
        ),

        event(
            Course.COGNITIVE_SCIENCE,
            16, 0,
            17, 0
        ),
    ),

    TimetableDay.THURSDAY to listOf(
        event(
            Course.DBMS,
            9, 0,
            10, 0
        ),

        event(
            Course.PROBABILITY,
            10, 0,
            11, 0
        ),

        event(
            Course.DBMS,
            11, 0,
            13, 0,
            isLab = true
        ),

        event(
            Course.PROBABILITY,
            14, 0,
            14, 55
        ),

        event(
            Course.IT_WORKSHOP,
            15, 0,
            16, 0
        ),

        event(
            Course.COGNITIVE_SCIENCE,
            16, 0,
            16, 59
        ),
    ),

    TimetableDay.FRIDAY to listOf(
        event(
            Course.IT_WORKSHOP,
            9, 0,
            10, 0
        ),

        event(
            Course.PROBABILITY,
            10, 0,
            11, 0
        ),

        event(
            Course.DBMS,
            11, 0,
            12, 0
        ),

        event(
            Course.DSA_II,
            12, 0,
            13, 0
        ),
    ),
)

val weeklyTimetable: Map<TimetableDay, List<TimetableEvent>> =
    templateTimetable

fun TimetableDay.events(
    timetable: Map<TimetableDay, List<TimetableEvent>> = weeklyTimetable,
): List<TimetableEvent> =
    timetable[this].orEmpty()

fun focusDay(
    now: LocalDateTime,
    timetable: Map<TimetableDay, List<TimetableEvent>> = weeklyTimetable,
): TimetableDay {

    val todayIndex =
        TimetableDay.entries.indexOfFirst {
            it.dayOfWeek == now.dayOfWeek
        }

    if (todayIndex == -1) {
        return TimetableDay.MONDAY
    }

    val today =
        TimetableDay.entries[todayIndex]

    val nowMinutes =
        time(now.hour, now.minute)

    val hasMoreToday =
        today.events(timetable).any {
            it.isHappeningAt(nowMinutes) ||
                it.startMinutes > nowMinutes
        }

    val isWeekend =
        today.dayOfWeek == DayOfWeek.SATURDAY ||
        today.dayOfWeek == DayOfWeek.SUNDAY

    if (isWeekend || hasMoreToday) {
        return today
    }

    for (offset in 1..TimetableDay.entries.size) {
        val day =
            TimetableDay.entries[
                (todayIndex + offset) %
                    TimetableDay.entries.size
            ]

        if (day.events(timetable).isNotEmpty()) {
            return day
        }
    }

    return TimetableDay.MONDAY
}

fun currentOrNextEvent(
    now: LocalDateTime,
    timetable: Map<TimetableDay, List<TimetableEvent>> = weeklyTimetable,
): ScheduledEvent? {

    val todayIndex =
        TimetableDay.entries.indexOfFirst {
            it.dayOfWeek == now.dayOfWeek
        }

    if (todayIndex == -1) {
        return TimetableDay.MONDAY
            .events(timetable)
            .firstOrNull()
            ?.let {
                ScheduledEvent(
                    TimetableDay.MONDAY,
                    it
                )
            }
    }

    val today =
        TimetableDay.entries[todayIndex]

    val nowMinutes =
        time(now.hour, now.minute)

    today.events(timetable)
        .firstOrNull {
            it.isHappeningAt(nowMinutes)
        }
        ?.let {
            return ScheduledEvent(today, it)
        }

    today.events(timetable)
        .firstOrNull {
            it.startMinutes > nowMinutes
        }
        ?.let {
            return ScheduledEvent(today, it)
        }

    for (offset in 1..TimetableDay.entries.size) {
        val day =
            TimetableDay.entries[
                (today.ordinal + offset) %
                    TimetableDay.entries.size
            ]

        day.events(timetable)
            .firstOrNull()
            ?.let {
                return ScheduledEvent(day, it)
            }
    }

    return null
}

fun complicationEvent(
    now: LocalDateTime,
    timetable: Map<TimetableDay, List<TimetableEvent>> = weeklyTimetable,
): ScheduledEvent? {

    val todayIndex =
        TimetableDay.entries.indexOfFirst {
            it.dayOfWeek == now.dayOfWeek
        }

    if (todayIndex == -1) {
        return currentOrNextEvent(
            now,
            timetable
        )
    }

    val today =
        TimetableDay.entries[todayIndex]

    val currentIndex =
        today.events(timetable)
            .indexOfFirst {
                it.isHappeningAt(now)
            }

    if (currentIndex == -1) {
        return currentOrNextEvent(
            now,
            timetable
        )
    }

    val currentEvent =
        today.events(timetable)[currentIndex]

    val nowSeconds =
        now.hour * 3_600 +
        now.minute * 60 +
        now.second

    val midpointSeconds =
        (currentEvent.startMinutes +
            currentEvent.endMinutes) * 30

    if (nowSeconds < midpointSeconds) {
        return ScheduledEvent(
            today,
            currentEvent
        )
    }

    today.events(timetable)
        .getOrNull(currentIndex + 1)
        ?.let {
            return ScheduledEvent(
                today,
                it
            )
        }

    for (offset in 1..TimetableDay.entries.size) {
        val day =
            TimetableDay.entries[
                (todayIndex + offset) %
                    TimetableDay.entries.size
            ]

        day.events(timetable)
            .firstOrNull()
            ?.let {
                return ScheduledEvent(
                    day,
                    it
                )
            }
    }

    return null
}