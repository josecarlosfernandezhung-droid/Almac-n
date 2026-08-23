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

## Chart.js y el import de Excel siguen necesitando internet

Igual que en el navegador, las gráficas (Chart.js) y la importación de Excel
(librería xlsx) se cargan desde CDN solo cuando hacen falta, y la app ya
avisa si no hay conexión — eso no cambió, y se comporta igual dentro del
APK: sin internet, todo lo demás (catálogo, stock, WhatsApp de texto,
respaldos) sigue funcionando 100% offline.
