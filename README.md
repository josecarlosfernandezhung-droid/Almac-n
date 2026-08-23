# Control de Almacén Geova — App Android

Este repositorio empaqueta `www/index.html` (tu app offline-first) como una
app Android (.apk) usando Capacitor. Todo el trabajo pesado (generar el
proyecto Android, instalar dependencias, compilar) lo hace GitHub Actions —
no necesitas Android Studio ni una PC.

## Cómo compilar (desde el móvil)

1. Sube estos archivos a un repositorio nuevo en GitHub (por la app de
   GitHub, o subiendo el .zip y extrayendo con "Add file → Upload files").
2. En cuanto hagas push a la rama `main`, se dispara solo el workflow
   **"Compilar APK Android"** (pestaña *Actions* del repo).
3. Espera unos 3-5 minutos a que termine (ícono verde ✅).
4. Entra a esa ejecución → sección **Artifacts** → descarga
   `control-almacen-geova-debug-apk` (es un .zip que trae el .apk adentro).
5. Instala el .apk en tu teléfono (puede que Android te pida permitir
   "instalar apps de orígenes desconocidos" la primera vez).

Si algún día quieres volver a compilar sin cambiar nada, entra a *Actions* →
el workflow → botón **"Run workflow"** (está disponible manualmente gracias
a `workflow_dispatch`).

## Qué se ajustó en `index.html` para que funcione igual de bien empaquetada

Una app compilada con Capacitor corre dentro de un WebView de Android, que
no se comporta exactamente igual que Chrome. Se hicieron 3 ajustes puntuales,
sin tocar nada de la lógica de negocio ni el diseño:

1. **Descargas de archivos (respaldo JSON, .zip, CSV, reportes).**
   El truco de `<a download>` con blob no guarda archivos de forma confiable
   dentro de un WebView empaquetado. Ahora `descargarArchivo()` primero
   intenta con la API nativa de "Compartir" del teléfono (abre el panel de
   Android para guardar en Archivos, mandar por WhatsApp, etc.), y si el
   dispositivo no la soporta (por ejemplo un navegador de escritorio), usa
   exactamente el método de siempre. En el navegador normal no cambia nada.

2. **Botones de "Compartir por WhatsApp" que usaban `wa.me`.**
   Usaban `window.open(url, '_blank')`, que dentro de una app compilada
   puede devolver `null` porque no hay soporte de ventanas nuevas (esto ya
   le pasa al botón de imprimir/PDF, que sí avisa con un mensaje). Se
   cambiaron a `window.location.href = url`, igual que ya hacía el otro
   botón de WhatsApp que sí funcionaba bien, así el WebView redirige directo
   a la app de WhatsApp sin depender de ventanas emergentes.

3. Se revisó que **ninguna función esté envuelta en un IIFE** (ese fue el
   problema que tuviste con "Arqueó Stock"), así que todos los `onclick` del
   HTML siguen encontrando sus funciones sin problema.

4. **La barra de estado tapaba el encabezado y aparecía una franja negra al
   lado.** Las apps nuevas de Capacitor en Android 15 usan "edge-to-edge" por
   defecto: el WebView se dibuja detrás de la barra de estado en vez de
   respetar su espacio, y en algunos teléfonos eso también desalinea el
   ancho del contenido (la franja negra que viste a la izquierda). Se agregó
   el plugin `@capacitor/status-bar` configurado en `capacitor.config.json`
   para que la barra de estado tenga su propio espacio reservado (como
   siempre debió ser), más un respaldo en CSS (`env(safe-area-inset-*)`) y
   en JS por si algún dispositivo puntual no toma la configuración nativa.
   En un navegador normal esto no cambia nada visualmente.

5. **Ese mismo corte seguía apareciendo solo en la pestaña Historial.** Se
   reforzó con `overflow-x: hidden` también en `<html>` (antes solo estaba en
   `<body>`) y con `max-width: 100%` en el contenedor de la tabla, para que
   una tabla ancha se desplace solo ella misma en vez de empujar toda la
   página.

6. **Rotación automática a horizontal en Búsqueda, Reportes e Historial.**
   Esas 3 pestañas tienen tablas con varias columnas que se leen mejor en
   horizontal. Se agregó el plugin `@capacitor/screen-orientation`: al entrar
   a cualquiera de esas 3 pestañas, el teléfono gira solo a horizontal
   (sin importar si el usuario tiene la rotación automática desactivada en
   los ajustes del sistema — el bloqueo por app funciona igual), y al pasar
   a cualquier otra pestaña vuelve solo a vertical. En el navegador normal
   esto no hace nada; la rotación la sigue controlando el usuario como
   siempre.

## Limitación conocida (no se tocó, a propósito)

El botón de generar **PDF por impresión** (`window.open('', '_blank')` para
armar una ventana e imprimir) probablemente seguirá mostrando el aviso de
"el navegador bloqueó la ventana nueva" dentro del APK, porque los WebView
empaquetados no soportan ventanas nuevas para imprimir. La app ya avisa con
un mensaje claro en vez de fallar en silencio, así que no rompe nada — solo
esa función puntual de imprimir no estará disponible en el APK (sí seguirá
funcionando normal si abres el mismo `index.html` en el navegador del
teléfono o en una PC). Si más adelante quieres esa función también dentro
del APK, se puede resolver con el plugin `@capacitor/share` compartiendo el
HTML como texto, pero no lo agregué ahora para no sumar una dependencia
nueva sin que la pidas primero.

## Ícono de la app

Se usó la imagen que enviaste (`resources/icon.png`, recortada a cuadrado y
sin el ícono de lupa de la esquina) como ícono de la app. Cada vez que
Actions compila, genera automáticamente todos los tamaños que pide Android
(incluyendo el ícono adaptativo redondo/cuadrado de Android 8+) a partir de
esa sola imagen — no hace falta subir varios tamaños a mano. Si más adelante
quieres cambiarlo, solo reemplaza `resources/icon.png` por otra imagen
cuadrada (idealmente 1024×1024) y vuelve a compilar.

## Chart.js y el import de Excel siguen necesitando internet

Igual que en el navegador, las gráficas (Chart.js) y la importación de Excel
(librería xlsx) se cargan desde CDN solo cuando hacen falta, y la app ya
avisa si no hay conexión — eso no cambió, y se comporta igual dentro del
APK: sin internet, todo lo demás (catálogo, stock, WhatsApp de texto,
respaldos) sigue funcionando 100% offline.
