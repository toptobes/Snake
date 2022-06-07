@file:OptIn(ExperimentalComposeUiApi::class)
@file:Suppress("ControlFlowWithEmptyBody")

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement.Absolute.SpaceBetween
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.delay
import kotlin.random.Random

typealias Snake = SnapshotStateList<Point>

lateinit var Window: ComposeWindow
    private set

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        undecorated = true,
        transparent = true,
        resizable = false,
        alwaysOnTop = true,
        title = "Snake",
        onKeyEvent = {
            if (it.key == Key.Q) {
                exitApplication()
            }
            false
        }
    ) {
        Window = window
        WindowDraggableArea {
            Game()
        }
    }
}

const val ROWS = 30
const val COLUMNS = 40

data class Point(val x: Int, val y: Int)
private infix fun Int.to(other: Int) = Point(this, other)

val UP = Key.K
val DOWN = Key.J
val RIGHT = Key.L
val LEFT = Key.H

private var moveSnake by mutableStateOf(true)
private var direction by mutableStateOf(RIGHT)
private var boost by mutableStateOf(false)

private var gameOver by mutableStateOf(false)
private var reset by mutableStateOf(false)

private var highScore by mutableStateOf(0)
private var score by mutableStateOf(0)

@Composable
fun Game() {

    if (reset) {
        reset()
        return
    }

    val requester = remember { FocusRequester() }

    val bgColor = if (gameOver) Color(.3f, .2f, .2f) else Color(.2f, .2f, .2f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .padding(10.dp)
            .onKeyEvent {

                //Yeah, I know this is buggy, but I'm too lazy to fix it now
                if (it.key == UP && direction != DOWN) direction = UP
                if (it.key == DOWN && direction != UP) direction = DOWN
                if (it.key == LEFT && direction != RIGHT) direction = LEFT
                if (it.key == RIGHT && direction != LEFT) direction = RIGHT

                if (it.key == Key.Spacebar) {
                    boost = it.type == KeyEventType.KeyDown
                }

                if (it.key == Key.S) {
                    reset = true
                }
                false
            }
            .focusRequester(requester)
            .focusable()
    ) {
        startClock()

        val snake = remember { mutableStateListOf(1 to 1, 2 to 1, 3 to 1, 4 to 1) }
        DrawSnake(snake)

        var food by remember { mutableStateOf(randomPoint(snake)) }
        DrawFood(food)

        DrawKeys()
        DrawScore()
        DrawHighScore()
        DrawResetText()

        if (moveSnake) {
            when (direction) {
                UP -> snake.add(snake.last().x to snake.last().y - 1)
                DOWN -> snake.add(snake.last().x to snake.last().y + 1)
                LEFT -> snake.add(snake.last().x - 1 to snake.last().y)
                RIGHT -> snake.add(snake.last().x + 1 to snake.last().y)
            }

            if (snake.last() == food) {
                food = randomPoint(snake)
            } else {
                snake.removeAt(0)
            }

            moveSnake = false
        }

        if (collision(snake)) {
            gameOver = true
        }
    }

    LaunchedEffect(Unit) {
        requester.requestFocus()
    }
}

@Composable
fun startClock() = LaunchedEffect(Unit) {
    while (true) {
        delay(if (boost) 100L else 200L)
        moveSnake = true

        if (gameOver) {
            break
        }
    }
}

@Composable
fun DrawSnake(snake: Snake) {
    DrawHeadingLine(snake)

    snake.forEach { body ->
        Box(
            modifier = Modifier.size(cellSize.dp)
                .absoluteOffset(x = body.x.cs.dp, y = body.y.cs.dp)
                .clip(CircleShape)
                .background(Color.Green)
        )
    }
}

@Composable
fun DrawFood(food: Point) = Box(
    modifier = Modifier.size(cellSize.dp)
        .absoluteOffset(x = food.x.cs.dp, y = food.y.cs.dp)
        .clip(CircleShape)
        .background(Color.Red)
)

@Composable
fun DrawHeadingLine(snake: Snake) {
    val xcs = snake.last().x.cs.dp
    val ycs = snake.last().y.cs.dp
    val hcs = (cellSize / 2).dp

    when (direction) {
        UP -> Box(Modifier.offset(xcs + hcs).background(Color.DarkGray).width(1.dp).height(ycs))
        DOWN -> Box(Modifier.offset(xcs + hcs, ycs + 2 * hcs).background(Color.DarkGray).width(1.dp).fillMaxHeight())
        LEFT -> Box(Modifier.offset(-hcs, ycs + hcs).background(Color.DarkGray).height(1.dp).width(xcs))
        RIGHT -> Box(Modifier.offset(xcs + hcs, ycs + hcs).background(Color.DarkGray).height(1.dp).fillMaxWidth())
    }
}

@Composable
fun BoxScope.DrawKeys() = Column(
    modifier = Modifier
        .size(135.dp, 30.dp)
        .align(Alignment.BottomStart),
) {
    Row(
        modifier = Modifier.size(135.dp, 29.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = SpaceBetween
    ) {
        Text("H←", color = Color.White.copy(if (direction == LEFT) .8f else .4f))
        Text("J↓", color = Color.White.copy(if (direction == DOWN) .8f else .4f))
        Text("K↑", color = Color.White.copy(if (direction == UP) .8f else .4f))
        Text("L→", color = Color.White.copy(if (direction == RIGHT) .8f else .4f))
    }
    if (boost) {
        Box(
            Modifier.size(135.dp, 1.dp).background(Color.White.copy(.8f))
        )
    }
}

@Composable
fun BoxScope.DrawScore() = Text(
    score.toString(), color = Color.White.copy(.8f), modifier = Modifier.align(Alignment.TopStart)
)

@Composable
fun BoxScope.DrawHighScore() = Text(
    highScore.toString(), color = Color.White.copy(.8f), modifier = Modifier.align(Alignment.TopEnd)
)

@Composable
fun BoxScope.DrawResetText() = if (gameOver) {
    Text(
        "Press 'S' to reset", color = Color.White.copy(.8f), modifier = Modifier.align(Alignment.TopCenter)
    )
} else { }

fun collision(snake: Snake): Boolean {
    if (snake.any { it.x > COLUMNS || it.y > ROWS || it.x < 0 || it.y < 0 }) {
        return true
    }
    if (snake.size != snake.distinct().size) {
        return true
    }
    return false
}

fun randomPoint(snake: Snake): Point {
    lateinit var point: Point

    do {
        point = Random.nextInt(0, COLUMNS) to Random.nextInt(0, ROWS)
    } while (point in snake)

    return point
}

fun reset() {
    reset = false
    gameOver = false
    direction = RIGHT
    score = 0
}

private val cellSize
    get() = (Window.height - 20) / ROWS

private val Int.cs
    get() = this * cellSize
