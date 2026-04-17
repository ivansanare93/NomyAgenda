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

- [ ] **Módulo de Ajustes** — Configuración de la app: tema (claro/oscuro), idioma, preferencias de notificación.
- [ ] **Filtro por tipo y categoría** — Filtrar la lista de agenda por tipo de entrada (nota/tarea/recordatorio) o categoría.
- [ ] **Ordenación personalizada** — Ordenar entradas por fecha de creación, fecha de vencimiento o categoría.
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
│   │   └── entity/       # AgendaEntry, AgendaEvent, ChecklistItem, EntryType
│   └── repository/       # AgendaRepository
├── notifications/         # NotificationHelper, ReminderReceiver
├── ui/
│   ├── agenda/            # AgendaFragment, AgendaViewModel, AgendaAdapter
│   ├── editor/            # EntryEditorFragment, EntryEditorViewModel, ChecklistAdapter
│   └── settings/          # SettingsFragment (próximamente)
├── MainActivity.kt
└── NomyAgendaApp.kt
```
