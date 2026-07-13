# Convenciones de código — Dojutsu Port

> Convenciones **realmente aplicadas** durante la Fase 0. Seguir para mantener consistencia.

## 1. Estructura de paquetes

- Namespace raíz: **`com.zamu.dojutsu`** (`maven_group`).
- Paquetes por **capacidad**, no por tipo técnico. Existentes hoy:
  - `platform` — puertos de plataforma + `PlatformServices`.
  - `registry` — clases `Mod*` de registro.
  - `core` — infraestructura del motor (hoy: `DataReloadListener`).
  - `entity`, `network`, `debug` — (network/debug son **temporales de F0**).
- Adaptadores por loader bajo `com.zamu.dojutsu.<loader>` (`neoforge`, `fabric`); el código de
  cliente de NeoForge en `neoforge.client`.
- **Regla dura:** `common` nunca importa clases específicas de loader; solo puertos.

## 2. Nombres de clases

- **Puertos:** sustantivo + `Service` (`RegistrarService`, `AttachmentService`, `NetworkService`).
- **Implementaciones por loader:** `<Loader><Puerto>` (`NeoForgeRegistrarService`,
  `FabricAttachmentService`, …).
- **Entrypoints:** `Dojutsu<Loader>` (`DojutsuNeoForge`, `DojutsuFabric`, `DojutsuFabricClient`).
- **Registries:** `Mod<Tipo>` en plural (`ModItems`, `ModEntities`, `ModAttributes`, `ModSounds`,
  `ModParticles`, `ModAttachments`).
- **Payloads:** `<Nombre>Payload` (`PingPayload`).
- **Clases de utilidad / registries:** `final` con **constructor privado** (no instanciables).

## 3. Nombres de métodos e identificadores

- Métodos en `camelCase`, verbos claros (`register`, `commit`, `sendToServer`, `handle`).
- Cada registry expone un método estático **`register()`** llamado desde `DojutsuMod.init()`.
- IDs de contenido con el helper **`DojutsuMod.id(path)`** → `dojutsu:<path>` (nunca construir
  `ResourceLocation` a mano en las clases de registro).
- Constantes en `UPPER_SNAKE_CASE` (`MOD_ID`, `NINJA_DEBUG`, `MAX_CHAKRA`).

## 4. Organización de registries

- Una clase `Mod*` por tipo de registro.
- Patrón: un `Registrar<T>` estático creado con `PlatformServices.REGISTRAR.create(Registries.X)`;
  cada entrada es un `RegistryEntry<...>` estático `public static final`; `register()` llama a
  `commit()`.
- Los attachments usan `PlatformServices.ATTACHMENTS.register(...)` (no `REGISTRAR`).
- **Toda `EntityType` registrada DEBE tener un renderer en cliente** (usar `NoopRenderer` para
  marcadores) o el cliente crashea.

## 5. Puertos y plataforma

- Interfaz del puerto en `common/platform`; una impl por loader; declararla en
  `META-INF/services/<FQCN-del-puerto>` de cada módulo de loader.
- Resolución vía `PlatformServices` (`ServiceLoader.load(...).findFirst()`).
- Código de cliente-only de Fabric (p. ej. `ClientPlayNetworking`) se **aísla** en una clase aparte
  (`FabricClientNetworking`) invocada solo bajo `FabricLoader...getEnvironmentType() == CLIENT`.

## 6. Eventos y registro

- Preferir el evento **cross-loader** de Architectury cuando funcione y sea estable (comandos:
  `CommandRegistrationEvent`; reload: `ReloadListenerRegistry`; cliente-join: `ClientPlayerEvent`).
- **Excepción documentada:** el **networking** de Architectury (`NetworkManager`) está roto en Fabric
  13.0.8 → usar el puerto `NetworkService` (nativo por loader).
- NeoForge: eventos de cliente vía clase `@EventBusSubscriber(modid=MOD_ID, bus=MOD, value=Dist.CLIENT)`.

## 7. Estilo de código

- Java 21, encoding UTF-8. Indentación 4 espacios.
- **Javadoc** en clases/métodos públicos y en cada puerto (explicando el contrato).
- Comentarios y mensajes de log **en español**; **logs sin acentos** (evita problemas de encoding en
  consola). Formato de log: `LOGGER.info("[{}] ...", MOD_NAME, ...)`.
- Logger: `com.mojang.logging.LogUtils.getLogger()`.
- `records` para tipos de valor inmutables (payloads, `SimpleRegistryEntry`, registros internos).
- Marcar el andamiaje temporal explícitamente en el Javadoc ("Temporal; se retira con…").

## 8. Principios SOLID aplicados

- **DIP:** `common` depende de abstracciones (puertos); las impl concretas viven en los loaders.
- **SRP:** una responsabilidad por clase (un `Mod*` por registro, un `*Service` por puerto).
- **OCP:** añadir contenido = añadir un `RegistryEntry`/definición, sin tocar el core.
- **ISP:** puertos pequeños y enfocados.
- **DRY:** colapsar duplicación (p. ej. `SimpleRegistryEntry` compartido entre loaders).
- **KISS/YAGNI:** no adelantar mecanismos no necesarios en la subtarea actual.

## 9. Buenas prácticas del proyecto

- **Un ticket a la vez**; validar en runtime en **ambos** loaders antes de cerrar.
- **Fijar versiones exactas** conocidas-buenas (no "la última de cada cosa"); no bumpear
  loom/loader/neoforge/architectury sin verificar.
- No sugerir `/summon` de una entidad sin renderer.
- No introducir imports de loader en `common`.
- Mantener el **CI verde**; el `gradlew` debe versionarse con finales de línea **LF**
  (`.gitattributes`).
