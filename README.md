# RobloxStudioLite Android

Aplikasi Android standalone yang mereplikasi fungsionalitas Roblox Studio Lite.

## Fitur

- 🎮 **3D Viewport** — OpenGL ES 2.0, render Part/Baseplate dengan lighting
- 🌍 **Explorer Panel** — Tree view semua Instance (Workspace, Players, dll)
- ⚙️ **Properties Panel** — Edit properti instance (Name, Size, Position, Color, dll)
- 📜 **Script Editor** — Editor Luau dengan syntax highlighting
- 🧱 **Toolbox** — Insert 45+ jenis Instance (Part, Script, GUI, dll)
- 📂 **File I/O** — Buka & simpan file `.rbxl` / `.rbxlx`
- ☁️ **Roblox Publish** — Upload ke Roblox via Open Cloud API
- ↩️ **Undo/Redo** — 50 level history
- ⚙️ **Settings** — Grid snap, camera speed, script font size, dll

## Tech Stack

- Kotlin + Jetpack Compose
- OpenGL ES 2.0 (tanpa dependency eksternal)
- OkHttp untuk Roblox Open Cloud API
- DataStore untuk settings

## Build via GitHub Actions

Push ke branch `main` → Actions otomatis build APK.
Download APK dari tab **Actions → Artifacts**.

## Publish ke Roblox

1. Buka [create.roblox.com](https://create.roblox.com) → Credentials → API Keys
2. Buat key baru dengan permission `universe-places:write`
3. Di app → tombol **Publish** → masukkan API Key, Universe ID, Place ID
# Stlite
