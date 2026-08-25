"""
registrar_plugin.py

Se ejecuta desde el workflow de GitHub Actions, después de `npx cap add android`.
Recibe como argumento la ruta a MainActivity.java (ya con FileSaverPlugin.java
copiado en la misma carpeta) y le agrega el registro del plugin, sin borrar
nada de lo que ya tenga esa clase.

Uso: python3 registrar_plugin.py android/app/src/main/java/.../MainActivity.java
"""
import sys
import re

path = sys.argv[1]

with open(path, 'r', encoding='utf-8') as f:
    contenido = f.read()

if 'FileSaverPlugin' in contenido:
    print("MainActivity.java ya tenía referencia al plugin, no se modifica")
else:
    metodo = (
        "\n    @Override\n"
        "    protected void onCreate(android.os.Bundle savedInstanceState) {\n"
        "        registerPlugin(FileSaverPlugin.class);\n"
        "        super.onCreate(savedInstanceState);\n"
        "    }\n"
    )
    nuevo_contenido, reemplazos = re.subn(
        r'(extends BridgeActivity\s*\{)', r'\1' + metodo, contenido, count=1
    )
    if reemplazos == 0:
        print("ERROR: no se encontró 'extends BridgeActivity {' en MainActivity.java")
        sys.exit(1)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(nuevo_contenido)
    print("Plugin FileSaver registrado correctamente en MainActivity.java")
