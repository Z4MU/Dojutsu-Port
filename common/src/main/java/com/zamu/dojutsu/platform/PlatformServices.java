package com.zamu.dojutsu.platform;

import java.util.ServiceLoader;

/**
 * Fachada de acceso a los servicios de plataforma. Cada servicio es un puerto cuya
 * implementacion aporta el loader activo (NeoForge o Fabric) y se resuelve por
 * {@link ServiceLoader} a partir de los ficheros {@code META-INF/services/*}.
 *
 * <p>Es el unico punto por el que el codigo comun accede a capacidades dependientes
 * del loader, manteniendo el resto del mod libre de imports de plataforma (DIP).
 */
public final class PlatformServices {

    /** Puerto de registro diferido de contenido. */
    public static final RegistrarService REGISTRAR = load(RegistrarService.class);

    /** Puerto de Data Attachments por entidad. */
    public static final AttachmentService ATTACHMENTS = load(AttachmentService.class);

    /** Puerto de red cliente<->servidor (payloads C2S/S2C). */
    public static final NetworkService NETWORK = load(NetworkService.class);

    private PlatformServices() {
    }

    private static <T> T load(Class<T> service) {
        return ServiceLoader.load(service)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No se encontro implementacion de plataforma para " + service.getName()));
    }
}
