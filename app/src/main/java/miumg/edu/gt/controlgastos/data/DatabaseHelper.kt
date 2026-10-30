package miumg.edu.gt.controlgastos.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import miumg.edu.gt.controlgastos.model.Categoria
import miumg.edu.gt.controlgastos.model.Movimiento
import miumg.edu.gt.controlgastos.model.TipoMovimiento

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "control_gastos.db"
        private const val DB_VERSION = 2

        private const val TABLA = "movimientos"
        private const val COL_ID = "id"
        private const val COL_TIPO = "tipo"
        private const val COL_MONTO = "monto"
        private const val COL_CATEGORIA = "categoria"
        private const val COL_DESCRIPCION = "descripcion"
        private const val COL_FECHA = "fecha"

        private const val TABLA_CAT = "categorias"
        private const val COL_CAT_ID = "id"
        private const val COL_CAT_NOMBRE = "nombre"
        private const val COL_CAT_TIPO = "tipo"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLA (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TIPO TEXT NOT NULL,
                $COL_MONTO REAL NOT NULL,
                $COL_CATEGORIA TEXT NOT NULL,
                $COL_DESCRIPCION TEXT,
                $COL_FECHA INTEGER NOT NULL
            )
            """.trimIndent()
        )
        crearTablaCategorias(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // No se borra nada: solo se agrega lo nuevo
        if (oldVersion < 2) crearTablaCategorias(db)
    }

    private fun crearTablaCategorias(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLA_CAT (
                $COL_CAT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_CAT_NOMBRE TEXT NOT NULL COLLATE NOCASE,
                $COL_CAT_TIPO TEXT NOT NULL,
                UNIQUE($COL_CAT_NOMBRE, $COL_CAT_TIPO)
            )
            """.trimIndent()
        )
    }

    // ---------- Utilidades internas ----------
    private fun aValores(m: Movimiento) = ContentValues().apply {
        put(COL_TIPO, m.tipo.name)
        put(COL_MONTO, m.monto)
        put(COL_CATEGORIA, m.categoria)
        put(COL_DESCRIPCION, m.descripcion)
        put(COL_FECHA, m.fecha)
    }

    private fun leerMovimiento(c: Cursor) = Movimiento(
        id = c.getLong(c.getColumnIndexOrThrow(COL_ID)),
        tipo = TipoMovimiento.valueOf(c.getString(c.getColumnIndexOrThrow(COL_TIPO))),
        monto = c.getDouble(c.getColumnIndexOrThrow(COL_MONTO)),
        categoria = Categoria.nombreVisible(c.getString(c.getColumnIndexOrThrow(COL_CATEGORIA))),
        descripcion = c.getString(c.getColumnIndexOrThrow(COL_DESCRIPCION)) ?: "",
        fecha = c.getLong(c.getColumnIndexOrThrow(COL_FECHA))
    )

    // ---------- Movimientos ----------
    fun insertarMovimiento(m: Movimiento): Long =
        writableDatabase.insert(TABLA, null, aValores(m))

    fun actualizarMovimiento(m: Movimiento): Int =
        writableDatabase.update(TABLA, aValores(m), "$COL_ID = ?", arrayOf(m.id.toString()))

    fun eliminarMovimiento(id: Long): Int =
        writableDatabase.delete(TABLA, "$COL_ID = ?", arrayOf(id.toString()))

    fun obtenerPorId(id: Long): Movimiento? {
        val cursor = readableDatabase.query(
            TABLA, null, "$COL_ID = ?", arrayOf(id.toString()), null, null, null
        )
        return cursor.use { if (it.moveToFirst()) leerMovimiento(it) else null }
    }

    fun obtenerMovimientos(desde: Long = 0L, hasta: Long = Long.MAX_VALUE): List<Movimiento> {
        val lista = mutableListOf<Movimiento>()
        val cursor = readableDatabase.query(
            TABLA, null,
            "$COL_FECHA >= ? AND $COL_FECHA < ?",
            arrayOf(desde.toString(), hasta.toString()),
            null, null, "$COL_FECHA DESC, $COL_ID DESC"
        )
        cursor.use { while (it.moveToNext()) lista.add(leerMovimiento(it)) }
        return lista
    }

    fun obtenerPorTipo(
        tipo: TipoMovimiento, desde: Long = 0L, hasta: Long = Long.MAX_VALUE
    ): List<Movimiento> {
        val lista = mutableListOf<Movimiento>()
        val cursor = readableDatabase.query(
            TABLA, null,
            "$COL_TIPO = ? AND $COL_FECHA >= ? AND $COL_FECHA < ?",
            arrayOf(tipo.name, desde.toString(), hasta.toString()),
            null, null, "$COL_FECHA DESC, $COL_ID DESC"
        )
        cursor.use { while (it.moveToNext()) lista.add(leerMovimiento(it)) }
        return lista
    }

    // ---------- Totales ----------
    fun totalPorTipo(
        tipo: TipoMovimiento, desde: Long = 0L, hasta: Long = Long.MAX_VALUE
    ): Double {
        val cursor = readableDatabase.rawQuery(
            """SELECT COALESCE(SUM($COL_MONTO), 0) FROM $TABLA
               WHERE $COL_TIPO = ? AND $COL_FECHA >= ? AND $COL_FECHA < ?""",
            arrayOf(tipo.name, desde.toString(), hasta.toString())
        )
        return cursor.use { if (it.moveToFirst()) it.getDouble(0) else 0.0 }
    }

    fun totalPorCategoria(
        tipo: TipoMovimiento, desde: Long = 0L, hasta: Long = Long.MAX_VALUE
    ): Map<String, Double> {
        val mapa = linkedMapOf<String, Double>()
        val cursor = readableDatabase.rawQuery(
            """SELECT $COL_CATEGORIA, SUM($COL_MONTO) FROM $TABLA
               WHERE $COL_TIPO = ? AND $COL_FECHA >= ? AND $COL_FECHA < ?
               GROUP BY $COL_CATEGORIA""",
            arrayOf(tipo.name, desde.toString(), hasta.toString())
        )
        cursor.use {
            while (it.moveToNext()) {
                // Se une "COMIDA" (registro viejo) con "Comida" (registro nuevo)
                val nombre = Categoria.nombreVisible(it.getString(0))
                mapa[nombre] = (mapa[nombre] ?: 0.0) + it.getDouble(1)
            }
        }
        return mapa
    }

    // ---------- Categorías propias del usuario ----------
    fun categoriasPersonalizadas(tipo: TipoMovimiento): List<String> {
        val lista = mutableListOf<String>()
        val cursor = readableDatabase.query(
            TABLA_CAT, arrayOf(COL_CAT_NOMBRE),
            "$COL_CAT_TIPO = ?", arrayOf(tipo.name),
            null, null, "$COL_CAT_NOMBRE ASC"
        )
        cursor.use { while (it.moveToNext()) lista.add(it.getString(0)) }
        return lista
    }

    // Devuelve -1 si ya existía una con ese nombre
    fun insertarCategoria(nombre: String, tipo: TipoMovimiento): Long {
        val valores = ContentValues().apply {
            put(COL_CAT_NOMBRE, nombre)
            put(COL_CAT_TIPO, tipo.name)
        }
        return writableDatabase.insertWithOnConflict(
            TABLA_CAT, null, valores, SQLiteDatabase.CONFLICT_IGNORE
        )
    }
}