// =========================================================================
// FileSaverPlugin.java
//
// DÓNDE VA:
//   android/app/src/main/java/TU/PAQUETE/AQUI/FileSaverPlugin.java
//
// Reemplaza "TU/PAQUETE/AQUI" por la misma ruta de carpetas que ya usa tu
// MainActivity.java (mira la línea "package ..." al inicio de ese archivo,
// ej: package com.tuempresa.gestorinventario;). El nombre del paquete de
// abajo debe coincidir EXACTO con el de tu MainActivity.
//
// QUÉ HACE:
//   Recibe un archivo en base64 desde JS y lo escribe directo en la carpeta
//   pública Descargas/GestorInventario/ usando MediaStore, que es la API
//   oficial de Android para guardar en Descargas sin pedir permisos de
//   almacenamiento (funciona en Android 10 en adelante sin tocar nada del
//   AndroidManifest). Esto es exactamente lo mismo que ya resolviste antes
//   con FileSaverPlugin en Arqueo Stock.
// =========================================================================

package TU.PAQUETE.AQUI;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.io.OutputStream;
import java.io.File;
import java.io.FileOutputStream;
import android.util.Base64;

@CapacitorPlugin(name = "FileSaver")
public class FileSaverPlugin extends Plugin {

    // Subcarpeta dentro de Descargas donde se guarda todo. Cámbiala si quieres
    // otro nombre de carpeta.
    private static final String CARPETA = "GestorInventario";

    @PluginMethod
    public void guardar(PluginCall call) {
        String nombre = call.getString("nombre");
        String datosBase64 = call.getString("datos");
        String tipo = call.getString("tipo", "application/octet-stream");

        if (nombre == null || datosBase64 == null) {
            call.reject("Faltan 'nombre' o 'datos'");
            return;
        }

        try {
            byte[] datos = Base64.decode(datosBase64, Base64.DEFAULT);
            Context context = getContext();
            String rutaMostrada;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+: API MediaStore, sin permisos especiales.
                ContentValues valores = new ContentValues();
                valores.put(MediaStore.Downloads.DISPLAY_NAME, nombre);
                valores.put(MediaStore.Downloads.MIME_TYPE, tipo);
                valores.put(MediaStore.Downloads.RELATIVE_PATH,
                        android.os.Environment.DIRECTORY_DOWNLOADS + "/" + CARPETA);

                Uri uri = context.getContentResolver().insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI, valores);

                if (uri == null) {
                    call.reject("No se pudo crear el archivo en Descargas");
                    return;
                }

                OutputStream out = context.getContentResolver().openOutputStream(uri);
                if (out == null) {
                    call.reject("No se pudo abrir el archivo para escribir");
                    return;
                }
                out.write(datos);
                out.flush();
                out.close();

                rutaMostrada = "Descargas/" + CARPETA + "/" + nombre;
            } else {
                // Android 9 o menor: escritura directa a la carpeta pública.
                // (Requiere permiso WRITE_EXTERNAL_STORAGE en el Manifest para
                // estas versiones viejas; si tu app ya soporta solo Android 10+
                // este bloque nunca se usa.)
                File carpeta = new File(
                        android.os.Environment.getExternalStoragePublicDirectory(
                                android.os.Environment.DIRECTORY_DOWNLOADS), CARPETA);
                if (!carpeta.exists()) {
                    carpeta.mkdirs();
                }
                File archivo = new File(carpeta, nombre);
                FileOutputStream out = new FileOutputStream(archivo);
                out.write(datos);
                out.flush();
                out.close();
                rutaMostrada = archivo.getAbsolutePath();
            }

            JSObject resultado = new JSObject();
            resultado.put("ruta", rutaMostrada);
            call.resolve(resultado);

        } catch (Exception e) {
            call.reject("Error al guardar el archivo: " + e.getMessage(), e);
        }
    }
}
