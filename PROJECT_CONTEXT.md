# Project Context

Este archivo resume el estado operativo del proyecto para futuras sesiones de
Codex. La app principal vive en este repositorio:

`C:\Users\whisperx\Downloads\videodownloader\video-downloader`

La copia portable que normalmente se prueba vive al lado:

`C:\Users\whisperx\Downloads\videodownloader\nuevabranch`

## Arquitectura General

Es una aplicacion Java Swing para programar y grabar streams. La GUI permite
configurar fuentes, horarios, nombre base de archivo, carpeta destino, pais,
tipo de stream y calidad preferida.

La app esta separada en modulos Maven independientes, no en un parent Maven
unico. Los modulos centrales son:

- `app`: GUI, configuracion de streams, schedule tasks y launcher principal.
- `core`: contratos base de configuracion, managers, schedules, tasks y support.
- `ffmpeg-support`: soporte base para transformar `Media` en comandos `ffmpeg`.
- `pluggable-support-manager`: carga dinamica de plugins `.jar` desde `supports`.
- `serializer-configuration-manager`: persistencia de configuraciones.
- `generic-support`: extractor grande para muchos sitios web y HLS genericos.
- `youtube-support`: integracion hibrida con `yt-dlp` para resolver HLS y grabar
  con `ffmpeg`.
- `*-support`: plugins concretos para sitios/canales especificos.

Entrada principal de la app:

`app/src/main/java/com/github/luischavez/videodownloader/app/Downloader.java`

La app registra managers, carga configuraciones, crea schedules y abre la GUI.
El manager de soportes usado en runtime es `PluggableSupportManager`, que busca
JARs en la carpeta `supports`.

## Como Se Ejecuta

La forma recomendada para el usuario es ejecutar la copia portable:

`C:\Users\whisperx\Downloads\videodownloader\nuevabranch\run-downloader.bat`

Ese `.bat`:

1. Define `APP_ROOT` como la carpeta `nuevabranch`.
2. Entra a esa carpeta.
3. Agrega `tools` al `PATH`.
4. Usa `runtime\bin\javaw.exe`.
5. Ejecuta `Downloader.jar`.

Tambien se puede abrir directamente:

`C:\Users\whisperx\Downloads\videodownloader\nuevabranch\Downloader.jar`

Si hay problemas con doble click, ejecutar el `.bat` ayuda porque prepara `PATH`
para `ffmpeg.exe` y `ffprobe.exe`.

## video-downloader vs nuevabranch

- `video-downloader`: repositorio Git con codigo fuente, historial y modulos.
- `nuevabranch`: carpeta portable de prueba/uso real. Contiene JARs ya armados,
  runtime Java embebido, `ffmpeg`, `ffprobe`, `yt-dlp`, configuraciones y logs.

Cuando se cambia codigo fuente en `video-downloader`, normalmente hay que copiar
o inyectar clases compiladas en los JARs dentro de `nuevabranch` para probarlo
en la GUI portable.

## Estructura De Carpetas Importante

Dentro de `video-downloader`:

- `app/`: GUI, tasks y armado de `Downloader.jar`.
- `core/`: interfaces y clases base.
- `ffmpeg-support/`: comandos y tasks basadas en `ffmpeg`.
- `generic-support/`: soporte generico para HLS y sitios variados.
- `youtube-support/`: soporte YouTube con `yt-dlp`.
- `supports/`: JARs de plugins usados por una copia del proyecto.
- `logs/`: logs locales, no suelen ser codigo util.
- `configurations/`: configuraciones serializadas locales.

Dentro de `nuevabranch`:

- `Downloader.jar`: launcher portable de la GUI.
- `run-downloader.bat`: entrada recomendada.
- `runtime/`: Java embebido.
- `supports/`: plugins `.jar` cargados por la app.
- `tools/`: `ffmpeg.exe` y `ffprobe.exe`.
- `youtube/`: `yt-dlp.exe`, `youtube-dl.exe` y configs.
- `configurations/`: configuraciones reales de streams.
- `output/`: descargas/pruebas.
- `logs/`: logs runtime.

## Ubicacion De Binarios Y Plugins

Portable actual:

- Java: `nuevabranch\runtime\bin\java.exe` y `javaw.exe`
- ffmpeg: `nuevabranch\tools\ffmpeg.exe`
- ffprobe: `nuevabranch\tools\ffprobe.exe`
- yt-dlp: `nuevabranch\youtube\yt-dlp.exe`
- youtube-dl legacy: `nuevabranch\youtube\youtube-dl.exe`
- plugins: `nuevabranch\supports\*.jar`

El loader de plugins espera que el nombre del JAR coincida con la clase support.
Ejemplo: `GenericSupport.jar` debe contener `GenericSupport` y
`GenericSupportLoader`.

## Flujo De Descarga

