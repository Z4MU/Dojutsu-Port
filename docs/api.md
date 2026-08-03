# API pública de Dojutsu (F10/DJT-220)

> Versión de API **1.0.0**, congelada al cerrar F10. Sigue *semver* sobre **la superficie de
> `com.zamu.dojutsu.api`**, no sobre la versión del mod.

## Qué es público y qué no

Lo que está en **`com.zamu.dojutsu.api`** es el contrato. Todo lo demás —`data.*`, `core.*`, los servicios, los
registros internos— es implementación y **puede cambiar sin aviso**, aunque sea `public` en Java: el mod es un
solo jar y no hay forma de esconderlo del compilador. Que algo sea alcanzable no lo hace parte del contrato.

Compruébalo al arrancar; es barato y evita fallos raros:

```java
if (DojutsuApi.majorVersion() != 1) {
    throw new IllegalStateException("Dojutsu API incompatible: " + DojutsuApi.API_VERSION);
}
```

## 1. Leer el estado de un ninja

```java
NinjaView ninja = DojutsuApi.view(player);

ninja.rank();                        // NinjaRank derivado del ninja_xp
ninja.chakra() + "/" + ninja.maxChakra();
ninja.skill(Skill.MEDICAL);          // 0 si nunca lo subió
ninja.affinity(NatureType.FIRE);
ninja.activeEye();                   // Optional<String>
ninja.isJinchuriki();
ninja.knows("great_fireball");
```

Es una **foto inmutable**, no una ventana: refleja el instante en que se pidió. Para reaccionar a los cambios,
suscríbete a los eventos — no sondees esto cada tick.

No se entrega el agregado real a propósito. Sus sub-estados son inmutables, pero el agregado tiene puertas de
escritura, y repartirlo permitiría saltarse los servicios que existen para que el chakra no se toque por dos
sitios.

## 2. Escribir

```java
DojutsuApi.restoreChakra(player, 50);
boolean pagado = DojutsuApi.consumeChakra(player, 200);   // todo o nada
DojutsuApi.addNinjaXp(player, 100);
DojutsuApi.grantJutsu(player, "great_fireball");
```

Cada uno delega en el **mismo servicio** que usa el mod. No es ceremonia: esas puertas hacen el clamp, emiten
los eventos de dominio y sincronizan al cliente. Escribir por fuera dejaría el HUD mintiendo y los desbloqueos
sin disparar.

## 3. Aportar contenido

### Datos (se recargan con `/reload`)

Cuatro eventos, uno por tipo de contenido data-driven. Saltan **en cada recarga**, después de leer los JSON del
mod y antes de publicar el registro — así tu contenido se recarga con el resto y no se queda pegado de un
arranque anterior.

```java
DojutsuRegistryEvents.JUTSU.register(sink ->
        sink.add("mimod_llamarada", new JutsuEntry(definicion, params)));

DojutsuRegistryEvents.EYE.register(sink -> sink.add("mimod_ojo", definicionDeOjo));
DojutsuRegistryEvents.CLAN.register(sink -> sink.add("mimod_clan", definicionDeClan));
DojutsuRegistryEvents.CONTRACT.register(sink -> sink.add("mimod_contrato", contrato));
```

**Escribir un id que ya existe lo sustituye.** Es una capacidad, no un accidente: sirve para rebalancear
contenido base. El arranque deja constancia en el log de cuántas entradas aportó cada evento, así que una
sustitución no pasa desapercibida.

### Código (una vez, en el arranque)

Las familias de efecto, las habilidades oculares y las técnicas de bestia son código, no datos, y no se recargan:

```java
DojutsuApi.registerJutsuEffect("mimod_rebote", new MiEfecto());
DojutsuApi.registerEyeAbility("mimod_vision", new MiHabilidad());
DojutsuApi.registerBeastMove(new MiTecnicaDeBestia());
```

Una vez registrada la familia, cualquier JSON de jutsu puede ponerla en su campo `effect` — incluidos los del
propio mod si alguien los sustituye.

## 4. Escuchar

```java
DojutsuApi.subscribe(EyeAwakenedEvent.class, e -> { ... });
DojutsuApi.subscribe(JutsuCastEvent.class,   e -> { ... });
DojutsuApi.subscribe(BeastSealedEvent.class, e -> { ... });
```

Los tipos están en `com.zamu.dojutsu.domain.event`: despertar y evolución de ojo, cast de jutsu, sellado de
bestia, cambio de nivel y de ninja_xp, concesión de naturaleza y de Kekkei Genkai, y encendido de modo.

Son **eventos de servidor** y se emiten dentro del tick: el suscriptor debe ser corto y no bloquear.

## Lo que la API no hace

- **No expone los temporizadores de ejecución** (pulsos de modo, cooldowns internos, contadores de despertar):
  son detalle de implementación y cambiarían el contrato cada vez que se afinara un sistema.
- **No permite desregistrar.** Los eventos de Architectury no se desuscriben; el contenido aportado vive
  mientras viva el juego.
- **No da acceso al cliente.** Todo lo de aquí es servidor. Un mod que quiera pintar algo usa sus propios
  hooks de cliente y lee su copia sincronizada.

## Por qué esta API llega la última

Se dejó para el final del proyecto a propósito. Una superficie pública es una promesa, y prometer sobre sistemas
que aún no se habían visto funcionar habría sido firmar a ciegas: la primera sesión de verificación en partida
encontró **cinco fallos reales** que ningún test veía —entre ellos que las Bestias con Cola no tenían caja de
colisión donde se las veía—. Lo que se congela aquí ya se ha ejecutado, mirado y corregido.
