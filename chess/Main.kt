package chess

import kotlin.math.abs

val VALID_MOVES = Regex("([a-h][1-8]){2}")

class Pawn(val player: Player) {
    var hasMoved = false
        private set

    fun move() {
        hasMoved = true
    }
}

class Player(val name: String, val properties: Properties) {
    var captures: Int = 0
    enum class Properties(val color: String, val dir: Int, val winRank: Int) {
        WHITE("white", 1, 7),
        BLACK("black", -1, 0)
    }
}

class Board(players: List<Player>) {
    private var enPassantCell: Cell? = null
    var curPlayer: Player? = null

    private val matrix: MutableList<MutableList<Pawn?>> = MutableList(8) { MutableList(8){ null } }

    data class Cell(val rank: Int, val file: Int) {
        companion object{
            fun fromCode(cellCode: String) =
                    Cell(cellCode[1].toString().toInt()-1, cellCode[0].code - 'a'.code)
        }
        fun toCode() = "${'a'+file}${rank+1}"
    }

    class Move(val from: Cell, val to: Cell, val dir: Int){
        val moveType: Type?
        enum class Type {
            SINGLE,
            DOUBLE,
            CAPTURE
        }
        companion object {
            fun fromCode(moveCode: String, dir: Int) =
                    Move(
                            Cell.fromCode(moveCode.substring(0..1)),
                            Cell.fromCode(moveCode.substring(2)),
                            dir
                    )
        }
        init {
            val ranksMoved = (to.rank - from.rank ) * dir
            moveType = when {
                ranksMoved == 1 &&
                        abs(to.file - from.file) == 1 -> Type.CAPTURE
                from.file == to.file &&
                        ranksMoved == 1 -> Type.SINGLE
                from.file == to.file &&
                        ranksMoved == 2 -> Type.DOUBLE
                else -> null
            }
        }
    }

    init {
        repeat(8) {
            setPawnAt(Cell(1, it), Pawn(players[0]))
        }
        repeat(8) {
            setPawnAt(Cell(6, it), Pawn(players[1]))
        }
    }

    fun movePiece(moveCode: String): Boolean{
        if (!moveCode.matches(VALID_MOVES)){
            println("Invalid Input")
            return false
        }
        val move = Move.fromCode(moveCode, curPlayer!!.properties.dir)
        if (move.moveType == Move.Type.CAPTURE) {
            if (pawnAt(move.to) != null && pawnAt(move.to)!!.player != curPlayer)
                capture(move, false)
            else if (move.to == enPassantCell) capture(move, true)
            else {
                println("Invalid Input")
                return false
            }
        }
        else if (isValidMove(move)) {
            val pawn = pawnAt(move.from)!!
            pawn.move()
            setPawnAt(move.to, pawn)
            setPawnAt(move.from, null)
            if (move.moveType == Move.Type.DOUBLE)
                enPassantCell = Cell(move.to.rank - 1*curPlayer!!.properties.dir, move.to.file)
        }
        else return false
        return true

    }

    fun checkForWin(move: Move): Boolean {
        return move.to.rank == curPlayer!!.properties.winRank || curPlayer!!.captures == 8
    }

    private fun capture(move: Move, isEnPassant: Boolean = false){
        if (isEnPassant){
            val captureCell = Cell(enPassantCell!!.rank-1*curPlayer!!.properties.dir, enPassantCell!!.file)
            setPawnAt(captureCell, null)
        }
        val pawn = pawnAt(move.from)!!
        pawn.move()
        setPawnAt(move.to, pawn)
        setPawnAt(move.from, null)

        curPlayer!!.captures += 1
    }

    private fun isValidMove(move: Move): Boolean{
        val pawn = pawnAt(move.from)

        if (pawn?.player != curPlayer){
            println("No ${curPlayer!!.properties.color} pawn at ${move.from.toCode()}")
            return false
        }

        val isValid = when (move.moveType){
            Move.Type.DOUBLE -> !pawn!!.hasMoved && pawnAt(move.to) == null
            Move.Type.SINGLE -> pawnAt(move.to) == null
            else -> false
        }

        if (!isValid) println("Invalid Input")

        return isValid

    }

    private fun setPawnAt(cell: Cell, pawn: Pawn?){
        matrix[cell.rank][cell.file] = pawn
    }

    private fun pawnAt(cell: Cell): Pawn?{
        return matrix[cell.rank][cell.file]
    }

    fun print(){
        val hBorder = "  ${"+---".repeat(8)}+"
        println(hBorder)
        for (i in 7 downTo 0){
            print("${i+1} ")
            for (pawn in matrix[i]){
                val piece = pawn?.player?.properties?.color?.first()?.uppercaseChar() ?: ' '
                print("| $piece ")
            }
            println("|")
            println(hBorder)
        }
        println("    ${(MutableList(8) {'a'+it}).joinToString("   ")}  ")
    }

    fun checkForStalemate(): Boolean {
        for (rank in matrix.indices){
            for (file in matrix[rank].indices){
                if (pawnAt(Cell(rank, file))?.player != curPlayer) continue
                val fRank = rank+1*curPlayer!!.properties.dir
                if (fRank !in 0..7) continue
                if (pawnAt(Cell(fRank, file)) == null ) return false
                var cell = Cell(fRank, file-1)
                if (file > 0 && (pawnAt(cell)?.let { it.player != curPlayer } == true || cell == enPassantCell))
                    return  false
                cell = Cell(fRank, file+1)
                if (file < 7 && (pawnAt(cell)?.let { it.player != curPlayer } == true || cell == enPassantCell))
                    return  false
            }
        }
        return true
    }
}

class Game(private val players: List<Player>) {
    private val board = Board(players)

    fun play(){
        var turn = 0
        while (true) {
            board.curPlayer = players[turn]
            if (board.checkForStalemate()) {
                println("Stalemate!")
                break
            }
            println("${players[turn].name}'s turn:")
            val inp = readInput("> ")
            if (inp.lowercase() == "exit") break
            if (board.movePiece(inp)) {
                board.print()
                if (board.checkForWin(Board.Move.fromCode(inp, players[turn].properties.dir))) {
                    println("${players[turn].properties.color.replaceFirstChar { it.uppercase() }} Wins!")
                    break
                } else turn = turn xor 1
            }
        }
        println("Bye")
    }

    fun printBoard() {
        board.print()
    }

}

fun main(){
    println("Pawns-Only Chess")
    val game = Game(initializePlayers())
    game.printBoard()
    game.play()
}

fun readInput(prompt: String): String{
    print(prompt)
    return readLine()!!

}

fun initializePlayers(): List<Player> {
    return List(2){
        val name: String
        val properties: Player.Properties
        if (it == 0) {
            name = readInput("First Player's name: ")
            properties = Player.Properties.WHITE
        }else {
            name = readInput("Second Player's name: ")
            properties = Player.Properties.BLACK
        }
        Player(name, properties)
    }
}
