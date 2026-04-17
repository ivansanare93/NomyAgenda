# NomyAgenda

Aplicación de agenda personal para Android, desarrollada en Kotlin con arquitectura MVVM y almacenamiento local mediante Room.

---

## 📱 Descripción

NomyAgenda es una app de productividad personal que permite gestionar notas, tareas con checklist y recordatorios con notificaciones programadas, todo en un mismo lugar y sin necesidad de conexión a internet.

---

## ✅ Funcionalidades actuales

### 📋 Agenda
- Listado de todas las entradas con la fecha de hoy en el encabezado.
- **Buscador en tiempo real** por título, contenido o etiquetas.
- **Filtro por tipo** mediante chips (Nota / Tarea / Recordatorio).
- **Ordenación personalizada** por fecha de vencimiento, fecha de creación o categoría.
- **Añadir entrada** mediante el botón flotante (FAB).
- **Eliminar entrada** con pulsación larga y confirmación.
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
- **Categoría** (opcional) para agrupar entradas.

### 📝 Previsualización Markdown
- Las notas se renderizan con formato usando la librería **Markwon**.
- Soporte para negrita, cursiva, encabezados, listas, bloques de código y más.
- La previsualización se muestra automáticamente al visualizar una nota.

### 🔔 Notificaciones y recordatorios
- Canal de notificaciones dedicado (`nomy_reminders`).
- Programación de alarmas exactas con `AlarmManager` (`setExactAndAllowWhileIdle`).
- Compatible con la restricción de alarmas exactas de Android 12+ (`SCHEDULE_EXACT_ALARM`).
- Cancelación automática de la alarma si se elimina el recordatorio.
- **Reagendado automático al reiniciar** el dispositivo mediante `BootReceiver` (`BOOT_COMPLETED`).
- **Aviso anticipado configurable** (sin aviso, 1 hora, 1 día o 1 semana antes).

### ⚙️ Ajustes
- **Tema**: claro, oscuro o según el sistema.
- **Idioma**: español, inglés o según el sistema (usando `AppCompatDelegate`).
- **Notificaciones**: activar o desactivar globalmente.
- **Aviso anticipado**: seleccionar el tiempo de antelación para recordatorios.

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
| Base de datos | Room (SQLite) |
| Navegación | Navigation Component (Safe Args) |
| UI | Material Design 3, ViewBinding |
| Renderizado Markdown | Markwon |
| Concurrencia | Coroutines |
| SDK mínimo | Android 7.0 (API 24) |
| SDK objetivo | Android 14 (API 34) |
| Versión actual | 1.0 |

---

## 🚀 Próximas funcionalidades

- [ ] **Filtro por categoría** — Filtrar la lista de agenda por categoría además de por tipo.
- [ ] **Exportación / importación** — Respaldar y restaurar entradas en formato JSON o CSV.
- [ ] **Widget para pantalla de inicio** — Visualización rápida de las próximas tareas y recordatorios.
- [ ] **Sincronización en la nube** — Backup opcional con Google Drive o servidor propio.

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
│   └── repository/       # AgendaRepository
├── notifications/         # NotificationHelper, ReminderReceiver, BootReceiver
├── ui/
│   ├── agenda/            # AgendaFragment, AgendaViewModel, AgendaAdapter
│   ├── editor/            # EntryEditorFragment, EntryEditorViewModel, ChecklistAdapter, ChecklistManager
│   └── settings/          # SettingsFragment, SettingsViewModel
├── MainActivity.kt
└── NomyAgendaApp.kt
```
