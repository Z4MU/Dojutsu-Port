/**
 * <b>Superficie pública del mod Dojutsu</b> (F10/DJT-220) — lo único que otro mod debería tocar.
 *
 * <h2>El contrato</h2>
 * Lo que hay en este paquete se mantiene estable dentro de una misma {@link com.zamu.dojutsu.api.DojutsuApi
 * versión de API}. Todo lo demás —{@code data.*}, {@code core.*}, los servicios, los registros internos— es
 * implementación y <b>puede cambiar sin aviso</b>, aunque sea {@code public} en Java: el mod es un solo jar y
 * no hay forma de esconderlo del compilador. Que algo sea alcanzable no lo hace parte del contrato; lo que lo
 * hace es estar aquí.
 *
 * <h2>Tres cosas, y sólo tres</h2>
 * <ol>
 *   <li><b>Leer</b> el estado ninja de un jugador ({@link com.zamu.dojutsu.api.NinjaView}). Es una
 *       <b>copia inmutable</b>: no se entrega el agregado real, porque entonces cualquiera podría escribir en
 *       él saltándose las puertas del mod.</li>
 *   <li><b>Escribir</b> por las mismas puertas que usa el mod ({@code DojutsuApi.restoreChakra},
 *       {@code addNinjaXp}, {@code grantJutsu}…). No hay atajos: el chakra sólo se toca por su servicio y la XP
 *       sólo por el suyo, para terceros igual que para el propio mod (reglas 10 y 10b).</li>
 *   <li><b>Aportar contenido</b>: técnicas, ojos, clanes y contratos por
 *       {@link com.zamu.dojutsu.api.DojutsuRegistryEvents} (se recargan con {@code /reload}); familias de
 *       efecto, habilidades oculares y técnicas de bestia por {@code DojutsuApi.register*} en el arranque.</li>
 * </ol>
 *
 * <h2>Por qué esta API llega la última</h2>
 * Se dejó para el final del proyecto a propósito. Una superficie pública es una promesa, y prometer sobre
 * sistemas que aún no se habían visto funcionar habría sido firmar un contrato a ciegas: la primera sesión de
 * verificación en partida encontró cinco fallos reales que ningún test veía. Lo que se congela aquí ya se ha
 * ejecutado, mirado y corregido.
 *
 * @see com.zamu.dojutsu.api.DojutsuApi
 */
package com.zamu.dojutsu.api;