1. `Downloader.main()` mata PIDs antiguos registrados por `RunningPids`.
2. La app inicializa managers y carga configuraciones serializadas.
3. Cada `StreamConfiguration` crea o actualiza un `ScheduleStreamTask`.
4. En cada ciclo, `ScheduleStreamTask` busca un `Support` compatible con la URL.
5. El `Support` devuelve una lista de `Media`.
6. La lista se ordena por calidad.
7. Para streams normales se toma la calidad mas baja disponible.
8. Para YouTube, `ScheduleStreamTask` intenta respetar `preferredQuality`:
   selecciona la primera variante con altura >= calidad preferida; si no existe,
   cae a la variante mas alta disponible.
9. El `Support` genera un `Task`.
10. En soportes HLS, `FFMPEGSupport` genera un comando `ffmpeg`.
11. `FFMPEGTask` ejecuta ese comando con `cmd.exe /c` en Windows.

YouTube usa un flujo hibrido:

- `youtube-support` llama a `yt-dlp --dump-single-json` para obtener formatos.
- Filtra formatos HLS (`m3u8`).
- Crea objetos `Video` con calidad y URL real.
- Graba con `ffmpeg`, no con descarga directa de `yt-dlp`.

## Como Se Compila

El repo tiene `pom.xml` por modulo, pero no un `pom.xml` raiz. En esta PC se
observo que `mvn` no esta disponible en `PATH`, por eso varias reparaciones se
hicieron con `javac` manual y actualizacion directa de JARs.

Si Maven esta disponible, el flujo normal seria instalar dependencias internas
en orden aproximado:

```powershell
mvn -f core/pom.xml clean install
mvn -f ffmpeg-support/pom.xml clean install
mvn -f pluggable-support-manager/pom.xml clean install
mvn -f serializer-configuration-manager/pom.xml clean install
mvn -f generic-support/pom.xml clean package
mvn -f youtube-support/pom.xml clean package
mvn -f app/pom.xml clean package
```

El `app/pom.xml` arma `Downloader.jar` con `maven-assembly-plugin` y main class
`com.github.luischavez.videodownloader.app.Downloader`.

Si Maven no existe, usar el Java embebido de `nuevabranch\runtime` o un JDK
local, compilando contra los `target/classes` y dependencias existentes. Evitar
reconstruir un JAR completo a ciegas si ya hay un JAR portable funcional.

## Como Reconstruir Los JARs

Con Maven disponible:

```powershell
mvn -f generic-support/pom.xml clean package
mvn -f app/pom.xml clean package
```

Despues copiar los artefactos resultantes a la portable segun corresponda:

- `generic-support/target/video-downloader-generic-support-1.0.jar` ->
  `..\nuevabranch\supports\GenericSupport.jar`
- `app/target/Downloader.jar` -> `..\nuevabranch\Downloader.jar`

Verificar siempre que el JAR arranque antes de reemplazar una copia portable
funcional. Mantener backups cuando se toca `Downloader.jar`.

## Como Actualizar GenericSupport.jar

Ruta fuente:

`generic-support/src/main/java/GenericSupport.java`

Ruta portable:

`..\nuevabranch\supports\GenericSupport.jar`

Con Maven:

```powershell
mvn -f generic-support/pom.xml clean package
Copy-Item generic-support\target\video-downloader-generic-support-1.0.jar ..\nuevabranch\supports\GenericSupport.jar -Force
```

Sin Maven, estrategia usada en sesiones previas:

1. Compilar `GenericSupport.java` con `javac` contra clases ya existentes.
2. Reemplazar solo `GenericSupport.class` dentro de
   `nuevabranch\supports\GenericSupport.jar`.
3. Reiniciar la app para que `PluggableSupportManager` recargue el JAR.

Usar `jar uf` o una herramienta equivalente solo sobre el JAR correcto. No
rearmar `Downloader.jar` para cambios que pertenecen a un plugin support.

## Como Probar Streams Manualmente

Herramientas utiles en `nuevabranch`:

- `PrintSupportMedia.java`: imprime media resuelta para una URL.
- `ProbeCommand.java` / `ProbeCnewsCommand.java`: inspeccionan comandos.
- `tools\ffprobe.exe`: verifica pistas de audio/video.
- `tools\ffmpeg.exe`: prueba grabaciones cortas.

Ejemplos:

```powershell
cd C:\Users\whisperx\Downloads\videodownloader\nuevabranch
.\runtime\bin\java.exe -cp ".;Downloader.jar;supports\*" PrintSupportMedia "https://streamfare.com/fox-business-live-stream"
```

Verificar pistas:

```powershell
.\tools\ffprobe.exe -hide_banner -i "URL_O_ARCHIVO"
```

Prueba corta con ffmpeg:

```powershell
.\tools\ffmpeg.exe -y -t 20 -i "URL_M3U8" -c copy output\smoke-test.mkv
.\tools\ffprobe.exe -hide_banner output\smoke-test.mkv
```

Para casos con headers en URL, revisar `LocationRequestUtils.extractHeaders`.
Algunos soportes agregan headers en fragmentos `#__headers__`.

## Problemas Conocidos

- `mvn` no estaba disponible en `PATH` durante las ultimas sesiones.
- Hay muchos artefactos temporales de diagnostico que no deben commitearse:
  `.class`, `tmp_*`, logs generados y helpers puntuales de debug.
