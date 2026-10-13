# Pico TTS Pro

**Pico TTS Pro** es un motor de Texto a Voz (TTS) moderno, accesible e independiente para Android, basado en las librerías nativas de SVOX Pico TTS pero completamente reconstruido y rediseñado en **Kotlin**.

Diseñado pensando en la comunidad de personas con discapacidad visual y usuarios de lectores de pantalla como **TalkBack**, incorporando opciones avanzadas y lectura de emojis.

---

## 🚀 Características Principales

- **Lectura completa de Emojis Unicode (CLDR)**:
  - Base de datos oficial con soporte para más de 3.900 emojis.
  - Opciones de idioma para los emojis: automático (según la voz), español o inglés.

- **Forzar  Parámetros de Voz **:
  - **Forzar Velocidad**: Ignora las solicitudes de velocidad de TalkBack o aplicaciones del sistema y mantiene una velocidad fija estricta elegida por el usuario.
  - **Forzar Tono (Pitch)**: Fija el tono de voz deseado.
  - **Forzar Volumen**: Mantiene el volumen en un nivel constante.

- **Control de Puntuación**:
  - Niveles configurables: *Ninguna*, *Alguna*, *La mayoría*, *Toda*.

- **Compatibilidad Moderna con Android**:
  - Compatible desde **Android 5.0 (Lollipop - API 21)** hasta **Android 16+ (API 36)**.
  - Librerías C++ nativas compiladas con alineación de páginas de 16 KB (`max-page-size=16384`) para los procesadores más recientes.
  - Extracción de datos de voz instantánea y dinámica en el arranque (sin reiniciar el teléfono ni TalkBack).

- **Voces SVOX Pico Incluidas**:
  - 🇪🇸 Español (España) — Soporta variantes regionales (EE.UU., México, etc.) sin silenciarse.
  - 🇺🇸 Inglés (Estados Unidos)
  - 🇬🇧 Inglés (Reino Unido)
  - 🇫🇷 Francés
  - 🇩🇪 Alemán
  - 🇮🇹 Italiano

---

## 📱 Instalación

1. Descarga el archivo `picoTTS-Pro` desde la sección de **Releases** en GitHub.
2. Instálalo en tu dispositivo Android.
3. Ve a **Ajustes > Accesibilidad > Salida de texto a voz** (o **Ajustes del Sistema > Idioma e Introducción de texto > Texto a voz**).
4. Selecciona **Pico TTS Pro** como tu motor preferido.
5. Abre la aplicación **Pico TTS Pro** desde tu menú de aplicaciones para personalizar la velocidad forzada, los emojis y la puntuación.

---

## 🛠️ Compilación desde el código fuente

Requiere JDK 17 y Android SDK / NDK instalados:

```bash
# Clonar el repositorio
git clone https://github.com/RME-Systems/picoTTS-pro.git
cd picoTTS-pro

# Compilar APK Debug
./gradlew assembleDebug

# Compilar APK Release
./gradlew assembleRelease
```

```

---

## 📜 Créditos y Agradecimientos

- **SVOX Deutschland GmbH / AOSP**: Creadores originales del motor de síntesis de voz Pico TTS.
- **IsHacker003 y ericJrdn**: Por los portes independientes originales del motor a sistemas modernos de compilación.
- **The Unicode Consortium & Emojibase**: Por los datos oficiales de anotaciones CLDR de emojis.
- **RME Systems**: Desarrollo, arquitectura en Kotlin, interfaz accesible de opciones y soporte extendido.
