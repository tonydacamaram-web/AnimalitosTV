package com.animalitostv.service

import android.content.Context
import android.util.Log
import com.animalitostv.util.StoragePaths
import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.net.URLEncoder

/**
 * Servidor web local para administrar el contenido del TV.
 *
 * - Usa [StoragePaths] (Environment.getExternalStorageDirectory) en lugar del literal "/sdcard".
 * - Devuelve los errores reales en el navegador (antes se perdían silenciosamente).
 * - Registra los fallos en el visor de logs de la app mediante [registrarLog].
 */
class AdHttpServer(
    port: Int = 8080,
    private val contexto: Context? = null,
    private val registrarLog: ((tipo: String, mensaje: String, stackTrace: String?) -> Unit)? = null
) : NanoHTTPD(port) {

    private val extMedia = setOf("jpg", "jpeg", "png", "webp", "bmp", "mp4", "avi", "mkv", "mov", "webm")
    private val extImagen = setOf("jpg", "jpeg", "png", "webp", "bmp")

    init {
        prepararCarpetas()
    }

    // ── Infraestructura ──────────────────────────────────────────────────────

    private fun prepararCarpetas() {
        listOf(StoragePaths.base, StoragePaths.ads, StoragePaths.fondos, StoragePaths.animales)
            .forEach { carpeta ->
                if (!StoragePaths.asegurar(carpeta)) {
                    reportar("No se pudo crear la carpeta ${carpeta.absolutePath}")
                }
            }
    }

    private fun reportar(mensaje: String, stackTrace: String? = null) {
        Log.e("AdHttpServer", mensaje)
        registrarLog?.invoke("ALMACENAMIENTO", mensaje, stackTrace)
    }

    private fun hayPermisoEscritura(): Boolean =
        contexto?.let { StoragePaths.permisoEscritura(it) } ?: true

    private fun mensajePermiso(): String =
        "El dispositivo no tiene permiso para escribir en el almacenamiento compartido.\n" +
            "En el TV: Ajustes → Aplicaciones → AnimalitosTV → Permisos → Archivos y medios\n" +
            "(o \"Acceso a todos los archivos\"). Luego vuelve a intentarlo.\n\n" +
            "Ruta usada: ${StoragePaths.ads.absolutePath}"

    override fun serve(session: IHTTPSession): Response {
        return try {
            val uri = session.uri
            val method = session.method
            when {
                method == Method.GET && uri == "/" -> paginaHtml("ads", session)
                method == Method.GET && uri == "/logo" -> paginaHtml("logo", session)
                method == Method.GET && uri == "/fondos" -> paginaHtml("fondos", session)
                method == Method.GET && uri == "/mensajes" -> paginaHtml("mensajes", session)
                method == Method.POST && uri == "/upload/ads" -> subirArchivos(session, StoragePaths.ads, extMedia, "/")
                method == Method.POST && uri == "/upload/logo" -> subirLogo(session)
                method == Method.POST && uri == "/upload/fondos" -> subirArchivos(session, StoragePaths.fondos, extImagen, "/fondos")
                method == Method.POST && uri == "/mensajes/agregar" -> agregarMensaje(session)
                method == Method.GET && uri == "/delete/ads" ->
                    confirmarEliminacion(session, StoragePaths.ads, "/", "publicidad", "ads")
                method == Method.POST && uri == "/delete/ads" -> eliminar(session, StoragePaths.ads, "/")
                method == Method.GET && uri == "/delete/fondos" ->
                    confirmarEliminacion(session, StoragePaths.fondos, "/fondos", "fondo", "fondos")
                method == Method.POST && uri == "/delete/fondos" -> eliminar(session, StoragePaths.fondos, "/fondos")
                method == Method.GET && uri == "/delete/mensaje" -> eliminarMensaje(session)
                else -> newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "404")
            }
        } catch (e: Exception) {
            reportar("Error procesando ${session.uri}: ${e.message}", e.stackTraceToString())
            paginaAviso(
                titulo = "Error inesperado",
                detalle = "${e.message ?: e.javaClass.simpleName}\n\nRuta: ${session.uri}\n" +
                    "Carpeta de publicidad: ${StoragePaths.ads.absolutePath}",
                volver = "/"
            )
        }
    }

    // ── Páginas HTML ─────────────────────────────────────────────────────────

    private fun css(): String = """
        *{box-sizing:border-box;margin:0;padding:0}
        body{background:#0f0f23;color:#e0e0e0;font-family:sans-serif;padding:16px}
        h1{color:#ffd700;font-size:22px;margin-bottom:4px}
        .sub{color:#aaa;font-size:13px;margin-bottom:16px}
        .tabs{display:flex;gap:8px;margin-bottom:16px;flex-wrap:wrap}
        .tab{padding:8px 18px;border-radius:6px 6px 0 0;background:#1a1a2e;color:#aaa;text-decoration:none;font-size:14px;border-bottom:2px solid transparent}
        .tab.active{color:#ffd700;border-bottom:2px solid #ffd700}
        .card{background:#1a1a2e;border-radius:0 8px 8px 8px;padding:16px;margin-bottom:16px}
        .card h3{color:#ffd700;font-size:13px;margin-bottom:12px;text-transform:uppercase;letter-spacing:1px}
        input[type=file]{color:#e0e0e0;background:#0f0f23;border:1px solid #444;border-radius:4px;padding:8px;width:100%;margin-bottom:10px}
        .btn{display:block;text-align:center;background:#43a047;color:#fff;border:none;padding:10px 20px;border-radius:4px;cursor:pointer;font-size:14px;width:100%;text-decoration:none}
        .btn:hover{background:#2e7d32}
        .btn.danger{background:#c62828}
        .btn.danger:hover{background:#8e0000}
        .info{background:#0d2137;border-radius:6px;padding:10px 14px;font-size:12px;color:#80cbc4;margin-bottom:12px;line-height:1.5}
        .ok{background:#12351f;border:1px solid #2e7d32;border-radius:6px;padding:10px 14px;font-size:13px;color:#8fe3a8;margin-bottom:12px}
        .bad{background:#3a1515;border:1px solid #c62828;border-radius:6px;padding:10px 14px;font-size:13px;color:#ff8a80;margin-bottom:12px;line-height:1.5}
        table{width:100%;border-collapse:collapse}
        th{color:#ffd700;font-size:12px;text-align:left;padding:6px 8px;border-bottom:1px solid #333}
        td{padding:8px;border-bottom:1px solid #1e1e3a;font-size:13px;word-break:break-all}
        a.del{color:#e53935;text-decoration:none;font-weight:bold}
        .empty{text-align:center;color:#666;padding:20px}
        .logo-preview{max-height:80px;max-width:200px;border-radius:4px;margin-bottom:10px}
    """.trimIndent()

    private fun layout(tab: String, contenido: String): String = """
        <!DOCTYPE html><html lang='es'>
        <head><meta charset='UTF-8'><meta name='viewport' content='width=device-width,initial-scale=1'>
        <title>AnimalitosTV — Admin</title><style>${css()}</style></head>
        <body>
        <h1>📺 AnimalitosTV</h1>
        <p class='sub'>Panel de administración de contenido</p>
        <div class='tabs'>
          <a class='tab${if (tab == "ads") " active" else ""}' href='/'>Publicidad</a>
          <a class='tab${if (tab == "logo") " active" else ""}' href='/logo'>Logo</a>
          <a class='tab${if (tab == "fondos") " active" else ""}' href='/fondos'>Fondos animales</a>
          <a class='tab${if (tab == "mensajes") " active" else ""}' href='/mensajes'>Mensajes ticker</a>
        </div>
        $contenido
        </body></html>
    """.trimIndent()

    private fun paginaHtml(tab: String, session: IHTTPSession): Response {
        val aviso = session.parameters["ok"]?.firstOrNull()?.takeIf { it.isNotBlank() }
        val banners = buildString {
            if (aviso != null) append("<div class='ok'>✔ ${escapeHtml(aviso)}</div>")
            if (!hayPermisoEscritura()) {
                append("<div class='bad'>⚠ Sin permiso de escritura: los archivos que subas no se guardarán.<br>")
                append("Ruta usada: ${escapeHtml(StoragePaths.ads.absolutePath)}</div>")
            }
        }
        val contenido = banners + when (tab) {
            "logo" -> tabLogo()
            "fondos" -> tabFondos()
            "mensajes" -> tabMensajes()
            else -> tabAds()
        }
        return newFixedLengthResponse(Response.Status.OK, "text/html; charset=UTF-8", layout(tab, contenido))
    }

    private fun paginaAviso(titulo: String, detalle: String, volver: String): Response {
        val contenido = """
            <div class='card'>
              <h3>${escapeHtml(titulo)}</h3>
              <div class='bad' style='white-space:pre-wrap'>${escapeHtml(detalle)}</div>
              <p style='margin-top:12px'><a class='btn' href='$volver'>Volver</a></p>
            </div>
        """.trimIndent()
        return newFixedLengthResponse(Response.Status.OK, "text/html; charset=UTF-8", layout("", contenido))
    }

    private fun listar(carpeta: File): List<File> =
        runCatching { carpeta.listFiles()?.filter { it.isFile }?.sortedBy { it.name.lowercase() } }
            .getOrNull()
            .orEmpty()

    // ── Tab Publicidad ───────────────────────────────────────────────────────

    private fun tabAds(): String {
        val carpeta = StoragePaths.ads
        val archivos = listar(carpeta)
        val filas = if (archivos.isEmpty()) {
            "<tr><td colspan='3' class='empty'>Sin archivos publicitarios</td></tr>"
        } else {
            archivos.joinToString("") { f ->
                val size = formatSize(f.length())
                "<tr><td>${escapeHtml(f.name)}</td><td style='color:#aaa'>$size</td>" +
                    "<td><a class='del' href='/delete/ads?file=${urlEncode(f.name)}'>✖ Eliminar</a></td></tr>"
            }
        }
        return """
            <div class='card'>
              <h3>Subir publicidad</h3>
              <p class='info'>Soporta: jpg, png, webp, bmp, mp4, avi, mkv, mov, webm<br>
              Carpeta en el TV: ${escapeHtml(carpeta.absolutePath)}</p>
              <form method='POST' action='/upload/ads' enctype='multipart/form-data'>
                <input type='file' name='files' multiple accept='.jpg,.jpeg,.png,.webp,.bmp,.mp4,.avi,.mkv,.mov,.webm'>
                <button class='btn' type='submit'>⬆ Subir archivos</button>
              </form>
            </div>
            <div class='card'>
              <h3>Archivos actuales (${archivos.size})</h3>
              <table><thead><tr><th>Archivo</th><th>Tamaño</th><th></th></tr></thead>
              <tbody>$filas</tbody></table>
            </div>
        """.trimIndent()
    }

    // ── Tab Logo ─────────────────────────────────────────────────────────────

    private fun tabLogo(): String {
        val logo = StoragePaths.logo
        val estado = if (logo.exists()) {
            "<p style='color:#aaa;font-size:12px;margin-bottom:6px'>Logo actual: logo.png (${formatSize(logo.length())})</p>"
        } else {
            "<p style='color:#666;font-size:12px;margin-bottom:6px'>Sin logo cargado</p>"
        }
        return """
            <div class='card'>
              <h3>Logo del negocio</h3>
              <p class='info'>El archivo se guarda como logo.png en ${escapeHtml(StoragePaths.base.absolutePath)}. Formatos: jpg, jpeg, png, webp</p>
              $estado
              <form method='POST' action='/upload/logo' enctype='multipart/form-data'>
                <input type='file' name='logo' accept='.jpg,.jpeg,.png,.webp'>
                <button class='btn' type='submit'>⬆ Subir logo</button>
              </form>
            </div>
        """.trimIndent()
    }

    // ── Tab Fondos ───────────────────────────────────────────────────────────

    private fun tabFondos(): String {
        val carpeta = StoragePaths.fondos
        val archivos = listar(carpeta).sortedBy { it.nameWithoutExtension.toIntOrNull() ?: 999 }
        val filas = if (archivos.isEmpty()) {
            "<tr><td colspan='3' class='empty'>Sin fondos cargados (0 / 101)</td></tr>"
        } else {
            archivos.joinToString("") { f ->
                val size = formatSize(f.length())
                "<tr><td>${escapeHtml(f.name)}</td><td style='color:#aaa'>$size</td>" +
                    "<td><a class='del' href='/delete/fondos?file=${urlEncode(f.name)}'>✖ Eliminar</a></td></tr>"
            }
        }
        return """
            <div class='card'>
              <h3>Fondos de animales (${archivos.size} / 101)</h3>
              <p class='info'>Nombrar cada archivo con el número del animal: 0.png, 1.png … 100.png<br>
              Tamaño recomendado: 360×120 px (ratio 3:1). Formato: png, jpg, webp<br>
              Carpeta en el TV: ${escapeHtml(carpeta.absolutePath)}</p>
              <form method='POST' action='/upload/fondos' enctype='multipart/form-data'>
                <input type='file' name='files' multiple accept='.jpg,.jpeg,.png,.webp'>
                <button class='btn' type='submit'>⬆ Subir fondos</button>
              </form>
            </div>
            <div class='card'>
              <h3>Fondos cargados (${archivos.size})</h3>
              <table><thead><tr><th>Archivo</th><th>Tamaño</th><th></th></tr></thead>
              <tbody>$filas</tbody></table>
            </div>
        """.trimIndent()
    }

    // ── Tab Mensajes ─────────────────────────────────────────────────────────

    private fun tabMensajes(): String {
        val mensajes = runCatching {
            StoragePaths.mensajes.takeIf { it.exists() }?.readLines()
                ?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
        }.getOrDefault(emptyList())

        val filas = if (mensajes.isEmpty()) {
            "<tr><td colspan='2' class='empty'>Sin mensajes personalizados</td></tr>"
        } else {
            mensajes.mapIndexed { i, msg ->
                "<tr><td>${escapeHtml(msg)}</td>" +
                    "<td><a class='del' href='/delete/mensaje?i=$i'>✖ Eliminar</a></td></tr>"
            }.joinToString("")
        }

        return """
            <div class='card'>
              <h3>Agregar mensaje al ticker</h3>
              <p class='info'>Los mensajes aparecen en el ticker inferior con el prefijo 📢, intercalados con las estadísticas. Se actualizan cada 30 segundos.</p>
              <form method='POST' action='/mensajes/agregar'>
                <input type='text' name='mensaje' placeholder='Escribe tu mensaje aquí...' maxlength='200'
                  style='width:100%;padding:10px;background:#0f0f23;color:#e0e0e0;border:1px solid #444;border-radius:4px;margin-bottom:10px;font-size:14px'>
                <button class='btn' type='submit'>➕ Agregar mensaje</button>
              </form>
            </div>
            <div class='card'>
              <h3>Mensajes activos (${mensajes.size})</h3>
              <table><thead><tr><th>Mensaje</th><th></th></tr></thead>
              <tbody>$filas</tbody></table>
            </div>
        """.trimIndent()
    }

    private fun escapeHtml(texto: String): String = texto
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")

    // ── Handlers de upload ───────────────────────────────────────────────────

    private fun subirArchivos(
        session: IHTTPSession,
        destino: File,
        extensiones: Set<String>,
        redirigir: String
    ): Response {
        if (!hayPermisoEscritura()) {
            return paginaAviso("Sin permiso de almacenamiento", mensajePermiso(), redirigir)
        }

        val temporales = mutableMapOf<String, String>()
        session.parseBody(temporales)

        val nombres = session.parameters["files"].orEmpty()
        if (nombres.isEmpty()) {
            return paginaAviso(
                "No se recibió ningún archivo",
                "Selecciona al menos un archivo e inténtalo de nuevo.",
                redirigir
            )
        }
        if (!StoragePaths.asegurar(destino)) {
            return paginaAviso(
                "No se pudo crear la carpeta",
                "No fue posible crear ${destino.absolutePath}.\n" +
                    "Verifica el permiso de almacenamiento del TV.",
                redirigir
            )
        }

        var subidos = 0
        val errores = mutableListOf<String>()

        nombres.forEachIndexed { indice, nombreOriginal ->
            // NanoHTTPD guarda los temporales multipart como "files", "files2", "files3"...
            val claveTemporal = if (indice == 0) "files" else "files${indice + 1}"
            val rutaTemporal = temporales[claveTemporal]
            val nombre = nombreOriginal.substringAfterLast('/').substringAfterLast('\\').trim()
            val extension = nombre.substringAfterLast('.', "").lowercase()

            when {
                rutaTemporal == null ->
                    errores += "$nombre (archivo #${indice + 1}): no llegó el contenido"
                nombre.isBlank() ->
                    errores += "Archivo #${indice + 1}: nombre inválido"
                extension !in extensiones ->
                    errores += "$nombre: formato .$extension no permitido"
                else -> {
                    val archivoDestino = File(destino, nombre)
                    val resultado = runCatching {
                        File(rutaTemporal).copyTo(archivoDestino, overwrite = true)
                    }
                    if (resultado.isSuccess && archivoDestino.length() > 0) {
                        subidos++
                        Log.i("AdHttpServer", "Subido: $nombre → ${destino.absolutePath}")
                    } else {
                        val causa = resultado.exceptionOrNull()?.message ?: "el sistema rechazó la escritura"
                        errores += "$nombre: $causa"
                        reportar(
                            "No se pudo guardar $nombre en ${destino.absolutePath}: $causa",
                            resultado.exceptionOrNull()?.stackTraceToString()
                        )
                    }
                }
            }
        }

        return when {
            errores.isEmpty() ->
                redirectConAviso(redirigir, "$subidos archivo(s) subido(s) correctamente")
            else -> paginaAviso(
                titulo = if (subidos > 0) "Subida parcial ($subidos guardado(s))"
                else "No se pudo subir ningún archivo",
                detalle = errores.joinToString("\n") { "• $it" } +
                    "\n\nCarpeta: ${destino.absolutePath}",
                volver = redirigir
            )
        }
    }

    private fun subirLogo(session: IHTTPSession): Response {
        if (!hayPermisoEscritura()) {
            return paginaAviso("Sin permiso de almacenamiento", mensajePermiso(), "/logo")
        }

        val temporales = mutableMapOf<String, String>()
        session.parseBody(temporales)
        val nombre = session.parameters["logo"]?.firstOrNull()
        val rutaTemporal = temporales["logo"]
        if (rutaTemporal == null) {
            return paginaAviso("No se recibió el logo", "Selecciona un archivo de imagen.", "/logo")
        }
        val extension = nombre?.substringAfterLast('.', "")?.lowercase().orEmpty()
        if (extension !in extImagen) {
            return paginaAviso("Formato no permitido", "Usa jpg, jpeg, png o webp.", "/logo")
        }
        if (!StoragePaths.asegurar(StoragePaths.base)) {
            return paginaAviso(
                "No se pudo crear la carpeta",
                "No fue posible crear ${StoragePaths.base.absolutePath}.",
                "/logo"
            )
        }

        val resultado = runCatching { File(rutaTemporal).copyTo(StoragePaths.logo, overwrite = true) }
        return if (resultado.isSuccess && StoragePaths.logo.length() > 0) {
            redirectConAviso("/logo", "Logo actualizado")
        } else {
            val causa = resultado.exceptionOrNull()?.message ?: "el sistema rechazó la escritura"
            reportar("No se pudo guardar el logo: $causa", resultado.exceptionOrNull()?.stackTraceToString())
            paginaAviso(
                "No se pudo guardar el logo",
                "$causa\n\nRuta: ${StoragePaths.logo.absolutePath}",
                "/logo"
            )
        }
    }

    // ── Handlers de mensajes ─────────────────────────────────────────────────

    private fun agregarMensaje(session: IHTTPSession): Response {
        if (!hayPermisoEscritura()) {
            return paginaAviso("Sin permiso de almacenamiento", mensajePermiso(), "/mensajes")
        }
        val cuerpo = mutableMapOf<String, String>()
        session.parseBody(cuerpo)
        val mensaje = session.parameters["mensaje"]?.firstOrNull()?.trim()
        if (mensaje.isNullOrBlank()) {
            return redirectConAviso("/mensajes", "Escribe un mensaje antes de agregar")
        }
        if (!StoragePaths.asegurar(StoragePaths.base)) {
            return paginaAviso(
                "No se pudo crear la carpeta",
                "No fue posible crear ${StoragePaths.base.absolutePath}.",
                "/mensajes"
            )
        }
        val resultado = runCatching { StoragePaths.mensajes.appendText("$mensaje\n") }
        return if (resultado.isSuccess) {
            redirectConAviso("/mensajes", "Mensaje agregado")
        } else {
            val causa = resultado.exceptionOrNull()?.message ?: "el sistema rechazó la escritura"
            reportar("No se pudo guardar el mensaje: $causa", resultado.exceptionOrNull()?.stackTraceToString())
            paginaAviso(
                "No se pudo guardar el mensaje",
                "$causa\n\nRuta: ${StoragePaths.mensajes.absolutePath}",
                "/mensajes"
            )
        }
    }

    private fun eliminarMensaje(session: IHTTPSession): Response {
        if (!hayPermisoEscritura()) {
            return paginaAviso("Sin permiso de almacenamiento", mensajePermiso(), "/mensajes")
        }
        val indice = session.parameters["i"]?.firstOrNull()?.toIntOrNull()
            ?: return paginaAviso("Mensaje no indicado", "Falta el parámetro 'i'.", "/mensajes")
        val lineas = runCatching {
            StoragePaths.mensajes.takeIf { it.exists() }
                ?.readLines()?.map { it.trim() }?.filter { it.isNotBlank() }
                ?: emptyList()
        }.getOrDefault(emptyList())
        if (indice !in lineas.indices) {
            return paginaAviso("El mensaje ya no existe", "Índice $indice fuera de rango.", "/mensajes")
        }
        val nuevas = lineas.toMutableList().also { it.removeAt(indice) }
        val resultado = runCatching {
            StoragePaths.mensajes.writeText(
                nuevas.joinToString("\n") + if (nuevas.isNotEmpty()) "\n" else ""
            )
        }
        return if (resultado.isSuccess) {
            redirectConAviso("/mensajes", "Mensaje eliminado")
        } else {
            val causa = resultado.exceptionOrNull()?.message ?: "el sistema rechazó la escritura"
            reportar("No se pudo eliminar el mensaje: $causa", resultado.exceptionOrNull()?.stackTraceToString())
            paginaAviso(
                "No se pudo eliminar el mensaje",
                "$causa\n\nRuta: ${StoragePaths.mensajes.absolutePath}",
                "/mensajes"
            )
        }
    }

    // ── Handlers de borrado ──────────────────────────────────────────────────

    private fun confirmarEliminacion(
        session: IHTTPSession,
        carpeta: File,
        redirigir: String,
        etiqueta: String,
        accion: String
    ): Response {
        val nombre = session.parameters["file"]?.firstOrNull()
            ?: return paginaAviso("Archivo no indicado", "Falta el parámetro 'file'.", redirigir)
        val archivo = File(carpeta, nombre)
        if (!archivo.canonicalPath.startsWith(carpeta.canonicalPath + File.separator)) {
            return paginaAviso(
                "Ruta no permitida",
                "El archivo debe estar dentro de ${carpeta.absolutePath}.",
                redirigir
            )
        }
        if (!archivo.exists()) {
            return paginaAviso("Archivo no encontrado", "${archivo.absolutePath} no existe.", redirigir)
        }
        val contenido = """
            <div class='card'>
              <h3>Eliminar $etiqueta</h3>
              <p class='info'>¿Eliminar <b>${escapeHtml(archivo.name)}</b> de ${escapeHtml(carpeta.absolutePath)}?</p>
              <form method='POST' action='/delete/$accion'>
                <input type='hidden' name='file' value='${escapeHtml(archivo.name)}'>
                <button class='btn danger' type='submit'>Sí, eliminar</button>
              </form>
              <p style='margin-top:12px'><a class='tab' href='$redirigir'>Cancelar</a></p>
            </div>
        """.trimIndent()
        return newFixedLengthResponse(Response.Status.OK, "text/html; charset=UTF-8", layout(accion, contenido))
    }

    private fun eliminar(session: IHTTPSession, carpeta: File, redirigir: String): Response {
        if (!hayPermisoEscritura()) {
            return paginaAviso("Sin permiso de almacenamiento", mensajePermiso(), redirigir)
        }
        val cuerpo = mutableMapOf<String, String>()
        runCatching { session.parseBody(cuerpo) }
        val nombre = session.parameters["file"]?.firstOrNull()
            ?: return paginaAviso("Archivo no indicado", "Falta el parámetro 'file'.", redirigir)
        val archivo = File(carpeta, nombre)
        if (!archivo.canonicalPath.startsWith(carpeta.canonicalPath + File.separator)) {
            return paginaAviso(
                "Ruta no permitida",
                "El archivo debe estar dentro de ${carpeta.absolutePath}.",
                redirigir
            )
        }
        if (!archivo.exists()) {
            return paginaAviso("El archivo ya no existe", archivo.absolutePath, redirigir)
        }
        val borrado = runCatching { archivo.delete() }.getOrDefault(false)
        return if (borrado) {
            redirectConAviso(redirigir, "Eliminado: $nombre")
        } else {
            reportar("No se pudo eliminar ${archivo.absolutePath}")
            paginaAviso(
                "No se pudo eliminar el archivo",
                "El sistema rechazó el borrado. Verifica el permiso de almacenamiento.\n\n" +
                    archivo.absolutePath,
                redirigir
            )
        }
    }

    // ── Utilidades ───────────────────────────────────────────────────────────

    private fun redirect(url: String): Response =
        newFixedLengthResponse(Response.Status.REDIRECT_SEE_OTHER, MIME_PLAINTEXT, "")
            .also { it.addHeader("Location", url) }

    private fun redirectConAviso(ruta: String, mensaje: String): Response =
        redirect("$ruta?ok=${urlEncode(mensaje)}")

    private fun urlEncode(valor: String): String =
        URLEncoder.encode(valor, "UTF-8").replace("+", "%20")

    private fun formatSize(bytes: Long): String {
        val kb = bytes / 1024
        return if (kb > 1024) "${kb / 1024} MB" else "$kb KB"
    }
}
