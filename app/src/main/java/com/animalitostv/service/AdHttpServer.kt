package com.animalitostv.service

import android.util.Log
import fi.iki.elonen.NanoHTTPD
import java.io.File

class AdHttpServer(port: Int = 8080) : NanoHTTPD(port) {

    private val carpetaAds    = File("/sdcard/AnimalitosAds").also { runCatching { it.mkdirs() } }
    private val carpetaBase   = File("/sdcard/AnimalitosTV").also { runCatching { it.mkdirs() } }
    private val carpetaFondos = File("/sdcard/AnimalitosTV/fondos").also { runCatching { it.mkdirs() } }
    private val archivoMensajes = File("/sdcard/AnimalitosTV/mensajes.txt")

    private val extMedia = setOf("jpg", "jpeg", "png", "webp", "bmp", "mp4", "avi", "mkv", "mov", "webm")
    private val extImagen = setOf("jpg", "jpeg", "png", "webp", "bmp")

    override fun serve(session: IHTTPSession): Response {
        return try {
            val uri = session.uri
            val method = session.method
            when {
                method == Method.GET  && uri == "/"                  -> paginaHtml("ads")
                method == Method.GET  && uri == "/logo"              -> paginaHtml("logo")
                method == Method.GET  && uri == "/fondos"            -> paginaHtml("fondos")
                method == Method.GET  && uri == "/mensajes"          -> paginaHtml("mensajes")
                method == Method.POST && uri == "/upload/ads"        -> subirArchivos(session, carpetaAds, extMedia, "/")
                method == Method.POST && uri == "/upload/logo"       -> subirLogo(session)
                method == Method.POST && uri == "/upload/fondos"     -> subirArchivos(session, carpetaFondos, extImagen, "/fondos")
                method == Method.POST && uri == "/mensajes/agregar"  -> agregarMensaje(session)
                method == Method.GET  && uri == "/delete/ads"        -> eliminar(session, carpetaAds, "/")
                method == Method.GET  && uri == "/delete/fondos"     -> eliminar(session, carpetaFondos, "/fondos")
                method == Method.GET  && uri == "/delete/mensaje"    -> eliminarMensaje(session)
                else -> newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "404")
            }
        } catch (e: Exception) {
            Log.e("AdHttpServer", "Error ${session.uri}", e)
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Error: ${e.message}")
        }
    }

    // ── HTML principal con tabs ──────────────────────────────────────────────

    private fun paginaHtml(tab: String): Response {
        val css = """
            *{box-sizing:border-box;margin:0;padding:0}
            body{background:#0f0f23;color:#e0e0e0;font-family:sans-serif;padding:16px}
            h1{color:#ffd700;font-size:22px;margin-bottom:4px}
            .sub{color:#aaa;font-size:13px;margin-bottom:16px}
            .tabs{display:flex;gap:8px;margin-bottom:16px}
            .tab{padding:8px 18px;border-radius:6px 6px 0 0;background:#1a1a2e;color:#aaa;text-decoration:none;font-size:14px;border-bottom:2px solid transparent}
            .tab.active{color:#ffd700;border-bottom:2px solid #ffd700}
            .card{background:#1a1a2e;border-radius:0 8px 8px 8px;padding:16px;margin-bottom:16px}
            .card h3{color:#ffd700;font-size:13px;margin-bottom:12px;text-transform:uppercase;letter-spacing:1px}
            input[type=file]{color:#e0e0e0;background:#0f0f23;border:1px solid #444;border-radius:4px;padding:8px;width:100%;margin-bottom:10px}
            .btn{background:#43a047;color:#fff;border:none;padding:10px 20px;border-radius:4px;cursor:pointer;font-size:14px;width:100%}
            .btn:hover{background:#2e7d32}
            .info{background:#0d2137;border-radius:6px;padding:10px 14px;font-size:12px;color:#80cbc4;margin-bottom:12px}
            table{width:100%;border-collapse:collapse}
            th{color:#ffd700;font-size:12px;text-align:left;padding:6px 8px;border-bottom:1px solid #333}
            td{padding:8px;border-bottom:1px solid #1e1e3a;font-size:13px;word-break:break-all}
            a.del{color:#e53935;text-decoration:none;font-weight:bold}
            .empty{text-align:center;color:#666;padding:20px}
            .logo-preview{max-height:80px;max-width:200px;border-radius:4px;margin-bottom:10px}
        """.trimIndent()

        val content = when (tab) {
            "logo"     -> tabLogo()
            "fondos"   -> tabFondos()
            "mensajes" -> tabMensajes()
            else       -> tabAds()
        }

        val html = """
            <!DOCTYPE html><html lang='es'>
            <head><meta charset='UTF-8'><meta name='viewport' content='width=device-width,initial-scale=1'>
            <title>AnimalitosTV — Admin</title><style>$css</style></head>
            <body>
            <h1>📺 AnimalitosTV</h1>
            <p class='sub'>Panel de administración de contenido</p>
            <div class='tabs'>
              <a class='tab ${if (tab=="ads") "active" else ""}' href='/'>Publicidad</a>
              <a class='tab ${if (tab=="logo") "active" else ""}' href='/logo'>Logo</a>
              <a class='tab ${if (tab=="fondos") "active" else ""}' href='/fondos'>Fondos animales</a>
              <a class='tab ${if (tab=="mensajes") "active" else ""}' href='/mensajes'>Mensajes ticker</a>
            </div>
            $content
            </body></html>
        """.trimIndent()

        return newFixedLengthResponse(Response.Status.OK, "text/html; charset=UTF-8", html)
    }

    // ── Tab Publicidad ───────────────────────────────────────────────────────

    private fun tabAds(): String {
        val archivos = carpetaAds.listFiles()?.filter { it.isFile }?.sortedBy { it.name } ?: emptyList()
        val filas = if (archivos.isEmpty()) {
            "<tr><td colspan='3' class='empty'>Sin archivos publicitarios</td></tr>"
        } else {
            archivos.joinToString("") { f ->
                val size = formatSize(f.length())
                "<tr><td>${f.name}</td><td style='color:#aaa'>$size</td>" +
                "<td><a class='del' href='/delete/ads?file=${f.name}' onclick=\"return confirm('Eliminar ${f.name}?')\">✕</a></td></tr>"
            }
        }
        return """
            <div class='card'>
              <h3>Subir publicidad</h3>
              <p class='info'>Soporta: jpg, png, webp, bmp, mp4, avi, mkv, mov, webm</p>
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
        val logoFile = File("/sdcard/AnimalitosTV/logo.png")
        val preview = if (logoFile.exists()) {
            "<p style='color:#aaa;font-size:12px;margin-bottom:6px'>Logo actual: logo.png (${formatSize(logoFile.length())})</p>"
        } else {
            "<p style='color:#666;font-size:12px;margin-bottom:6px'>Sin logo cargado</p>"
        }
        return """
            <div class='card'>
              <h3>Logo del negocio</h3>
              <p class='info'>El archivo se guardará como logo.png en /sdcard/AnimalitosTV/. Formatos: jpg, jpeg, png, webp</p>
              $preview
              <form method='POST' action='/upload/logo' enctype='multipart/form-data'>
                <input type='file' name='logo' accept='.jpg,.jpeg,.png,.webp'>
                <button class='btn' type='submit'>⬆ Subir logo</button>
              </form>
            </div>
        """.trimIndent()
    }

    // ── Tab Fondos ───────────────────────────────────────────────────────────

    private fun tabFondos(): String {
        val archivos = carpetaFondos.listFiles()?.filter { it.isFile }?.sortedBy { it.nameWithoutExtension.toIntOrNull() ?: 999 } ?: emptyList()
        val cargados = archivos.size
        val filas = if (archivos.isEmpty()) {
            "<tr><td colspan='3' class='empty'>Sin fondos cargados (0 / 101)</td></tr>"
        } else {
            archivos.joinToString("") { f ->
                val size = formatSize(f.length())
                "<tr><td>${f.name}</td><td style='color:#aaa'>$size</td>" +
                "<td><a class='del' href='/delete/fondos?file=${f.name}' onclick=\"return confirm('Eliminar ${f.name}?')\">✕</a></td></tr>"
            }
        }
        return """
            <div class='card'>
              <h3>Fondos de animales ($cargados / 101)</h3>
              <p class='info'>Nombrar cada archivo con el número del animal: 0.png, 1.png … 100.png<br>
              Tamaño recomendado: 360×120 px (ratio 3:1). Formato: png, jpg, webp</p>
              <form method='POST' action='/upload/fondos' enctype='multipart/form-data'>
                <input type='file' name='files' multiple accept='.jpg,.jpeg,.png,.webp'>
                <button class='btn' type='submit'>⬆ Subir fondos</button>
              </form>
            </div>
            <div class='card'>
              <h3>Fondos cargados ($cargados)</h3>
              <table><thead><tr><th>Archivo</th><th>Tamaño</th><th></th></tr></thead>
              <tbody>$filas</tbody></table>
            </div>
        """.trimIndent()
    }

    // ── Tab Mensajes ─────────────────────────────────────────────────────────

    private fun tabMensajes(): String {
        val mensajes = runCatching {
            archivoMensajes.takeIf { it.exists() }?.readLines()
                ?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
        }.getOrDefault(emptyList())

        val filas = if (mensajes.isEmpty()) {
            "<tr><td colspan='2' class='empty'>Sin mensajes personalizados</td></tr>"
        } else {
            mensajes.mapIndexed { i, msg ->
                "<tr><td>${escapeHtml(msg)}</td>" +
                "<td><a class='del' href='/delete/mensaje?i=$i' onclick=\"return confirm('Eliminar este mensaje?')\">✕</a></td></tr>"
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

    private fun escapeHtml(text: String): String =
        text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    // ── Handlers de upload / delete ──────────────────────────────────────────

    private fun subirArchivos(
        session: IHTTPSession,
        destino: File,
        extensiones: Set<String>,
        redirigir: String
    ): Response {
        val files = mutableMapOf<String, String>()
        session.parseBody(files)
        val params = session.parameters
        var subidos = 0
        files.forEach { (key, tempPath) ->
            val nombre = params[key]?.firstOrNull() ?: return@forEach
            val ext = nombre.substringAfterLast('.', "").lowercase()
            if (ext !in extensiones) return@forEach
            val nombreLimpio = nombre.substringAfterLast('/').substringAfterLast('\\')
            runCatching {
                File(tempPath).copyTo(File(destino, nombreLimpio), overwrite = true)
                subidos++
                Log.i("AdHttpServer", "Subido: $nombreLimpio → ${destino.name}/")
            }.onFailure { Log.e("AdHttpServer", "Error al guardar $nombreLimpio", it) }
        }
        Log.i("AdHttpServer", "$subidos archivo(s) subidos a ${destino.name}")
        return redirect(redirigir)
    }

    private fun subirLogo(session: IHTTPSession): Response {
        val files = mutableMapOf<String, String>()
        session.parseBody(files)
        val params = session.parameters
        val tempPath = files["logo"] ?: return redirect("/logo")
        val nombre = params["logo"]?.firstOrNull() ?: return redirect("/logo")
        val ext = nombre.substringAfterLast('.', "").lowercase()
        if (ext !in extImagen) return redirect("/logo")
        runCatching {
            File(tempPath).copyTo(File("/sdcard/AnimalitosTV/logo.png"), overwrite = true)
            Log.i("AdHttpServer", "Logo actualizado")
        }.onFailure { Log.e("AdHttpServer", "Error al guardar logo", it) }
        return redirect("/logo")
    }

    private fun agregarMensaje(session: IHTTPSession): Response {
        val body = mutableMapOf<String, String>()
        session.parseBody(body)
        val mensaje = session.parameters["mensaje"]?.firstOrNull()?.trim()
            ?: return redirect("/mensajes")
        if (mensaje.isBlank()) return redirect("/mensajes")
        runCatching {
            archivoMensajes.appendText("$mensaje\n")
            Log.i("AdHttpServer", "Mensaje agregado: $mensaje")
        }.onFailure { Log.e("AdHttpServer", "Error al guardar mensaje", it) }
        return redirect("/mensajes")
    }

    private fun eliminarMensaje(session: IHTTPSession): Response {
        val indice = session.parameters["i"]?.firstOrNull()?.toIntOrNull()
            ?: return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Falta 'i'")
        runCatching {
            val lineas = archivoMensajes.takeIf { it.exists() }
                ?.readLines()?.map { it.trim() }?.filter { it.isNotBlank() }
                ?: emptyList()
            if (indice in lineas.indices) {
                val nuevas = lineas.toMutableList().also { it.removeAt(indice) }
                archivoMensajes.writeText(nuevas.joinToString("\n") + if (nuevas.isNotEmpty()) "\n" else "")
                Log.i("AdHttpServer", "Mensaje $indice eliminado")
            }
        }.onFailure { Log.e("AdHttpServer", "Error al eliminar mensaje", it) }
        return redirect("/mensajes")
    }

    private fun eliminar(session: IHTTPSession, carpeta: File, redirigir: String): Response {
        val nombre = session.parameters["file"]?.firstOrNull()
            ?: return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Falta 'file'")
        val archivo = File(carpeta, nombre)
        if (!archivo.canonicalPath.startsWith(carpeta.canonicalPath))
            return newFixedLengthResponse(Response.Status.FORBIDDEN, MIME_PLAINTEXT, "Ruta no permitida")
        if (archivo.exists()) {
            archivo.delete()
            Log.i("AdHttpServer", "Eliminado: $nombre")
        }
        return redirect(redirigir)
    }

    // ── Utilidades ───────────────────────────────────────────────────────────

    private fun redirect(url: String) =
        newFixedLengthResponse(Response.Status.REDIRECT_SEE_OTHER, MIME_PLAINTEXT, "")
            .also { it.addHeader("Location", url) }

    private fun formatSize(bytes: Long): String {
        val kb = bytes / 1024
        return if (kb > 1024) "${kb / 1024} MB" else "$kb KB"
    }
}
