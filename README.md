# NomyAgenda

Aplicación de agenda personal para Android, desarrollada en Kotlin con arquitectura MVVM, almacenamiento local mediante Room y sincronización opcional en la nube con Firebase.

---

## 📱 Descripción

NomyAgenda es una app de productividad personal que permite gestionar notas, tareas con checklist y recordatorios con notificaciones programadas, todo en un mismo lugar. Funciona sin conexión a internet y, opcionalmente, sincroniza las entradas con Firebase Firestore mediante una cuenta de correo electrónico.

---

## ✅ Funcionalidades actuales

### 🔐 Autenticación
- **Inicio de sesión y registro** con correo electrónico y contraseña (Firebase Auth).
- Tras iniciar sesión, las entradas almacenadas en Firestore se sincronizan automáticamente al dispositivo.
- La app funciona sin cuenta: las entradas se guardan localmente en todo momento.

### 📋 Agenda
- Listado de todas las entradas con la fecha de hoy en el encabezado.
- **Tira de calendario semanal** con navegación por semanas; los días con entradas muestran un indicador de punto. Al pulsar un día se filtran las entradas de esa fecha.
- **Buscador en tiempo real** por título, contenido o etiquetas.
- **Filtro por tipo** mediante chips (Nota / Tarea / Recordatorio).
- **Ordenación personalizada** por fecha de vencimiento, fecha de creación o categoría.
- **Añadir entrada** mediante el botón flotante (FAB).
- **Eliminar entrada** con confirmación por diálogo.
- Estado vacío con mensaje cuando no hay entradas.

### ✏️ Editor de entradas
Cada entrada puede ser de uno de estos tres tipos:

| Tipo | Descripción |
|------|-------------|
| **Nota** | Texto libre con **previsualización Markdown** formateada. |
| **Tarea** | Checklist interactivo: añade y marca elementos como completados. |
| **Recordatorio** | Texto con fecha y hora; dispara una notificación en el momento indicado. |

Campos adicionales disponibles en todos los tipos:
- **Etiquetas** (separadas por comas) para clasificación libre.
- **Color de entrada** — paleta de 10 colores predefinidos para identificar visualmente cada entrada.

### 📝 Edición y previsualización Markdown
- Las notas se renderizan con formato usando la librería **Markwon** (con soporte HTML).
- **Barra de herramientas de formato** integrada en el editor: negrita, cursiva, encabezado, lista de viñetas, lista numerada, cita y color de texto (mediante etiquetas HTML `<font>`).
- Alternancia entre modo **edición** y modo **previsualización** con un toggle.

### 🔔 Notificaciones y recordatorios
- Canal de notificaciones dedicado (`nomy_reminders`).
- Programación de alarmas exactas con `AlarmManager` (`setExactAndAllowWhileIdle`).
- Compatible con la restricción de alarmas exactas de Android 12+ (`SCHEDULE_EXACT_ALARM`).
- Cancelación automática de la alarma si se elimina el recordatorio.
- **Reagendado automático al reiniciar** el dispositivo mediante `BootReceiver` (`BOOT_COMPLETED`).
- **Aviso anticipado configurable** (sin aviso, 1 hora, 1 día o 1 semana antes).

### ☁️ Sincronización con la nube
- Sincronización **write-through** con **Firebase Firestore**: cada creación, edición o eliminación se replica en la nube si el usuario está autenticado.
- Room es la fuente de verdad; Firestore actúa como caché remota.
- Tolerante a fallos de red: las operaciones locales nunca se bloquean por falta de conectividad.
- Al iniciar sesión se descarga y fusiona automáticamente el historial de entradas almacenado en Firestore.

### ⚙️ Ajustes
- **Tema claro / oscuro / sistema**.
- **Tema decorativo**: lavanda (por defecto), océano, bosque o atardecer. Al activarse, fuerza el modo claro.
- **Fondo ilustrado**: 9 ilustraciones seleccionables (floral, estrellas, geométrico, puntos, hojas, mariposa, mandala, montañas, olas) o sin fondo. Al activarse, fuerza el modo claro.
- **Idioma**: español, inglés o según el sistema (usando `AppCompatDelegate`).
- **Notificaciones**: activar o desactivar globalmente.
- **Aviso anticipado**: seleccionar el tiempo de antelación para recordatorios.
- **Cuenta**: muestra el correo del usuario autenticado y botón de cierre de sesión.

### 🗄️ Base de datos local
- Persistencia con **Room** (SQLite).
- Operaciones reactivas mediante **Kotlin Flow** y **LiveData**.
- Soporte para insertar y actualizar en una sola operación (`upsert`).

---

## 🛠️ Stack tecnológico

| Componente | Tecnología |
|------------|------------|
| Lenguaje | Kotlin |
| Arquitectura | MVVM (ViewModel + LiveData + Flow) |
| Base de datos local | Room (SQLite) |
| Base de datos en la nube | Firebase Firestore |
| Autenticación | Firebase Auth (email/contraseña) |
| Navegación | Navigation Component (Safe Args) |
| UI | Material Design 3, ViewBinding |
| Renderizado Markdown | Markwon (core + HTML) |
| Concurrencia | Coroutines + coroutines-play-services |
| SDK mínimo | Android 7.0 (API 24) |
| SDK objetivo | Android 14 (API 34) |
| Versión actual | 1.0 |

---

## 🔧 Configuración de Firebase

Para habilitar la autenticación y la sincronización en la nube consulta el archivo [FIREBASE_SETUP.md](FIREBASE_SETUP.md).

---

## 🚀 Próximas funcionalidades

- [ ] **Filtro por categoría** — Filtrar la lista de agenda por categoría además de por tipo.
- [ ] **Exportación / importación** — Respaldar y restaurar entradas en formato JSON o CSV.
- [ ] **Widget para pantalla de inicio** — Visualización rápida de las próximas tareas y recordatorios.

---

## 📂 Estructura del proyecto

```
app/src/main/kotlin/com/nomyagenda/app/
├── data/
│   ├── local/
│   │   ├── dao/          # AgendaEntryDao, AgendaEventDao
│   │   ├── database/     # NomyAgendaDatabase (Room)
│   │   └── entity/       # AgendaEntry, AgendaEvent, ChecklistItem, EntryType, SortOrder
│   ├── preferences/       # SettingsRepository
│   ├── remote/            # FirestoreDataSource
│   └── repository/       # AgendaRepository
├── notifications/         # NotificationHelper, ReminderReceiver, BootReceiver
├── ui/
│   ├── agenda/            # AgendaFragment, AgendaViewModel, AgendaAdapter
│   ├── auth/              # LoginFragment, LoginViewModel
│   ├── editor/            # EntryEditorFragment, EntryEditorViewModel, ChecklistAdapter, ChecklistManager
│   └── settings/          # SettingsFragment, SettingsViewModel
├── MainActivity.kt
└── NomyAgendaApp.kt
```
