# RetroTech — etapa 1

Remake no oficial y hecho desde cero, inspirado en IndustrialCraft 2 y BuildCraft, para **Minecraft 1.20.1 con Forge**.
No usa código ni texturas de IC2 ni de BuildCraft: todo es original.

## Cómo compilarlo

1. Instalá **Java 17** (por ejemplo, Eclipse Temurin 17).
2. Descargá el **MDK de Forge 1.20.1** desde https://files.minecraftforge.net (botón "Mdk") y descomprimilo en una carpeta.
3. En esa carpeta **borrá la carpeta `src/`** del MDK.
4. Copiá dentro de la carpeta del MDK la carpeta `src/` y el archivo `gradle.properties` de este zip (reemplazando el que viene).
   - El `build.gradle` y el `gradlew` del MDK se quedan como están.
5. Abrí una terminal en la carpeta y ejecutá:
   - Windows: `gradlew runClient` (para probar) o `gradlew build` (para generar el .jar)
   - Linux/Mac: `./gradlew runClient` o `./gradlew build`
6. El .jar queda en `build/libs/retrotech-0.1.0.jar`. Va en la carpeta `mods` de una instalación de Forge 1.20.1.

La primera vez Gradle descarga Minecraft y Forge, así que tarda varios minutos.
Si aparece algún error de compilación, copiame el mensaje completo y lo corrijo.

## Qué incluye

**Energía (EU):** 1 EU = 1 FE, así que también funciona con máquinas y cables de otros mods.
Hay niveles de voltaje como en IC2: si una fuente de más voltaje alimenta una máquina de menor nivel, **la máquina explota**.

| Bloque | Datos |
|---|---|
| Mineral de estaño (normal y de pizarra profunda) | Aparece entre Y -32 y Y 96 |
| Generador | Quema combustible: 10 EU/t (un carbón = 4.000 EU). Guarda 4.000 EU. Emite por los 6 lados. |
| BatBox | Guarda 40.000 EU. Recibe por todos los lados menos el frente; emite 32 EU/t por el frente. |
| Horno eléctrico | Usa las recetas del horno: 3 EU/t, 130 ticks por item. |
| Cable de estaño | 32 EU/t, pérdida 0,025 EU por bloque |
| Cable de cobre | 128 EU/t, pérdida 0,2 EU por bloque |
| Cable de oro | 512 EU/t, pérdida 0,4 EU por bloque |

**Recetas:**
- Lingote de estaño: fundiendo estaño en bruto o el mineral (horno o alto horno). Bloque de estaño con 9 lingotes.
- Cable de estaño: 3 lingotes de estaño en fila → 6 cables.
- Cable de cobre / oro: fila de lingotes entre dos filas de lana → 6 cables.
- Carcasa de máquina: 8 lingotes de hierro alrededor.
- Generador: redstone arriba, estaño–carcasa–estaño en el medio, horno abajo.
- Horno eléctrico: cable de estaño arriba, redstone–horno–redstone en el medio, carcasa abajo.
- BatBox: madera–cable–madera / 3 redstone / 3 maderas.

Las tolvas pueden cargar combustible en el generador, cargar y vaciar el horno eléctrico.

## Estructura del código

- `energy/` — red de energía (`EnergyNet`), niveles de voltaje y almacenamiento.
- `block/` — cables y bloques de máquinas.
- `blockentity/` — lógica de cada máquina.
- `menu/` y `client/` — interfaces.
- `registry/` — registro de bloques, items, menús y la pestaña creativa.
- `resources/data/retrotech/worldgen` — generación del estaño en el mundo.

## Próximas etapas

2. Árboles de caucho y caucho, cables aislados, macerador, extractor, compresor, polvos (duplicar minerales), baterías y paneles solares.
3. BuildCraft: motores, tuberías de items y de fluidos, quarry.
4. Petróleo y refinería, MFE/MFSU, reactor nuclear y herramientas eléctricas.