- Algunos sitios cambian HTML/API con frecuencia; `GenericSupport` es fragil.
- YouTube requiere cookies de Firefox en varios flujos (`--cookies-from-browser firefox`).
- Las variantes YouTube 144p/240p pueden traer HE-AAC; el soporte normaliza audio
  a AAC-LC para esas calidades.
- `Downloader.jar` puede quedar corrupto si se reconstuye manualmente mal. En una
  sesion anterior se restauro desde `Downloader-old.jar` y se inyecto solo la
  clase necesaria.
- Algunos HLS de canales son video-only aunque parezcan validos. Confirmar con
  `ffprobe`, no asumir que ffmpeg fallo.

### YouTube: configuracion activa

El 2026-07-25 se diagnostico un caso en el que YouTube devolvia `No video
formats found` aun usando cookies de Firefox, Node y EJS. La combinacion que
resolvio correctamente tanto una URL directa como una URL de canal `/streams`
fue:

```text
yt-dlp nightly 2026.07.23.234303
Node v24.13.1
--cookies-from-browser firefox
--js-runtimes node:RUTA_AL_NODE_24
--remote-components ejs:github
--extractor-args youtube:player_client=mweb
```

La implementacion activa prefiere `nuevabranch\youtube\node.exe` y agrega
`youtube:player_client=mweb` en `YouTubeSupport.runYtDlp`. Tanto
`nuevabranch\youtube\yt-dlp.exe` como Node 24 y `YouTubeSupport.jar` deben
mantenerse juntos al copiar o reconstruir el portable.

Esta configuracion no resuelve todavia la seleccion de multiples vivos simultaneos:
`/streams` sigue limitado al primer resultado.

## Branches Importantes

- `master`: rama remota estable historica.
- `feature/ytdlp-url-ffmpeg`: rama activa de trabajo actual. Contiene los cambios
  para usar `yt-dlp` como resolvedor y grabar con `ffmpeg`, manejo de calidades y
  fixes recientes de soportes.
- `feature/remote-control-api`: rama derivada para la API HTTP, el panel movil y
  futuras integraciones como Telegram. No mezclar este trabajo con fixes de
  supports hasta estabilizar la API.

Remote:

`origin https://github.com/seba1993/video-downloader.git`

## Ultimos Cambios Relevantes

- `2026-09-11 Remote control API`: monitoreo, control enable/disable/restart,
  logs, estado del sistema y panel web autenticado.
- `2026-07-29 Fox Business 720p con audio integrado`: elimina la mezcla entre
  el preview video-only de 240p y el audio temporal de LiveNewsNow. Ahora
  `streamfare.com/fox-business-live-stream` graba directamente el HLS combinado
  de LiveNewsNow a 1280x720 con video H.264 y audio AAC.
- `2026-05-17 only lowest quality`: politica general para preferir calidades bajas.
- `2026-05-15 Add America TV support and runtime fixes`.
- `Add LiveNOW FOX local stream support`.
- `Use lowest i24news variant with audio`.
- `Use yt-dlp only to resolve HLS URL; record YouTube with ffmpeg`.

## Partes Mas Fragiles

- `generic-support/src/main/java/GenericSupport.java`: archivo grande con muchas
  reglas especificas, regex y excepciones por dominio.
- `app/src/main/java/.../ScheduleStreamTask.java`: seleccion de media, retry y
  politica de calidad. Cambios aqui afectan todos los streams programados.
- `youtube-support/src/main/java/YouTubeSupport.java`: depende de salida JSON de
  `yt-dlp`, cookies del navegador y formatos HLS disponibles.
- `nuevabranch\Downloader.jar`: launcher portable; no reemplazar sin probar.
- `nuevabranch\supports\*.jar`: plugins cargados dinamicamente; nombre de JAR y
  nombre de clase deben coincidir.

## API Remota

La rama `feature/remote-control-api` agrega una API HTTP integrada y un panel
web movil. La configuracion se lee desde `remote-api.properties`, junto a
`Downloader.jar`; el archivo real contiene un token y no debe commitearse.

Endpoints iniciales: `/api/health`, `/api/streams`, `/api/tasks`, `/api/logs`,
`/api/system` y acciones `enable`, `disable`, `restart`, `refresh` por UID. Las
acciones pasan por `ConfigurationManager`, `TaskManager` y `AppContext`.

La documentacion operativa esta en `REMOTE_API.md`.

## Reglas Practicas Para Futuras Sesiones

- Antes de tocar codigo, revisar `git status --short --branch`.
- No commitear logs, `.class`, archivos `tmp_*`, descargas `.mkv` ni helpers de
  diagnostico salvo que el usuario lo pida.
- Cuando se arregla un support, actualizar tambien el JAR portable si el usuario
  necesita probarlo en la GUI.
- Para problemas de audio/video, validar con `ffprobe` la fuente HLS y el archivo
  resultante.
- Si el cambio es en un plugin, preferir actualizar solo el plugin JAR. Si el
  cambio es en `app`, actualizar `Downloader.jar`.
- Reiniciar la app despues de cambiar JARs en `nuevabranch`.
