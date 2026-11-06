<p align="center">
  <img src="assets/jflap_logo_256.png" width="96" alt="JFLAP Studio Logo" />
</p>

<h1 align="center">JFLAP Studio</h1>

<p align="center">
  <b>Entorno moderno estilo IDE (JetBrains Island Dark / Light) para diseñar, depurar y simular Autómatas Finitos, Autómatas de Pila, Gramáticas Formales y Máquinas de Turing.</b>
</p>

---

## ✨ Características Principales

- **Interfaz Unificada Estilo IDE Moderno**:
  - Cabecera superior oscura acoplada sin barra blanca nativa de Windows, con controles vectoriales (`—`, `□`, `×`), arrastre fluido y redimensionamiento en 8 direcciones.
  - Temas **JetBrains Island Dark** (`#1E1F22` / `#2B2D30`) e **Island Light** intercambiables en tiempo real (`Ctrl + Shift + D`).
  - **Pestañas multiproyecto** en una sola ventana para trabajar con varios archivos `.jff` simultáneamente sin abrir ventanas sueltas.
- **Lienzo Vectorial Optimizado**:
  - Barra de herramientas flotante tipo píldora (*Floating Dock*) integrada sobre el lienzo.
  - Agrupación inteligente de múltiples reglas de transición en contenedores limpios y compactos (evitando torres gigantes de texto).
  - Cuadrícula de puntos (`Ctrl + G`), ajuste magnético (*Imán*, `Ctrl + Shift + G`), zoom suave con rueda del ratón y paneo libre.
  - Creación rápida de transiciones y lazos con barra de acciones rápidas (`+ Otra regla`, `Invertir`, `+ → Nuevo Estado`).
- **Simulación Moderna de Máquinas de Turing y Autómatas**:
  - Cinta de Turing rediseñada con celdas tipo tarjeta, cabezal resaltado con insignia de estado activa (`q0`, `q1`...) y desplazamiento interpolado suave.
  - Barra multimedia de depuración paso a paso (*Reiniciar*, *Paso Atrás*, *Reproducir/Pausa*, *Paso Adelante*, *Detener* y selector de velocidad `0.5x`, `1x`, `2x`, `Max`).
  - Editor de **Tabla de Transiciones de Turing** (`Ctrl + Shift + T`).
- **Expresiones Abreviadas en Transiciones**:
  - Soporte directo para rangos `[a-z]`, `[0-9]`, clases `\d`, `\w`, `\s` y negaciones `[^abc]`.

---

## 🚀 Ejecución Rápida

### En Windows (Recomendado)
Ejecuta directamente **`JFLAP Studio.exe`** (ejecutable autocontenido con el núcleo integrado).

### Multiplataforma (Windows / macOS / Linux)
Requiere Java 8 o superior:
```bash
java -jar JFLAP.jar
```

---

## ⌨️ Atajos de Teclado

| Atajo | Acción |
| :--- | :--- |
| `Ctrl + N` | Menú rápido para crear nuevo modelo en una pestaña |
| `Ctrl + O` | Explorador moderno de proyectos `.jff` |
| `Ctrl + 1..4` | Cambiar herramienta (*Cursor*, *Estado*, *Transición*, *Borrar*) |
| `Ctrl + B` | Mostrar / Ocultar panel lateral (*Inspector de Estado* y *Proyectos*) |
| `Ctrl + Z` / `Ctrl + Y` | Deshacer / Rehacer cambios |
| `Ctrl + D` | Duplicar estados seleccionados y sus transiciones |
| `Ctrl + G` | Mostrar / Ocultar cuadrícula de guía |
| `Ctrl + Shift + G` | Activar / Desactivar ajuste magnético (*Imán*) |
| `Ctrl + Shift + T` | Abrir editor de Tabla de Transiciones (Máquinas de Turing) |
| `Ctrl + Shift + D` | Alternar entre tema oscuro (*Island Dark*) y claro (*Island Light*) |
| `F5` | Ejecutar prueba múltiple (*Multiple Run*) al instante |

---

## 🛠️ Compilación desde Código Fuente

En Windows PowerShell (con JDK instalado):
```powershell
.\build.ps1
```
