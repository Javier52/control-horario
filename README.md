# Control Horario (Android)

App para fichar entrada/salida con distinción entre **horas normales** y **horas de alta demanda**,
gráfica semanal con detalle diario e **informe diario por email** a javiermarcosfernandez@gmail.com.

## Franjas de alta demanda (de `horas_alta_demanda.xlsx`)
| Día | Franja |
|---|---|
| Lunes–Jueves | 07:00–10:00 y 16:00–20:00 |
| Viernes | 13:00–24:00 |
| Sábado y Domingo | 00:00–06:00 |

Un fichaje que cruza medianoche se reparte por día natural (viernes 22:00 → sábado 02:00 = 4 h de alta demanda).
Las franjas están en `HoursCalculator.windows()`.

## Informe diario
Se envía cada día a la hora configurada (22:00 por defecto) por SMTP de Gmail.
1. Activa la verificación en dos pasos en tu cuenta de Google.
2. Crea una **contraseña de aplicación** (myaccount.google.com/apppasswords).
3. En la app: *Ajustes* → pega la contraseña → *Guardar*. (Se guarda solo en el móvil, nunca en el repositorio.)
4. *Guardar y enviar informe ahora* para probarlo.

## Compilar
El workflow **Build APK** (GitHub Actions) compila y publica el APK como artefacto:
pestaña *Actions* → última ejecución → `control-horario-debug-apk`.
En local: `gradle :app:assembleDebug` (JDK 17 + Android SDK).

## Stack
Kotlin · Jetpack Compose · WorkManager · JavaMail (android-mail). Datos en almacenamiento local.
